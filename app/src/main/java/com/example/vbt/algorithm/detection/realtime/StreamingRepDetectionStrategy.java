package com.example.vbt.algorithm.detection.realtime;

import com.example.vbt.algorithm.model.ImuSample;
import com.example.vbt.algorithm.model.Lift;

public interface StreamingRepDetectionStrategy {
    Lift lift();
    void setListener(RepListener listener);
    void onSample(ImuSample sample);
    void newSet();
    void reset();
    RepListener.Status status();
}
