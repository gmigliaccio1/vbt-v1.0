package com.example.vbt.algorithm.detection.offline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


import org.junit.Test;

import com.example.vbt.algorithm.model.Lift;
import com.example.vbt.algorithm.model.SetResult;
import com.example.vbt.algorithm.model.SignalData;

/** Synthetic set: sensor tilted 20 deg, 5 reps (half-sine velocity, peak 0.6 m/s, 0.9 s), still in between. */
public class StrategyTest {
    private static final double G = 9.80665;

    private static SignalData syntheticSet(double[] repStarts) {
        int fs = 200;
        int n = 40 * fs;
        double[] t = new double[n];
        double[][] acc = new double[n][3];
        double[][] gyro = new double[n][3];
        double th = Math.toRadians(20);
        for (int i = 0; i < n; i++) {
            t[i] = i / (double) fs;
            double a = 0;
            double gx = 4.6;
            for (double s : repStarts) {
                if (t[i] >= s && t[i] < s + 0.9) {
                    a = 0.6 * Math.PI / 0.9 * Math.cos(Math.PI * (t[i] - s) / 0.9) / G;
                    gx += 15 * Math.sin(Math.PI * (t[i] - s) / 0.9);
                }
            }
            acc[i][0] = Math.sin(th) * (1 + a) * 0.948;   // 0.948 = accelerometer scale error seen on real data
            acc[i][2] = Math.cos(th) * (1 + a) * 0.948;
            gyro[i][0] = gx;
            gyro[i][1] = -14.4;
            gyro[i][2] = 24.0;
        }
        return new SignalData(t, acc, gyro);
    }

    private static final double[] STARTS = {8, 13, 18, 23, 28};

    @Test
    public void benchFindsAllRepsWithExpectedVelocity() {
        SetResult r = new SetAnalyzer(Lift.BENCH).analyze(syntheticSet(STARTS));
        assertEquals(5, r.getReps().size());
        for (int i = 0; i < 5; i++) {
            assertEquals(0.6, r.getReps().get(i).peakVelocity, 0.03);
            assertEquals(0.9, r.getReps().get(i).durationS(), 0.05);
            assertEquals(i + 1, r.getReps().get(i).number);
        }
    }

    @Test
    public void strategiesAreSwappable() {
        SetAnalyzer analyzer = new SetAnalyzer(Lift.BENCH);
        assertEquals(Lift.BENCH, analyzer.currentLift());
        analyzer.selectLift(Lift.SQUAT);
        assertEquals(Lift.SQUAT, analyzer.currentLift());
        // squat strategy currently rejects reps shorter than 1.5 s, bench keeps them
        SetResult squat = analyzer.analyze(syntheticSet(STARTS));
        assertEquals(0, squat.getReps().size());
        assertTrue(squat.getSkippedPeaks() > 0);
    }

    @Test
    public void emptyRecordingGivesNoReps() {
        SignalData empty = new SignalData(new double[0], new double[0][3], new double[0][3]);
        assertEquals(0, new SetAnalyzer(Lift.BENCH).analyze(empty).getReps().size());
    }
}