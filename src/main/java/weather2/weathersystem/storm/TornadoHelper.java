package weather2.weathersystem.storm;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import CoroUtil.util.CoroUtilBlock;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import weather2.Weather;
import weather2.config.ConfigMisc;
import weather2.entity.EntityMovingBlock;
import weather2.util.WeatherUtil;
import weather2.util.WeatherUtilEntity;
import weather2.util.WeatherUtilSound;

public class TornadoHelper {

    public StormObject storm;
    public int blockCount = 0;
    public int ripCount = 0;
    public long lastGrabTime = 0L;
    public int tickGrabCount = 0;
    public int removeCount = 0;
    public int tryRipCount = 0;
    public int tornadoBaseSize = 5;
    public int grabDist = 100;
    public boolean lastTickPlayerClose;

    public TornadoHelper(StormObject parStorm) {
        this.storm = parStorm;
    }

    public int getTornadoBaseSize() {
        byte sizeChange = 10;
        return this.storm.levelCurIntensityStage >= StormObject.STATE_STAGE5 ? sizeChange * 9
            : (this.storm.levelCurIntensityStage >= StormObject.STATE_STAGE4 ? sizeChange * 7
                : (this.storm.levelCurIntensityStage >= StormObject.STATE_STAGE3 ? sizeChange * 5
                    : (this.storm.levelCurIntensityStage >= StormObject.STATE_STAGE2 ? sizeChange * 4
                        : (this.storm.levelCurIntensityStage >= StormObject.STATE_STAGE1 ? sizeChange * 3
                            : (this.storm.levelCurIntensityStage >= StormObject.STATE_FORMING ? sizeChange * 1 : 5)))));
    }

    public void tick(World parWorld) {
        if (this.storm != null) {
            boolean seesLight = false;
            this.tickGrabCount = 0;
            this.removeCount = 0;
            this.tryRipCount = 0;
            short tryRipMax = 300;
            this.tornadoBaseSize = this.getTornadoBaseSize();
            StormObject var10001 = this.storm;
            if (this.storm.stormType == StormObject.TYPE_WATER) {
                this.tornadoBaseSize *= 3;
            }

            this.forceRotate(parWorld);
            Random rand = new Random();
            int spawnYOffset = (int) this.storm.posBaseFormationPos.yCoord;
            // NTNH start: check planetary block grab permission
            if (!parWorld.isRemote && ConfigMisc.Storm_Tornado_grabBlocks
                && weather2.compat.WeatherNTNHHooks.canTornadoGrabBlocks(this.storm)) {
                // NTNH end
                byte yStart = 0;
                int yEnd = (int) this.storm.pos.yCoord;
                byte yInc = 1;
                BiomeGenBase bgb = parWorld.getBiomeGenForCoords(
                    MathHelper.floor_double(this.storm.pos.xCoord),
                    MathHelper.floor_double(this.storm.pos.zCoord));
                if (bgb != null && (double) (bgb.rootHeight + bgb.heightVariation) <= 0.7D) {
                    int k;
                    int tryX;
                    int tryY;
                    int tryZ;
                    double dist;
                    for (k = yStart; k < yEnd; k += yInc) {
                        int randSize = k;
                        tryX = k / 4;
                        if (k <= 20 || rand.nextInt(2) == 0) {
                            if (this.tryRipCount > tryRipMax) {
                                break;
                            }

                            float var10000 = (float) (this.storm.levelCurIntensityStage + 1);
                            var10001 = this.storm;
                            tryY = (int) ((var10000 - StormObject.levelStormIntensityFormingStartVal) * 5.0F);
                            tryZ = 5 + tryX + tryY;
                            if (this.storm.stormType == StormObject.TYPE_WATER) {
                                tryZ = 1 + tryX / 2;
                            }

                            for (int d0 = 0; d0 < tryZ && this.tryRipCount <= tryRipMax; ++d0) {
                                int tryY1 = (int) ((double) (spawnYOffset + randSize) - 1.5D);
                                if (tryY1 > 255) {
                                    tryY1 = 255;
                                }

                                int d2 = (int) this.storm.pos.xCoord + rand.nextInt(this.tornadoBaseSize + tryX)
                                    - (this.tornadoBaseSize / 2 + tryX / 2);
                                int tryZ1 = (int) this.storm.pos.zCoord + rand.nextInt(this.tornadoBaseSize + tryX)
                                    - (this.tornadoBaseSize / 2 + tryX / 2);
                                dist = this.storm.pos.xCoord - (double) d2;
                                double blockID = this.storm.pos.zCoord - (double) tryZ1;
                                double dist1 = (double) MathHelper.sqrt_double(dist * dist + blockID * blockID);
                                if (dist1 < (double) (this.tornadoBaseSize / 2 + tryX / 2)
                                    && this.tryRipCount < tryRipMax) {
                                    Block blockID1 = parWorld.getBlock(d2, tryY1, tryZ1);
                                    boolean performed = false;
                                    if (!CoroUtilBlock.isAir(blockID1)
                                        && this.canGrab(parWorld, d2, tryY1, tryZ1, blockID1)) {
                                        ++this.tryRipCount;
                                        seesLight = this.tryRip(parWorld, d2, tryY1, tryZ1, true);
                                        performed = seesLight;
                                    }

                                    if (!performed && ConfigMisc.Storm_Tornado_RefinedGrabRules
                                        && blockID1 == Blocks.grass) {
                                        parWorld.setBlock(d2, tryY1, tryZ1, Blocks.dirt);
                                    }
                                }
                            }
                        }
                    }

                    for (k = 0; k < 10; ++k) {
                        byte var27 = 40;
                        tryX = (int) this.storm.pos.xCoord + rand.nextInt(var27) - 20;
                        tryY = spawnYOffset - 2 + rand.nextInt(8);
                        tryZ = (int) this.storm.pos.zCoord + rand.nextInt(var27) - 20;
                        double var28 = this.storm.pos.xCoord - (double) tryX;
                        double var29 = this.storm.pos.zCoord - (double) tryZ;
                        dist = (double) MathHelper.sqrt_double(var28 * var28 + var29 * var29);
                        if (dist < (double) (this.tornadoBaseSize / 2 + var27 / 2) && this.tryRipCount < tryRipMax) {
                            Block var30 = parWorld.getBlock(tryX, tryY, tryZ);
                            if (!CoroUtilBlock.isAir(var30) && this.canGrab(parWorld, tryX, tryY, tryZ, var30)) {
                                ++this.tryRipCount;
                                this.tryRip(parWorld, tryX, tryY, tryZ, true);
                            }
                        }
                    }
                }
            } else {
                seesLight = true;
            }

            if (Math.abs((double) spawnYOffset - this.storm.pos.yCoord) > 5.0D) {
                seesLight = true;
            }

        }
    }

    public boolean isNoDigCoord(int x, int y, int z) {
        return false;
    }

    public boolean tryRip(World parWorld, int tryX, int tryY, int tryZ, boolean notify) {
        // NTNH start: block rip permission check
        if (!ConfigMisc.Storm_Tornado_grabBlocks
            || !weather2.compat.WeatherNTNHHooks.canTornadoGrabBlocks(this.storm)) {
            // NTNH end
            return true;
        } else if (this.isNoDigCoord(tryX, tryY, tryZ)) {
            return true;
        } else {
            boolean seesLight;
            if (parWorld.isRemote) {
                seesLight = false;
            }

            seesLight = false;
            Block blockID = parWorld.getBlock(tryX, tryY, tryZ);
            if (parWorld.getHeightValue(tryX, tryZ) - 1 == tryY || parWorld.getHeightValue(tryX + 1, tryZ) - 1 < tryY
                || parWorld.getHeightValue(tryX, tryZ + 1) - 1 < tryY
                || parWorld.getHeightValue(tryX - 1, tryZ) - 1 < tryY
                || parWorld.getHeightValue(tryX, tryZ - 1) - 1 < tryY) {
                if (parWorld.getChunkProvider()
                    .chunkExists((int) this.storm.pos.xCoord / 16, (int) this.storm.pos.zCoord / 16)
                    && this.blockCount <= ConfigMisc.Storm_Tornado_maxBlocksPerStorm
                    && this.lastGrabTime < System.currentTimeMillis()
                    && this.tickGrabCount < ConfigMisc.Storm_Tornado_maxBlocksGrabbedPerTick) {
                    this.lastGrabTime = System.currentTimeMillis() - 5L;
                    if (blockID != Blocks.snow && blockID != Blocks.glass) {
                        if (parWorld.getClosestPlayer(
                            this.storm.posBaseFormationPos.xCoord,
                            this.storm.posBaseFormationPos.yCoord,
                            this.storm.posBaseFormationPos.zCoord,
                            140.0D) != null) {
                            EntityMovingBlock mBlock;
                            if (blockID == Blocks.grass) {
                                mBlock = new EntityMovingBlock(parWorld, tryX, tryY, tryZ, Blocks.dirt, this.storm);
                            } else {
                                mBlock = new EntityMovingBlock(parWorld, tryX, tryY, tryZ, blockID, this.storm);
                            }

                            ++this.blockCount;
                            // NTNH start: track dimension moving blocks count
                            if (!parWorld.isRemote && parWorld.provider != null) {
                                weather2.compat.WeatherNTNHHooks.incrementMovingBlocks(parWorld.provider.dimensionId);
                            }
                            // NTNH end
                            mBlock.setPosition((double) tryX, (double) tryY, (double) tryZ);
                            if (!parWorld.isRemote) {
                                parWorld.spawnEntityInWorld(mBlock);
                            }

                            ++this.tickGrabCount;
                            ++this.ripCount;
                            if (this.ripCount % 10 == 0) {
                                ;
                            }

                            mBlock.type = 0;
                            seesLight = true;
                        }
                    } else if (blockID == Blocks.glass) {
                        parWorld
                            .playSoundEffect((double) tryX, (double) tryY, (double) tryZ, "random.glass", 5.0F, 1.0F);
                    }
                }

                if (WeatherUtil.shouldRemoveBlock(blockID)) {
                    ++this.removeCount;
                    if (notify) {
                        parWorld.setBlock(tryX, tryY, tryZ, Blocks.air, 0, 3);
                    } else {
                        parWorld.setBlock(tryX, tryY, tryZ, Blocks.air, 0, 0);
                    }
                }
            }

            return seesLight;
        }
    }

    public boolean canGrab(World parWorld, int x, int y, int z, Block blockID) {
        if (CoroUtilBlock.isAir(blockID)) return false;
        return weather2.compat.WeatherNTNHHooks.canGrab(parWorld, x, y, z, blockID, this.storm);
    }

    public boolean canGrab(World parWorld, Block blockID) {
        return canGrab(parWorld, 0, 0, 0, blockID);
    }

    public boolean forceRotate(World parWorld) {
        double dist = (double) this.grabDist;
        AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
            this.storm.pos.xCoord,
            (double) this.storm.currentTopYBlock,
            this.storm.pos.zCoord,
            this.storm.pos.xCoord,
            (double) this.storm.currentTopYBlock,
            this.storm.pos.zCoord);
        List list = parWorld
            .getEntitiesWithinAABB(Entity.class, aabb.expand(dist, (double) (this.storm.maxHeight * 3), dist));
        boolean foundEnt = false;
        boolean killCount = false;
        if (list != null) {
            for (int i = 0; i < list.size(); ++i) {
                Entity entity1 = (Entity) list.get(i);
                if (!(entity1 instanceof EntityPlayer) || ConfigMisc.Storm_Tornado_grabPlayer) {
                    if (!(entity1 instanceof EntityPlayer) && ConfigMisc.Storm_Tornado_grabPlayersOnly) {
                        continue;
                    }

                    if (this.getDistanceXZ(this.storm.posBaseFormationPos, entity1.posX, entity1.posY, entity1.posZ)
                        < dist) {
                        if (entity1 instanceof EntityMovingBlock && !((EntityMovingBlock) entity1).collideFalling) {
                            this.storm.spinEntity(entity1);
                            foundEnt = true;
                        } else if (entity1 instanceof EntityPlayer) {
                            if (WeatherUtilEntity.isEntityOutside(entity1)) {
                                this.storm.spinEntity(entity1);
                                foundEnt = true;
                            }
                        } else if (entity1 instanceof EntityLivingBase
                            && WeatherUtilEntity.isEntityOutside(entity1, true)) {
                                this.storm.spinEntity(entity1);
                                foundEnt = true;
                            }
                    }
                }

                if (entity1 instanceof EntityMovingBlock && !entity1.isDead) {
                    int var3 = MathHelper.floor_double(entity1.posX);
                    int var4 = MathHelper.floor_double(entity1.posZ);
                    boolean var12 = true;
                }

                if (entity1 instanceof EntityMovingBlock
                    && this.blockCount + 5 > ConfigMisc.Storm_Tornado_maxBlocksPerStorm
                    && entity1.posY > 255.0D) {
                    entity1.setDead();
                }
            }
        }

        return foundEnt;
    }

    public double getDistanceXZ(Vec3 parVec, double var1, double var3, double var5) {
        double var7 = parVec.xCoord - var1;
        double var11 = parVec.zCoord - var5;
        return (double) MathHelper.sqrt_double(var7 * var7 + var11 * var11);
    }

    public double getDistanceXZ(Entity ent, double var1, double var3, double var5) {
        double var7 = ent.posX - var1;
        double var11 = ent.posZ - var5;
        return (double) MathHelper.sqrt_double(var7 * var7 + var11 * var11);
    }

    @SideOnly(Side.CLIENT)
    public void soundUpdates(boolean playFarSound, boolean playNearSound) {
        Minecraft mc = FMLClientHandler.instance()
            .getClient();
        if (mc.thePlayer != null) {
            short far = 200;
            short close = 120;
            StormObject var10001 = this.storm;
            if (this.storm.stormType == StormObject.TYPE_WATER) {
                close = 200;
            }

            Vec3 plPos = Vec3.createVectorHelper(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
            double distToPlayer = this.storm.posGround.distanceTo(plPos);
            float volScaleFar = (float) (((double) far - distToPlayer) / (double) far);
            float volScaleClose = (float) (((double) close - distToPlayer) / (double) close);
            if (volScaleFar < 0.0F) {
                volScaleFar = 0.0F;
            }

            if (volScaleClose < 0.0F) {
                volScaleClose = 0.0F;
            }

            // NTNH start: audio balance scaling (0.75x)
            volScaleFar = weather2.compat.WeatherNTNHHooks.getAdjustedWindVolume(volScaleFar);
            volScaleClose = weather2.compat.WeatherNTNHHooks.getAdjustedWindVolume(volScaleClose);
            // NTNH end

            if (distToPlayer < (double) close) {
                if (!this.lastTickPlayerClose) {
                    ;
                }

                this.lastTickPlayerClose = true;
            } else {
                this.lastTickPlayerClose = false;
            }

            if (distToPlayer < (double) far) {
                if (playFarSound) {
                    this.tryPlaySound(WeatherUtilSound.snd_wind_far, 2, mc.thePlayer, volScaleFar, (float) far);
                }

                if (playNearSound) {
                    this.tryPlaySound(WeatherUtilSound.snd_wind_close, 1, mc.thePlayer, volScaleClose, (float) close);
                }

                var10001 = this.storm;
                if (this.storm.levelCurIntensityStage >= StormObject.STATE_FORMING) {
                    var10001 = this.storm;
                    if (this.storm.stormType == StormObject.TYPE_LAND) {
                        this.tryPlaySound(
                            WeatherUtilSound.snd_dmg_close,
                            0,
                            mc.thePlayer,
                            volScaleClose,
                            (float) close);
                    }
                }
            }

        }
    }

    public boolean tryPlaySound(String[] sound, int arrIndex, Entity source, float vol, float parCutOffRange) {
        Random rand = new Random();
        if (WeatherUtilSound.soundTimer[arrIndex] <= System.currentTimeMillis()) {
            WeatherUtilSound.playMovingSound(
                this.storm,
                Weather.modID + ":streaming." + sound[WeatherUtilSound.snd_rand[arrIndex]],
                vol,
                1.0F,
                parCutOffRange);
            int length = ((Integer) WeatherUtilSound.soundToLength.get(sound[WeatherUtilSound.snd_rand[arrIndex]]))
                .intValue();
            WeatherUtilSound.soundTimer[arrIndex] = System.currentTimeMillis() + (long) length - 500L;
            WeatherUtilSound.snd_rand[arrIndex] = rand.nextInt(3);
        }

        return false;
    }
}
