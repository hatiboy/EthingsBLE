package demo.ethings.com.ethingsble.sensor.ti;

/** Periodical demo.ethings.com.ethingsble.sensor. */
public interface TiPeriodicalSensor {

    String getPeriodUUID();

    int getMinPeriod();

    int getMaxPeriod();

    void setPeriod(int period);

    int getPeriod();
}
