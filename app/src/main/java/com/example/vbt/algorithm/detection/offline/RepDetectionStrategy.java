package com.example.vbt.algorithm.detection.offline;

import com.example.vbt.algorithm.model.Lift;
import com.example.vbt.algorithm.model.SetResult;
import com.example.vbt.algorithm.model.SignalData;

public interface RepDetectionStrategy {
    Lift lift();
    SetResult analyze(SignalData recording);
}
