package com.example.vbt.algorithm.detection.realtime;

import android.view.Gravity;

import com.example.vbt.algorithm.config.LiftParameters;
import com.example.vbt.algorithm.model.ImuSample;
import com.example.vbt.algorithm.model.Lift;
import com.example.vbt.algorithm.signal.GravityTracker;

import java.util.ArrayDeque;

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


}
