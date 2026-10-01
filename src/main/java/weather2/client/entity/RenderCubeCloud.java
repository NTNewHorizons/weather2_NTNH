package weather2.client.entity;

import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import weather2.weathersystem.storm.StormObject;

@SideOnly(Side.CLIENT)
public class RenderCubeCloud extends Render {

    protected ResourceLocation getEntityTexture(Entity entity) {
        return TextureMap.locationBlocksTexture;
    }

    public void doRenderClouds(StormObject parStorm, double var2, double var4, double var6, float var8, float var9) {
        EntityClientPlayerMP entP = FMLClientHandler.instance()
            .getClient().thePlayer;
        if (entP != null) {
            GL11.glPushMatrix();
            boolean age = false;
            float size = 80.0F;
            if (size < 0.0F) {
                size = 0.0F;
            }

            GL11.glTranslatef((float) (var2 - entP.posX), (float) (var4 - entP.posY), (float) (var6 - entP.posZ));
            RenderManager.instance.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
            World var11 = parStorm.manager.getWorld();
            RenderBlocks rb = new RenderBlocks(var11);
            GL11.glScalef(size, size, size);
            Block renderBlock = Blocks.ice;
            rb.setRenderBoundsFromBlock(renderBlock);
            rb.renderBlockAsItem(renderBlock, 0, 0.8F);
            GL11.glDisable(2896);
            GL11.glPopMatrix();
        }
    }

    public void doRender(Entity entity, double d0, double d1, double d2, float f, float f1) {}
}
