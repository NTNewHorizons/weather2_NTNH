package weather2.block;

import java.util.Random;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;

import weather2.ServerTickHandler;
import weather2.config.ConfigMisc;
import weather2.weathersystem.WeatherManagerServer;
import weather2.weathersystem.storm.StormObject;

public class TileEntityWeatherMachine extends TileEntity {

    public int weatherType = 1;
    public int weatherIntensity = 0;
    public int weatherRate = 0;
    public int weatherSize = 50;
    public boolean lockStormHere = true;
    public StormObject lastTickStormObject = null;

    public void cycleWeatherType() {
        ++this.weatherType;
        byte maxID = 6;
        if (ConfigMisc.Storm_NoTornadosOrCyclones || ConfigMisc.Block_WeatherMachineNoTornadosOrCyclones) {
            maxID = 4;
        }

        if (this.weatherType > maxID) {
            this.weatherType = 1;
        }

    }

    public void invalidate() {
        super.invalidate();
        WeatherManagerServer wm = (WeatherManagerServer) ServerTickHandler.lookupDimToWeatherMan
            .get(Integer.valueOf(this.worldObj.provider.dimensionId));
        if (wm != null && this.lastTickStormObject != null) {
            wm.removeStormObject(this.lastTickStormObject.ID);
            wm.syncStormRemove(this.lastTickStormObject);
        }

    }

    public void updateEntity() {
        if (!this.worldObj.isRemote) {
            this.weatherSize = 100;
            if (this.worldObj.getTotalWorldTime() % 40L == 0L) {
                if (this.lastTickStormObject != null && this.lastTickStormObject.isDead) {
                    this.lastTickStormObject = null;
                }

                if (this.lastTickStormObject == null) {
                    WeatherManagerServer rand = (WeatherManagerServer) ServerTickHandler.lookupDimToWeatherMan
                        .get(Integer.valueOf(this.worldObj.provider.dimensionId));
                    if (rand != null) {
                        StormObject so = new StormObject(rand);
                        so.initFirstTime();
                        so.pos = Vec3.createVectorHelper(
                            (double) this.xCoord,
                            (double) ((Integer) StormObject.layers.get(0)).intValue(),
                            (double) this.zCoord);
                        so.layer = 0;
                        so.userSpawnedFor = "" + this.xCoord + this.yCoord + this.zCoord;
                        so.naturallySpawned = false;
                        rand.addStormObject(so);
                        rand.syncStormNew(so);
                        this.lastTickStormObject = so;
                    }
                }
            }

            if (this.lastTickStormObject != null && !this.lastTickStormObject.isDead) {
                new Random();
                if (this.lockStormHere) {
                    this.lastTickStormObject.pos = Vec3.createVectorHelper(
                        (double) this.xCoord,
                        (double) ((Integer) StormObject.layers.get(0)).intValue(),
                        (double) this.zCoord);
                }

                this.lastTickStormObject.size = this.weatherSize;
                this.lastTickStormObject.levelWater = 1000;
                this.lastTickStormObject.attrib_precipitation = true;
                this.lastTickStormObject.hasStormPeaked = false;
                this.lastTickStormObject.levelCurStagesIntensity = 0.9F;
                this.lastTickStormObject.levelCurIntensityStage = StormObject.STATE_NORMAL;
                this.lastTickStormObject.stormType = StormObject.TYPE_LAND;
                this.lastTickStormObject.levelTemperature = 40.0F;
                if (this.weatherType == 0) {
                    this.lastTickStormObject.levelTemperature = -40.0F;
                } else if (this.weatherType != 1) {
                    if (this.weatherType == 2) {
                        this.lastTickStormObject.stormType = StormObject.TYPE_LAND;
                        this.lastTickStormObject.levelCurIntensityStage = StormObject.STATE_THUNDER;
                    } else if (this.weatherType == 3) {
                        this.lastTickStormObject.stormType = StormObject.TYPE_LAND;
                        this.lastTickStormObject.levelCurIntensityStage = StormObject.STATE_HIGHWIND;
                    } else if (this.weatherType == 4) {
                        this.lastTickStormObject.stormType = StormObject.TYPE_LAND;
                        this.lastTickStormObject.levelCurIntensityStage = StormObject.STATE_HAIL;
                    } else if (this.weatherType == 5) {
                        this.lastTickStormObject.stormType = StormObject.TYPE_LAND;
                        this.lastTickStormObject.levelCurIntensityStage = StormObject.STATE_STAGE1;
                    } else if (this.weatherType == 6) {
                        this.lastTickStormObject.stormType = StormObject.TYPE_WATER;
                        this.lastTickStormObject.levelCurIntensityStage = StormObject.STATE_STAGE1;
                    }
                }
            }
        }

    }

    public void writeToNBT(NBTTagCompound var1) {
        super.writeToNBT(var1);
        var1.setInteger("weatherType", this.weatherType);
    }

    public void readFromNBT(NBTTagCompound var1) {
        super.readFromNBT(var1);
        this.weatherType = var1.getInteger("weatherType");
    }
}
