package weather2.weathersystem;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import CoroUtil.packet.PacketHelper;
import CoroUtil.util.CoroUtilEntity;
import cpw.mods.fml.common.event.FMLInterModComms;
import weather2.Weather;
import weather2.config.ConfigMisc;
import weather2.entity.EntityLightningBolt;
import weather2.util.WeatherUtilConfig;
import weather2.volcano.VolcanoObject;
import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.wind.WindManager;

public class WeatherManagerServer extends WeatherManagerBase {

    public int syncRange = 256;
    private int tickerSyncWeatherCheckVanilla = 0;
    private int tickerSyncWeatherLowWind = 0;
    private int tickerSyncWeatherHighWind = 0;
    private int tickerSyncVolcanos = 0;
    private int tickerSyncWindAndIMC = 0;
    private int tickerSyncStormSpawnOrRemoveChecks = 0;

    public WeatherManagerServer(int parDim) {
        super(parDim);
    }

    public World getWorld() {
        return DimensionManager.getWorld(this.dim);
    }

    public void tick() {
        super.tick();
        ++this.tickerSyncWeatherCheckVanilla;
        ++this.tickerSyncWeatherLowWind;
        ++this.tickerSyncWeatherHighWind;
        ++this.tickerSyncVolcanos;
        ++this.tickerSyncWindAndIMC;
        ++this.tickerSyncStormSpawnOrRemoveChecks;
        World world = this.getWorld();
        if (StormObject.lastUsedStormID >= Long.MAX_VALUE) {
            StormObject.lastUsedStormID = 0L;
        }

        if (world != null) {
            // NTNH start: periodic cold-path reconciliation of moving blocks budget (every 100 ticks / 5s)
            if (world.getTotalWorldTime() % 100L == 0L && world.provider != null) {
                weather2.compat.WeatherNTNHHooks
                    .reconcileMovingBlocks(world.provider.dimensionId, this.getStormObjects());
            }
            // NTNH end

            if (!ConfigMisc.overcastMode && ConfigMisc.lockServerWeatherMode != -1) {
                world.getWorldInfo()
                    .setRaining(ConfigMisc.lockServerWeatherMode == 1);
                world.getWorldInfo()
                    .setThundering(ConfigMisc.lockServerWeatherMode == 1);
            }

            if (ConfigMisc.preventServerThunderstorms) {
                world.getWorldInfo()
                    .setThundering(false);
            }

            if (this.tickerSyncWeatherCheckVanilla == ConfigMisc.tickerRateSyncWeatherCheckVanilla) {
                this.isVanillaRainActiveOnServer = this.getWorld()
                    .isRaining();
                this.syncWeatherVanilla();
                this.tickerSyncWeatherCheckVanilla = 0;
            }

            boolean shouldUpdateHighWind = false;
            boolean shouldUpdateLowWind = false;
            if (this.tickerSyncWeatherHighWind == ConfigMisc.tickerRateSyncWeatherHighWind) {
                shouldUpdateHighWind = true;
                this.tickerSyncWeatherHighWind = 0;
            }

            if (this.tickerSyncWeatherLowWind == ConfigMisc.tickerRateSyncWeatherLowWind) {
                shouldUpdateLowWind = true;
                this.tickerSyncWeatherLowWind = 0;
            }

            int i;
            if (shouldUpdateHighWind || shouldUpdateLowWind) {
                HashSet rand = new HashSet();

                for (i = 0; i < this.getStormObjects()
                    .size(); ++i) {
                    StormObject entP = (StormObject) this.getStormObjects()
                        .get(i);
                    if (entP.levelCurIntensityStage >= StormObject.STATE_HIGHWIND) {
                        if (shouldUpdateHighWind) {
                            rand.add(entP.nbtSyncForClient());
                        }
                    } else if (shouldUpdateLowWind) {
                        rand.add(entP.nbtSyncForClient());
                    }
                }

                if (rand.size() > 0) {
                    this.syncStormUpdate(rand);
                }
            }

            int var7;
            if (this.tickerSyncVolcanos == ConfigMisc.tickerRateSyncVolcanos) {
                this.tickerSyncVolcanos = 0;

                for (var7 = 0; var7 < this.getVolcanoObjects()
                    .size(); ++var7) {
                    this.syncVolcanoUpdate(
                        (VolcanoObject) this.getVolcanoObjects()
                            .get(var7));
                }
            }

            if (this.tickerSyncWindAndIMC == ConfigMisc.tickerRateSyncWindAndIMC) {
                this.syncWindUpdate(this.windMan);
                this.nbtStormsForIMC();
                this.tickerSyncWindAndIMC = 0;
            }

            if (WeatherUtilConfig.listDimensionsClouds.contains(Integer.valueOf(world.provider.dimensionId))
                && this.tickerSyncStormSpawnOrRemoveChecks == ConfigMisc.tickerRateSyncStormSpawnOrRemoveChecks) {
                this.tickerSyncStormSpawnOrRemoveChecks = 0;

                EntityPlayer var10;
                for (var7 = 0; var7 < this.getStormObjects()
                    .size(); ++var7) {
                    StormObject var9 = (StormObject) this.getStormObjects()
                        .get(var7);
                    var10 = world.getClosestPlayer(
                        var9.posGround.xCoord,
                        var9.posGround.yCoord,
                        var9.posGround.zCoord,
                        (double) ConfigMisc.Misc_simBoxRadiusCutoff);
                    if (var10 == null) {
                        this.removeStormObject(var9.ID);
                        this.syncStormRemove(var9);
                    }
                }

                Random var8 = new Random();

                for (i = 0; i < world.playerEntities.size(); ++i) {
                    var10 = (EntityPlayer) world.playerEntities.get(i);
                    if (this.getStormObjectsByLayer(0)
                        .size() < ConfigMisc.Storm_MaxPerPlayerPerLayer * world.playerEntities.size()
                        && var8.nextInt(5) == 0) {
                        this.trySpawnNearPlayerForLayer(var10, 0);
                    }

                    if (this.getStormObjectsByLayer(1)
                        .size() < ConfigMisc.Storm_MaxPerPlayerPerLayer * world.playerEntities.size()
                        && ConfigMisc.Cloud_Layer1_Enable
                        && var8.nextInt(5) == 0) {
                        ;
                    }
                }
            }
        }

    }

    public void trySpawnNearPlayerForLayer(EntityPlayer entP, int layer) {
        Random rand = new Random();
        byte tryCountMax = 10;
        int tryCountCur = 0;
        boolean spawnX = true;
        boolean spawnZ = true;
        Vec3 tryPos = null;
        StormObject soClose = null;
        EntityPlayer playerClose = null;
        short closestToPlayer = 128;
        float windOffsetDist = (float) Math.min(256, ConfigMisc.Misc_simBoxRadiusCutoff / 4 * 3);
        double angle = (double) this.windMan.getWindAngleForClouds();
        double vecX = -Math.sin(Math.toRadians(angle)) * (double) windOffsetDist;

        int var20;
        int var21;
        for (double vecZ = Math.cos(Math.toRadians(angle)) * (double) windOffsetDist; tryCountCur++ == 0
            || tryCountCur < tryCountMax && (soClose != null || playerClose != null); playerClose = entP.worldObj
                .getClosestPlayer((double) var20, 50.0D, (double) var21, (double) closestToPlayer)) {
            var20 = (int) (entP.posX - vecX
                + (double) rand.nextInt(ConfigMisc.Misc_simBoxRadiusSpawn)
                - (double) rand.nextInt(ConfigMisc.Misc_simBoxRadiusSpawn));
            var21 = (int) (entP.posZ - vecZ
                + (double) rand.nextInt(ConfigMisc.Misc_simBoxRadiusSpawn)
                - (double) rand.nextInt(ConfigMisc.Misc_simBoxRadiusSpawn));
            tryPos = Vec3.createVectorHelper(
                (double) var20,
                (double) ((Integer) StormObject.layers.get(layer)).intValue(),
                (double) var21);
            soClose = this.getClosestStormAny(tryPos, (double) ConfigMisc.Cloud_Formation_MinDistBetweenSpawned);
        }

        if (soClose == null) {
            StormObject so = new StormObject(this);
            so.initFirstTime();
            so.pos = tryPos;
            so.layer = layer;
            so.userSpawnedFor = CoroUtilEntity.getName(entP);
            this.addStormObject(so);
            this.syncStormNew(so);
        } else {
            Weather.dbg("couldnt find space to spawn cloud formation");
        }

    }

    public void playerJoinedServerSyncFull(EntityPlayerMP entP) {
        World world = this.getWorld();
        if (world != null) {
            int i;
            for (i = 0; i < this.getStormObjects()
                .size(); ++i) {
                this.syncStormNew(
                    (StormObject) this.getStormObjects()
                        .get(i),
                    entP);
            }

            for (i = 0; i < this.getVolcanoObjects()
                .size(); ++i) {
                this.syncVolcanoNew(
                    (VolcanoObject) this.getVolcanoObjects()
                        .get(i),
                    entP);
            }
        }

    }

    public void nbtStormsForIMC() {
        NBTTagCompound data = new NBTTagCompound();

        for (int i = 0; i < this.getStormObjects()
            .size(); ++i) {
            StormObject so = (StormObject) this.getStormObjects()
                .get(i);
            if (so.levelCurIntensityStage > 0 || so.attrib_precipitation) {
                NBTTagCompound nbtStorm = so.nbtForIMC();
                data.setTag("storm_" + so.ID, nbtStorm);
            }
        }

        if (!data.hasNoTags()) {
            FMLInterModComms.sendRuntimeMessage(Weather.instance, Weather.modID, "weather.storms", data);
        }

    }

    public void syncLightningNew(EntityLightningBolt parEnt) {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("packetCommand", "WeatherData");
        data.setString("command", "syncLightningNew");
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("posX", MathHelper.floor_double(parEnt.posX));
        nbt.setInteger("posY", MathHelper.floor_double(parEnt.posY));
        nbt.setInteger("posZ", MathHelper.floor_double(parEnt.posZ));
        nbt.setInteger("entityID", parEnt.getEntityId());
        data.setTag("data", nbt);
        Weather.eventChannel.sendToDimension(
            PacketHelper.getNBTPacket(data, Weather.eventChannelName),
            this.getWorld().provider.dimensionId);
        FMLInterModComms.sendRuntimeMessage(Weather.instance, Weather.modID, "weather.lightning", data);
    }

    public void syncWindUpdate(WindManager parManager) {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("packetCommand", "WeatherData");
        data.setString("command", "syncWindUpdate");
        data.setTag("data", parManager.nbtSyncForClient());
        Weather.eventChannel.sendToDimension(
            PacketHelper.getNBTPacket(data, Weather.eventChannelName),
            this.getWorld().provider.dimensionId);
        FMLInterModComms.sendRuntimeMessage(Weather.instance, Weather.modID, "weather.wind", data);
    }

    public void syncStormNew(StormObject parStorm) {
        this.syncStormNew(parStorm, (EntityPlayerMP) null);
    }

    public void syncStormNew(StormObject parStorm, EntityPlayerMP entP) {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("packetCommand", "WeatherData");
        data.setString("command", "syncStormNew");
        data.setTag("data", parStorm.nbtSyncForClient());
        if (entP == null) {
            Weather.eventChannel.sendToDimension(
                PacketHelper.getNBTPacket(data, Weather.eventChannelName),
                this.getWorld().provider.dimensionId);
        } else {
            Weather.eventChannel.sendTo(PacketHelper.getNBTPacket(data, Weather.eventChannelName), entP);
        }

    }

    private void syncStormUpdate(Set stormObjectsData) {
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger("stormCount", stormObjectsData.size());
        data.setString("packetCommand", "WeatherData");
        data.setString("command", "syncStormUpdate");
        int stormNumber = 0;

        for (Iterator var4 = stormObjectsData.iterator(); var4.hasNext(); ++stormNumber) {
            NBTTagCompound stormObjectData = (NBTTagCompound) var4.next();
            data.setTag("storm" + stormNumber, stormObjectData);
        }

        Weather.eventChannel.sendToDimension(
            PacketHelper.getNBTPacket(data, Weather.eventChannelName),
            this.getWorld().provider.dimensionId);
    }

    public void syncStormRemove(StormObject parStorm) {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("packetCommand", "WeatherData");
        data.setString("command", "syncStormRemove");
        data.setTag("data", parStorm.nbtSyncForClient());
        data.getCompoundTag("data")
            .setBoolean("isDead", true);
        Weather.eventChannel.sendToDimension(
            PacketHelper.getNBTPacket(data, Weather.eventChannelName),
            this.getWorld().provider.dimensionId);
    }

    public void syncVolcanoNew(VolcanoObject parStorm) {
        this.syncVolcanoNew(parStorm, (EntityPlayerMP) null);
    }

    public void syncVolcanoNew(VolcanoObject parStorm, EntityPlayerMP entP) {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("packetCommand", "WeatherData");
        data.setString("command", "syncVolcanoNew");
        data.setTag("data", parStorm.nbtSyncForClient());
        if (entP == null) {
            Weather.eventChannel.sendToDimension(
                PacketHelper.getNBTPacket(data, Weather.eventChannelName),
                this.getWorld().provider.dimensionId);
        } else {
            Weather.eventChannel.sendTo(PacketHelper.getNBTPacket(data, Weather.eventChannelName), entP);
        }

    }

    public void syncVolcanoUpdate(VolcanoObject parStorm) {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("packetCommand", "WeatherData");
        data.setString("command", "syncVolcanoUpdate");
        data.setTag("data", parStorm.nbtSyncForClient());
        Weather.eventChannel.sendToDimension(
            PacketHelper.getNBTPacket(data, Weather.eventChannelName),
            this.getWorld().provider.dimensionId);
    }

    public void syncVolcanoRemove(VolcanoObject parStorm) {}

    public void syncWeatherVanilla() {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("packetCommand", "WeatherData");
        data.setString("command", "syncWeatherUpdate");
        data.setBoolean("isVanillaRainActiveOnServer", this.isVanillaRainActiveOnServer);
        Weather.eventChannel.sendToDimension(
            PacketHelper.getNBTPacket(data, Weather.eventChannelName),
            this.getWorld().provider.dimensionId);
    }
}
