package weather2.client.block;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import weather2.Weather;
import weather2.block.TileEntityAnemometer;

public class TileEntityAnemometerRenderer extends TileEntitySpecialRenderer {

    public ModelAnemometer model;
    public ResourceLocation texture;

    public TileEntityAnemometerRenderer() {
        this.texture = new ResourceLocation(Weather.modID + ":textures/blocks/anemometer.png");
        this.model = new ModelAnemometer();
    }

    public void renderTileEntityAt(TileEntity var1, double var2, double var4, double var6, float var8) {
        float renderAngle = ((TileEntityAnemometer) var1).smoothAnglePrev
            + (((TileEntityAnemometer) var1).smoothAngle - ((TileEntityAnemometer) var1).smoothAnglePrev) * var8;
        float scale = 1.0F;
        this.model.scaleX = scale;
        this.model.scaleY = scale;
        this.model.scaleZ = scale;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) var2 + 0.5F, (float) var4 + 0.0F, (float) var6 + 0.5F);
        boolean isTransparent = false;
        boolean isInv = false;
        this.bindTexture(this.texture);
        if (isTransparent) {
            GL11.glEnable(2977);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
        }

        if (isInv) {
            GL11.glTranslatef(0.0F, 1.0F * this.model.scaleY * this.model.scaleItem, 0.0F);
            GL11.glScalef(this.model.scaleItem, this.model.scaleItem, this.model.scaleItem);
            GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
        } else {
            GL11.glTranslatef(0.0F, 1.5F * this.model.scaleY, 0.0F);
        }

        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
        if (!isInv) {
            GL11.glTranslatef(this.model.offsetX, this.model.offsetY, this.model.offsetZ);
        } else {
            GL11.glTranslatef(this.model.offsetInvX, this.model.offsetInvY, 0.0F);
        }

        GL11.glScalef(this.model.scaleX, this.model.scaleY, this.model.scaleZ);
        Tessellator tessellator = Tessellator.instance;
        float br = 0.9F;
        this.model.render(0.0625F, renderAngle);
        GL11.glPopMatrix();
    }
}
