package com.example.vbt.algorithm.signal;

import java.util.ArrayList;
import java.util.List;

import com.example.vbt.algorithm.model.QuietWindow;
import com.example.vbt.algorithm.model.SignalData;
public final class QuietWindowDetector {

    private QuietWindowDetector(){
    }

    public static List<QuietWindow> find(SignalData d, double gyroThresh,
                                          double magTol, double minDurationS){
        double[] accMag = d.accMagnitude();
        double[] gyroMag = d.gyroMagnitude();
        double[] t = d.t;
        List<QuietWindow> windows = new ArrayList<>();
        boolean inWindow = false;
        int start = 0;
        for(int i = 0; i < d.size(); i++){
            boolean quiet = gyroMag[i] < gyroThresh && Math.abs(accMag[i] - 1.0) < magTol;
            if (quiet && !inWindow) {
                start = i;
                inWindow = true;
            } else if (!quiet && inWindow) {
                inWindow = false;
                if (t[i - 1] - t[start] >= minDurationS) {
                    windows.add(new QuietWindow(t[start], t[i - 1]));
                }
            }
        }
        if (inWindow && t[d.size() - 1] - t[start] >= minDurationS) {
            windows.add(new QuietWindow(t[start], t[d.size() - 1]));
        }
        return windows;
    }

    public static boolean[] mask(double[] t, List<QuietWindow> windows) {
        boolean[] m = new boolean[t.length];
        for (QuietWindow w : windows) {
            for (int i = 0; i < t.length; i++) {
                if (t[i] >= w.startS && t[i] <= w.endS) {
                    m[i] = true;
                }
            }
        }
        return m;
    }
}
