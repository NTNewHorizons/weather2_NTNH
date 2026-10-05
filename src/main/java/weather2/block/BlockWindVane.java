package weather2.block;

import java.util.Random;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockWindVane extends BlockContainer {

    // NTNH start: [FIX] (registry): Add no-arg constructor for Forge 1.7.10 GameRegistry auto-ID assignment
    public BlockWindVane() {
        super(Material.circuits);
        this.setBlockBounds(0.4F, 0.0F, 0.4F, 0.6F, 0.3F, 0.6F);
    }

    public BlockWindVane(int var1) {
        this();
    }
    // NTNH end

    public IIcon getIcon(int par1, int par2) {
        return Blocks.stone.getIcon(par1, par2);
    }

    public int tickRate() {
        return 90;
    }

    public void updateTick(World var1, int var2, int var3, int var4, Random var5) {}

    public TileEntity createNewTileEntity(World var1, int meta) {
        return new TileEntityWindVane();
    }

    public boolean isOpaqueCube() {
        return false;
    }

    public boolean isBlockSolid(IBlockAccess par1iBlockAccess, int par2, int par3, int par4, int par5) {
        return true;
    }

    public boolean renderAsNormalBlock() {
        return false;
    }
}
