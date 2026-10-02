package weather2.protection;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import weather2.weathersystem.storm.StormObject;

/**
 * NTNH Deep Module: Block Context (Flyweight Evaluation Context).
 * Encapsulates the complete spatial, ontological, and thermodynamic context of a potential block grab.
 * Employs ThreadLocal pooling to ensure zero heap allocations during the hot loop (up to 300 checks/tick).
 * Employs lazy evaluation for World queries (getBlockMetadata, getTileEntity, getBlockHardness).
 */
public class BlockContext {

    public World world;
    public int x;
    public int y;
    public int z;
    public Block block;
    public StormObject storm;

    // Lazy evaluation state
    private int metadata = -1;
    private TileEntity tileEntity = null;
    private boolean tileEntityQueried = false;
    private float hardness = -999.0F;
    private boolean hardnessQueried = false;
    private String registryName = null;
    private boolean registryNameQueried = false;

    private static final ThreadLocal<BlockContext> POOL = new ThreadLocal<BlockContext>() {

        @Override
        protected BlockContext initialValue() {
            return new BlockContext();
        }
    };

    public BlockContext() {}

    public BlockContext set(World world, int x, int y, int z, Block block, StormObject storm) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
        this.storm = storm;
        this.metadata = -1;
        this.tileEntity = null;
        this.tileEntityQueried = false;
        this.hardness = -999.0F;
        this.hardnessQueried = false;
        this.registryName = null;
        this.registryNameQueried = false;
        return this;
    }

    public static BlockContext getPooled(World world, int x, int y, int z, Block block, StormObject storm) {
        BlockContext ctx = POOL.get();
        ctx.set(world, x, y, z, block, storm);
        return ctx;
    }

    public static BlockContext of(Object blockObj) {
        Block b = (blockObj instanceof Block) ? (Block) blockObj : null;
        return getPooled(null, 0, 0, 0, b, null);
    }

    public int getMetadata() {
        if (metadata == -1 && world != null) {
            try {
                metadata = world.getBlockMetadata(x, y, z);
            } catch (Throwable t) {
                metadata = 0;
            }
        }
        return Math.max(0, metadata);
    }

    public TileEntity getTileEntity() {
        if (!tileEntityQueried && world != null) {
            tileEntityQueried = true;
            try {
                tileEntity = world.getTileEntity(x, y, z);
            } catch (Throwable t) {
                tileEntity = null;
            }
        }
        return tileEntity;
    }

    public float getHardness() {
        if (!hardnessQueried) {
            hardnessQueried = true;
            if (block != null) {
                try {
                    if (world != null) {
                        hardness = block.getBlockHardness(world, x, y, z);
                    } else {
                        hardness = block.getBlockHardness(null, 0, 0, 0);
                    }
                } catch (Throwable t) {
                    hardness = -1.0F;
                }
            } else {
                hardness = -1.0F;
            }
        }
        return hardness;
    }

    public String getRegistryName() {
        if (!registryNameQueried) {
            registryNameQueried = true;
            if (block != null) {
                registryName = BlockProtectionPipeline.getBlockRegistryName(block);
            }
        }
        return registryName;
    }

    public int getDimensionId() {
        if (world != null && world.provider != null) {
            return world.provider.dimensionId;
        }
        if (storm != null && storm.manager != null) {
            return storm.manager.dim;
        }
        return 0;
    }

    public int getStormStage() {
        return storm != null ? storm.levelCurIntensityStage : 0;
    }
}
