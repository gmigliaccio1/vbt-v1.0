package com.example.vbt.algorithm.detection.continuous;

import com.example.vbt.R;
import com.example.vbt.algorithm.config.LiftParameters;
import com.example.vbt.algorithm.model.Lift;
import com.example.vbt.algorithm.model.Rep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ContinuousRepAnalyzer {
    static final double MAX_DT = 0.06;
    static final double KNOT_WINDOW_S = 0.6;
    static final double KNOT_ACC_STD = 0.2;
    static final double KNOT_ACC_MEAN = 0.5;
    static final double KNOT_GYRO_MEAN = 25.0;
    static final double SMOOTH_S = 0.15;
    static final double PEAK_WINDOW_S = 6.0;
    static final double END_GUARD_S = 6.0;

    private ContinuousRepAnalyzer(){

    }

    public static List<Rep> analyze(double [] t, double[] net, double[] gyroMag, int n,
                                    LiftParameters p, double afterPeakT){
        List<Rep> reps = new ArrayList<>();
        if(n < 20){
            return reps;
        }
        double[] raw = new double[n];
        for(int i = 1; i < n; i++){
            double dt = Math.min(Math.max(t[i] - t[i - 1], 0.0), MAX_DT);
            raw[i] = raw[i - 1] + net[i] * dt;
        }
        double[] v0 = new double[n];
        List<double[]> knots = new ArrayList<>();
        double[] base = driftBaseLine(t, net, gyroMag, raw, n, knots);
        for(int i = 0; i < n; i++){
            v0[i] = raw[i] - base[i];
        }
        double fs = (n - 1) / (t[n - 1] - t[0]);
        double[] s = smooth(v0, Math.max(3, (int) Math.round(SMOOTH_S * fs)));
        int[] peaks = findPeaks(s, p.minPeakHeight, (int) Math.round(p.minRepDistanceS * fs),
                (int) Math.round(PEAK_WINDOW_S * fs));
        for(int k = 0; k < peaks.length; k++){
            int pk = peaks[k];
            if(t[pk] <= afterPeakT + 0.5){
                continue;
            }
            double level = 0.0;
            if(k > 0 && t[pk] - t[peaks[k - 1]] <= p.cycleOffsetMaxS){
                double sum = 0;
                for(int i = peaks[k - 1]; i < pk; i++){
                    sum += v0[i];
                }
                level = sum / (pk - peaks[k - 1]);
            }
            Rep rep = summarize(t, v0, level, gyroMag, n, pk, p);
            if(rep != null && t[n - 1] - rep.endS >= END_GUARD_S){
                reps.add(rep);
            }
        }
        List<Rep> numbered = new ArrayList<>();
        for(Rep r : reps){
            numbered.add(r.withNumber(numbered.size() + 1));
        }
        return numbered;
    }

    private static Rep summarize(double[] t, double[] v0, double level, double[] gyro, int n,
                                 int pk, LiftParameters p){
        double peakV = v0[pk] - level;
        if(peakV <= 0){
            return null;
        }
        double th = p.repEndFraction * peakV;
        int l = pk;
        while(l > 0 && v0[l] - level > th){
            l--;
        }
        int r = pk;
        while(r < n - 1 && v0[r] - level > th){
            r++;
        }
        if(r >= n - 1 || l == 0 || r <= l + 1){
            return null;
        }
        double rom = 0.0;
        double peak = Double.NEGATIVE_INFINITY;
        double gyroSum = 0.0;
        double gyroMax = 0.0;
        for(int i = l + 1; i <= r; i++){
            double v = v0[i] - level;
            rom += 0.5 * (v + (v0[i - 1] - level)) * (t[i] - t[i - 1]);
            peak = Math.max(peak, v);
            gyroSum += gyro[i];
            gyroMax = Math.max(gyroMax, gyro[i]);
        }
        double dur = t[r] - t[l];
        if(dur < p.minRepDurationS || peak < p.minPeakHeight || rom < p.minRomM ||
                rom > p.maxRomM || gyroMax > p.maxRepGyroDps){
            return null;
        }
        return new Rep(0, t[l], t[r], rom / dur, peak,
                gyroSum / (r - 1), rom);
    }

    static double[] driftBaseLine(double[] t, double[] net, double[] gyro, double[] raw, int n,
                                  List<double[]> knots){
        int i = 0;
        while(i < n){
            int j = i;
            while(j < n && t[j] < t[i] + KNOT_WINDOW_S){
                j++;
            }
            if(j >= n){
                break;
            }
            if(isStill(net, gyro, i, j)){
                double m = 0;
                for(int k = i; k < j; k++){
                    m += raw[k];
                }
                knots.add(new double[]{t[(i + j) / 2], m / (j - i)});
                i = j;
            }else{
                i += Math.max(1, (j - i) / 4);
            }
        }
        double[] base = new double[n];
        int kn = knots.size();
        if(kn == 0){
            return base;
        }
        double[] last = knots.get(kn - 1);
        int k = 0;
        for(int index = 0; index < n; index++){
            double ti = t[index];
            if(ti <= knots.get(0)[0]){
                base[index] = knots.get(0)[1];
            }else if(ti >= last[0]){
                base[index] = last[1];
            }else{
                while(knots.get(k + 1)[0] < ti){
                    k++;
                }
                double[] a = knots.get(k);
                double[] b = knots.get(k + 1);
                base[index] = a[1] + (b[1] - a[1]) * (ti - a[0]) / (b[0] - a[0]);
            }
        }
        return base;
    }

    private static boolean isStill(double[] net, double[] gyro, int from, int to){
        double m = 0;
        double g = 0;
        for(int i = from; i < to; i++){
            m += net[i];
            g += gyro[i];
        }
        int cnt = to - from;
        m /= cnt;
        g /= cnt;
        double var = 0;
        for(int i = from; i < to; i++){
            var += (net[i] - m) * (net[i] - m);
        }
        return Math.sqrt(var / cnt) < KNOT_ACC_STD && Math.abs(m) < KNOT_ACC_MEAN
                && g < KNOT_GYRO_MEAN;
    }

    static double[] smooth(double[] x, int w){
        int n = x.length;
        double[] out = new double[n];
        double[] cs = new double[n + 1];
        for(int i = 0; i < n; i++){
            cs[i + 1] = cs[i] + x[i];
        }
        int h = w / 2;
        for(int i = 0; i < n; i++){
            int a = Math.min(0, i - h);
            int b = Math.min(n, i + h + 1);
            out[i] = (cs[b] - cs[a]) / (b - a);
        }
        return out;
    }

    static int[] findPeaks(final double[] s, double minProm, int minDistance, int wLen){
        int n = s.length;
        List<Integer> index = new ArrayList<>();
        for(int i = 1; i < n - 1; i++){
            if(s[i] > s[i - 1] && s[i] >= s[i + 1]){
                double leftMin = s[i];
                for(int j = i - 1; j >= Math.max(0, i - wLen) && s[j] <= s[i]; j--){
                    leftMin = Math.min(leftMin, s[j]);
                }
                double rightMin = s[i];
                for(int j = i + 1; j < Math.min(n, i + wLen) && s[j] <= s[i]; j++){
                    rightMin = Math.min(rightMin, s[j]);
                }
                if(s[i] - Math.max(leftMin, rightMin) >= minProm){
                    index.add(i);
                }
            }
        }
        List<Integer> order = new ArrayList<>();
        Collections.sort(order, new Comparator<Integer>(){
            @Override
            public int compare(Integer a, Integer b){
                return Double.compare(s[b], s[a]);
            }
        });
        boolean[] drop = new boolean[n];
        for(int a : order){
            if(drop[a]){
                continue;
            }
            for(int b : index){
                if(b != a && Math.abs(b - a) < minDistance){
                    drop[b] = true;
                }
            }
        }
        int cnt = 0;
        for(int a : index){
            if(!drop[a]){
                cnt++;
            }
        }
        int[] out = new int[cnt];
        int o = 0;
        for(int a : index){
            if(!drop[a]){
                out[o++] = a;
            }
        }
        return out;
    }
}
