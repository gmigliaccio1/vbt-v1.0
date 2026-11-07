package com.example.vbt.algorithm.detection.realtime;

import com.example.vbt.algorithm.config.LiftProfiles;
import com.example.vbt.algorithm.model.Lift;

public final class SquatStreamingStrategy extends AbstractStreamingStrategy{
    public SquatStreamingStrategy(){
        super(Lift.SQUAT, LiftProfiles.SQUAT);
    }
}
