package sensor.ti;

/** Periodical sensor. */
public interface TiPeriodicalSensor {

    String getPeriodUUID();

    int getMinPeriod();

    int getMaxPeriod();

    void setPeriod(int period);

    int getPeriod();
}
