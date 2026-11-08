package com.example.vbt.algorithm.signal;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

/** Expected values were produced with scipy.signal.find_peaks on the same array. */
public class PeakFinderTest {
    private static final double[] X = {
            0.0, 0.3, -0.3, -0.9, -0.5, -1.0, 0.1, 1.3, -0.5, -0.6, 2.5, 2.5, 2.5, 2.5, -0.0, 0.7,
            -1.3, -0.5, -1.9, -1.3, -1.8, -0.2, -1.3, 0.3, 0.2, -0.2, -2.5, -0.5, -0.0, 0.1, -1.5,
            -0.5, -1.0, -0.8, 1.1, -0.8, -0.0, 0.9, -0.6, -0.1, 0.1, 0.1, -1.2, 0.1, 1.4, -1.5, 0.9,
            0.1, -0.6, 2.0, 0.8, -1.2, 0.1, 0.6, -0.2, 0.7, -0.1, 0.7, 1.4, -0.7};

    @Test
    public void heightOnly() {
        assertArrayEquals(new int[]{7, 11, 15, 34, 37, 44, 46, 49, 53, 55, 58}, PeakFinder.find(X, 0.5, 1));
    }

    @Test
    public void distanceKeepsTallestFirst() {
        assertArrayEquals(new int[]{11, 34, 44, 49, 58}, PeakFinder.find(X, 0.5, 5));
        assertArrayEquals(new int[]{11, 34, 49, 58}, PeakFinder.find(X, 1.0, 8));
    }
}