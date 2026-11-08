package com.example.vbt.algorithm.detection.realtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import com.example.vbt.algorithm.detection.offline.BenchStrategy;
import com.example.vbt.algorithm.model.ImuSample;
import com.example.vbt.algorithm.model.Lift;
import com.example.vbt.algorithm.model.Rep;
import com.example.vbt.algorithm.model.SetResult;
import com.example.vbt.algorithm.model.SignalData;

public class StreamingStrategyTest {
    private static final double G = 9.80665;

    /** Resting for restS, then 5 reps (half-sine, peak 0.6 m/s, 0.9 s) every 5 s, still in between. */
    private static List<ImuSample> synthetic(double restS, double gapStartS, double gapLenS) {
        List<ImuSample> out = new ArrayList<>();
        int fs = 200;
        double th = Math.toRadians(20);
        for (int i = 0; i < 40 * fs; i++) {
            double t = i / (double) fs;
            if (gapLenS > 0 && t >= gapStartS && t < gapStartS + gapLenS) {
                continue;
            }
            double a = 0;
            double gx = 4.6;
            for (int r = 0; r < 5; r++) {
                double s = restS + 3 + 5 * r;
                if (t >= s && t < s + 0.9) {
                    a = 0.6 * Math.PI / 0.9 * Math.cos(Math.PI * (t - s) / 0.9) / G;
                    gx += 15 * Math.sin(Math.PI * (t - s) / 0.9);
                }
            }
            double sc = 0.948;
            out.add(new ImuSample(t, Math.sin(th) * (1 + a) * sc, 0, Math.cos(th) * (1 + a) * sc, gx, -14.4, 24.0));
        }
        return out;
    }

    private static List<Rep> run(StreamingRepDetectionStrategy s, List<ImuSample> samples, final List<String> skipped) {
        final List<Rep> reps = new ArrayList<>();
        s.setListener(new RepListener() {
            @Override
            public void onStatusChanged(Status status) {
            }

            @Override
            public void onRep(Rep rep) {
                reps.add(rep);
            }

            @Override
            public void onEpisodeSkipped(String reason) {
                skipped.add(reason);
            }
        });
        for (ImuSample x : samples) {
            s.onSample(x);
        }
        return reps;
    }

    @Test
    public void benchStreamMatchesBatchResult() {
        List<ImuSample> samples = synthetic(2.0, 0, 0);
        List<Rep> live = run(new BenchStreamingStrategy(), samples, new ArrayList<String>());

        double[] t = new double[samples.size()];
        double[][] acc = new double[t.length][3];
        double[][] gyro = new double[t.length][3];
        for (int i = 0; i < t.length; i++) {
            ImuSample x = samples.get(i);
            t[i] = x.t;
            acc[i] = new double[]{x.ax, x.ay, x.az};
            gyro[i] = new double[]{x.gx, x.gy, x.gz};
        }
        SetResult batch = new BenchStrategy().analyze(new SignalData(t, acc, gyro));

        assertEquals(5, live.size());
        assertEquals(batch.getReps().size(), live.size());
        for (int i = 0; i < live.size(); i++) {
            assertEquals(i + 1, live.get(i).number);
            assertEquals(batch.getReps().get(i).meanVelocity, live.get(i).meanVelocity, 0.01);
            assertEquals(batch.getReps().get(i).peakVelocity, live.get(i).peakVelocity, 0.01);
        }
    }

    @Test
    public void repArrivesRightAfterTheBarIsStill() {
        List<ImuSample> samples = synthetic(2.0, 0, 0);
        final double[] firstRepAt = {-1};
        final double[] now = {0};
        BenchStreamingStrategy s = new BenchStreamingStrategy();
        s.setListener(new RepListener() {
            @Override
            public void onStatusChanged(Status status) {
            }

            @Override
            public void onRep(Rep rep) {
                if (firstRepAt[0] < 0) {
                    firstRepAt[0] = now[0];
                }
            }

            @Override
            public void onEpisodeSkipped(String reason) {
            }
        });
        for (ImuSample x : samples) {
            now[0] = x.t;
            s.onSample(x);
        }
        // rep ends at ~5.9 s, still-window confirmed 0.8 s later
        assertEquals(6.7, firstRepAt[0], 0.2);
    }

    @Test
    public void missingSamplesDuringARepDiscardsIt() {
        List<String> skipped = new ArrayList<>();
        // 0.3 s hole inside the first rep (starts at 5.0 s)
        List<Rep> reps = run(new BenchStreamingStrategy(), synthetic(2.0, 5.3, 0.3), skipped);
        assertEquals(4, reps.size());
        assertTrue(skipped.contains("samples missing during the movement"));
    }

    @Test
    public void newSetRestartsNumberingWithoutRecalibrating() {
        BenchStreamingStrategy s = new BenchStreamingStrategy();
        run(s, synthetic(2.0, 0, 0), new ArrayList<String>());
        s.newSet();
        assertEquals(RepListener.Status.READY, s.status());
    }

    @Test
    public void factoryGivesOneStrategyPerLift() {
        assertEquals(Lift.SQUAT, StreamingStrategyFactory.forLift(Lift.SQUAT).lift());
        assertEquals(Lift.BENCH, StreamingStrategyFactory.forLift(Lift.BENCH).lift());
    }
}