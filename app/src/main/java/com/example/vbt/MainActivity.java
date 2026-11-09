package com.example.vbt;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import java.util.Locale;
import java.util.Map;

import com.example.vbt.algorithm.detection.realtime.RepListener;
import com.example.vbt.algorithm.model.Lift;
import com.example.vbt.algorithm.model.Rep;
import com.example.vbt.ble.BlePermissions;
import com.example.vbt.ble.BleSensorClient;
import com.example.vbt.session.TrackerSession;

public class MainActivity extends AppCompatActivity implements TrackerSession.Listener {

    private TrackerSession session;
    private ArrayAdapter<String> repsAdapter;
    private double bestMean;
    private int repCount;

    private Button btnConnect;
    private TextView tvConnection, tvStatus, tvLastRep, tvSummary, tvLogFile;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    (Map<String, Boolean> result) -> {
                        if (BlePermissions.hasAll(this)) {
                            session.connect();
                        } else {
                            Toast.makeText(this, R.string.permission_denied,
                                    Toast.LENGTH_LONG).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main),
                (v,
                 insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnConnect = findViewById(R.id.btnConnect);
        tvConnection = findViewById(R.id.tvConnection);
        tvStatus = findViewById(R.id.tvStatus);
        tvLastRep = findViewById(R.id.tvLastRep);
        tvSummary = findViewById(R.id.tvSummary);
        tvLogFile = findViewById(R.id.tvLogFile);
        RadioGroup liftGroup = findViewById(R.id.liftGroup);
        ListView lvReps = findViewById(R.id.lvReps);

        repsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        lvReps.setAdapter(repsAdapter);

        session = new TrackerSession(this, this, selectedLift(liftGroup));

        liftGroup.setOnCheckedChangeListener((group, checkedId) -> {
            clearSetDisplay();
            session.selectLift(selectedLift(group));
        });

        btnConnect.setOnClickListener(v -> {
            if (session.connectionState() == BleSensorClient.State.DISCONNECTED) {
                if (BlePermissions.hasAll(this)) {
                    session.connect();
                } else {
                    permissionLauncher.launch(BlePermissions.required());
                }
            } else {
                session.disconnect();
            }
        });

        findViewById(R.id.btnNewSet).setOnClickListener((View v) -> {
            clearSetDisplay();
            session.newSet();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        session.close();
    }

    private Lift selectedLift(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == R.id.radioSquat) {
            return Lift.SQUAT;
        }
        if (id == R.id.radioDeadlift) {
            return Lift.DEADLIFT;
        }
        return Lift.BENCH;
    }

    private void clearSetDisplay() {
        repsAdapter.clear();
        bestMean = 0;
        repCount = 0;
        tvLastRep.setText(R.string.no_reps_yet);
        tvSummary.setText("");
    }


    @Override
    public void onConnectionChanged(BleSensorClient.State state, String detail) {
        tvConnection.setText(detail);
        switch (state) {
            case DISCONNECTED:
                btnConnect.setText(R.string.connect);
                tvStatus.setText("");
                break;
            case SCANNING:
            case CONNECTING:
                btnConnect.setText(R.string.cancel_scan);
                break;
            case CONNECTED:
                btnConnect.setText(R.string.disconnect);
                break;
        }
    }

    @Override
    public void onStatusChanged(RepListener.Status status) {
        switch (status) {
            case CALIBRATING:
                tvStatus.setText(R.string.status_calibrating);
                break;
            case READY:
                tvStatus.setText(R.string.status_ready);
                break;
            case MOVING:
                tvStatus.setText(R.string.status_moving);
                break;
        }
    }

    @Override
    public void onRep(Rep rep, double velocityLossPercent) {
        repCount = rep.number;
        bestMean = Math.max(bestMean, rep.meanVelocity);
        tvLastRep.setText(String.format(Locale.US, "%.2f m/s   ROM %.0f cm",
                rep.meanVelocity, rep.romM * 100));
        tvSummary.setText(String.format(Locale.US, "Rep %d   best %.2f   loss %.0f%%",
                repCount, bestMean, velocityLossPercent));
        repsAdapter.insert(String.format(Locale.US, "Rep %d   mean %.2f   peak %.2f m/s " +
                        "  ROM %.0f cm   %.1f s",
                rep.number, rep.meanVelocity, rep.peakVelocity, rep.romM * 100,
                rep.durationS()), 0);
    }

    @Override
    public void onEpisodeSkipped(String reason) {
        tvSummary.setText(getString(R.string.status_ready) + " - " + reason);
    }

    @Override
    public void onLogFile(String path) {
        tvLogFile.setText(path);
    }
}