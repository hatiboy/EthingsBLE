package demo.ethings.com.ethingsble.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import demo.ethings.com.ethingsble.R;
import demo.ethings.com.ethingsble.activity.BLESQLiteHelper;
import demo.ethings.com.ethingsble.activity.ListTagActivity;
import demo.ethings.com.ethingsble.model.TagDevice;
import demo.ethings.com.ethingsble.singleton.EthingSingleTon;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private String DEVICE_ADDRESS;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public static final String ACTION_READ_RSSI = "com.ething.ble.tag.device.read.rssi";
    public static final String ACTION_INTENT_DEVICE_ADDRESS = "com.ething.ble.tag.device.address";
    public static final String ACTION_INTENT_DEVICE_RSSI = "com.ething.ble.tag.device.rssi";

    public static final String ACTION_CALL_DISCONNECT = "com.ething.ble.tag.device.disconnect.gatt";
    public static final String ACTION_CALL_CONNECT = "com.ething.ble.tag.device.connect.gatt";
    public static final String ACTION_WRITE_65 = "com.ething.ble.tag.device.write65";

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(HEART_RATE_MEASUREMENT);
    private String uuidNotifiService = "0000FFE0-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_NOTIFICATION_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private String uuidNotifiCharacteristic = "0000ffe1-0000-1000-8000-00805f9b34fb";


    public ArrayList<String> deviceAddress;
    public static final int NOTIFI_STATE_ID = 158;
    private BLESQLiteHelper helper;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
//    private final BluetoothGattCallback mGattCallback =

    private void broadcastUpdateState(final String action, String address) {
        final Intent intent = new Intent(action);
        intent.putExtra(ACTION_INTENT_DEVICE_ADDRESS, address);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdateRssi(String action, String address, int rssi) {
        final Intent intent = new Intent(action);
        intent.putExtra(ACTION_INTENT_DEVICE_ADDRESS, address);
        intent.putExtra(ACTION_INTENT_DEVICE_RSSI, rssi);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        helper = new BLESQLiteHelper(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                deviceAddress = bundle.getStringArrayList(ListTagActivity.INTENT_EXTRA_LIST_TAG_DEVICE_ADDRESS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert deviceAddress != null;
        for (int i = 0; i < deviceAddress.size(); i++) {
            Log.d(TAG, "onStartCommand: device address: " + deviceAddress);
            if (initialize())
                connect(deviceAddress.get(i));
        }

        //register Broadcast
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_CALL_DISCONNECT);
        intentFilter.addAction(ACTION_CALL_CONNECT);
        intentFilter.addAction(ACTION_WRITE_65);
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        LocalBroadcastManager.getInstance(this).registerReceiver(controllReceiver, intentFilter);

        //start timer to read remote rssi every 3s
        startTimer();

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        //unregister receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(controllReceiver);
        //send broadcast to restart demo.ethings.com.ethingsble.service
        Intent broadcastIntent = new Intent("ething.ble.BroadcastRestartService");
        sendBroadcast(broadcastIntent);
        //cancel timer
        super.onDestroy();
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth demo.ethings.com.ethingsble.adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                String intentAction;
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    intentAction = ACTION_GATT_CONNECTED;
                    mConnectionState = STATE_CONNECTED;
                    //send broadcast to listtag activity
                    broadcastUpdateState(intentAction, device.getAddress());
                    //save state device in database
                    setStateDevice(gatt.getDevice(), true);
                    //show notification
                    String title = "Device connected";
                    String message = "Your device : " + device.getName() + " is connected";
                    showNotification(title, message, false, NOTIFI_STATE_ID);

                    Log.i(TAG, "Connected to GATT server.");
                    // Attempts to discover services after successful connection.
                    Log.i(TAG, "Attempting to start demo.ethings.com.ethingsble.service discovery:" +
                            mBluetoothGatt.discoverServices());

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    intentAction = ACTION_GATT_DISCONNECTED;
                    mConnectionState = STATE_DISCONNECTED;
                    Log.i(TAG, "Disconnected from GATT server.");
                    broadcastUpdateState(intentAction, device.getAddress());
                    //save state device in database
                    setStateDevice(gatt.getDevice(), false);
                    check = false;
                    //show Notification
                    String title = "Device disconnect";
                    String message = "Your device : " + device.getName() + " is not in your range or disconnect";
                    showNotification(title, message, true, NOTIFI_STATE_ID);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//                if (status == BluetoothGatt.GATT_SUCCESS) {
//                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
//                } else {
//                    Log.w(TAG, "onServicesDiscovered received: " + status);
//                }
                if (status == BluetoothGatt.GATT_SUCCESS) {
//                    readPinLevel();
                    write65(true);
                } else if (status == BluetoothGatt.GATT_FAILURE) {
                    Toast.makeText(getApplicationContext(), "Service discovered fail", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic,
                                             int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic) {
//                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                if (characteristic.getUuid().toString().equals(uuidNotifiCharacteristic)) {
                    showNotification("Ethings", gatt.getDevice().getName() + " is finding your phone", true, 99);
                    gatt.readRemoteRssi();
                }
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, int status) {
//                super.onReadRemoteRssi(gatt, rssi, status);
                Log.d(TAG, "onReadRemoteRssi: " + rssi);
                if (BluetoothGatt.GATT_SUCCESS == status) {
                    broadcastUpdateRssi(ACTION_READ_RSSI, device.getAddress(), rssi);
                }
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
        });
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }


    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    //remote led:
    byte[] config;
    private boolean check = true;

    private String port66 = "f000aa66-0451-4000-b000-000000000000";
    private String port65 = "f000aa65-0451-4000-b000-000000000000";
    private String port64 = "F000AA64-0451-4000-B000-000000000000";


    private void write65(boolean b) {
        try {
            BluetoothGattService bluetoothGattService = mBluetoothGatt.getService(UUID.fromString(port64));
            BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(port65));
            if (b) config = new byte[]{(byte) 1};
            else config = new byte[]{(byte) 0};
            bluetoothGattCharacteristic.setValue(config);
            mBluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
            EthingSingleTon.getInstance().setLedState(b);
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
    //save state of tag
    private void setStateDevice(BluetoothDevice bluetoothDevice, boolean state) {
        TagDevice device = new TagDevice();
        device.setAddress(bluetoothDevice.getAddress());
        device.setName(bluetoothDevice.getName());
        device.setRSSI(0);
        device.setPin(72);
        device.setState(state);
        helper.insertDevice(device);
    }

    private Handler mHandler = new Handler();
    public void startTimer() {
        mHandler.postDelayed(updateTask, 3000);
    }

    private Runnable updateTask = new Runnable() {
        public void run() {
            Log.d(getString(R.string.app_name) + " ChatList.updateTask()",
                    "updateTask run!");
            mBluetoothGatt.readRemoteRssi();
            // queue the task to run again in 3 seconds...
            mHandler.postDelayed(updateTask, 3000);


        }
    };
    BroadcastReceiver controllReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_CALL_DISCONNECT:
                    mBluetoothGatt.disconnect();
                    mBluetoothGatt.close();
                    break;
                case ACTION_CALL_CONNECT:
                    mBluetoothGatt.connect();
                    break;
                case ACTION_WRITE_65:
                    boolean led_state = intent.getExtras().getBoolean("state");
                    Log.d(TAG, "ACTION_WRITE_65 " + led_state);
                    write65(led_state);
                    break;
                case Intent.ACTION_TIME_TICK:
                    if (mBluetoothGatt != null)
                        mBluetoothGatt.readRemoteRssi();
                    break;
            }
        }
    };

}
