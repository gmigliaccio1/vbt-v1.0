package com.example.vbt.ble;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

public final class BlePermissions {
    private BlePermissions(){
    }
    public static String[] required(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            return new String[]{Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT};
        }
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
    }

    public static boolean hasAll(Context context){
        for(String p : required()){
            if(ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }
}
