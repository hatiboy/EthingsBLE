package model;

/**
 * Created by dangv on 26/05/2017.
 */

public class TagDevice {
    private String id;
    private String name;
    private int intensity;
    private int pin;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public int getPin() {
        return pin;
    }

    public void setPin(int pin) {
        this.pin = pin;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TagDevice(String id, String name, int intensity, int pin) {
        this.id = id;
        this.name = name;
        this.intensity = intensity;
        this.pin = pin;
    }
}
