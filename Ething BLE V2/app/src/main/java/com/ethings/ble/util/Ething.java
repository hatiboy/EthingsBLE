package com.ethings.ble.util;

import android.content.Context;

public class Ething {
    private final static String TAG = Ething.class.getSimpleName();
    private Context context;
    private Ething INSTANCE;
    private BLEDeviceDataChangeInterface bleDeviceDataChangeInterface;

    public Ething(Context context){
        this.context = context;
    }
    public Ething(){

    }

    public synchronized Ething getInstance(){
        if(INSTANCE == null) INSTANCE = new Ething();
        return INSTANCE;
    }
    
    public void setOnBLEDeviceDataChangeInterface(BLEDeviceDataChangeInterface onBLEDeviceDataChangeInterface){

    }


    public interface BLEDeviceDataChangeInterface{
        void onStateChange();
        void onRSSIChange();
        void onPinchange();
    }
}
