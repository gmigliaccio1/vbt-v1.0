package com.example.vbt.recording;

import android.credentials.CredentialOption;

import com.example.vbt.algorithm.model.SignalData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public final class CsvSetReader {
    private static final double MAX_TIMESTAMP_S = 2 * 3600;
    private static final double MAX_STEP_S = 1.0;
    private CsvSetReader(){
    }

    public static final class Result{
        public final SignalData data;
        public final int droppedLines;
        Result(SignalData data, int droppedLines){
            this.data = data;
            this.droppedLines = droppedLines;
        }
    }

    public static Result read(Reader source) throws IOException{
        BufferedReader in  = new BufferedReader(source);
        in.readLine();
        List<double[]> candidates = new ArrayList<>();
        int dropped = 0;
        String line;
        while((line = in.readLine()) != null){
            String[] parts = line.trim().split(",", -1);
            if(parts.length != 7){
                dropped++;
                continue;
            }
            double[] vals = new double[7];
            boolean ok = true;
            for(int i = 0; i < 7; i++){
                try{
                    vals[i] = Double.parseDouble(parts[i].trim());
                }catch(NumberFormatException ex){
                    ok = false;
                    break;
                }
            }
            if(!ok || vals[0] > MAX_TIMESTAMP_S){
                dropped++;
                continue;
            }
            candidates.add(vals);
        }
        List<double[]> rows = new ArrayList<>();
        for(int i = 0; i < candidates.size(); i++){
            double[] v = candidates.get(i);
            if(!rows.isEmpty()){
                double prev = rows.get(rows.size() - 1)[0];
                boolean backward = v[0] <= prev;
                boolean bigJump = v[0] - prev > MAX_STEP_S;
                boolean confirmed = i + 1 < candidates.size() && candidates.get(i + 1)[0] > v[0]
                        && candidates.get(i + 1)[0] - v[0] < 0.1;
                if(backward || (bigJump && !confirmed)){
                    dropped++;
                    continue;
                }
            }
            rows.add(v);
        }
        int n = rows.size();
        double[] t = new double[n];
        double[][] acc = new double[n][3];
        double[][] gyro = new double[n][3];
        double t0;
        if(n > 0){
            t0 = rows.get(0)[0];
        }else{
            t0 = 0.0;
        }
        for(int i = 0; i < n; i++){
            double[] r = rows.get(i);
            t[i] = r[0] - t0;
            acc[i][0] = r[1];
            acc[i][1] = r[2];
            acc[i][2] = r[3];
            gyro[i][0] = r[4];
            gyro[i][1] = r[5];
            gyro[i][2] = r[6];
        }
        return new Result(new SignalData(t, acc, gyro), dropped);
    }
}
