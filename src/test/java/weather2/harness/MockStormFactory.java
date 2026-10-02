package weather2.harness;

import java.lang.reflect.Field;

import sun.misc.Unsafe;
import weather2.weathersystem.storm.StormObject;

/**
 * Headless factory for creating and configuring StormObject instances in tests
 * without triggering client/audio/OpenGL subcomponents.
 */
public final class MockStormFactory {

    private static final Unsafe unsafe;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to access sun.misc.Unsafe for headless test mocking", t);
        }
    }

    private MockStormFactory() {}

    /**
     * Creates a headless StormObject with the specified intensity stage and ID.
     * Stage 0..3: Rain / Thunder / High Wind / Hail
     * Stage 4: F0 / C0
     * Stage 5: F1 / C1
     * Stage 6: F2 / C2
     * Stage 7: F3 / C3
     * Stage 8: F4 / C4
     * Stage 9+: F5 / C5
     */
    public static StormObject create(int stage, long id, int dim) {
        try {
            StormObject storm = (StormObject) unsafe.allocateInstance(StormObject.class);
            storm.levelCurIntensityStage = stage;
            storm.ID = id;
            storm.isDead = false;
            storm.manager = new weather2.weathersystem.WeatherManagerBase(dim);
            return storm;
        } catch (Exception e) {
            throw new RuntimeException("Failed to allocate headless StormObject", e);
        }
    }

    public static StormObject create(int stage, long id) {
        return create(stage, id, 0);
    }

    /**
     * Creates a headless StormObject with default ID 1.
     */
    public static StormObject create(int stage) {
        return create(stage, 1L, 0);
    }
}
