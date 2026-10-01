package weather2.integration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * NTNH Deep Module: Dynamic Surroundings Bridge & Audio/Command Calibration.
 * Handles:
 * - Soft-fail reflective sync with Dynamic Surroundings (Weather.setIntensity)
 * - Native Minecraft client rain strength sync when Weather2 particles are suppressed
 * - Voice chat friendly acoustic volume calibration (thunder, lightning detonation, tornado howling)
 * - Ergonomic command aliases for /weather2
 */
public class DSurroundBridge {

    private static boolean worldRainInit = false;
    private static Method worldSetRainStrength = null;
    private static Field worldRainingStrength = null;
    private static Field worldPrevRainingStrength = null;

    public static void setWorldRainStrength(Object world, float strength) {
        if (world == null) return;
        try {
            if (!worldRainInit) {
                worldRainInit = true;
                try {
                    worldSetRainStrength = world.getClass().getMethod("setRainStrength", float.class);
                } catch (Throwable t) {
                    try { worldSetRainStrength = world.getClass().getMethod("func_72894_k", float.class); } catch (Throwable ignored) {}
                }
                try {
                    worldRainingStrength = world.getClass().getField("rainingStrength");
                } catch (Throwable t) {
                    try { worldRainingStrength = world.getClass().getField("field_73004_o"); } catch (Throwable ignored) {}
                }
                try {
                    worldPrevRainingStrength = world.getClass().getField("prevRainingStrength");
                } catch (Throwable t) {
                    try { worldPrevRainingStrength = world.getClass().getField("field_73017_q"); } catch (Throwable ignored) {}
                }
            }

            if (worldSetRainStrength != null) {
                worldSetRainStrength.invoke(world, strength);
            } else if (worldRainingStrength != null) {
                worldRainingStrength.setFloat(world, strength);
                if (worldPrevRainingStrength != null) {
                    worldPrevRainingStrength.setFloat(world, strength);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static boolean dsurroundInit = false;
    private static Method dsurroundSetIntensity = null;

    public static void updateDSurroundIntensity(float strength) {
        try {
            if (!dsurroundInit) {
                dsurroundInit = true;
                try {
                    Class<?> dsurroundWeather = Class.forName("org.blockartistry.mod.DynSurround.client.weather.Weather");
                    dsurroundSetIntensity = dsurroundWeather.getMethod("setIntensity", float.class);
                } catch (Throwable ignored) {}
            }
            if (dsurroundSetIntensity != null) {
                dsurroundSetIntensity.invoke(null, strength);
            }
        } catch (Throwable ignored) {}
    }

    private static Object cachedMinecraft = null;
    private static Field fieldTheWorld = null;
    private static boolean clientReflectionInit = false;

    private static void initClientReflection() {
        if (clientReflectionInit) return;
        clientReflectionInit = true;
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Method getMc = mcClass.getMethod("getMinecraft");
            cachedMinecraft = getMc.invoke(null);
            if (cachedMinecraft != null) {
                try {
                    fieldTheWorld = mcClass.getField("theWorld");
                } catch (Throwable t) {
                    try {
                        fieldTheWorld = mcClass.getField("field_71441_e");
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Synchronizes Weather2's client precipitation strength with vanilla theWorld and Dynamic Surroundings.
     * Called on client render tick.
     */
    public static void onPrecipitationTick(float curPrecipStr) {
        initClientReflection();
        if (cachedMinecraft != null && fieldTheWorld != null) {
            try {
                Object world = fieldTheWorld.get(cachedMinecraft);
                if (world != null) {
                    float strength = Math.abs(curPrecipStr);
                    setWorldRainStrength(world, strength);
                    updateDSurroundIntensity(strength);
                }
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Resets rain strength on both vanilla world and Dynamic Surroundings upon dimension reload.
     */
    public static void resetPrecipitation() {
        onPrecipitationTick(0.0F);
    }

    /**
     * Calibrates thunder volume for voice chat comfort (~70% of upstream).
     */
    public static float getAdjustedThunderVolume(float baseVol) {
        return baseVol * 0.70F;
    }

    /**
     * Calibrates tornado wind howl volume (75% of upstream).
     */
    public static float getAdjustedWindVolume(float baseVol) {
        return baseVol * 0.75F;
    }

    /**
     * Normalizes user commands, adding aliases for /weather2 kill, /weather2 killall, /weather2 clear.
     */
    public static String[] normalizeCommandArgs(String[] args) {
        if (args == null || args.length == 0 || args[0].equalsIgnoreCase("help")) {
            return new String[] { "help" };
        }
        if (args[0].equalsIgnoreCase("kill") || args[0].equalsIgnoreCase("killall") || args[0].equalsIgnoreCase("clear")) {
            return new String[] { "storm", "killall" };
        }
        return args;
    }
}
