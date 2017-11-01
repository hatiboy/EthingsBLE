package service;

import com.chimeraiot.android.ble.BleManager;

import sensor.App;

/** BLE service. */
public class BleSensorService extends com.chimeraiot.android.ble.BleService {

    @Override
    protected BleManager createBleManager() {
        return new BleManager(App.DEVICE_DEF_COLLECTION);
    }
}
