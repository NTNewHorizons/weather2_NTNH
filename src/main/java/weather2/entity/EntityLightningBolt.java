package weather2.entity;

import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityWeatherEffect;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

import CoroUtil.util.CoroUtilBlock;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import weather2.config.ConfigMisc;

public class EntityLightningBolt extends EntityWeatherEffect {

    private int lightningState;
    public long boltVertex;
    private int boltLivingTime;
    public int fireLifeTime;
    public int fireChance;

    public EntityLightningBolt(World par1World, double par2, double par4, double par6) {
        super(par1World);
        this.ignoreFrustumCheck = true;
        this.renderDistanceWeight = 10.0D;
        this.fireLifeTime = ConfigMisc.Lightning_lifetimeOfFire;
        this.fireChance = ConfigMisc.Lightning_OddsTo1OfFire;
        this.setLocationAndAngles(par2, par4, par6, 0.0F, 0.0F);
        this.lightningState = 2;
        this.boltVertex = this.rand.nextLong();
        this.boltLivingTime = this.rand.nextInt(2) + 1;
        Random rand = new Random();
        if (!par1World.isRemote && (this.fireChance == 0 || rand.nextInt(this.fireChance) == 0)
            && par1World.getGameRules()
                .getGameRuleBooleanValue("doFireTick")
            && (par1World.difficultySetting == EnumDifficulty.NORMAL
                || par1World.difficultySetting == EnumDifficulty.HARD)
            && par1World.doChunksNearChunkExist(
                MathHelper.floor_double(par2),
                MathHelper.floor_double(par4),
                MathHelper.floor_double(par6),
                10)) {
            int i = MathHelper.floor_double(par2);
            int j = MathHelper.floor_double(par4);
            int k = MathHelper.floor_double(par6);
            if (CoroUtilBlock.isAir(par1World.getBlock(i, j, k)) && Blocks.fire.canPlaceBlockAt(par1World, i, j, k)) {
                par1World.setBlock(i, j, k, Blocks.fire, this.fireLifeTime, 3);
            }

            for (i = 0; i < 4; ++i) {
                j = MathHelper.floor_double(par2) + this.rand.nextInt(3) - 1;
                k = MathHelper.floor_double(par4) + this.rand.nextInt(3) - 1;
                int l = MathHelper.floor_double(par6) + this.rand.nextInt(3) - 1;
                if (CoroUtilBlock.isAir(par1World.getBlock(j, k, l))
                    && Blocks.fire.canPlaceBlockAt(par1World, j, k, l)) {
                    par1World.setBlock(j, k, l, Blocks.fire, this.fireLifeTime, 3);
                }
            }
        }

    }

    public void onUpdate() {
        super.onUpdate();
        if (this.worldObj.isRemote && this.lightningState == 2) {
            // NTNH start: audio comfort calibration (~70%)
            this.worldObj.playSound(
                this.posX,
                this.posY,
                this.posZ,
                "ambient.weather.thunder",
                45.0F,
                0.8F + this.rand.nextFloat() * 0.2F,
                false);
            this.worldObj.playSound(
                this.posX,
                this.posY,
                this.posZ,
                "random.explode",
                1.4F,
                0.5F + this.rand.nextFloat() * 0.2F,
                false);
            // NTNH end
        }

        --this.lightningState;
        if (this.lightningState < 0) {
            if (this.boltLivingTime == 0) {
                this.setDead();
            } else if (this.lightningState < -this.rand.nextInt(10)) {
                --this.boltLivingTime;
                this.lightningState = 1;
                this.boltVertex = this.rand.nextLong();
                if (!this.worldObj.isRemote && this.rand.nextInt(this.fireChance) == 0
                    && this.worldObj.getGameRules()
                        .getGameRuleBooleanValue("doFireTick")
                    && this.worldObj.doChunksNearChunkExist(
                        MathHelper.floor_double(this.posX),
                        MathHelper.floor_double(this.posY),
                        MathHelper.floor_double(this.posZ),
                        10)) {
                    int d0 = MathHelper.floor_double(this.posX);
                    int j = MathHelper.floor_double(this.posY);
                    int list = MathHelper.floor_double(this.posZ);
                    if (CoroUtilBlock.isAir(this.worldObj.getBlock(d0, j, list))
                        && Blocks.fire.canPlaceBlockAt(this.worldObj, d0, j, list)) {
                        this.worldObj.setBlock(d0, j, list, Blocks.fire, this.fireLifeTime, 3);
                    }
                }
            }
        }

        if (this.lightningState >= 0) {
            if (this.worldObj.isRemote) {
                this.updateFlashEffect();
            } else {
                double var6 = 3.0D;
                List var7 = this.worldObj.getEntitiesWithinAABBExcludingEntity(
                    this,
                    AxisAlignedBB.getBoundingBox(
                        this.posX - var6,
                        this.posY - var6,
                        this.posZ - var6,
                        this.posX + var6,
                        this.posY + 6.0D + var6,
                        this.posZ + var6));

                for (int l = 0; l < var7.size(); ++l) {
                    Entity var5 = (Entity) var7.get(l);
                }
            }
        }

    }

    @SideOnly(Side.CLIENT)
    public void updateFlashEffect() {
        Minecraft mc = FMLClientHandler.instance()
            .getClient();
        if (mc.thePlayer != null && mc.thePlayer.getDistanceToEntity(this) < 180.0F) {
            this.worldObj.lastLightningBolt = 2;
        }

    }

    protected void entityInit() {}

    protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {}

    protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {}

    // NTNH start: override 1.7.10 render range checks (replaces dead 1.6.4 isInRangeToRenderVec3D)
    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRender3d(double x, double y, double z) {
        double dx = this.posX - x;
        double dy = this.posY - y;
        double dz = this.posZ - z;
        return this.isInRangeToRenderDist(dx * dx + dy * dy + dz * dz);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distanceSq) {
        return this.lightningState >= 0 && distanceSq < 512.0D * 512.0D;
    }
    // NTNH end

    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderVec3D(Vec3 par1Vec3) {
        return this.lightningState >= 0;
    }
}
