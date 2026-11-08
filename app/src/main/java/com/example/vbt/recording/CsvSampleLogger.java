package com.example.vbt.recording;

import com.example.vbt.algorithm.model.ImuSample;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public final class CsvSampleLogger implements Closeable {
    private final BufferedWriter out;
    private final File file;
    public CsvSampleLogger(File file) throws IOException{
        this.file = file;
        this.out = new BufferedWriter(new FileWriter(file), 1 << 16);
        out.write("t_s,ax_g,ay_g,az_g,gx_dps,gy_dps,gz_dps\n");
    }

    public File getFile(){
        return file;
    }
    public void log(ImuSample s) throws IOException{
        out.write(String.format(Locale.US, "%.6f,%.3f,%.3f,%.3f,%.1f,%.1f,%.1f\n",
                s.t, s.ax, s.ay, s.az, s.gx, s.gy, s.gz));
    }

    @Override
    public void close() throws IOException{
        out.close();
    }

}
