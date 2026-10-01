package weather2;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.world.World;

import cpw.mods.fml.client.FMLClientHandler;
import weather2.client.SceneEnhancer;
import weather2.client.gui.GuiEZConfig;
import weather2.config.ConfigMisc;
import weather2.util.WeatherUtilConfig;
import weather2.weathersystem.EntityRendererProxyWeather2Mini;
import weather2.weathersystem.WeatherManagerClient;

public class ClientTickHandler {

    public static World lastWorld;
    public static WeatherManagerClient weatherManager;
    public static SceneEnhancer sceneEnhancer;
    public boolean hasOpenedConfig = false;
    public GuiButton configButton;

    public ClientTickHandler() {
        if (sceneEnhancer == null) {
            sceneEnhancer = new SceneEnhancer();
            (new Thread(sceneEnhancer, "Weather2 Scene Enhancer")).start();
        }

    }

    public void onRenderScreenTick() {}

    public void onTickInGUI(GuiScreen guiscreen) {}

    public void onTickInGame() {
        Minecraft mc = FMLClientHandler.instance()
            .getClient();
        WorldClient world = mc.theWorld;
        if (ConfigMisc.Misc_proxyRenderOverrideEnabled) {
            if (!(mc.entityRenderer instanceof EntityRendererProxyWeather2Mini)) {
                EntityRendererProxyWeather2Mini msg = new EntityRendererProxyWeather2Mini(mc, mc.getResourceManager());
                mc.entityRenderer = msg;
            }
        } else if (mc.entityRenderer instanceof EntityRendererProxyWeather2Mini) {
            mc.entityRenderer = new EntityRenderer(mc, mc.getResourceManager());
        }

        if (world != null && world != lastWorld) {
            init(world);
        }

        if (world != null) {
            weatherManager.tick();
            if (ConfigMisc.Misc_ForceVanillaCloudsOff && world.provider.dimensionId == 0) {
                mc.gameSettings.clouds = false;
            }
        }

        if (world != null
            && WeatherUtilConfig.listDimensionsWindEffects.contains(Integer.valueOf(world.provider.dimensionId))) {
            sceneEnhancer.tickClient();
        }

        if (world != null && mc.ingameGUI.getChatGUI()
            .getSentMessages()
            .size() > 0) {
            String msg1 = (String) mc.ingameGUI.getChatGUI()
                .getSentMessages()
                .get(
                    mc.ingameGUI.getChatGUI()
                        .getSentMessages()
                        .size() - 1);
            if (msg1.equals("/weather2 config")) {
                mc.ingameGUI.getChatGUI()
                    .getSentMessages()
                    .remove(
                        mc.ingameGUI.getChatGUI()
                            .getSentMessages()
                            .size() - 1);
                mc.displayGuiScreen(new GuiEZConfig());
            }
        }

    }

    public static void checkClientWeather() {
        try {
            if (weatherManager == null) {
                init(
                    FMLClientHandler.instance()
                        .getClient().theWorld);
            }
        } catch (Exception var1) {
            System.out.println(
                "Warning, Weather2 weatherManager init failed for some reason, printing stacktrace, report to https://github.com/Corosauce/weather2/issues");
            var1.printStackTrace();
        }

    }

    public static void init(World world) {
        Weather.dbg("Initializing WeatherManagerClient for client world");
        lastWorld = world;
        weatherManager = new WeatherManagerClient(world.provider.dimensionId);
    }

    static void getField(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & -17);
        field.set((Object) null, newValue);
    }
}
