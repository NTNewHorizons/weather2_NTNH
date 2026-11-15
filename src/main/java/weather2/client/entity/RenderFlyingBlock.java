package weather2.client.entity;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import weather2.entity.EntityMovingBlock;

@SideOnly(Side.CLIENT)
public class RenderFlyingBlock extends Render {

    Block renderBlock;
    private final RenderBlocks cachedRenderBlocks = new RenderBlocks();

    public RenderFlyingBlock(Block parBlock) {
        this.renderBlock = parBlock;
    }

    protected ResourceLocation getEntityTexture(Entity entity) {
        return TextureMap.locationBlocksTexture;
    }

    public void doRender(Entity var1, double var2, double var4, double var6, float var8, float var9) {
        GL11.glPushMatrix();
        boolean fogWasEnabled = GL11.glIsEnabled(GL11.GL_FOG);
        boolean lightingWasEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
        if (fogWasEnabled) {
            GL11.glDisable(GL11.GL_FOG);
        }
        int age = var1.ticksExisted * 5;
        float size = 0.3F;
        if (var1 instanceof EntityMovingBlock) {
            size = 1.0F;
        }

        GL11.glTranslatef((float) var2, (float) var4, (float) var6);
        this.bindEntityTexture(var1);
        RenderBlocks rb = this.cachedRenderBlocks;
        rb.blockAccess = var1.worldObj;
        GL11.glScalef(size, size, size);
        if (var1 instanceof EntityMovingBlock) {
            EntityMovingBlock movingBlock = (EntityMovingBlock) var1;
            Block i = movingBlock.tile;
            if (i != null) {
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
                rb.renderBlockAsItem(i, movingBlock.metadata, 0.8F);
            }
        } else if (this.renderBlock != null) {
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
            rb.setRenderBoundsFromBlock(this.renderBlock);
            rb.renderBlockAsItem(this.renderBlock, 0, 0.8F);
        }

        if (fogWasEnabled) {
            GL11.glEnable(GL11.GL_FOG);
        }
        if (lightingWasEnabled) {
            GL11.glEnable(GL11.GL_LIGHTING);
        } else {
            GL11.glDisable(GL11.GL_LIGHTING);
        }
        GL11.glPopMatrix();
    }
}
