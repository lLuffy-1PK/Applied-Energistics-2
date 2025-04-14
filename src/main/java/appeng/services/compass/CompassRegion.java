package appeng.services.compass;

import appeng.core.AELog;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.Objects;

public class CompassRegion extends WorldSavedData {
    /**
     * The number of chunks that get saved in a region on each axis.
     */
    private static final int CHUNKS_PER_REGION = 1024;

    private static final int BITMAP_LENGTH = CHUNKS_PER_REGION * CHUNKS_PER_REGION;
    private static final String NBT_PREFIX = "section";

    // Key is the section index
    private final Int2ObjectMap<BitSet> sections = new Int2ObjectOpenHashMap<>();

    public CompassRegion(String name) {
        super(name);
    }

    /**
     * Gets the name of the save data for a region that has the given coordinates.
     */
    private static String getRegionSaveName(int regionX, int regionZ) {
        return "ae2_compass_" + regionX + "_" + regionZ;
    }

    /**
     * Retrieve the compass region that serves the given chunk position.
     */
    public static CompassRegion get(WorldServer world, ChunkPos chunkPos) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(chunkPos, "chunkPos");

        int regionX = chunkPos.x / CHUNKS_PER_REGION;
        int regionZ = chunkPos.z / CHUNKS_PER_REGION;
        var name = getRegionSaveName(regionX, regionZ);

        MapStorage worldStorage = world.getPerWorldStorage();
        var compassRegion = (CompassRegion) worldStorage.getOrLoadData(CompassRegion.class, name);
        if (compassRegion == null) {
            compassRegion = new CompassRegion(name);
            worldStorage.setData(name, compassRegion);
        }
        return compassRegion;
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound nbt) {
        for (String key : nbt.getKeySet()) {
            if (key.startsWith(NBT_PREFIX)) {
                try {
                    var sectionIndex = Integer.parseInt(key.substring(NBT_PREFIX.length()));
                    sections.put(sectionIndex, BitSet.valueOf(nbt.getByteArray(key)));
                } catch (NumberFormatException e) {
                    AELog.warn("Compass region contains invalid NBT tag %s", key);
                }
            } else {
                AELog.warn("Compass region contains unknown NBT tag %s", key);
            }
        }
    }

    @Override
    @NotNull
    public NBTTagCompound writeToNBT(@NotNull NBTTagCompound compound) {
        for (var entry : sections.int2ObjectEntrySet()) {
            String key = NBT_PREFIX + entry.getIntKey();
            if (entry.getValue().isEmpty()) {
                continue;
            }
            compound.setByteArray(key, entry.getValue().toByteArray());
        }
        return compound;
    }

    boolean hasCompassTarget(int cx, int cz) {
        var bitmapIndex = getBitmapIndex(cx, cz);
        for (BitSet bitmap : sections.values()) {
            if (bitmap.get(bitmapIndex)) {
                return true;
            }
        }
        return false;
    }

    boolean hasCompassTarget(int cx, int cz, int sectionIndex) {
        var bitmapIndex = getBitmapIndex(cx, cz);
        var section = sections.get(sectionIndex);
        if (section != null) {
            return section.get(bitmapIndex);
        }
        return false;
    }

    void setHasCompassTarget(int cx, int cz, int sectionIndex, boolean hasTarget) {
        var bitmapIndex = getBitmapIndex(cx, cz);
        var section = sections.get(sectionIndex);
        if (section == null) {
            if (hasTarget) {
                section = new BitSet(BITMAP_LENGTH);
                section.set(bitmapIndex);
                sections.put(sectionIndex, section);
                markDirty();
            }
        } else {
            if (section.get(bitmapIndex) != hasTarget) {
                markDirty();
            }
            // There already was data on this y-section in this region
            if (!hasTarget) {
                section.clear(bitmapIndex);
                if (section.isEmpty()) {
                    sections.remove(sectionIndex);
                }
                markDirty();
            } else {
                section.set(bitmapIndex);
            }
        }
    }

    private static int getBitmapIndex(int cx, int cz) {
        cx &= CHUNKS_PER_REGION - 1;
        cz &= CHUNKS_PER_REGION - 1;
        return cx + cz * CHUNKS_PER_REGION;
    }
}
