package weather2.block;

import java.util.Random;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockWeatherDeflector extends BlockContainer {

    public BlockWeatherDeflector(int var1) {
        super(Material.clay);
    }

    public int tickRate() {
        return 90;
    }

    public void updateTick(World var1, int var2, int var3, int var4, Random var5) {}

    public TileEntity createNewTileEntity(World var1, int meta) {
        return new TileEntityWeatherDeflector();
    }

    public boolean isOpaqueCube() {
        return false;
    }

    public boolean renderAsNormalBlock() {
        return false;
    }
}
