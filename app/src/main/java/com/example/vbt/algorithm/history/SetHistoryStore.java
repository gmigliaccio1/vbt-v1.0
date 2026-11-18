package com.example.vbt.algorithm.history;

import android.icu.text.UFormat;

import com.example.vbt.algorithm.model.Lift;
import com.example.vbt.algorithm.model.Rep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kotlin.text.RegexOption;

public final class SetHistoryStore  {
    private final File file;

    public SetHistoryStore(File file){
        this.file = file;
    }

    public synchronized void append(SavedSet set) throws IOException{
        try(FileWriter w = new FileWriter(file, true)){
            w.write(format(set));
        }
    }

    public synchronized List<SavedSet> loadAll() throws IOException{
        List<SavedSet> sets = new ArrayList<>();
        if(!file.exists()){
            return sets;
        }
        long ts = 0;
        Lift lift = null;
        double load = 0;
        List<Rep> reps = new ArrayList<>();
        boolean open = false;
        try(BufferedReader in = new BufferedReader(new FileReader(file))){
            String line;
            while((line = in.readLine()) != null){
                String[] p = line.split("\\|", -1);
                try{
                    if(p[0].equals("S") && p.length == 4){
                        if(open){
                            sets.add(new SavedSet(ts, lift, load, reps));
                            open = false;
                        }
                        long newTs = Long.parseLong(p[1]);
                        Lift newLift = Lift.valueOf(p[2]);
                        double newLoad = Double.parseDouble(p[3]);
                        ts = newTs;
                        lift = newLift;
                        load = newLoad;
                        reps = new ArrayList<>();
                        open = true;
                    }else if(p[0].equals("R") && p.length == 8 && open){
                        reps.add(new Rep(Integer.parseInt(p[1]), Double.parseDouble(p[2]),
                                Double.parseDouble(p[3]), Double.parseDouble(p[4]),
                                Double.parseDouble(p[5]),  Double.parseDouble(p[6]),
                                Double.parseDouble(p[7])));
                    }
                }catch (IllegalArgumentException ignored){

                }
            }
        }
        if(open){
            sets.add(new SavedSet(ts, lift, load, reps));
        }
        return sets;
    }

    public synchronized void delete(long timestampMs) throws IOException{
        List<SavedSet> all = loadAll();
        File tmp = new File(file.getPath() + ".tmp");
        try(FileWriter w = new FileWriter(tmp, false)){
            for(SavedSet s : all){
                if(s.timestampMs != timestampMs){
                    w.write(format(s));
                }
            }
        }
        if(!tmp.renameTo(file)){
            if(!file.delete() || !tmp.renameTo(file)){
                throw new IOException("Could not replace " + file);
            }
        }
    }

    private static String format(SavedSet set){
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "S|%d|%s|%.2f%n", set.timestampMs,
                set.lift.name(), set.loadKg));
        for(Rep r : set.reps){
            sb.append(String.format(Locale.US, "R|%d|%.4f|%.4f|%.4f|%.4f|%.2f|%.4f%n",
                    r.number, r.startS, r.endS, r.meanVelocity, r.peakVelocity, r.meanGyro, r.romM));
        }
        return sb.toString();
    }
}
