package com.example.vbt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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

import com.example.vbt.algorithm.detection.realtime.RepListener;
import com.example.vbt.algorithm.history.HistoryStorage;
import com.example.vbt.algorithm.history.SavedSet;
import com.example.vbt.algorithm.history.SetHistoryStore;
import com.example.vbt.algorithm.history.SetStats;
import com.example.vbt.algorithm.model.Lift;
import com.example.vbt.algorithm.model.Rep;
import com.example.vbt.ble.BlePermissions;
import com.example.vbt.ble.BleSensorClient;
import com.example.vbt.session.TrackerSession;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements TrackerSession.Listener {

    private static final String PREFS = "vbt";
    private static final String PREF_LIFT = "lift";
    private static final String PREF_LOAD = "load";
    private static final String PREF_ALERT = "alert";
    private static final int DEFAULT_ALERT_PERCENT = 20;
    private TrackerSession session;
    private SetHistoryStore history;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Rep> setReps = new ArrayList<>();
    private Lift currentLift = Lift.BENCH;
    private Button btnConnect;
    private TextView tvConnection, tvStatus, tvBattery, tvLogFile;
    private TextView tvLastRep, tvLastUnit, tvLastDetail, tvLoss;
    private MaterialCardView cardLast;
    private LinearLayout repsContainer;
    private RadioGroup liftGroup;
    private TextInputEditText etLoad, etAlert;
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
            v.setPadding(systemBars.left + dp(16), systemBars.top + dp(16),
                    systemBars.right + dp(16), systemBars.bottom + dp(16));
            return insets;
        });

        btnConnect = findViewById(R.id.btnConnect);
        tvConnection = findViewById(R.id.tvConnection);
        tvStatus = findViewById(R.id.tvStatus);
        tvBattery = findViewById(R.id.tvBattery);
        tvLogFile = findViewById(R.id.tvLogFile);
        tvLastRep = findViewById(R.id.tvLastRep);
        tvLastUnit = findViewById(R.id.tvLastUnit);
        tvLastDetail = findViewById(R.id.tvLastDetail);
        tvLoss = findViewById(R.id.tvLoss);
        cardLast = findViewById(R.id.cardLast);
        repsContainer = findViewById(R.id.repsContainer);
        liftGroup = findViewById(R.id.liftGroup);
        etLoad = findViewById(R.id.etLoad);
        etAlert = findViewById(R.id.etAlert);

        history = HistoryStorage.open(this);
        restorePreferences();
        currentLift = selectedLift();
        session = new TrackerSession(this, this, currentLift);
        showAlertStyle(false);

        liftGroup.setOnCheckedChangeListener((group, checkedId) -> {
            saveSetIfAny(currentLift);
            clearSetDisplay();
            currentLift = selectedLift();
            session.selectLift(currentLift);
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
            saveSetIfAny(currentLift);
            clearSetDisplay();
            session.newSet();
            session.mark("set lift=" + currentLift.name() + " kg=" + loadKg());
        });

        findViewById(R.id.btnHistory).setOnClickListener(
                (View v) -> startActivity(new Intent(this, HistoryActivity.class)));
    }

    @Override
    protected void onPause(){
        super.onPause();
        savePreferences();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        session.close();
        io.shutdown();
    }

    private Lift selectedLift() {
        int id = liftGroup.getCheckedRadioButtonId();
        if (id == R.id.radioSquat) {
            return Lift.SQUAT;
        }
        if (id == R.id.radioDeadlift) {
            return Lift.DEADLIFT;
        }
        return Lift.BENCH;
    }

    private double loadKg(){
        String text = textOf(etLoad).replace(',','.');
        try {
            if (text.isBlank()) {
                return 0.0;
            } else {
                return Double.parseDouble(text);
            }
        }catch (NumberFormatException e){
            return 0.0;
        }
    }

    private int alertPercent(){
        String text = textOf(etAlert);
        try{
            if(text.isEmpty()){
                return 0;
            }else{
                return Integer.parseInt(text);
            }
        }catch (NumberFormatException e){
            return 0;
        }
    }

    private static String textOf(TextInputEditText field){
        if(field.getText() == null){
            return "";
        }else{
            return field.getText().toString().trim();
        }
    }

    private void restorePreferences(){
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        String lift = p.getString(PREF_LIFT, Lift.BENCH.name());
        if(lift.equals(Lift.SQUAT.name())){
            liftGroup.check(R.id.radioSquat);
        }else if(lift.equals(Lift.DEADLIFT.name())){
            liftGroup.check(R.id.radioDeadlift);
        }else{
            liftGroup.check(R.id.radioBench);
        }
        etLoad.setText(p.getString(PREF_LOAD, ""));
        etAlert.setText(String.valueOf(p.getInt(PREF_ALERT, DEFAULT_ALERT_PERCENT)));
    }

    private void savePreferences(){
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(PREF_LIFT, selectedLift().name())
                .putString(PREF_LOAD, textOf(etLoad))
                .putInt(PREF_ALERT, alertPercent())
                .apply();
    }

    private void saveSetIfAny(Lift lift){
        if(setReps.isEmpty()){
            return;
        }
        final SavedSet set = new SavedSet(System.currentTimeMillis(), lift, loadKg(), setReps);
        io.execute(()-> {
            try{
                history.append(set);
                handler.post(() -> Toast.makeText(this, R.string.set_saved,
                        Toast.LENGTH_SHORT).show());
            }catch (IOException e){
                handler.post(() -> Toast.makeText(this, R.string.set_not_saved,
                        Toast.LENGTH_LONG).show());
            }
        });
    }
    private void clearSetDisplay() {
        setReps.clear();
        repsContainer.removeAllViews();
        tvLastRep.setText(R.string.dash);
        tvLastDetail.setText(R.string.no_reps_yet);
        tvLoss.setText("");
        showAlertStyle(false);
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
    public void onBattery(int percent){
        String warning = "";
        if(percent <= 15){
            warning = " - charge it!";
        }
        tvBattery.setText(String.format(Locale.US, "Sensor battery %d%%%s", percent, warning));
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
        setReps.add(rep);
        double loss = SetStats.lastLossPercent(setReps);
        int limit = alertPercent();
        boolean stop = limit > 0 && loss >= limit;

        tvLastRep.setText(String.format(Locale.US, "%.2f", rep.meanVelocity));
        tvLastDetail.setText(String.format(Locale.US, "Rep %d - peak %.2f m/s - ROM %.0f cm",
                 rep.number, rep.peakVelocity, rep.romM * 100));
        String stopMessage = "";
        if(stop){
            stopMessage = " - STOP";
        }
        tvLoss.setText(String.format(Locale.US, "Velocity loss %.0f%%%s", loss, stopMessage));
        if(stop){
            alertFeedback();
        }
        addRepRow(rep, loss);
    }

    @Override
    public void onEpisodeSkipped(String reason) {
        tvStatus.setText(getString(R.string.status_ready) + " - " + reason);
    }

    @Override
    public void onLogFile(String path) {
        tvLogFile.setText(path);
    }

    private void addRepRow(Rep rep, double loss){
        View row = LayoutInflater.from(this).inflate(R.layout.item_rep, repsContainer, false);
        ((TextView) row.findViewById(R.id.tvRepNo)).setText(String.valueOf(rep.number));
        ((TextView) row.findViewById(R.id.tvRepMean)).setText(String.format(Locale.US, "%.2f",
                rep.meanVelocity));
        ((TextView) row.findViewById(R.id.tvRepPeak)).setText(String.format(Locale.US, "%.2f",
                rep.peakVelocity));
        ((TextView) row.findViewById(R.id.tvRepRom)).setText(String.format(Locale.US, "%.0f cm",
                rep.romM * 100));
        ((TextView) row.findViewById(R.id.tvRepLoss)).setText(String.format(Locale.US, "%.0f%%",
                loss));
        repsContainer.addView(row, 0);
    }

    private void showAlertStyle(boolean alert){
        int bg;
        int fg;
        if(alert){
            bg = MaterialColors.getColor(cardLast,
                    com.google.android.material.R.attr.colorErrorContainer);
            fg = MaterialColors.getColor(cardLast,
                    com.google.android.material.R.attr.colorOnErrorContainer);
        }else{
            bg = MaterialColors.getColor(cardLast,
                    com.google.android.material.R.attr.colorSurfaceVariant);
            fg = MaterialColors.getColor(cardLast,
                    com.google.android.material.R.attr.colorOnSurfaceVariant);
        }
        cardLast.setCardBackgroundColor(bg);
        tvLastRep.setTextColor(fg);
        tvLastUnit.setTextColor(fg);
        tvLastDetail.setTextColor(fg);
        tvLoss.setTextColor(fg);
    }

    @SuppressWarnings("deprecation")
    private void alertFeedback() {
        try {
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager manager = (VibratorManager) getSystemService(
                        Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = manager == null ? null : manager.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(400,
                            VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(400);
                }
            }
            final ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_ALARM, 80);
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 300);
            handler.postDelayed(tone::release, 700);
        } catch (RuntimeException ignored) {

        }
    }

    private int dp(int value){
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}