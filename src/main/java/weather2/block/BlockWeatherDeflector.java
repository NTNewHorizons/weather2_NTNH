package weather2.block;

import java.util.Random;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockWeatherDeflector extends BlockContainer {

    // NTNH start: [FIX] (registry): Add no-arg constructor for Forge 1.7.10 GameRegistry auto-ID assignment
    public BlockWeatherDeflector() {
        super(Material.clay);
    }

    public BlockWeatherDeflector(int var1) {
        this();
    }
    // NTNH end

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

    // NTNH start: Redstone & Comparator FSM Integration
    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityWeatherDeflector) {
            return ((TileEntityWeatherDeflector) te).getComparatorOutput();
        }
        return 0;
    }
    // NTNH end
}
