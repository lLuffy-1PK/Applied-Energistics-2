package appeng.core.worlddata;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Metadata to determine if we should process old compass data.
 */
public final class CompassMetadata extends WorldSavedData {
    // Increment this if old compass data needs to be re-converted due to code changes.
    public static final int CURRENT_VERSION = 1;
    private static final String NBT_KEY = "upgrade_version";

    private int version;

    public CompassMetadata(String name) {
        super(name);
    }

    private static String getSaveName() {
        return "ae2_compass_metadata";
    }

    public static CompassMetadata get(WorldServer world) {
        Objects.requireNonNull(world, "world");

        MapStorage worldStorage = world.getPerWorldStorage();
        var name = getSaveName();
        var metadata = (CompassMetadata) worldStorage.getOrLoadData(CompassMetadata.class, name);
        if (metadata == null) {
            metadata = new CompassMetadata(name);
            worldStorage.setData(name, metadata);
        }
        return metadata;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.version = nbt.getInteger(NBT_KEY);
    }

    @Override
    @NotNull
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger(NBT_KEY, this.version);
        return compound;
    }

    public boolean isUpToDate(World world) {
        var isNewWorld = world.getWorldInfo().getWorldTotalTime() == 0;
        return isNewWorld || this.version >= CompassMetadata.CURRENT_VERSION;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        if (this.version == version) {
            return;
        }
        this.version = version;
        markDirty();
    }
}
