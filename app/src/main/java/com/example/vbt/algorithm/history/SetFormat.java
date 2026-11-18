package com.example.vbt.algorithm.history;

import com.example.vbt.algorithm.model.Rep;

import java.util.Locale;

public final class SetFormat {
    private SetFormat(){

    }

    private static String load(double kg){
        if(kg == Math.rint(kg)){
            return String.format(Locale.US, "%.0f kg", kg);
        }else {
            return String.format(Locale.US, "%.1f kg", kg);
        }
    }

    public static String summaryLine(SavedSet set, String dateText){
        double best = SetStats.bestMean(set.reps);
        double loss = SetStats.lastLossPercent(set.reps);
    return String.format(Locale.US,  "%s - %s - %s - %d reps - best %.2f m/s - loss %.0f%%",
            dateText,set.lift.getDisplayName(), load(set.loadKg), set.reps.size(), best, loss);
    }

    public static String details(SavedSet set){
        StringBuilder sb = new StringBuilder();
        double[] loss = SetStats.lossPerRep(set.reps);
        for(int i = 0; i < set.reps.size(); i++){
            Rep r = set.reps.get(i);
            sb.append(String.format(Locale.US, "Rep %d  mean %.2f m/s  " +
                            " peak %.2f m/s ROM %.0f cm  loss %.0f%%%n",
                    r.number, r.meanVelocity, r.romM * 100, loss[i]));
        }
        return sb.toString();
    }
}
