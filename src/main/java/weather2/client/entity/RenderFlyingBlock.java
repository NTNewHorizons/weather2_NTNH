package weather2.client.entity;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import weather2.entity.EntityMovingBlock;
import weather2.util.WeatherUtilParticle;

@SideOnly(Side.CLIENT)
public class RenderFlyingBlock extends Render {

    Block renderBlock;

    public RenderFlyingBlock(Block parBlock) {
        this.renderBlock = parBlock;
    }

    protected ResourceLocation getEntityTexture(Entity entity) {
        return TextureMap.locationBlocksTexture;
    }

    public void doRender(Entity var1, double var2, double var4, double var6, float var8, float var9) {
        GL11.glPushMatrix();
        GL11.glDisable(2912);
        int age = var1.ticksExisted * 5;
        float size = 0.3F;
        if (size < 0.0F) {
            size = 0.0F;
        }

        if (var1 instanceof EntityMovingBlock) {
            size = 1.0F;
        }

        GL11.glTranslatef((float) var2, (float) var4, (float) var6);
        this.bindEntityTexture(var1);
        World var11 = var1.worldObj;
        RenderBlocks rb = new RenderBlocks(var1.worldObj);
        GL11.glScalef(size, size, size);
        if (var1 instanceof EntityMovingBlock) {
            Block i = ((EntityMovingBlock) var1).tile;
            GL11.glRotatef(
                (float) ((double) ((float) age * 0.1F) * 180.0D / 12.566370964050293D - 0.0D),
                1.0F,
                0.0F,
                0.0F);
            GL11.glRotatef(
                (float) ((double) ((float) age * 0.1F) * 180.0D / 6.283185307179586D - 0.0D),
                0.0F,
                1.0F,
                0.0F);
            GL11.glRotatef(
                (float) ((double) ((float) age * 0.1F) * 180.0D / 6.283185307179586D - 0.0D),
                0.0F,
                0.0F,
                1.0F);
            rb.setRenderBoundsFromBlock(i);
            rb.renderBlockAsItem(i, 0, 0.8F);
        } else {
            rb.setRenderBoundsFromBlock(this.renderBlock);
            rb.renderBlockAsItem(this.renderBlock, 0, 0.8F);

            for (int var15 = 0; var15 < Math.min(4, WeatherUtilParticle.maxRainDrops); ++var15) {
                GL11.glPushMatrix();
                GL11.glTranslatef(
                    (float) WeatherUtilParticle.rainPositions[var15].xCoord * 3.0F,
                    (float) WeatherUtilParticle.rainPositions[var15].yCoord * 3.0F,
                    (float) WeatherUtilParticle.rainPositions[var15].zCoord * 3.0F);
                GL11.glRotatef(
                    (float) ((double) ((float) age * 0.1F) * 180.0D / 12.566370964050293D - 0.0D),
                    1.0F,
                    0.0F,
                    0.0F);
                GL11.glRotatef(
                    (float) ((double) ((float) age * 0.1F) * 180.0D / 6.283185307179586D - 0.0D),
                    0.0F,
                    1.0F,
                    0.0F);
                GL11.glRotatef(
                    (float) ((double) ((float) age * 0.1F) * 180.0D / 6.283185307179586D - 0.0D),
                    0.0F,
                    0.0F,
                    1.0F);
                rb.setRenderBoundsFromBlock(this.renderBlock);
                rb.renderBlockAsItem(this.renderBlock, 0, 0.8F);
                GL11.glPopMatrix();
            }
        }

        GL11.glEnable(2912);
        GL11.glEnable(2896);
        GL11.glPopMatrix();
    }
}
