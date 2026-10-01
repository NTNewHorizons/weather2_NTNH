package weather2.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntityFlameFX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import CoroUtil.OldUtil;
import CoroUtil.api.weather.WindHandler;
import CoroUtil.util.ChunkCoordinatesBlock;
import CoroUtil.util.CoroUtilBlock;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import extendedrenderer.ExtendedRenderer;
import extendedrenderer.particle.ParticleRegistry;
import extendedrenderer.particle.behavior.ParticleBehaviors;
import extendedrenderer.particle.entity.EntityRotFX;
import extendedrenderer.particle.entity.EntityTexBiomeColorFX;
import extendedrenderer.particle.entity.EntityTexFX;
import weather2.ClientTickHandler;
import weather2.Weather;
import weather2.api.WindReader;
import weather2.client.entity.particle.EntityFallingRainFX;
import weather2.client.entity.particle.EntityFallingSnowFX;
import weather2.client.entity.particle.EntityWaterfallFX;
import weather2.config.ConfigMisc;
import weather2.util.WeatherUtil;
import weather2.util.WeatherUtilConfig;
import weather2.util.WeatherUtilEntity;
import weather2.util.WeatherUtilParticle;
import weather2.weathersystem.WeatherManagerClient;
import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.wind.WindManager;

@SideOnly(Side.CLIENT)
public class SceneEnhancer implements Runnable {

    public World lastWorldDetected = null;
    public static ParticleBehaviors pm;
    public static List spawnQueueNormal = new ArrayList();
    public static List spawnQueue = new ArrayList();
    public static long threadLastWorldTickTime;
    public static int lastTickFoundBlocks;
    public static long lastTickAmbient;
    public static long lastTickAmbientThreaded;
    public static ArrayList soundLocations = new ArrayList();
    public static HashMap soundTimeLocations = new HashMap();
    public static Block SOUNDMARKER_WATER = Blocks.water;
    public static Block SOUNDMARKER_LEAVES = Blocks.leaves;
    public static float curPrecipStr = 0.0F;
    public static float curPrecipStrTarget = 0.0F;
    public static float curOvercastStr = 0.0F;
    public static float curOvercastStrTarget = 0.0F;

    public SceneEnhancer() {
        pm = new ParticleBehaviors((Vec3) null);
    }

    public void run() {
        while (true) {
            try {
                while (true) {
                    this.tickClientThreaded();
                    Thread.sleep((long) ConfigMisc.Thread_Particle_Process_Delay);
                }
            } catch (Throwable var2) {
                var2.printStackTrace();
            }
        }
    }

    public void tickClient() {
        if (!WeatherUtil.isPaused()) {
            this.tryParticleSpawning();
            this.tickParticlePrecipitation();
            this.trySoundPlaying();
            Minecraft mc = FMLClientHandler.instance()
                .getClient();
            tryWind(mc.theWorld);
        }

    }

    public void tickClientThreaded() {
        Minecraft mc = FMLClientHandler.instance()
            .getClient();
        if (mc.theWorld != null && this.lastWorldDetected != mc.theWorld) {
            this.lastWorldDetected = mc.theWorld;
            this.reset();
        }

        if (mc.theWorld != null && mc.thePlayer != null
            && WeatherUtilConfig.listDimensionsWindEffects
                .contains(Integer.valueOf(mc.theWorld.provider.dimensionId))) {
            this.profileSurroundings();
            tryAmbientSounds();
        }

    }

    public synchronized void trySoundPlaying() {
        try {
            if (lastTickAmbient < System.currentTimeMillis()) {
                lastTickAmbient = System.currentTimeMillis() + 500L;
                Minecraft ex = FMLClientHandler.instance()
                    .getClient();
                WorldClient worldRef = ex.theWorld;
                EntityClientPlayerMP player = ex.thePlayer;
                byte size = 32;
                int hsize = size / 2;
                int curX = (int) player.posX;
                int curY = (int) player.posY;
                int curZ = (int) player.posZ;
                Random rand = new Random();

                for (int i = 0; i < soundLocations.size(); ++i) {
                    ChunkCoordinatesBlock cCor = (ChunkCoordinatesBlock) soundLocations.get(i);
                    if (Math.sqrt((double) cCor.getDistanceSquared(curX, curY, curZ)) > (double) size) {
                        soundLocations.remove(i--);
                        soundTimeLocations.remove(cCor);
                    } else {
                        Block block = getBlock(worldRef, cCor.posX, cCor.posY, cCor.posZ);
                        if (block != null
                            && (block.getMaterial() == Material.water || block.getMaterial() == Material.leaves)) {
                            long lastPlayTime = 0L;
                            if (soundTimeLocations.containsKey(cCor)) {
                                lastPlayTime = ((Long) soundTimeLocations.get(cCor)).longValue();
                            }

                            if (lastPlayTime < System.currentTimeMillis()) {
                                if (cCor.block == SOUNDMARKER_WATER) {
                                    soundTimeLocations.put(
                                        cCor,
                                        Long.valueOf(System.currentTimeMillis() + 2500L + (long) rand.nextInt(50)));
                                    ex.theWorld.playSound(
                                        (double) cCor.posX,
                                        (double) cCor.posY,
                                        (double) cCor.posZ,
                                        Weather.modID + ":env.waterfall",
                                        (float) ConfigMisc.volWaterfallScale,
                                        0.75F + rand.nextFloat() * 0.05F,
                                        false);
                                } else if (cCor.block == SOUNDMARKER_LEAVES) {
                                    float windSpeed = WindReader.getWindSpeed(
                                        ex.theWorld,
                                        Vec3.createVectorHelper(
                                            (double) cCor.posX,
                                            (double) cCor.posY,
                                            (double) cCor.posZ),
                                        WindReader.WindType.EVENT);
                                    if (windSpeed > 0.2F) {
                                        soundTimeLocations.put(
                                            cCor,
                                            Long.valueOf(
                                                System.currentTimeMillis() + 12000L + (long) rand.nextInt(50)));
                                        ex.theWorld.playSound(
                                            (double) cCor.posX,
                                            (double) cCor.posY,
                                            (double) cCor.posZ,
                                            Weather.modID + ":env.wind_calmfade",
                                            (float) ((double) (windSpeed * 4.0F) * ConfigMisc.volWindTreesScale),
                                            0.7F + rand.nextFloat() * 0.1F,
                                            false);
                                    } else {
                                        windSpeed = WindReader.getWindSpeed(
                                            ex.theWorld,
                                            Vec3.createVectorHelper(
                                                (double) cCor.posX,
                                                (double) cCor.posY,
                                                (double) cCor.posZ));
                                        if (ex.theWorld.rand.nextInt(15) == 0) {
                                            soundTimeLocations.put(
                                                cCor,
                                                Long.valueOf(
                                                    System.currentTimeMillis() + 12000L + (long) rand.nextInt(50)));
                                            ex.theWorld.playSound(
                                                (double) cCor.posX,
                                                (double) cCor.posY,
                                                (double) cCor.posZ,
                                                Weather.modID + ":env.wind_calmfade",
                                                (float) ((double) (windSpeed * 2.0F) * ConfigMisc.volWindTreesScale),
                                                0.7F + rand.nextFloat() * 0.1F,
                                                false);
                                        }
                                    }
                                }
                            }
                        } else {
                            soundLocations.remove(i);
                            soundTimeLocations.remove(cCor);
                        }
                    }
                }
            }
        } catch (Exception var16) {
            System.out.println("Weather2: Error handling sound play queue: ");
            var16.printStackTrace();
        }

    }

    @SideOnly(Side.CLIENT)
    public static void tryAmbientSounds() {
        Minecraft mc = FMLClientHandler.instance()
            .getClient();
        WorldClient worldRef = mc.theWorld;
        EntityClientPlayerMP player = mc.thePlayer;
        new Random();
        if (lastTickAmbientThreaded < System.currentTimeMillis()) {
            lastTickAmbientThreaded = System.currentTimeMillis() + 500L;
            byte size = 32;
            int hsize = size / 2;
            int curX = (int) player.posX;
            int curY = (int) player.posY;
            int curZ = (int) player.posZ;

            for (int xx = curX - hsize; xx < curX + hsize; ++xx) {
                for (int yy = curY - hsize / 2; yy < curY + hsize; ++yy) {
                    for (int zz = curZ - hsize; zz < curZ + hsize; ++zz) {
                        Block block = getBlock(worldRef, xx, yy, zz);
                        if (block != null) {
                            int j;
                            if (ConfigMisc.Wind_Particle_waterfall && block.getMaterial() == Material.water) {
                                int var20 = getBlockMetadata(worldRef, xx, yy, zz);
                                if ((var20 & 8) != 0) {
                                    int index;
                                    for (index = 0; yy - index > 0; ++index) {
                                        Block meta2 = getBlock(worldRef, xx, yy - index, zz);
                                        if (meta2 != null && meta2.getMaterial() != Material.water) {
                                            break;
                                        }
                                    }

                                    j = yy - index + 1;
                                    int var21 = getBlockMetadata(worldRef, xx, j + 10, zz);
                                    Block block2 = getBlock(worldRef, xx, j + 10, zz);
                                    if (index >= 4 && block2 != null
                                        && block2.getMaterial() == Material.water
                                        && (var21 & 8) != 0) {
                                        boolean proxFail1 = false;

                                        for (int j1 = 0; j1 < soundLocations.size(); ++j1) {
                                            if (Math.sqrt(
                                                (double) ((ChunkCoordinatesBlock) soundLocations.get(j1))
                                                    .getDistanceSquared(xx, j, zz))
                                                < 5.0D) {
                                                proxFail1 = true;
                                                break;
                                            }
                                        }

                                        if (!proxFail1) {
                                            soundLocations
                                                .add(new ChunkCoordinatesBlock(xx, j, zz, SOUNDMARKER_WATER, 0));
                                        }
                                    }
                                }
                            } else if (ConfigMisc.volWindTreesScale > 0.0D && block.getMaterial() == Material.leaves) {
                                boolean proxFail = false;

                                for (j = 0; j < soundLocations.size(); ++j) {
                                    if (Math.sqrt(
                                        (double) ((ChunkCoordinatesBlock) soundLocations.get(j))
                                            .getDistanceSquared(xx, yy, zz))
                                        < 15.0D) {
                                        proxFail = true;
                                        break;
                                    }
                                }

                                if (!proxFail) {
                                    soundLocations.add(new ChunkCoordinatesBlock(xx, yy, zz, SOUNDMARKER_LEAVES, 0));
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    public void reset() {
        if (ExtendedRenderer.rotEffRenderer != null) {
            for (int i = 0; i < ExtendedRenderer.rotEffRenderer.layers; ++i) {
                if (ExtendedRenderer.rotEffRenderer.fxLayers[i] != null) {
                    ExtendedRenderer.rotEffRenderer.fxLayers[i].clear();
                }
            }
        }

        this.lastWorldDetected.weatherEffects.clear();
        WeatherUtilParticle.getFXLayers();
        // NTNH start: reset DSurround rain sync on world transition
        weather2.compat.WeatherNTNHHooks.onPrecipitationTick(0.0F);
        // NTNH end
    }

    public void tickParticlePrecipitation() {
        if (ConfigMisc.Particle_RainSnow) {
            EntityClientPlayerMP entP = FMLClientHandler.instance()
                .getClient().thePlayer;
            float curPrecipVal = getRainStrengthAndControlVisuals(entP);
            float maxPrecip = 0.5F;
            int precipitationHeight = entP.worldObj
                .getPrecipitationHeight(MathHelper.floor_double(entP.posX), MathHelper.floor_double(entP.posZ));
            BiomeGenBase biomegenbase = entP.worldObj
                .getBiomeGenForCoords(MathHelper.floor_double(entP.posX), MathHelper.floor_double(entP.posZ));
            if (biomegenbase != null) {
                float temperature = biomegenbase.getFloatTemperature(
                    MathHelper.floor_double(entP.posX),
                    MathHelper.floor_double(entP.posY),
                    MathHelper.floor_double(entP.posZ));
                int i;
                byte spawnAreaSize;
                if (entP.worldObj.getWorldChunkManager()
                    .getTemperatureAtHeight(temperature, precipitationHeight) >= 0.15F) {
                    curPrecipVal = Math.min(maxPrecip, Math.abs(curPrecipVal));
                    if (curPrecipVal > 0.0F && entP.worldObj.canLightningStrikeAt(
                        MathHelper.floor_double(entP.posX),
                        MathHelper.floor_double(entP.boundingBox.minY),
                        MathHelper.floor_double(entP.posZ))) {
                        for (i = 0; (double) i
                            < (double) (curPrecipVal * 20.0F) * ConfigMisc.Particle_Precipitation_effect_rate; ++i) {
                            spawnAreaSize = 15;
                            EntityFallingRainFX spawnAbove = new EntityFallingRainFX(
                                entP.worldObj,
                                entP.posX + (double) entP.worldObj.rand.nextInt(spawnAreaSize)
                                    - (double) (spawnAreaSize / 2),
                                entP.posY + 15.0D,
                                entP.posZ + (double) entP.worldObj.rand.nextInt(spawnAreaSize)
                                    - (double) (spawnAreaSize / 2),
                                0.0D,
                                -5.0D - (double) entP.worldObj.rand.nextInt(5) * -1.0D,
                                0.0D,
                                1.5D,
                                3);
                            spawnAbove.severityOfRainRate = (int) (curPrecipVal * 5.0F);
                            spawnAbove.renderDistanceWeight = 1.0D;
                            spawnAbove.setSize(1.2F, 1.2F);
                            spawnAbove.rotationYaw = (float) spawnAbove.worldObj.rand.nextInt(360) - 180.0F;
                            spawnAbove.setGravity(1.0E-5F);
                            spawnAbove.spawnAsWeatherEffect();
                        }
                    }
                } else {
                    curPrecipVal = Math.min(maxPrecip, Math.abs(curPrecipVal));
                    if (curPrecipVal > 0.0F) {
                        for (i = 0; (double) i
                            < (double) (curPrecipVal * 5.0F) * ConfigMisc.Particle_Precipitation_effect_rate; ++i) {
                            spawnAreaSize = 50;
                            byte var14 = 10;
                            EntityFallingSnowFX ent = new EntityFallingSnowFX(
                                entP.worldObj,
                                entP.posX + (double) entP.worldObj.rand.nextInt(spawnAreaSize)
                                    - (double) (spawnAreaSize / 2),
                                entP.posY + (double) var14,
                                entP.posZ + (double) entP.worldObj.rand.nextInt(spawnAreaSize)
                                    - (double) (spawnAreaSize / 2),
                                0.0D,
                                -5.0D - (double) entP.worldObj.rand.nextInt(5) * -1.0D,
                                0.0D,
                                5.5D,
                                6);
                            ent.severityOfRainRate = (int) (curPrecipVal * 5.0F);
                            ent.renderDistanceWeight = 1.0D;
                            ent.setSize(1.2F, 1.2F);
                            ent.rotationYaw = (float) ent.worldObj.rand.nextInt(360) - 180.0F;
                            ent.setGravity(1.0E-5F);
                            ent.spawnAsWeatherEffect();
                        }
                    }
                }
            }
        }

    }

    public static float getRainStrengthAndControlVisuals(EntityPlayer entP) {
        return getRainStrengthAndControlVisuals(entP, false);
    }

    public static float getRainStrengthAndControlVisuals(EntityPlayer entP, boolean forOvercast) {
        Minecraft mc = FMLClientHandler.instance()
            .getClient();
        double maxStormDist = 384.0D;
        Vec3 plPos = Vec3.createVectorHelper(entP.posX, (double) StormObject.static_YPos_layer0, entP.posZ);
        StormObject storm = null;
        ClientTickHandler.checkClientWeather();
        if (ClientTickHandler.weatherManager == null) {
            return 0.0F;
        } else {
            storm = ClientTickHandler.weatherManager
                .getClosestStorm(plPos, maxStormDist, StormObject.STATE_FORMING, true);
            if (forOvercast) {
                ;
            }

            boolean closeEnough = false;
            double stormDist = 9999.0D;
            float tempAdj = 1.0F;
            float sizeToUse = 0.0F;
            float overcastModeMinPrecip = 0.2F;
            if (storm != null) {
                sizeToUse = (float) storm.size;
                if (forOvercast) {
                    sizeToUse *= 1.0F;
                }

                stormDist = storm.pos.distanceTo(plPos);
                if ((double) sizeToUse > stormDist) {
                    closeEnough = true;
                }
            }

            if (closeEnough) {
                double stormIntensity = ((double) sizeToUse - stormDist) / (double) sizeToUse;
                tempAdj = storm.levelTemperature > 0.0F ? 1.0F : -1.0F;
                if (storm.levelCurIntensityStage == StormObject.STATE_NORMAL && stormIntensity > 0.3D) {
                    stormIntensity = 0.3D;
                }

                if (ConfigMisc.Storm_NoRainVisual) {
                    stormIntensity = 0.0D;
                }

                mc.theWorld.getWorldInfo()
                    .setRaining(true);
                mc.theWorld.getWorldInfo()
                    .setThundering(true);
                if (forOvercast) {
                    curOvercastStrTarget = (float) stormIntensity;
                } else {
                    curPrecipStrTarget = (float) stormIntensity;
                }
            } else if (!ConfigMisc.overcastMode) {
                mc.theWorld.getWorldInfo()
                    .setRaining(false);
                mc.theWorld.getWorldInfo()
                    .setThundering(false);
                if (forOvercast) {
                    curOvercastStrTarget = 0.0F;
                } else {
                    curPrecipStrTarget = 0.0F;
                }
            } else if (ClientTickHandler.weatherManager.isVanillaRainActiveOnServer) {
                mc.theWorld.getWorldInfo()
                    .setRaining(true);
                mc.theWorld.getWorldInfo()
                    .setThundering(true);
                if (forOvercast) {
                    curOvercastStrTarget = overcastModeMinPrecip;
                } else {
                    curPrecipStrTarget = overcastModeMinPrecip;
                }
            } else if (forOvercast) {
                curOvercastStrTarget = 0.0F;
            } else {
                curPrecipStrTarget = 0.0F;
            }

            if (forOvercast) {
                if (curOvercastStr > curOvercastStrTarget) {
                    curOvercastStr -= 0.001F;
                } else if (curOvercastStr < curOvercastStrTarget) {
                    curOvercastStr += 0.001F;
                }

                if ((double) curOvercastStr < 1.0E-4D && curOvercastStr > -1.0E-4F) {
                    curOvercastStr = 0.0F;
                }

                return curOvercastStr * tempAdj;
            } else {
                if (curPrecipStr > curPrecipStrTarget) {
                    curPrecipStr -= 0.001F;
                } else if (curPrecipStr < curPrecipStrTarget) {
                    curPrecipStr += 0.001F;
                }

                if ((double) curPrecipStr < 1.0E-4D && curPrecipStr > -1.0E-4F) {
                    curPrecipStr = 0.0F;
                }

                // NTNH start: Dynamic Surroundings synergy & client rain strength synchronization
                weather2.compat.WeatherNTNHHooks.onPrecipitationTick(curPrecipStr);
                // NTNH end

                return curPrecipStr * tempAdj;
            }
        }
    }

    public synchronized void tryParticleSpawning() {
        if (spawnQueue.size() > 0) {
            ;
        }

        try {
            int ex;
            for (ex = 0; ex < spawnQueue.size(); ++ex) {
                Entity ent = (Entity) spawnQueue.get(ex);
                if (ent != null && ent.worldObj != null) {
                    if (ent instanceof EntityRotFX) {
                        ((EntityRotFX) ent).spawnAsWeatherEffect();
                    } else {
                        ent.worldObj.addWeatherEffect(ent);
                    }
                }
            }

            for (ex = 0; ex < spawnQueueNormal.size(); ++ex) {
                EntityFX var4 = (EntityFX) spawnQueueNormal.get(ex);
                if (var4 != null && var4.worldObj != null) {
                    Minecraft.getMinecraft().effectRenderer.addEffect(var4);
                }
            }
        } catch (Exception var3) {
            System.out.println("Weather2: Error handling particle spawn queue: ");
            var3.printStackTrace();
        }

        spawnQueue.clear();
        spawnQueueNormal.clear();
    }

    public void profileSurroundings() {
        Minecraft mc = FMLClientHandler.instance()
            .getClient();
        World worldRef = this.lastWorldDetected;
        EntityClientPlayerMP player = FMLClientHandler.instance()
            .getClient().thePlayer;
        WeatherManagerClient manager = ClientTickHandler.weatherManager;
        if (worldRef != null && player != null && manager != null && manager.windMan != null) {
            if (threadLastWorldTickTime != worldRef.getTotalWorldTime()) {
                threadLastWorldTickTime = worldRef.getTotalWorldTime();
                Random rand = new Random();
                byte size = 40;
                int hsize = size / 2;
                int curX = (int) player.posX;
                int curY = (int) player.posY;
                int curZ = (int) player.posZ;
                float windStr = manager.windMan.getWindSpeedForPriority();
                float lastBlockCount;
                if (mc.objectMouseOver != null) {
                    Block spawnRate = mc.theWorld
                        .getBlock(mc.objectMouseOver.blockX, mc.objectMouseOver.blockY, mc.objectMouseOver.blockZ);
                    if (CoroUtilBlock.isAir(spawnRate) && spawnRate.getMaterial() == Material.wood) {
                        lastBlockCount = 0.0F;
                        lastBlockCount = ((Float) OldUtil.getPrivateValueSRGMCP(
                            PlayerControllerMP.class,
                            mc.playerController,
                            OldUtil.refl_curBlockDamageMP_obf,
                            OldUtil.refl_curBlockDamageMP_mcp)).floatValue();
                        if (lastBlockCount > 0.0F) {
                            ;
                        }
                    }
                }

                if (ConfigMisc.Wind_Particle_leafs || ConfigMisc.Wind_Particle_air
                    || ConfigMisc.Wind_Particle_sand
                    || ConfigMisc.Wind_Particle_waterfall) {
                    int var32 = (int) (30.0D / ((double) windStr + 0.001D));
                    lastBlockCount = (float) lastTickFoundBlocks;
                    float particleCreationRate = (float) ConfigMisc.Wind_Particle_effect_rate;
                    float maxScaleSample = 15000.0F;
                    if (lastBlockCount > maxScaleSample) {
                        lastBlockCount = maxScaleSample - 1.0F;
                    }

                    float scaleRate = (maxScaleSample - lastBlockCount) / maxScaleSample;
                    var32 = (int) ((float) var32 / (scaleRate + 0.001F) / (particleCreationRate + 0.001F));
                    int BlockCountRate = (int) ((300.0F / scaleRate + 0.001F) / (particleCreationRate + 0.001F));
                    var32 *= mc.gameSettings.particleSetting + 1;
                    BlockCountRate *= mc.gameSettings.particleSetting + 1;
                    var32 /= 2;
                    if (var32 < 40) {
                        var32 = 40;
                    }

                    if (BlockCountRate < 80) {
                        BlockCountRate = 80;
                    }

                    if (BlockCountRate > 5000) {
                        BlockCountRate = 5000;
                    }

                    lastTickFoundBlocks = 0;

                    for (int xx = curX - hsize; xx < curX + hsize; ++xx) {
                        for (int yy = curY - hsize / 2; yy < curY + hsize; ++yy) {
                            for (int zz = curZ - hsize; zz < curZ + hsize; ++zz) {
                                Block block = getBlock(worldRef, xx, yy, zz);
                                if (block != null && (block.getMaterial() == Material.leaves
                                    || block.getMaterial() == Material.vine)) {
                                    ++lastTickFoundBlocks;
                                    if (worldRef.rand.nextInt(var32) == 0 && ConfigMisc.Wind_Particle_leafs
                                        && (CoroUtilBlock.isAir(getBlock(worldRef, xx, yy - 1, zz))
                                            || CoroUtilBlock.isAir(getBlock(worldRef, xx - 1, yy, zz)))) {
                                        EntityTexBiomeColorFX var34 = new EntityTexBiomeColorFX(
                                            worldRef,
                                            (double) xx,
                                            (double) yy - 0.5D,
                                            (double) zz,
                                            0.0D,
                                            0.0D,
                                            0.0D,
                                            10.0D,
                                            0,
                                            WeatherUtilParticle.effLeafID,
                                            getBlockMetadata(worldRef, xx, yy, zz),
                                            xx,
                                            yy,
                                            zz);
                                        var34.setGravity(0.1F);
                                        var34.rotationYaw = (float) rand.nextInt(360);
                                        var34.rotationPitch = (float) rand.nextInt(360);
                                        spawnQueue.add(var34);
                                    }
                                } else if (ConfigMisc.Wind_Particle_waterfall
                                    && player.getDistance((double) xx, (double) yy, (double) zz) < 16.0D
                                    && block != null
                                    && block.getMaterial() == Material.water) {
                                        int var33 = getBlockMetadata(worldRef, xx, yy, zz);
                                        if ((var33 & 8) != 0) {
                                            lastTickFoundBlocks += 70;
                                            int chance = (int) (1.0F + (float) BlockCountRate / 120.0F);
                                            Block var35 = getBlock(worldRef, xx, yy - 1, zz);
                                            int meta2 = getBlockMetadata(worldRef, xx, yy - 1, zz);
                                            Block block3 = getBlock(worldRef, xx, yy + 10, zz);
                                            if ((var35 == null || var35.getMaterial() != Material.water
                                                || (meta2 & 8) == 0) && block3 != null
                                                && block3.getMaterial() == Material.water
                                                || worldRef.rand.nextInt(chance) == 0) {
                                                float range = 0.5F;
                                                new EntityWaterfallFX(
                                                    worldRef,
                                                    (double) xx + 0.5D
                                                        + (double) (rand.nextFloat() * range - range / 2.0F),
                                                    (double) yy + 0.5D
                                                        + (double) (rand.nextFloat() * range - range / 2.0F),
                                                    (double) zz + 0.5D
                                                        + (double) (rand.nextFloat() * range - range / 2.0F),
                                                    0.0D,
                                                    0.0D,
                                                    0.0D,
                                                    6.0D,
                                                    2);
                                                EntityWaterfallFX waterP;
                                                if ((var35 == null || var35.getMaterial() != Material.water
                                                    || (meta2 & 8) == 0) && block3 != null
                                                    && block3.getMaterial() == Material.water) {
                                                    range = 2.0F;
                                                    float speed1 = 0.2F;

                                                    for (int i = 0; i < 10; ++i) {
                                                        if (worldRef.rand.nextInt(chance / 2) == 0) {
                                                            waterP = new EntityWaterfallFX(
                                                                worldRef,
                                                                (double) xx + 0.5D
                                                                    + (double) (rand.nextFloat() * range
                                                                        - range / 2.0F),
                                                                (double) yy + 0.699999988079071D
                                                                    + (double) (rand.nextFloat() * range
                                                                        - range / 2.0F),
                                                                (double) zz + 0.5D
                                                                    + (double) (rand.nextFloat() * range
                                                                        - range / 2.0F),
                                                                (double) (rand.nextFloat() * speed1 - speed1 / 2.0F),
                                                                (double) (rand.nextFloat() * speed1 - speed1 / 2.0F),
                                                                (double) (rand.nextFloat() * speed1 - speed1 / 2.0F),
                                                                2.0D,
                                                                3);
                                                            waterP.motionY = 4.5D;
                                                            spawnQueueNormal.add(waterP);
                                                        }
                                                    }
                                                } else {
                                                    waterP = new EntityWaterfallFX(
                                                        worldRef,
                                                        (double) xx + 0.5D
                                                            + (double) (rand.nextFloat() * range - range / 2.0F),
                                                        (double) yy + 0.5D
                                                            + (double) (rand.nextFloat() * range - range / 2.0F),
                                                        (double) zz + 0.5D
                                                            + (double) (rand.nextFloat() * range - range / 2.0F),
                                                        0.0D,
                                                        0.0D,
                                                        0.0D,
                                                        6.0D,
                                                        2);
                                                    waterP.motionY = 0.5D;
                                                    spawnQueueNormal.add(waterP);
                                                }
                                            }
                                        }
                                    } else if (ConfigMisc.Wind_Particle_fire && block != null && block == Blocks.fire) {
                                        ++lastTickFoundBlocks;
                                        if (worldRef.rand.nextInt(Math.max(1, var32 / 100)) == 0) {
                                            double speed = 0.15D;
                                            EntityRotFX entityfx = pm.spawnNewParticleIconFX(
                                                worldRef,
                                                ParticleRegistry.smoke,
                                                (double) xx + rand.nextDouble(),
                                                (double) yy + 0.2D + rand.nextDouble() * 0.2D,
                                                (double) zz + rand.nextDouble(),
                                                (rand.nextDouble() - rand.nextDouble()) * speed,
                                                0.03D,
                                                (rand.nextDouble() - rand.nextDouble()) * speed);
                                            ParticleBehaviors.setParticleRandoms(entityfx, true, true);
                                            ParticleBehaviors.setParticleFire(entityfx);
                                            entityfx.setMaxAge(100 + rand.nextInt(300));
                                            spawnQueueNormal.add(entityfx);
                                        }
                                    }
                            }
                        }
                    }

                }
            }
        } else {
            try {
                Thread.sleep(1000L);
            } catch (Exception var31) {
                var31.printStackTrace();
            }

        }
    }

    @SideOnly(Side.CLIENT)
    public static void tryWind(World world) {
        Minecraft mc = FMLClientHandler.instance()
            .getClient();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player != null) {
            boolean dist = true;
            List list = null;
            list = world.loadedEntityList;
            Random rand = new Random();
            int handleCount = 0;
            if ((double) ClientTickHandler.weatherManager.windMan.getWindSpeedForPriority() >= 0.1D) {
                for (int windMan = 0; windMan < world.weatherEffects.size(); ++windMan) {
                    ++handleCount;
                    Entity volScaleFar = (Entity) world.weatherEffects.get(windMan);
                    if (!(volScaleFar instanceof EntityLightningBolt) && volScaleFar instanceof EntityFX
                        && volScaleFar != null) {
                        if (world.getHeightValue(
                            MathHelper.floor_double(volScaleFar.posX),
                            MathHelper.floor_double(volScaleFar.posZ)) - 1 < (int) volScaleFar.posY + 1
                            || volScaleFar instanceof EntityTexFX) {
                            if (volScaleFar instanceof EntityFlameFX) {
                                WeatherUtilParticle.setParticleAge(
                                    (EntityFX) volScaleFar,
                                    WeatherUtilParticle.getParticleAge((EntityFX) volScaleFar) + 2);
                            } else if (volScaleFar instanceof WindHandler) {
                                if (((WindHandler) volScaleFar).getParticleDecayExtra() > 0
                                    && WeatherUtilParticle.getParticleAge((EntityFX) volScaleFar) % 2 == 0) {
                                    WeatherUtilParticle.setParticleAge(
                                        (EntityFX) volScaleFar,
                                        WeatherUtilParticle.getParticleAge((EntityFX) volScaleFar)
                                            + ((WindHandler) volScaleFar).getParticleDecayExtra());
                                }
                            } else if (WeatherUtilParticle.getParticleAge((EntityFX) volScaleFar) % 2 == 0) {
                                WeatherUtilParticle.setParticleAge(
                                    (EntityFX) volScaleFar,
                                    WeatherUtilParticle.getParticleAge((EntityFX) volScaleFar) + 1);
                            }

                            if (volScaleFar instanceof EntityTexFX
                                && ((EntityTexFX) volScaleFar).getParticleTextureIndex()
                                    == WeatherUtilParticle.effLeafID) {
                                if (volScaleFar.motionX < 0.009999999776482582D
                                    && volScaleFar.motionZ < 0.009999999776482582D) {
                                    volScaleFar.motionY += rand.nextDouble() * 0.02D;
                                }

                                volScaleFar.motionY -= 0.009999999776482582D;
                            }
                        }

                        applyWindForce(volScaleFar);
                        applyWindForce(volScaleFar);
                    }
                }
            }

            WindManager var12 = ClientTickHandler.weatherManager.windMan;
            if (WeatherUtilParticle.fxLayers != null && (double) var12.getWindSpeedForPriority() >= 0.1D) {
                int i;
                int var13;
                for (var13 = 0; var13 < 4; ++var13) {
                    for (i = 0; i < WeatherUtilParticle.fxLayers[var13].size(); ++i) {
                        Entity entity1 = (Entity) WeatherUtilParticle.fxLayers[var13].get(i);
                        if (ConfigMisc.Particle_VanillaAndWeatherOnly) {
                            String className = entity1.getClass()
                                .getName();
                            if (!className.contains("net.minecraft.") && !className.contains("weather2.")) {
                                continue;
                            }
                        }

                        if (world.getHeightValue(
                            MathHelper.floor_double(entity1.posX),
                            MathHelper.floor_double(entity1.posZ)) - 1 < (int) entity1.posY + 1
                            || entity1 instanceof EntityTexFX) {
                            if (entity1 instanceof EntityFlameFX) {
                                if ((double) var12.getWindSpeedForPriority() >= 0.5D) {
                                    WeatherUtilParticle.setParticleAge(
                                        (EntityFX) entity1,
                                        WeatherUtilParticle.getParticleAge((EntityFX) entity1) + 2);
                                }
                            } else if (entity1 instanceof WindHandler) {
                                if (((WindHandler) entity1).getParticleDecayExtra() > 0
                                    && WeatherUtilParticle.getParticleAge((EntityFX) entity1) % 2 == 0) {
                                    WeatherUtilParticle.setParticleAge(
                                        (EntityFX) entity1,
                                        WeatherUtilParticle.getParticleAge((EntityFX) entity1)
                                            + ((WindHandler) entity1).getParticleDecayExtra());
                                }
                            } else if (WeatherUtilParticle.getParticleAge((EntityFX) entity1) % 2 == 0) {
                                WeatherUtilParticle.setParticleAge(
                                    (EntityFX) entity1,
                                    WeatherUtilParticle.getParticleAge((EntityFX) entity1) + 1);
                            }

                            if (!(entity1 instanceof EntityWaterfallFX)) {
                                if (entity1.onGround) {
                                    entity1.motionY += rand.nextDouble() * entity1.motionX;
                                }

                                if (entity1.motionX < 0.009999999776482582D
                                    && entity1.motionZ < 0.009999999776482582D) {
                                    entity1.motionY += rand.nextDouble() * 0.02D;
                                }
                            }

                            applyWindForce(entity1);
                        }
                    }
                }

                for (var13 = 0; var13 < ExtendedRenderer.rotEffRenderer.layers; ++var13) {
                    for (i = 0; i < ExtendedRenderer.rotEffRenderer.fxLayers[var13].size(); ++i) {
                        Entity var10000 = (Entity) ExtendedRenderer.rotEffRenderer.fxLayers[var13].get(i);
                    }
                }
            }

            float var14 = var12.getWindSpeedForPriority() * 1.0F;
            if (var12.getWindSpeedForPriority() <= 0.07F) {
                var14 = 0.0F;
            }

            var14 = (float) ((double) var14 * ConfigMisc.volWindScale);
        }
    }

    public static void applyWindForce(Entity ent) {
        applyWindForce(ent, 1.0D);
    }

    public static void applyWindForce(Entity ent, double multiplier) {
        WindManager windMan = ClientTickHandler.weatherManager.windMan;
        float windSpeed = windMan.getWindSpeedForPriority();
        float windAngle = windMan.getWindAngleForPriority();
        double speed = (double) windSpeed * 0.1D / (double) WeatherUtilEntity.getWeight(ent);
        speed *= multiplier;
        if (ent.onGround && (double) windSpeed < 0.7D && speed < 0.3D) {
            speed = 0.0D;
        }

        ent.motionX += speed * (double) (-MathHelper.sin(windAngle / 180.0F * 3.1415927F) * MathHelper.cos(0.0F));
        ent.motionZ += speed * (double) (MathHelper.cos(windAngle / 180.0F * 3.1415927F) * MathHelper.cos(0.0F));
    }

    @SideOnly(Side.CLIENT)
    private static Block getBlock(World parWorld, int x, int y, int z) {
        try {
            return !parWorld.checkChunksExist(x, 0, z, x, 128, z) ? null : parWorld.getBlock(x, y, z);
        } catch (Exception var5) {
            return null;
        }
    }

    @SideOnly(Side.CLIENT)
    private static int getBlockMetadata(World parWorld, int x, int y, int z) {
        return !parWorld.checkChunksExist(x, 0, z, x, 128, z) ? 0 : parWorld.getBlockMetadata(x, y, z);
    }

}
