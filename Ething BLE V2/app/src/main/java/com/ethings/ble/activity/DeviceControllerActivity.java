package com.ethings.ble.activity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.chimeraiot.android.ble.BleService;
import com.chimeraiot.android.ble.BleServiceBindingActivity;
import com.ethings.ble.R;
import com.ethings.ble.model.TagDevice;
import com.ethings.ble.service.BleSensorService;
import com.ethings.ble.util.Constants;

import java.util.UUID;


public class DeviceControllerActivity extends BleServiceBindingActivity {

    private ToggleButton tg_connect;
    private BLESQLiteHelper helper;
    private static final String TAG = DeviceControllerActivity.class.getName();
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private byte[] config;
    private int pinlevel;
    private int mRssi = 0;

    private String port66 = "f000aa66-0451-4000-b000-000000000000";
    private String port65 = "f000aa65-0451-4000-b000-000000000000";
    private String port64 = "F000AA64-0451-4000-B000-000000000000";
    private String Battery_Service_UUID = "0000180F-0000-1000-8000-00805f9b34fb";
    private static final UUID Battery_Level_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    private String uuidNotifiService = "0000FFE0-0000-1000-8000-00805f9b34fb";
    private String uuidNotifiCharacteristic = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_NOTIFICATION_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    private ProgressDialog progressDialog;
    private ToggleButton tg_led;

    @Override
    public Class<? extends BleService> getServiceClass() {
        return BleSensorService.class;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_device);

        //init sqlite helper
        helper = new BLESQLiteHelper(this);

        //noinspection ConstantConditions
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tg_connect = toolbar.findViewById(R.id.tg_connect);
        tg_connect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                    connect();
                else if (mBluetoothGatt != null)
                    try {
                        mBluetoothGatt.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            }
        });
        tg_led = findViewById(R.id.tg_led);
        tg_led.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                write65(isChecked);
            }
        });

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
        boolean isConnected = false;
        try {
            if (bundle != null) {
                mBluetoothDevice = bundle.getParcelableExtra(Constants.EXTRAS_BLUETOOTH_DEVICE);
                isConnected = bundle.getBooleanExtra(Constants.EXTRAS_DEVICE_IS_CONNECTED, false);
                tg_connect.setChecked(isConnected);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (isConnected) {
            connect();
        }
    }


    public void connect() {
        if (mBluetoothDevice == null) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.connection_fail), Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.show();
        mBluetoothGatt = mBluetoothDevice.connectGatt(getApplicationContext(), true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Attempting to start service discovery:" + gatt.discoverServices());
                    gatt.discoverServices();
                    progressDialog.dismiss();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from GATT server.");
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                // super.onServicesDiscovered(gatt, status);
                Log.d(TAG, "onServicesDiscovered: " + status);
                progressDialog.dismiss();
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    setStateDevice(true);
                    write65(true);
                } else if (status == BluetoothGatt.GATT_FAILURE) {
                    setStateDevice(false);
                    Toast.makeText(getApplicationContext(), "Service discovered fail", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                Log.d(TAG, "onCharacteristicChanged: characteristic.getValue()[0]" + characteristic.getValue()[0]);
                if (characteristic.getValue()[0] == 1) {
                    showNotification("Ethings", gatt.getDevice().getName() + " is finding your phone", true, 99);
                    gatt.readRemoteRssi();
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                Log.d(TAG, "onCharacteristicWrite: characteristic" + characteristic.getUuid() + " status: " + status);
                if (characteristic.getUuid().toString().equals(port65)) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        write66(true);
                    }
                } else if (characteristic.getUuid().toString().equals(port66)) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        toggleNotificationClick(true);
                    }
                }
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                super.onReadRemoteRssi(gatt, rssi, status);
                Log.d(TAG, "onReadRemoteRssi: " + rssi);
                if (BluetoothGatt.GATT_SUCCESS == status) {
                    mRssi = rssi;
                    getbattery();
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                if (status == BluetoothGatt.GATT_SUCCESS && characteristic.getUuid().equals(Battery_Level_UUID)) {
                    pinlevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    Log.d(TAG, "onCharacteristicRead: pinlevel" + pinlevel);
                }
            }
        });
    }

    private void write65(boolean b) {
        try {
            tg_led.setChecked(b);
            if (mBluetoothGatt == null) connect();
            BluetoothGattService bluetoothGattService = mBluetoothGatt.getService(UUID.fromString(port64));
            BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(port65));
            if (b) config = new byte[]{(byte) 1};
            else config = new byte[]{(byte) 0};
            bluetoothGattCharacteristic.setValue(config);
            mBluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void write66(boolean b) {
        try {
            if (mBluetoothGatt != null) {
                try {
                    BluetoothGattService bluetoothGattService = mBluetoothGatt.getService(UUID.fromString(port64));
                    BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(port66));
                    if (b) config = new byte[]{(byte) 1};
                    else config = new byte[]{(byte) 0};
                    bluetoothGattCharacteristic.setValue(config);
                    mBluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*config notification demo.ethings.com.ethingsble.service of tag*/
    public void toggleNotificationClick(boolean state) {
        //turn on notification
        BluetoothGattService bluetoothGattService = mBluetoothGatt.getService(UUID.fromString(uuidNotifiService));
        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(uuidNotifiCharacteristic));
        mBluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true); //If so then enable notification in the BluetoothGatt
        BluetoothGattDescriptor descriptor = bluetoothGattCharacteristic.getDescriptor(UUID.fromString(CHARACTERISTIC_NOTIFICATION_CONFIG)); //Get the descripter that enables notification on the server
        if (state)
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); //Set the value of the descriptor to enable notification
        else
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);//Write the descriptor
    }

    /*code xu ly notification*/
    public void showNotification(String title, String message, boolean vibaration, int notificationid) {
        if (vibaration) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            v.vibrate(300);
        }
        Intent intent = new Intent(this, ListTagActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, notificationid, intent, 0);
        Notification mBuilder = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.logo_blue)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.logo_blue))
                .setStyle(new Notification.BigTextStyle()
                        .bigText(message))
                .build();

        // Sets an ID for the notification
        // Gets an instance of the NotificationManager demo.ethings.com.ethingsble.service
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(notificationid + 158, mBuilder);
    }

    /*read pin level*/
    public void getbattery() {
        BluetoothGattService batteryService = mBluetoothGatt.getService(UUID.fromString(Battery_Service_UUID));
        if (batteryService == null) {
            Log.d(TAG, "Battery service not found!");
            return;
        }

        BluetoothGattCharacteristic batteryLevel = batteryService.getCharacteristic(Battery_Level_UUID);
        if (batteryLevel == null) {
            Log.d(TAG, "Battery level not found!");
            return;
        }
        mBluetoothGatt.readCharacteristic(batteryLevel);
        Log.v(TAG, "batteryLevel = " + mBluetoothGatt.readCharacteristic(batteryLevel));
    }

    private void setStateDevice(boolean state) {
        TagDevice device = new TagDevice();
        device.setAddress(mBluetoothDevice.getAddress());
        device.setName(mBluetoothDevice.getName());
        device.setRSSI(mRssi);
        device.setPin(pinlevel);
        device.setState(state);
        helper.insertDevice(device);
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
}