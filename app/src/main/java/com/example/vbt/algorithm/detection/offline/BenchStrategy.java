package com.example.vbt.algorithm.detection.offline;

import com.example.vbt.algorithm.config.LiftProfiles;
import com.example.vbt.algorithm.model.Lift;

public final class BenchStrategy extends AbstractRepDetectionStrategy{
    public BenchStrategy(){
        super(Lift.BENCH, LiftProfiles.BENCH);
    }
}
