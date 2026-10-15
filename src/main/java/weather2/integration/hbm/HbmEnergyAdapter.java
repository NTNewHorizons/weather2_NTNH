package weather2.integration.hbm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import api.hbm.energymk2.IEnergyReceiverMK2;
import cpw.mods.fml.common.Optional;
import weather2.integration.AdapterReport;
import weather2.integration.DiagnosableAdapter;
import weather2.integration.IntegrationManager;

/**
 * NTNH Deep Module: HBM NTM Energy Network Adapter.
 * Manages MK2 power network connections and subscribers for deflector and industrial tiles.
 * Provides resilient, version-agnostic connection handling across NTM X5785 - X5849+.
 */
public class HbmEnergyAdapter implements DiagnosableAdapter {

    public static final HbmEnergyAdapter INSTANCE = new HbmEnergyAdapter();

    public static final long MAX_DEFLECTOR_POWER = 100000000L;
    public static final long IDLE_DRAIN = 25000L;

    // Reflection cache for version-agnostic NTM integration
    private static boolean reflectionInitialized = false;
    private static Method legacyTrySubscribe = null;
    private static Method legacyTryUnsubscribe = null;
    private static Method getNodeMethod = null;
    private static Field netField = null;
    private static Method addReceiverMethod = null;
    private static Method removeReceiverMethod = null;

    private static synchronized void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;

        // 1. Try legacy IEnergyReceiverMK2 helper methods (present in NTM X5785..X5837)
        try {
            legacyTrySubscribe = IEnergyReceiverMK2.class
                .getMethod("trySubscribe", World.class, int.class, int.class, int.class, ForgeDirection.class);
        } catch (Throwable ignored) {
            legacyTrySubscribe = null;
        }

        try {
            legacyTryUnsubscribe = IEnergyReceiverMK2.class
                .getMethod("tryUnsubscribe", World.class, int.class, int.class, int.class);
        } catch (Throwable ignored) {
            legacyTryUnsubscribe = null;
        }

        // 2. Try Nodespace / NodeNet direct connection (present in NTM X5849+)
        try {
            Class<?> nodespaceClass = Class.forName("api.hbm.energymk2.Nodespace");
            getNodeMethod = nodespaceClass.getMethod("getNode", World.class, int.class, int.class, int.class);

            Class<?> genNodeClass = Class.forName("com.hbm.uninos.GenNode");
            netField = genNodeClass.getField("net");

            Class<?> nodeNetClass = Class.forName("com.hbm.uninos.NodeNet");
            for (Method m : nodeNetClass.getMethods()) {
                if (m.getName()
                    .equals("addReceiver") && m.getParameterTypes().length == 1) {
                    addReceiverMethod = m;
                } else if (m.getName()
                    .equals("removeReceiver") && m.getParameterTypes().length == 1) {
                        removeReceiverMethod = m;
                    }
            }
        } catch (Throwable ignored) {
            getNodeMethod = null;
            netField = null;
            addReceiverMethod = null;
            removeReceiverMethod = null;
        }
    }

    @Override
    public String getAdapterName() {
        return "HBM Energy Adapter";
    }

    @Override
    public String getTargetModId() {
        return IntegrationManager.MODID_HBM;
    }

    @Override
    public AdapterReport diagnose() {
        if (!IntegrationManager.isHbmLoaded()) {
            return AdapterReport.notInstalled(getAdapterName(), getTargetModId());
        }

        try {
            Class.forName("api.hbm.energymk2.IEnergyReceiverMK2");
            initReflection();
            String mode = (legacyTrySubscribe != null) ? "legacy trySubscribe" : "Nodespace power net";
            return AdapterReport.active(
                getAdapterName(),
                getTargetModId(),
                "IEnergyReceiverMK2 linked (" + mode + "), MK2 power network interface ready");
        } catch (Throwable t) {
            return AdapterReport.failed(
                getAdapterName(),
                getTargetModId(),
                "api.hbm.energymk2.IEnergyReceiverMK2 missing from classpath (" + t.getMessage() + ")");
        }
    }

    @Optional.Method(modid = "hbm")
    public static void updateConnections(TileEntity te, World world, int x, int y, int z) {
        if (world == null || te == null) return;
        if (te instanceof IEnergyReceiverMK2) {
            IEnergyReceiverMK2 receiver = (IEnergyReceiverMK2) te;
            initReflection();

            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                int nx = x + dir.offsetX;
                int ny = y + dir.offsetY;
                int nz = z + dir.offsetZ;

                try {
                    if (legacyTrySubscribe != null) {
                        legacyTrySubscribe.invoke(receiver, world, nx, ny, nz, dir);
                    } else if (getNodeMethod != null && netField != null && addReceiverMethod != null) {
                        Object node = getNodeMethod.invoke(null, world, nx, ny, nz);
                        if (node != null) {
                            Object net = netField.get(node);
                            if (net != null) {
                                addReceiverMethod.invoke(net, receiver);
                            }
                        }
                    }
                } catch (Throwable ignored) {
                    // Hot-loop circuit breaker: never throw to caller on third-party API changes
                }
            }
        }
    }

    @Optional.Method(modid = "hbm")
    public static void unsubscribeConnections(TileEntity te, World world, int x, int y, int z) {
        if (world == null || te == null) return;
        if (te instanceof IEnergyReceiverMK2) {
            IEnergyReceiverMK2 receiver = (IEnergyReceiverMK2) te;
            initReflection();

            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                int nx = x + dir.offsetX;
                int ny = y + dir.offsetY;
                int nz = z + dir.offsetZ;

                try {
                    if (legacyTryUnsubscribe != null) {
                        legacyTryUnsubscribe.invoke(receiver, world, nx, ny, nz);
                    } else if (getNodeMethod != null && netField != null && removeReceiverMethod != null) {
                        Object node = getNodeMethod.invoke(null, world, nx, ny, nz);
                        if (node != null) {
                            Object net = netField.get(node);
                            if (net != null) {
                                removeReceiverMethod.invoke(net, receiver);
                            }
                        }
                    }
                } catch (Throwable ignored) {
                    // Safe cleanup
                }
            }
        }
    }

    public static long clampPower(long power, long max) {
        return Math.max(0, Math.min(max, power));
    }
}
