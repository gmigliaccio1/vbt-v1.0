package com.example.vbt.algorithm.model;
import java.util.Arrays;
public final class SignalData {
    public final double[] t;
    public final double[][] acc;
    public final double [][] gyro;

    public SignalData(double[] t, double[][] acc, double[][]gyro ){
        if(acc.length != t.length || gyro.length != t.length){
            throw new IllegalArgumentException("t, acc, gyro must have the same length");
        }
        this.t = t;
        this.acc = acc;
        this.gyro = gyro;
    }
    public int size(){
        return t.length;
    }

    public double medianDt(){
        if(t.length < 2) {
            return 0.005;
        }
        double[] d = new double[t.length - 1];
        for(int i = 1; i < t.length; i++){
            d[i - 1] = t[i] - t[i - 1];
        }
        Arrays.sort(d);
        int n = d.length;
        if(n % 2 == 1){
            return d[n / 2];
        }else{
            return 0.5 * (d[n / 2 - 1] + d[n / 2]);
        }
    }

    public double[] accMagnitude(){
        return magnitude(acc);
    }

    public double[] gyroMagnitude(){
        return magnitude(gyro);
    }

    private static double[] magnitude(double [][] v){
        double[] m = new double[v.length];
        for(int i = 0; i < v.length; i++){
            m[i] = Math.sqrt(v[i][0] * v[i][0] + v[i][1] * v[i][1] + v[i][2] * v[i][2]);
        }
        return m;
    }
}
