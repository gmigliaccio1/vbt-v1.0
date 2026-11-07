package com.example.vbt.algorithm.config;

import com.example.vbt.algorithm.model.Lift;

public final class LiftProfiles {
    public static final LiftParameters BENCH = LiftParameters.builder()
            .minRepDurationS(0.4)
            .build();

    public static final LiftParameters SQUAT = LiftParameters.builder()
            .minRepDurationS(1.5)
            .build();

    private LiftProfiles() {
    }

    public static LiftParameters forLift(Lift lift) {
        switch (lift) {
            case SQUAT:
                return SQUAT;
            case BENCH:
                return BENCH;
            default:
                throw new IllegalArgumentException("No profile for " + lift);
        }
    }
}