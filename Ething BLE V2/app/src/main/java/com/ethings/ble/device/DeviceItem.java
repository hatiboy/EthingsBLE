package com.ethings.ble.device;

/**
 * Created by FRAMGIA\nguyen.thanh.cong on 23/09/2015.
 */
public class DeviceItem {
    private String deviceName;
    private String address;
    private boolean connected;

    public String getDeviceName() {
        return deviceName;
    }

    public boolean getConnected() {
        return connected;
    }

    public String getAddress() {
        return address;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public DeviceItem(String name, String address, String connected){
        this.deviceName = name;
        this.address = address;
        this.connected = connected.equals("true");
    }
}
