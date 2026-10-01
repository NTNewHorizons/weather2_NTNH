package weather2;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import com.google.common.collect.ImmutableList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage;
import weather2.config.ConfigMisc;
import weather2.util.WeatherUtilConfig;
import weather2.weathersystem.WeatherManagerBase;
import weather2.weathersystem.WeatherManagerServer;

public class ServerTickHandler {

    public static ArrayList listWeatherMans = new ArrayList();
    public static HashMap lookupDimToWeatherMan = new HashMap();
    public static World lastWorld;
    public static NBTTagCompound worldNBT = new NBTTagCompound();

    public static void onTickInGame() {
        if (FMLCommonHandler.instance() != null && FMLCommonHandler.instance()
            .getMinecraftServerInstance() != null) {
            WorldServer world = FMLCommonHandler.instance()
                .getMinecraftServerInstance()
                .worldServerForDimension(0);
            if (world != null && lastWorld != world) {
                lastWorld = world;
            }

            if (world != null && world.getTotalWorldTime() % (long) ConfigMisc.Misc_AutoDataSaveIntervalInTicks == 0L) {
                Weather.writeOutData(false);
            }

            WorldServer[] worlds = DimensionManager.getWorlds();

            for (int testRainRequest = 0; testRainRequest < worlds.length; ++testRainRequest) {
                if (!lookupDimToWeatherMan.containsKey(Integer.valueOf(worlds[testRainRequest].provider.dimensionId))
                    && WeatherUtilConfig.listDimensionsWeather
                        .contains(Integer.valueOf(worlds[testRainRequest].provider.dimensionId))) {
                    addWorldToWeather(worlds[testRainRequest].provider.dimensionId);
                }

                WeatherManagerServer debugIMC = (WeatherManagerServer) lookupDimToWeatherMan
                    .get(Integer.valueOf(worlds[testRainRequest].provider.dimensionId));
                if (debugIMC != null) {
                    ((WeatherManagerServer) lookupDimToWeatherMan
                        .get(Integer.valueOf(worlds[testRainRequest].provider.dimensionId))).tick();
                }
            }

            boolean var8 = false;
            if (var8) {
                new ArrayList();
                ImmutableList var9 = FMLInterModComms.fetchRuntimeMessages(Weather.modID);

                for (int ex = 0; ex < var9.size(); ++ex) {
                    if (((IMCMessage) var9.get(ex)).key.equals("weather.raining")) {
                        NBTTagCompound i = ((IMCMessage) var9.get(ex)).getNBTValue();
                        String replyMod = i.getString("replymod");
                        i.setBoolean("isRaining", true);
                        FMLInterModComms.sendRuntimeMessage(replyMod, replyMod, "weather.raining", i);
                    }
                }
            }

            boolean var10 = false;
            if (var10) {
                try {
                    new ArrayList();
                    ImmutableList var11 = FMLInterModComms.fetchRuntimeMessages(Weather.modID);

                    for (int var12 = 0; var12 < var11.size(); ++var12) {
                        ;
                    }
                } catch (Exception var7) {
                    var7.printStackTrace();
                }
            }

        }
    }

    public static void addWorldToWeather(int dim) {
        Weather.dbg("Registering Weather2 manager for dim: " + dim);
        WeatherManagerServer wm = new WeatherManagerServer(dim);
        listWeatherMans.add(wm);
        lookupDimToWeatherMan.put(Integer.valueOf(dim), wm);
        wm.readFromFile();
    }

    public static void removeWorldFromWeather(int dim) {
        Weather.dbg("Deregistering Weather2 manager for dim: " + dim);
        WeatherManagerServer wm = (WeatherManagerServer) lookupDimToWeatherMan.get(Integer.valueOf(dim));
        if (wm != null) {
            listWeatherMans.remove(wm);
            lookupDimToWeatherMan.remove(Integer.valueOf(dim));
        }

        wm.writeToFile();
    }

    public static void playerJoinedServerSyncFull(EntityPlayerMP entP) {
        WeatherManagerServer wm = (WeatherManagerServer) lookupDimToWeatherMan
            .get(Integer.valueOf(entP.worldObj.provider.dimensionId));
        if (wm != null) {
            wm.playerJoinedServerSyncFull(entP);
        }

    }

    public static void initialize() {
        if (lookupDimToWeatherMan.get(Integer.valueOf(0)) == null) {
            addWorldToWeather(0);
        }

    }

    public static void reset() {
        for (int i = 0; i < listWeatherMans.size(); ++i) {
            WeatherManagerBase wm = (WeatherManagerBase) listWeatherMans.get(i);
            int dim = wm.dim;
            if (lookupDimToWeatherMan.containsKey(Integer.valueOf(dim))) {
                removeWorldFromWeather(dim);
            }
        }

        if (listWeatherMans.size() > 0 || lookupDimToWeatherMan.size() > 0) {
            Weather.dbg(
                "Weather2 reset state failed to manually clear lists, listWeatherMans.size(): " + listWeatherMans.size()
                    + " - lookupDimToWeatherMan.size(): "
                    + lookupDimToWeatherMan.size()
                    + " - forcing a full clear of lists");
            listWeatherMans.clear();
            lookupDimToWeatherMan.clear();
        }

    }

}
