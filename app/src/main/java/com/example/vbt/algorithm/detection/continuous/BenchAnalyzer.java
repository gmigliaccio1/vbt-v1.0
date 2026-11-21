package com.example.vbt.algorithm.detection.continuous;

import com.example.vbt.algorithm.config.LiftParameters;
import com.example.vbt.algorithm.model.Rep;

import java.util.ArrayList;
import java.util.List;

public final class BenchAnalyzer implements LiftAnalyzer{

    static final double MAX_PRESS_S = 3.0;
    @Override
    public List<Rep> analyze(AnalysisSignal s, LiftParameters p, double afterPeakT){
        List<Rep> reps = new ArrayList<>();
        for(Rep r : ContinuousRepAnalyzer.analyze(s.t, s.net, s.gyro, s.n, p, afterPeakT)){
            if(r.durationS() <= MAX_PRESS_S){
                reps.add(r);
            }
        }
        return reps;
    }
}
