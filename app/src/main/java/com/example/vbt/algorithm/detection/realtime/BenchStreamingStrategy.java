package com.example.vbt.algorithm.detection.realtime;

import com.example.vbt.algorithm.config.LiftProfiles;
import com.example.vbt.algorithm.model.Lift;

public final class BenchStreamingStrategy extends AbstractStreamingStrategy {
    public BenchStreamingStrategy() {
        super(Lift.BENCH, LiftProfiles.BENCH);
    }
}