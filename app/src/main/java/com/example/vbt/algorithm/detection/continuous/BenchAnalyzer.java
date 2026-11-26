package com.example.vbt.algorithm.detection.continuous;

import com.example.vbt.algorithm.config.LiftParameters;
import com.example.vbt.algorithm.config.LiftProfiles;
import com.example.vbt.algorithm.model.Rep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BenchAnalyzer implements LiftAnalyzer{

    static final double MAX_PRESS_S = 3.0;
    static final double MIN_MEAN_TO_PEAK = 0.5;
    static final double UNRACK_MAX_ROM_M = 0.14;
    static final double UNRACK_MAX_MEAN_TO_PEAK = 0.60;

    @Override
    public List<Rep> analyze(AnalysisSignal s, LiftParameters p, double afterPeakT){
        List<Rep> normal = run(s, p, afterPeakT);
        List<Rep> calm = run(s, LiftProfiles.BENCH_CALM, afterPeakT);
        double typicalRom = medianRom(normal, calm);
        List<Rep> merged = new ArrayList<>();
        List<Rep> taken = new ArrayList<>();
        for(Rep a : normal){
            Rep best = a;
            for(Rep b : calm){
                if(overlaps(a, b)){
                    taken.add(b);
                    if(Math.abs(b.romM - typicalRom) < Math.abs(best.romM - typicalRom)){
                        best = b;
                    }
                }
            }
            merged.add(best);
        }
        for(Rep b : calm){
            if(!taken.contains(b)){
                merged.add(b);
            }
        }
        merged.sort((x, y) -> Double.compare(x.startS, y.startS));
        List<Rep> out = new ArrayList<>();
        for(Rep r : merged){
            if(out.isEmpty() || r.startS >= out.get(out.size() - 1).endS){
                out.add(r);
            }
        }
        return out;
    }

    private static List<Rep> run(AnalysisSignal s, LiftParameters p, double afterPeakT){
        List<Rep> reps = new ArrayList<>();
        for(Rep r : ContinuousRepAnalyzer.analyze(s.t, s.net, s.gyro, s.n, p, afterPeakT)){
           boolean unrack = r.romM < UNRACK_MAX_ROM_M
                   && r.meanVelocity < UNRACK_MAX_MEAN_TO_PEAK * r.peakVelocity;
           if(r.durationS() <= MAX_PRESS_S && r.meanVelocity >= MIN_MEAN_TO_PEAK * r.peakVelocity
           && !unrack){
               reps.add(r);
           }
        }
        return reps;
    }

    private static boolean overlaps(Rep a, Rep b){
        return a.startS < b.endS && b.startS < a.endS;
    }

    private static double medianRom(List<Rep> a, List<Rep> b){
        List<Rep> all = new ArrayList<>(a);
        all.addAll(b);
        if(all.isEmpty()){
            return 0.0;
        }
        double[] roms = new double[all.size()];
        for(int i = 0; i < roms.length; i++){
            roms[i] = all.get(i).romM;
        }
        Arrays.sort(roms);
        return roms[roms.length / 2];
    }
}
