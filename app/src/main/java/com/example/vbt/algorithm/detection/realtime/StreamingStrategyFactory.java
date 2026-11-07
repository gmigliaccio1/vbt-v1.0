package com.example.vbt.algorithm.detection.realtime;

import com.example.vbt.algorithm.model.Lift;

public final class StreamingStrategyFactory {
    private StreamingStrategyFactory(){

    }
    public static StreamingRepDetectionStrategy forLift(Lift lift){
        switch (lift){
            case SQUAT:
                return new SquatStreamingStrategy();
            case BENCH:
                return new BenchStreamingStrategy();
            default:
                throw new IllegalArgumentException("No streaming strategy for " + lift);
        }
    }
}
