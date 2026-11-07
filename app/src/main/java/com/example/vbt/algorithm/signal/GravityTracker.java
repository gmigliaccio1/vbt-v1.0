package com.example.vbt.algorithm.signal;

public final class GravityTracker {
    public static final double G = 9.80665;
    private final double alpha;
    private double[] g;

    public GravityTracker(double alpha){
        this.alpha = alpha;
    }

    public boolean isInitialized(){
        return g != null;
    }

    public double update(double [] acc, double [] gyro, double dt){
        if(g == null){
            g = normalized(acc);
            return (dot(acc, g) - 1.0) * G;
        }
        double wx = Math.toRadians(gyro[0]);
        double wy = Math.toRadians(gyro[1]);
        double wz = Math.toRadians(gyro[2]);
        double cx = wy * g[2] - wz * g[1];
        double cy = wz * g[0] - wx * g[2];
        double cz = wx * g[1] - wy * g[0];
        double px = g[0] - dt * cx;
        double py = g[1] - dt * cy;
        double pz = g[2] - dt * cz;

        double accNorm = norm(acc);
        double gyroMag = norm(gyro);
        if(accNorm < 1e-9){
            g = normalized(new double[]{px, py, pz});
        }else{
            double a = (Math.abs(accNorm - 1.0) > 0.08 || gyroMag > 10.0) ? 1.0 : alpha;
            g = normalized(new double[]{
                    a * px + (1 - a) * acc[0] / accNorm,
                    a * py + (1 - a) * acc[1] / accNorm,
                    a * pz + (1 - a) * acc[2] / accNorm
            });
        }
        return (dot(acc, g) - 1.0) * G;
    }

    public static double norm (double[] v){
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    public static double dot(double[] a, double[] b){
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    public static double[] normalized(double[] v){
        double n = norm(v);
        if(n < 1e-12){
            return new double[]{0, 0, 1};
        }
        return new double[]{v[0] / n, v[1] / n, v[2] / n};
    }
}