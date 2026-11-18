package com.example.vbt.algorithm.history;

import com.example.vbt.algorithm.model.Lift;
import com.example.vbt.algorithm.model.Rep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SavedSet {
    public final long timestampMs;
    public final Lift lift;
    public final double loadKg;
    public final List<Rep> reps;

    public SavedSet(long timestampMs, Lift lift, double loadKg, List<Rep> reps){
        this.timestampMs = timestampMs;
        this.lift = lift;
        this.loadKg = loadKg;
        this.reps = Collections.unmodifiableList(new ArrayList<>(reps));
    }
}
