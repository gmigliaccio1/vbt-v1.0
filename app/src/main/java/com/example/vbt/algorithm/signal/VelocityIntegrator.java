package com.example.vbt.algorithm.signal;

import java.util.List;

import com.example.vbt.algorithm.model.QuietWindow;

public class VelocityIntegrator {

    private VelocityIntegrator(){
    }

    public static double[] naiveZupt(double[] net, double[] t, boolean[] quietMask){
        double[] dt = OrientationEstimator.clippedDt(t);
        double[] v = new double[net.length];
        for(int i = 1; i < net.length; i++){
            if(quietMask[i]){
                v[i] = 0.0;
            }else{
                v[i] = v[i - 1] + net[i] * dt[i];
            }
        }
        return v;
    }

    public static final class Segment{
        public final int startIdx;
        public final double[] vel;

        Segment(int startIdx, double[] vel){
            this.startIdx = startIdx;
            this.vel = vel;
        }
    }

    public static Segment anchored(double[] net, double[] t, List<QuietWindow> quiet, int peakIdx){
        int a = -1;
        int b = -1;
        for(QuietWindow w : quiet){
            int s = lowerBound(t, w.startS);
            int e = lowerBound(t, w.endS);
            if(e <= peakIdx){
                a = e;
            }
            if(b < 0 && s > peakIdx){
                b = s;
            }
        }
        if(a < 0 || b < 0){
            return null;
        }
        return new Segment(a, anchoredSegment(net, t, a, b));
    }

    public static double[] anchoredSegment(double[] net, double[] t, int a, int b){
        int len = b - a + 1;
        double[] v = new double[len];
        double acc = 0.0;
        for(int k = 0; k < len; k++){
            int idx = a + k;
            double dt;
            if(k == 0){
                dt = 0.0;
            }else{
                double diff = t[idx] - t[idx - 1];
                if(diff < 0.0){
                    dt = 0.0;
                } else if (diff > 0.02) {
                    dt = 0.02;
                }else{
                    dt = diff;
                }
            }
            acc += net[idx] * dt;
            v[k] = acc;
        }
        double first = v[0];
        for(int k = 0; k < len; k++){
            v[k] -= first;
        }
        double last = v[len - 1];
        for(int k = 0; k < len; k++){
            double frac;
            if(len == 1){
                frac = 0.0;
            }else{
                frac = (double) k / (len - 1);
            }
            v[k] -= last * frac;
        }
        return v;
    }

    static int lowerBound(double[] t, double value){
        int low = 0;
        int high = t.length;
        while(low < high){
            int mid = (low + high) >>> 1;
            if(t[mid] < value){
                low = mid + 1;
            }else{
                high = mid;
            }
        }
        return low;
    }
}
