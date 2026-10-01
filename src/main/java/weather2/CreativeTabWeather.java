package weather2;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class CreativeTabWeather extends CreativeTabs {

    public CreativeTabWeather(String label) {
        super(label);
    }

    @SideOnly(Side.CLIENT)
    public ItemStack getIconItemStack() {
        return new ItemStack(CommonProxy.blockTSensor);
    }

    public Item getTabIconItem() {
        return this.getIconItemStack()
            .getItem();
    }
}
