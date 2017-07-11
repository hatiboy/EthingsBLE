package demo.ethings.com.ethingsble.service;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.chimeraiot.android.ble.BleUtils;

import demo.ethings.com.ethingsble.configure.AppConfig;


/**
 * Bluetooth state broadcast receiver. Used to re-enable listener demo.ethings.com.ethingsble.service.
 */
public class BluetoothStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            context.startService(new Intent(context, BluetoothLeService.class));
        }
        if (!AppConfig.ENABLE_RECORD_SERVICE) {
            return;
        }

        final BluetoothAdapter adapter = BleUtils.getBluetoothAdapter(context);
        final Intent gattServiceIntent = new Intent(context, BleSensorsRecordService.class);
        if (adapter != null && adapter.isEnabled()) {
            context.startService(gattServiceIntent);
        } else {
            context.stopService(gattServiceIntent);
        }
    }
}