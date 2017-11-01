package demo.ethings.com.ethingsble.model;

/**
 * Created by dangv on 26/05/2017.
 */

public class TagDevice {
    private String address;
    private String name;
    private int rssi;
    private int pin;
    private boolean state;

    public TagDevice() {

    }

    public boolean getState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRSSI() {
        return rssi;
    }

    public void setRSSI(int rssi) {
        this.rssi = rssi;
    }

    public int getPin() {
        return pin;
    }

    public void setPin(int pin) {
        this.pin = pin;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public TagDevice(String address, String name, int rssi, int pin, boolean state) {
        this.address = address;
        this.name = name;
        this.rssi = rssi;
        this.pin = pin;
        this.state = state;
    }
}
