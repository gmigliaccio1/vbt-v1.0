package com.example.vbt.algorithm.detection.continuous;

import com.example.vbt.algorithm.config.LiftParameters;
import com.example.vbt.algorithm.config.LiftProfiles;
import com.example.vbt.algorithm.detection.realtime.RepListener;
import com.example.vbt.algorithm.detection.realtime.StreamingRepDetectionStrategy;
import com.example.vbt.algorithm.model.ImuSample;
import com.example.vbt.algorithm.model.Lift;
import com.example.vbt.algorithm.model.Rep;
import com.example.vbt.algorithm.signal.GravityTracker;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

public final class ContinuousStreamingStrategy implements StreamingRepDetectionStrategy {
    static final double WINDOW_S = 60.0;
    static final int ANALYZE_EVERY = 20;
    private static final double MAX_DT = 0.06;
    private static final double MOVING_GYRO_DPS = 15.0;
    private static final double CALM_S = 1.0;
    private final Lift lift;
    private final LiftParameters params;
    private RepListener listener = RepListener.NONE;
    private RepListener.Status status = RepListener.Status.CALIBRATING;
    private final ArrayDeque<ImuSample> calibWindow = new ArrayDeque<>();
    private int sinceLastCalibCheck;
    private final double[] gyroBias = new double[3];
    private double accScale = 1.0;
    private GravityTracker tracker;
    private boolean havePrev;
    private double prevT;
    private double lastMotionT;
    private double[] bt = new double[4096];
    private double[] bnet = new double[4096];
    private double[] bgyro = new double[4096];
    private int bn;
    private int sinceAnalyze;
    private double lastPeakT = Double.NEGATIVE_INFINITY;
    private int repCount;

    public ContinuousStreamingStrategy(Lift lift){
        this.lift = lift;
        this.params = LiftProfiles.continuousForLift(lift);
        reset();
    }

    @Override
    public Lift lift(){
        return lift;
    }

    @Override
    public void setListener(RepListener listener){
        if(listener == null){
            this.listener = RepListener.NONE;
        }else{
            this.listener = listener;
        }
    }

    @Override
    public RepListener.Status status(){
        return status;
    }

    @Override
    public void newSet(){
        repCount = 0;
    }

    @Override
    public void reset(){
        calibWindow.clear();
        sinceLastCalibCheck = 0;
        tracker = null;
        havePrev = false;
        bn = 0;
        sinceAnalyze = 0;
        lastPeakT = Double.NEGATIVE_INFINITY;
        repCount = 0;
        setStatus(RepListener.Status.CALIBRATING);
    }

    @Override
    public void onSample(ImuSample s){
        if(havePrev && s.t <= prevT){
            return;
        }
        double dt;
        if(havePrev){
            dt = Math.min(s.t - prevT, MAX_DT);
        }else{
            dt = 0.0;
        }
        prevT = s.t;
        havePrev = true;
        if(status == RepListener.Status.CALIBRATING){
            tryCalibrate(s);
            return;
        }
        double[] acc = {s.ax * accScale, s.ay * accScale, s.az * accScale};
        double[] gyro = {s.gx - gyroBias[0], s.gy - gyroBias[1], s.gz - gyroBias[2]};
        double net = tracker.update(acc, gyro, dt);
        double gyroMag = GravityTracker.norm(gyro);
        append(s.t, net, gyroMag);
        if (gyroMag > MOVING_GYRO_DPS || Math.abs(GravityTracker.norm(acc) - 1.0) > 0.08){
            lastMotionT = s.t;
            setStatus(RepListener.Status.MOVING);
        }else if(s.t - lastMotionT > CALM_S){
            setStatus(RepListener.Status.READY);
        }
        if(++sinceAnalyze >= ANALYZE_EVERY){
            sinceAnalyze = 0;
            findReps();
        }
    }

    private void append(double t, double net, double gyroMag){
        if(bn == bt.length){
            if(t - bt[0] > WINDOW_S){
                int cut = 0;
                while(cut < bn && t - bt[cut] > WINDOW_S){
                    cut++;
                }
                System.arraycopy(bt, cut, bt, 0, bn - cut);
                System.arraycopy(bnet, cut, bnet, 0, bn - cut);
                System.arraycopy(bgyro, cut, bgyro, 0, bn - cut);
                bn -= cut;
            }
            if(bn == bt.length){
                int cap = bt.length * 2;
                bt = Arrays.copyOf(bt, cap);
                bnet = Arrays.copyOf(bnet, cap);
                bgyro = Arrays.copyOf(bgyro, cap);
            }
        }
        bt[bn] = t;
        bnet[bn] = net;
        bgyro[bn] = gyroMag;
        bn++;
    }

    private void findReps(){
        int from = 0;
        while(from < bn && bt[bn - 1] - bt[from] > WINDOW_S){
            from++;
        }

        int n = bn - from;
        double[] t = Arrays.copyOfRange(bt, from, bn);
        double[] net = Arrays.copyOfRange(bnet, from, bn);
        double[] gyro = Arrays.copyOfRange(bgyro, from, bn);
        List<Rep> reps = ContinuousRepAnalyzer.analyze(t, net, gyro, n, params, lastPeakT);
        for(Rep r : reps){
            if(r.startS < lastPeakT){
                continue;
            }
            lastPeakT = Math.max(lastPeakT, 0.5 * (r.startS + r.endS));
            repCount++;
            listener.onRep(r.withNumber(repCount));
        }
    }

    private void tryCalibrate(ImuSample s){
        calibWindow.addLast(s);
        while(s.t - calibWindow.peekFirst().t > params.calibrationWindowS){
            calibWindow.removeFirst();
        }
        if(++sinceLastCalibCheck < 10){
            return;
        }
        sinceLastCalibCheck = 0;
        if(s.t - calibWindow.peekFirst().t < 0.95 * params.calibrationWindowS){
            return;
        }
        int n = calibWindow.size();
        double[][] g = new double[3][n];
        double[] a = new double[n];
        int i = 0;
        for(ImuSample cW : calibWindow){
            g[0][i] = cW.gx;
            g[1][i] = cW.gy;
            g[2][i] = cW.gz;
            a[i] = Math.sqrt(cW.ax * cW.ax + cW.ay * cW.ay + cW.az * cW.az);
            i++;
        }
        for(int axis = 0; axis < 3; axis++){
            if(std(g[axis]) > params.calibStillGyroStd){
                return;
            }
        }
        if(std(a) > params.calibStillAccStd){
            return;
        }
        double accMedian = median(a);
        if(accMedian < 0.8 || accMedian > 1.2){
            return;
        }
        for(int axis = 0; axis < 3; axis++){
            gyroBias[axis] = median(g[axis]);
        }
        accScale = 1.0 / accMedian;
        tracker = new GravityTracker(params.orientationAlpha);
        tracker.update(new double[]{s.ax * accScale, s.ay * accScale, s.az * accScale},
                new double[]{s.gx - gyroBias[0], s.gy - gyroBias[1], s.gz - gyroBias[2]}, 0.0);
        calibWindow.clear();
        lastMotionT = s.t;
        setStatus(RepListener.Status.READY);
    }

    private void setStatus(RepListener.Status st){
        if(status != st){
            status = st;
            listener.onStatusChanged(st);
        }
    }

    private static double std(double[] x){
        double m = 0;
        for(double v : x){
            m += v;
        }
        m/= x.length;
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
