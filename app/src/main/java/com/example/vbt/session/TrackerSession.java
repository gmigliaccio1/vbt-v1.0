package com.example.vbt.session;

import android.content.Context;


import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.vbt.algorithm.detection.realtime.RepListener;
import com.example.vbt.algorithm.detection.realtime.StreamingRepDetectionStrategy;
import com.example.vbt.algorithm.detection.realtime.StreamingStrategyFactory;
import com.example.vbt.algorithm.model.ImuSample;
import com.example.vbt.algorithm.model.Lift;
import com.example.vbt.algorithm.model.QuietWindow;
import com.example.vbt.algorithm.model.Rep;
import com.example.vbt.algorithm.model.SetResult;
import com.example.vbt.ble.BleSensorClient;
import com.example.vbt.ble.SamplePacketParser;
import com.example.vbt.recording.CsvSampleLogger;

public final class TrackerSession implements BleSensorClient.Listener, RepListener {

    public interface Listener {
        void onConnectionChanged(BleSensorClient.State state, String detail);

        void onStatusChanged(RepListener.Status status);

        void onRep(Rep rep, double velocityLossPercent);

        void onEpisodeSkipped(String reason);

        void onLogFile(String path);
    }

    private final Context context;
    private final Listener ui;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final BleSensorClient client;

    private final SamplePacketParser parser = new SamplePacketParser();
    private final List<Rep> setReps = new ArrayList<>();
    private StreamingRepDetectionStrategy strategy;
    private CsvSampleLogger logger;
    private final SamplePacketParser.Sink sink = new SamplePacketParser.Sink() {
        @Override
        public void onSample(ImuSample s) {
            if (logger != null) {
                try {
                    logger.log(s);
                } catch (IOException e) {
                    closeLogger();
                }
            }
            strategy.onSample(s);
        }
    };

    public TrackerSession(Context context, Listener ui, Lift initialLift) {
        this.context = context.getApplicationContext();
        this.ui = ui;
        this.client = new BleSensorClient(context, this);
        this.strategy = createStrategy(initialLift);
    }

    public void connect() {
        client.connect();
    }

    public void disconnect() {
        client.disconnect();
    }

    public BleSensorClient.State connectionState() {
        return client.getState();
    }

    public void selectLift(final Lift lift) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                strategy = createStrategy(lift);
                setReps.clear();
            }
        });
    }

    public void newSet() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                setReps.clear();
                strategy.newSet();
            }
        });
    }

    public void close() {
        client.disconnect();
        worker.execute(new Runnable() {
            @Override
            public void run() {
                closeLogger();
            }
        });
        worker.shutdown();
    }


    @Override
    public void onStateChange(final BleSensorClient.State state, final String detail) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (state == BleSensorClient.State.CONNECTED) {
                    parser.reset();
                    strategy.reset();
                    setReps.clear();
                    openLogger();
                } else if (state == BleSensorClient.State.DISCONNECTED) {
                    closeLogger();
                }
            }
        });
        ui.onConnectionChanged(state, detail);
    }

    @Override
    public void onData(final byte[] packet) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                parser.parse(packet, sink);
            }
        });
    }


    @Override
    public void onStatusChanged(final RepListener.Status status) {
        main.post(new Runnable() {
            @Override
            public void run() {
                ui.onStatusChanged(status);
            }
        });
    }

    @Override
    public void onRep(final Rep rep) {
        setReps.add(rep);
        final double loss = new SetResult(strategy.lift(), setReps, new ArrayList<QuietWindow>(), 0)
                .velocityLossPercent();
        main.post(new Runnable() {
            @Override
            public void run() {
                ui.onRep(rep, loss);
            }
        });
    }

    @Override
    public void onEpisodeSkipped(final String reason) {
        main.post(new Runnable() {
            @Override
            public void run() {
                ui.onEpisodeSkipped(reason);
            }
        });
    }


    private StreamingRepDetectionStrategy createStrategy(Lift lift) {
        StreamingRepDetectionStrategy s = StreamingStrategyFactory.forLift(lift);
        s.setListener(this);
        return s;
    }

    private void openLogger() {
        closeLogger();
        File dir = context.getExternalFilesDir("raw");
        if (dir == null) {
            dir = new File(context.getFilesDir(), "raw");
        }
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }
        String name = "stream_" + new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(new Date()) + ".csv";
        try {
            logger = new CsvSampleLogger(new File(dir, name));
            final String path = logger.getFile().getAbsolutePath();
            main.post(new Runnable() {
                @Override
                public void run() {
                    ui.onLogFile(path);
                }
            });
        } catch (IOException e) {
            logger = null;
        }
    }

    private void closeLogger() {
        if (logger != null) {
            try {
                logger.close();
            } catch (IOException ignored) {
                // nothing sensible to do
            }
            logger = null;
        }
    }
}