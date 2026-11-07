package com.example.vbt.algorithm.detection.offline;

import java.util.ArrayList;
import java.util.List;

import com.example.vbt.algorithm.config.LiftParameters;
import com.example.vbt.algorithm.detection.EpisodeAnalyzer;
import com.example.vbt.algorithm.model.Lift;
import com.example.vbt.algorithm.model.QuietWindow;
import com.example.vbt.algorithm.model.Rep;
import com.example.vbt.algorithm.model.SetResult;
import com.example.vbt.algorithm.model.SignalData;
import com.example.vbt.algorithm.signal.OrientationEstimator;
import com.example.vbt.algorithm.signal.PeakFinder;
import com.example.vbt.algorithm.signal.QuietWindowDetector;
import com.example.vbt.algorithm.signal.RestCalibrator;
import com.example.vbt.algorithm.signal.VelocityIntegrator;


public abstract class AbstractRepDetectionStrategy implements RepDetectionStrategy{
    private final Lift lift;
    protected final LiftParameters params;

    protected AbstractRepDetectionStrategy(Lift lift, LiftParameters params){
        this.lift = lift;
        this.params = params;
    }

    @Override
    public final Lift lift(){
        return lift;
    }

    @Override
    public final SetResult analyze(SignalData recording){
        if(recording.size() < 2){
            return new SetResult(lift, new ArrayList<Rep>(),
                    new ArrayList<QuietWindow>(), 0);
        }
        SignalData d = calibrate(recording);
        double[] net = verticalAcceleration(d);
        List<QuietWindow> quiet = findQuietWindows(d);
        boolean[] mask = QuietWindowDetector.mask(d.t, quiet);
        double[] v = VelocityIntegrator.naiveZupt(net, d.t, mask);
        int[] peaks = findRepPeaks(d, v);
        double[] vFixed = v.clone();
        double[] gyroMag = d.gyroMagnitude();
        List<Rep> reps = new ArrayList<>();
        int skipped = 0;

        for(int peak : peaks){
            VelocityIntegrator.Segment seg = VelocityIntegrator.anchored(net, d.t, quiet, peak);
            if(seg == null){
                skipped++;
                continue;
            }
            int a = seg.startIdx;
            System.arraycopy(seg.vel, 0, vFixed, a, seg.vel.length);
            int[] window = EpisodeAnalyzer.concentricWindow(seg.vel, peak - a);
            int s = a + window[0];
            int e = a + window[1];
            Rep rep = EpisodeAnalyzer.summarize(d.t, vFixed, gyroMag, s, e, reps.size() + 1);
            if(rep.durationS() < params.minRepDurationS){
                skipped++;
                continue;
            }
            reps.add(rep);
        }
        return new SetResult(lift, reps, quiet, skipped);
    }

    protected SignalData calibrate(SignalData raw){
        return RestCalibrator.calibrate(raw, params.calibrationWindowS, params.calibrationSearchS);
    }

    protected double[] verticalAcceleration(SignalData d){
        return OrientationEstimator.verticalAcceleration(d, params.orientationAlpha);
    }

    protected List<QuietWindow> findQuietWindows(SignalData d){
        return QuietWindowDetector.find(d, params.quietGyroThresh,
                params.quietMagTol, params.quietMinDurationS);
    }

    protected int[] findRepPeaks(SignalData d, double[] velocity){
        int distance = Math.max(1, (int) (params.minRepDistanceS / d.medianDt()));
        return PeakFinder.find(velocity, params.minPeakHeight, distance);
    }
}
