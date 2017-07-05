package demo.ethings.com.ethingsble.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.chimeraiot.android.ble.BleScanner;
import com.chimeraiot.android.ble.BleUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import adapter.BleDevicesAdapter;
import adapter.TagDeviceAdapter;
import demo.ethings.com.ethingsble.R;
import model.TagDevice;
import service.BluetoothLeService;
import ui.DeviceScanActivity;
import ui.ErrorDialog;

public class ListTagActivity extends Activity  {

    private static final String TAG = ListTagActivity.class.getName();
    private ListView listTag;
    private Button addnewtag;
    private ArrayList<TagDevice> list_tag_devices;
    private ArrayList<String> list_tag_address;
    BLESQLiteHelper helper;
    private BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
    private Set<BluetoothDevice> boundedAdapter = BTAdapter.getBondedDevices();
    private TagDeviceAdapter adapter;
    private BleScanner scanner;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice currentDevice;
    protected SwipeRefreshLayout refresh;
    /**
     * Request to enable Bluetooth.
     */
    private static final int REQUEST_ENABLE_BT = 1;
    public OnItemTagDeviceDeleteListener onItemTagDeviceDeleteListener;

    /**
     * Scan delay period.
     */
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final long SCAN_PERIOD = 3000L;
    private BleDevicesAdapter leDeviceListAdapter;
    private List<BluetoothDevice> connectedDevices;

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
    //    public final static String INTENT_EXTRA_DEVICE_ADDRESS = "device_address";
    public static final String INTENT_EXTRA_LIST_TAG_DEVICE_ADDRESS = "list_tag_devices";

//    private static final String CHARACTERISTIC_NOTIFICATION_CONFIG = "F0002902-0451-4000-B000-000000000000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
        setContentView(R.layout.activity_list_tag);

        listTag = (ListView) findViewById(R.id.list_tag);
        addnewtag = (Button) findViewById(R.id.btn_add_new_tag);
        refresh = (SwipeRefreshLayout) findViewById(R.id.sw_refresh);
        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                scanner.start();
                adapter.notifyDataSetChanged();
            }
        });

        helper = new BLESQLiteHelper(this);

        list_tag_devices = new ArrayList<TagDevice>();
        list_tag_address = new ArrayList<>();
//        ArrayList<TagDevice> devicess = helper.getAllDevices();

//        list_tag_devices.add(new TagDevice("24:71:89:E6:78:06", "Bag", 4 , 100, false));

        addnewtag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ListTagActivity.this, DeviceScanActivity.class);
                startActivity(i);
            }
        });
        listTag.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                TagDevice tag = list_tag_devices.get(position);
                if(tag.getState()){
                    final Intent intent = new Intent(ListTagActivity.this, FindDeviceActivity.class);
                    intent.putExtra(FindDeviceActivity.EXTRAS_DEVICE_ADDRESS, tag.getAddress());
                    startActivity(intent);
                }else{
                    Toast.makeText(getApplicationContext(), "device not connected", Toast.LENGTH_SHORT).show();
                }

            }
        });

        listTag.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int position, long l) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(ListTagActivity.this);
                dialog.setMessage("");
                dialog.setNeutralButton("Find", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                dialog.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        helper.deleteDevice(list_tag_devices.get(position));
                        list_tag_devices.remove(position);
                        adapter.notifyDataSetChanged();
                        //send data to Service to delete
                    }
                });
                dialog.show();
                return false;
            }
        });


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

        // initialize scanner
        scanner = new BleScanner(bluetoothAdapter, new ScanProcessor());
        scanner.setScanPeriod(SCAN_PERIOD);
        scanner.start();

        requestLocation();


    }


    // Also handle calls to onNewIntent.
    @Override
    protected void onNewIntent(Intent intent) {
        loadData(intent);
    }

    private void loadData(Intent intent) {
        list_tag_devices = helper.getAllDevices();
        adapter = new TagDeviceAdapter(this, R.layout.item_tag_device, list_tag_devices);
        listTag.setAdapter(adapter);
        if (list_tag_devices != null && list_tag_devices.size() > 0) {
            for (int i = 0; i < list_tag_devices.size(); i++) {
                list_tag_address.add(list_tag_devices.get(i).getAddress());
            }
//            String deviceAddress = list_tag_devices.get(0).getAddress();
            Intent i = new Intent(ListTagActivity.this, BluetoothLeService.class);
//            i.putExtra(INTENT_EXTRA_DEVICE_ADDRESS, deviceAddress);
            i.putStringArrayListExtra(INTENT_EXTRA_LIST_TAG_DEVICE_ADDRESS, list_tag_address);
            startService(i);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadData(getIntent());
        //config Bluetooth Adapter:
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_READ_RSSI);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
        BTAdapter.startDiscovery();
        //notify data
//        list_tag_devices = helper.getAllDevices();
        adapter.notifyDataSetChanged();

//        tryconnect();

        if (leDeviceListAdapter == null) {
            leDeviceListAdapter = new BleDevicesAdapter(getBaseContext());
        }


    }

    private void tryconnect() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        for (BluetoothDevice device : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
//            int type = device.getType();

//            if (type == BluetoothDevice.DEVICE_TYPE_LE || type == BluetoothDevice.DEVICE_TYPE_DUAL) {
        connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        for (final BluetoothDevice tag : connectedDevices) {
//            connectDevice(tag);
        }
        loops:
        for (int i = 0; i < list_tag_devices.size(); i++) {
            for (final BluetoothDevice tag : connectedDevices) {
                if (tag.getAddress().equals(list_tag_devices.get(i).getAddress())) {
                    list_tag_devices.get(i).setState(true);
                    list_tag_devices.get(i).setRSSI(0);
                    adapter.notifyDataSetChanged();
                    break loops;
                }
            }
            list_tag_devices.get(i).setState(false);
        }
    }

    private void setStateDevice(BluetoothDevice device, boolean state) {
        for (int i = 0; i < list_tag_devices.size(); i++) {
            if (device.getAddress().equals(list_tag_devices.get(i).getAddress())) {
                if (state) {
                    list_tag_devices.get(i).setState(true);
//                    ListTagActivity.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
                    adapter.notifyDataSetChanged();
//                        }
//                    });

                } else {
                    TagDevice tag = list_tag_devices.get(i);
                    tag.setState(false);
                    tag.setRSSI(0);
                    list_tag_devices.remove(i);
                    list_tag_devices.add(tag);
//                    ListTagActivity.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
                    adapter.notifyDataSetChanged();
//                        }
//                    });

                }
            }
        }
    }

    public void setDeviceRssi(BluetoothDevice device, int rssi) {
        for (int i = 0; i < list_tag_devices.size(); i++) {
            if (device.getAddress().equals(list_tag_devices.get(i).getAddress())) {
                list_tag_devices.get(i).setRSSI(rssi);
                list_tag_devices.get(i).setPin(72);
                ListTagActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                        listTag.setAdapter(adapter);
                    }
                });
            }
        }
    }

    private void connectDevice(final BluetoothDevice device) {
//        setCurrentDevice(device);
        final BluetoothGatt bluetoothGatt = device.connectGatt(this, true, new BluetoothGattCallback() {
            //            BluetoothDevice device = getCurrentDevice();
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//                                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    setStateDevice(device, true);
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    setStateDevice(device, false);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//                super.onServicesDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
//                    readPinLevel();
                    gatt.readRemoteRssi();
                } else if (status == BluetoothGatt.GATT_FAILURE) {
                    Toast.makeText(getApplicationContext(), "Service discovered fail", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//                super.onCharacteristicWrite(gatt, characteristic, status);

            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//                super.onCharacteristicChanged(gatt, characteristic);
                Log.d(TAG, "onCharacteristicChanged: " + characteristic.getUuid());
                if (characteristic.getUuid().equals(UUID.fromString(uuidNotifiCharacteristic))) {
//                    showNotification("Ethings", gatt.getDevice().getName() + " is finding your phone", true, 99);
                    gatt.discoverServices();
                }
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, int status) {
//                super.onReadRemoteRssi(gatt, rssi, status);
                Log.d(TAG, "onReadRemoteRssi: " + rssi);
                if (BluetoothGatt.GATT_SUCCESS == status) {
                    setDeviceRssi(device, rssi);
//                    write66(true);
                    BluetoothGattService bluetoothGattService = gatt.getService(UUID.fromString(uuidNotifiService));
                    BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(uuidNotifiCharacteristic));
                    gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true); //If so then enable notification in the BluetoothGatt
                    BluetoothGattDescriptor descriptor = bluetoothGattCharacteristic.getDescriptor(UUID.fromString(CHARACTERISTIC_NOTIFICATION_CONFIG)); //Get the descripter that enables notification on the server
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); //Set the value of the descriptor to enable notification
//                        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);//Write the descriptor
                }
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                if (BluetoothGatt.GATT_SUCCESS == status) {
//                    showNotification("Ethings", gatt.getDevice().getName() + " is finding your phone", true, 99);
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
//                super.onDescriptorWrite(gatt, descriptor, status);
//                if(BluetoothGatt.GATT_SUCCESS == status){
//                    showNotification("Ethings", "Finding your phone", true, 99);
//                }
            }
        });
    }

    private BluetoothDevice getCurrentDevice() {
        return currentDevice;
    }

    private void setCurrentDevice(BluetoothDevice device) {
        this.currentDevice = device;
    }

    //    private android.bluetooth.BluetoothGattCallback mBluetoothGattCalBack =
    int count = 0;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            if (count < 2)
                                requestLocation();
                            else return;
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        saveDatabase();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        scanner.stop();
    }

    private void saveDatabase() {
        for (int i = 0; i < list_tag_devices.size(); i++) {
            helper.updateDevice(list_tag_devices.get(i));
        }
    }

    private void requestLocation() {
        //request permision:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle bundle = intent.getExtras();
            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    //do some thing
                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    Log.d(TAG, "onReceive: " + BluetoothLeService.ACTION_GATT_DISCONNECTED);
                    if (bundle != null) {
                        String address = bundle.getString(BluetoothLeService.ACTION_INTENT_DEVICE_ADDRESS);
                        if (address != null && BTAdapter != null) {
                            BluetoothDevice device = BTAdapter.getRemoteDevice(address);
                            if (device == null) {
                                Log.w(TAG, "Device not found.  Unable to connect.");
                                return;
                            }
                            setStateDevice(device, false);
                        }
                    }
                    break;
                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    Log.d(TAG, "onReceive: " + BluetoothLeService.ACTION_GATT_CONNECTED);

                    if (bundle != null) {
                        String address = bundle.getString(BluetoothLeService.ACTION_INTENT_DEVICE_ADDRESS);
                        if (address != null && BTAdapter != null) {
                            BluetoothDevice device = BTAdapter.getRemoteDevice(address);
                            if (device == null) {
                                Log.w(TAG, "Device not found.  Unable to connect.");
                                return;
                            }
                            setStateDevice(device, true);
                        }
                    }
                    break;
                case BluetoothLeService.ACTION_READ_RSSI:
                    Log.d(TAG, "onReceive: " + BluetoothLeService.ACTION_READ_RSSI);
                    if (bundle != null) {
                        String address = bundle.getString(BluetoothLeService.ACTION_INTENT_DEVICE_ADDRESS);
                        int rssi = bundle.getInt(BluetoothLeService.ACTION_INTENT_DEVICE_RSSI);
                        if (address != null && BTAdapter != null) {
                            BluetoothDevice device = BTAdapter.getRemoteDevice(address);
                            if (device == null) {
                                Log.w(TAG, "Device not found.  Unable to connect.");
                                return;
                            }
                            setDeviceRssi(device, rssi);
                        }
                    }
                    break;
            }
        }
    };

    private class ScanProcessor implements BleScanner.BleScannerListener {
        private String TAG = "ScanProcessor";
        private int count = 0;
        /**
         * Scan map. Holds device which was found on ever scan.
         */
        private final Map<BluetoothDevice, Integer> scanMap = new HashMap<>();

        @Override
        public void onScanStarted() {
            Log.d(TAG, "onScanStarted: ");
            refresh.setRefreshing(true);
//            listView.post(new Runnable() {
//                @Override
//                public void run() {
//                    emptyView.setVisibility(View.GONE);
//                }
//            });
        }

        @Override
        public void onScanRepeat() {
            Log.d(TAG, "onScanRepeat");
            if (scanMap.isEmpty()) {
                scanner.stop();
            }
            updateDevices();
//            refresh.setRefreshing(false);
        }

        @Override
        public void onScanStopped() {
            Log.d(TAG, "onScanStopped: ");
            refresh.setRefreshing(false);
//            listView.post(new Runnable() {
//                @Override
//                public void run() {
//                    updateDevices();
//                    if (leDeviceListAdapter.isEmpty()) {
//                        emptyView.setVisibility(View.VISIBLE);
//                    }
//                    setScanActive(false);
//                }
//            });

        }

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] bytes) {
            scanMap.put(device, rssi);
            Log.d(TAG, "device Address: " + device.getAddress());
            Log.d(TAG, "device Name: " + device.getName());
            Log.d(TAG, "device RSSI: " + rssi);
        }

        private synchronized void updateDevices() {
            // look for lost list_tag_devices
//            final List<BluetoothDevice> toRemove = new ArrayList<>();
//            final List<BluetoothDevice> devices = new ArrayList<>(leDeviceListAdapter.getDevices());
//            for (BluetoothDevice device : devices) {
//                if (!scanMap.containsKey(device)) {
//                    toRemove.add(device);
//                }
//            }
//            // remove missed list_tag_devices
//            for (BluetoothDevice device : toRemove) {
//                devices.remove(device);
//            }
            // update device rssi
            final Map<BluetoothDevice, Integer> rssiMap = new HashMap<>();
            for (BluetoothDevice device : scanMap.keySet()) {
//                final int rssi = scanMap.get(device);
//                rssiMap.put(device, rssi);
                for (int i = 0; i < list_tag_devices.size(); i++) {
                    if (list_tag_devices.get(i).getAddress().equals(device.getAddress())) {
//                        connectDevice(device);
                    }
                }
            }

            scanMap.clear();

            //update list tag adapter
//            devicelop:
//            for (int i = 0; i < list_tag_devices.size(); i++) {
//                for (BluetoothDevice device : rssiMap.keySet()) {
//                    if (list_tag_devices.get(i).getAddress().equals(device.getAddress())) {
//                        list_tag_devices.get(i).setRSSI(rssiMap.get(device));
//                        list_tag_devices.get(i).setState(true);
////                        helper.updateDevice(list_tag_devices.get(i));
//                        String title = "Device connected";
//                        String message = "Your device : " + list_tag_devices.get(i).getName() + " is connected";
////                        showNotification(title, message, false, i);
//                        break devicelop;
//                    }
//                }
//                if (list_tag_devices.get(i).getState()) {
//                    count = 0;
//                    list_tag_devices.get(i).setState(false);
//                    String title = "Device disconnect";
//                    String message = "Your device : " + list_tag_devices.get(i).getName() + " is not in your range or disconnect";
////                    showNotification(title, message, true, i);
////                    helper.updateDevice(list_tag_devices.get(i));
//                }
//            }

            //update listview:
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    leDeviceListAdapter.setDevices(rssiMap);
                    leDeviceListAdapter.notifyDataSetChanged();

//                    list_tag_devices = helper.getAllDevices();
                    adapter.notifyDataSetChanged();
                }
            });
//            listTag.post(new Runnable() {
//                @Override
//                public void run() {
//                    leDeviceListAdapter.setDevices(rssiMap);
//                    leDeviceListAdapter.notifyDataSetChanged();
//
//                    Log.d(TAG, "listtagdevice size: " + list_tag_devices.size());
//                    adapter.notifyDataSetChanged();
//                    listTag.setAdapter(adapter);
//                }
//            });
        }

    }


    public void setOnItemTagDeviceDelete(OnItemTagDeviceDeleteListener onItemTagDeviceDelete){
        this.onItemTagDeviceDeleteListener = onItemTagDeviceDelete;
    }
    public interface OnItemTagDeviceDeleteListener{
        public void deleteItem(BluetoothDevice device);
    }

    @Override
    protected void onDestroy() {
        //remove all gatt

        super.onDestroy();
    }
}
