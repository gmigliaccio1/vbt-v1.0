package com.example.vbt.algorithm.signal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class PeakFinder {
    private PeakFinder(){
    }

    public static int[] find(final double[] x, double minHeight, int distance){
        List<Integer> maxima = localMaxima(x);
        List<Integer> tall = new ArrayList<>();
        for(int p : maxima){
            if(x[p] >= minHeight){
                tall.add(p);
            }
        }
        int[] peaks = new int[tall.size()];
        for(int i = 0; i < peaks.length; i++){
            peaks[i] = tall.get(i);
        }
        if(distance < 1){
            return peaks;
        }
        return selectByDistance(peaks, x, distance);
    }

    private static List<Integer> localMaxima(double[] x){
        List<Integer> out = new ArrayList<>();
        int i = 1;
        int iMax = x.length - 1;
        while(i < iMax){
            if(x[i - 1] < x[i]){
                int ahead = i + 1;
                while(ahead < iMax && x[ahead] == x[i]){
                    ahead++;
                }
                if(x[ahead] < x[i]){
                    int left = i;
                    int right = ahead - 1;
                    out.add((left + right) / 2);
                    i = ahead;
                }
            }
            i++;
        }
        return out;
    }

    private static int[] selectByDistance(final int[] peaks, final double[] x, int distance){
        int size = peaks.length;
        boolean[] keep = new boolean[size];
        Arrays.fill(keep, true);
        Integer[] order = new Integer[size];
        for(int i = 0; i < size; i++){
            order[i] = i;
        }
        Arrays.sort(order, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Double.compare(x[peaks[a]], x[peaks[b]]);
            }
        });
        for (int i = size - 1; i >= 0; i--){
            int j = order[i];
            if(!keep[j]){
                continue;
            }
            int k = j - 1;
            while (k >= 0 && peaks[j] - peaks[k] < distance){
                keep[k] = false;
                k--;
            }
            k = j + 1;
            while(k < size && peaks[k] - peaks[j] < distance) {
                keep[k] = false;
                k++;
            }
        }
        int count = 0;
        for(boolean b : keep){
            if(b){
                count++;
            }
        }
        int[] res = new int[count];
        int w = 0;
        for(int i = 0; i < size; i++){
            if(keep[i]){
                res[w++] = peaks[i];
            }
        }
        return res;
    }
}
