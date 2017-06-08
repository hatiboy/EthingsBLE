package demo.ethings.com.ethingsble.activity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.chimeraiot.android.ble.BleService;
import com.chimeraiot.android.ble.BleServiceBindingActivity;

import demo.ethings.com.ethingsble.R;
import service.BleSensorService;

public class FindDeviceActivity extends BleServiceBindingActivity {

    private static final String TAG = FindDeviceActivity.class.getName();
    private BluetoothDevice bluetoothDevice;

    @Override
    public Class<? extends BleService> getServiceClass() {
        return BleSensorService.class;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_device);

        //noinspection ConstantConditions
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        final String deviceName = getDeviceName();
        if (TextUtils.isEmpty(deviceName)) {
            //noinspection ConstantConditions
            actionBar.setTitle(getDeviceAddress());
        } else {
            //noinspection ConstantConditions
            actionBar.setTitle(deviceName);
            actionBar.setSubtitle(getDeviceAddress());
        }
        actionBar.setDisplayHomeAsUpEnabled(true);
        Intent bundle = getIntent();
        try {
            if (bundle != null)
                bluetoothDevice = bundle.getParcelableExtra("device");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    BluetoothGatt bluetoothGatt;

    public void toggleClick(View view) {
        byte[] config = new byte[]{1};
        String port64 = "F000AA64-0451-4000-B000-000000000000";
        String port65 = "0xAA65";
        String port66 = "F000AA66-0451-4000-B000-000000000000";
//        BluetoothGattService bluetoothGattService = new BluetoothGattService(UUID.fromString(port66), 1);
//        List<BluetoothGattCharacteristic> b = bluetoothGattService.getCharacteristics();
//        BluetoothGattCharacteristic bluetoothGattCharacteristic = b.get(0);
//        bluetoothGattCharacteristic.setValue(config);
        if (bluetoothDevice == null) {
            return;
        }
        bluetoothGatt = bluetoothDevice.connectGatt(getApplicationContext(), true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                String address = gatt.getDevice().getAddress();

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Attempting to start service discovery:" + gatt.discoverServices());
                    gatt.discoverServices();

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // onDisconnected(gatt);
                    Log.i(TAG, "Disconnected from GATT server.");
                }
            }

        });
//        BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(port64));
//        if(bluetoothGattService == null){
//            Log.d(TAG, "toggleClick: service not found");
//            return;
//        }
//        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(port66));
//        if(bluetoothGattCharacteristic == null){
//            Log.d(TAG, "toggleClick: find characteristic error");
//            return;
//        }
//        bluetoothGattCharacteristic.setValue(config);
//        if(!bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)){
//            Log.d(TAG, "toggleClick: write charateristic fail");
//        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDisconnected(final String name, final String address) {
        super.onDisconnected(name, address);
        finish();
    }

    @Override
    public void onServiceDiscovered(final String name, final String address) {
//        super.onServiceDiscovered(name, address);
        if (bluetoothGatt == null) return;
        for (BluetoothGattService blueToothGattService : bluetoothGatt.getServices()) {
            if (blueToothGattService.getUuid().toString().contains("64")) {
                for (BluetoothGattCharacteristic characteristic : blueToothGattService.getCharacteristics()) {
                    if (characteristic.getUuid().toString().contains("66")) {
//                        bluetoothGatt.setCharacteristicNotification(characteristic, true);
//                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(HEART_RATE_VALUE_CHAR_DESC_ID));
//                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                        return bluetoothGatt.writeDescriptor(descriptor);
                        byte[] config = new byte[]{0 , 1};
                        characteristic.setValue(config);
                        bluetoothGatt.writeCharacteristic(characteristic);
                    }
                }
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        super.onServiceDisconnected(name);
        Log.d(TAG, "onServiceDisconnected: name: " + name.toString());
    }

    @Override
    public void onCharacteristicChanged(String name, String address, String serviceUuid, String characteristicUuid) {
        super.onCharacteristicChanged(name, address, serviceUuid, characteristicUuid);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothGatt != null)
            bluetoothGatt.disconnect();
    }
}
