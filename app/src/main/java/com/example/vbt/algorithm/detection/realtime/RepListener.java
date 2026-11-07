package com.example.vbt.algorithm.detection.realtime;

import com.example.vbt.algorithm.model.Rep;

public interface RepListener {

    enum Status {
        CALIBRATING,
        READY,
        MOVING
    }

    void onStatusChanged(Status status);
    void onRep(Rep rep);
    void onEpisodeSkipped(String reason);

    RepListener NONE = new RepListener() {
        @Override
        public void onStatusChanged(Status status) {
        }

        @Override
        public void onRep(Rep rep) {
        }

        @Override
        public void onEpisodeSkipped(String reason) {
        }
    };
}