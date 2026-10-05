package weather2;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import weather2.block.BlockAnemometer;
import weather2.block.BlockTSensor;
import weather2.block.BlockTSiren;
import weather2.block.BlockWeatherDeflector;
import weather2.block.BlockWeatherForecast;
import weather2.block.BlockWeatherMachine;
import weather2.block.BlockWindVane;
import weather2.block.TileEntityAnemometer;
import weather2.block.TileEntityTSiren;
import weather2.block.TileEntityWeatherDeflector;
import weather2.block.TileEntityWeatherForecast;
import weather2.block.TileEntityWeatherMachine;
import weather2.block.TileEntityWindVane;
import weather2.entity.EntityIceBall;
import weather2.entity.EntityLightningBolt;
import weather2.entity.EntityMovingBlock;
import weather2.util.WeatherUtil;
import weather2.util.WeatherUtilConfig;

public class CommonProxy implements IGuiHandler {

    public static Block blockTSensor;
    public static Block blockTSiren;
    public static Block blockWindVane;
    public static Block blockAnemometer;
    public static Block blockWeatherForecast;
    public static Block blockWeatherMachine;
    public static Block blockWeatherDeflector;
    public static CreativeTabWeather tab;

    public void init() {
        tab = new CreativeTabWeather("Weather2");
        WeatherUtil.doBlockList();
        WeatherUtilConfig.processLists();
        addMapping(EntityIceBall.class, "Weather Hail", 0, 128, 5, true);
        addMapping(EntityMovingBlock.class, "Moving Block", 1, 128, 5, true);
        addMapping(EntityLightningBolt.class, "Weather2 Lightning Bolt", 2, 512, 5, true);
        // NTNH start: [FIX] (registry): Instantiate blocks without static config IDs (Forge 1.7.10 auto-assigns IDs)
        addBlock(blockTSensor = new BlockTSensor(), "TornadoSensor", "Tornado Sensor");
        addBlock(blockTSiren = new BlockTSiren(), TileEntityTSiren.class, "TornadoSiren", "Tornado Siren");
        addBlock(blockWindVane = new BlockWindVane(), TileEntityWindVane.class, "WindVane", "Wind Vane");
        addBlock(
            blockWeatherForecast = new BlockWeatherForecast(),
            TileEntityWeatherForecast.class,
            "WeatherForecast",
            "Weather Forecast");
        addBlock(
            blockWeatherMachine = new BlockWeatherMachine(),
            TileEntityWeatherMachine.class,
            "WeatherMachine",
            "Weather Machine (right click to cycle)");
        addBlock(
            blockWeatherDeflector = new BlockWeatherDeflector(),
            TileEntityWeatherDeflector.class,
            "WeatherDeflector",
            "Weather Deflector");
        addBlock(blockAnemometer = new BlockAnemometer(), TileEntityAnemometer.class, "Anemometer", "Anemometer");
        // NTNH end
        GameRegistry.addRecipe(
            new ItemStack(blockTSensor, 1),
            new Object[] { "X X", "DID", "X X", Character.valueOf('D'), Items.redstone, Character.valueOf('I'),
                Items.gold_ingot, Character.valueOf('X'), Items.iron_ingot });
        GameRegistry.addRecipe(
            new ItemStack(blockTSiren, 1),
            new Object[] { "XDX", "DID", "XDX", Character.valueOf('D'), Items.redstone, Character.valueOf('I'),
                blockTSensor, Character.valueOf('X'), Items.iron_ingot });
        GameRegistry.addRecipe(
            new ItemStack(blockWindVane, 1),
            new Object[] { "X X", "DXD", "X X", Character.valueOf('D'), Items.redstone, Character.valueOf('X'),
                Items.iron_ingot });
        GameRegistry.addRecipe(
            new ItemStack(blockWeatherForecast, 1),
            new Object[] { "XDX", "DID", "XDX", Character.valueOf('D'), Items.redstone, Character.valueOf('I'),
                Items.compass, Character.valueOf('X'), Items.iron_ingot });
        GameRegistry.addRecipe(
            new ItemStack(blockWeatherMachine, 1),
            new Object[] { "XDX", "DID", "XDX", Character.valueOf('D'), Items.redstone, Character.valueOf('I'),
                Items.diamond, Character.valueOf('X'), Items.iron_ingot });
        GameRegistry.addRecipe(
            new ItemStack(blockWeatherDeflector, 1),
            new Object[] { "XDX", "DID", "XDX", Character.valueOf('D'), Items.redstone, Character.valueOf('I'),
                blockWeatherMachine, Character.valueOf('X'), Items.iron_ingot });
        LanguageRegistry.instance()
            .addStringLocalization("itemGroup.Weather2", "Weather2 Items");
    }

    public static void addItem(ItemStack is, String unlocalizedName) {
        addItem(is, unlocalizedName, "");
    }

    public static void addItem(ItemStack is, String unlocalizedName, String itemNameBase) {
        Item item = is.getItem();
        item.setUnlocalizedName(Weather.modID + ":" + unlocalizedName);
        item.setTextureName(Weather.modID + ":" + unlocalizedName);
        item.setCreativeTab(CreativeTabs.tabMisc);
        LanguageRegistry.addName(item, itemNameBase);
    }

    public static void addBlock(Block block, Class tEnt, String unlocalizedName, String blockNameBase) {
        addBlock(block, unlocalizedName, blockNameBase);
        GameRegistry.registerTileEntity(tEnt, unlocalizedName);
    }

    public static void addBlock(Block parBlock, String unlocalizedName, String blockNameBase) {
        GameRegistry.registerBlock(parBlock, unlocalizedName);
        parBlock.setBlockName(Weather.modID + ":" + unlocalizedName);
        parBlock.setBlockTextureName(Weather.modID + ":" + unlocalizedName);
        parBlock.setCreativeTab(tab);
        LanguageRegistry.addName(parBlock, blockNameBase);
    }

    public static void addMapping(Class par0Class, String par1Str, int entityId, int distSync, int tickRateSync,
        boolean syncMotion) {
        EntityRegistry
            .registerModEntity(par0Class, par1Str, entityId, Weather.instance, distSync, tickRateSync, syncMotion);
    }

    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }
}
