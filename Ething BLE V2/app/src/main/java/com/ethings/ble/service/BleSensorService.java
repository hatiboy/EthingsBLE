package com.ethings.ble.service;

import com.chimeraiot.android.ble.BleManager;
import com.ethings.ble.sensor.App;

/** BLE service. */
public class BleSensorService extends com.chimeraiot.android.ble.BleService {

    @Override
    protected BleManager createBleManager() {
        return new BleManager(App.DEVICE_DEF_COLLECTION);
    }
}
