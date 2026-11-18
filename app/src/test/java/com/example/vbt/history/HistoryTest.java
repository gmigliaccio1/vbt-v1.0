package com.example.vbt.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.example.vbt.algorithm.history.SavedSet;
import com.example.vbt.algorithm.history.SetFormat;
import com.example.vbt.algorithm.history.SetHistoryStore;
import com.example.vbt.algorithm.history.SetStats;
import com.example.vbt.algorithm.model.Lift;
import com.example.vbt.algorithm.model.Rep;

public class HistoryTest {

    private static Rep rep(int n, double mean) {
        return new Rep(n, n * 3.0, n * 3.0 + 1.0, mean, mean * 1.4, 5.0, 0.26);
    }

    private static List<Rep> reps(double... means) {
        List<Rep> out = new ArrayList<>();
        for (int i = 0; i < means.length; i++) {
            out.add(rep(i + 1, means[i]));
        }
        return out;
    }

    @Test
    public void lossIsMeasuredAgainstTheFastestRepSoFar() {
        double[] loss = SetStats.lossPerRep(reps(0.50, 0.55, 0.44, 0.60));
        assertEquals(0.0, loss[0], 1e-9);
        assertEquals(0.0, loss[1], 1e-9);
        assertEquals(20.0, loss[2], 1e-9);
        assertEquals(0.0, loss[3], 1e-9);
        assertEquals(0.60, SetStats.bestMean(reps(0.50, 0.55, 0.44, 0.60)), 1e-9);
        assertEquals(20.0, SetStats.lastLossPercent(reps(0.50, 0.55, 0.44)), 1e-9);
        assertEquals(0.0, SetStats.lastLossPercent(new ArrayList<Rep>()), 1e-9);
    }

    @Test
    public void savedSetsComeBackIdenticalAndInOrder() throws IOException {
        File f = File.createTempFile("history", ".txt");
        f.deleteOnExit();
        SetHistoryStore store = new SetHistoryStore(f);
        assertTrue(store.loadAll().isEmpty());

        store.append(new SavedSet(1000L, Lift.BENCH, 130.0, reps(0.285, 0.226, 0.205)));
        store.append(new SavedSet(2000L, Lift.SQUAT, 192.5, reps(0.50, 0.48)));

        List<SavedSet> all = store.loadAll();
        assertEquals(2, all.size());
        assertEquals(Lift.BENCH, all.get(0).lift);
        assertEquals(130.0, all.get(0).loadKg, 1e-9);
        assertEquals(3, all.get(0).reps.size());
        assertEquals(0.226, all.get(0).reps.get(1).meanVelocity, 1e-4);
        assertEquals(0.26, all.get(0).reps.get(1).romM, 1e-4);
        assertEquals(2000L, all.get(1).timestampMs);
        assertEquals(192.5, all.get(1).loadKg, 1e-9);
        assertEquals(2, all.get(1).reps.size());
    }

    @Test
    public void deletingOneSetKeepsTheOthers() throws IOException {
        File f = File.createTempFile("history", ".txt");
        f.deleteOnExit();
        SetHistoryStore store = new SetHistoryStore(f);
        store.append(new SavedSet(1000L, Lift.BENCH, 100, reps(0.3)));
        store.append(new SavedSet(2000L, Lift.DEADLIFT, 200, reps(0.4, 0.38)));
        store.delete(1000L);
        List<SavedSet> all = store.loadAll();
        assertEquals(1, all.size());
        assertEquals(Lift.DEADLIFT, all.get(0).lift);
        assertEquals(2, all.get(0).reps.size());
    }

    @Test
    public void damagedLinesAreSkippedNotFatal() throws IOException {
        File f = File.createTempFile("history", ".txt");
        f.deleteOnExit();
        try (FileWriter w = new FileWriter(f)) {
            w.write("garbage\n");
            w.write("S|1000|BENCH|100.00\n");
            w.write("R|1|0|1|0.3|0.4|5|0.25\n");
            w.write("R|x|0|1|0.3|0.4|5|0.25\n");
            w.write("S|2000|NOT_A_LIFT|50\n");
            w.write("R|1|0|1|0.3|0.4|5|0.25\n");
        }
        List<SavedSet> all = new SetHistoryStore(f).loadAll();
        assertEquals(1, all.size());
        assertEquals(1, all.get(0).reps.size());
    }

    @Test
    public void textForTheHistoryScreen() {
        SavedSet set = new SavedSet(1000L, Lift.BENCH, 130.0, reps(0.50, 0.40));
        assertEquals("10 Nov 10:15 - Bench Press - 130 kg - 2 reps - best 0.50 m/s - loss 20%",
                SetFormat.summaryLine(set, "10 Nov 10:15"));
        String details = SetFormat.details(set);
        assertTrue(details.contains("Rep 2   mean 0.40   peak 0.56 m/s   ROM 26 cm   loss 20%"));
        assertEquals("192.5 kg", SetFormat.summaryLine(
                new SavedSet(1L, Lift.SQUAT, 192.5, reps(0.5)), "x").split(" - ")[2]);
    }
}