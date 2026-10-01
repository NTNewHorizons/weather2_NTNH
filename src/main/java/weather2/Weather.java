package weather2;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

import CoroUtil.util.CoroUtilFile;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import modconfig.ConfigMod;
import weather2.config.ConfigMisc;
import weather2.player.PlayerData;
import weather2.util.WeatherUtilConfig;
import weather2.weathersystem.WeatherManagerServer;

@Mod(modid = "weather2", name = "weather2", version = "v2.3.19")
public class Weather {

    @Instance("weather2")
    public static Weather instance;
    public static String modID = "weather2";
    public static long lastWorldTime;
    public Configuration preInitConfig;
    @SidedProxy(clientSide = "weather2.ClientProxy", serverSide = "weather2.CommonProxy")
    public static CommonProxy proxy;
    public static boolean initProperNeededForWorld = true;
    public static String eventChannelName = "weather2";
    public static final FMLEventChannel eventChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(eventChannelName);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        eventChannel.register(new EventHandlerPacket());
        MinecraftForge.EVENT_BUS.register(new EventHandlerForge());
        FMLCommonHandler.instance()
            .bus()
            .register(new EventHandlerFML());
        ConfigMod.addConfigFile(event, "weather2Misc", new ConfigMisc());
        WeatherUtilConfig.nbtLoadDataAll();
    }

    @EventHandler
    public void load(FMLInitializationEvent event) {
        proxy.init();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandWeather2());
    }

    @EventHandler
    public void serverStart(FMLServerStartedEvent event) {}

    @EventHandler
    public void serverStop(FMLServerStoppedEvent event) {
        writeOutData(true);
        resetStates();
        initProperNeededForWorld = true;
    }

    public static void initTry() {
        if (initProperNeededForWorld) {
            System.out.println("Weather2 being reinitialized");
            initProperNeededForWorld = false;
            CoroUtilFile.getWorldFolderName();
            ServerTickHandler.initialize();
        }

    }

    public static void resetStates() {
        ServerTickHandler.reset();
    }

    public static void writeOutData(boolean unloadInstances) {
        try {
            WeatherManagerServer ex = (WeatherManagerServer) ServerTickHandler.lookupDimToWeatherMan
                .get(Integer.valueOf(0));
            if (ex != null) {
                ex.writeToFile();
            }

            PlayerData.writeAllPlayerNBT(unloadInstances);
        } catch (Exception var2) {
            var2.printStackTrace();
        }

    }

    @EventHandler
    public void handleIMCMessages(IMCMessage event) {}

    public static void dbg(Object obj) {
        if (ConfigMisc.consoleDebug) {
            System.out.println(obj);
        }

    }

}
