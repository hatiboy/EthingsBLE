package demo.ethings.com.ethingsble.activity;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.chimeraiot.android.ble.BleService;
import com.chimeraiot.android.ble.BleServiceBindingActivity;
import com.chimeraiot.android.ble.BleUtils;

import java.util.UUID;

import demo.ethings.com.ethingsble.R;
import model.TagDevice;
import service.BleSensorService;
import ui.ErrorDialog;

public class FindDeviceActivity extends BleServiceBindingActivity {

    //    private ToggleButton tg_find66;
    private ToggleButton tg_find65;
    private ImageButton ib_led;
    private ToggleButton tg_connect;
    private BLESQLiteHelper helper;
    private static final String TAG = FindDeviceActivity.class.getName();
    private BluetoothDevice bluetoothDevice;
    private BluetoothGatt bluetoothGatt;
    private String port64 = "F000AA64-0451-4000-B000-000000000000";
    //    private String port65 = "F000AA65-0451-4000-B000-000000000000";
    private String batteryService = "0x180f";
    private String port66 = "f000aa66-0451-4000-b000-000000000000";
    private String port65 = "f000aa65-0451-4000-b000-000000000000";
    //    private String uuidNotifiService  = "F000FFE0-0451-4000-B000-000000000000";
    private String uuidNotifiService = "0000FFE0-0000-1000-8000-00805f9b34fb";
    //    private String uuidNotifiCharacteristic = "F000ffe1-0451-4000-B000-000000000000";
    private String uuidNotifiCharacteristic = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_NOTIFICATION_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
//    private static final String CHARACTERISTIC_NOTIFICATION_CONFIG = "F0002902-0451-4000-B000-000000000000";

    ProgressDialog progressDialog;
    private boolean check = true;
    private String batteryPort = "00002a19-0000-1000-8000-00805f9b34fb";
    private int pin = 0;
    private BluetoothAdapter bluetoothAdapter;
    private boolean led_state;

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

//        tg_find66 = (ToggleButton) findViewById(R.id.tg_find66);
        tg_find65 = (ToggleButton) findViewById(R.id.tg_find65);
        ib_led = (ImageButton) findViewById(R.id.ib_led);
//
//        tg_find66.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//                write66(b);
//            }
//        });

        tg_find65.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                write65(b);
            }
        });
        ib_led.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tg_connect.isChecked()) {
                    write65(!led_state);
                    led_state = !led_state;
                    if (led_state) ib_led.setImageResource(R.drawable.led_on);
                    else ib_led.setImageResource(R.drawable.led_off);
                } else {
                    Toast.makeText(getApplicationContext(), "connect device first", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //noinspection ConstantConditions
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tg_connect = (ToggleButton) toolbar.findViewById(R.id.tg_connect);
        tg_connect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                    bluetoothGatt = getBluetoothGatt();
//                bluetoothGatt.getConnec
                else if (bluetoothGatt != null)
                    try {
                        bluetoothGatt.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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

        //get Adapter
        final int bleStatus = BleUtils.getBleStatus(getBaseContext());
        switch (bleStatus) {
            case BleUtils.STATUS_BLE_NOT_AVAILABLE:
                ErrorDialog.newInstance(R.string.dialog_error_no_ble)
                        .show(getFragmentManager(), ErrorDialog.TAG);
                return;
            case BleUtils.STATUS_BLUETOOTH_NOT_AVAILABLE:
                ErrorDialog.newInstance(R.string.dialog_error_no_bluetooth)
                        .show(getFragmentManager(), ErrorDialog.TAG);
                return;
            default:
                bluetoothAdapter = BleUtils.getBluetoothAdapter(getBaseContext());
        }

        if (bluetoothAdapter == null) {
            return;
        }

        Intent bundle = getIntent();
        String address;
        try {
            if (bundle != null) {
                address = bundle.getStringExtra(FindDeviceActivity.EXTRAS_DEVICE_ADDRESS);
                bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void write65(boolean b) {
        try {
            BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(port64));
            BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(port65));
            if (b) config = new byte[]{(byte) 1};
            else config = new byte[]{(byte) 0};
            bluetoothGattCharacteristic.setValue(config);
            bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void write66(boolean b) {
        try {
            if (bluetoothGatt != null) {
                try {
                    BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(port64));
                    BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(port66));
                    if (b) config = new byte[]{(byte) 1};
                    else config = new byte[]{(byte) 0};
                    bluetoothGattCharacteristic.setValue(config);
                    bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    byte[] config;

    public void toggleNotificationClick(boolean state) {
        //turn on notification
        BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(uuidNotifiService));
        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(uuidNotifiCharacteristic));
        bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true); //If so then enable notification in the BluetoothGatt
        BluetoothGattDescriptor descriptor = bluetoothGattCharacteristic.getDescriptor(UUID.fromString(CHARACTERISTIC_NOTIFICATION_CONFIG)); //Get the descripter that enables notification on the server
        if (state)
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); //Set the value of the descriptor to enable notification
        else
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(descriptor);//Write the descriptor
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();
            Toast.makeText(getApplicationContext(), "Cannot connect device", Toast.LENGTH_SHORT).show();
            finish();
        }
    };

    public BluetoothGatt getBluetoothGatt() {
        if (bluetoothDevice == null) {
            return null;
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.show();
//        mHandler.sendMessageDelayed(new Message(), 10000);
        return bluetoothDevice.connectGatt(getApplicationContext(), true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                String address = gatt.getDevice().getAddress();
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Attempting to start service discovery:" + gatt.discoverServices());
                    gatt.discoverServices();
                    setStateDevice(true);
                    progressDialog.dismiss();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from GATT server.");
                    //Toast.makeText(getApplicationContext(), "Disconnected from GATT server", Toast.LENGTH_SHORT).show();
                    String title = "Device disconnect";
                    String message = "Your device : " + bluetoothDevice.getName() + " is not in your range or disconnect";
//                    showNotification(title, message, true);
                    setStateDevice(false);
//                    finish();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                // super.onServicesDiscovered(gatt, status);
                Log.d(TAG, "onServicesDiscovered: " + status);
                progressDialog.dismiss();
                if (status == BluetoothGatt.GATT_SUCCESS) {
//                    readPinLevel();
                    write65(true);
                    tg_find65.setChecked(true);
                    led_state = true;
                    ib_led.setImageResource(R.drawable.led_on);
//                    write66(true);
                } else if (status == BluetoothGatt.GATT_FAILURE) {
                    Toast.makeText(getApplicationContext(), "Service discovered fail", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//                super.onCharacteristicChanged(gatt, characteristic);
//                if (characteristic.getValue() != null) {
//                    showNotification("Ethings", "Finding your phone", true);
//                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//                super.onCharacteristicWrite(gatt, characteristic, status);
                if (characteristic.getUuid().toString().equals(port65)) {
                    if (check && status == BluetoothGatt.GATT_SUCCESS) {
                        write66(true);
                        check = false;
                    }
                } else if (characteristic.getUuid().toString().equals(port66)) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        toggleNotificationClick(true);
                    }
                }
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
//                super.onReadRemoteRssi(gatt, rssi, status);
                Log.d(TAG, "onReadRemoteRssi: " + rssi);
            }


            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//                super.onCharacteristicRead(gatt, characteristic, status);
                if (status == BluetoothGatt.GATT_SUCCESS && characteristic.getUuid().toString().equals(batteryService)) {
                    Log.d(TAG, "onCharacteristicRead value: " + characteristic.getValue());
                }
            }
        });
    }

    private void readPinLevel() {
        try {
            BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(batteryService));
            BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(batteryPort));
            bluetoothGatt.readCharacteristic(bluetoothGattCharacteristic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setStateDevice(boolean state) {
        TagDevice device = new TagDevice();
        device.setAddress(bluetoothDevice.getAddress());
        device.setName(bluetoothDevice.getName());
        device.setRSSI(0);
        device.setPin(pin);
        device.setState(state);
        helper.insertDevice(device);
    }

    private void unsaveDevice() {
        helper.deleteDevice(bluetoothDevice.getAddress());
    }

    public void showNotification(String title, String message, boolean vibaration, int id) {
        if (vibaration) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            v.vibrate(500);
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(FindDeviceActivity.this)
                .setSmallIcon(R.mipmap.ething_ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.logo_blue))
                .setContentTitle(title)
                .setContentText(message);
        // Sets an ID for the notification
        int mNotificationId = 1;
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
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
//        if (bluetoothGatt != null)
//            bluetoothGatt.disconnect();
    }
}