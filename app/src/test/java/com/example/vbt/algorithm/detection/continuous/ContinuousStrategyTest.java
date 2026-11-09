package com.example.vbt.algorithm.detection.continuous;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import com.example.vbt.algorithm.detection.realtime.RepListener;
import com.example.vbt.algorithm.detection.realtime.StreamingRepDetectionStrategy;
import com.example.vbt.algorithm.detection.realtime.StreamingStrategyFactory;
import com.example.vbt.algorithm.model.ImuSample;
import com.example.vbt.algorithm.model.Lift;
import com.example.vbt.algorithm.model.Rep;

public class ContinuousStrategyTest {
    private static final double G = 9.80665;
    private static final int FS = 200;

    private static double bumpAcc(double t, double start, double len, double vp) {
        if (t < start || t >= start + len) {
            return 0.0;
        }
        return vp * Math.PI / len * Math.cos(Math.PI * (t - start) / len);
    }

    private static List<ImuSample> toSamples(double seconds,
                                             java.util.function.DoubleUnaryOperator acc,
                                             double biasAcc) {
        List<ImuSample> out = new ArrayList<>();
        double th = Math.toRadians(20);
        double sc = 0.948;
        for (int i = 0; i < seconds * FS; i++) {
            double t = i / (double) FS;
            double a = (acc.applyAsDouble(t) + biasAcc) / G;
            out.add(new ImuSample(t, Math.sin(th) * (1 + a) * sc, 0,
                    Math.cos(th) * (1 + a) * sc,
                    4.6, -14.4, 24.0));
        }
        return out;
    }

    private static List<Rep> run(Lift lift, List<ImuSample> samples, List<Double> reportedAt) {
        final List<Rep> reps = new ArrayList<>();
        final double[] now = {0};
        final List<Double> at = reportedAt;
        StreamingRepDetectionStrategy s = StreamingStrategyFactory.forLift(lift);
        s.setListener(new RepListener() {
            @Override
            public void onStatusChanged(Status status) {
            }

            @Override
            public void onRep(Rep rep) {
                reps.add(rep);
                at.add(now[0]);
            }

            @Override
            public void onEpisodeSkipped(String reason) {
            }
        });
        for (ImuSample x : samples) {
            now[0] = x.t;
            s.onSample(x);
        }
        return reps;
    }

    @Test
    public void touchAndGoBenchIsCountedWithoutAnyPause() {
        final double rest = 3.0;
        final double cycle = 2.4;
        List<ImuSample> samples = toSamples(rest + 5 * cycle + 3,
                new java.util.function.DoubleUnaryOperator() {
            @Override
            public double applyAsDouble(double t) {
                double a = 0;
                for (int r = 0; r < 5; r++) {
                    double s = rest + r * cycle;
                    a += bumpAcc(t, s, 1.4, -0.393);
                    a += bumpAcc(t, s + 1.4, 1.0, 0.55);
                }
                return a;
            }
        }, 0.03);
        List<Double> at = new ArrayList<>();
        List<Rep> reps = run(Lift.BENCH, samples, at);
        assertEquals(5, reps.size());
        for (int i = 0; i < reps.size(); i++) {
            Rep r = reps.get(i);
            assertEquals(0.35, r.romM, 0.08);
            assertEquals(0.55, r.peakVelocity, 0.1);
            double pushEnd = rest + i * cycle + 2.4;
            assertTrue("rep " + (i + 1) + " reported late: " + at.get(i) + " vs " + pushEnd,
                    at.get(i) - pushEnd < 0.5);
        }
    }

    @Test
    public void deadliftRepsAreFoundWithTheirRange() {
        final double rest = 3.0;
        List<ImuSample> samples = toSamples(rest + 3 * 4 + 2,
                new java.util.function.DoubleUnaryOperator() {
            @Override
            public double applyAsDouble(double t) {
                double a = 0;
                for (int r = 0; r < 3; r++) {
                    a += bumpAcc(t, rest + 4 * r, 1.6, 0.50);
                }
                return a;
            }
        }, 0.0);
        List<Rep> reps = run(Lift.DEADLIFT, samples, new ArrayList<Double>());
        assertEquals(3, reps.size());
        for (Rep r : reps) {
            assertEquals(0.51, r.romM, 0.08);
            assertEquals(0.50, r.peakVelocity, 0.08);
        }
    }

    @Test
    public void tooShortAMovementIsRejected() {
        List<ImuSample> samples = toSamples(10,
                new java.util.function.DoubleUnaryOperator() {
            @Override
            public double applyAsDouble(double t) {
                return bumpAcc(t, 4.0, 0.2, 0.5);
            }
        }, 0.0);
        assertEquals(0, run(Lift.BENCH, samples, new ArrayList<Double>()).size());
    }

    @Test
    public void factoryBuildsAStrategyForEveryLift() {
        for (Lift l : Lift.values()) {
            assertEquals(l, StreamingStrategyFactory.forLift(l).lift());
        }
    }
}