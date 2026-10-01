package weather2.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import CoroUtil.packet.PacketHelper;
import modconfig.gui.GuiConfigEditor;
import weather2.Weather;
import weather2.client.gui.elements.GuiButtonBoolean;
import weather2.client.gui.elements.GuiButtonCycle;
import weather2.util.WeatherUtilConfig;

public class GuiEZConfig extends GuiScreen {

    public int xCenter;
    public int yCenter;
    public int xStart;
    public int yStart;
    public ResourceLocation resGUI;
    public String guiCur;
    public HashMap buttonsLookup;
    public NBTTagCompound nbtSendCache;
    public static int CMD_CLOSE = 0;
    public static int CMD_ADVANCED = 99;
    public static int CMD_SUBGUI_PERFORMANCE = 40;
    public static int CMD_SUBGUI_COMPATIBILITY = 41;
    public static int CMD_SUBGUI_PREFERENCE = 42;
    public static int CMD_SUBGUI_DIMENSIONS = 43;
    public static int CMD_BUTTON_DIMENSIONS_PREV = 44;
    public static int CMD_BUTTON_DIMENSIONS_NEXT = 45;
    public static String GUI_SUBGUI_PERFORMANCE = "Performance";
    public static String GUI_SUBGUI_COMPATIBILITY = "Compatibility";
    public static String GUI_SUBGUI_PREFERENCE = "Preference";
    public static String GUI_SUBGUI_DIMENSIONS = "Dimensions";
    protected int xSize;
    protected int ySize;
    public boolean canPlayerChangeServerSettings;
    public int curDimListPage;
    public int curDimListCountPerPage;
    public int optionsPerDim;
    public int btnDimIndexStart;
    public List listDimIDs;
    public List listDimNames;
    public List listSettingWeather;
    public List listSettingClouds;
    public List listSettingStorms;
    public List listSettingEffects;

    public GuiEZConfig() {
        this.resGUI = new ResourceLocation(Weather.modID + ":textures/gui/gui512.png");
        this.guiCur = GUI_SUBGUI_PERFORMANCE;
        this.buttonsLookup = new HashMap();
        this.nbtSendCache = new NBTTagCompound();
        this.xSize = 176;
        this.ySize = 166;
        this.canPlayerChangeServerSettings = false;
        this.curDimListPage = 0;
        this.curDimListCountPerPage = 4;
        this.optionsPerDim = 4;
        this.btnDimIndexStart = 50;
        this.listDimIDs = new ArrayList();
        this.listDimNames = new ArrayList();
        this.listSettingWeather = new ArrayList();
        this.listSettingClouds = new ArrayList();
        this.listSettingStorms = new ArrayList();
        this.listSettingEffects = new ArrayList();
        Weather.dbg("EZGUI constructor");
        if (MinecraftServer.getServer() != null && MinecraftServer.getServer()
            .isSinglePlayer()) {
            this.canPlayerChangeServerSettings = true;
        }

        NBTTagCompound data = new NBTTagCompound();
        data.setString("command", "syncRequest");
        data.setString("packetCommand", "EZGuiData");
        Weather.eventChannel.sendToServer(PacketHelper.getNBTPacket(data, Weather.eventChannelName));
        this.nbtSendCache.setTag("guiData", new NBTTagCompound());
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    public void onGuiClosed() {
        super.onGuiClosed();
    }

    public void addButton(GuiButton btn) {
        this.buttonsLookup.put(Integer.valueOf(btn.id), btn);
        this.buttonList.add(btn);
    }

    public void resetGuiElements() {
        this.buttonList.clear();
        this.buttonsLookup.clear();
    }

    public void drawBackground(int par1) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager()
            .bindTexture(this.resGUI);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, 512, 512);
        byte yEleSize = 24;
        byte yEleSize2 = 44;
        this.drawString(
            this.fontRendererObj,
            "Weather2 EZ GUI Configuration" + (this.guiCur.equals("main") ? "" : " - GUI Tab: " + this.guiCur),
            this.xStart + 7,
            this.yStart - 9,
            16777215);
        int yStart2 = this.yStart + 34;
        short xOP = 260;
        String op = "For OP/Singleplayer";
        this.drawString(
            this.fontRendererObj,
            "--------------------------------------------------------",
            this.xStart + 7,
            yStart2 - 3 - 4,
            16777215);
        if (this.guiCur.equals(GUI_SUBGUI_PERFORMANCE)) {
            this.drawString(this.fontRendererObj, "Cloud/Storm effects", this.xStart + 7, yStart2 + 8, 16777215);
            this.drawString(
                this.fontRendererObj,
                "Nature effects",
                this.xStart + 7,
                yStart2 + 8 + yEleSize * 1,
                16777215);
            this.drawString(
                this.fontRendererObj,
                "Particle precipitation rate",
                this.xStart + 7,
                yStart2 + 8 + yEleSize * 2,
                16777215);
        } else if (this.guiCur.equals(GUI_SUBGUI_COMPATIBILITY)) {
            this.drawString(this.fontRendererObj, "Storms when", this.xStart + 7, yStart2 + 8, 16777215);
            this.drawString(
                this.fontRendererObj,
                "Lock vanilla weather",
                this.xStart + 7,
                yStart2 + 8 + yEleSize * 1,
                16777215);
            this.drawString(
                this.fontRendererObj,
                "Particle precipitation",
                this.xStart + 7,
                yStart2 + 8 + yEleSize * 2,
                16777215);
            this.drawString(
                this.fontRendererObj,
                "Extra snowfall blocks",
                this.xStart + 7,
                yStart2 + 8 + yEleSize * 3,
                16777215);
            this.drawString(
                this.fontRendererObj,
                "Wind only for vanilla particles",
                this.xStart + 7,
                yStart2 + 8 + yEleSize * 4,
                16777215);
            this.drawString(this.fontRendererObj, op, this.xStart + xOP, yStart2 + 8, 16777215);
            this.drawString(this.fontRendererObj, op, this.xStart + xOP, yStart2 + 8 + yEleSize * 1, 16777215);
            this.drawString(this.fontRendererObj, op, this.xStart + xOP, yStart2 + 8 + yEleSize * 3, 16777215);
        } else if (this.guiCur.equals(GUI_SUBGUI_PREFERENCE)) {
            this.drawString(
                this.fontRendererObj,
                "Rate of storms per each player",
                this.xStart + 7,
                yStart2 + 8,
                16777215);
            this.drawString(
                this.fontRendererObj,
                "Chance of storms",
                this.xStart + 7,
                yStart2 + 8 + yEleSize * 1,
                16777215);
            this.drawString(
                this.fontRendererObj,
                "Chance of rain",
                this.xStart + 7,
                yStart2 + 8 + yEleSize * 2,
                16777215);
            this.drawString(
                this.fontRendererObj,
                "Block destruction",
                this.xStart + 7,
                yStart2 + 8 + yEleSize * 3,
                16777215);
            this.drawString(
                this.fontRendererObj,
                "Tornados and Cyclones",
                this.xStart + 7,
                yStart2 + 8 + yEleSize * 4,
                16777215);
            this.drawString(this.fontRendererObj, op, this.xStart + xOP, yStart2 + 8, 16777215);
            this.drawString(this.fontRendererObj, op, this.xStart + xOP, yStart2 + 8 + yEleSize * 1, 16777215);
            this.drawString(this.fontRendererObj, op, this.xStart + xOP, yStart2 + 8 + yEleSize * 2, 16777215);
            this.drawString(this.fontRendererObj, op, this.xStart + xOP, yStart2 + 8 + yEleSize * 3, 16777215);
            this.drawString(this.fontRendererObj, op, this.xStart + xOP, yStart2 + 8 + yEleSize * 4, 16777215);
        } else if (this.guiCur.equals(GUI_SUBGUI_DIMENSIONS)) {
            try {
                for (int ex = 0; ex < this.curDimListCountPerPage; ++ex) {
                    if (this.curDimListPage * this.curDimListCountPerPage + ex < this.listDimIDs.size()) {
                        this.drawString(
                            this.fontRendererObj,
                            "§6" + (String) this.listDimNames
                                .get(this.curDimListPage * this.curDimListCountPerPage + ex),
                            this.xStart + 7,
                            yStart2 + 8 + yEleSize2 * ex,
                            16777215);
                        this.drawString(
                            this.fontRendererObj,
                            "Weather: ",
                            this.xStart + 7,
                            yStart2 + 28 + yEleSize2 * ex,
                            16777215);
                        this.drawString(
                            this.fontRendererObj,
                            "Clouds: ",
                            this.xStart + 100,
                            yStart2 + 28 + yEleSize2 * ex,
                            16777215);
                        this.drawString(
                            this.fontRendererObj,
                            "Storms: ",
                            this.xStart + 186,
                            yStart2 + 28 + yEleSize2 * ex,
                            16777215);
                        this.drawString(
                            this.fontRendererObj,
                            "Effects: ",
                            this.xStart + 270,
                            yStart2 + 28 + yEleSize2 * ex,
                            16777215);
                    }
                }

                this.drawString(
                    this.fontRendererObj,
                    "" + (this.curDimListPage + 1) + "/" + (this.listDimNames.size() / this.curDimListCountPerPage + 1),
                    this.xStart + 80,
                    yStart2 + 194,
                    16777215);
            } catch (Exception var10) {
                var10.printStackTrace();
            }
        }

    }

    public void drawScreen(int par1, int par2, float par3) {
        this.drawBackground(0);
        super.drawScreen(par1, par2, par3);
    }

    protected void keyTyped(char par1, int par2) {
        if (par2 == 1) {
            ;
        }

        super.keyTyped(par1, par2);
    }

    protected void mouseClicked(int par1, int par2, int par3) {
        super.mouseClicked(par1, par2, par3);
    }

    public void updateScreen() {
        super.updateScreen();
        if (WeatherUtilConfig.nbtClientCache.getBoolean("markUpdated")) {
            Weather.dbg("EZGUI client markUpdated detected");
            WeatherUtilConfig.nbtClientCache.setBoolean("markUpdated", false);
            this.updateGuiElements();
        }

        if (this.guiCur.equals(GUI_SUBGUI_COMPATIBILITY)) {
            if (((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_COMP_STORM)))
                .getIndex() == 1) {
                ((GuiButtonCycle) this.buttonsLookup
                    .get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_COMP_LOCK))).enabled = false;
                ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_COMP_LOCK)))
                    .setIndex(2);
            } else {
                ((GuiButtonCycle) this.buttonsLookup
                    .get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_COMP_LOCK))).enabled = true;
            }
        } else if (this.guiCur.equals(GUI_SUBGUI_DIMENSIONS)) {
            try {
                for (int ex = 0; ex < this.curDimListCountPerPage; ++ex) {
                    boolean show = false;
                    int startIndex = this.btnDimIndexStart + this.curDimListCountPerPage * ex;
                    if (this.curDimListPage * this.curDimListCountPerPage + ex < this.listDimIDs.size()) {
                        show = true;
                    }

                    if (show) {
                        boolean enabled = true;
                        if (((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(startIndex + 0))).getIndex()
                            == 0) {
                            enabled = false;
                        }

                        ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(startIndex + 1))).enabled = enabled;
                        ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(startIndex + 2))).enabled = enabled;
                        ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(startIndex + 3))).enabled = enabled;
                    }
                }
            } catch (Exception var5) {
                var5.printStackTrace();
            }
        }

    }

    public void updateGuiElements() {
        Weather.dbg("updateGuiElements");
        this.canPlayerChangeServerSettings = WeatherUtilConfig.nbtClientCache.getBoolean("isPlayerOP");
        NBTTagCompound serverDataCache = WeatherUtilConfig.nbtClientCache.getCompoundTag("data");
        NBTTagCompound dimData = WeatherUtilConfig.nbtClientCache.getCompoundTag("dimListing");
        this.listDimIDs.clear();
        this.listDimNames.clear();
        this.listSettingWeather.clear();
        this.listSettingStorms.clear();
        this.listSettingClouds.clear();
        this.listSettingEffects.clear();
        Iterator it = dimData.func_150296_c()
            .iterator();

        while (it.hasNext()) {
            String ex = (String) it.next();
            NBTTagCompound show = dimData.getCompoundTag(ex);
            this.listDimIDs.add(Integer.valueOf(show.getInteger("ID")));
            this.listDimNames.add(show.getString("name"));
            this.listSettingWeather.add(Boolean.valueOf(show.getBoolean("weather")));
            this.listSettingStorms.add(Boolean.valueOf(show.getBoolean("storms")));
            this.listSettingClouds.add(Boolean.valueOf(show.getBoolean("clouds")));
            this.listSettingEffects.add(
                Boolean.valueOf(
                    WeatherUtilConfig.listDimensionsWindEffects.contains(Integer.valueOf(show.getInteger("ID")))));
        }

        if (this.guiCur.equals(GUI_SUBGUI_PERFORMANCE)) {
            if (WeatherUtilConfig.nbtClientData.hasKey("btn_" + WeatherUtilConfig.CMD_BTN_PERF_STORM)) {
                ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_PERF_STORM)))
                    .setIndex(
                        WeatherUtilConfig.nbtClientData.getInteger("btn_" + WeatherUtilConfig.CMD_BTN_PERF_STORM));
            }

            if (WeatherUtilConfig.nbtClientData.hasKey("btn_" + WeatherUtilConfig.CMD_BTN_PERF_NATURE)) {
                ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_PERF_NATURE)))
                    .setIndex(
                        WeatherUtilConfig.nbtClientData.getInteger("btn_" + WeatherUtilConfig.CMD_BTN_PERF_NATURE));
            }

            if (WeatherUtilConfig.nbtClientData.hasKey("btn_" + WeatherUtilConfig.CMD_BTN_PERF_PRECIPRATE)) {
                ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_PERF_PRECIPRATE)))
                    .setIndex(
                        WeatherUtilConfig.nbtClientData.getInteger("btn_" + WeatherUtilConfig.CMD_BTN_PERF_PRECIPRATE));
            }
        } else if (this.guiCur.equals(GUI_SUBGUI_COMPATIBILITY)) {
            if (serverDataCache.hasKey("btn_" + WeatherUtilConfig.CMD_BTN_COMP_STORM)) {
                ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_COMP_STORM)))
                    .setIndex(serverDataCache.getInteger("btn_" + WeatherUtilConfig.CMD_BTN_COMP_STORM));
            }

            if (serverDataCache.hasKey("btn_" + WeatherUtilConfig.CMD_BTN_COMP_LOCK)) {
                ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_COMP_LOCK)))
                    .setIndex(serverDataCache.getInteger("btn_" + WeatherUtilConfig.CMD_BTN_COMP_LOCK));
            }

            if (WeatherUtilConfig.nbtClientData.hasKey("btn_" + WeatherUtilConfig.CMD_BTN_COMP_PARTICLEPRECIP)) {
                ((GuiButtonCycle) this.buttonsLookup
                    .get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_COMP_PARTICLEPRECIP))).setIndex(
                        WeatherUtilConfig.nbtClientData
                            .getInteger("btn_" + WeatherUtilConfig.CMD_BTN_COMP_PARTICLEPRECIP));
            }

            if (serverDataCache.hasKey("btn_" + WeatherUtilConfig.CMD_BTN_COMP_SNOWFALLBLOCKS)) {
                ((GuiButtonCycle) this.buttonsLookup
                    .get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_COMP_SNOWFALLBLOCKS)))
                        .setIndex(serverDataCache.getInteger("btn_" + WeatherUtilConfig.CMD_BTN_COMP_SNOWFALLBLOCKS));
            }

            if (WeatherUtilConfig.nbtClientData.hasKey("btn_" + WeatherUtilConfig.CMD_BTN_COMP_PARTICLESNOMODS)) {
                ((GuiButtonCycle) this.buttonsLookup
                    .get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_COMP_PARTICLESNOMODS))).setIndex(
                        WeatherUtilConfig.nbtClientData
                            .getInteger("btn_" + WeatherUtilConfig.CMD_BTN_COMP_PARTICLESNOMODS));
            }

            ((GuiButtonCycle) this.buttonsLookup.get(
                Integer.valueOf(WeatherUtilConfig.CMD_BTN_COMP_STORM))).enabled = this.canPlayerChangeServerSettings;
            ((GuiButtonCycle) this.buttonsLookup.get(
                Integer.valueOf(WeatherUtilConfig.CMD_BTN_COMP_LOCK))).enabled = this.canPlayerChangeServerSettings;
            ((GuiButtonCycle) this.buttonsLookup.get(
                Integer.valueOf(
                    WeatherUtilConfig.CMD_BTN_COMP_SNOWFALLBLOCKS))).enabled = this.canPlayerChangeServerSettings;
        } else if (this.guiCur.equals(GUI_SUBGUI_PREFERENCE)) {
            if (serverDataCache.hasKey("btn_" + WeatherUtilConfig.CMD_BTN_PREF_RATEOFSTORM)) {
                ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_PREF_RATEOFSTORM)))
                    .setIndex(serverDataCache.getInteger("btn_" + WeatherUtilConfig.CMD_BTN_PREF_RATEOFSTORM));
            }

            if (serverDataCache.hasKey("btn_" + WeatherUtilConfig.CMD_BTN_PREF_CHANCEOFSTORM)) {
                ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_PREF_CHANCEOFSTORM)))
                    .setIndex(serverDataCache.getInteger("btn_" + WeatherUtilConfig.CMD_BTN_PREF_CHANCEOFSTORM));
            }

            if (serverDataCache.hasKey("btn_" + WeatherUtilConfig.CMD_BTN_PREF_CHANCEOFRAIN)) {
                ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_PREF_CHANCEOFRAIN)))
                    .setIndex(serverDataCache.getInteger("btn_" + WeatherUtilConfig.CMD_BTN_PREF_CHANCEOFRAIN));
            }

            if (serverDataCache.hasKey("btn_" + WeatherUtilConfig.CMD_BTN_PREF_BLOCKDESTRUCTION)) {
                ((GuiButtonCycle) this.buttonsLookup
                    .get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_PREF_BLOCKDESTRUCTION)))
                        .setIndex(serverDataCache.getInteger("btn_" + WeatherUtilConfig.CMD_BTN_PREF_BLOCKDESTRUCTION));
            }

            if (serverDataCache.hasKey("btn_" + WeatherUtilConfig.CMD_BTN_PREF_TORNADOANDCYCLONES)) {
                ((GuiButtonCycle) this.buttonsLookup
                    .get(Integer.valueOf(WeatherUtilConfig.CMD_BTN_PREF_TORNADOANDCYCLONES))).setIndex(
                        serverDataCache.getInteger("btn_" + WeatherUtilConfig.CMD_BTN_PREF_TORNADOANDCYCLONES));
            }

            ((GuiButtonCycle) this.buttonsLookup.get(
                Integer
                    .valueOf(WeatherUtilConfig.CMD_BTN_PREF_RATEOFSTORM))).enabled = this.canPlayerChangeServerSettings;
            ((GuiButtonCycle) this.buttonsLookup.get(
                Integer.valueOf(
                    WeatherUtilConfig.CMD_BTN_PREF_CHANCEOFSTORM))).enabled = this.canPlayerChangeServerSettings;
            ((GuiButtonCycle) this.buttonsLookup.get(
                Integer.valueOf(
                    WeatherUtilConfig.CMD_BTN_PREF_CHANCEOFRAIN))).enabled = this.canPlayerChangeServerSettings;
            ((GuiButtonCycle) this.buttonsLookup.get(
                Integer.valueOf(
                    WeatherUtilConfig.CMD_BTN_PREF_BLOCKDESTRUCTION))).enabled = this.canPlayerChangeServerSettings;
            ((GuiButtonCycle) this.buttonsLookup.get(
                Integer.valueOf(
                    WeatherUtilConfig.CMD_BTN_PREF_TORNADOANDCYCLONES))).enabled = this.canPlayerChangeServerSettings;
        } else if (this.guiCur.equals(GUI_SUBGUI_DIMENSIONS)) {
            try {
                for (int var8 = 0; var8 < this.curDimListCountPerPage; ++var8) {
                    boolean var9 = false;
                    int startIndex = this.btnDimIndexStart + this.curDimListCountPerPage * var8;
                    if (this.curDimListPage * this.curDimListCountPerPage + var8 < this.listDimIDs.size()) {
                        var9 = true;
                        ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(startIndex + 0))).setIndex(
                            ((Boolean) this.listSettingWeather
                                .get(this.curDimListPage * this.curDimListCountPerPage + var8)).booleanValue() ? 1 : 0);
                        ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(startIndex + 1))).setIndex(
                            ((Boolean) this.listSettingClouds
                                .get(this.curDimListPage * this.curDimListCountPerPage + var8)).booleanValue() ? 1 : 0);
                        ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(startIndex + 2))).setIndex(
                            ((Boolean) this.listSettingStorms
                                .get(this.curDimListPage * this.curDimListCountPerPage + var8)).booleanValue() ? 1 : 0);
                        ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(startIndex + 3))).setIndex(
                            ((Boolean) this.listSettingEffects
                                .get(this.curDimListPage * this.curDimListCountPerPage + var8)).booleanValue() ? 1 : 0);
                    }

                    Weather.dbg("page: " + this.curDimListPage + " - index: " + startIndex);
                    ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(startIndex + 0))).visible = var9;
                    ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(startIndex + 1))).visible = var9;
                    ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(startIndex + 2))).visible = var9;
                    ((GuiButtonCycle) this.buttonsLookup.get(Integer.valueOf(startIndex + 3))).visible = var9;
                }
            } catch (Exception var7) {
                var7.printStackTrace();
            }
        }

    }

    public void initGui() {
        super.initGui();
        this.resetGuiElements();
        this.xSize = 372;
        this.ySize = 250;
        ScaledResolution var8 = new ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
        int scaledWidth = var8.getScaledWidth();
        int scaledHeight = var8.getScaledHeight();
        this.xCenter = scaledWidth / 2;
        this.yCenter = scaledHeight / 2;
        this.xStart = this.xCenter - this.xSize / 2;
        this.yStart = this.yCenter - this.ySize / 2;
        byte guiPadding = 8;
        int xStartPadded = this.xStart + guiPadding - 1;
        int yStartPadded = this.yStart + guiPadding - 1;
        byte btnWidth = 80;
        byte btnWidthAndPadding = 84;
        boolean btnWidthBool = true;
        byte btnHeight = 20;
        byte btnHeightAndPadding = 24;
        boolean padding = true;
        boolean btnSpacing = true;
        byte yEleSize2 = 44;
        int xStartPadded2 = xStartPadded + 168;
        int yStartPadded2 = yStartPadded + 30;
        int yStartPadded3 = yStartPadded2 + 20;
        int btnDimIndex = this.btnDimIndexStart;
        this.addButton(
            new GuiButton(
                CMD_CLOSE,
                this.xStart + this.xSize - guiPadding - btnWidth,
                this.yStart + this.ySize - guiPadding - btnHeight,
                btnWidth,
                btnHeight,
                "Save & Close"));
        this.addButton(
            new GuiButton(
                CMD_ADVANCED,
                this.xStart + this.xSize - guiPadding - btnWidth - btnWidthAndPadding,
                this.yStart + this.ySize - guiPadding - btnHeight,
                btnWidth,
                btnHeight,
                "Advanced"));
        this.addButton(
            new GuiButton(
                CMD_SUBGUI_PERFORMANCE,
                xStartPadded + btnWidthAndPadding * 0,
                yStartPadded,
                btnWidth,
                btnHeight,
                (this.guiCur.equals(GUI_SUBGUI_PERFORMANCE) ? "§2" : "") + GUI_SUBGUI_PERFORMANCE));
        this.addButton(
            new GuiButton(
                CMD_SUBGUI_COMPATIBILITY,
                xStartPadded + btnWidthAndPadding * 1,
                yStartPadded,
                btnWidth,
                btnHeight,
                (this.guiCur.equals(GUI_SUBGUI_COMPATIBILITY) ? "§2" : "") + GUI_SUBGUI_COMPATIBILITY));
        this.addButton(
            new GuiButton(
                CMD_SUBGUI_PREFERENCE,
                xStartPadded + btnWidthAndPadding * 2,
                yStartPadded,
                btnWidth,
                btnHeight,
                (this.guiCur.equals(GUI_SUBGUI_PREFERENCE) ? "§2" : "") + GUI_SUBGUI_PREFERENCE));
        this.addButton(
            new GuiButton(
                CMD_SUBGUI_DIMENSIONS,
                xStartPadded + btnWidthAndPadding * 3,
                yStartPadded,
                btnWidth,
                btnHeight,
                (this.guiCur.equals(GUI_SUBGUI_DIMENSIONS) ? "§2" : "") + GUI_SUBGUI_DIMENSIONS));
        if (this.guiCur.equals(GUI_SUBGUI_PERFORMANCE)) {
            this.addButton(
                new GuiButtonCycle(
                    WeatherUtilConfig.CMD_BTN_PERF_STORM,
                    xStartPadded2 + btnWidthAndPadding * 0,
                    yStartPadded2,
                    btnWidth,
                    btnHeight,
                    WeatherUtilConfig.LIST_RATES,
                    0));
            this.addButton(
                new GuiButtonCycle(
                    WeatherUtilConfig.CMD_BTN_PERF_NATURE,
                    xStartPadded2 + btnWidthAndPadding * 0,
                    yStartPadded2 + btnHeightAndPadding * 1,
                    btnWidth,
                    btnHeight,
                    WeatherUtilConfig.LIST_RATES2,
                    0));
            this.addButton(
                new GuiButtonCycle(
                    WeatherUtilConfig.CMD_BTN_PERF_PRECIPRATE,
                    xStartPadded2 + btnWidthAndPadding * 0,
                    yStartPadded2 + btnHeightAndPadding * 2,
                    btnWidth,
                    btnHeight,
                    WeatherUtilConfig.LIST_RATES2,
                    0));
        } else if (this.guiCur.equals(GUI_SUBGUI_COMPATIBILITY)) {
            this.addButton(
                new GuiButtonCycle(
                    WeatherUtilConfig.CMD_BTN_COMP_STORM,
                    xStartPadded2 + btnWidthAndPadding * 0,
                    yStartPadded2,
                    btnWidth,
                    btnHeight,
                    WeatherUtilConfig.LIST_STORMSWHEN,
                    0));
            this.addButton(
                new GuiButtonCycle(
                    WeatherUtilConfig.CMD_BTN_COMP_LOCK,
                    xStartPadded2 + btnWidthAndPadding * 0,
                    yStartPadded2 + btnHeightAndPadding * 1,
                    btnWidth,
                    btnHeight,
                    WeatherUtilConfig.LIST_LOCK,
                    0));
            this.addButton(
                new GuiButtonCycle(
                    WeatherUtilConfig.CMD_BTN_COMP_PARTICLEPRECIP,
                    xStartPadded2 + btnWidthAndPadding * 0,
                    yStartPadded2 + btnHeightAndPadding * 2,
                    btnWidth,
                    btnHeight,
                    WeatherUtilConfig.LIST_TOGGLE,
                    1));
            this.addButton(
                new GuiButtonCycle(
                    WeatherUtilConfig.CMD_BTN_COMP_SNOWFALLBLOCKS,
                    xStartPadded2 + btnWidthAndPadding * 0,
                    yStartPadded2 + btnHeightAndPadding * 3,
                    btnWidth,
                    btnHeight,
                    WeatherUtilConfig.LIST_TOGGLE,
                    0));
            this.addButton(
                new GuiButtonCycle(
                    WeatherUtilConfig.CMD_BTN_COMP_PARTICLESNOMODS,
                    xStartPadded2 + btnWidthAndPadding * 0,
                    yStartPadded2 + btnHeightAndPadding * 4,
                    btnWidth,
                    btnHeight,
                    WeatherUtilConfig.LIST_TOGGLE,
                    0));
        } else if (this.guiCur.equals(GUI_SUBGUI_PREFERENCE)) {
            this.addButton(
                new GuiButtonCycle(
                    WeatherUtilConfig.CMD_BTN_PREF_RATEOFSTORM,
                    xStartPadded2 + btnWidthAndPadding * 0,
                    yStartPadded2,
                    btnWidth,
                    btnHeight,
                    WeatherUtilConfig.LIST_CHANCE,
                    1));
            this.addButton(
                new GuiButtonCycle(
                    WeatherUtilConfig.CMD_BTN_PREF_CHANCEOFSTORM,
                    xStartPadded2 + btnWidthAndPadding * 0,
                    yStartPadded2 + btnHeightAndPadding * 1,
                    btnWidth,
                    btnHeight,
                    WeatherUtilConfig.LIST_RATES,
                    0));
            this.addButton(
                new GuiButtonCycle(
                    WeatherUtilConfig.CMD_BTN_PREF_CHANCEOFRAIN,
                    xStartPadded2 + btnWidthAndPadding * 0,
                    yStartPadded2 + btnHeightAndPadding * 2,
                    btnWidth,
                    btnHeight,
                    WeatherUtilConfig.LIST_RATES2,
                    0));
            this.addButton(
                new GuiButtonCycle(
                    WeatherUtilConfig.CMD_BTN_PREF_BLOCKDESTRUCTION,
                    xStartPadded2 + btnWidthAndPadding * 0,
                    yStartPadded2 + btnHeightAndPadding * 3,
                    btnWidth,
                    btnHeight,
                    WeatherUtilConfig.LIST_TOGGLE,
                    1));
            this.addButton(
                new GuiButtonCycle(
                    WeatherUtilConfig.CMD_BTN_PREF_TORNADOANDCYCLONES,
                    xStartPadded2 + btnWidthAndPadding * 0,
                    yStartPadded2 + btnHeightAndPadding * 4,
                    btnWidth,
                    btnHeight,
                    WeatherUtilConfig.LIST_TOGGLE,
                    1));
        } else if (this.guiCur.equals(GUI_SUBGUI_DIMENSIONS)) {
            this.addButton(
                new GuiButton(
                    CMD_BUTTON_DIMENSIONS_PREV,
                    xStartPadded,
                    this.yStart + this.ySize - guiPadding - btnHeight,
                    60,
                    20,
                    "Prev Page"));
            this.addButton(
                new GuiButton(
                    CMD_BUTTON_DIMENSIONS_NEXT,
                    xStartPadded + 20 + btnWidthAndPadding * 1,
                    this.yStart + this.ySize - guiPadding - btnHeight,
                    60,
                    20,
                    "Next Page"));

            for (int i = 0; i < this.curDimListCountPerPage; ++i) {
                this.addButton(
                    new GuiButtonCycle(
                        btnDimIndex++,
                        xStartPadded + 46,
                        yStartPadded3 + yEleSize2 * i,
                        40,
                        20,
                        WeatherUtilConfig.LIST_TOGGLE,
                        1));
                this.addButton(
                    new GuiButtonCycle(
                        btnDimIndex++,
                        xStartPadded + 132,
                        yStartPadded3 + yEleSize2 * i,
                        40,
                        20,
                        WeatherUtilConfig.LIST_TOGGLE,
                        1));
                this.addButton(
                    new GuiButtonCycle(
                        btnDimIndex++,
                        xStartPadded + 218,
                        yStartPadded3 + yEleSize2 * i,
                        40,
                        20,
                        WeatherUtilConfig.LIST_TOGGLE,
                        1));
                this.addButton(
                    new GuiButtonCycle(
                        btnDimIndex++,
                        xStartPadded + 306,
                        yStartPadded3 + yEleSize2 * i,
                        40,
                        20,
                        WeatherUtilConfig.LIST_TOGGLE,
                        1));
            }
        }

        if (this.guiCur.equals("main")) {
            ;
        }

        this.updateGuiElements();
    }

    protected void actionPerformed(GuiButton var1) {
        String guiForPacket = this.guiCur;
        boolean sendPacket = false;
        if (WeatherUtilConfig.listSettingsServer.contains(Integer.valueOf(var1.id))) {
            if (var1 instanceof GuiButtonBoolean) {
                ((GuiButtonBoolean) var1).setBooleanToggle();
                this.nbtSendCache.getCompoundTag("guiData")
                    .setInteger("btn_" + var1.id, ((GuiButtonBoolean) var1).getBoolean() ? 1 : 0);
            }

            if (var1 instanceof GuiButtonCycle) {
                ((GuiButtonCycle) var1).cycleIndex();
                this.nbtSendCache.getCompoundTag("guiData")
                    .setInteger("btn_" + var1.id, ((GuiButtonCycle) var1).getIndex());
            }
        } else if (WeatherUtilConfig.listSettingsClient.contains(Integer.valueOf(var1.id))) {
            if (var1 instanceof GuiButtonCycle) {
                ((GuiButtonCycle) var1).cycleIndex();
                WeatherUtilConfig.nbtClientData.setInteger("btn_" + var1.id, ((GuiButtonCycle) var1).getIndex());
            }
        } else if (var1.id != CMD_SUBGUI_PERFORMANCE && var1.id != CMD_SUBGUI_COMPATIBILITY
            && var1.id != CMD_SUBGUI_PREFERENCE
            && var1.id != CMD_SUBGUI_DIMENSIONS) {
                if (var1.id == CMD_BUTTON_DIMENSIONS_PREV) {
                    --this.curDimListPage;
                    if (this.curDimListPage < 0) {
                        this.curDimListPage = 0;
                    }

                    this.initGui();
                } else if (var1.id == CMD_BUTTON_DIMENSIONS_NEXT) {
                    ++this.curDimListPage;
                    if (this.curDimListPage > this.listDimIDs.size() / this.curDimListCountPerPage) {
                        --this.curDimListPage;
                    }

                    this.initGui();
                } else if (var1.id == CMD_CLOSE) {
                    sendPacket = true;
                    this.mc.thePlayer.closeScreen();
                    WeatherUtilConfig.processNBTToModConfigClient();
                } else if (var1.id == CMD_ADVANCED) {
                    Minecraft.getMinecraft()
                        .displayGuiScreen(new GuiConfigEditor());
                } else {
                    try {
                        NBTTagCompound val;
                        if ((var1.id - this.btnDimIndexStart) % 4 == 3) {
                            ((GuiButtonCycle) var1).cycleIndex();
                            val = WeatherUtilConfig.nbtClientData.getCompoundTag("dimData");
                            val.setInteger(
                                "dim_"
                                    + this.listDimIDs.get(
                                        this.curDimListPage * this.curDimListCountPerPage
                                            + (var1.id - this.btnDimIndexStart) / this.curDimListCountPerPage)
                                    + "_"
                                    + (var1.id - this.btnDimIndexStart) % 4,
                                ((GuiButtonCycle) var1).getIndex());
                            WeatherUtilConfig.nbtClientData.setTag("dimData", val);
                            Weather.dbg("nbtClientData: " + WeatherUtilConfig.nbtClientData);
                        } else {
                            Weather.dbg(
                                (var1.id - this.btnDimIndexStart) % 4 + " - "
                                    + this.listDimIDs.get(
                                        this.curDimListPage * this.curDimListCountPerPage
                                            + (var1.id - this.btnDimIndexStart) / this.curDimListCountPerPage));
                            ((GuiButtonCycle) var1).cycleIndex();
                            val = this.nbtSendCache.getCompoundTag("guiData")
                                .getCompoundTag("dimData");
                            val.setInteger(
                                "dim_"
                                    + this.listDimIDs.get(
                                        this.curDimListPage * this.curDimListCountPerPage
                                            + (var1.id - this.btnDimIndexStart) / this.curDimListCountPerPage)
                                    + "_"
                                    + (var1.id - this.btnDimIndexStart) % 4,
                                ((GuiButtonCycle) var1).getIndex());
                            this.nbtSendCache.getCompoundTag("guiData")
                                .setTag("dimData", val);
                            Weather.dbg("nbtSendCache: " + this.nbtSendCache);
                        }
                    } catch (Exception var5) {
                        var5.printStackTrace();
                    }
                }
            } else {
                if (var1.id == CMD_SUBGUI_PERFORMANCE) {
                    this.guiCur = GUI_SUBGUI_PERFORMANCE;
                } else if (var1.id == CMD_SUBGUI_COMPATIBILITY) {
                    this.guiCur = GUI_SUBGUI_COMPATIBILITY;
                } else if (var1.id == CMD_SUBGUI_PREFERENCE) {
                    this.guiCur = GUI_SUBGUI_PREFERENCE;
                } else if (var1.id == CMD_SUBGUI_DIMENSIONS) {
                    this.guiCur = GUI_SUBGUI_DIMENSIONS;
                }

                this.initGui();
            }

        if (sendPacket) {
            boolean var6 = false;
            this.nbtSendCache.setString("command", "applySettings");
            this.nbtSendCache.setString("packetCommand", "EZGuiData");
            Weather.eventChannel.sendToServer(PacketHelper.getNBTPacket(this.nbtSendCache, Weather.eventChannelName));
        }

    }

    public int sanitize(int val) {
        return this.sanitize(val, 0, 9999);
    }

    public int sanitize(int val, int min, int max) {
        if (val > max) {
            val = max;
        }

        if (val < min) {
            val = min;
        }

        return val;
    }

    public void drawTexturedModalRect(int par1, int par2, int par3, int par4, int par5, int par6) {
        float f = 0.001953125F;
        float f1 = 0.001953125F;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(
            (double) (par1 + 0),
            (double) (par2 + par6),
            (double) this.zLevel,
            (double) ((float) (par3 + 0) * f),
            (double) ((float) (par4 + par6) * f1));
        tessellator.addVertexWithUV(
            (double) (par1 + par5),
            (double) (par2 + par6),
            (double) this.zLevel,
            (double) ((float) (par3 + par5) * f),
            (double) ((float) (par4 + par6) * f1));
        tessellator.addVertexWithUV(
            (double) (par1 + par5),
            (double) (par2 + 0),
            (double) this.zLevel,
            (double) ((float) (par3 + par5) * f),
            (double) ((float) (par4 + 0) * f1));
        tessellator.addVertexWithUV(
            (double) (par1 + 0),
            (double) (par2 + 0),
            (double) this.zLevel,
            (double) ((float) (par3 + 0) * f),
            (double) ((float) (par4 + 0) * f1));
        tessellator.draw();
    }

}
