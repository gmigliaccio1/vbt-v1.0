package com.example.vbt.algorithm.detection.offline;

import com.example.vbt.algorithm.config.LiftProfiles;
import com.example.vbt.algorithm.model.Lift;

public final class SquatStrategy extends AbstractRepDetectionStrategy {
    public SquatStrategy(){
        super(Lift.SQUAT, LiftProfiles.SQUAT);
    }
}
