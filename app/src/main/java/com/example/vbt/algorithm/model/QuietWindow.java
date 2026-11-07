package com.example.vbt.algorithm.model;

public final class QuietWindow {
    public final double startS;
    public final double endS;

    public QuietWindow(double startS, double endS){
        this.startS = startS;
        this.endS = endS;
    }

    public double durationS(){
        return endS - startS;
    }
}
