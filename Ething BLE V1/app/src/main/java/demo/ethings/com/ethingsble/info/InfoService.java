package demo.ethings.com.ethingsble.info;

import android.bluetooth.BluetoothGattCharacteristic;

import com.chimeraiot.android.ble.sensor.Sensor;

/**
 * BLE demo.ethings.com.ethingsble.info demo.ethings.com.ethingsble.service.
 * @param <M> data demo.ethings.com.ethingsble.model.
 */
public abstract class InfoService<M> extends Sensor<M> {

    /** Data value. */
    private String value;

    protected InfoService(M model) {
        super(model);
    }

    public String getValue() {
        return value;
    }

    @Override
    protected boolean apply(final BluetoothGattCharacteristic c, final M data) {
        value = c.getStringValue(0);
        // always set it to true to notify update
        return true;
    }
}
