package com.example.vbt.algorithm.detection.continuous;

import com.example.vbt.algorithm.config.LiftParameters;
import com.example.vbt.algorithm.model.Rep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class FloorPullAnalyzer {

    static final double SHOCK_NET = 30.0;
    static final double SHOCK_GYRO_DPS = 350.0;
    static final double SHOCK_MARGIN_S = 0.3;
    static final double MIN_PIECE_S = 1.5;
    static final double LOBE_SMOOTH_S = 0.4;
    static final double LOBE_MIN_ACC = 0.6;
    static final double ONSET_ACC = 0.3;
    static final double REST_SKIP_S = 0.3;
    static final double REST_BEFORE_PULL_S = 1.2;
    static final double BIAS_WINDOW_S = 1.0;
    static final double BIAS_GAP_S = 0.1;
    static final double V_START_BEFORE_ONSET_S = 0.2;
    static final double V_SMOOTH_S = 0.15;
    static final double NEXT_PULL_GAP_S = 0.5;
    static final int MAX_PULLS_PER_PIECE = 8;
    private FloorPullAnalyzer(){

    }

    public static List<Rep> analyze(double[] t, double[] net, double[] gyroMag, int n,
                                    LiftParameters p, double afterPeakT){
        List<Rep> reps = new ArrayList<>();
        if(n < 20 || t[n - 1] <= t[0]){
            return reps;
        }
        double fs = (n - 1)/(t[n - 1] - t[0]);
        boolean[] shock = shockMask(net, gyroMag, n, (int) Math.round(SHOCK_MARGIN_S * fs));
        int i = 0;
        while(i < n){
            if(shock[i]){
                i++;
                continue;
            }
            int j = i;
            while(j < n && !shock[j]){
                j++;
            }
            if(j - i >= MIN_PIECE_S * fs){
                analyzePiece(t, net, gyroMag, i, j, fs, p, afterPeakT, reps);
            }
            i = j;
        }
        List<Rep> numbered = new ArrayList<>();
        for(Rep r : reps){
            numbered.add(r.withNumber(numbered.size() + 1));
        }
        return numbered;
    }

    private static boolean[] shockMask(double[] net, double[] gyro, int n, int margin){
        boolean shock[] = new boolean[n];
        for(int i = 0; i < n; i++){
            if(Math.abs(net[i]) > SHOCK_NET || gyro[i] > SHOCK_GYRO_DPS){
                for(int j = Math.max(0, i - margin); j <= Math.min(n -1, i + margin); j++){
                    shock[j] = true;
                }
            }
        }
        return shock;
    }

    private static void analyzePiece(double[] t, double[] netAll, double[] gyroAll,
                                     int from, int to, double fs, LiftParameters p,
                                     double afterPeakT, List<Rep> out){
        int mid = to - from;
        double[] ts = Arrays.copyOfRange(t, from, to);
        double[] ns = Arrays.copyOfRange(netAll, from, to);
        double[] gs = Arrays.copyOfRange(gyroAll, from, to);
        double[] sm = ContinuousRepAnalyzer.smooth(ns, Math.max(3, (int) (LOBE_SMOOTH_S * fs)));
        int searchFrom = 0;
        for(int pull = 0; pull < MAX_PULLS_PER_PIECE; pull++){
            int ia = -1;
            double best = LOBE_MIN_ACC;
            for(int i = searchFrom; i < mid; i++){
                if(sm[i] > best){
                    best = sm[i];
                    ia = i;
                }
            }
            if(ia < 0){
                return;
            }
            int low = searchFrom + (int) (REST_SKIP_S * fs);
            int high = Math.max(low + (int) (0.3 * fs), ia - (int) (REST_BEFORE_PULL_S * fs));
            high = Math.min(high, mid);
            double b0;
            if(high > low){
                b0 = median(ns, low, high);
            }else{
                b0 = median(ns, searchFrom, Math.max(searchFrom + 1, ia));
            }
            int on = ia;
            while(on > searchFrom && sm[on] - b0 > ONSET_ACC){
                on--;
            }
            int s0 = Math.max(searchFrom, on - (int) (BIAS_WINDOW_S * fs));
            int s1 = Math.min(mid, Math.max(s0 + (int) (0.2 * fs), on - (int) (BIAS_GAP_S * fs)));
            double bias = median(ns, s0, Math.max(s0 + 1, s1));
            int st = Math.max(searchFrom, on - (int) (V_START_BEFORE_ONSET_S * fs));
            int len = mid - st;
            double[] v = new double[len];
            double acc = 0.0;
            for(int k = 0; k < len; k++){
                double dt = 0.0;
                if(st + k > 0){
                    dt = Math.min(Math.max(ts[st + k] - ts[st + k -1], 0.0),
                            ContinuousRepAnalyzer.MAX_DT);
                }
                acc += (ns[st + k] - bias) * dt;
                v[k] = acc;
            }
            v = ContinuousRepAnalyzer.smooth(v, Math.max(3, (int) (V_SMOOTH_S * fs)));
            int pk = 0;
            for(int k = 1; k < len; k++){
                if(v[k] > v[pk]){
                    pk = k;
                }
            }
            double peak = v[pk];
            double th = p.repEndFraction * peak;
            int l = pk;
            while(l > 0 && v[l] > th){
                l--;
            }
            int r = pk;
            while(r < len - 1 && v[r] > th){
                r++;
            }
            int after = st + r + (int) (NEXT_PULL_GAP_S * fs);
            if(peak < p.minPeakHeight || r >= len - 1 || r <= l + 1){
                searchFrom = Math.max(ia + (int) (NEXT_PULL_GAP_S * fs), searchFrom + 1);
                if(searchFrom >= mid){
                    return;
                }
                continue;
            }
            double rom = 0.0;
            double gyroSum = 0.0;
            double gyroMax = 0.0;
            for(int k = l + 1; k <= r; k++){
                rom += 0.5 * (v[k] + v[k - 1]) * (ts[st + k] - ts[st + k - 1]);
                gyroSum += gs[st + k];
                gyroMax = Math.max(gyroMax, gs[st + k]);
            }
            double dur = ts[st + r] - ts[st + l];
            boolean valid = dur >= p.minRepDurationS && rom >= p.minRomM && rom <= p.maxRomM
                    && gyroMax <= p.maxRepGyroDps;
            if(valid && ts[st + pk] > afterPeakT + 0.5){
                out.add(new Rep(0, ts[st + l], ts[st + r], rom / dur,
                        peak, gyroSum / (r - l), rom));
            }
            searchFrom = after;
            if(searchFrom >= mid){
                return;
            }
        }
    }

    private static double median(double[] x, int from, int to){
        double[] c = Arrays.copyOfRange(x, from, Math.max(from + 1, to));
        Arrays.sort(c);
        int n = c.length;
        if(n % 2 == 1){
            return c[n / 2];
        }else{
            return 0.5 * (c[n / 2 - 1] + c[n / 2]);
        }
    }
}
