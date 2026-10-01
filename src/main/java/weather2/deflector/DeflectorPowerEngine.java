package weather2.deflector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import weather2.weathersystem.WeatherManagerServer;
import weather2.weathersystem.storm.StormObject;

/**
 * NTNH Deep Module: Weather Deflector Power Engine & State Machine Controller.
 * Manages active HBM HE MK2 power consumption and explicit state transitions:
 * - OFFLINE (0 HE): Unpowered or depleted buffer (< 25 kHE). Field collapsed.
 * - ACTIVE (25 kHE/t): Nominal operation, 150 blocks radius protection dome.
 * - DISSIPATING (100k..50M HE): High-energy EMP pulse neutralizing storm F0-F4.
 * - BLACKOUT_DEPLETED (200 ticks / 10s): Power failure lockout when depleted under load.
 * - OVERLOAD_COLLAPSED (300 ticks / 15s): Catastrophic collapse upon F5/C5 impact (unsuppressable).
 * - Redstone comparator integration: emits distinct analog signals (15, 12, 6, 1, 0) per state.
 */
public class DeflectorPowerEngine {

    public static final long DEFLECTOR_IDLE_DRAIN = 25000L;
    public static final long DEFLECTOR_MAX_POWER = 100000000L;

    public static final int COOLDOWN_BLACKOUT = 200;
    public static final int COOLDOWN_OVERLOAD = 300;
    public static final int COOLDOWN_DISSIPATING = 5;

    private static Field deflectorPowerField = null;
    private static Field deflectorFieldActiveField = null;
    private static Field deflectorBlackoutField = null;
    private static boolean deflectorReflectionInit = false;

    private static void initDeflectorReflection(Class<?> deflectorClass) {
        if (deflectorReflectionInit) return;
        deflectorReflectionInit = true;
        try {
            deflectorPowerField = deflectorClass.getField("power");
        } catch (Throwable ignored) {}
        try {
            deflectorFieldActiveField = deflectorClass.getField("isFieldActive");
        } catch (Throwable ignored) {}
        try {
            deflectorBlackoutField = deflectorClass.getField("blackoutCooldown");
        } catch (Throwable ignored) {}
    }

    /**
     * Dynamic HE dissipation cost per storm intensity stage:
     * - Stages 0-3 (Rain, Thunder, Highwind, Hail): 100k HE
     * - Stage 4 (F0/C0 Forming): 500k HE
     * - Stage 5 (F1/C1 Initial): 1.5M HE
     * - Stage 6 (F2/C2 Moderate): 5.0M HE
     * - Stage 7 (F3/C3 Severe): 15.0M HE
     * - Stage 8 (F4/C4 Devastating): 50.0M HE (Endgame grid load)
     * - Stage 9+ (F5/C5 Catastrophic): -1L (Unsuppressable! Overload collapse!)
     */
    public static long getDeflectorDissipationCost(int stage) {
        if (stage <= 3) return 100000L;
        if (stage == 4) return 500000L;
        if (stage == 5) return 1500000L;
        if (stage == 6) return 5000000L;
        if (stage == 7) return 15000000L;
        if (stage == 8) return 50000000L;
        return -1L; // F5/C5 cannot be deflected!
    }

    /**
     * Ticks deflector power and executes State Machine transitions.
     * Called every tick on logical server.
     */
    public static boolean tickDeflectorPower(Object deflectorObj) {
        if (deflectorObj == null) return false;

        if (deflectorObj instanceof IDeflectorTE) {
            IDeflectorTE te = (IDeflectorTE) deflectorObj;
            DeflectorState state = te.getState();
            int timer = te.getStateTimer();
            long power = te.getPower();

            switch (state) {
                case OFFLINE:
                    if (power >= DEFLECTOR_IDLE_DRAIN) {
                        te.transitionTo(DeflectorState.ACTIVE, 0);
                        return true;
                    }
                    return false;

                case ACTIVE:
                    if (power < DEFLECTOR_IDLE_DRAIN) {
                        te.transitionTo(DeflectorState.OFFLINE, 0);
                        return false;
                    }
                    te.setPower(power - DEFLECTOR_IDLE_DRAIN);
                    return true;

                case DISSIPATING:
                    if (timer > 0) {
                        te.setStateTimer(timer - 1);
                    }
                    if (te.getStateTimer() <= 0) {
                        if (power >= DEFLECTOR_IDLE_DRAIN) {
                            te.transitionTo(DeflectorState.ACTIVE, 0);
                            return true;
                        } else {
                            te.transitionTo(DeflectorState.OFFLINE, 0);
                            return false;
                        }
                    }
                    if (power >= DEFLECTOR_IDLE_DRAIN) {
                        te.setPower(power - DEFLECTOR_IDLE_DRAIN);
                    }
                    return true;

                case BLACKOUT_DEPLETED:
                case OVERLOAD_COLLAPSED:
                    if (timer > 0) {
                        te.setStateTimer(timer - 1);
                    }
                    if (te.getStateTimer() <= 0) {
                        if (power >= DEFLECTOR_IDLE_DRAIN) {
                            te.transitionTo(DeflectorState.ACTIVE, 0);
                            return true;
                        } else {
                            te.transitionTo(DeflectorState.OFFLINE, 0);
                            return false;
                        }
                    }
                    return false;

                default:
                    return false;
            }
        }

        // Reflective fallback for test mocks
        initDeflectorReflection(deflectorObj.getClass());

        int blackout = 0;
        try {
            if (deflectorBlackoutField != null) {
                blackout = deflectorBlackoutField.getInt(deflectorObj);
            }
        } catch (Throwable ignored) {}

        if (blackout > 0) {
            blackout--;
            try {
                if (deflectorBlackoutField != null) {
                    deflectorBlackoutField.setInt(deflectorObj, blackout);
                }
                if (deflectorFieldActiveField != null) {
                    deflectorFieldActiveField.setBoolean(deflectorObj, false);
                }
            } catch (Throwable ignored) {}
            return false;
        }

        long power = 0;
        try {
            if (deflectorPowerField != null) {
                power = deflectorPowerField.getLong(deflectorObj);
            }
        } catch (Throwable ignored) {}

        if (power >= DEFLECTOR_IDLE_DRAIN) {
            power -= DEFLECTOR_IDLE_DRAIN;
            try {
                if (deflectorPowerField != null) {
                    deflectorPowerField.setLong(deflectorObj, power);
                }
                if (deflectorFieldActiveField != null) {
                    deflectorFieldActiveField.setBoolean(deflectorObj, true);
                }
            } catch (Throwable ignored) {}
            return true;
        } else {
            try {
                if (deflectorFieldActiveField != null) {
                    deflectorFieldActiveField.setBoolean(deflectorObj, false);
                }
            } catch (Throwable ignored) {}
            return false;
        }
    }

    private static Method wmRemoveStormMethod = null;
    private static Method wmSyncStormMethod = null;
    private static boolean wmMethodsInit = false;

    private static void initWeatherManagerMethods(Class<?> wmClass) {
        if (wmMethodsInit) return;
        wmMethodsInit = true;
        try {
            wmRemoveStormMethod = wmClass.getMethod("removeStormObject", long.class);
        } catch (Throwable ignored) {}
        try {
            wmSyncStormMethod = wmClass.getMethod("syncStormRemove", StormObject.class);
        } catch (Throwable ignored) {}
    }

    /**
     * Interacts deflector field with a storm.
     * Returns true if field collapsed/blacked out (stop scanning),
     * false if storm was successfully neutralized or skipped (continue scanning).
     */
    public static boolean processDeflectorStorm(Object deflectorObj, Object wmObj, Object stormObj) {
        if (deflectorObj == null || wmObj == null || stormObj == null) return false;
        StormObject storm = (StormObject) stormObj;
        if (storm.isDead) return false;

        long cost = getDeflectorDissipationCost(storm.levelCurIntensityStage);

        if (deflectorObj instanceof IDeflectorTE) {
            IDeflectorTE te = (IDeflectorTE) deflectorObj;
            long currentPower = te.getPower();

            if (cost < 0) {
                // Case 1: F5 / C5 Catastrophic Storm — OVERLOAD COLLAPSE!
                te.transitionTo(DeflectorState.OVERLOAD_COLLAPSED, COOLDOWN_OVERLOAD);
                playSound(
                    te.getDeflectorWorld(),
                    te.getDeflectorX() + 0.5D,
                    te.getDeflectorY() + 0.5D,
                    te.getDeflectorZ() + 0.5D,
                    "random.explode",
                    0.8F,
                    1.6F);
                playSound(
                    te.getDeflectorWorld(),
                    te.getDeflectorX() + 0.5D,
                    te.getDeflectorY() + 0.5D,
                    te.getDeflectorZ() + 0.5D,
                    "random.fizz",
                    1.2F,
                    0.5F);
                return true;
            } else if (currentPower >= cost) {
                // Case 2: Sufficient HE power to dissipate storm (F0..F4)
                te.setPower(currentPower - cost);

                if (wmObj instanceof WeatherManagerServer) {
                    WeatherManagerServer wm = (WeatherManagerServer) wmObj;
                    wm.removeStormObject(storm.ID);
                    wm.syncStormRemove(storm);
                } else {
                    initWeatherManagerMethods(wmObj.getClass());
                    try {
                        if (wmRemoveStormMethod != null) wmRemoveStormMethod.invoke(wmObj, storm.ID);
                        if (wmSyncStormMethod != null) wmSyncStormMethod.invoke(wmObj, storm);
                    } catch (Throwable ignored) {}
                }

                te.transitionTo(DeflectorState.DISSIPATING, COOLDOWN_DISSIPATING);
                playSound(
                    te.getDeflectorWorld(),
                    te.getDeflectorX() + 0.5D,
                    te.getDeflectorY() + 0.5D,
                    te.getDeflectorZ() + 0.5D,
                    "random.fizz",
                    1.0F,
                    1.2F);
                return false;
            } else {
                // Case 3: Insufficient HE power in buffer — POWER FAILURE BLACKOUT!
                te.transitionTo(DeflectorState.BLACKOUT_DEPLETED, COOLDOWN_BLACKOUT);
                playSound(
                    te.getDeflectorWorld(),
                    te.getDeflectorX() + 0.5D,
                    te.getDeflectorY() + 0.5D,
                    te.getDeflectorZ() + 0.5D,
                    "random.fizz",
                    1.0F,
                    0.6F);
                return true;
            }
        }

        // Reflective fallback for test mocks
        initDeflectorReflection(deflectorObj.getClass());

        long currentPower = 0;
        try {
            if (deflectorPowerField != null) {
                currentPower = deflectorPowerField.getLong(deflectorObj);
            }
        } catch (Throwable ignored) {}

        Object worldObj = null;
        int x = 0, y = 0, z = 0;
        try {
            Class<?> c = deflectorObj.getClass();
            worldObj = c.getField("field_145850_b")
                .get(deflectorObj);
            x = c.getField("field_145851_c")
                .getInt(deflectorObj);
            y = c.getField("field_145848_d")
                .getInt(deflectorObj);
            z = c.getField("field_145849_e")
                .getInt(deflectorObj);
        } catch (Throwable ignored) {}

        if (cost < 0) {
            triggerDeflectorOverload(deflectorObj, COOLDOWN_OVERLOAD);
            playSound(worldObj, x + 0.5, y + 0.5, z + 0.5, "random.explode", 0.8F, 1.6F);
            playSound(worldObj, x + 0.5, y + 0.5, z + 0.5, "random.fizz", 1.2F, 0.5F);
            return true;
        } else if (currentPower >= cost) {
            currentPower -= cost;
            try {
                if (deflectorPowerField != null) {
                    deflectorPowerField.setLong(deflectorObj, currentPower);
                }
            } catch (Throwable ignored) {}

            initWeatherManagerMethods(wmObj.getClass());
            try {
                if (wmRemoveStormMethod != null) {
                    wmRemoveStormMethod.invoke(wmObj, storm.ID);
                }
                if (wmSyncStormMethod != null) {
                    wmSyncStormMethod.invoke(wmObj, storm);
                }
            } catch (Throwable ignored) {}

            playSound(worldObj, x + 0.5, y + 0.5, z + 0.5, "random.fizz", 1.0F, 1.2F);
            return false;
        } else {
            triggerDeflectorOverload(deflectorObj, COOLDOWN_BLACKOUT);
            playSound(worldObj, x + 0.5, y + 0.5, z + 0.5, "random.fizz", 1.0F, 0.6F);
            return true;
        }
    }

    private static void triggerDeflectorOverload(Object deflectorObj, int cooldownTicks) {
        try {
            if (deflectorFieldActiveField != null) {
                deflectorFieldActiveField.setBoolean(deflectorObj, false);
            }
            if (deflectorBlackoutField != null) {
                deflectorBlackoutField.setInt(deflectorObj, cooldownTicks);
            }
        } catch (Throwable ignored) {}
    }

    private static Method worldPlaySoundMethod = null;
    private static boolean worldPlaySoundInit = false;

    private static void playSound(Object worldObj, double x, double y, double z, String name, float vol, float pitch) {
        if (worldObj == null) return;
        try {
            if (!worldPlaySoundInit) {
                worldPlaySoundInit = true;
                try {
                    worldPlaySoundMethod = worldObj.getClass()
                        .getMethod(
                            "playSoundEffect",
                            double.class,
                            double.class,
                            double.class,
                            String.class,
                            float.class,
                            float.class);
                } catch (Throwable t) {
                    try {
                        worldPlaySoundMethod = worldObj.getClass()
                            .getMethod(
                                "func_72908_a",
                                double.class,
                                double.class,
                                double.class,
                                String.class,
                                float.class,
                                float.class);
                    } catch (Throwable ignored) {}
                }
            }
            if (worldPlaySoundMethod != null) {
                worldPlaySoundMethod.invoke(worldObj, x, y, z, name, vol, pitch);
            }
        } catch (Throwable ignored) {}
    }
}
