package weather2.entity;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import CoroUtil.util.CoroUtilBlock;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;
import weather2.config.ConfigMisc;
import weather2.util.WeatherUtil;
import weather2.weathersystem.storm.StormObject;

public class EntityMovingBlock extends Entity implements IEntityAdditionalSpawnData {

    public Block tile;
    public static final int falling = 0;
    public static final int grabbed = 1;
    public int mode = 1;
    public static final float slowdown = 0.98F;
    public static final float curvature = 0.05F;
    public int metadata;
    public TileEntity tileentity;
    public Material material;
    public int age = 0;
    public int type;
    public boolean noCollision;
    public boolean collideFalling = false;
    public double vecX;
    public double vecY;
    public double vecZ;
    public double lastPosX;
    public double lastPosZ;
    public StormObject owner;
    public int gravityDelay;

    public EntityMovingBlock(World var1) {
        super(var1);
        this.tile = null;
        this.noCollision = true;
        this.gravityDelay = 60;
    }

    public EntityMovingBlock(World var1, int var2, int var3, int var4, Block var5, StormObject parOwner) {
        super(var1);
        this.type = 0;
        this.noCollision = false;
        this.gravityDelay = 60;
        this.noCollision = true;
        this.tile = var5;
        this.setSize(0.9F, 0.9F);
        this.yOffset = this.height / 2.0F;
        this.setPosition((double) var2 + 0.5D, (double) var3 + 0.5D, (double) var4 + 0.5D);
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.prevPosX = (double) ((float) var2 + 0.5F);
        this.prevPosY = (double) ((float) var3 + 0.5F);
        this.prevPosZ = (double) ((float) var4 + 0.5F);
        this.material = this.tile.getMaterial();
        this.tileentity = var1.getTileEntity(var2, var3, var4);
        this.metadata = var1.getBlockMetadata(var2, var3, var4);
        this.owner = parOwner;
        if (this.tileentity != null) {
            var1.setBlock(var2, var3, var4, Blocks.air, 0, 2);
        }

    }

    public boolean isInRangeToRenderDist(double var1) {
        return true;
    }

    public boolean canTriggerWalking() {
        return false;
    }

    public void entityInit() {}

    public boolean canBePushed() {
        return !this.isDead;
    }

    public boolean canBeCollidedWith() {
        return !this.isDead && !this.noCollision;
    }

    public void onUpdate() {
        if (!this.worldObj.isRemote && this.worldObj.getClosestPlayer(this.posX, 50.0D, this.posZ, 140.0D) == null) {
            this.setDead();
        }

        if (CoroUtilBlock.isAir(this.tile)) {
            this.setDead();
        } else {
            ++this.age;
            if (this.age > this.gravityDelay && this.type == 0) {
                this.mode = 0;
                if (this.tileentity == null && ConfigMisc.Storm_Tornado_rarityOfDisintegrate != -1
                    && this.rand.nextInt((ConfigMisc.Storm_Tornado_rarityOfDisintegrate + 1) * 20) == 0) {
                    this.setDead();
                }

                if (this.tileentity == null && ConfigMisc.Storm_Tornado_rarityOfFirenado != -1
                    && this.rand.nextInt((ConfigMisc.Storm_Tornado_rarityOfFirenado + 1) * 20) == 0) {
                    this.tile = Blocks.fire;
                }
            }

            if (this.type == 0) {
                ++this.vecX;
                ++this.vecY;
                ++this.vecZ;
            }

            if (this.mode == 1) {
                this.fallDistance = 0.0F;
                this.isCollidedHorizontally = false;
            }

            Vec3 var1 = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
            Vec3 var2 = Vec3.createVectorHelper(
                this.posX + this.motionX * 1.3D,
                this.posY + this.motionY * 1.3D,
                this.posZ + this.motionZ * 1.3D);
            MovingObjectPosition var3 = this.worldObj.rayTraceBlocks(var1, var2);
            var2 = Vec3.createVectorHelper(
                this.posX + this.motionX * 1.3D,
                this.posY + this.motionY * 1.3D,
                this.posZ + this.motionZ * 1.3D);
            if (var3 != null) {
                var2 = Vec3.createVectorHelper(var3.hitVec.xCoord, var3.hitVec.yCoord, var3.hitVec.zCoord);
            }

            Entity var4 = null;
            List var5 = null;
            if (this.age > this.gravityDelay / 4) {
                var5 = this.worldObj.getEntitiesWithinAABBExcludingEntity(
                    this,
                    this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ));
            }

            double var6 = 0.0D;

            int var8;
            int var9;
            int var11;
            for (var8 = 0; var5 != null && var8 < var5.size() && var8 < 5; ++var8) {
                Entity var18 = (Entity) var5.get(var8);
                if (!(var18 instanceof EntityMovingBlock) && var18.canBeCollidedWith() && this.canEntityBeSeen(var18)) {
                    if (!(var18 instanceof EntityPlayer) || !((EntityPlayer) var18).capabilities.isCreativeMode) {
                        var18.motionX = this.motionX / 1.5D;
                        var18.motionY = this.motionY / 1.5D;
                        var18.motionZ = this.motionZ / 1.5D;
                    }

                    if (ConfigMisc.Storm_FlyingBlocksHurt && Math
                        .sqrt(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ)
                        > 0.4000000059604645D) {
                        DamageSource var20 = DamageSource.causeThrownDamage(this, this);
                        var20.damageType = "wm.movingblock";
                        var18.attackEntityFrom(var20, 4.0F);
                    }
                }

                if (var18.canBeCollidedWith() && !this.noCollision) {
                    if (var18.canBePushed()) {
                        var18.getDistanceSqToEntity(this);
                        if (this.isBurning()) {
                            var18.setFire(15);
                        }

                        if (this.tile == Blocks.cactus) {
                            var18.attackEntityFrom(DamageSource.causeThrownDamage(this, this), 1.0F);
                        } else if (this.material == Material.lava) {
                            var18.setFire(15);
                        } else {
                            var9 = MathHelper.floor_double(this.posX);
                            var11 = MathHelper.floor_double(this.posY);
                            int var19 = MathHelper.floor_double(this.posZ);
                            this.tile.onEntityCollidedWithBlock(this.worldObj, var9, var11, var19, var18);
                        }
                    }

                    float var201 = 0.3F;
                    AxisAlignedBB var21 = var18.boundingBox.expand((double) var201, (double) var201, (double) var201);
                    MovingObjectPosition var13 = var21.calculateIntercept(var1, var2);
                    if (var13 != null) {
                        double var14 = var1.distanceTo(var13.hitVec);
                        if (var14 < var6 || var6 == 0.0D) {
                            var4 = var18;
                            var6 = var14;
                        }
                    }
                }
            }

            if (var4 != null) {
                var3 = new MovingObjectPosition(var4);
            }

            if (var3 != null && var3.entityHit == null && this.mode == 0) {
                var8 = var3.blockX;
                int var181 = var3.blockY;
                var9 = var3.blockZ;
                if (var3.sideHit == 0) {
                    --var181;
                }

                if (var3.sideHit == 1) {
                    ++var181;
                }

                if (var3.sideHit == 2) {
                    --var9;
                }

                if (var3.sideHit == 3) {
                    ++var9;
                }

                if (var3.sideHit == 4) {
                    --var8;
                }

                if (var3.sideHit == 5) {
                    ++var8;
                }

                if (this.type == 0) {
                    if (var3.sideHit != 0 && !this.collideFalling) {
                        if (!this.collideFalling) {
                            this.collideFalling = true;
                            this.posX = (double) MathHelper.floor_double(this.posX);
                            this.posZ = (double) MathHelper.floor_double(this.posZ);
                            this.setPosition(this.posX, this.posY, this.posZ);
                            this.motionX = 0.0D;
                            this.motionZ = 0.0D;
                        }
                    } else {
                        this.blockify(var8, var181, var9, var3.sideHit);
                    }

                    this.lastPosX = this.posX;
                    this.lastPosZ = this.posZ;
                } else {
                    this.blockify(var8, var181, var9, var3.sideHit);
                }

                return;
            }

            float var17 = 0.98F;
            if (this.type == 1) {
                var17 = (float) ((double) var17 * 0.92D);
                if (this.mode == 0) {
                    this.motionY -= 0.05000000074505806D;
                }
            } else {
                this.motionY -= 0.05000000074505806D;
            }

            this.motionX *= (double) var17;
            this.motionY *= (double) var17;
            this.motionZ *= (double) var17;
            var11 = (int) (this.posX + this.motionX * 5.0D);
            byte var211 = 50;
            int var22 = (int) (this.posZ + this.motionZ * 5.0D);
            if (!this.worldObj.checkChunksExist(var11, var211, var22, var11, var211, var22)) {
                this.setDead();
            }

            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            if (this.mode == 1) {
                this.posX += this.motionX;
                this.posY += this.motionY;
                this.posZ += this.motionZ;
            } else if (this.mode == 0) {
                this.posX += this.motionX;
                this.posY += this.motionY;
                this.posZ += this.motionZ;
            }

            this.setPosition(this.posX, this.posY, this.posZ);
        }

    }

    public boolean canEntityBeSeen(Entity par1Entity) {
        return this.worldObj.rayTraceBlocks(
            Vec3.createVectorHelper(this.posX, this.posY + (double) this.getEyeHeight(), this.posZ),
            Vec3.createVectorHelper(
                par1Entity.posX,
                par1Entity.posY + (double) par1Entity.getEyeHeight(),
                par1Entity.posZ))
            == null;
    }

    private void blockify(int var1, int var2, int var3, int var4) {
        this.setDead();
        Block var5 = this.worldObj.getBlock(var1, var2, var3);
        if (this.tileentity != null || this.type != 0
            || ConfigMisc.Storm_Tornado_rarityOfBreakOnFall > 0
                && this.rand.nextInt(ConfigMisc.Storm_Tornado_rarityOfBreakOnFall + 1) != 0) {
            if (!WeatherUtil.shouldRemoveBlock(var5) && !WeatherUtil.isOceanBlock(var5) && var2 < 255) {
                this.worldObj.setBlock(var1, var2 + 1, var3, this.tile, this.metadata, 3);
            }

            boolean var6 = false;
            if (!WeatherUtil.isOceanBlock(var5)
                && this.worldObj.setBlock(var1, var2, var3, this.tile, this.metadata, 3)) {
                var6 = true;
            }

            if (var6 && this.tileentity != null) {
                this.worldObj.setTileEntity(var1, var2, var3, this.tileentity);
            }
        }

    }

    public boolean attackEntityFrom(Entity var1, int var2) {
        return false;
    }

    protected void writeEntityToNBT(NBTTagCompound var1) {
        var1.setString("Tile", Block.blockRegistry.getNameForObject(this.tile));
        var1.setByte("Metadata", (byte) this.metadata);
        var1.setInteger("blocktype", this.type);
        NBTTagCompound var2 = new NBTTagCompound();
        if (this.tileentity != null) {
            this.tileentity.writeToNBT(var2);
        }

        var1.setTag("TileEntity", var2);
    }

    protected void readEntityFromNBT(NBTTagCompound var1) {
        this.tile = (Block) Block.blockRegistry.getObject(var1.getString("Tile"));
        this.metadata = var1.getByte("Metadata") & 15;
        this.type = var1.getInteger("blocktype");
        this.tileentity = null;
        if (this.tile instanceof BlockContainer) {
            this.tileentity = ((BlockContainer) this.tile).createNewTileEntity(this.worldObj, this.metadata);
            NBTTagCompound var2 = var1.getCompoundTag("TileEntity");
            this.tileentity.readFromNBT(var2);
        }

        if (this.type == 0) {
            this.setDead();
        }

    }

    public float getShadowSize() {
        return 0.0F;
    }

    public boolean isInRangeToRenderVec3D(Vec3 asd) {
        return true;
    }

    public World func_22685_k() {
        return this.worldObj;
    }

    public void setDead() {
        if (!this.isDead && !this.worldObj.isRemote && this.owner != null && this.owner.tornadoHelper != null) {
            --this.owner.tornadoHelper.blockCount;
            if (this.owner.tornadoHelper.blockCount < 0) {
                this.owner.tornadoHelper.blockCount = 0;
            }
            // NTNH start: track dimension moving blocks count
            if (this.worldObj.provider != null) {
                weather2.compat.WeatherNTNHHooks.decrementMovingBlocks(this.worldObj.provider.dimensionId);
            }
            // NTNH end
        }

        this.owner = null;
        super.setDead();
    }

    public void writeSpawnData(ByteBuf data) {
        ByteBufUtils.writeUTF8String(data, Block.blockRegistry.getNameForObject(this.tile));
        data.writeInt(this.metadata);
    }

    public void readSpawnData(ByteBuf data) {
        this.tile = (Block) Block.blockRegistry.getObject(ByteBufUtils.readUTF8String(data));
        this.metadata = data.readInt();
    }
}
