package weather2.integration.hbm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * NTNH Deep Module: HBM NTM Space & Celestial Atmosphere Adapter.
 * Encapsulates dynamic dimension ID resolution from SpaceConfig and
 * vacuum world atmosphere detection via WorldProviderCelestial.hasWeatherCycle().
 */
public class HbmSpaceAdapter {

    private static Field worldProviderField = null;
    private static Field providerDimensionIdField = null;
    private static Class<?> celestialProviderClass = null;
    private static Method celestialHasWeatherCycleMethod = null;
    private static boolean celestialReflectionInit = false;

    private static Class<?> spaceConfigClass = null;
    private static boolean spaceConfigReflectionInit = false;

    private static void initCelestialReflection(Class<?> worldClass) {
        if (celestialReflectionInit) return;
        celestialReflectionInit = true;
        try {
            worldProviderField = worldClass.getField("provider");
        } catch (Throwable t) {
            try {
                worldProviderField = worldClass.getField("field_73011_v");
            } catch (Throwable ignored) {}
        }
        try {
            celestialProviderClass = Class.forName("com.hbm.dim.WorldProviderCelestial");
            celestialHasWeatherCycleMethod = celestialProviderClass.getMethod("hasWeatherCycle");
        } catch (Throwable ignored) {}
    }

    private static void initSpaceConfig() {
        if (spaceConfigReflectionInit) return;
        spaceConfigReflectionInit = true;
        try {
            spaceConfigClass = Class.forName("com.hbm.config.SpaceConfig");
        } catch (Throwable ignored) {}
    }

    /**
     * Dynamically resolves celestial dimension IDs from NTM SpaceConfig.
     * Returns null if HBM is not installed, the token is not recognized, or reflection fails.
     */
    public static Integer resolveSpaceDimension(String token) {
        if (token == null) return null;
        token = token.trim()
            .toLowerCase();

        initSpaceConfig();
        if (spaceConfigClass == null) return null;

        try {
            if (token.equals("duna")) return spaceConfigClass.getField("dunaDimension")
                .getInt(null);
            if (token.equals("eve")) return spaceConfigClass.getField("eveDimension")
                .getInt(null);
            if (token.equals("laythe")) return spaceConfigClass.getField("laytheDimension")
                .getInt(null);
            if (token.equals("tekto")) return spaceConfigClass.getField("tektoDimension")
                .getInt(null);
            if (token.equals("moon") || token.equals("mun")) return spaceConfigClass.getField("moonDimension")
                .getInt(null);
            if (token.equals("minmus")) return spaceConfigClass.getField("minmusDimension")
                .getInt(null);
            if (token.equals("ike")) return spaceConfigClass.getField("ikeDimension")
                .getInt(null);
            if (token.equals("dres")) return spaceConfigClass.getField("dresDimension")
                .getInt(null);
            if (token.equals("moho")) return spaceConfigClass.getField("mohoDimension")
                .getInt(null);
            if (token.equals("orbit")) return spaceConfigClass.getField("orbitDimension")
                .getInt(null);
            if (token.equals("thatmo")) return spaceConfigClass.getField("thatmoDimension")
                .getInt(null);
        } catch (Throwable ignored) {}

        return null;
    }

    /**
     * Checks if the given world provider belongs to HBM's celestial system.
     */
    public static boolean isCelestialWorldProvider(Object provider) {
        if (provider == null) return false;
        if (celestialProviderClass == null) {
            try {
                celestialProviderClass = Class.forName("com.hbm.dim.WorldProviderCelestial");
            } catch (Throwable t) {
                return false;
            }
        }
        return celestialProviderClass.isInstance(provider);
    }

    /**
     * Checks if the celestial body possesses an atmosphere with weather cycle support.
     * Returns null if the provider is not a WorldProviderCelestial or reflection fails.
     */
    public static Boolean hasWeatherCycle(Object provider) {
        if (!isCelestialWorldProvider(provider)) return null;
        try {
            if (celestialHasWeatherCycleMethod == null && celestialProviderClass != null) {
                celestialHasWeatherCycleMethod = celestialProviderClass.getMethod("hasWeatherCycle");
            }
            if (celestialHasWeatherCycleMethod != null) {
                return (Boolean) celestialHasWeatherCycleMethod.invoke(provider);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Universal check for whether a world has an atmosphere.
     * Evaluates Overworld (always true), queries HBM celestial provider, or falls back to true.
     */
    public static boolean isAtmosphericWorld(Object worldObj) {
        if (worldObj == null) return false;
        try {
            initCelestialReflection(worldObj.getClass());
            if (worldProviderField == null) return true;
            Object provider = worldProviderField.get(worldObj);
            if (provider == null) return false;

            if (providerDimensionIdField == null) {
                try {
                    providerDimensionIdField = provider.getClass()
                        .getField("dimensionId");
                } catch (Throwable t) {
                    try {
                        providerDimensionIdField = provider.getClass()
                            .getField("field_76574_g");
                    } catch (Throwable ignored) {}
                }
            }
            if (providerDimensionIdField != null) {
                int dim = providerDimensionIdField.getInt(provider);
                if (dim == 0) return true; // Overworld is always atmospheric
            }

            Boolean celestialWeather = hasWeatherCycle(provider);
            if (celestialWeather != null) {
                return celestialWeather.booleanValue();
            }
        } catch (Throwable ignored) {}
        return true;
    }
}
