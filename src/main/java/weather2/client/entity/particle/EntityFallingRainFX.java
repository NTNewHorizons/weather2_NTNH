package weather2.client.entity.particle;

import java.awt.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.World;

import CoroUtil.api.weather.WindHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import extendedrenderer.particle.entity.EntityRotFX;
import weather2.util.WeatherUtilParticle;

@SideOnly(Side.CLIENT)
public class EntityFallingRainFX extends EntityRotFX implements WindHandler {

    public int age;
    public float brightness;
    public int severityOfRainRate = 2;

    public EntityFallingRainFX(World var1, double var2, double var4, double var6, double var8, double var10,
        double var12, double var14, int colorIndex) {
        super(var1, var2, var4, var6, var8, var10, var12);
        this.motionX = var8 + (double) ((float) (Math.random() * 2.0D - 1.0D) * 0.05F);
        this.motionY = var10 + (double) ((float) (Math.random() * 2.0D - 1.0D) * 0.05F);
        this.motionZ = var12 + (double) ((float) (Math.random() * 2.0D - 1.0D) * 0.05F);
        Color color = null;
        if (colorIndex == 0) {
            this.particleRed = this.particleGreen = this.particleBlue = this.rand.nextFloat() * 0.3F;
        } else if (colorIndex == 1) {
            color = new Color(7951674);
        } else if (colorIndex == 2) {
            color = new Color(14077848);
        } else if (colorIndex == 3) {
            color = new Color(10973);
        } else if (colorIndex == 4) {
            color = new Color(15663103);
        } else if (colorIndex == 5) {
            color = new Color(7951674);
        }

        this.brightness = 2.0F;
        if (colorIndex != 0) {
            this.particleRed = (float) color.getRed() / 255.0F;
            this.particleGreen = (float) color.getGreen() / 255.0F;
            this.particleBlue = (float) color.getBlue() / 255.0F;
        }

        this.particleScale = this.rand.nextFloat() * this.rand.nextFloat() * 6.0F;
        this.particleMaxAge = 18;
        this.particleMaxAge = (int) ((double) ((float) this.particleMaxAge) * var14);
        this.particleGravity = 1.0F;
        this.setParticleTextureIndex(WeatherUtilParticle.effRainID);
        this.noClip = true;
    }

    public void renderParticle(Tessellator var1, float var2, float var3, float var4, float var5, float var6,
        float var7) {
        float framesX = 5.0F;
        float framesY = 1.0F;
        float index = (float) this.getParticleTextureIndex();
        float var8 = index / framesX;
        float var9 = var8 + 1.0F / framesX;
        float var10 = index / framesY;
        float var11 = var10 + 1.0F / framesY;
        float var12 = 0.1F * this.particleScale;
        Minecraft mc = Minecraft.getMinecraft();
        float br = (0.9F + mc.gameSettings.gammaSetting * 0.1F
            - (float) mc.theWorld.calculateSkylightSubtracted(var2) * 0.03F) * mc.theWorld.getSunBrightness(1.0F);
        br = 0.35F * Math.max(0.3F, br) * 2.0F;
        var1.setColorRGBA_F(this.particleRed * br, this.particleGreen * br, this.particleBlue * br, 0.5F);
        int rainDrops = 5 + Math.max(0, this.severityOfRainRate - 1) * 5;

        for (int i = 0; i < Math.min(rainDrops, WeatherUtilParticle.maxRainDrops); ++i) {
            float var13 = (float) (this.prevPosX + (this.posX - this.prevPosX) * (double) var2 - interpPosX);
            float var14 = (float) (this.prevPosY + (this.posY - this.prevPosY) * (double) var2 - interpPosY);
            float var15 = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * (double) var2 - interpPosZ);
            if (i != 0) {
                var13 = (float) ((double) var13 + WeatherUtilParticle.rainPositions[i].xCoord);
                var14 = (float) ((double) var14 + WeatherUtilParticle.rainPositions[i].yCoord);
                var15 = (float) ((double) var15 + WeatherUtilParticle.rainPositions[i].zCoord);
            }

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

    }

    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.lastTickPosX = this.posX;
        this.lastTickPosY = this.posY;
        this.lastTickPosZ = this.posZ;
        if (this.particleAge++ >= this.particleMaxAge || this.onGround
            || this.isInWater()
            || this.posY + this.motionY
                < (double) this.worldObj.getHeightValue((int) Math.floor(this.posX), (int) Math.floor(this.posZ))) {
            this.setDead();
        }

        this.setParticleTextureIndex(WeatherUtilParticle.effRainID);
        this.motionY -= 0.01D * (double) this.particleGravity;
        float var20 = 0.98F;
        this.motionX *= (double) var20;
        this.motionZ *= (double) var20;
        this.moveEntity(this.motionX, this.motionY, this.motionZ);
    }

    public int getFXLayer() {
        return 5;
    }

    public float getWindWeight() {
        return 10.0F;
    }

    public int getParticleDecayExtra() {
        return 0;
    }

    public float maxRenderRange() {
        return 40.0F;
    }
}
