package weather2.integration.dsurround;

import java.lang.reflect.Method;

import weather2.integration.AdapterReport;
import weather2.integration.DiagnosableAdapter;
import weather2.integration.IntegrationManager;

/**
 * NTNH Deep Module: Dynamic Surroundings Weather Sync Adapter.
 * Bridges Weather2 precipitation intensity to Dynamic Surroundings acoustic & visual engine.
 */
public class DSurroundWeatherAdapter implements DiagnosableAdapter {

    public static final DSurroundWeatherAdapter INSTANCE = new DSurroundWeatherAdapter();

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

    @Override
    public String getAdapterName() {
        return "DSurround Weather Adapter";
    }

    @Override
    public String getTargetModId() {
        return IntegrationManager.MODID_DSURROUND;
    }

    @Override
    public AdapterReport diagnose() {
        if (!IntegrationManager.isDSurroundLoaded()) {
            return AdapterReport.notInstalled(getAdapterName(), getTargetModId());
        }

        initReflection();
        if (dsurroundSetIntensity != null) {
            return AdapterReport.active(
                getAdapterName(),
                getTargetModId(),
                "DynSurround Weather.setIntensity linked, acoustic & visual precipitation sync active");
        } else {
            return AdapterReport.degraded(
                getAdapterName(),
                getTargetModId(),
                "DynSurround Weather.setIntensity method not found",
                "Precipitation intensity sync disabled, vanilla rain sound fallback");
        }
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
