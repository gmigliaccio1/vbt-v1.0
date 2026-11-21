package com.example.vbt.algorithm.detection.continuous;

public final class AnalysisSignal {
    public final double[] t;
    public final double[] net;
    public final double[] gyro;
    public final double[] mag;
    public final int n;

    public AnalysisSignal(double[] t, double[] net, double[] mag, double[] gyro, int n){
        this.t = t;
        this.net = net;
        this.mag = mag;
        this.gyro = gyro;
        this.n = n;
    }
}
