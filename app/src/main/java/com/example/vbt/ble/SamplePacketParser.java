package com.example.vbt.ble;

import com.example.vbt.algorithm.model.ImuSample;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class SamplePacketParser {
    public static final int HEADER_BYTES = 2;
    public static final int SAMPLE_BYTES = 16;
    public interface Sink{
        void onSample(ImuSample sample);
    }

    private long wraps;
    private long lastRawUs = -1;
    private int lastSeq = -1;
    private int lostPackets;
    private int malformedPackets;
    public void reset(){
        wraps = 0;
        lastRawUs = -1;
        lastSeq = -1;
        lostPackets = 0;
        malformedPackets = 0;
    }

    public int getLostPackets(){
        return lostPackets;
    }

    public int getMalformedPackets(){
        return malformedPackets;
    }

    public int parse(byte[] data, Sink sink){
        if(data == null || data.length < HEADER_BYTES){
            malformedPackets++;
            return -1;
        }
        int seq = data[0] & 0xFF;
        int cnt = data[1] & 0xFF;
        if(data.length != HEADER_BYTES + cnt * SAMPLE_BYTES){
            malformedPackets++;
            return -1;
        }
        if(lastSeq >= 0){
            lostPackets += (seq - lastSeq - 1) & 0xFF;
        }
        lastSeq = seq;
        ByteBuffer bBuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        bBuf.position(HEADER_BYTES);
        for(int i = 0; i < cnt; i++){
            long raw = bBuf.getInt() & 0xFFFFFFFFL;
            if(lastRawUs >= 0 && raw < lastRawUs && lastRawUs - raw > (1L << 31)){
                wraps++;
            }
            lastRawUs = raw;
            double t = (raw + (wraps << 32)) / 1e6;
            double ax = bBuf.getShort() / 1000.0;
            double ay = bBuf.getShort() / 1000.0;
            double az = bBuf.getShort() / 1000.0;
            double gx = bBuf.getShort() / 10.0;
            double gy = bBuf.getShort() / 10.0;
            double gz = bBuf.getShort() / 10.0;
            sink.onSample(new ImuSample(t, ax, ay, az, gx, gy, gz));
        }
        return cnt;
    }
}
