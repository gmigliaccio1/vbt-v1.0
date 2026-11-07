package com.example.vbt.algorithm.detection.offline;

import com.example.vbt.algorithm.model.Lift;

public class RepDetectionStrategyFactory {
    private RepDetectionStrategyFactory(){
    }
    public static RepDetectionStrategy forLift(Lift lift){
        switch(lift){
            case SQUAT:
                return new SquatStrategy();
            case BENCH:
                return new BenchStrategy();
            default:
                throw new IllegalArgumentException("No strategy for" + lift);
        }
    }
}
