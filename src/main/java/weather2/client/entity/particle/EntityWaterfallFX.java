package weather2.client.entity.particle;

import java.awt.Color;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.World;

import CoroUtil.api.weather.WindHandler;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import extendedrenderer.particle.entity.EntityRotFX;

@SideOnly(Side.CLIENT)
public class EntityWaterfallFX extends EntityRotFX implements WindHandler {

    public int age;
    public float brightness;

    public EntityWaterfallFX(World var1, double var2, double var4, double var6, double var8, double var10, double var12,
        double var14, int var16) {
        super(var1, var2, var4, var6, var8, var10, var12);
        this.motionX = var8 + (double) ((float) (Math.random() * 2.0D - 1.0D) * 0.05F);
        this.motionY = var10 + (double) ((float) (Math.random() * 2.0D - 1.0D) * 0.05F);
        this.motionZ = var12 + (double) ((float) (Math.random() * 2.0D - 1.0D) * 0.05F);
        Color var17 = null;
        if (var16 == 0) {
            this.particleRed = this.particleGreen = this.particleBlue = this.rand.nextFloat() * 0.3F;
        } else if (var16 == 1) {
            var17 = new Color(16732160);
        } else if (var16 == 2) {
            var17 = new Color(255);
        } else if (var16 == 3) {
            var17 = new Color(6711039);
        } else if (var16 == 4) {
            var17 = new Color(16777215);
        } else if (var16 == 5) {
            var17 = new Color(7951674);
        }

        this.brightness = 1.0F;
        if (var17 != null && var16 != 0) {
            this.particleRed = (float) var17.getRed() / 255.0F;
            this.particleGreen = (float) var17.getGreen() / 255.0F;
            this.particleBlue = (float) var17.getBlue() / 255.0F;
        }

        this.particleMaxAge = 18;
        this.particleMaxAge = (int) ((double) ((float) this.particleMaxAge) * var14);
        this.particleGravity = 0.2F;
        this.particleScale = 0.5F;
        this.setParticleTextureIndex(0);
    }

    public void renderParticle(Tessellator var1, float var2, float var3, float var4, float var5, float var6,
        float var7) {
        float var8 = (float) (this.getParticleTextureIndex() % 16) / 16.0F;
        float var9 = var8 + 0.0624375F;
        float var10 = (float) (this.getParticleTextureIndex() / 16) / 16.0F;
        float var11 = var10 + 0.0624375F;
        float var12 = 0.1F * this.particleScale;
        float var13 = (float) (this.prevPosX + (this.posX - this.prevPosX) * (double) var2 - interpPosX);
        float var14 = (float) (this.prevPosY + (this.posY - this.prevPosY) * (double) var2 - interpPosY);
        float var15 = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * (double) var2 - interpPosZ);
        float var16 = this.getBrightness(var2) * this.brightness;
        var16 = 1.0F + FMLClientHandler.instance()
            .getClient().gameSettings.gammaSetting - (float) this.worldObj.calculateSkylightSubtracted(var2) * 0.13F;
        var1.setColorOpaque_F(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16);
        var1.addVertexWithUV(
            (double) (var13 - var3 * var12 - var6 * var12),
            (double) (var14 - var4 * var12),
            (double) (var15 - var5 * var12 - var7 * var12),
            (double) var9,
            (double) var11);
        var1.addVertexWithUV(
            (double) (var13 - var3 * var12 + var6 * var12),
            (double) (var14 + var4 * var12),
            (double) (var15 - var5 * var12 + var7 * var12),
            (double) var9,
            (double) var10);
        var1.addVertexWithUV(
            (double) (var13 + var3 * var12 + var6 * var12),
            (double) (var14 + var4 * var12),
            (double) (var15 + var5 * var12 + var7 * var12),
            (double) var8,
            (double) var10);
        var1.addVertexWithUV(
            (double) (var13 + var3 * var12 - var6 * var12),
            (double) (var14 - var4 * var12),
            (double) (var15 + var5 * var12 - var7 * var12),
            (double) var8,
            (double) var11);
    }

    public int getFXLayer() {
        return 0;
    }

    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        float adj = 0.08F * this.rand.nextFloat();
        if (this.particleRed < 255.0F) {
            this.particleRed += 0.01F;
        }

        if (this.particleGreen < 255.0F) {
            this.particleGreen += 0.01F;
        }

        if (this.particleBlue < 255.0F) {
            this.particleBlue += 0.01F;
        }

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setDead();
        }

        this.setParticleTextureIndex(7 - this.particleAge * 8 / this.particleMaxAge);
        Block id = this.worldObj
            .getBlock((int) Math.floor(this.posX), (int) Math.floor(this.posY), (int) Math.floor(this.posZ));
        int meta = 0;
        if (id.getMaterial() == Material.water) {
            Double var1 = Double.valueOf(
                BlockLiquid.getFlowDirection(
                    this.worldObj,
                    (int) Math.floor(this.posX),
                    (int) Math.floor(this.posY),
                    (int) Math.floor(this.posZ),
                    Material.water));
            float meta2;
            if (var1.doubleValue() != -1000.0D) {
                meta2 = 0.005F;
                this.motionX -= Math.sin(var1.doubleValue()) * (double) meta2;
                this.motionZ += Math.cos(var1.doubleValue()) * (double) meta2;
            }

            meta2 = 0.03F;
            this.motionX += (double) (this.rand.nextFloat() * meta2 - meta2 / 2.0F);
            this.motionZ += (double) (this.rand.nextFloat() * meta2 - meta2 / 2.0F);
            meta = this.worldObj.getBlockMetadata(
                (int) Math.floor(this.posX),
                (int) Math.floor(this.posY),
                (int) Math.floor(this.posZ));
            if ((meta & 8) != 0) {
                this.motionY -= 0.05000000074505806D * (double) this.particleGravity;
            } else {
                this.motionY += (double) (0.05F * this.particleGravity * 0.2F);
            }
        } else {
            this.motionY -= 0.05000000074505806D * (double) this.particleGravity * 1.5D;
            if (this.onGround) {
                this.setDead();
            }
        }

        if (this.motionY > 0.029999999329447746D) {
            this.motionY = 0.029999999329447746D;
        }

        float var7 = 0.98F;
        this.motionX *= (double) var7;
        this.motionY *= (double) var7;
        this.motionZ *= (double) var7;
        this.moveEntity(this.motionX, this.motionY, this.motionZ);
        int var8 = meta;
        if (meta > 9) {
            var8 = 9;
        }

        float height = (float) (10 - var8) * 0.1F;
        if (id.getMaterial() == Material.water && this.motionY > 0.0D
            && this.posY > (double) ((float) ((int) Math.floor(this.posY)) + height)) {
            this.motionY = -0.05000000074505806D;
        }

    }

    public float getWindWeight() {
        return 60.0F;
    }

    public int getParticleDecayExtra() {
        return 0;
    }
}
