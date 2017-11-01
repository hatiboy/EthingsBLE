package demo.ethings.com.ethingsble.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BroadcastRestartService extends BroadcastReceiver {
    private final String TAG = "BroadcastRestartService";
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
//        throw new UnsupportedOperationException("Not yet implemented");
        context.startService(new Intent(context, BluetoothLeService.class));
        Log.d(TAG, "Started Service" );
    }
}
