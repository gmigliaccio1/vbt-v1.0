package com.example.vbt.algorithm.detection.continuous;

import com.example.vbt.algorithm.config.LiftParameters;
import com.example.vbt.algorithm.model.Rep;

import java.util.List;

public final class SumoDeadliftAnalyzer implements LiftAnalyzer {
    @Override
    public List<Rep> analyze(AnalysisSignal s, LiftParameters p, double afterPeakT){
        return FloorPullAnalyzer.analyze(s.t, s.net, s.gyro, s.n, p, afterPeakT);
    }
}
