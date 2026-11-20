package com.example.vbt.algorithm.detection.realtime;

import com.example.vbt.algorithm.detection.continuous.ContinuousStreamingStrategy;
import com.example.vbt.algorithm.model.Lift;

public final class StreamingStrategyFactory {
    private StreamingStrategyFactory(){

    }

    public static StreamingRepDetectionStrategy forLift(Lift lift){
        return new ContinuousStreamingStrategy(lift);
    }
}
