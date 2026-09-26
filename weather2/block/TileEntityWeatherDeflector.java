package weather2.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import weather2.ServerTickHandler;
import weather2.weathersystem.WeatherManagerServer;
import weather2.weathersystem.storm.StormObject;

@Optional.InterfaceList({
    @Optional.Interface(iface = "api.hbm.energymk2.IEnergyReceiverMK2", modid = "hbm"),
    @Optional.Interface(iface = "cofh.api.energy.IEnergyReceiver", modid = "CoFHCore")
})
public class TileEntityWeatherDeflector extends TileEntity implements api.hbm.energymk2.IEnergyReceiverMK2, cofh.api.energy.IEnergyReceiver
{
    public int deflectorRadius = 150;

    // NTNH start: High Energy (HE) consumption & active field mechanics
    public long power = 0;
    public static final long maxPower = 2000000L;
    public static final long IDLE_DRAIN = 50000L;
    public static final long PULSE_DRAIN = 250000L;

    public boolean isFieldActive = false;
    private boolean prevFieldActive = false;

    @Override
    public void updateEntity()
    {
        if (!worldObj.isRemote) {
            long time = worldObj.getTotalWorldTime();

            // Periodic connection refresh for NTM MK2 energy network
            if (Loader.isModLoaded("hbm") && time % 20 == 0) {
                updateHbmConnections();
            }

            // Power drain: idle field maintenance
            if (this.power >= IDLE_DRAIN) {
                this.power -= IDLE_DRAIN;
                this.isFieldActive = true;
            } else {
                this.isFieldActive = false;
            }

            // Active deflector scan: 1-second interval (20 ticks) when powered
            if (this.isFieldActive && time % 20 == 0) {
                WeatherManagerServer wm = ServerTickHandler.lookupDimToWeatherMan.get(worldObj.provider.dimensionId);
                if (wm != null) {
                    Vec3 center = Vec3.createVectorHelper(xCoord, StormObject.layers.get(0), zCoord);
                    List<StormObject> storms = wm.getStormsAround(center, deflectorRadius);

                    for (int i = 0; i < storms.size(); i++) {
                        StormObject storm = storms.get(i);
                        if (storm != null && !storm.isDead) {
                            if (this.power >= PULSE_DRAIN) {
                                this.power -= PULSE_DRAIN;
                                wm.removeStormObject(storm.ID);
                                wm.syncStormRemove(storm);
                            } else {
                                // Blackout under severe overload
                                this.isFieldActive = false;
                                break;
                            }
                        }
                    }
                }
            }

            // Staggered network sync: only on state transition or throttled 40-tick interval
            if (this.isFieldActive != this.prevFieldActive || (time + (xCoord ^ zCoord)) % 40 == 0) {
                this.prevFieldActive = this.isFieldActive;
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                markDirty();
            }
        }
    }

    private void updateHbmConnections() {
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            this.trySubscribe(worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
        }
    }

    private void unsubscribeHbmConnections() {
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            this.tryUnsubscribe(worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (worldObj != null && !worldObj.isRemote && Loader.isModLoaded("hbm")) {
            unsubscribeHbmConnections();
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (worldObj != null && !worldObj.isRemote && Loader.isModLoaded("hbm")) {
            unsubscribeHbmConnections();
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        nbt.setLong("power", this.power);
        nbt.setBoolean("isFieldActive", this.isFieldActive);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        this.power = nbt.getLong("power");
        this.isFieldActive = nbt.getBoolean("isFieldActive");
        this.prevFieldActive = this.isFieldActive;
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        this.writeToNBT(nbt);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 0, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.func_148857_g());
    }

    // ==========================================
    // HBM NTM IEnergyReceiverMK2 Implementation
    // ==========================================

    @Override
    @Optional.Method(modid = "hbm")
    public boolean canConnect(ForgeDirection dir) {
        return true;
    }

    @Override
    @Optional.Method(modid = "hbm")
    public long getPower() {
        return this.power;
    }

    @Override
    @Optional.Method(modid = "hbm")
    public void setPower(long power) {
        this.power = Math.max(0, Math.min(maxPower, power));
    }

    @Override
    @Optional.Method(modid = "hbm")
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    @Optional.Method(modid = "hbm")
    public boolean isLoaded() {
        return !this.isInvalid();
    }

    @Override
    @Optional.Method(modid = "hbm")
    public Vec3 getDebugParticlePosMK2() {
        return Vec3.createVectorHelper(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D);
    }

    @Override
    @Optional.Method(modid = "hbm")
    public void provideInfoForECMK2(NBTTagCompound nbt) {
        nbt.setLong("power", this.power);
        nbt.setLong("maxPower", maxPower);
    }

    // ==========================================
    // CoFH RF IEnergyReceiver Implementation
    // ==========================================

    @Override
    @Optional.Method(modid = "CoFHCore")
    public boolean canConnectEnergy(ForgeDirection from) {
        return true;
    }

    @Override
    @Optional.Method(modid = "CoFHCore")
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        // 1 HE = 4 RF
        long heEquivalent = maxReceive / 4;
        long acceptedHE = Math.min(maxPower - this.power, heEquivalent);
        if (!simulate) {
            this.power += acceptedHE;
        }
        return (int) (acceptedHE * 4);
    }

    @Override
    @Optional.Method(modid = "CoFHCore")
    public int getEnergyStored(ForgeDirection from) {
        return (int) Math.min(Integer.MAX_VALUE, this.power * 4);
    }

    @Override
    @Optional.Method(modid = "CoFHCore")
    public int getMaxEnergyStored(ForgeDirection from) {
        return (int) Math.min(Integer.MAX_VALUE, maxPower * 4);
    }
    // NTNH end
}
