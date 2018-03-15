package com.ethings.ble.fusion;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.ethings.ble.R;
import com.ethings.ble.configure.AppConfig;
import com.ethings.ble.fusion.engine.SensorFusionHelper;
import com.ethings.ble.fusion.sensors.AndroidSensorManager;
import com.ethings.ble.fusion.sensors.BleSensorManager;
import com.ethings.ble.fusion.sensors.ISensor;
import com.ethings.ble.fusion.sensors.ISensorManager;
import com.ethings.ble.gl.GlFragment;

import rajawali.Object3D;

public class SensorFusionFragment extends GlFragment implements ISensorManager.SensorEventListener {
    public final static String TAG = SensorFusionFragment.class.getSimpleName();

    public static final String ARG_DEVICE_ADDRESS = TAG+":DEVICE_ADDRESS";

    private ISensorManager sensorManager;
    private TextView viewFused;

    private final SensorFusionHelper sensorFusion = new SensorFusionHelper() {
        @Override
        public void onOrientationChanged(final float[] orientation) {
            final Object3D model = getModel();
            if (model == null)
                return;

            final double[] patchedOrientation = sensorManager.patchSensorFusion(orientation);
            model.setRotation(patchedOrientation[0], patchedOrientation[1], patchedOrientation[2]);
            getActivity().runOnUiThread(new Runnable() {
                @SuppressLint("DefaultLocale")
                @Override
                public void run() {
                    viewFused.setText(String.format("%+.6f\n%+.6f\n%+.6f",
                                      orientation[0], orientation[1], orientation[2]));
                }
            });
        }
    };

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Bundle args = getArguments();
        if (args == null || !args.containsKey(ARG_DEVICE_ADDRESS)) {
            sensorManager = new AndroidSensorManager(getActivity());
        } else {
            final String deviceAddress = args.getString(ARG_DEVICE_ADDRESS);
            sensorManager = new BleSensorManager(getActivity(), deviceAddress);
        }
        sensorManager.setListener(this);

        viewFused = view.findViewById(R.id.fused);
    }

    @Override
    public int getContentViewId() {
        return R.layout.sensor_fusion_fragment;
    }

    @Override
    public void onStart() {
        super.onStart();

        sensorManager.enable();
        if (AppConfig.SENSOR_FUSION_USE_MAGNET_SENSOR)
            sensorManager.registerSensor(ISensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerSensor(ISensor.TYPE_ACCELEROMETER);
        sensorManager.registerSensor(ISensor.TYPE_GYROSCOPE);
        sensorFusion.start();
    }

    @Override
    public void onStop() {
        super.onStop();

        sensorFusion.stop();
        sensorManager.disable();
    }

    @Override
    public void onSensorChanged(int sensorType, float[] values) {
        switch(sensorType) {
            case ISensor.TYPE_ACCELEROMETER:
                sensorFusion.onAccDataUpdate(values);
                break;

            case ISensor.TYPE_GYROSCOPE:
                sensorFusion.onGyroDataUpdate(values);
                break;

            case ISensor.TYPE_MAGNETIC_FIELD:
                sensorFusion.onMagDataUpdate(values);
                break;
        }
    }

    public boolean isLocalSensorsModeEnabled() {
        return sensorManager instanceof AndroidSensorManager;
    }
}
