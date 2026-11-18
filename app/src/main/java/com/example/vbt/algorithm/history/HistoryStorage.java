package com.example.vbt.algorithm.history;

import android.content.Context;

import java.io.File;

public final class HistoryStorage {
    private HistoryStorage(){

    }

    public static SetHistoryStore open(Context context){
        return new SetHistoryStore(new File(context.getApplicationContext().getFilesDir(), "sets.txt"));
    }
}
