package appeng.worldgen.meteorite;

import appeng.worldgen.meteorite.fallout.FalloutMode;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public final class PlacedMeteoriteSettings {
    private static final String TAG_POS = "pos";
    private static final String TAG_RADIUS = "radius";
    private static final String TAG_CRATER = "type";
    private static final String TAG_FALLOUT = "fallout";
    private static final String TAG_PURE = "pure";
    private static final String TAG_LAKE = "lake";

    private BlockPos pos;
    private final float meteoriteRadius;
    private final CraterType craterType;
    private final boolean pureCrater;
    private CraterLakeState craterLake;
    private final FalloutMode fallout;

    public PlacedMeteoriteSettings(BlockPos pos, float meteoriteRadius, CraterType craterType, boolean pureCrater,
                                   CraterLakeState craterLake, FalloutMode fallout) {
        this.pos = pos;
        this.meteoriteRadius = meteoriteRadius;
        this.craterType = craterType;
        this.pureCrater = pureCrater;
        this.craterLake = craterLake;
        this.fallout = fallout;
    }

    public BlockPos getPos() {
        return pos;
    }

    public CraterType getCraterType() {
        return craterType;
    }

    public float getMeteoriteRadius() {
        return meteoriteRadius;
    }

    public FalloutMode getFallout() {
        return fallout;
    }

    public boolean shouldPlaceCrater() {
        return this.craterType != CraterType.NONE;
    }

    public boolean isPureCrater() {
        return pureCrater;
    }

    public boolean isCraterLakeSet() {
        return craterLake != CraterLakeState.UNSET;
    }

    public boolean isCraterLake() {
        return craterLake == CraterLakeState.TRUE;
    }

    public void setHeight(int y) {
        this.pos = new BlockPos(pos.getX(), y, pos.getZ());
    }

    public void setCraterLake(CraterLakeState state) {
        this.craterLake = state;
    }

    public static PlacedMeteoriteSettings read(NBTTagCompound nbt) {
        BlockPos pos = new BlockPos(BlockPos.fromLong(nbt.getLong(TAG_POS)));
        float meteoriteRadius = nbt.getFloat(TAG_RADIUS);
        CraterType craterType = CraterType.values()[nbt.getByte(TAG_CRATER)];
        FalloutMode fallout = FalloutMode.values()[nbt.getByte(TAG_FALLOUT)];
        boolean pureCrater = nbt.getBoolean(TAG_PURE);
        CraterLakeState craterLake = CraterLakeState.values()[nbt.getByte(TAG_LAKE)];

        return new PlacedMeteoriteSettings(pos, meteoriteRadius, craterType, pureCrater, craterLake, fallout);
    }

    public NBTTagCompound write(NBTTagCompound nbt) {
        nbt.setLong(TAG_POS, pos.toLong());
        nbt.setFloat(TAG_RADIUS, meteoriteRadius);
        nbt.setByte(TAG_CRATER, (byte) craterType.ordinal());
        nbt.setByte(TAG_FALLOUT, (byte) fallout.ordinal());
        nbt.setBoolean(TAG_PURE, this.pureCrater);
        nbt.setByte(TAG_LAKE, (byte) this.craterLake.ordinal());
        return nbt;
    }

    @Override
    public String toString() {
        return "PlacedMeteoriteSettings [pos=" + pos + ", meteoriteRadius=" + meteoriteRadius + ", craterType="
                + craterType + ", fallout=" + fallout + ", pureCrater=" + pureCrater + ", craterLake=" + craterLake
                + "]";
    }
}