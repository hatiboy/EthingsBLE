package demo.ethings.com.ethingsble.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

import demo.ethings.com.ethingsble.R;
import model.TagDevice;

public class AddNewTaskActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addnewtask);
//        devicelist = (ListView) findViewById(R.id.devicelist);
        pairedDevicesList();
    }

    ListView devicelist;
    private BluetoothAdapter myBluetooth = null;
    private Set pairedDevices;

    private void pairedDevicesList() {
        try {
            pairedDevices = myBluetooth.getBondedDevices();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        ArrayList list = new ArrayList();

        if (pairedDevices != null && pairedDevices.size() > 0) {
//            Iterator iterator = pairedDevices.iterator();
//            while (iterator.hasNext()) {
//                BluetoothDevice bt  = (BluetoothDevice) iterator.next();
//                list.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
//            }

            for (Object bt2 : pairedDevices) {
                try {
                    BluetoothDevice bt = (BluetoothDevice) bt2;
                    list.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked

    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Toast.makeText(getApplicationContext(), "item: " + i, Toast.LENGTH_SHORT).show();
        }
    };

    private final BroadcastReceiver bReciever = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Create a new device item
                TagDevice newDevice = new TagDevice(device.getAddress(), device.getName(), 0, 0);
                // Add it to our adapter
//                mAdapter.add(newDevice);
            }
        }
    };

}
