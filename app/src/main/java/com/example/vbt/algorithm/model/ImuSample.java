package com.example.vbt.algorithm.model;

public final class ImuSample {
    public final double t;
    public final double ax, ay, az;
    public final double gx, gy, gz;

    public ImuSample(double t, double ax, double ay, double az, double gx, double gy, double gz){
        this.t = t;
        this.ax = ax;
        this.ay = ay;
        this.az = az;
        this.gx = gx;
        this.gy = gy;
        this.gz = gz;
    }
}
