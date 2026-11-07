package com.example.vbt.algorithm.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class SetResult {
    private final Lift lift;
    private final List<Rep> reps;
    private final List<QuietWindow> quietWindows;
    private final int skippedPeaks;

    public SetResult(Lift lift, List<Rep> reps, List<QuietWindow> quietWindows, int skippedPeaks){
        this.lift = lift;
        this.reps = Collections.unmodifiableList(new ArrayList<>(reps));
        this.quietWindows = Collections.unmodifiableList(new ArrayList<>(quietWindows));
        this.skippedPeaks = skippedPeaks;
    }

    public Lift getLift(){
        return lift;
    }

    public List<Rep> getReps(){
        return reps;
    }

    public List<QuietWindow> getQuietWindows(){
        return quietWindows;
    }

    public int getSkippedPeaks(){
        return skippedPeaks;
    }

    public double velocityLossPercent(){
        if(reps.size() < 2) {
            return 0.0;
        }
        double best = 0.0;
        for(Rep r : reps){
            best = Math.max(best, r.meanVelocity);
        }
        if(best <= 0.0){
            return 0.0;
        }
        double last = reps.get(reps.size() - 1).meanVelocity;
        return Math.max(0.0, (best - last) / best * 100.0);
    }
}
