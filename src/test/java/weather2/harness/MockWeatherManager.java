package weather2.harness;

import weather2.weathersystem.storm.StormObject;

/**
 * Headless mock for WeatherManagerServer to track storm removal and packet sync in tests.
 */
public class MockWeatherManager {

    public long removedStormId = -1;
    public boolean syncCalled = false;

    public void removeStormObject(long id) {
        this.removedStormId = id;
    }

    public void syncStormRemove(StormObject storm) {
        this.syncCalled = true;
        if (storm != null) {
            storm.isDead = true;
        }
    }

    public void reset() {
        this.removedStormId = -1;
        this.syncCalled = false;
    }
}
