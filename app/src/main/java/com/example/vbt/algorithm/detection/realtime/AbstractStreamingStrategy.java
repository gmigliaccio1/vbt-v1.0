package com.example.vbt.algorithm.detection.realtime;

import com.example.vbt.algorithm.config.LiftParameters;
import com.example.vbt.algorithm.detection.EpisodeAnalyzer;
import com.example.vbt.algorithm.model.ImuSample;
import com.example.vbt.algorithm.model.Lift;
import com.example.vbt.algorithm.model.Rep;
import com.example.vbt.algorithm.signal.GravityTracker;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractStreamingStrategy implements StreamingRepDetectionStrategy{
    private final Lift lift;
    protected final LiftParameters params;
    private RepListener listener = RepListener.NONE;
    private RepListener.Status status = RepListener.Status.CALIBRATING;

    private final ArrayDeque<ImuSample> calibWindow = new ArrayDeque<>();
    private int sinceLastCalibCheck;
    private final double[] gyroBias = new double[3];
    private double accScale = 1.0;

    private GravityTracker tracker;
    private boolean havePrev;
    private double prevT;

    private boolean quietRunActive;
    private double quietRunStartT;
    private int quietRunStartLocal;
    private boolean stillConfirmed;
    private double anchorT, anchorNet, anchorGyro;

    private double[] et = new double[0];
    private double[] enet = new double[0];
    private double[] egyro = new double[0];
    private int en;
    private boolean inEpisode;
    private boolean episodeAnchored;
    private boolean episodeHasGap;

    private int repCnt;

    protected AbstractStreamingStrategy(Lift lift, LiftParameters params){
        this.lift = lift;
        this.params = params;
        reset();
    }

    @Override
    public final Lift lift(){
        return lift;
    }
    @Override
    public final void setListener(RepListener listener){
        if(listener == null){
            this.listener = RepListener.NONE;
        }else{
            this.listener = listener;
        }
    }
    @Override
    public final RepListener.Status status(){
        return status;
    }
    @Override
    public final void newSet(){
        repCnt = 0;
    }
    @Override
    public final void reset(){
        calibWindow.clear();
        sinceLastCalibCheck = 0;
        tracker = null;
        havePrev = false;
        quietRunActive = false;
        stillConfirmed = false;
        inEpisode = false;
        en = 0;
        repCnt = 0;
        setStatus(RepListener.Status.CALIBRATING);
    }
    @Override
    public final void onSample(ImuSample s){
        if(havePrev && s.t <= prevT){
            return;
        }
        double gap;
        if(havePrev){
            gap = s.t - prevT;
        }else{
            gap = 0.0;
        }
        double dt = Math.min(gap, 0.02);
        prevT = s.t;
        havePrev = true;
        if (status == RepListener.Status.CALIBRATING){
            tryCalibrate(s);
            return;
        }
        double[] acc = {s.ax * accScale, s.ay * accScale, s.az * accScale};
        double[] gyro = {s.gx - gyroBias[0], s.gy - gyroBias[1], s.gz - gyroBias[2]};
        double net = tracker.update(acc, gyro, dt);
        step(s.t, net, GravityTracker.norm(gyro),
                isQuiet(GravityTracker.norm(gyro), GravityTracker.norm(acc)), gap);
    }

    protected boolean isQuiet(double gyroMag, double accMag){
        return gyroMag < params.quietGyroThresh && Math.abs(accMag - 1.0) < params.quietMagTol;
    }

    protected List<Rep> analyzeEpisode(double[] t, double[] net, double[] gyroMag){
        return EpisodeAnalyzer.analyze(t, net, gyroMag, params);
    }

    private void tryCalibrate(ImuSample s) {
        calibWindow.addLast(s);
        while (s.t - calibWindow.peekFirst().t > params.calibrationWindowS) {
            calibWindow.removeFirst();
        }
        if (++sinceLastCalibCheck < 10) {
            return;
        }
        sinceLastCalibCheck = 0;
        if (s.t - calibWindow.peekFirst().t < 0.95 * params.calibrationWindowS) {
            return;
        }
        int n = calibWindow.size();
        double[][] g = new double[3][n];
        double[] a = new double[n];
        int i = 0;
        for (ImuSample c : calibWindow) {
            g[0][i] = c.gx;
            g[1][i] = c.gy;
            g[2][i] = c.gz;
            a[i] = Math.sqrt(c.ax * c.ax + c.ay * c.ay + c.az * c.az);
            i++;
        }
        for (int axis = 0; axis < 3; axis++) {
            if (std(g[axis]) > params.calibStillGyroStd) {
                return;
            }
        }
        if (std(a) > params.calibStillAccStd) {
            return;
        }
        double accMedian = median(a);
        if (accMedian < 0.8 || accMedian > 1.2) {
            return;
        }
        for (int axis = 0; axis < 3; axis++) {
            gyroBias[axis] = median(g[axis]);
        }
        accScale = 1.0 / accMedian;
        tracker = new GravityTracker(params.orientationAlpha);
        ImuSample first = calibWindow.peekFirst();
        quietRunActive = true;
        quietRunStartT = first.t;
        stillConfirmed = true;
        anchorT = s.t;
        anchorNet = 0.0;
        anchorGyro = 0.0;
        tracker.update(new double[]{s.ax * accScale, s.ay * accScale, s.az * accScale},
                new double[]{s.gx - gyroBias[0], s.gy - gyroBias[1], s.gz - gyroBias[2]}, 0.0);
        calibWindow.clear();
        setStatus(RepListener.Status.READY);
    }
    private void step ( double t, double net, double gyroMag, boolean quiet, double gap){
        if (quiet) {
            if (!quietRunActive) {
                quietRunActive = true;
                quietRunStartT = t;
                if (inEpisode) {
                    quietRunStartLocal = en;
                } else {
                    quietRunStartLocal = -1;
                }
            }
            if (!inEpisode) {
                anchorT = t;
                anchorNet = net;
                anchorGyro = gyroMag;
                if (!stillConfirmed && t - quietRunStartT >= params.quietMinDurationS) {
                    stillConfirmed = true;
                    setStatus(RepListener.Status.READY);
                }
            } else {
                append(t, net, gyroMag, gap);
                if (inEpisode && t - quietRunStartT >= params.quietMinDurationS) {
                    finishEpisode(quietRunStartLocal);
                }
            }
        } else {
            quietRunActive = false;
            if (!inEpisode) {
                startEpisode();
            }
            append(t, net, gyroMag, gap);
        }
    }

    private void startEpisode(){
        inEpisode = true;
        en = 0;
        episodeHasGap = false;
        episodeAnchored = stillConfirmed;
        stillConfirmed = false;
        if(episodeAnchored){
            append(anchorT, anchorNet, anchorGyro, 0.0);
        }
        setStatus(RepListener.Status.MOVING);
    }

    private void append(double t, double net, double gyroMag, double gap){
        if(gap > params.maxGapS){
            episodeHasGap = true;
        }
        if(en > 0 && t - et[0] > params.maxEpisodeS){
            abortEpisode("movement too long without a still moment");
            return;
        }
        if(en == et.length){
            int cap = Math.max(1024, et.length * 2);
            et = Arrays.copyOf(et, cap);
            enet = Arrays.copyOf(enet, cap);
            egyro = Arrays.copyOf(egyro, cap);
        }
        et[en] = t;
        enet[en] = net;
        egyro[en] = gyroMag;
        en++;
    }

    private void abortEpisode(String reason){
        inEpisode = false;
        en = 0;
        stillConfirmed = false;
        quietRunActive = false;
        setStatus(RepListener.Status.READY);
        listener.onEpisodeSkipped(reason);;
    }

    private void finishEpisode(int endLocal){
        inEpisode = false;
        stillConfirmed = true;
        setStatus(RepListener.Status.READY);
        if(!episodeAnchored){
            listener.onEpisodeSkipped("no still moment before the movement");
            return;
        }
        if(episodeHasGap){
            listener.onEpisodeSkipped("samples missing during the movement");
            return;
        }
        double[] t = Arrays.copyOf(et, endLocal + 1);
        double[] net = Arrays.copyOf(enet, endLocal + 1);
        double[] gyro = Arrays.copyOf(egyro, endLocal + 1);
        List<Rep> reps = analyzeEpisode(t, net, gyro);
        if(reps.isEmpty()){
            listener.onEpisodeSkipped("movement without a valid rep");
            return;
        }
        for(Rep r : reps){
            repCnt++;
            listener.onRep(r.withNumber(repCnt));
        }
    }

    private void setStatus(RepListener.Status s){
        if(status != s){
            status = s;
            listener.onStatusChanged(s);
        }
    }

    private static double std(double[] x){
        double m = 0;
        for(double v : x){
            m += v;
        }
        m /= x.length;
        double s = 0;
        for(double v : x){
            s += (v - m) * (v - m);
        }
        return Math.sqrt(s / x.length);
    }

    private static double median(double[] x){
        double[] c = x.clone();
        Arrays.sort(c);
        int n = c.length;
        if(n % 2 == 1){
            return c[n / 2];
        }else{
            return 0.5 * (c[n / 2 - 1] + c[n / 2]);
        }
    }
}
