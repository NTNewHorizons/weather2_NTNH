package weather2.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import org.apache.commons.lang3.StringUtils;

import CoroUtil.util.CoroUtilFile;
import modconfig.ConfigMod;
import modconfig.ModConfigData;
import weather2.Weather;
import weather2.config.ConfigMisc;

public class WeatherUtilConfig {

    public static List listDimensionsWeather = new ArrayList();
    public static List listDimensionsClouds = new ArrayList();
    public static List listDimensionsStorms = new ArrayList();
    public static List listDimensionsWindEffects = new ArrayList();
    public static int CMD_BTN_PERF_STORM = 2;
    public static int CMD_BTN_PERF_NATURE = 3;
    public static int CMD_BTN_PERF_PRECIPRATE = 12;
    public static int CMD_BTN_COMP_STORM = 4;
    public static int CMD_BTN_COMP_LOCK = 5;
    public static int CMD_BTN_COMP_PARTICLEPRECIP = 6;
    public static int CMD_BTN_COMP_SNOWFALLBLOCKS = 7;
    public static int CMD_BTN_COMP_LEAFFALLBLOCKS = 8;
    public static int CMD_BTN_COMP_PARTICLESNOMODS = 13;
    public static int CMD_BTN_PREF_RATEOFSTORM = 9;
    public static int CMD_BTN_PREF_CHANCEOFSTORM = 14;
    public static int CMD_BTN_PREF_CHANCEOFRAIN = 10;
    public static int CMD_BTN_PREF_BLOCKDESTRUCTION = 11;
    public static int CMD_BTN_PREF_TORNADOANDCYCLONES = 15;
    public static int CMD_BTN_HIGHEST_ID = 15;
    public static List LIST_RATES = new ArrayList(Arrays.asList(new String[] { "High", "Medium", "Low" }));
    public static List LIST_RATES2 = new ArrayList(Arrays.asList(new String[] { "High", "Medium", "Low", "None" }));
    public static List LIST_TOGGLE = new ArrayList(Arrays.asList(new String[] { "Off", "On" }));
    public static List LIST_CHANCE = new ArrayList(
        Arrays.asList(
            new String[] { "1/2 Day", "1 Day", "2 Days", "3 Days", "4 Days", "5 Days", "6 Days", "7 Days", "8 Days",
                "9 Days", "10 Days", "Never" }));
    public static List LIST_STORMSWHEN = new ArrayList(
        Arrays.asList(new String[] { "Local Biomes", "Global Overcast" }));
    public static List LIST_LOCK = new ArrayList(Arrays.asList(new String[] { "Off", "On", "Don\'t lock" }));
    public static List listSettingsClient = new ArrayList();
    public static List listSettingsServer = new ArrayList();
    public static NBTTagCompound nbtClientCache = new NBTTagCompound();
    public static NBTTagCompound nbtServerData = new NBTTagCompound();
    public static NBTTagCompound nbtClientData = new NBTTagCompound();

    public static void processNBTToModConfigClient() {
        nbtSaveDataClient();
        Weather.dbg("processNBTToModConfigClient");
        Weather.dbg("nbtClientData: " + nbtClientData);
        String modID = "weather2Misc";

        try {
            if (nbtClientData.hasKey("btn_" + CMD_BTN_COMP_PARTICLEPRECIP)) {
                ConfigMisc.Particle_RainSnow = ((String) LIST_TOGGLE
                    .get(nbtClientData.getInteger("btn_" + CMD_BTN_COMP_PARTICLEPRECIP))).equalsIgnoreCase("on");
            }

            if (nbtClientData.hasKey("btn_" + CMD_BTN_PERF_STORM)) {
                if (((String) LIST_RATES.get(nbtClientData.getInteger("btn_" + CMD_BTN_PERF_STORM)))
                    .equalsIgnoreCase("high")) {
                    ConfigMisc.Cloud_ParticleSpawnDelay = 0;
                } else if (((String) LIST_RATES.get(nbtClientData.getInteger("btn_" + CMD_BTN_PERF_STORM)))
                    .equalsIgnoreCase("medium")) {
                        ConfigMisc.Cloud_ParticleSpawnDelay = 2;
                    } else if (((String) LIST_RATES.get(nbtClientData.getInteger("btn_" + CMD_BTN_PERF_STORM)))
                        .equalsIgnoreCase("low")) {
                            ConfigMisc.Cloud_ParticleSpawnDelay = 5;
                        }
            }

            if (nbtClientData.hasKey("btn_" + CMD_BTN_PERF_NATURE)) {
                if (((String) LIST_RATES2.get(nbtClientData.getInteger("btn_" + CMD_BTN_PERF_NATURE)))
                    .equalsIgnoreCase("high")) {
                    ConfigMisc.Wind_Particle_effect_rate = 1.0D;
                } else if (((String) LIST_RATES2.get(nbtClientData.getInteger("btn_" + CMD_BTN_PERF_NATURE)))
                    .equalsIgnoreCase("medium")) {
                        ConfigMisc.Wind_Particle_effect_rate = 0.699999988079071D;
                    } else if (((String) LIST_RATES2.get(nbtClientData.getInteger("btn_" + CMD_BTN_PERF_NATURE)))
                        .equalsIgnoreCase("low")) {
                            ConfigMisc.Wind_Particle_effect_rate = 0.30000001192092896D;
                        } else if (((String) LIST_RATES2.get(nbtClientData.getInteger("btn_" + CMD_BTN_PERF_NATURE)))
                            .equalsIgnoreCase("none")) {
                                ConfigMisc.Wind_Particle_effect_rate = 0.0D;
                            }
            }

            if (nbtClientData.hasKey("btn_" + CMD_BTN_PERF_PRECIPRATE)) {
                if (((String) LIST_RATES2.get(nbtClientData.getInteger("btn_" + CMD_BTN_PERF_PRECIPRATE)))
                    .equalsIgnoreCase("high")) {
                    ConfigMisc.Particle_Precipitation_effect_rate = 1.0D;
                } else if (((String) LIST_RATES2.get(nbtClientData.getInteger("btn_" + CMD_BTN_PERF_PRECIPRATE)))
                    .equalsIgnoreCase("medium")) {
                        ConfigMisc.Particle_Precipitation_effect_rate = 0.7D;
                    } else if (((String) LIST_RATES2.get(nbtClientData.getInteger("btn_" + CMD_BTN_PERF_PRECIPRATE)))
                        .equalsIgnoreCase("low")) {
                            ConfigMisc.Particle_Precipitation_effect_rate = 0.3D;
                        } else
                        if (((String) LIST_RATES2.get(nbtClientData.getInteger("btn_" + CMD_BTN_PERF_PRECIPRATE)))
                            .equalsIgnoreCase("none")) {
                                ConfigMisc.Particle_Precipitation_effect_rate = 0.0D;
                            }
            }

            if (nbtClientData.hasKey("btn_" + CMD_BTN_COMP_PARTICLESNOMODS)) {
                ConfigMisc.Particle_VanillaAndWeatherOnly = ((String) LIST_TOGGLE
                    .get(nbtClientData.getInteger("btn_" + CMD_BTN_COMP_PARTICLESNOMODS))).equalsIgnoreCase("on");
            }

            NBTTagCompound ex = nbtClientData.getCompoundTag("dimData");
            Weather.dbg("before cl: " + listDimensionsWindEffects);
            Iterator it = ex.func_150296_c()
                .iterator();

            while (it.hasNext()) {
                String tagName = (String) it.next();
                NBTTagInt entry = (NBTTagInt) ex.getTag(tagName);
                String[] vals = tagName.split("_");
                if (vals[2].equals("3")) {
                    int dimID = Integer.parseInt(vals[1]);
                    if (entry.func_150287_d() == 0) {
                        if (listDimensionsWindEffects.contains(Integer.valueOf(dimID))) {
                            listDimensionsWindEffects.remove(Integer.valueOf(dimID));
                        }
                    } else if (!listDimensionsWindEffects.contains(Integer.valueOf(dimID))) {
                        listDimensionsWindEffects.add(Integer.valueOf(dimID));
                    }
                }
            }

            Weather.dbg("after cl: " + listDimensionsWindEffects);
            processListsReverse();
        } catch (Exception var7) {
            var7.printStackTrace();
        }

        ((ModConfigData) ConfigMod.configLookup.get(modID)).writeConfigFile(true);
    }

    public static void processNBTToModConfigServer() {
        nbtSaveDataServer();
        Weather.dbg("processNBTToModConfigServer");
        Weather.dbg("nbtServerData: " + nbtServerData);
        String modID = "weather2Misc";

        try {
            if (nbtServerData.hasKey("btn_" + CMD_BTN_COMP_STORM)) {
                ConfigMisc.overcastMode = ((String) LIST_STORMSWHEN
                    .get(nbtServerData.getInteger("btn_" + CMD_BTN_COMP_STORM))).equalsIgnoreCase("Global Overcast");
            }

            if (nbtServerData.hasKey("btn_" + CMD_BTN_COMP_LOCK)) {
                if (((String) LIST_LOCK.get(nbtServerData.getInteger("btn_" + CMD_BTN_COMP_LOCK)))
                    .equalsIgnoreCase("on")) {
                    ConfigMisc.lockServerWeatherMode = 1;
                } else if (((String) LIST_LOCK.get(nbtServerData.getInteger("btn_" + CMD_BTN_COMP_LOCK)))
                    .equalsIgnoreCase("off")) {
                        ConfigMisc.lockServerWeatherMode = 0;
                    } else {
                        ConfigMisc.lockServerWeatherMode = -1;
                    }
            }

            if (nbtServerData.hasKey("btn_" + CMD_BTN_COMP_SNOWFALLBLOCKS)) {
                boolean ex = ((String) LIST_TOGGLE.get(nbtServerData.getInteger("btn_" + CMD_BTN_COMP_SNOWFALLBLOCKS)))
                    .equalsIgnoreCase("on");
                ConfigMisc.Snow_PerformSnowfall = ex;
                ConfigMisc.Snow_ExtraPileUp = ex;
            }

            if (nbtServerData.hasKey("btn_" + CMD_BTN_PREF_RATEOFSTORM)) {
                int ex1 = nbtServerData.getInteger("btn_" + CMD_BTN_PREF_RATEOFSTORM);
                if (ex1 == 0) {
                    ConfigMisc.Player_Storm_Deadly_TimeBetweenInTicks = 12000;
                    ConfigMisc.Server_Storm_Deadly_TimeBetweenInTicks = 12000;
                } else if (ex1 == 11) {
                    ConfigMisc.Player_Storm_Deadly_TimeBetweenInTicks = -1;
                    ConfigMisc.Server_Storm_Deadly_TimeBetweenInTicks = -1;
                } else {
                    ConfigMisc.Player_Storm_Deadly_TimeBetweenInTicks = 24000 * ex1;
                    ConfigMisc.Server_Storm_Deadly_TimeBetweenInTicks = 24000 * ex1;
                }
            }

            if (nbtServerData.hasKey("btn_" + CMD_BTN_PREF_CHANCEOFSTORM)) {
                if (((String) LIST_RATES2.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_CHANCEOFSTORM)))
                    .equalsIgnoreCase("high")) {
                    ConfigMisc.Player_Storm_Deadly_OddsTo1 = 30;
                    ConfigMisc.Server_Storm_Deadly_OddsTo1 = 30;
                } else if (((String) LIST_RATES2.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_CHANCEOFSTORM)))
                    .equalsIgnoreCase("medium")) {
                        ConfigMisc.Player_Storm_Deadly_OddsTo1 = 45;
                        ConfigMisc.Server_Storm_Deadly_OddsTo1 = 45;
                    } else if (((String) LIST_RATES2.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_CHANCEOFSTORM)))
                        .equalsIgnoreCase("low")) {
                            ConfigMisc.Player_Storm_Deadly_OddsTo1 = 60;
                            ConfigMisc.Server_Storm_Deadly_OddsTo1 = 60;
                        }
            }

            if (nbtServerData.hasKey("btn_" + CMD_BTN_PREF_CHANCEOFRAIN)) {
                if (((String) LIST_RATES2.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_CHANCEOFRAIN)))
                    .equalsIgnoreCase("high")) {
                    ConfigMisc.Player_Storm_Rain_OddsTo1 = 150;
                } else if (((String) LIST_RATES2.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_CHANCEOFRAIN)))
                    .equalsIgnoreCase("medium")) {
                        ConfigMisc.Player_Storm_Rain_OddsTo1 = 300;
                    } else if (((String) LIST_RATES2.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_CHANCEOFRAIN)))
                        .equalsIgnoreCase("low")) {
                            ConfigMisc.Player_Storm_Rain_OddsTo1 = 450;
                        } else
                        if (((String) LIST_RATES2.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_CHANCEOFRAIN)))
                            .equalsIgnoreCase("none")) {
                                ConfigMisc.Player_Storm_Rain_OddsTo1 = -1;
                            }
            }

            if (nbtServerData.hasKey("btn_" + CMD_BTN_PREF_BLOCKDESTRUCTION)) {
                ConfigMisc.Storm_Tornado_grabBlocks = ((String) LIST_TOGGLE
                    .get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_BLOCKDESTRUCTION))).equalsIgnoreCase("on");
            }

            if (nbtServerData.hasKey("btn_" + CMD_BTN_PREF_TORNADOANDCYCLONES)) {
                ConfigMisc.Storm_NoTornadosOrCyclones = ((String) LIST_TOGGLE
                    .get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_TORNADOANDCYCLONES))).equalsIgnoreCase("off");
            }

            NBTTagCompound ex2 = nbtServerData.getCompoundTag("dimData");
            Weather.dbg("before: " + listDimensionsWeather);

            NBTTagInt entry;
            String[] vals;
            for (Iterator it = ex2.func_150296_c()
                .iterator(); it.hasNext(); Weather
                    .dbg("dim: " + vals[1] + " - setting ID: " + vals[2] + " - data: " + entry.func_150287_d())) {
                String tagName = (String) it.next();
                entry = (NBTTagInt) ex2.getTag(tagName);
                vals = tagName.split("_");
                int dimID;
                if (vals[2].equals("0")) {
                    dimID = Integer.parseInt(vals[1]);
                    if (entry.func_150287_d() == 0) {
                        if (listDimensionsWeather.contains(Integer.valueOf(dimID))) {
                            listDimensionsWeather.remove(dimID);
                        }
                    } else if (!listDimensionsWeather.contains(Integer.valueOf(dimID))) {
                        listDimensionsWeather.add(Integer.valueOf(dimID));
                    }
                } else if (vals[2].equals("1")) {
                    dimID = Integer.parseInt(vals[1]);
                    if (entry.func_150287_d() == 0) {
                        if (listDimensionsClouds.contains(Integer.valueOf(dimID))) {
                            listDimensionsClouds.remove(dimID);
                        }
                    } else if (!listDimensionsClouds.contains(Integer.valueOf(dimID))) {
                        listDimensionsClouds.add(Integer.valueOf(dimID));
                    }
                } else if (vals[2].equals("2")) {
                    dimID = Integer.parseInt(vals[1]);
                    if (entry.func_150287_d() == 0) {
                        if (listDimensionsStorms.contains(Integer.valueOf(dimID))) {
                            listDimensionsStorms.remove(dimID);
                        }
                    } else if (!listDimensionsStorms.contains(Integer.valueOf(dimID))) {
                        listDimensionsStorms.add(Integer.valueOf(dimID));
                    }
                }
            }

            Weather.dbg("after: " + listDimensionsWeather);
            processListsReverse();
        } catch (Exception var7) {
            var7.printStackTrace();
        }

        ((ModConfigData) ConfigMod.configLookup.get(modID)).writeConfigFile(true);
    }

    public static void nbtReceiveClientData(NBTTagCompound parNBT) {
        for (int i = 0; i <= CMD_BTN_HIGHEST_ID; ++i) {
            if (parNBT.hasKey("btn_" + i)) {
                nbtServerData.setInteger("btn_" + i, parNBT.getInteger("btn_" + i));
            }
        }

        nbtServerData.setTag("dimData", parNBT.getCompoundTag("dimData"));
        processNBTToModConfigServer();
    }

    public static void nbtReceiveServerDataForCache(NBTTagCompound parNBT) {
        nbtClientCache = parNBT;
        Weather.dbg("nbtClientCache: " + nbtServerData);
    }

    public static void nbtSaveDataClient() {
        nbtWriteNBTToDisk(nbtClientData, true);
    }

    public static void nbtSaveDataServer() {
        nbtWriteNBTToDisk(nbtServerData, false);
    }

    public static void nbtLoadDataAll() {
        nbtLoadDataClient();
        nbtLoadDataServer();
    }

    public static void nbtLoadDataClient() {
        nbtClientData = nbtReadNBTFromDisk(true);
    }

    public static void nbtLoadDataServer() {
        nbtServerData = nbtReadNBTFromDisk(false);
    }

    public static NBTTagCompound createNBTDimensionListing() {
        NBTTagCompound data = new NBTTagCompound();
        WorldServer[] worlds = DimensionManager.getWorlds();

        for (int i = 0; i < worlds.length; ++i) {
            NBTTagCompound nbtDim = new NBTTagCompound();
            int dimID = worlds[i].provider.dimensionId;
            nbtDim.setInteger("ID", dimID);
            nbtDim.setString("name", worlds[i].provider.getDimensionName());
            nbtDim.setBoolean("weather", listDimensionsWeather.contains(Integer.valueOf(dimID)));
            nbtDim.setBoolean("clouds", listDimensionsClouds.contains(Integer.valueOf(dimID)));
            nbtDim.setBoolean("storms", listDimensionsStorms.contains(Integer.valueOf(dimID)));
            data.setTag("" + dimID, nbtDim);
        }

        return data;
    }

    public static void processLists() {
        listDimensionsWeather = parseList(ConfigMisc.Dimension_List_Weather);
        listDimensionsClouds = parseList(ConfigMisc.Dimension_List_Clouds);
        listDimensionsStorms = parseList(ConfigMisc.Dimension_List_Storms);
        listDimensionsWindEffects = parseList(ConfigMisc.Dimension_List_WindEffects);
    }

    public static void processListsReverse() {
        ConfigMisc.Dimension_List_Weather = StringUtils.join(listDimensionsWeather, " ");
        ConfigMisc.Dimension_List_Clouds = StringUtils.join(listDimensionsClouds, " ");
        ConfigMisc.Dimension_List_Storms = StringUtils.join(listDimensionsStorms, " ");
        ConfigMisc.Dimension_List_WindEffects = StringUtils.join(listDimensionsWindEffects, " ");
    }

    public static List parseList(String parData) {
        String listStr = parData.replace(",", " ");
        String[] arrStr = listStr.split(" ");
        Integer[] arrInt = new Integer[arrStr.length];

        for (int i = 0; i < arrStr.length; ++i) {
            try {
                arrInt[i] = Integer.valueOf(Integer.parseInt(arrStr[i]));
            } catch (Exception var6) {
                arrInt[i] = Integer.valueOf(-999999);
            }
        }

        return new ArrayList(Arrays.asList(arrInt));
    }

    public static void nbtWriteNBTToDisk(NBTTagCompound parData, boolean saveForClient) {
        String fileURL = null;
        if (saveForClient) {
            fileURL = CoroUtilFile.getMinecraftSaveFolderPath() + File.separator
                + "Weather2"
                + File.separator
                + "EZGUIConfigClientData.dat";
        } else {
            fileURL = CoroUtilFile.getMinecraftSaveFolderPath() + File.separator
                + "Weather2"
                + File.separator
                + "EZGUIConfigServerData.dat";
        }

        try {
            FileOutputStream ex = new FileOutputStream(fileURL);
            CompressedStreamTools.writeCompressed(parData, ex);
            ex.close();
        } catch (Exception var4) {
            var4.printStackTrace();
            Weather.dbg("Error writing Weather2 EZ GUI data");
        }

    }

    public static NBTTagCompound nbtReadNBTFromDisk(boolean loadForClient) {
        NBTTagCompound data = new NBTTagCompound();
        String fileURL = null;
        if (loadForClient) {
            fileURL = CoroUtilFile.getMinecraftSaveFolderPath() + File.separator
                + "Weather2"
                + File.separator
                + "EZGUIConfigClientData.dat";
        } else {
            fileURL = CoroUtilFile.getMinecraftSaveFolderPath() + File.separator
                + "Weather2"
                + File.separator
                + "EZGUIConfigServerData.dat";
        }

        try {
            if ((new File(fileURL)).exists()) {
                data = CompressedStreamTools.readCompressed(new FileInputStream(fileURL));
            }
        } catch (Exception var4) {
            var4.printStackTrace();
            Weather.dbg("Error reading Weather2 EZ GUI data");
        }

        return data;
    }

    static {
        listSettingsClient.add(Integer.valueOf(CMD_BTN_PERF_STORM));
        listSettingsClient.add(Integer.valueOf(CMD_BTN_PERF_NATURE));
        listSettingsClient.add(Integer.valueOf(CMD_BTN_COMP_PARTICLEPRECIP));
        listSettingsClient.add(Integer.valueOf(CMD_BTN_PERF_PRECIPRATE));
        listSettingsClient.add(Integer.valueOf(CMD_BTN_COMP_PARTICLESNOMODS));
        listSettingsServer.add(Integer.valueOf(CMD_BTN_COMP_STORM));
        listSettingsServer.add(Integer.valueOf(CMD_BTN_COMP_LOCK));
        listSettingsServer.add(Integer.valueOf(CMD_BTN_COMP_SNOWFALLBLOCKS));
        listSettingsServer.add(Integer.valueOf(CMD_BTN_COMP_LEAFFALLBLOCKS));
        listSettingsServer.add(Integer.valueOf(CMD_BTN_PREF_RATEOFSTORM));
        listSettingsServer.add(Integer.valueOf(CMD_BTN_PREF_CHANCEOFSTORM));
        listSettingsServer.add(Integer.valueOf(CMD_BTN_PREF_CHANCEOFRAIN));
        listSettingsServer.add(Integer.valueOf(CMD_BTN_PREF_BLOCKDESTRUCTION));
        listSettingsServer.add(Integer.valueOf(CMD_BTN_PREF_TORNADOANDCYCLONES));
    }
}
