package com.example.vbt.algorithm.detection.continuous;

import com.example.vbt.algorithm.config.LiftParameters;
import com.example.vbt.algorithm.model.Rep;

import java.util.List;

public interface LiftAnalyzer {
    List<Rep> analyze(AnalysisSignal signal, LiftParameters p, double afterPeakT);
}
