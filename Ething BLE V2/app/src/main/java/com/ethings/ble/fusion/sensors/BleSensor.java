package com.ethings.ble.fusion.sensors;

import android.hardware.Sensor;

import com.ethings.ble.sensor.ti.TiAccelerometerSensor;
import com.ethings.ble.sensor.ti.TiGyroscopeSensor;
import com.ethings.ble.sensor.ti.TiMagnetometerSensor;
import com.ethings.ble.sensor.ti.TiRangeSensors;

import java.util.HashMap;

public class BleSensor implements ISensor {

    private static final HashMap<String, Integer> TYPE_MAP = new HashMap<>();
    private static final HashMap<Integer, String> UUID_MAP = new HashMap<>();

    static {
        TYPE_MAP.put(TiAccelerometerSensor.UUID_SERVICE, Sensor.TYPE_ACCELEROMETER);
        TYPE_MAP.put(TiMagnetometerSensor.UUID_SERVICE, Sensor.TYPE_MAGNETIC_FIELD);
        TYPE_MAP.put(TiGyroscopeSensor.UUID_SERVICE, Sensor.TYPE_GYROSCOPE);

        UUID_MAP.put(Sensor.TYPE_ACCELEROMETER, TiAccelerometerSensor.UUID_SERVICE);
        UUID_MAP.put(Sensor.TYPE_MAGNETIC_FIELD, TiMagnetometerSensor.UUID_SERVICE);
        UUID_MAP.put(Sensor.TYPE_GYROSCOPE, TiGyroscopeSensor.UUID_SERVICE);
    }



    public static int getSensorType(String uuid) {
        return TYPE_MAP.get(uuid);
    }

    public static String getSensorUuid(int sensorType) {
        return UUID_MAP.get(sensorType);
    }

    private final TiRangeSensors<float[], Float> sensor;

    public BleSensor(TiRangeSensors<float[], Float> sensor) {
        this.sensor = sensor;
    }

    @Override
    public float getMaxRange() {
        return sensor.getMaxRange();
    }

    @Override
    public int getType() {
        return getSensorType(sensor.getServiceUUID());
    }

    public TiRangeSensors<?, ?> getTiSensor() {
        return sensor;
    }
}
