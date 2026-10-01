package weather2.block;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import weather2.ServerTickHandler;
import weather2.compat.WeatherNTNHHooks;
import weather2.deflector.DeflectorState;
import weather2.weathersystem.WeatherManagerServer;
import weather2.weathersystem.storm.StormObject;

// NTNH start: High Energy (HE) consumption & explicit State Machine
@Optional.InterfaceList({ @Optional.Interface(iface = "api.hbm.energymk2.IEnergyReceiverMK2", modid = "hbm"),
    @Optional.Interface(iface = "cofh.api.energy.IEnergyReceiver", modid = "CoFHCore") })
public class TileEntityWeatherDeflector extends TileEntity
    implements weather2.deflector.IDeflectorTE, api.hbm.energymk2.IEnergyReceiverMK2, cofh.api.energy.IEnergyReceiver {

    public int deflectorRadius = 150;

    public long power = 0;
    public static final long maxPower = 100000000L;
    public static final long IDLE_DRAIN = 25000L;

    // Explicit State Machine
    public DeflectorState state = DeflectorState.OFFLINE;
    public int stateTimer = 0;

    // Backward-compatibility mirror fields
    public boolean isFieldActive = false;
    private boolean prevFieldActive = false;
    private DeflectorState prevState = DeflectorState.OFFLINE;
    public int blackoutCooldown = 0;

    @Override
    public Object getDeflectorWorld() {
        return this.worldObj;
    }

    @Override
    public int getDeflectorX() {
        return this.xCoord;
    }

    @Override
    public int getDeflectorY() {
        return this.yCoord;
    }

    @Override
    public int getDeflectorZ() {
        return this.zCoord;
    }

    public DeflectorState getState() {
        return this.state != null ? this.state : DeflectorState.OFFLINE;
    }

    public int getStateTimer() {
        return this.stateTimer;
    }

    public void setStateTimer(int timer) {
        this.stateTimer = timer;
        this.blackoutCooldown = timer;
    }

    public int getComparatorOutput() {
        return this.state != null ? this.state.getComparatorSignal() : 0;
    }

    public void transitionTo(DeflectorState newState, int timer) {
        if (newState == null) newState = DeflectorState.OFFLINE;
        this.state = newState;
        this.stateTimer = timer;
        this.isFieldActive = newState.isFieldActive();
        this.blackoutCooldown = (newState == DeflectorState.BLACKOUT_DEPLETED
            || newState == DeflectorState.OVERLOAD_COLLAPSED) ? timer : 0;

        if (worldObj != null && !worldObj.isRemote) {
            this.markDirty();
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, this.getBlockType());
            worldObj.func_147453_f(xCoord, yCoord, zCoord, this.getBlockType());
        }
    }

    @Override
    public void updateEntity() {
        if (!worldObj.isRemote) {
            long time = worldObj.getTotalWorldTime();

            // Periodic connection refresh for NTM MK2 energy network (staggered by coordinates)
            if (Loader.isModLoaded("hbm") && (time + (xCoord ^ zCoord)) % 20 == 0) {
                updateHbmConnections();
            }

            // Power drain & FSM tick
            WeatherNTNHHooks.tickDeflectorPower(this);

            // Active deflector scan: 1-second interval (20 ticks, staggered) when field is active
            if (this.isFieldActive && (time + (xCoord ^ zCoord)) % 20 == 0) {
                WeatherManagerServer wm = (WeatherManagerServer) ServerTickHandler.lookupDimToWeatherMan
                    .get(worldObj.provider.dimensionId);
                if (wm != null) {
                    Vec3 center = Vec3
                        .createVectorHelper(xCoord, ((Integer) StormObject.layers.get(0)).doubleValue(), zCoord);
                    List storms = wm.getStormsAround(center, deflectorRadius);

                    for (int i = 0; i < storms.size(); i++) {
                        StormObject storm = (StormObject) storms.get(i);
                        if (storm != null && !storm.isDead) {
                            boolean collapsed = WeatherNTNHHooks.processDeflectorStorm(this, wm, storm);
                            if (collapsed) {
                                break;
                            }
                        }
                    }
                }
            }

            // Staggered network sync: state transition triggers update packet, periodic 40-tick sync updates energy
            if (this.state != this.prevState || this.isFieldActive != this.prevFieldActive) {
                this.prevState = this.state;
                this.prevFieldActive = this.isFieldActive;
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                markDirty();
            } else if ((time + (xCoord ^ zCoord)) % 40 == 0) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
        } else {
            // Client-side ambient & diagnostic effects according to FSM state
            spawnClientStateEffects();
        }
    }

    private void spawnClientStateEffects() {
        if (this.state == null) return;
        switch (this.state) {
            case ACTIVE:
                if (worldObj.rand.nextInt(4) == 0) {
                    worldObj.spawnParticle(
                        "reddust",
                        xCoord + 0.5D + (worldObj.rand.nextDouble() - 0.5D) * 0.4D,
                        yCoord + 1.1D,
                        zCoord + 0.5D + (worldObj.rand.nextDouble() - 0.5D) * 0.4D,
                        0.0D,
                        0.6D,
                        1.0D);
                }
                break;
            case DISSIPATING:
                for (int i = 0; i < 3; i++) {
                    worldObj.spawnParticle(
                        "fireworksSpark",
                        xCoord + 0.5D,
                        yCoord + 1.2D,
                        zCoord + 0.5D,
                        (worldObj.rand.nextDouble() - 0.5D) * 0.2D,
                        0.3D,
                        (worldObj.rand.nextDouble() - 0.5D) * 0.2D);
                }
                break;
            case BLACKOUT_DEPLETED:
                if (worldObj.rand.nextInt(3) == 0) {
                    worldObj.spawnParticle("smoke", xCoord + 0.5D, yCoord + 0.8D, zCoord + 0.5D, 0.0D, 0.04D, 0.0D);
                    worldObj.spawnParticle(
                        "crit",
                        xCoord + 0.5D,
                        yCoord + 0.8D,
                        zCoord + 0.5D,
                        (worldObj.rand.nextDouble() - 0.5D) * 0.1D,
                        0.1D,
                        (worldObj.rand.nextDouble() - 0.5D) * 0.1D);
                }
                break;
            case OVERLOAD_COLLAPSED:
                if (worldObj.rand.nextInt(2) == 0) {
                    worldObj
                        .spawnParticle("largesmoke", xCoord + 0.5D, yCoord + 0.9D, zCoord + 0.5D, 0.0D, 0.06D, 0.0D);
                }
                if (worldObj.rand.nextInt(5) == 0) {
                    worldObj.spawnParticle(
                        "flame",
                        xCoord + 0.5D,
                        yCoord + 0.8D,
                        zCoord + 0.5D,
                        (worldObj.rand.nextDouble() - 0.5D) * 0.05D,
                        0.03D,
                        (worldObj.rand.nextDouble() - 0.5D) * 0.05D);
                }
                break;
            case OFFLINE:
            default:
                break;
        }
    }

    @Optional.Method(modid = "hbm")
    private void updateHbmConnections() {
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            this.trySubscribe(worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
        }
    }

    @Optional.Method(modid = "hbm")
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
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setLong("power", this.power);
        nbt.setByte("deflectorState", (byte) (this.state != null ? this.state.getId() : 0));
        nbt.setInteger("stateTimer", this.stateTimer);
        // Legacy keys for backward compatibility
        nbt.setBoolean("isFieldActive", this.state != null ? this.state.isFieldActive() : false);
        nbt.setInteger("blackoutCooldown", this.blackoutCooldown);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.power = nbt.getLong("power");
        if (nbt.hasKey("deflectorState")) {
            this.state = DeflectorState.fromId(nbt.getByte("deflectorState"));
            this.stateTimer = nbt.getInteger("stateTimer");
        } else {
            this.blackoutCooldown = nbt.getInteger("blackoutCooldown");
            boolean fieldActive = nbt.getBoolean("isFieldActive");
            if (this.blackoutCooldown > 0) {
                this.state = DeflectorState.BLACKOUT_DEPLETED;
                this.stateTimer = this.blackoutCooldown;
            } else if (fieldActive) {
                this.state = DeflectorState.ACTIVE;
                this.stateTimer = 0;
            } else {
                this.state = DeflectorState.OFFLINE;
                this.stateTimer = 0;
            }
        }
        this.isFieldActive = this.state.isFieldActive();
        this.prevFieldActive = this.isFieldActive;
        this.prevState = this.state;
        this.blackoutCooldown = (this.state == DeflectorState.BLACKOUT_DEPLETED
            || this.state == DeflectorState.OVERLOAD_COLLAPSED) ? this.stateTimer : 0;
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
