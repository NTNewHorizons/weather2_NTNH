package weather2.integration.hbm;

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
 */
public class HbmEnergyAdapter implements DiagnosableAdapter {

    public static final HbmEnergyAdapter INSTANCE = new HbmEnergyAdapter();

    public static final long MAX_DEFLECTOR_POWER = 100000000L;
    public static final long IDLE_DRAIN = 25000L;

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
            return AdapterReport.active(
                getAdapterName(),
                getTargetModId(),
                "IEnergyReceiverMK2 linked, MK2 power network interface ready");
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
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                receiver.trySubscribe(world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ, dir);
            }
        }
    }

    @Optional.Method(modid = "hbm")
    public static void unsubscribeConnections(TileEntity te, World world, int x, int y, int z) {
        if (world == null || te == null) return;
        if (te instanceof IEnergyReceiverMK2) {
            IEnergyReceiverMK2 receiver = (IEnergyReceiverMK2) te;
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                receiver.tryUnsubscribe(world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
            }
        }
    }

    public static long clampPower(long power, long max) {
        return Math.max(0, Math.min(max, power));
    }
}
