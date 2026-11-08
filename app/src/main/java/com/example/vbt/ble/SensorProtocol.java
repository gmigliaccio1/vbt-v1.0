package com.example.vbt.ble;

import java.util.UUID;

public final class SensorProtocol {
    public static final UUID SERVICE_UUID =
            UUID.fromString("7a0f0001-5b1e-4c52-9a0b-6f7c0e3b5a10");
    public static final UUID DATA_UUID =
            UUID.fromString("7a0f0001-5b1e-4c52-9a0b-6f7c0e3b5a10");
    public static final UUID CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final int REQUESTED_MTU = 247;
    private SensorProtocol(){
    }
}
