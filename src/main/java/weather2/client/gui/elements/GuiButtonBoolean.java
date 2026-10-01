package weather2.client.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiButtonBoolean extends GuiButton {

    boolean boolState;
    String strEnabled;
    String strDisabled;

    public GuiButtonBoolean(int par1, int par2, int par3, String str, String parDisabled) {
        this(par1, par2, par3, 20, 20, str, parDisabled);
    }

    public GuiButtonBoolean(int par1, int par2, int par3, int par4, int par5, String str, String parDisabled) {
        super(par1, par2, par3, par4, par5, str);
        this.boolState = false;
        this.strEnabled = "";
        this.strDisabled = "";
        this.width = 20;
        this.height = 20;
        this.enabled = true;
        this.visible = true;
        this.id = par1;
        this.xPosition = par2;
        this.yPosition = par3;
        this.width = par4;
        this.height = par5;
        this.displayString = str;
        this.strEnabled = str;
        this.strDisabled = parDisabled;
    }

    public void setBooleanToggle() {
        this.setBoolean(!this.getBoolean());
    }

    public boolean getBoolean() {
        return this.boolState;
    }

    public void setBoolean(boolean val) {
        this.boolState = val;
    }

    public int getHoverState(boolean par1) {
        byte var2 = 1;
        if (par1) {
            var2 = 2;
        } else if (!this.enabled) {
            var2 = 0;
        }

        return var2;
    }

    public void drawButton(Minecraft par1Minecraft, int par2, int par3) {
        if (this.visible) {
            FontRenderer fontrenderer = par1Minecraft.fontRenderer;
            par1Minecraft.getTextureManager()
                .bindTexture(buttonTextures);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.field_146123_n = par2 >= this.xPosition && par3 >= this.yPosition
                && par2 < this.xPosition + this.width
                && par3 < this.yPosition + this.height;
            int k = this.getHoverState(this.field_146123_n);
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, 46 + k * 20, this.width / 2, this.height);
            this.drawTexturedModalRect(
                this.xPosition + this.width / 2,
                this.yPosition,
                200 - this.width / 2,
                46 + k * 20,
                this.width / 2,
                this.height);
            this.mouseDragged(par1Minecraft, par2, par3);
            int l = 14737632;
            if (!this.enabled) {
                l = -6250336;
            } else if (this.field_146123_n) {
                l = 16777120;
            }

            String str = "";
            if (this.boolState) {
                str = "§2" + this.strEnabled;
            } else {
                str = "§c" + this.strDisabled;
            }

            this.drawCenteredString(
                fontrenderer,
                str,
                this.xPosition + this.width / 2,
                this.yPosition + (this.height - 8) / 2,
                l);
        }

    }

    protected void mouseDragged(Minecraft par1Minecraft, int par2, int par3) {}

    public void mouseReleased(int par1, int par2) {}

    public boolean mousePressed(Minecraft par1Minecraft, int par2, int par3) {
        return this.visible && par2 >= this.xPosition
            && par3 >= this.yPosition
            && par2 < this.xPosition + this.width
            && par3 < this.yPosition + this.height;
    }
}
