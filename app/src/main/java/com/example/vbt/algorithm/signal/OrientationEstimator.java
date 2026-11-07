package com.example.vbt.algorithm.signal;

import com.example.vbt.algorithm.model.SignalData;

public final class OrientationEstimator {
    public static final double G = GravityTracker.G;
    private static final double MAX_DT = 0.02;

    private OrientationEstimator(){
    }

    public static double[] verticalAcceleration(SignalData d, double alpha){
        int n = d.size();
        double[] net = new double[n];
        double[] dt = clippedDt(d.t);
        GravityTracker tracker = new GravityTracker(alpha);
        for(int i = 0; i < n; i++){
            net[i] = tracker.update(d.acc[i], d.gyro[i], dt[i]);
        }
        return net;
    }

    public static double[] clippedDt(double[] t){
        double[] dt = new double[t.length];
        for(int i = 1; i < t.length; i++){
            dt[i] = Math.min(Math.max(t[i] - t[i-1], 0.0), MAX_DT);
        }
        return dt;
    }
}
