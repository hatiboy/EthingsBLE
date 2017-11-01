package demo.ethings.com.ethingsble.service;

import com.chimeraiot.android.ble.BleManager;

import demo.ethings.com.ethingsble.sensor.App;

/** BLE demo.ethings.com.ethingsble.service. */
public class BleSensorService extends com.chimeraiot.android.ble.BleService {

    @Override
    protected BleManager createBleManager() {
        return new BleManager(App.DEVICE_DEF_COLLECTION);
    }
}
