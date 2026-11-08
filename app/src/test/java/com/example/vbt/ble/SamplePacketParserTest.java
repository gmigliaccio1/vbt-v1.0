package com.example.vbt.ble;

import static org.junit.Assert.assertEquals;


import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.example.vbt.algorithm.model.ImuSample;

public class SamplePacketParserTest {

    private static byte[] packet(int seq, long[] tUs) {
        ByteBuffer b = ByteBuffer.allocate(2 + tUs.length * 16).order(ByteOrder.LITTLE_ENDIAN);
        b.put((byte) seq).put((byte) tUs.length);
        for (long t : tUs) {
            b.putInt((int) t);
            b.putShort((short) 154).putShort((short) 9).putShort((short) 1042);   // mg
            b.putShort((short) 49).putShort((short) -144).putShort((short) 240);  // 0.1 dps
        }
        return b.array();
    }

    private static final SamplePacketParser.Sink NOOP = new SamplePacketParser.Sink() {
        @Override
        public void onSample(ImuSample sample) {
        }
    };

    @Test
    public void decodesUnits() {
        final List<ImuSample> got = new ArrayList<>();
        SamplePacketParser p = new SamplePacketParser();
        int n = p.parse(packet(0, new long[]{0, 5000}), new SamplePacketParser.Sink() {
            @Override
            public void onSample(ImuSample s) {
                got.add(s);
            }
        });
        assertEquals(2, n);
        assertEquals(0.005, got.get(1).t, 1e-9);
        assertEquals(0.154, got.get(0).ax, 1e-9);
        assertEquals(1.042, got.get(0).az, 1e-9);
        assertEquals(4.9, got.get(0).gx, 1e-9);
        assertEquals(-14.4, got.get(0).gy, 1e-9);
        assertEquals(24.0, got.get(0).gz, 1e-9);
    }

    @Test
    public void countsLostPacketsAcrossSequenceWrap() {
        SamplePacketParser p = new SamplePacketParser();
        p.parse(packet(254, new long[]{0}), NOOP);
        p.parse(packet(1, new long[]{5000}), NOOP);   // 255 and 0 never arrived
        assertEquals(2, p.getLostPackets());
    }

    @Test
    public void rejectsWrongLength() {
        SamplePacketParser p = new SamplePacketParser();
        byte[] bad = packet(0, new long[]{0, 5000});
        byte[] truncated = java.util.Arrays.copyOf(bad, bad.length - 3);
        assertEquals(-1, p.parse(truncated, NOOP));
        assertEquals(1, p.getMalformedPackets());
    }

    @Test
    public void unwrapsMicrosecondCounter() {
        final List<ImuSample> got = new ArrayList<>();
        SamplePacketParser p = new SamplePacketParser();
        SamplePacketParser.Sink sink = new SamplePacketParser.Sink() {
            @Override
            public void onSample(ImuSample s) {
                got.add(s);
            }
        };
        p.parse(packet(0, new long[]{0xFFFFFFFFL - 1000}), sink);
        p.parse(packet(1, new long[]{4000}), sink);   // counter wrapped
        assertEquals(true, got.get(1).t > got.get(0).t);
        assertEquals(0.005, got.get(1).t - got.get(0).t, 1e-4);
    }
}