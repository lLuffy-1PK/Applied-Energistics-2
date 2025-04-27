package appeng.worldgen.meteorite.converter;

import appeng.worldgen.meteorite.fallout.FalloutMode;
import appeng.worldgen.meteorite.settings.CraterLakeState;
import appeng.worldgen.meteorite.settings.CraterType;
import appeng.worldgen.meteorite.settings.PlacedMeteoriteSettings;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public class MeteoriteSettingsConverter {
    private static final String TAG_X = "x";
    private static final String TAG_Y = "y";
    private static final String TAG_Z = "z";
    private static final String TAG_BLOCK = "blk";
    private static final String TAG_METEORITE_RADIUS = "real_sizeOfMeteorite";
    private static final String TAG_SKY_MODE = "skyMode";

    public static PlacedMeteoriteSettings convertOld(NBTTagCompound oldSettings) {
        var pos = new BlockPos(oldSettings.getInteger(TAG_X),
                oldSettings.getInteger(TAG_Y),
                oldSettings.getInteger(TAG_Z));
        float radius = (float) oldSettings.getDouble(TAG_METEORITE_RADIUS);

        // Can't suppress block updates from fluids, so let's just ignore the old lava tag, even if we lose parity.
        var isLava = false;
        var skyMode = oldSettings.getInteger(TAG_SKY_MODE);
        var craterType = determineCraterType(isLava, skyMode);

        var falloutBlock = Block.getBlockById(oldSettings.getInteger(TAG_BLOCK));
        var falloutMode = determineFalloutMode(falloutBlock);

        var doDecay = skyMode > 3;
        // No block updates, because we can only work with 1 chunk at a time (this prevents cascading worldgen).
        var update = false;

        return new PlacedMeteoriteSettings(pos, radius, craterType,
                false, CraterLakeState.FALSE, falloutMode, doDecay, update);
    }

    private static CraterType determineCraterType(boolean isLava, int skyMode) {
        if (isLava) {
            return CraterType.LAVA;
        } else if (skyMode <= 10) {
            return CraterType.NONE;
        } else {
            return CraterType.NORMAL;
        }
    }

    private static FalloutMode determineFalloutMode(Block falloutBlock) {
        if (falloutBlock == Blocks.SAND) {
            return FalloutMode.SAND;
        } else if (falloutBlock == Blocks.HARDENED_CLAY) {
            return FalloutMode.TERRACOTTA;
        } else if (falloutBlock == Blocks.ICE || falloutBlock == Blocks.SNOW) {
            return FalloutMode.ICE_SNOW;
        } else {
            return FalloutMode.DEFAULT;
        }
    }
}
