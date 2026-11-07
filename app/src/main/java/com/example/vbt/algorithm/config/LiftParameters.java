package com.example.vbt.algorithm.config;

public final class LiftParameters {
    public final double calibrationWindowS;
    public final double calibrationSearchS;
    public final double orientationAlpha;
    public final double quietGyroThresh;
    public final double quietMagTol;
    public final double quietMinDurationS;
    public final double minPeakHeight;
    public final double minRepDistanceS;
    public final double minRepDurationS;
    // streaming only
    public final double calibStillGyroStd;
    public final double calibStillAccStd;
    public final double maxEpisodeS;
    public final double maxGapS;

    private LiftParameters(Builder b) {
        this.calibrationWindowS = b.calibrationWindowS;
        this.calibrationSearchS = b.calibrationSearchS;
        this.orientationAlpha = b.orientationAlpha;
        this.quietGyroThresh = b.quietGyroThresh;
        this.quietMagTol = b.quietMagTol;
        this.quietMinDurationS = b.quietMinDurationS;
        this.minPeakHeight = b.minPeakHeight;
        this.minRepDistanceS = b.minRepDistanceS;
        this.minRepDurationS = b.minRepDurationS;
        this.calibStillGyroStd = b.calibStillGyroStd;
        this.calibStillAccStd = b.calibStillAccStd;
        this.maxEpisodeS = b.maxEpisodeS;
        this.maxGapS = b.maxGapS;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private double calibrationWindowS = 2.0;
        private double calibrationSearchS = 10.0;
        private double orientationAlpha = 0.98;
        private double quietGyroThresh = 8.0;
        private double quietMagTol = 0.04;
        private double quietMinDurationS = 0.8;
        private double minPeakHeight = 0.35;
        private double minRepDistanceS = 2.5;
        private double minRepDurationS = 0.0;
        private double calibStillGyroStd = 1.0;
        private double calibStillAccStd = 0.005;
        private double maxEpisodeS = 90.0;
        private double maxGapS = 0.1;

        public Builder calibration(double windowS, double searchS) {
            this.calibrationWindowS = windowS;
            this.calibrationSearchS = searchS;
            return this;
        }

        public Builder orientationAlpha(double v) {
            this.orientationAlpha = v;
            return this;
        }

        public Builder quiet(double gyroThreshDps, double magTolG, double minDurationS) {
            this.quietGyroThresh = gyroThreshDps;
            this.quietMagTol = magTolG;
            this.quietMinDurationS = minDurationS;
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

        public Builder streaming(double calibStillGyroStd, double calibStillAccStd,
                                 double maxEpisodeS, double maxGapS) {
            this.calibStillGyroStd = calibStillGyroStd;
            this.calibStillAccStd = calibStillAccStd;
            this.maxEpisodeS = maxEpisodeS;
            this.maxGapS = maxGapS;
            return this;
        }

        public LiftParameters build() {
            return new LiftParameters(this);
        }
    }
}