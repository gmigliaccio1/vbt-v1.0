package com.example.vbt.algorithm.detection;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import com.example.vbt.algorithm.config.LiftParameters;
import com.example.vbt.algorithm.model.Rep;
import com.example.vbt.algorithm.signal.PeakFinder;
import com.example.vbt.algorithm.signal.VelocityIntegrator;

public class EpisodeAnalyzer {

    private EpisodeAnalyzer(){
    }

    public static List<Rep> analyze(double[] t, double[] net, double[] gyroMag, LiftParameters p){
        List<Rep> reps = new ArrayList<>();
        int n = t.length;
        if (n < 3){
            return reps;
        }
        double[] v = VelocityIntegrator.anchoredSegment(net, t, 0, n - 1);
        double[] dts = new double[n - 1];
        for(int i = 1; i < n; i++){
            dts[i - 1] = t[i] - t[i - 1];
        }
        Arrays.sort(dts);
        double medianDt = dts[dts.length / 2];
        int distance = Math.max(1, (int) (p.minRepDistanceS / medianDt));
        int lastEnd = -1;
        for(int peak : PeakFinder.find(v, p.minPeakHeight, distance)){
            int[] window = concentricWindow(v, peak);
            if(window[0] <= lastEnd){
                continue;
            }
            lastEnd = window[1];
            Rep rep = summarize(t, v, gyroMag, window[0], window[1], reps.size() + 1);
            if(rep.durationS() >= p.minRepDurationS){
                reps.add(rep);
            }
        }
        return reps;
    }

    public static int[] concentricWindow(double[] v, int peak){
        int i = peak;
        while(i > 0 && v[i] > 0){
            i--;
        }
        int j = peak;
        while(j < v.length - 1 && v[j] > 0){
            j++;
        }
        return new int[]{i, j};
    }

    public static Rep summarize(double[] t, double[] v, double[] gyroMag, int s, int e, int number){
        double sum = 0.0;
        int count = 0;
        double peak = Double.NEGATIVE_INFINITY;
        double gyroSum = 0.0;
        double rom = 0.0;
        for(int k = s; k<= e; k++){
            if(k > s){
                rom += 0.5 * (v[k] + v[k - 1]) * (t[k]  - t[k - 1]);
            }
            if(v[k] > 0){
                sum += v[k];
                count++;
            }
            peak = Math.max(peak, v[k]);
            gyroSum += gyroMag[k];
        }
        double mean;
        if(count > 0){
            mean = sum / count;
        }else{
            mean = 0.0;
        }
        return new Rep(number, t[s], t[e], mean, peak, gyroSum / (e - s + 1), rom);
    }
}
