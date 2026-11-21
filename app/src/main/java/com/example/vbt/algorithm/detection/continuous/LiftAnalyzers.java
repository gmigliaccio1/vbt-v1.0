package com.example.vbt.algorithm.detection.continuous;

import com.example.vbt.algorithm.model.Lift;

public final class LiftAnalyzers {
    private LiftAnalyzers(){

    }

    public static LiftAnalyzer forLift(Lift lift){
        switch (lift){
            case SQUAT:
                return new SquatAnalyzer();
            case BENCH:
                return new BenchAnalyzer();
            case DEADLIFT:
                return new SumoDeadliftAnalyzer();
            default:
                throw new IllegalArgumentException("No analyzer for " + lift);
        }
    }
}
