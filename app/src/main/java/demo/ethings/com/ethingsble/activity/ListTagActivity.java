package demo.ethings.com.ethingsble.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.chimeraiot.android.ble.BleScanner;
import com.chimeraiot.android.ble.BleUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import adapter.BleDevicesAdapter;
import adapter.TagDeviceAdapter;
import demo.ethings.com.ethingsble.R;
import model.TagDevice;
import ui.DeviceScanActivity;
import ui.ErrorDialog;

public class ListTagActivity extends Activity {

    private static final String TAG = ListTagActivity.class.getName();
    private ListView listTag;
    private Button addnewtag;
    ArrayList<TagDevice> list_tag_devices;
    BLESQLiteHelper helper;
    private BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
    private TagDeviceAdapter adapter;
    private BleScanner scanner;
    private BluetoothAdapter bluetoothAdapter;
    protected SwipeRefreshLayout refresh;
    /**
     * Request to enable Bluetooth.
     */
    private static final int REQUEST_ENABLE_BT = 1;

    /**
     * Scan delay period.
     */
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final long SCAN_PERIOD = 3000L;
    private BleDevicesAdapter leDeviceListAdapter;

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
            }
        });

        helper = new BLESQLiteHelper(this);
//        helper.insertDevice(new TagDevice("1", "Bag", 4 , 100));

        list_tag_devices = helper.getAllDevices();
//        list_tag_devices = new ArrayList<TagDevice>();
//        list_tag_devices.add(new TagDevice("1", "Bag", 4 , 100));
//        list_tag_devices.add(new TagDevice("2", "Keys" , 5 , 90));
//        list_tag_devices.add(new TagDevice("3", "Iphone", 5 , 100));


        adapter = new TagDeviceAdapter(this, R.layout.item_tag_device, list_tag_devices);
        listTag.setAdapter(adapter);

        addnewtag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ListTagActivity.this, DeviceScanActivity.class);
                startActivity(i);
            }
        });
        listTag.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    //if device connected:
                    final BluetoothDevice device = leDeviceListAdapter.getDevice(i);
                    if (device == null) {
                        return;
                    }
                    //find device connected:
                    BluetoothDevice bl = BTAdapter.getRemoteDevice(device.getAddress());
                    Log.d(TAG, "onItemClick: " + bl.getName());

                    final Intent intent = new Intent(ListTagActivity.this, FindDeviceActivity.class);
                    intent.putExtra(FindDeviceActivity.EXTRAS_DEVICE_NAME, device.getName());
                    intent.putExtra(FindDeviceActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                    intent.putExtra("device", leDeviceListAdapter.getDevice(i));
                    startActivity(intent);

                } catch (Exception e) {
                    e.printStackTrace();
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
                        list_tag_devices = helper.getAllDevices();
                        adapter.notifyDataSetChanged();
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

        requestLocation();
    }

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
    protected void onResume() {
        super.onResume();
        //config Bluetooth Adapter:
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        BTAdapter.startDiscovery();
        adapter.notifyDataSetChanged();
        if (leDeviceListAdapter == null) {
            leDeviceListAdapter = new BleDevicesAdapter(getBaseContext());
        }
        scanner.start();

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        scanner.stop();
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
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                try {
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                    Log.d(TAG, "onReceive: rssi " + rssi + " name: " + name);
                    for (int i = 0; i < list_tag_devices.size(); i++) {
                        if (list_tag_devices.get(i).getName().equals(name)) {
                            list_tag_devices.get(i).setRSSI(rssi);
                            list_tag_devices.get(i).setState(!list_tag_devices.get(i).getState());
                            helper.updateDevice(list_tag_devices.get(i));
                            adapter.notifyDataSetChanged();
                        }
                    }
                } catch (Exception e) {

                }
            }
        }
    };

    private class ScanProcessor implements BleScanner.BleScannerListener {
        private String TAG = "ScanProcessor";
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
            Log.d(TAG, "onLeScan: " + device.getAddress());
        }

        private synchronized void updateDevices() {
            // look for lost list_tag_devices
            final List<BluetoothDevice> toRemove = new ArrayList<>();
            final List<BluetoothDevice> devices = new ArrayList<>(leDeviceListAdapter.getDevices());
            for (BluetoothDevice device : devices) {
                if (!scanMap.containsKey(device)) {
                    toRemove.add(device);
                }
            }
            // remove missed list_tag_devices
            for (BluetoothDevice device : toRemove) {
                devices.remove(device);
            }
            // update device rssi
            final Map<BluetoothDevice, Integer> rssiMap = new HashMap<>();
            for (BluetoothDevice device : scanMap.keySet()) {
                final int rssi = scanMap.get(device);
                rssiMap.put(device, rssi);
            }

            scanMap.clear();

            //update list tag adapter
            for(BluetoothDevice device : scanMap.keySet()) {
                for(int i = 0; i < list_tag_devices.size(); i++){
                    if(list_tag_devices.get(i).equals(device)){
                        list_tag_devices.get(i).setRSSI(scanMap.get(device));
                        list_tag_devices.get(i).setState(true);
                        helper.updateDevice(list_tag_devices.get(i));
                        continue;
                    }
                    list_tag_devices.get(i).setState(false);
                }
            }

            //update listview:
            listTag.post(new Runnable() {
                @Override
                public void run() {
                    leDeviceListAdapter.setDevices(rssiMap);
                    leDeviceListAdapter.notifyDataSetChanged();

                    adapter.notifyDataSetChanged();
                }
            });
        }
    }
}
