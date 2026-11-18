package com.example.vbt.algorithm.history;

import com.example.vbt.algorithm.model.Rep;

import java.util.List;

public final class SetStats {
    private SetStats(){

    }

    public static double lossPercent(double best, double mean){
        if(best <= 0.0){
            return 0.0;
        }
        return Math.max(0.0, (best - mean) / best * 100.0);
    }

    public static double[] lossPerRep(List<Rep> reps){
        double[] loss = new double[reps.size()];
        double best = 0.0;
        for(int i = 0; i < loss.length; i++){
            best = Math.max(best, reps.get(i).meanVelocity);
            loss[i] = lossPercent(best, reps.get(i).meanVelocity);
        }
        return loss;
    }

    public static double bestMean(List<Rep> reps){
        double best = 0.0;
        for(Rep r : reps){
            best = Math.max(best, r.meanVelocity);
        }
        return best;
    }

    public static double lastLossPercent(List<Rep> reps){
        if(reps.isEmpty()){
            return 0.0;
        }
        double[] loss = lossPerRep(reps);
        return loss[loss.length - 1];
    }
}
