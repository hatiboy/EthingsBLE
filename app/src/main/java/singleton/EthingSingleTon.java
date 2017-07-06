package singleton;

/**
 * Created by dangv on 07/07/2017.
 */

public class EthingSingleTon {
    public static EthingSingleTon ethingSingleTon;

    public static EthingSingleTon getInstance() {
        if (ethingSingleTon == null) {
            ethingSingleTon = new EthingSingleTon();
        }
        return ethingSingleTon;
    }

    public boolean led_state;

    public void setLedState(boolean led_state) {
        this.led_state = led_state;
    }

    public boolean getLedState() {
        return led_state;
    }
}
