package com.example.vbt.algorithm.detection.continuous;


import com.example.vbt.algorithm.config.LiftParameters;
import com.example.vbt.algorithm.model.Rep;

import java.util.List;

public final class SquatAnalyzer implements LiftAnalyzer {
    @Override
    public List<Rep> analyze(AnalysisSignal s, LiftParameters p, double afterPeakT){
        return ContinuousRepAnalyzer.analyze(s.t, s.net, s.gyro, s.n, p, afterPeakT);
    }
}
