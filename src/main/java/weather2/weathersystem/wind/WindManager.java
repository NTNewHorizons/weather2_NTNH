package weather2.weathersystem.wind;

import java.util.Random;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import weather2.Weather;
import weather2.config.ConfigMisc;
import weather2.weathersystem.WeatherManagerBase;
import weather2.weathersystem.WeatherManagerServer;
import weather2.weathersystem.storm.StormObject;

public class WindManager {

    public WeatherManagerBase manager;
    public float windAngleGlobal = 0.0F;
    public float windSpeedGlobal = 0.0F;
    public float windSpeedGlobalChangeRate = 0.05F;
    public int windSpeedGlobalRandChangeTimer = 0;
    public int windSpeedGlobalRandChangeDelay = 10;
    public float windSpeedMin = 0.01F;
    public float windSpeedMax = 1.0F;
    public float windAngleEvent = 0.0F;
    public float windSpeedEvent = 0.0F;
    public int windTimeEvent = 0;
    public float windAngleGust = 0.0F;
    public float windSpeedGust = 0.0F;
    public int windTimeGust = 0;
    public int windGustEventTimeRand = 60;
    public float chanceOfWindGustEvent = 0.5F;
    public int lowWindTimer = 0;
    public int lowWindTimerEnableAmountBase = 2400;
    public int lowWindTimerEnableAmountRnd = 12000;
    public int lowWindOddsTo1 = 4000;

    public WindManager(WeatherManagerBase parManager) {
        this.manager = parManager;
        Random rand = new Random();
        this.windAngleGlobal = (float) rand.nextInt(360);
    }

    public float getWindSpeedForPriority() {
        return this.windTimeEvent > 0 ? this.getWindSpeedForEvents()
            : (this.windTimeGust > 0 ? this.getWindSpeedForGusts() : this.getWindSpeedForClouds());
    }

    public float getWindSpeedForEvents() {
        return this.windTimeEvent > 0 ? this.windSpeedEvent : 0.0F;
    }

    public float getWindSpeedForGusts() {
        return this.windSpeedGust;
    }

    public float getWindSpeedForClouds() {
        return this.windSpeedGlobal;
    }

    public float getWindAngleForPriority() {
        return this.windTimeEvent > 0 ? this.getWindAngleForEvents()
            : (this.windTimeGust > 0 ? this.getWindAngleForGusts() : this.getWindAngleForClouds());
    }

    public float getWindAngleForEvents() {
        return this.windAngleEvent;
    }

    public float getWindAngleForGusts() {
        return this.windAngleGust;
    }

    public float getWindAngleForClouds() {
        return this.windAngleGlobal;
    }

    public void setWindTimeGust(int parVal) {
        this.windTimeGust = parVal;
        this.syncData();
    }

    public void setWindTimeEvent(int parVal) {
        this.windTimeEvent = parVal;
    }

    public void tick() {
        Random rand = new Random();
        if (!ConfigMisc.Misc_windOn) {
            this.windSpeedGlobal = 0.0F;
            this.windSpeedGust = 0.0F;
            this.windTimeGust = 0;
        } else if (!this.manager.getWorld().isRemote) {
            if (!ConfigMisc.Wind_NoWindEvents) {
                this.lowWindTimer = 0;
            }

            if (this.lowWindTimer <= 0) {
                if (this.windSpeedGlobalRandChangeTimer-- <= 0) {
                    this.windSpeedGlobal = (float) ((double) this.windSpeedGlobal
                        + (rand.nextDouble() * (double) this.windSpeedGlobalChangeRate
                            - (double) (this.windSpeedGlobalChangeRate / 2.0F)));
                    this.windSpeedGlobalRandChangeTimer = this.windSpeedGlobalRandChangeDelay;
                }

                if (ConfigMisc.Wind_NoWindEvents && rand.nextInt(this.lowWindOddsTo1) == 0) {
                    this.lowWindTimer = this.lowWindTimerEnableAmountBase
                        + rand.nextInt(this.lowWindTimerEnableAmountRnd);
                    Weather.dbg("no wind event, for ticks: " + this.lowWindTimer);
                }
            } else {
                --this.lowWindTimer;
                this.windSpeedGlobal -= 0.01F;
            }

            if (this.windSpeedGlobal < this.windSpeedMin) {
                this.windSpeedGlobal = this.windSpeedMin;
            }

            if (this.windSpeedGlobal > this.windSpeedMax) {
                this.windSpeedGlobal = this.windSpeedMax;
            }

            if (this.windTimeGust > 0) {
                --this.windTimeGust;
                if (this.windTimeGust == 0) {
                    this.syncData();
                }
            }

            float randGustWindFactor = 1.0F;
            if (this.windTimeGust == 0 && this.lowWindTimer <= 0
                && this.chanceOfWindGustEvent > 0.0F
                && rand.nextInt((int) ((100.0F - this.chanceOfWindGustEvent) * randGustWindFactor)) == 0) {
                this.windSpeedGust = this.windSpeedGlobal + rand.nextFloat() * 0.6F;
                this.windAngleGust = (float) (rand.nextInt(360) - 180);
                this.setWindTimeGust(rand.nextInt(this.windGustEventTimeRand));
            }

            this.windAngleGlobal += (float) ((new Random()).nextInt(5) - 2) * 0.5F;
            if (this.windAngleGlobal < -180.0F) {
                this.windAngleGlobal += 360.0F;
            }

            if (this.windAngleGlobal > 180.0F) {
                this.windAngleGlobal -= 360.0F;
            }
        } else {
            this.tickClient();
        }

    }

    @SideOnly(Side.CLIENT)
    public void tickClient() {
        EntityClientPlayerMP entP = FMLClientHandler.instance()
            .getClient().thePlayer;
        if (this.windTimeEvent > 0) {
            --this.windTimeEvent;
        }

        if (entP != null && this.manager.getWorld()
            .getTotalWorldTime() % 10L == 0L) {
            StormObject so = this.manager.getClosestStorm(
                Vec3.createVectorHelper(
                    entP.posX,
                    (double) ((Integer) StormObject.layers.get(0)).intValue(),
                    entP.posZ),
                256.0D,
                StormObject.STATE_HIGHWIND);
            if (so != null) {
                this.setWindTimeEvent(80);
                entP.getDistance(so.posGround.xCoord, so.posGround.yCoord, so.posGround.zCoord);
                double var11 = so.posGround.xCoord - entP.posX;
                double var15 = so.posGround.zCoord - entP.posZ;
                float yaw = -((float) Math.atan2(var11, var15)) * 180.0F / 3.1415927F;
                this.windAngleEvent = yaw;
                this.windSpeedEvent = 2.0F;
            }
        }

    }

    public NBTTagCompound nbtSyncForClient() {
        NBTTagCompound data = new NBTTagCompound();
        data.setFloat("windSpeedGlobal", this.windSpeedGlobal);
        data.setFloat("windAngleGlobal", this.windAngleGlobal);
        data.setFloat("windSpeedGust", this.windSpeedGust);
        data.setFloat("windAngleGust", this.windAngleGust);
        data.setInteger("windTimeGust", this.windTimeGust);
        return data;
    }

    public void nbtSyncFromServer(NBTTagCompound parNBT) {
        this.windSpeedGlobal = parNBT.getFloat("windSpeedGlobal");
        this.windAngleGlobal = parNBT.getFloat("windAngleGlobal");
        this.windSpeedGust = parNBT.getFloat("windSpeedGust");
        this.windAngleGust = parNBT.getFloat("windAngleGust");
        this.windTimeGust = parNBT.getInteger("windTimeGust");
    }

    public void syncData() {
        if (this.manager instanceof WeatherManagerServer) {
            ((WeatherManagerServer) this.manager).syncWindUpdate(this);
        }

    }

    public void reset() {
        this.manager = null;
    }
}
