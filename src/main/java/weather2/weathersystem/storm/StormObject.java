package weather2.weathersystem.storm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import CoroUtil.util.ChunkCoordinatesBlock;
import CoroUtil.util.CoroUtilBlock;
import CoroUtil.util.CoroUtilEntity;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import extendedrenderer.ExtendedRenderer;
import extendedrenderer.particle.ParticleRegistry;
import extendedrenderer.particle.behavior.ParticleBehaviorFog;
import extendedrenderer.particle.entity.EntityRotFX;
import weather2.ServerTickHandler;
import weather2.Weather;
import weather2.client.entity.RenderCubeCloud;
import weather2.config.ConfigMisc;
import weather2.entity.EntityIceBall;
import weather2.entity.EntityLightningBolt;
import weather2.player.PlayerData;
import weather2.util.WeatherUtil;
import weather2.util.WeatherUtilConfig;
import weather2.util.WeatherUtilEntity;
import weather2.weathersystem.WeatherManagerBase;
import weather2.weathersystem.WeatherManagerServer;

public class StormObject {

    public static long lastUsedStormID = 0L;
    public long ID;
    public WeatherManagerBase manager;
    public String userSpawnedFor = "";
    @SideOnly(Side.CLIENT)
    public List listParticlesCloud;
    @SideOnly(Side.CLIENT)
    public List listParticlesGround;
    @SideOnly(Side.CLIENT)
    public List listParticlesFunnel;
    @SideOnly(Side.CLIENT)
    public ParticleBehaviorFog particleBehaviorFog;
    public int sizeMaxFunnelParticles = 600;
    public static int static_YPos_layer0 = ConfigMisc.Cloud_Layer0_Height;
    public static int static_YPos_layer1 = 350;
    public static int static_YPos_layer2 = 500;
    public static List layers = new ArrayList(
        Arrays.asList(
            new Integer[] { Integer.valueOf(static_YPos_layer0), Integer.valueOf(static_YPos_layer1),
                Integer.valueOf(static_YPos_layer2) }));
    public int layer = 0;
    public Vec3 pos;
    public Vec3 posGround;
    public Vec3 motion;
    public boolean angleIsOverridden;
    public float angleMovementTornadoOverride;
    public int size;
    public int maxSize;
    public boolean isGrowing;
    public int levelWater;
    public float levelWindMomentum;
    public float levelTemperature;
    public int levelWaterStartRaining;
    public int levelCurIntensityStage;
    public float levelCurStagesIntensity;
    public boolean hasStormPeaked;
    public int maxIntensityStage;
    public int stormType;
    public static int TYPE_LAND = 0;
    public static int TYPE_WATER = 1;
    public static int STATE_NORMAL = 0;
    public static int STATE_THUNDER = 1;
    public static int STATE_HIGHWIND = 2;
    public static int STATE_HAIL = 3;
    public static int STATE_FORMING = 4;
    public static int STATE_STAGE1 = 5;
    public static int STATE_STAGE2 = 6;
    public static int STATE_STAGE3 = 7;
    public static int STATE_STAGE4 = 8;
    public static int STATE_STAGE5 = 9;
    public static float levelStormIntensityFormingStartVal = (float) STATE_FORMING;
    public double spinSpeed;
    public boolean attrib_precipitation;
    public boolean attrib_waterSpout;
    public float scale;
    public float strength;
    public int maxHeight;
    public int currentTopYBlock;
    public TornadoHelper tornadoHelper;
    public Set doneChunks;
    public int updateLCG;
    public float formingStrength;
    public Vec3 posBaseFormationPos;
    public boolean naturallySpawned;
    public boolean canSnowFromCloudTemperature;
    public boolean alwaysProgresses;
    public boolean isDead;
    @SideOnly(Side.CLIENT)
    public RenderCubeCloud renderBlock;
    public long ticksSinceLastPacketReceived;
    private NBTTagCompound cachedClientNBTState;

    public StormObject(WeatherManagerBase parManager) {
        this.pos = Vec3.createVectorHelper(0.0D, (double) static_YPos_layer0, 0.0D);
        this.posGround = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
        this.motion = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
        this.angleIsOverridden = false;
        this.angleMovementTornadoOverride = 0.0F;
        this.size = 50;
        this.maxSize = ConfigMisc.Storm_MaxRadius;
        this.isGrowing = true;
        this.levelWater = 0;
        this.levelWindMomentum = 0.0F;
        this.levelTemperature = 0.0F;
        this.levelWaterStartRaining = 100;
        this.levelCurIntensityStage = 0;
        this.levelCurStagesIntensity = 0.0F;
        this.hasStormPeaked = false;
        this.maxIntensityStage = STATE_STAGE5;
        this.stormType = TYPE_LAND;
        this.spinSpeed = 0.02D;
        this.attrib_precipitation = false;
        this.attrib_waterSpout = false;
        this.scale = 1.0F;
        this.strength = 100.0F;
        this.maxHeight = 60;
        this.currentTopYBlock = -1;
        this.tornadoHelper = new TornadoHelper(this);
        this.doneChunks = new HashSet();
        this.updateLCG = (new Random()).nextInt();
        this.formingStrength = 0.0F;
        this.posBaseFormationPos = Vec3.createVectorHelper(this.pos.xCoord, this.pos.yCoord, this.pos.zCoord);
        this.naturallySpawned = true;
        this.canSnowFromCloudTemperature = false;
        this.alwaysProgresses = false;
        this.isDead = false;
        this.ticksSinceLastPacketReceived = 0L;
        this.manager = parManager;
        if (parManager.getWorld().isRemote) {
            this.listParticlesCloud = new ArrayList();
            this.listParticlesFunnel = new ArrayList();
            this.listParticlesGround = new ArrayList();
            this.renderBlock = new RenderCubeCloud();
        }

    }

    public void initFirstTime() {
        this.ID = (long) (lastUsedStormID++);
        BiomeGenBase bgb = this.manager.getWorld()
            .getBiomeGenForCoords(MathHelper.floor_double(this.pos.xCoord), MathHelper.floor_double(this.pos.zCoord));
        float temp = 1.0F;
        if (bgb != null) {
            temp = bgb.getFloatTemperature(
                MathHelper.floor_double(this.pos.xCoord),
                MathHelper.floor_double(this.pos.yCoord),
                MathHelper.floor_double(this.pos.zCoord));
        }

        if (this.naturallySpawned) {
            this.levelTemperature = this.getTemperatureMCToWeatherSys(temp);
        }

        this.levelWindMomentum = 0.0F;
    }

    public boolean isPrecipitating() {
        return this.attrib_precipitation;
    }

    public void setPrecipitating(boolean parVal) {
        this.attrib_precipitation = parVal;
    }

    public boolean isRealStorm() {
        return this.levelCurIntensityStage > STATE_NORMAL;
    }

    public boolean isTornadoFormingOrGreater() {
        return this.stormType == TYPE_LAND && this.levelCurIntensityStage >= STATE_FORMING;
    }

    public boolean isCycloneFormingOrGreater() {
        return this.stormType == TYPE_WATER && this.levelCurIntensityStage >= STATE_FORMING;
    }

    public boolean isSpinning() {
        return this.levelCurIntensityStage >= STATE_HIGHWIND;
    }

    public boolean isTropicalCyclone() {
        return this.levelCurIntensityStage >= STATE_STAGE1;
    }

    public boolean isHurricane() {
        return this.levelCurIntensityStage >= STATE_STAGE5;
    }

    public void readFromNBT(NBTTagCompound var1) {
        this.nbtSyncFromServer(var1);
        this.motion = Vec3.createVectorHelper(var1.getDouble("vecX"), var1.getDouble("vecY"), var1.getDouble("vecZ"));
        this.angleIsOverridden = var1.getBoolean("angleIsOverridden");
        this.angleMovementTornadoOverride = var1.getFloat("angleMovementTornadoOverride");
    }

    public NBTTagCompound writeToNBT() {
        this.nbtSyncForClient();
        NBTTagCompound nbt = this.cachedClientNBTState;
        nbt.setDouble("vecX", this.motion.xCoord);
        nbt.setDouble("vecY", this.motion.yCoord);
        nbt.setDouble("vecZ", this.motion.zCoord);
        nbt.setBoolean("angleIsOverridden", this.angleIsOverridden);
        nbt.setFloat("angleMovementTornadoOverride", this.angleMovementTornadoOverride);
        return nbt;
    }

    public void nbtSyncFromServer(NBTTagCompound parNBT) {
        StormObject.CachedNBTTagCompound newData = new StormObject.CachedNBTTagCompound(parNBT);
        newData.setCachedNBT(this.cachedClientNBTState);
        this.ID = newData.getLong("ID");
        this.pos = Vec3.createVectorHelper(
            (double) newData.getInteger("posX"),
            (double) newData.getInteger("posY"),
            (double) newData.getInteger("posZ"));
        this.size = newData.getInteger("size");
        this.maxSize = newData.getInteger("maxSize");
        this.attrib_precipitation = newData.getBoolean("attrib_rain");
        this.attrib_waterSpout = newData.getBoolean("attrib_waterSpout");
        this.currentTopYBlock = newData.getInteger("currentTopYBlock");
        this.levelTemperature = newData.getFloat("levelTemperature");
        this.levelWater = newData.getInteger("levelWater");
        this.layer = newData.getInteger("layer");
        this.levelCurIntensityStage = newData.getInteger("levelCurIntensityStage");
        this.levelCurStagesIntensity = newData.getFloat("levelCurStagesIntensity");
        this.stormType = newData.getInteger("stormType");
        this.hasStormPeaked = newData.getBoolean("hasStormPeaked");
        this.isDead = newData.getBoolean("isDead");
        this.cachedClientNBTState = newData.getNewNBT();
        this.ticksSinceLastPacketReceived = 0L;
    }

    public NBTTagCompound nbtSyncForClient() {
        StormObject.CachedNBTTagCompound data = new StormObject.CachedNBTTagCompound();
        data.setCachedNBT(this.cachedClientNBTState);
        data.setUpdateForced(true);
        data.setLong("ID", this.ID);
        data.setUpdateForced(false);
        data.setInteger("posX", (int) this.pos.xCoord);
        data.setInteger("posY", (int) this.pos.yCoord);
        data.setInteger("posZ", (int) this.pos.zCoord);
        data.setInteger("size", this.size);
        data.setInteger("maxSize", this.maxSize);
        data.setBoolean("attrib_rain", this.attrib_precipitation);
        data.setBoolean("attrib_waterSpout", this.attrib_waterSpout);
        data.setInteger("currentTopYBlock", this.currentTopYBlock);
        data.setFloat("levelTemperature", this.levelTemperature);
        data.setInteger("levelWater", this.levelWater);
        data.setInteger("layer", this.layer);
        data.setInteger("levelCurIntensityStage", this.levelCurIntensityStage);
        data.setFloat("levelCurStagesIntensity", this.levelCurStagesIntensity);
        data.setInteger("stormType", this.stormType);
        data.setBoolean("hasStormPeaked", this.hasStormPeaked);
        data.setBoolean("isDead", this.isDead);
        this.cachedClientNBTState = data.getCachedNBT();
        return data.getNewNBT();
    }

    public NBTTagCompound nbtForIMC() {
        return this.nbtSyncForClient();
    }

    @SideOnly(Side.CLIENT)
    public void tickRender(float partialTick) {
        if (this.layer == 1) {
            this.renderBlock.doRenderClouds(this, this.pos.xCoord, this.pos.yCoord, this.pos.zCoord, 0.0F, partialTick);
        }

    }

    public void tick() {
        this.posGround = Vec3.createVectorHelper(this.pos.xCoord, this.pos.yCoord, this.pos.zCoord);
        this.posGround.yCoord = (double) this.currentTopYBlock;
        Side side = FMLCommonHandler.instance()
            .getEffectiveSide();
        if (side == Side.CLIENT) {
            if (!WeatherUtil.isPaused()) {
                ++this.ticksSinceLastPacketReceived;
                if (this.layer == 0) {
                    this.tickClient();
                }

                if (this.isTornadoFormingOrGreater() || this.isCycloneFormingOrGreater()) {
                    this.tornadoHelper.tick(this.manager.getWorld());
                }

                if (this.levelCurIntensityStage >= STATE_HIGHWIND && this.manager.getWorld().isRemote) {
                    this.tornadoHelper
                        .soundUpdates(true, this.isTornadoFormingOrGreater() || this.isCycloneFormingOrGreater());
                }
            }
        } else {
            if (this.isTornadoFormingOrGreater() || this.isCycloneFormingOrGreater()) {
                this.tornadoHelper.tick(this.manager.getWorld());
            }

            if (this.levelCurIntensityStage >= STATE_HIGHWIND && this.manager.getWorld().isRemote) {
                this.tornadoHelper
                    .soundUpdates(true, this.isTornadoFormingOrGreater() || this.isCycloneFormingOrGreater());
            }

            this.tickMovement();
            if (this.layer == 0) {
                this.tickWeatherEvents();
                this.tickProgression();
                this.tickSnowFall();
            }
        }

        if (this.layer == 0) {
            this.posBaseFormationPos = Vec3.createVectorHelper(this.pos.xCoord, this.pos.yCoord, this.pos.zCoord);
            if ((float) this.levelCurIntensityStage >= levelStormIntensityFormingStartVal) {
                if ((float) this.levelCurIntensityStage >= levelStormIntensityFormingStartVal + 1.0F) {
                    this.formingStrength = 1.0F;
                    this.posBaseFormationPos.yCoord = this.posGround.yCoord;
                } else {
                    float intensityAdj = Math.min(1.0F, this.levelCurStagesIntensity * 2.0F);
                    float val = (float) this.levelCurIntensityStage + intensityAdj - levelStormIntensityFormingStartVal;
                    this.formingStrength = val;
                    double yDiff = this.pos.yCoord - this.posGround.yCoord;
                    this.posBaseFormationPos.yCoord = this.pos.yCoord - yDiff * (double) this.formingStrength;
                }
            } else if (this.levelCurIntensityStage == STATE_HIGHWIND) {
                this.formingStrength = 1.0F;
                this.posBaseFormationPos.yCoord = this.posGround.yCoord;
            } else {
                this.formingStrength = 0.0F;
                this.posBaseFormationPos.yCoord = this.pos.yCoord;
            }
        }

    }

    public void tickMovement() {
        float angle = this.getAdjustedAngle();
        if (this.angleIsOverridden) {
            angle = this.angleMovementTornadoOverride;
        }

        double vecX = -Math.sin(Math.toRadians((double) angle));
        double vecZ = Math.cos(Math.toRadians((double) angle));
        float cloudSpeedAmp = 0.2F;
        float finalSpeed = this.getAdjustedSpeed() * cloudSpeedAmp;
        if (this.levelCurIntensityStage >= STATE_FORMING) {
            finalSpeed = 0.2F;
        } else if (this.levelCurIntensityStage >= STATE_THUNDER) {
            finalSpeed = 0.05F;
        }

        if ((float) this.levelCurIntensityStage >= levelStormIntensityFormingStartVal) {
            finalSpeed /= (float) this.levelCurIntensityStage - levelStormIntensityFormingStartVal + 1.0F;
        }

        if (finalSpeed < 0.03F) {
            finalSpeed = 0.03F;
        }

        if (finalSpeed > 0.3F) {
            finalSpeed = 0.3F;
        }

        if (this.manager.getWorld()
            .getTotalWorldTime() % 100L == 0L && this.levelCurIntensityStage >= STATE_FORMING) {
            ;
        }

        this.motion.xCoord = vecX * (double) finalSpeed;
        this.motion.zCoord = vecZ * (double) finalSpeed;
        double max = 0.2D;
        this.pos.xCoord += this.motion.xCoord;
        this.pos.zCoord += this.motion.zCoord;
    }

    public void tickWeatherEvents() {
        Random rand = new Random();
        World world = this.manager.getWorld();
        if (this.size == 0) {
            this.size = 1;
        }

        if (this.maxSize == 0) {
            this.maxSize = 1;
        }

        this.currentTopYBlock = world
            .getHeightValue(MathHelper.floor_double(this.pos.xCoord), MathHelper.floor_double(this.pos.zCoord));
        int i;
        int x;
        int z;
        int baseLightningOdds = Math
            .max(1, ConfigMisc.Storm_LightningStrikeBaseValueOddsTo1 - this.levelCurIntensityStage * 10);
        int lightningOdds = weather2.compat.WeatherNTNHHooks.getAdjustedLightningOdds(baseLightningOdds, this);
        if (this.levelCurIntensityStage >= STATE_THUNDER && rand.nextInt(lightningOdds) == 0) {
            i = (int) (this.pos.xCoord + (double) rand.nextInt(this.size) - (double) rand.nextInt(this.size));
            x = (int) (this.pos.zCoord + (double) rand.nextInt(this.size) - (double) rand.nextInt(this.size));
            z = world.getPrecipitationHeight(i, x);
            if (world.checkChunksExist(i, z, x, i, z, x)) {
                this.addWeatherEffectLightning(new EntityLightningBolt(world, (double) i, (double) z, (double) x));
            }
        }

        if (this.isPrecipitating() && this.levelCurIntensityStage == STATE_HAIL && this.stormType == TYPE_LAND) {
            for (i = 0; i < Math.max(1, ConfigMisc.Storm_HailPerTick * (this.size / this.maxSize)); ++i) {
                x = (int) (this.pos.xCoord + (double) rand.nextInt(this.size) - (double) rand.nextInt(this.size));
                z = (int) (this.pos.zCoord + (double) rand.nextInt(this.size) - (double) rand.nextInt(this.size));
                if (world.checkChunksExist(x, static_YPos_layer0, z, x, static_YPos_layer0, z)
                    && world.getClosestPlayer((double) x, 50.0D, (double) z, 80.0D) != null) {
                    EntityIceBall hail = new EntityIceBall(world);
                    hail.setPosition((double) x, (double) ((Integer) layers.get(this.layer)).intValue(), (double) z);
                    world.spawnEntityInWorld(hail);
                }
            }
        }

    }

    public void tickSnowFall() {
        if (ConfigMisc.Snow_PerformSnowfall) {
            if (this.isPrecipitating()) {
                World world = this.manager.getWorld();
                long startTime = System.nanoTime();
                boolean xx = false;
                boolean zz = false;
                if (this.size == 0) {
                    this.size = 1;
                }

                for (int var27 = (int) (this.pos.xCoord - (double) (this.size / 2)); (double) var27
                    < this.pos.xCoord + (double) (this.size / 2); var27 += 16) {
                    for (int var28 = (int) (this.pos.zCoord - (double) (this.size / 2)); (double) var28
                        < this.pos.zCoord + (double) (this.size / 2); var28 += 16) {
                        int chunkX = var27 / 16;
                        int chunkZ = var28 / 16;
                        int x = chunkX * 16;
                        int z = chunkZ * 16;
                        if (world.blockExists(var27, 0, var28)) {
                            Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
                            if (world.provider.canDoRainSnowIce(chunk) && (ConfigMisc.Snow_RarityOfBuildup == 0
                                || world.rand.nextInt(ConfigMisc.Snow_RarityOfBuildup) == 0)) {
                                this.updateLCG = this.updateLCG * 3 + 1013904223;
                                int i1 = this.updateLCG >> 2;
                                int xxx = i1 & 15;
                                int zzz = i1 >> 8 & 15;
                                double d0 = this.pos.xCoord - (double) (var27 + xxx);
                                double d2 = this.pos.zCoord - (double) (var28 + zzz);
                                if ((double) MathHelper.sqrt_double(d0 * d0 + d2 * d2) <= (double) this.size) {
                                    byte snowMetaMax = 7;
                                    int setBlockHeight = world.getPrecipitationHeight(xxx + x, zzz + z);
                                    if (this.canSnowAtBody(xxx + x, setBlockHeight, zzz + z)
                                        && Blocks.snow.canPlaceBlockAt(world, xxx + x, setBlockHeight, zzz + z)) {
                                        boolean perform = false;
                                        Block id = world.getBlock(xxx + x, setBlockHeight, zzz + z);
                                        int meta = 0;
                                        int origMeta;
                                        if (id.getMaterial() == Material.snow) {
                                            if (ConfigMisc.Snow_ExtraPileUp) {
                                                meta = world.getBlockMetadata(xxx + x, setBlockHeight, zzz + z);
                                                if (meta < snowMetaMax) {
                                                    perform = true;
                                                    ++meta;
                                                } else if (ConfigMisc.Snow_MaxBlockBuildupHeight > 1) {
                                                    int coords = setBlockHeight;

                                                    for (origMeta = 0; origMeta
                                                        < ConfigMisc.Snow_MaxBlockBuildupHeight; ++origMeta) {
                                                        Block checkID = world
                                                            .getBlock(xxx + x, coords + origMeta, zzz + z);
                                                        if (checkID.getMaterial() == Material.snow) {
                                                            meta = world
                                                                .getBlockMetadata(xxx + x, coords + origMeta, zzz + z);
                                                            if (meta < snowMetaMax) {
                                                                setBlockHeight = coords + origMeta;
                                                                perform = true;
                                                                ++meta;
                                                                break;
                                                            }
                                                        } else if (CoroUtilBlock.isAir(checkID)) {
                                                            meta = 0;
                                                            setBlockHeight = coords + origMeta;
                                                            perform = true;
                                                            break;
                                                        }
                                                    }

                                                    if (origMeta == ConfigMisc.Snow_MaxBlockBuildupHeight) {
                                                        perform = false;
                                                    }
                                                }
                                            }
                                        } else {
                                            perform = true;
                                        }

                                        if (perform && ConfigMisc.Snow_SmoothOutPlacement) {
                                            origMeta = Math.max(0, meta - 1);
                                            if (origMeta > snowMetaMax - 4) {
                                                ChunkCoordinatesBlock var29 = this.getSnowfallEvenOutAdjustCheck(
                                                    xxx + x,
                                                    setBlockHeight,
                                                    zzz + z,
                                                    origMeta);
                                                if (var29.posX != 0 || var29.posZ != 0) {
                                                    if (meta != var29.meta + 1) {
                                                        xxx = var29.posX;
                                                        zzz = var29.posZ;
                                                        meta = var29.meta + 1;
                                                    } else {
                                                        perform = false;
                                                    }
                                                }
                                            }
                                        }

                                        if (perform) {
                                            world
                                                .setBlock(xxx + x, setBlockHeight, zzz + z, Blocks.snow_layer, meta, 3);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    public ChunkCoordinatesBlock getSnowfallEvenOutAdjustCheck(int x, int y, int z, int sourceMeta) {
        ChunkCoordinatesBlock attempt = this.getSnowfallEvenOutAdjust(x - 1, y, z, sourceMeta);
        if (attempt.posX == 0 && attempt.posZ == 0) {
            attempt = this.getSnowfallEvenOutAdjust(x + 1, y, z, sourceMeta);
            if (attempt.posX == 0 && attempt.posZ == 0) {
                attempt = this.getSnowfallEvenOutAdjust(x, y, z - 1, sourceMeta);
                if (attempt.posX == 0 && attempt.posZ == 0) {
                    attempt = this.getSnowfallEvenOutAdjust(x, y, z + 1, sourceMeta);
                    return attempt.posX == 0 && attempt.posZ == 0 ? new ChunkCoordinatesBlock(0, 0, 0, Blocks.air, 0)
                        : attempt;
                } else {
                    return attempt;
                }
            } else {
                return attempt;
            }
        } else {
            return attempt;
        }
    }

    public ChunkCoordinatesBlock getSnowfallEvenOutAdjust(int x, int y, int z, int sourceMeta) {
        boolean metaToSet = false;
        World world = this.manager.getWorld();
        Block checkID = world.getBlock(x, y, z);
        if (CoroUtilBlock.isAir(checkID)) {
            Block checkMeta1 = world.getBlock(x, y - 1, z);
            return CoroUtilBlock.isAir(checkMeta1) ? new ChunkCoordinatesBlock(0, 0, 0, Blocks.air, 0)
                : new ChunkCoordinatesBlock(x, y, z, Blocks.air, 0);
        } else if (checkID == Blocks.snow) {
            int checkMeta = world.getBlockMetadata(x, y, z);
            return checkMeta < sourceMeta ? new ChunkCoordinatesBlock(x, y, z, checkID, checkMeta)
                : new ChunkCoordinatesBlock(0, 0, 0, Blocks.air, 0);
        } else {
            return new ChunkCoordinatesBlock(0, 0, 0, Blocks.air, 0);
        }
    }

    public boolean canSnowAtBody(int par1, int par2, int par3) {
        World world = this.manager.getWorld();
        BiomeGenBase biomegenbase = world.getBiomeGenForCoords(par1, par3);
        if (biomegenbase == null) {
            return false;
        } else {
            biomegenbase.getFloatTemperature(par1, par2, par3);
            if ((!this.canSnowFromCloudTemperature || this.levelTemperature <= 0.0F)
                && (this.canSnowFromCloudTemperature || biomegenbase.getFloatTemperature(par1, par2, par3) <= 0.15F)) {
                if (par2 >= 0 && par2 < 256 && world.getSavedLightValue(EnumSkyBlock.Block, par1, par2, par3) < 10) {
                    Block block = world.getBlock(par1, par2, par3);
                    if ((block.isAir(world, par1, par2, par3) || block == Blocks.snow_layer)
                        && Blocks.snow_layer.canPlaceBlockAt(world, par1, par2, par3)) {
                        return true;
                    }
                }

                return false;
            } else {
                return false;
            }
        }
    }

    public void tickProgression() {
        World world = this.manager.getWorld();
        if (world.getTotalWorldTime() % 3L == 0L && this.isGrowing && this.size < this.maxSize) {
            ++this.size;
        }

        float tempAdjustRate = (float) ConfigMisc.Storm_TemperatureAdjustRate;
        int levelWaterBuildRate = ConfigMisc.Storm_Rain_WaterBuildUpRate;
        int levelWaterSpendRate = ConfigMisc.Storm_Rain_WaterSpendRate;
        int randomChanceOfWaterBuildFromWater = ConfigMisc.Storm_Rain_WaterBuildUpOddsTo1FromSource;
        int randomChanceOfWaterBuildFromNothing = ConfigMisc.Storm_Rain_WaterBuildUpOddsTo1FromNothing;
        boolean isInOcean = false;
        boolean isOverWater = false;
        if (world.getTotalWorldTime() % (long) ConfigMisc.Storm_AllTypes_TickRateDelay == 0L) {
            NBTTagCompound playerNBT = PlayerData.getPlayerNBT(this.userSpawnedFor);
            long lastStormDeadlyTime = playerNBT.getLong("lastStormDeadlyTime");
            BiomeGenBase bgb = world.getBiomeGenForCoords(
                MathHelper.floor_double(this.pos.xCoord),
                MathHelper.floor_double(this.pos.zCoord));
            if (bgb != null) {
                isInOcean = bgb.biomeName.contains("Ocean") || bgb.biomeName.contains("ocean");
                float performBuildup = this.getTemperatureMCToWeatherSys(
                    bgb.getFloatTemperature(
                        MathHelper.floor_double(this.pos.xCoord),
                        MathHelper.floor_double(this.pos.yCoord),
                        MathHelper.floor_double(this.pos.zCoord)));
                if (this.levelTemperature > performBuildup) {
                    this.levelTemperature -= tempAdjustRate;
                } else {
                    this.levelTemperature += tempAdjustRate;
                }
            }

            boolean var24 = false;
            Random rand = new Random();
            if (!this.isPrecipitating() && rand.nextInt(randomChanceOfWaterBuildFromNothing) == 0) {
                var24 = true;
            }

            Block blockID = world.getBlock(
                MathHelper.floor_double(this.pos.xCoord),
                this.currentTopYBlock - 1,
                MathHelper.floor_double(this.pos.zCoord));
            if (!CoroUtilBlock.isAir(blockID) && blockID.getMaterial() instanceof MaterialLiquid) {
                isOverWater = true;
            }

            if (!var24 && !this.isPrecipitating() && rand.nextInt(randomChanceOfWaterBuildFromWater) == 0) {
                if (isOverWater) {
                    var24 = true;
                }

                if (!var24 && bgb != null
                    && (isInOcean || bgb.biomeName.contains("Swamp")
                        || bgb.biomeName.contains("Jungle")
                        || bgb.biomeName.contains("River"))) {
                    var24 = true;
                }
            }

            if (var24) {
                this.levelWater += levelWaterBuildRate;
            }

            if (this.isPrecipitating()) {
                this.levelWater -= levelWaterSpendRate;
                if (this.levelWater < 0) {
                    this.levelWater = 0;
                }

                if (this.levelWater <= 0) {
                    this.setPrecipitating(false);
                    Weather.dbg("ending raining for: " + this.ID);
                }
            } else if ((!ConfigMisc.overcastMode || this.manager.getWorld()
                .isRaining()) && this.levelWater >= this.levelWaterStartRaining
                && ConfigMisc.Player_Storm_Rain_OddsTo1 != -1
                && rand.nextInt(ConfigMisc.Player_Storm_Rain_OddsTo1) == 0) {
                    this.setPrecipitating(true);
                    Weather.dbg("starting raining for: " + this.ID);
                }

            WeatherManagerServer wm = (WeatherManagerServer) ServerTickHandler.lookupDimToWeatherMan
                .get(Integer.valueOf(world.provider.dimensionId));
            boolean tryFormStorm = false;
            // NTNH start: dynamic deadly cooldown (Eve 800 ticks vs base 1800)
            int deadlyTimeBetween = weather2.compat.WeatherNTNHHooks
                .getDeadlyTimeBetween(ConfigMisc.Player_Storm_Deadly_TimeBetweenInTicks, this);
            if (ConfigMisc.Server_Storm_Deadly_UseGlobalRate) {
                if (ConfigMisc.Server_Storm_Deadly_TimeBetweenInTicks != -1 && (wm.lastStormFormed == 0L
                    || wm.lastStormFormed + (long) deadlyTimeBetween < world.getTotalWorldTime())) {
                    tryFormStorm = true;
                }
            } else if (ConfigMisc.Player_Storm_Deadly_TimeBetweenInTicks != -1 && (lastStormDeadlyTime == 0L
                || lastStormDeadlyTime + (long) deadlyTimeBetween < world.getTotalWorldTime())) {
                    tryFormStorm = true;
                }
            // NTNH end

            int oddsTo1OfIntensityProgressionBase;
            if ((ConfigMisc.overcastMode && this.manager.getWorld()
                .isRaining() || !ConfigMisc.overcastMode)
                && WeatherUtilConfig.listDimensionsStorms
                    .contains(Integer.valueOf(this.manager.getWorld().provider.dimensionId))
                && tryFormStorm) {
                int levelStormIntensityRate = ConfigMisc.Storm_Deadly_CollideDistance;
                int minIntensityToProgress = ConfigMisc.Player_Storm_Deadly_OddsTo1;
                EntityPlayer var27;
                // NTNH start: dynamic planetary spawn odds (Eve accelerated / Duna / Tekto)
                int oceanOdds = weather2.compat.WeatherNTNHHooks
                    .getOceanStormOdds(ConfigMisc.Storm_OddsTo1OfOceanBasedStorm, this);
                int landOdds = weather2.compat.WeatherNTNHHooks
                    .getLandStormOdds(ConfigMisc.Storm_OddsTo1OfLandBasedStorm, this);
                if (isInOcean && oceanOdds > 0 && rand.nextInt(oceanOdds) == 0) {
                    var27 = world.getPlayerEntityByName(this.userSpawnedFor);
                    if (var27 != null) {
                        this.initRealStorm(var27, (StormObject) null);
                    } else {
                        this.initRealStorm((EntityPlayer) null, (StormObject) null);
                    }

                    if (ConfigMisc.Server_Storm_Deadly_UseGlobalRate) {
                        wm.lastStormFormed = world.getTotalWorldTime();
                    } else {
                        playerNBT.setLong("lastStormDeadlyTime", world.getTotalWorldTime());
                    }
                } else if (!isInOcean && landOdds > 0 && rand.nextInt(landOdds) == 0) {
                    var27 = world.getPlayerEntityByName(this.userSpawnedFor);
                    if (var27 != null) {
                        this.initRealStorm(var27, (StormObject) null);
                    } else {
                        this.initRealStorm((EntityPlayer) null, (StormObject) null);
                    }

                    if (ConfigMisc.Server_Storm_Deadly_UseGlobalRate) {
                        wm.lastStormFormed = world.getTotalWorldTime();
                    } else {
                        playerNBT.setLong("lastStormDeadlyTime", world.getTotalWorldTime());
                    }
                }
                // NTNH end
                else if (rand.nextInt(minIntensityToProgress) == 0) {
                    for (oddsTo1OfIntensityProgressionBase = 0; oddsTo1OfIntensityProgressionBase
                        < this.manager.getStormObjects()
                            .size(); ++oddsTo1OfIntensityProgressionBase) {
                        StormObject oddsTo1OfIntensityProgression = (StormObject) this.manager.getStormObjects()
                            .get(oddsTo1OfIntensityProgressionBase);
                        boolean startStorm = false;
                        if (oddsTo1OfIntensityProgression.ID != this.ID
                            && oddsTo1OfIntensityProgression.levelCurIntensityStage <= 0
                            && oddsTo1OfIntensityProgression.pos.distanceTo(this.pos)
                                < (double) levelStormIntensityRate) {
                            if (this.levelTemperature < 0.0F) {
                                if (oddsTo1OfIntensityProgression.levelTemperature > 0.0F) {
                                    startStorm = true;
                                }
                            } else if (this.levelTemperature > 0.0F
                                && oddsTo1OfIntensityProgression.levelTemperature < 0.0F) {
                                    startStorm = true;
                                }
                        }

                        if (startStorm) {
                            playerNBT.setLong("lastStormDeadlyTime", world.getTotalWorldTime());
                            EntityPlayer entP = world.getPlayerEntityByName(this.userSpawnedFor);
                            if (entP != null) {
                                this.initRealStorm(entP, oddsTo1OfIntensityProgression);
                            } else {
                                this.initRealStorm((EntityPlayer) null, oddsTo1OfIntensityProgression);
                            }
                            break;
                        }
                    }
                }
            }

            if (this.isRealStorm()) {
                if (ConfigMisc.overcastMode && !this.manager.getWorld()
                    .isRaining()) {
                    this.hasStormPeaked = true;
                }

                if (!this.hasStormPeaked) {
                    this.levelWater = this.levelWaterStartRaining;
                    this.setPrecipitating(true);
                }

                if ((this.levelCurIntensityStage == STATE_HIGHWIND || this.levelCurIntensityStage == STATE_HAIL)
                    && isOverWater) {
                    if (ConfigMisc.Storm_OddsTo1OfHighWindWaterSpout != 0
                        && rand.nextInt(ConfigMisc.Storm_OddsTo1OfHighWindWaterSpout) == 0) {
                        this.attrib_waterSpout = true;
                    }
                } else {
                    this.attrib_waterSpout = false;
                }

                float var25 = 0.02F;
                float var26 = 0.6F;
                oddsTo1OfIntensityProgressionBase = ConfigMisc.Storm_OddsTo1OfProgressionBase;
                if ((float) this.levelCurIntensityStage >= levelStormIntensityFormingStartVal) {
                    var25 *= 3.0F;
                    oddsTo1OfIntensityProgressionBase /= 3;
                }

                int var28 = oddsTo1OfIntensityProgressionBase
                    + this.levelCurIntensityStage * ConfigMisc.Storm_OddsTo1OfProgressionStageMultiplier;
                if (!this.hasStormPeaked) {
                    this.levelCurStagesIntensity += var25;
                    if (this.levelCurIntensityStage < this.maxIntensityStage
                        && (!ConfigMisc.Storm_NoTornadosOrCyclones || this.levelCurIntensityStage < STATE_FORMING - 1)
                        && this.levelCurStagesIntensity >= var26
                        && (this.alwaysProgresses || rand.nextInt(var28) == 0)) {
                        this.stageNext();
                        Weather.dbg("storm ID: " + this.ID + " - growing, stage: " + this.levelCurIntensityStage);
                        if (isInOcean && this.levelCurIntensityStage == STATE_FORMING) {
                            Weather.dbg("storm ID: " + this.ID + " marked as tropical cyclone!");
                            this.stormType = TYPE_WATER;
                        }
                    }

                    if (this.levelCurStagesIntensity >= 1.0F) {
                        Weather.dbg("storm peaked at: " + this.levelCurIntensityStage);
                        this.hasStormPeaked = true;
                    }
                } else {
                    if (ConfigMisc.overcastMode && this.manager.getWorld()
                        .isRaining()) {
                        this.levelCurStagesIntensity -= var25 * 0.9F;
                    } else {
                        this.levelCurStagesIntensity -= var25 * 0.3F;
                    }

                    if (this.levelCurStagesIntensity <= 0.0F) {
                        this.stagePrev();
                        Weather.dbg("storm ID: " + this.ID + " - dying, stage: " + this.levelCurIntensityStage);
                        if (this.levelCurIntensityStage <= 0) {
                            this.setNoStorm();
                        }
                    }
                }
            } else if (ConfigMisc.overcastMode && !this.manager.getWorld()
                .isRaining() && this.attrib_precipitation) {
                    this.setPrecipitating(false);
                }
        }

    }

    public WeatherEntityConfig getWeatherEntityConfigForStorm() {
        WeatherEntityConfig weatherConfig = (WeatherEntityConfig) WeatherTypes.weatherEntTypes.get(0);
        if (this.levelCurIntensityStage >= STATE_STAGE5) {
            weatherConfig = (WeatherEntityConfig) WeatherTypes.weatherEntTypes.get(5);
        } else if (this.levelCurIntensityStage >= STATE_STAGE4) {
            weatherConfig = (WeatherEntityConfig) WeatherTypes.weatherEntTypes.get(4);
        } else if (this.levelCurIntensityStage >= STATE_STAGE3) {
            weatherConfig = (WeatherEntityConfig) WeatherTypes.weatherEntTypes.get(3);
        } else if (this.levelCurIntensityStage >= STATE_STAGE2) {
            weatherConfig = (WeatherEntityConfig) WeatherTypes.weatherEntTypes.get(2);
        } else if (this.levelCurIntensityStage >= STATE_STAGE1) {
            weatherConfig = (WeatherEntityConfig) WeatherTypes.weatherEntTypes.get(1);
        } else if (this.levelCurIntensityStage >= STATE_FORMING) {
            weatherConfig = (WeatherEntityConfig) WeatherTypes.weatherEntTypes.get(0);
        }

        return weatherConfig;
    }

    public void stageNext() {
        ++this.levelCurIntensityStage;
        this.levelCurStagesIntensity = 0.0F;
        if (ConfigMisc.Storm_Tornado_aimAtPlayerOnSpawn && !this.hasStormPeaked
            && this.levelCurIntensityStage == STATE_FORMING) {
            this.aimStormAtClosestOrProvidedPlayer((EntityPlayer) null);
        }

    }

    public void stagePrev() {
        --this.levelCurIntensityStage;
        this.levelCurStagesIntensity = 1.0F;
    }

    public void initRealStorm(EntityPlayer entP, StormObject stormToAbsorb) {
        this.levelCurIntensityStage = STATE_THUNDER;
        float diff = 4.0F;
        if (stormToAbsorb != null) {
            float var10000 = this.levelTemperature - stormToAbsorb.levelTemperature;
        }

        if (this.naturallySpawned) {
            this.levelWater = this.levelWaterStartRaining * 2;
        }

        this.attrib_precipitation = true;
        if (stormToAbsorb != null) {
            Weather.dbg("stormfront collision happened between ID " + this.ID + " and " + stormToAbsorb.ID);
            this.manager.removeStormObject(stormToAbsorb.ID);
            ((WeatherManagerServer) this.manager).syncStormRemove(stormToAbsorb);
        } else {
            Weather.dbg("ocean storm happened, ID " + this.ID);
        }

        if (ConfigMisc.Storm_Tornado_aimAtPlayerOnSpawn && entP != null) {
            this.aimStormAtClosestOrProvidedPlayer(entP);
        }

        // NTNH start: apply planetary climate profile (Eve supercells / Tekto limits)
        weather2.compat.WeatherNTNHHooks.onInitRealStorm(this);
        // NTNH end

    }

    public void aimStormAtClosestOrProvidedPlayer(EntityPlayer entP) {
        if (entP == null) {
            entP = this.manager.getWorld()
                .getClosestPlayer(this.pos.xCoord, this.pos.yCoord, this.pos.zCoord, -1.0D);
        }

        if (entP != null) {
            Random rand = new Random();
            double var11 = entP.posX - this.pos.xCoord;
            double var15 = entP.posZ - this.pos.zCoord;
            float yaw = -((float) (Math.atan2(var11, var15) * 180.0D / 3.141592653589793D));
            int size = ConfigMisc.Storm_Tornado_aimAtPlayerAngleVariance;
            if (size > 0) {
                yaw += (float) (rand.nextInt(size) - size / 2);
            }

            this.angleIsOverridden = true;
            this.angleMovementTornadoOverride = yaw;
            Weather.dbg("stormfront aimed at player " + CoroUtilEntity.getName(entP));
        }

    }

    public void setNoStorm() {
        Weather.dbg("storm ID: " + this.ID + " - ended storm event");
        this.levelCurIntensityStage = STATE_NORMAL;
        this.levelCurStagesIntensity = 0.0F;
    }

    @SideOnly(Side.CLIENT)
    public void tickClient() {
        if (this.particleBehaviorFog == null) {
            this.particleBehaviorFog = new ParticleBehaviorFog(
                Vec3.createVectorHelper(this.pos.xCoord, this.pos.yCoord, this.pos.zCoord));
        } else if (!Minecraft.getMinecraft()
            .isSingleplayer() || !(Minecraft.getMinecraft().currentScreen instanceof GuiIngameMenu)) {
                this.particleBehaviorFog.tickUpdateList();
            }

        EntityClientPlayerMP entP = Minecraft.getMinecraft().thePlayer;
        this.spinSpeed = 0.02D;
        double spinSpeedMax = 0.4D;
        if (this.isCycloneFormingOrGreater()) {
            this.spinSpeed = spinSpeedMax * 0.0D
                + (double) ((float) this.levelCurIntensityStage - levelStormIntensityFormingStartVal + 1.0F)
                    * spinSpeedMax
                    * 0.2D;
        } else if (this.isTornadoFormingOrGreater()) {
            this.spinSpeed = spinSpeedMax * 0.2D;
        } else if (this.levelCurIntensityStage >= STATE_HIGHWIND) {
            this.spinSpeed = spinSpeedMax * 0.05D;
        } else {
            this.spinSpeed = spinSpeedMax * 0.02D;
        }

        if (this.isHurricane()) {
            this.spinSpeed += 0.1D;
        }

        if (this.size == 0) {
            this.size = 1;
        }

        int delay = Math.max(1, (int) (100.0F / (float) this.size * 1.0F));
        int loopSize = 1;
        short extraSpawning = 0;
        if (this.isSpinning()) {
            loopSize += 4;
            extraSpawning = 300;
        }

        if (this.stormType == TYPE_WATER) {
            if (this.levelCurIntensityStage >= STATE_STAGE5) {
                loopSize = 10;
                extraSpawning = 800;
            } else if (this.levelCurIntensityStage >= STATE_STAGE4) {
                loopSize = 8;
                extraSpawning = 700;
            } else if (this.levelCurIntensityStage >= STATE_STAGE3) {
                loopSize = 6;
                extraSpawning = 500;
            } else if (this.levelCurIntensityStage >= STATE_STAGE2) {
                loopSize = 4;
                extraSpawning = 400;
            } else {
                extraSpawning = 300;
            }
        }

        Random rand = new Random();
        Vec3 playerAdjPos = Vec3.createVectorHelper(entP.posX, this.pos.yCoord, entP.posZ);
        double maxSpawnDistFromPlayer = 512.0D;
        int spawnRad;
        double spawnRad1;
        Vec3 ent;
        EntityRotFX curDist;
        if (this.manager.getWorld()
            .getTotalWorldTime() % (long) (delay + ConfigMisc.Cloud_ParticleSpawnDelay) == 0L) {
            for (spawnRad = 0; spawnRad < loopSize; ++spawnRad) {
                if (this.listParticlesCloud.size() < this.size + extraSpawning) {
                    spawnRad1 = (double) this.size;
                    if (this.layer != 0) {
                        spawnRad1 = (double) (this.size * 5);
                    }

                    ent = Vec3.createVectorHelper(
                        this.pos.xCoord + rand.nextDouble() * spawnRad1 - rand.nextDouble() * spawnRad1,
                        (double) ((Integer) layers.get(this.layer)).intValue(),
                        this.pos.zCoord + rand.nextDouble() * spawnRad1 - rand.nextDouble() * spawnRad1);
                    if (ent.distanceTo(playerAdjPos) < maxSpawnDistFromPlayer) {
                        curDist = this.spawnFogParticle(ent.xCoord, ent.yCoord, ent.zCoord, 2);
                        this.listParticlesCloud.add(curDist);
                    }
                }
            }
        }

        if (this.levelCurIntensityStage >= STATE_HIGHWIND) {
            for (spawnRad = 0; spawnRad < (this.stormType == TYPE_WATER ? 50 : 3); ++spawnRad) {
                if (this.listParticlesGround.size() < (this.stormType == TYPE_WATER ? 600 : 150)) {
                    spawnRad1 = (double) (this.size / 4 * 3);
                    if (this.stormType == TYPE_WATER) {
                        spawnRad1 = (double) (this.size * 3);
                    }

                    ent = Vec3.createVectorHelper(
                        this.pos.xCoord + rand.nextDouble() * spawnRad1 - rand.nextDouble() * spawnRad1,
                        this.posGround.yCoord,
                        this.pos.zCoord + rand.nextDouble() * spawnRad1 - rand.nextDouble() * spawnRad1);
                    if (ent.distanceTo(playerAdjPos) < maxSpawnDistFromPlayer) {
                        int var38 = this.manager.getWorld()
                            .getHeightValue((int) ent.xCoord, (int) ent.zCoord);
                        EntityRotFX baseBright = this.spawnFogParticle(ent.xCoord, (double) (var38 + 3), ent.zCoord, 2);
                        baseBright.setScale(100.0F);
                        baseBright.rotationYaw = (float) rand.nextInt(360);
                        baseBright.rotationPitch = (float) rand.nextInt(360);
                        this.listParticlesGround.add(baseBright);
                    }
                }
            }
        }

        byte var34 = 1;
        byte var35 = 2;
        double var36 = (double) (this.size / 48);
        if (this.levelCurIntensityStage >= STATE_STAGE5) {
            var36 = 200.0D;
            var35 = 10;
            this.sizeMaxFunnelParticles = 1200;
        } else if (this.levelCurIntensityStage >= STATE_STAGE4) {
            var36 = 150.0D;
            var35 = 8;
            this.sizeMaxFunnelParticles = 1000;
        } else if (this.levelCurIntensityStage >= STATE_STAGE3) {
            var36 = 100.0D;
            var35 = 6;
            this.sizeMaxFunnelParticles = 800;
        } else if (this.levelCurIntensityStage >= STATE_STAGE2) {
            var36 = 50.0D;
            var35 = 4;
            this.sizeMaxFunnelParticles = 600;
        } else {
            this.sizeMaxFunnelParticles = 600;
        }

        int i;
        if ((this.isTornadoFormingOrGreater() || this.isCycloneFormingOrGreater() || this.attrib_waterSpout)
            && this.manager.getWorld()
                .getTotalWorldTime() % (long) (var34 + ConfigMisc.Storm_ParticleSpawnDelay) == 0L) {
            for (i = 0; i < var35; ++i) {
                if (this.listParticlesFunnel.size() >= this.sizeMaxFunnelParticles) {
                    ((EntityRotFX) this.listParticlesFunnel.get(0)).setDead();
                    this.listParticlesFunnel.remove(0);
                }

                if (this.listParticlesFunnel.size() < this.sizeMaxFunnelParticles) {
                    ent = Vec3.createVectorHelper(
                        this.pos.xCoord + rand.nextDouble() * var36 - rand.nextDouble() * var36,
                        this.pos.yCoord,
                        this.pos.zCoord + rand.nextDouble() * var36 - rand.nextDouble() * var36);
                    if (ent.distanceTo(playerAdjPos) < maxSpawnDistFromPlayer) {
                        curDist = this.spawnFogParticle(ent.xCoord, this.posBaseFormationPos.yCoord, ent.zCoord, 3);
                        curDist.setMaxAge(150 + (this.levelCurIntensityStage - 1) * 100 + rand.nextInt(100));
                        float var40 = 0.3F;
                        float curSpeed = rand.nextFloat() * 0.6F;
                        curDist.rotationYaw = (float) rand.nextInt(360);
                        float finalBright = Math.min(1.0F, var40 + curSpeed);
                        if (this.levelCurIntensityStage == STATE_HIGHWIND) {
                            curDist.setScale(150.0F);
                            curDist.setRBGColorF(finalBright - 0.2F, finalBright - 0.2F, finalBright);
                            // NTNH start: oceanic water-mist styling for tropical cyclones
                        } else if (this.stormType == TYPE_WATER) {
                            curDist.setScale(280.0F);
                            curDist.setRBGColorF(
                                Math.max(0.1F, finalBright - 0.08F),
                                Math.max(0.1F, finalBright - 0.04F),
                                Math.min(1.0F, finalBright + 0.12F));
                            // NTNH end
                        } else {
                            curDist.setScale(250.0F);
                            curDist.setRBGColorF(finalBright, finalBright, finalBright);
                        }

                        this.listParticlesFunnel.add(curDist);
                    }
                }
            }
        }

        float extraDropCalc;
        float distt;
        EntityRotFX var37;
        double var39;
        double var41;
        for (i = 0; i < this.listParticlesFunnel.size(); ++i) {
            var37 = (EntityRotFX) this.listParticlesFunnel.get(i);
            if (var37.isDead) {
                this.listParticlesFunnel.remove(var37);
            } else if (var37.posY > this.pos.yCoord) {
                var37.setDead();
                this.listParticlesFunnel.remove(var37);
            } else {
                var39 = this.pos.xCoord - var37.posX;
                var41 = this.pos.zCoord - var37.posZ;
                var37.rotationYaw = (float) (Math.atan2(var41, var39) * 180.0D / 3.141592653589793D) - 90.0F;
                var37.rotationYaw += (float) (var37.getEntityId() % 90);
                var37.rotationPitch = -30.0F;
                // NTNH start: include tropical cyclones in water-fade gradient
                if (this.levelCurIntensityStage == STATE_HIGHWIND || this.stormType == TYPE_WATER) {
                    // NTNH end
                    byte speed = 30;
                    if (var37.posY > this.posGround.yCoord + (double) speed) {
                        extraDropCalc = var37.getBlueColorF();
                        distt = 0.002F;
                        var37.setRBGColorF(
                            Math.min(extraDropCalc, var37.getRedColorF() + distt),
                            Math.min(extraDropCalc, var37.getGreenColorF() + distt),
                            extraDropCalc);
                    }
                }

                this.spinEntity(var37);
            }
        }

        double vecX;
        double vecZ;
        double var44;
        for (i = 0; i < this.listParticlesCloud.size(); ++i) {
            var37 = (EntityRotFX) this.listParticlesCloud.get(i);
            if (var37.isDead) {
                this.listParticlesCloud.remove(var37);
            } else {
                var39 = Math.sqrt(
                    var37.motionX * var37.motionX + var37.motionY * var37.motionY + var37.motionZ * var37.motionZ);
                var41 = var37.getDistance(this.pos.xCoord, var37.posY, this.pos.zCoord);
                float var42 = 15.0F;
                extraDropCalc = 0.0F;
                if (var41 < 200.0D && var37.getEntityId() % 20 < 5) {
                    extraDropCalc = (float) (var37.getEntityId() % 20) * var42;
                    if (this.isCycloneFormingOrGreater()) {
                        extraDropCalc = (float) (var37.getEntityId() % 20) * var42 * 5.0F;
                    }
                }

                if (this.isSpinning()) {
                    var44 = this.spinSpeed + rand.nextDouble() * 0.01D;
                    vecX = (double) this.size;
                    vecZ = var37.posX - this.pos.xCoord;
                    double angle = var37.posZ - this.pos.zCoord;
                    float var16 = (float) (Math.atan2(angle, vecZ) * 180.0D / 3.141592653589793D);
                    var16 = (float) ((double) var16 + var44 * 50.0D);
                    var16 = (float) ((double) var16 - (double) (var37.getEntityId() % 10) * 3.0D);
                    var16 += (float) (rand.nextInt(10) - rand.nextInt(10));
                    if (var41 > vecX) {
                        var16 += 40.0F;
                    }

                    if (var37.getEntityId() % 20 < 5) {
                        if (this.levelCurIntensityStage >= STATE_FORMING) {
                            if (this.stormType == TYPE_WATER) {
                                var16 += (float) (40 + var37.getEntityId() % 5 * 4);
                                if (var41 > (double) (150.0F
                                    + ((float) this.levelCurIntensityStage - levelStormIntensityFormingStartVal + 1.0F)
                                        * 30.0F)) {
                                    var16 += 10.0F;
                                }
                            } else {
                                var16 += (float) (30 + var37.getEntityId() % 5 * 4);
                            }
                        } else if (var41 > 150.0D) {
                            var16 += (float) (50 + var37.getEntityId() % 5 * 4);
                        }

                        double var161 = this.pos.xCoord - var37.posX;
                        double var18 = this.pos.zCoord - var37.posZ;
                        var37.rotationYaw = (float) (Math.atan2(var18, var161) * 180.0D / 3.141592653589793D) - 90.0F;
                        var37.rotationPitch = -20.0F - (float) (var37.getEntityId() % 10);
                    }

                    if (var39 < var44 * 20.0D) {
                        var37.motionX += -Math.sin(Math.toRadians((double) var16)) * var44;
                        var37.motionZ += Math.cos(Math.toRadians((double) var16)) * var44;
                    }
                } else {
                    distt = 0.2F * (float) (1 + this.layer);
                    float speed1 = this.getAdjustedSpeed() * distt;
                    float var45 = this.getAdjustedAngle();
                    var42 = 5.0F;
                    if (var37.getEntityId() % 20 < 5) {
                        extraDropCalc = (float) (var37.getEntityId() % 20) * var42;
                    }

                    if (var39 < (double) speed1 * 1.0D) {
                        var37.motionX += -Math.sin(Math.toRadians((double) var45)) * (double) speed1;
                        var37.motionZ += Math.cos(Math.toRadians((double) var45)) * (double) speed1;
                    }
                }

                if (Math.abs(var37.posY - (this.pos.yCoord - (double) extraDropCalc)) > 2.0D) {
                    if (var37.posY < this.pos.yCoord - (double) extraDropCalc) {
                        var37.motionY += 0.1D;
                    } else {
                        var37.motionY -= 0.1D;
                    }
                }

                distt = 0.15F;
                if (this.isCycloneFormingOrGreater()) {
                    distt = 0.9F;
                }

                if (var37.motionY < (double) (-distt)) {
                    var37.motionY = (double) (-distt);
                }

                if (var37.motionY > (double) distt) {
                    var37.motionY = (double) distt;
                }
            }
        }

        for (i = 0; i < this.listParticlesGround.size(); ++i) {
            var37 = (EntityRotFX) this.listParticlesGround.get(i);
            var39 = var37.getDistance(this.pos.xCoord, var37.posY, this.pos.zCoord);
            if (var37.isDead) {
                this.listParticlesGround.remove(var37);
            } else {
                var41 = Math.sqrt(
                    var37.motionX * var37.motionX + var37.motionY * var37.motionY + var37.motionZ * var37.motionZ);
                double var43 = Math.max(0.20000000298023224D, 5.0D * this.spinSpeed) + rand.nextDouble() * 0.01D;
                var44 = (double) this.size;
                vecX = var37.posX - this.pos.xCoord;
                vecZ = var37.posZ - this.pos.zCoord;
                float var46 = (float) (Math.atan2(vecZ, vecX) * 180.0D / 3.141592653589793D);
                var46 += 85.0F;
                short maxParticleSize = 60;
                if (this.stormType == TYPE_WATER) {
                    maxParticleSize = 150;
                    var43 /= 5.0D;
                }

                var37.setScale((float) Math.min((double) maxParticleSize, var39 * 2.0D));
                if (var39 < 20.0D) {
                    var37.setDead();
                }

                double var10000 = this.pos.xCoord - var37.posX;
                var10000 = this.pos.zCoord - var37.posZ;
                if (var41 < var43 * 20.0D) {
                    var37.motionX += -Math.sin(Math.toRadians((double) var46)) * var43;
                    var37.motionZ += Math.cos(Math.toRadians((double) var46)) * var43;
                }
            }
        }

    }

    public float getAdjustedSpeed() {
        return this.manager.windMan.getWindSpeedForClouds();
    }

    public float getAdjustedAngle() {
        float angle = this.manager.windMan.getWindAngleForClouds();
        float angleAdjust = Math.max(10.0F, Math.min(45.0F, 45.0F * this.levelTemperature * 0.2F));
        float targetYaw = 0.0F;
        if (this.levelTemperature > 0.0F) {
            targetYaw = 180.0F;
        } else {
            targetYaw = 0.0F;
        }

        float bestMove = MathHelper.wrapAngleTo180_float(targetYaw - angle);
        if (Math.abs(bestMove) < 180.0F) {
            if (bestMove > 0.0F) {
                angle -= angleAdjust;
            }

            if (bestMove < 0.0F) {
                angle += angleAdjust;
            }
        }

        return angle;
    }

    public void spinEntity(Entity entity1) {
        WeatherEntityConfig conf = this.getWeatherEntityConfigForStorm();
        Random rand = new Random();
        boolean forTornado = true;
        double radius = 10.0D;
        double scale = (double) conf.tornadoWidthScale;
        double d1 = this.pos.xCoord - entity1.posX;
        double d2 = this.pos.zCoord - entity1.posZ;
        float f;
        float f1;
        if (conf.type == WeatherEntityConfig.TYPE_SPOUT) {
            f = 30.0F * (float) Math.sin(
                Math.toRadians(
                    (double) (((float) entity1.worldObj.getTotalWorldTime() * 0.5F + (float) (this.ID * 50L))
                        % 360.0F)));
            f1 = (float) (1.0D - (entity1.posY - this.posGround.yCoord) / (this.pos.yCoord - this.posGround.yCoord));
            float distY = (float) Math.sin(Math.toRadians((double) (f1 * 360.0F)));
            float posOffsetZ = (float) (-Math.cos(Math.toRadians((double) (f1 * 360.0F))));
            d1 += (double) (f * distY);
            d2 += (double) (f * posOffsetZ);
        }

        f = (float) (Math.atan2(d2, d1) * 180.0D / 3.141592653589793D) - 90.0F;

        for (f1 = f; f1 < -180.0F; f1 += 360.0F) {
            ;
        }

        while (f1 >= 180.0F) {
            f1 -= 360.0F;
        }

        double var10000 = this.pos.yCoord - entity1.posY;
        double distXZ = Math.sqrt(Math.abs(d1)) + Math.sqrt(Math.abs(d2));
        double distY1;
        if (entity1.posY - this.pos.yCoord < 0.0D) {
            distY1 = 1.0D;
        } else {
            distY1 = entity1.posY - this.pos.yCoord;
        }

        if (distY1 > (double) this.maxHeight) {
            distY1 = (double) this.maxHeight;
        }

        double grab = 10.0D / (double) WeatherUtilEntity.getWeight(entity1, forTornado)
            * (Math.abs((double) this.maxHeight - distY1) / (double) this.maxHeight);
        float pullY = 0.0F;
        if (rand.nextInt(5) != 0) {
            ;
        }

        if (distXZ > 5.0D) {
            grab *= radius / distXZ;
        }

        pullY += conf.tornadoLiftRate / (WeatherUtilEntity.getWeight(entity1, forTornado) / 2.0F);
        double profileAngle;
        if (entity1 instanceof EntityPlayer) {
            profileAngle = 0.2D
                / ((double) WeatherUtilEntity.getWeight(entity1, forTornado) * ((distXZ + 1.0D) / radius));
            pullY = (float) ((double) pullY + profileAngle);
            double f3 = 10.0D * (double) ((float) (((double) WeatherUtilEntity.playerInAirTime + 1.0D) / 400.0D));
            if (f3 > 50.0D) {
                f3 = 50.0D;
            }

            if (f3 < -50.0D) {
                f3 = -50.0D;
            }

            grab -= f3;
            if (entity1.motionY > -0.8D) {
                entity1.fallDistance = 0.0F;
            } else if (entity1.motionY > -1.5D) {
                ;
            }
        } else if (entity1 instanceof EntityLivingBase) {
            profileAngle = 0.005D
                / ((double) WeatherUtilEntity.getWeight(entity1, forTornado) * ((distXZ + 1.0D) / radius));
            pullY = (float) ((double) pullY + profileAngle);
            int f31 = entity1.getEntityData()
                .getInteger("timeInAir");
            double f4 = 10.0D * (double) ((float) (((double) f31 + 1.0D) / 400.0D));
            if (f4 > 50.0D) {
                f4 = 50.0D;
            }

            if (f4 < -50.0D) {
                f4 = -50.0D;
            }

            grab -= f4;
            if (entity1.motionY > -1.5D) {
                entity1.fallDistance = 0.0F;
            }

            if (entity1.motionY > 0.30000001192092896D) {
                entity1.motionY = 0.30000001192092896D;
            }

            if (forTornado) {
                entity1.onGround = false;
            }
        }

        grab += (double) conf.relTornadoSize;
        profileAngle = Math.max(1.0D, 75.0D + grab - 10.0D * scale);
        f1 = (float) ((double) f1 + profileAngle);
        if (this != null && this.scale != 1.0F) {
            f1 += 20.0F - 20.0F * this.scale;
        }

        float f32 = (float) Math.cos((double) (-f1 * 0.01745329F - 3.1415927F));
        float f41 = (float) Math.sin((double) (-f1 * 0.01745329F - 3.1415927F));
        float f5 = conf.tornadoPullRate * 1.0F;
        if (this != null && this.scale != 1.0F) {
            f5 *= this.scale * 1.2F;
        }

        if (entity1 instanceof EntityLivingBase) {
            f5 = (float) ((double) f5
                / ((double) WeatherUtilEntity.getWeight(entity1, forTornado) * ((distXZ + 1.0D) / radius)));
        }

        if (entity1 instanceof EntityPlayer && conf.type != 0) {
            if (entity1.onGround) {
                f5 *= 10.5F;
            } else {
                f5 *= 5.0F;
            }
        } else if (entity1 instanceof EntityLivingBase && conf.type != 0) {
            f5 *= 1.5F;
        }

        if (conf.type == WeatherEntityConfig.TYPE_SPOUT && entity1 instanceof EntityLivingBase) {
            f5 *= 0.3F;
        }

        float moveX = f32 * f5;
        float moveZ = f41 * f5;
        float str = 1.0F;
        str = this.strength;
        if (conf.type == WeatherEntityConfig.TYPE_SPOUT && entity1 instanceof EntityLivingBase) {
            str *= 0.3F;
        }

        pullY *= str / 100.0F;
        if (this != null && this.scale != 1.0F) {
            pullY *= this.scale * 1.0F;
            pullY += 0.002F;
        }

        long lastPullTime = entity1.getEntityData()
            .getLong("lastPullTime");
        if (lastPullTime == entity1.worldObj.getTotalWorldTime()) {
            pullY = 0.0F;
        }

        entity1.getEntityData()
            .setLong("lastPullTime", entity1.worldObj.getTotalWorldTime());
        this.setVel(entity1, -moveX, pullY, moveZ);
    }

    public void setVel(Entity entity, float f, float f1, float f2) {
        entity.motionX += (double) f;
        entity.motionY += (double) f1;
        entity.motionZ += (double) f2;
        if (entity instanceof EntitySquid) {
            entity.setPosition(entity.posX + entity.motionX * 5.0D, entity.posY, entity.posZ + entity.motionZ * 5.0D);
        }

    }

    @SideOnly(Side.CLIENT)
    public EntityRotFX spawnFogParticle(double x, double y, double z, int parRenderOrder) {
        double speed = 0.0D;
        Random rand = new Random();
        EntityRotFX entityfx = this.particleBehaviorFog.spawnNewParticleIconFX(
            Minecraft.getMinecraft().theWorld,
            ParticleRegistry.cloud256,
            x,
            y,
            z,
            (rand.nextDouble() - rand.nextDouble()) * speed,
            0.0D,
            (rand.nextDouble() - rand.nextDouble()) * speed,
            parRenderOrder);
        this.particleBehaviorFog.initParticle(entityfx);
        entityfx.noClip = true;
        entityfx.callUpdatePB = false;
        boolean debug = false;
        if (debug) {
            ;
        }

        if (this.levelCurIntensityStage == STATE_NORMAL) {
            entityfx.setMaxAge(300 + rand.nextInt(100));
        } else {
            entityfx.setMaxAge(this.size / 2 + rand.nextInt(100));
        }

        if (entityfx.getEntityId() % 20 < 5 && this.isSpinning()) {
            entityfx.renderOrder = 3;
            entityfx.setMaxAge(this.size + rand.nextInt(100));
        }

        float randFloat = rand.nextFloat() * 0.6F;
        float baseBright = 0.7F;
        float finalBright;
        if (this.levelCurIntensityStage > STATE_NORMAL) {
            baseBright = 0.2F;
        } else if (this.attrib_precipitation) {
            baseBright = 0.2F;
        } else if (this.manager.isVanillaRainActiveOnServer) {
            baseBright = 0.2F;
        } else {
            finalBright = Math.min(1.0F, (float) (this.levelWater / this.levelWaterStartRaining)) * 0.6F;
            baseBright -= finalBright;
        }

        if (this.layer == 1) {
            baseBright = 0.1F;
        }

        finalBright = Math.min(1.0F, baseBright + randFloat);
        entityfx.setRBGColorF(finalBright, finalBright, finalBright);
        if (debug) {
            if (this.levelTemperature < 0.0F) {
                entityfx.setRBGColorF(0.0F, 0.0F, finalBright);
            } else if (this.levelTemperature > 0.0F) {
                entityfx.setRBGColorF(finalBright, 0.0F, 0.0F);
            }
        }

        ExtendedRenderer.rotEffRenderer.addEffect(entityfx);
        this.particleBehaviorFog.particles.add(entityfx);
        return entityfx;
    }

    public void reset() {
        this.setDead();
    }

    public void setDead() {
        this.isDead = true;
        if (FMLCommonHandler.instance()
            .getEffectiveSide() == Side.CLIENT) {
            this.cleanupClient();
        }

        this.cleanup();
    }

    public void cleanup() {
        this.manager = null;
        if (this.tornadoHelper != null) {
            this.tornadoHelper.storm = null;
        }

        this.tornadoHelper = null;
    }

    @SideOnly(Side.CLIENT)
    public void cleanupClient() {
        this.listParticlesCloud.clear();
        this.listParticlesFunnel.clear();
        if (this.particleBehaviorFog != null && this.particleBehaviorFog.particles != null) {
            this.particleBehaviorFog.particles.clear();
        }

        this.particleBehaviorFog = null;
    }

    public float getTemperatureMCToWeatherSys(float parOrigVal) {
        parOrigVal = (float) ((double) parOrigVal - 0.7D);
        parOrigVal *= 2.0F;
        return parOrigVal;
    }

    public void addWeatherEffectLightning(EntityLightningBolt parEnt) {
        this.manager.getWorld().weatherEffects.add(parEnt);
        ((WeatherManagerServer) this.manager).syncLightningNew(parEnt);
    }

    public class CachedNBTTagCompound {

        private NBTTagCompound newData;
        private NBTTagCompound cachedData;
        private boolean forced;

        public CachedNBTTagCompound() {
            this(new NBTTagCompound());
        }

        public CachedNBTTagCompound(NBTTagCompound newData) {
            this.forced = false;
            this.newData = newData;
        }

        public void setCachedNBT(NBTTagCompound cachedData) {
            if (cachedData == null) {
                cachedData = new NBTTagCompound();
            }

            this.cachedData = cachedData;
        }

        public NBTTagCompound getCachedNBT() {
            return this.cachedData;
        }

        public NBTTagCompound getNewNBT() {
            return this.newData;
        }

        public void setUpdateForced(boolean forced) {
            this.forced = forced;
        }

        public long getLong(String key) {
            if (!this.newData.hasKey(key)) {
                this.newData.setLong(key, this.cachedData.getLong(key));
            }

            return this.newData.getLong(key);
        }

        public void setLong(String key, long newVal) {
            if (!this.cachedData.hasKey(key) || this.cachedData.getLong(key) != newVal || this.forced) {
                this.newData.setLong(key, newVal);
            }

            this.cachedData.setLong(key, newVal);
        }

        public int getInteger(String key) {
            if (!this.newData.hasKey(key)) {
                this.newData.setInteger(key, this.cachedData.getInteger(key));
            }

            return this.newData.getInteger(key);
        }

        public void setInteger(String key, int newVal) {
            if (!this.cachedData.hasKey(key) || this.cachedData.getInteger(key) != newVal || this.forced) {
                this.newData.setInteger(key, newVal);
            }

            this.cachedData.setInteger(key, newVal);
        }

        public short getShort(String key) {
            if (!this.newData.hasKey(key)) {
                this.newData.setShort(key, this.cachedData.getShort(key));
            }

            return this.newData.getShort(key);
        }

        public void setShort(String key, short newVal) {
            if (!this.cachedData.hasKey(key) || this.cachedData.getShort(key) != newVal || this.forced) {
                this.newData.setShort(key, newVal);
            }

            this.cachedData.setShort(key, newVal);
        }

        public boolean getBoolean(String key) {
            if (!this.newData.hasKey(key)) {
                this.newData.setBoolean(key, this.cachedData.getBoolean(key));
            }

            return this.newData.getBoolean(key);
        }

        public void setBoolean(String key, boolean newVal) {
            if (!this.cachedData.hasKey(key) || this.cachedData.getBoolean(key) != newVal || this.forced) {
                this.newData.setBoolean(key, newVal);
            }

            this.cachedData.setBoolean(key, newVal);
        }

        public float getFloat(String key) {
            if (!this.newData.hasKey(key)) {
                this.newData.setFloat(key, this.cachedData.getFloat(key));
            }

            return this.newData.getFloat(key);
        }

        public void setFloat(String key, float newVal) {
            if (!this.cachedData.hasKey(key) || this.cachedData.getFloat(key) != newVal || this.forced) {
                this.newData.setFloat(key, newVal);
            }

            this.cachedData.setFloat(key, newVal);
        }
    }
}
