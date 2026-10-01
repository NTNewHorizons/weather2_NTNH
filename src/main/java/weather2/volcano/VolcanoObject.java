package weather2.volcano;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import CoroUtil.util.CoroUtilBlock;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import extendedrenderer.ExtendedRenderer;
import extendedrenderer.particle.ParticleRegistry;
import extendedrenderer.particle.behavior.ParticleBehaviors;
import extendedrenderer.particle.entity.EntityRotFX;
import weather2.Weather;
import weather2.util.WeatherUtil;
import weather2.weathersystem.WeatherManagerBase;

public class VolcanoObject {

    public static long lastUsedID = 0L;
    public long ID;
    public WeatherManagerBase manager;
    @SideOnly(Side.CLIENT)
    public List listParticlesSmoke = new ArrayList();
    @SideOnly(Side.CLIENT)
    public ParticleBehaviors particleBehaviors;
    public int sizeMaxParticles = 300;
    public static int staticYPos = 200;
    public Vec3 pos;
    public int processRateDelay;
    public Block topBlockID;
    public int startYPos;
    public int curRadius;
    public int curHeight;
    public int state;
    public int size;
    public int maxSize;
    public int step;
    public int stepsBuildupMax;
    public int ticksToErupt;
    public int ticksPerformedErupt;
    public int ticksToCooldown;
    public int ticksPerformedCooldown;
    public int growthStage;

    public VolcanoObject(WeatherManagerBase parManager) {
        this.pos = Vec3.createVectorHelper(0.0D, (double) staticYPos, 0.0D);
        this.processRateDelay = 20;
        this.topBlockID = Blocks.air;
        this.startYPos = -1;
        this.curRadius = 5;
        this.curHeight = 3;
        this.state = 0;
        this.size = 0;
        this.maxSize = 20;
        this.step = 0;
        this.stepsBuildupMax = 20;
        this.ticksToErupt = 600;
        this.ticksPerformedErupt = 0;
        this.ticksToCooldown = 600;
        this.ticksPerformedCooldown = 0;
        this.growthStage = 0;
        this.manager = parManager;
    }

    public void initFirstTime() {
        this.ID = (long) (lastUsedID++);
    }

    public void initPost() {}

    public void resetEruption() {
        this.step = 0;
        this.ticksPerformedErupt = 0;
        this.ticksPerformedCooldown = 0;
        this.state = 2;
        this.ticksPerformedErupt = 0;
        this.ticksPerformedCooldown = 0;
    }

    public void readFromNBT(NBTTagCompound data) {
        this.ID = data.getLong("ID");
        this.pos = Vec3.createVectorHelper(
            (double) data.getInteger("posX"),
            (double) data.getInteger("posY"),
            (double) data.getInteger("posZ"));
        this.size = data.getInteger("size");
        this.maxSize = data.getInteger("maxSize");
        this.state = data.getInteger("state");
        this.curRadius = data.getInteger("curRadius");
        this.curHeight = data.getInteger("curHeight");
        this.topBlockID = (Block) Block.blockRegistry.getObject(data.getString("topBlockID"));
        this.startYPos = data.getInteger("startYPos");
        this.step = data.getInteger("step");
        this.ticksPerformedErupt = data.getInteger("ticksPerformedErupt");
        this.ticksPerformedCooldown = data.getInteger("ticksPerformedCooldown");
    }

    public void writeToNBT(NBTTagCompound data) {
        data.setLong("ID", this.ID);
        data.setInteger("posX", (int) this.pos.xCoord);
        data.setInteger("posY", (int) this.pos.yCoord);
        data.setInteger("posZ", (int) this.pos.zCoord);
        data.setInteger("size", this.size);
        data.setInteger("maxSize", this.maxSize);
        data.setInteger("state", this.state);
        data.setInteger("curRadius", this.curRadius);
        data.setInteger("curHeight", this.curHeight);
        data.setString("topBlockID", Block.blockRegistry.getNameForObject(this.topBlockID));
        data.setInteger("startYPos", this.startYPos);
        data.setInteger("step", this.step);
        data.setInteger("ticksPerformedErupt", this.ticksPerformedErupt);
        data.setInteger("ticksPerformedCooldown", this.ticksPerformedCooldown);
    }

    public void nbtSyncFromServer(NBTTagCompound parNBT) {
        this.ID = parNBT.getLong("ID");
        Weather.dbg("VolcanoObject " + this.ID + " receiving sync");
        this.pos = Vec3.createVectorHelper(
            (double) parNBT.getInteger("posX"),
            (double) parNBT.getInteger("posY"),
            (double) parNBT.getInteger("posZ"));
        this.size = parNBT.getInteger("size");
        this.maxSize = parNBT.getInteger("maxSize");
        this.state = parNBT.getInteger("state");
    }

    public NBTTagCompound nbtSyncForClient() {
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger("posX", (int) this.pos.xCoord);
        data.setInteger("posY", (int) this.pos.yCoord);
        data.setInteger("posZ", (int) this.pos.zCoord);
        data.setLong("ID", this.ID);
        data.setInteger("size", this.size);
        data.setInteger("maxSize", this.maxSize);
        data.setInteger("state", this.state);
        return data;
    }

    public void tick() {
        this.processRateDelay = 10;
        Side side = FMLCommonHandler.instance()
            .getEffectiveSide();
        if (side == Side.CLIENT) {
            if (!WeatherUtil.isPaused()) {
                this.tickClient();
            }
        } else {
            World world = this.manager.getWorld();
            float res = 5.0F;
            int posX;
            int posY;
            double posZ;
            double blockID1;
            double angle;
            Vec3 vec;
            int posX1;
            int posZ1;
            Block blockID2;
            if (this.state == 0) {
                this.pos.xCoord = Math.floor(this.pos.xCoord);
                this.pos.zCoord = Math.floor(this.pos.zCoord);
                this.pos.yCoord = (double) world.getHeightValue((int) this.pos.xCoord, (int) this.pos.zCoord);
                this.startYPos = (int) this.pos.yCoord;
                this.topBlockID = world.getBlock(
                    MathHelper.floor_double(this.pos.xCoord),
                    MathHelper.floor_double(this.pos.yCoord - 1.0D),
                    MathHelper.floor_double(this.pos.zCoord));
                if (CoroUtilBlock.isAir(this.topBlockID) || !this.topBlockID
                    .isBlockSolid(world, (int) this.pos.xCoord, (int) this.pos.yCoord - 1, (int) this.pos.zCoord, 0)) {
                    this.topBlockID = world
                        .getBlock((int) this.pos.xCoord, (int) this.pos.yCoord - 1, (int) this.pos.zCoord);
                }

                for (posX = this.startYPos + this.curHeight; posX > 2; --posX) {
                    for (posY = 0; posY <= this.curRadius; ++posY) {
                        posZ = (double) posY;
                        blockID1 = 0.0D;
                        if (posX > this.startYPos) {
                            posZ = (double) (posY + (this.startYPos - posX));
                        }

                        for (angle = 0.0D; angle <= 360.0D; angle += (double) res) {
                            vec = Vec3.createVectorHelper(posZ, 0.0D, blockID1);
                            vec.rotateAroundY((float) angle);
                            posX1 = (int) Math.floor(this.pos.xCoord + vec.xCoord + 0.5D);
                            posZ1 = (int) Math.floor(this.pos.zCoord + vec.zCoord + 0.5D);
                            blockID2 = Blocks.obsidian;
                            if (posX >= this.startYPos) {
                                blockID2 = this.topBlockID;
                            } else if (posY < this.curRadius) {
                                blockID2 = Blocks.lava;
                            }

                            if (posX != this.startYPos + this.curHeight) {
                                Block rand = world.getBlock(posX1, posX, posZ1);
                                if (CoroUtilBlock.isAir(rand) || rand.getMaterial() == Material.water) {
                                    world.setBlock(posX1, posX, posZ1, blockID2);
                                }
                            }
                        }
                    }
                }

                ++this.state;
                System.out.println("initial volcano created");
            } else if (this.state == 1) {
                if (this.manager.getWorld()
                    .getTotalWorldTime() % (long) this.processRateDelay == 0L) {
                    ++this.size;
                    ++this.curHeight;
                    ++this.curRadius;
                    if (this.size >= this.maxSize) {
                        ++this.state;
                    }

                    res = 1.0F;

                    for (posX = 0; posX <= this.curHeight; ++posX) {
                        posY = Math.max(0, this.curRadius - posX - 2);
                        posZ = (double) posY;
                        blockID1 = 0.0D;

                        for (angle = 0.0D; angle <= 360.0D; angle += (double) res) {
                            vec = Vec3.createVectorHelper(posZ, 0.0D, blockID1);
                            vec.rotateAroundY((float) angle);
                            posX1 = (int) Math.floor(this.pos.xCoord + vec.xCoord + 0.5D);
                            posZ1 = (int) Math.floor(this.pos.zCoord + vec.zCoord + 0.5D);
                            blockID2 = this.topBlockID;
                            Random var22 = new Random();
                            if (var22.nextInt(4) == 0) {
                                if (posX != this.curHeight
                                    && CoroUtilBlock.isAir(world.getBlock(posX1, this.startYPos + posX, posZ1))) {
                                    world.setBlock(posX1, this.startYPos + posX, posZ1, blockID2);
                                }

                                int underY = this.startYPos + posX - 1;

                                for (Block underBlockID = world
                                    .getBlock(posX1, underY, posZ1); (CoroUtilBlock.isAir(underBlockID)
                                        || underBlockID.getMaterial() == Material.water)
                                        && underY > 1; underBlockID = world.getBlock(posX1, underY, posZ1)) {
                                    world.setBlock(posX1, underY, posZ1, Blocks.dirt);
                                    --underY;
                                }
                            }
                        }
                    }

                    System.out.println("cur size: " + this.size + " - " + this.curHeight + " - " + this.curRadius);
                }
            } else {
                int var19;
                if (this.state == 2) {
                    if (this.manager.getWorld()
                        .getTotalWorldTime() % (long) this.processRateDelay == 0L) {
                        if (this.step <= this.maxSize) {
                            posX = (int) Math.floor(this.pos.xCoord);
                            posY = (int) Math.floor((double) this.startYPos) + this.step;
                            var19 = (int) Math.floor(this.pos.zCoord);
                            world.setBlock(posX, posY, var19, Blocks.lava);
                            world.setBlock(posX + 1, posY, var19, Blocks.lava);
                            world.setBlock(posX - 1, posY, var19, Blocks.lava);
                            world.setBlock(posX, posY, var19 + 1, Blocks.lava);
                            world.setBlock(posX, posY, var19 - 1, Blocks.lava);
                        } else {
                            this.step = 0;
                            ++this.state;
                        }

                        ++this.step;
                    }
                } else if (this.state == 3) {
                    if (this.manager.getWorld()
                        .getTotalWorldTime() % (long) this.processRateDelay == 0L) {
                        ++this.step;
                        if (this.step > this.stepsBuildupMax) {
                            this.step = 0;
                            ++this.state;
                        }
                    }
                } else if (this.state == 4) {
                    if (this.ticksPerformedErupt == 0) {
                        Weather.dbg("volcano " + this.ID + " is erupting");

                        for (posX = 0; posX < 3; ++posX) {
                            posY = (int) Math.floor(this.pos.xCoord);
                            var19 = (int) Math.floor((double) this.startYPos) + this.maxSize + posX;
                            int blockID = (int) Math.floor(this.pos.zCoord);
                            Block var21 = Blocks.lava;
                            world.setBlock(posY, var19, blockID, var21);
                            world.setBlock(posY + 1, var19, blockID, var21);
                            world.setBlock(posY - 1, var19, blockID, var21);
                            world.setBlock(posY, var19, blockID + 1, var21);
                            world.setBlock(posY, var19, blockID - 1, var21);
                            world.setBlock(posY + 1, var19, blockID + 1, var21);
                            world.setBlock(posY - 1, var19, blockID - 1, var21);
                            world.setBlock(posY - 1, var19, blockID + 1, var21);
                            world.setBlock(posY + 1, var19, blockID - 1, var21);
                        }
                    }

                    ++this.ticksPerformedErupt;
                    if (this.ticksPerformedErupt > this.ticksToErupt) {
                        ++this.state;
                    }
                } else if (this.state == 5) {
                    if (this.ticksPerformedCooldown == 0) {
                        Weather.dbg("volcano " + this.ID + " is cooling");
                    }

                    if (this.ticksPerformedCooldown % this.processRateDelay == 0) {
                        posX = (int) Math.floor(this.pos.xCoord);
                        posY = (int) Math.floor((double) this.startYPos) + this.maxSize - this.step + 2;
                        var19 = (int) Math.floor(this.pos.zCoord);
                        Block var20 = Blocks.stone;
                        world.setBlock(posX, posY, var19, var20);
                        world.setBlock(posX + 1, posY, var19, var20);
                        world.setBlock(posX - 1, posY, var19, var20);
                        world.setBlock(posX, posY, var19 + 1, var20);
                        world.setBlock(posX, posY, var19 - 1, var20);
                        world.setBlock(posX + 1, posY, var19 + 1, var20);
                        world.setBlock(posX - 1, posY, var19 - 1, var20);
                        world.setBlock(posX - 1, posY, var19 + 1, var20);
                        world.setBlock(posX + 1, posY, var19 - 1, var20);
                        ++this.step;
                    }

                    ++this.ticksPerformedCooldown;
                    if (this.ticksPerformedCooldown > this.ticksToCooldown) {
                        ++this.state;
                    }
                } else if (this.state == 6) {
                    Weather.dbg("volcano " + this.ID + " has reset!");
                    this.resetEruption();
                }
            }
        }

    }

    @SideOnly(Side.CLIENT)
    public void tickClient() {
        if (this.particleBehaviors == null) {
            this.particleBehaviors = new ParticleBehaviors(
                Vec3.createVectorHelper(this.pos.xCoord, this.pos.yCoord, this.pos.zCoord));
        } else if (!Minecraft.getMinecraft()
            .isSingleplayer() || !(Minecraft.getMinecraft().currentScreen instanceof GuiIngameMenu)) {
                this.particleBehaviors.tickUpdateList();
            }

        byte delay = 1;
        byte loopSize = 1;
        Random rand = new Random();
        int i;
        if (this.manager.getWorld()
            .getTotalWorldTime() % (long) delay == 0L) {
            for (i = 0; i < loopSize; ++i) {
                if (this.listParticlesSmoke.size() < 500) {
                    double ent = (double) (this.size / 48);
                    EntityRotFX particle = this.spawnSmokeParticle(
                        this.pos.xCoord + rand.nextDouble() * ent - rand.nextDouble() * ent,
                        this.pos.yCoord + (double) this.size + 2.0D,
                        this.pos.zCoord + rand.nextDouble() * ent - rand.nextDouble() * ent);
                    this.listParticlesSmoke.add(particle);
                }
            }
        }

        boolean var19 = true;
        boolean var20 = true;

        for (i = 0; i < this.listParticlesSmoke.size(); ++i) {
            EntityRotFX var21 = (EntityRotFX) this.listParticlesSmoke.get(i);
            if (var21.isDead) {
                this.listParticlesSmoke.remove(var21);
            } else {
                double speed = 0.4D + rand.nextDouble() * 1.0D * 0.01D;
                double distt = 300.0D;
                double curDist = var21.getDistance(this.pos.xCoord, (double) staticYPos, this.pos.zCoord);
                double vecX = var21.posX - this.pos.xCoord;
                double vecZ = var21.posZ - this.pos.zCoord;
                float angle = (float) (Math.atan2(vecZ, vecX) * 180.0D / 3.141592653589793D);
                angle += 50.0F;
                angle = (float) ((double) angle - (double) (var21.getEntityId() % 10) * 3.0D);
                angle += (float) (rand.nextInt(10) - rand.nextInt(10));
                if (curDist > distt) {
                    angle += 20.0F;
                }

                double var17 = Math.sqrt(
                    var21.motionX * var21.motionX + var21.motionY * var21.motionY + var21.motionZ * var21.motionZ);
            }
        }

    }

    @SideOnly(Side.CLIENT)
    public EntityRotFX spawnSmokeParticle(double x, double y, double z) {
        double speed = 0.0D;
        Random rand = new Random();
        EntityRotFX entityfx = this.particleBehaviors.spawnNewParticleIconFX(
            Minecraft.getMinecraft().theWorld,
            ParticleRegistry.cloud256,
            x,
            y,
            z,
            (rand.nextDouble() - rand.nextDouble()) * speed,
            0.0D,
            (rand.nextDouble() - rand.nextDouble()) * speed);
        this.particleBehaviors.initParticle(entityfx);
        ParticleBehaviors var10000 = this.particleBehaviors;
        ParticleBehaviors.setParticleRandoms(entityfx, true, true);
        var10000 = this.particleBehaviors;
        ParticleBehaviors.setParticleFire(entityfx);
        entityfx.noClip = true;
        entityfx.callUpdatePB = false;
        entityfx.setMaxAge(400 + rand.nextInt(200));
        entityfx.setScale(50.0F);
        float randFloat = rand.nextFloat() * 0.6F;
        float baseBright = 0.1F;
        float finalBright = Math.min(1.0F, baseBright + randFloat);
        entityfx.setRBGColorF(finalBright, finalBright, finalBright);
        ExtendedRenderer.rotEffRenderer.addEffect(entityfx);
        this.particleBehaviors.particles.add(entityfx);
        return entityfx;
    }

    public void reset() {
        this.setDead();
    }

    public void setDead() {
        Weather.dbg("volcano... killed? NO ONE KILLS A VOLCANO!");
    }

}
