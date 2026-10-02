package weather2.integration.hbm;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import weather2.protection.BlockProtectionPipeline;

/**
 * NTNH Deep Module: HBM NTM Block Protection & Whitelist Adapter.
 * Enforces ontological immunity for all HBM machinery, reactors, cables, and hazardous blocks,
 * with an explicit whitelist for lightweight planetary debris (waste, scrap, ash, meteorite rubble).
 */
public class HbmProtectionAdapter {

    public static final String HBM_PACKAGE_PREFIX = "com.hbm.";
    public static final String HBM_REGISTRY_PREFIX = "hbm:";

    private static final Set<String> HBM_GRAB_WHITELIST = new HashSet<String>();

    static {
        // Only lightweight decoration/clutter blocks from HBM are allowed to be grabbed on hostile planets
        HBM_GRAB_WHITELIST.add("hbm:block_waste");
        HBM_GRAB_WHITELIST.add("hbm:waste_earth");
        HBM_GRAB_WHITELIST.add("hbm:waste_mycelium");
        HBM_GRAB_WHITELIST.add("hbm:waste_sand");
        HBM_GRAB_WHITELIST.add("hbm:block_meteor");
        HBM_GRAB_WHITELIST.add("hbm:block_meteor_broken");
        HBM_GRAB_WHITELIST.add("hbm:ash");
        HBM_GRAB_WHITELIST.add("hbm:block_scrap");
    }

    /**
     * Checks if a block belongs to the HBM NTM mod either by class hierarchy or registry prefix.
     */
    public static boolean isHbmBlock(Class<?> clazz, String regName) {
        if (regName != null && regName.startsWith(HBM_REGISTRY_PREFIX)) {
            return true;
        }
        if (clazz != null) {
            for (Class<?> curr = clazz; curr != null && curr != Object.class; curr = curr.getSuperclass()) {
                if (curr.getName()
                    .startsWith(HBM_PACKAGE_PREFIX)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the given HBM registry name is explicitly whitelisted for tornado grabbing.
     */
    public static boolean isHbmBlockWhitelisted(String regName) {
        return regName != null && HBM_GRAB_WHITELIST.contains(regName);
    }

    /**
     * Checks if the given block object is an HBM block whitelisted for tornado grabbing.
     */
    public static boolean isHbmBlockWhitelisted(Object blockObj) {
        if (blockObj == null) return false;
        String regName = BlockProtectionPipeline.getBlockRegistryName(blockObj);
        return isHbmBlockWhitelisted(regName);
    }

    /**
     * Determines whether an HBM block is immune to storm destruction.
     * All HBM blocks are protected unless explicitly whitelisted as lightweight clutter.
     */
    public static boolean isHbmBlockProtected(Object blockObj, Class<?> clazz, String regName) {
        if (!isHbmBlock(clazz, regName)) {
            return false;
        }
        // If whitelisted, it is NOT protected (can be ripped by planetary storm)
        return !isHbmBlockWhitelisted(regName);
    }

    public static void addWhitelistedBlock(String regName) {
        if (regName != null) {
            HBM_GRAB_WHITELIST.add(regName);
        }
    }

    public static void removeWhitelistedBlock(String regName) {
        if (regName != null) {
            HBM_GRAB_WHITELIST.remove(regName);
        }
    }

    public static Set<String> getGrabWhitelist() {
        return Collections.unmodifiableSet(HBM_GRAB_WHITELIST);
    }
}
