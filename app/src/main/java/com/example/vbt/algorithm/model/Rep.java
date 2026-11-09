package com.example.vbt.algorithm.model;

public final class Rep {
    public final int number;
    public final double startS;
    public final double endS;
    public final double meanVelocity;
    public final double peakVelocity;
    public final double meanGyro;
    public final double romM;

    public Rep(int number, double startS, double endS, double meanVelocity,
                double peakVelocity, double meanGyro, double romM){
        this.number = number;
        this.startS = startS;
        this.endS = endS;
        this.meanVelocity = meanVelocity;
        this.peakVelocity = peakVelocity;
        this.meanGyro = meanGyro;
        this.romM = romM;
    }

    public double durationS(){
        return endS - startS;
    }

    public Rep withNumber(int newNumber){
        return new Rep(newNumber, startS, endS, meanVelocity, peakVelocity, meanGyro, romM);
    }
}
