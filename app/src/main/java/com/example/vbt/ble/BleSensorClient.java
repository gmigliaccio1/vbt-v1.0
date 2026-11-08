package com.example.vbt.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

import java.util.Collections;

@SuppressLint("MissingPermission")
public final class BleSensorClient {
    public enum State {DISCONNECTED, SCANNING, CONNECTING, CONNECTED};
    public interface Listener{
        void onStateChange(State state, String detail);
        void onData(byte[] packet);
    }
    private static final long SCAN_TIMEOUT_MS = 15_000;
    private final Context context;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile State state = State.DISCONNECTED;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private boolean userRequestDisconnect;
    public BleSensorClient(Context context, Listener listener){
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public State getState(){
        return state;
    }

    public void connect(){
        if(state != State.DISCONNECTED){
            return;
        }
        if(!BlePermissions.hasAll(context)){
            publish(State.DISCONNECTED, "Bluetooth permission missing");
            return;
        }
        BluetoothManager manager = (BluetoothManager)
                context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter;
        if(manager == null){
            adapter = null;
        }else{
            adapter = manager.getAdapter();
        }
        if(adapter == null || !adapter.isEnabled()){
            publish(State.DISCONNECTED, "BLE scanner not available");
            return;
        }
        userRequestDisconnect = false;
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(SensorProtocol.SERVICE_UUID))
                .build();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        publish(State.SCANNING, "Looking for the sensor...");
        scanner.startScan(Collections.singletonList(filter), settings, scanCallback);
        main.postDelayed(scanTimeout, SCAN_TIMEOUT_MS);
    }

    public void disconnect(){
        userRequestDisconnect = true;
        stopScan();
        if(gatt != null){
            gatt.disconnect();
        }else{
            publish(State.DISCONNECTED, "Disconnected");
        }
    }

    private final Runnable scanTimeout = new Runnable() {
        @Override
        public void run() {
            if(state == State.SCANNING){
                stopScan();
                publish(State.DISCONNECTED, "Sensor not found. Is it on and in range?");
            }
        }
    };

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (state != State.SCANNING) {
                return;
            }
            stopScan();
            publish(State.CONNECTING, "Connecting to " + result.getDevice().getAddress());
            gatt = result.getDevice().connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        }

        @Override
        public void onScanFailed(int errorCode) {
            publish(State.DISCONNECTED, "Scan failed (code " + errorCode + ")");
        }
    };

    private void stopScan() {
        main.removeCallbacks(scanTimeout);
        if (scanner != null) {
            try {
                scanner.stopScan(scanCallback);
            } catch (IllegalStateException ignored) {
                // Bluetooth was switched off meanwhile
            }
        }
    }
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                g.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                if (!g.requestMtu(SensorProtocol.REQUESTED_MTU)) {
                    g.discoverServices();
                }
            } else {
                g.close();
                gatt = null;
                String why = userRequestedDisconnect ? "Disconnected"
                        : "Connection lost (status " + status + ")";
                publish(State.DISCONNECTED, why);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt g, int mtu, int status) {
            g.discoverServices();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt g, int status) {
            BluetoothGattService service = g.getService(SensorProtocol.SERVICE_UUID);
            BluetoothGattCharacteristic data = service == null ? null : service.getCharacteristic(SensorProtocol.DATA_UUID);
            BluetoothGattDescriptor cccd = data == null ? null : data.getDescriptor(SensorProtocol.CCCD_UUID);
            if (status != BluetoothGatt.GATT_SUCCESS || cccd == null) {
                publish(State.CONNECTING, "Sensor service not found");
                g.disconnect();
                return;
            }
            g.setCharacteristicNotification(data, true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                g.writeDescriptor(cccd);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt g, BluetoothGattDescriptor descriptor,
                                      int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                publish(State.CONNECTED, "Streaming");
            } else {
                publish(State.CONNECTING, "Could not enable notifications (status "
                        + status + ")");
                g.disconnect();
            }
        }

        // Android 13+ calls this overload ...
        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c,
                                            byte[] value) {
            listener.onData(value);
        }

        // ... older versions call this one.
        @SuppressWarnings("deprecation")
        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) {
            listener.onData(c.getValue());
        }
    };

    private void publish(final State newState, final String detail) {
        state = newState;
        main.post(new Runnable() {
            @Override
            public void run() {
                listener.onStateChanged(newState, detail);
            }
        });
    }
}
