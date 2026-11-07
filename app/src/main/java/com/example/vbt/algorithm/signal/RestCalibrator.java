package com.example.vbt.algorithm.signal;

import java.util.Arrays;
import com.example.vbt.algorithm.model.SignalData;

public class RestCalibrator {
    private RestCalibrator(){
    }

    public static SignalData calibrate(SignalData d, double windowSeconds, double maxSearchSeconds){
        double fs = 1.0 / d.medianDt();
        int winLen = (int) (windowSeconds * fs);
        int n = Math.min((int) (maxSearchSeconds * fs), d.size());
        if(winLen < 1 || n < winLen) {
            return d;
        }
        double[] gyroMag = d.gyroMagnitude();
        double[] accMag = d.accMagnitude();

        int bestEnd = -1;
        double bestScore = Double.POSITIVE_INFINITY;
        for (int end = winLen - 1; end < n; end++){
            double sg = 0.0;
            double sa = 0.0;
            for (int k = end - winLen + 1; k <= end; k++){
                sg += gyroMag[k];
                sa += accMag[k];
            }
            double score = sg / winLen + Math.abs(sa / winLen - 1.0) * 10.0;
            if(score < bestScore){
                bestScore = score;
                bestEnd = end;
            }
        }
        int bestStart = Math.max(0, bestEnd - winLen);
        int len = bestEnd - bestStart;
        if(len < 1){
            return d;
        }
        double[] bias = new double[3];
        for (int axis = 0; axis < 3; axis++){
            double[] col = new double[len];
            for(int k = 0; k < len; k++){
                col[k] = d.gyro[bestStart + k][axis];
            }
            bias[axis] = median(col);
        }
        double restMag[] = Arrays.copyOfRange(accMag, bestStart, bestEnd);
        double accScale = 1.0 / median(restMag);
        double[][] acc = new double[d.size()][3];
        double[][] gyro = new double[d.size()][3];
        for(int i = 0; i < d.size(); i++){
            for(int axis = 0; axis < 3; axis++){
                acc[i][axis] = d.acc[i][axis] * accScale;
                gyro[i][axis] = d.gyro[i][axis] - bias[axis];
            }
        }
        return new SignalData(d.t, acc, gyro);
    }

    static double median(double[] x){
        double[] c = x.clone();
        Arrays.sort(c);
        int n = c.length;
        if(n % 2 == 1){
            return c[n / 2];
        }else{
            return 0.5 * ((c[n / 2 - 1] + c[n / 2]));
        }
    }
}
