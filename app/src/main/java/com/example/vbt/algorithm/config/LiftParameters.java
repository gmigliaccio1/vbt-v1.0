package com.example.vbt.algorithm.config;

public final class LiftParameters {
    public final double calibrationWindowS;
    public final double orientationAlpha;
    public final double minPeakHeight;
    public final double minRepDistanceS;
    public final double minRepDurationS;
    // streaming only
    public final double calibStillGyroStd;
    public final double calibStillAccStd;
    public final double minRomM;
    public final double maxRomM;
    public final double repEndFraction;
    public final double maxRepGyroDps;
    public final double cycleOffsetMaxS;
    public final double repSettleS;
    public final boolean floorPulls;
    private LiftParameters(Builder b) {
        this.calibrationWindowS = b.calibrationWindowS;
        this.orientationAlpha = b.orientationAlpha;
        this.minPeakHeight = b.minPeakHeight;
        this.minRepDistanceS = b.minRepDistanceS;
        this.minRepDurationS = b.minRepDurationS;
        this.calibStillGyroStd = b.calibStillGyroStd;
        this.calibStillAccStd = b.calibStillAccStd;
        this.minRomM = b.minRomM;
        this.maxRomM = b.maxRomM;
        this.repEndFraction = b.repEndFraction;
        this.maxRepGyroDps = b.maxRepGyroDps;
        this.cycleOffsetMaxS = b.cycleOffsetMaxS;
        this.repSettleS = b.repSettleS;
        this.floorPulls = b.floorPulls;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private double calibrationWindowS = 2.0;
        private double orientationAlpha = 0.98;
        private double minPeakHeight = 0.35;
        private double minRepDistanceS = 2.5;
        private double minRepDurationS = 0.0;
        private double calibStillGyroStd = 1.0;
        private double calibStillAccStd = 0.005;
        private double minRomM = 0.10;
        private double maxRomM = 1.20;
        private double repEndFraction = 0.15;
        private double maxRepGyroDps = 175.0;
        private double cycleOffsetMaxS = 0.0;
        private double repSettleS = 0.1;
        private boolean floorPulls = false;

        public Builder orientationAlpha(double v) {
            this.orientationAlpha = v;
            return this;
        }

        public Builder minPeakHeight(double v) {
            this.minPeakHeight = v;
            return this;
        }

        public Builder minRepDistanceS(double v) {
            this.minRepDistanceS = v;
            return this;
        }

        public Builder minRepDurationS(double v) {
            this.minRepDurationS = v;
            return this;
        }

        public Builder rom(double minM, double maxM){
            this.minRomM = minM;
            this.maxRomM = maxM;
            return this;
        }

        public Builder repEndFraction(double v){
            this.repEndFraction = v;
            return this;
        }

        public Builder cycleOffsetMaxS(double s){
            this.cycleOffsetMaxS = s;
            return this;
        }

        public Builder repSettleS(double s){
            this.repSettleS = s;
            return this;
        }

        public Builder floorPulls(boolean on){
            this.floorPulls = on;
            return this;
        }

        public Builder maxRepGyro(double dps){
            this.maxRepGyroDps = dps;
            return this;
        }

        public LiftParameters build() {
            return new LiftParameters(this);
        }
    }
}