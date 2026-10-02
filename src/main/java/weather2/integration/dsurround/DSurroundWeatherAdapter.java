package weather2.integration.dsurround;

import java.lang.reflect.Method;

/**
 * NTNH Deep Module: Dynamic Surroundings Weather Sync Adapter.
 * Bridges Weather2 precipitation intensity to Dynamic Surroundings acoustic & visual engine.
 */
public class DSurroundWeatherAdapter {

    private static boolean dsurroundInit = false;
    private static Method dsurroundSetIntensity = null;

    private static void initReflection() {
        if (dsurroundInit) return;
        dsurroundInit = true;
        try {
            Class<?> dsurroundWeather = Class.forName("org.blockartistry.mod.DynSurround.client.weather.Weather");
            dsurroundSetIntensity = dsurroundWeather.getMethod("setIntensity", float.class);
        } catch (Throwable ignored) {}
    }

    public static boolean isAvailable() {
        initReflection();
        return dsurroundSetIntensity != null;
    }

    /**
     * Updates Dynamic Surroundings rain/storm intensity via reflection.
     */
    public static void setIntensity(float strength) {
        initReflection();
        if (dsurroundSetIntensity != null) {
            try {
                dsurroundSetIntensity.invoke(null, strength);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Resets Dynamic Surroundings rain intensity to 0.0F.
     */
    public static void reset() {
        setIntensity(0.0F);
    }

    public static void resetReflection() {
        dsurroundInit = false;
        dsurroundSetIntensity = null;
    }
}
