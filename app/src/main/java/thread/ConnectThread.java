package thread;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by dangv on 04/06/2017.
 */

public class ConnectThread extends Thread {
    private static final String TAG = ConnectThread.class.getName();
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;

    private final UUID MY_UUID = UUID.fromString("F0000000-0451-4000-B000-000000000000");

    public ConnectThread(BluetoothDevice device) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        mmDevice = device;

//        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
//            tmp = device.createRfcommSocketToServiceRecord(device.getUuids()[0].getUuid());
            Log.d(TAG, "ConnectThread: UUID: " + device.getUuids()[0].getUuid() + " size: " + device.getUuids().length);
//            tmp =  device.createInsecureRfcommSocketToServiceRecord(MY_UUID);

//        } catch (IOException e) {
//            Log.e(TAG, "Socket's create() method failed", e);
//        }
        mmSocket = tmp;
    }

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        mBluetoothAdapter.cancelDiscovery();

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        manageMyConnectedSocket(mmSocket);
    }

    private void manageMyConnectedSocket(BluetoothSocket mmSocket) {
        Log.d(TAG, "manageMyConnectedSocket: " + mmSocket.isConnected());
    }


    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}