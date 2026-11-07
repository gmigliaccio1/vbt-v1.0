package com.example.vbt.algorithm.detection.offline;

import com.example.vbt.algorithm.model.Lift;
import com.example.vbt.algorithm.model.SetResult;
import com.example.vbt.algorithm.model.SignalData;

public class SetAnalyzer {
    private RepDetectionStrategy strategy;
    public SetAnalyzer(Lift lift){
        this.strategy = RepDetectionStrategyFactory.forLift(lift);
    }
    public void selectLift(Lift lift){
        this.strategy = RepDetectionStrategyFactory.forLift(lift);
    }
    public Lift currentLift(){
        return strategy.lift();
    }
    public SetResult analyze(SignalData recording){
        return strategy.analyze(recording);
    }
}
