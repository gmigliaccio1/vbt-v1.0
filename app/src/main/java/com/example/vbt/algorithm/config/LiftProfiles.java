package com.example.vbt.algorithm.config;

import com.example.vbt.algorithm.model.Lift;

public final class LiftProfiles {
    public static final LiftParameters BENCH = LiftParameters.builder()
            .minRepDurationS(0.4)
            .build();

    public static final LiftParameters SQUAT = LiftParameters.builder()
            .minRepDurationS(1.5)
            .build();

    public static final LiftParameters BENCH_CONTINUOUS = LiftParameters.builder()
            .minPeakHeight(0.10).minRepDistanceS(1.0).minRepDurationS(0.3).rom(0.12,0.40)
            .repEndFraction(0.15).cycleOffsetMaxS(6.0).build();

    public static final LiftParameters SQUAT_CONTINUOUS = LiftParameters.builder()
            .minPeakHeight(0.30).minRepDistanceS(1.5).minRepDurationS(0.6).rom(0.35, 0.80)
            .repEndFraction(0.30).build();

    public static final LiftParameters DEADLIFT_CONTINUOUS = LiftParameters.builder()
            .minPeakHeight(0.20).minRepDistanceS(1.5).minRepDurationS(0.6).rom(0.30,0.80)
            .repEndFraction(0.30).build();
    private LiftProfiles() {
    }

    public static LiftParameters continuousForLift(Lift lift) {
        switch (lift) {
            case SQUAT:
                return SQUAT_CONTINUOUS;
            case BENCH:
                return BENCH_CONTINUOUS;
            case DEADLIFT:
                return DEADLIFT_CONTINUOUS;
            default:
                throw new IllegalArgumentException("No continuous profile for " + lift);
        }
    }

    public static LiftParameters forLift(Lift lift){
        switch(lift){
            case SQUAT:
                return SQUAT;
            case BENCH:
                return BENCH;
            default:
                throw new IllegalArgumentException("No profile for " + lift);
        }
    }
}