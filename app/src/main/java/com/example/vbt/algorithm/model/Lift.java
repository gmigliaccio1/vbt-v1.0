package com.example.vbt.algorithm.model;

public enum Lift {
    SQUAT("Squat"),
    BENCH("Bench Press"),
    DEADLIFT("Deadlift");

    private final String displayName;

    Lift(String displayName){
        this.displayName = displayName;
    }

    String getDisplayName() {
        return displayName;
    }
}
