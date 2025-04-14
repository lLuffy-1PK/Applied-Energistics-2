package appeng.services.compass.converter;


import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.util.math.ChunkPos;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;


public final class OldCompassRegion {
    private final File worldCompassFolder;
    private final int regionX;
    private final int regionZ;

    private boolean hasFile = false;
    private ByteBuffer buffer;

    public OldCompassRegion(@Nonnull final File worldCompassFolder, @Nonnull String fileName) {
        Preconditions.checkNotNull(worldCompassFolder);
        Preconditions.checkArgument(worldCompassFolder.isDirectory());

        this.worldCompassFolder = worldCompassFolder;
        var matcher = OldCompassReader.regionNameFormat.matcher(fileName);
        // The file name was already verified by the reader
        Preconditions.checkArgument(matcher.find());
        this.regionX = NumberUtils.toInt(matcher.group(2));
        this.regionZ = NumberUtils.toInt(matcher.group(3));

        this.openFile(fileName);
    }

    public void close() {
        if (this.hasFile) {
            this.buffer = null;
            this.hasFile = false;
        }
    }

    public Collection<ChunkPos> getBeacons() {
        ArrayList<ChunkPos> positions = new ArrayList<>();
        var packedPositions = getBeaconIndices();
        for (int i = 0; i < packedPositions.size(); ++i) {
            var beaconIndex = packedPositions.getInt(i);
            positions.add(unpack(beaconIndex));
        }
        return positions;
    }

    private IntArrayList getBeaconIndices() {
        var indices = new IntArrayList();
        if (this.hasFile) {
            for (int i = 0; i < buffer.limit(); i++) {
                if (buffer.get(i) != 0) {
                    indices.add(i);
                }
            }
        }
        return indices;
    }

    /**
     * Unpacks the packed value as a ChunkPos.
     * <p>
     * The old reader's packing formula:
     * <p>
     * (cx & 0x3FF) + (cz & 0x3FF) * 0x400
     * @param packed the packed value from {@link OldCompassRegion#buffer}
     * @return the unpacked ChunkPos
     */
    private ChunkPos unpack(int packed) {
        int cx = ChunkCoord.of(packed)
                .unmask()
                .signExtend()
                .regionToWorld(regionX)
                .get();
        int cz = ChunkCoord.of(packed >>> 10)
                .unmask()
                .signExtend()
                .regionToWorld(regionZ)
                .get();
        return new ChunkPos(cx, cz);
    }

    /**
     * Small wrapper class implementing a Fluent Interface for unpacking chunk coords.
     */
    private static class ChunkCoord {
        private int val;

        private ChunkCoord(int val) {
            this.val = val;
        }

        private static ChunkCoord of(int packedVal) {
            return new ChunkCoord(packedVal);
        }

        private ChunkCoord unmask() {
            val &= 0x3FF;
            return this;
        }

        private ChunkCoord signExtend() {
            val = (val ^ 0x200) - 0x200;
            return this;
        }

        private ChunkCoord regionToWorld(int regionCoord) {
            val = regionCoord | (val & 0x3FF);
            return this;
        }

        private int get() {
            return val;
        }
    }

    private void openFile(String fileName) {
        if (this.hasFile) {
            return;
        }

        final File file = new File(this.worldCompassFolder, fileName);
        if (this.isFileExistent(file)) {
            try (var raf = new RandomAccessFile(file, "r")){
                final FileChannel fc = raf.getChannel();
                this.buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, 0x400 * 0x400);// fc.size() );
                this.hasFile = true;
            } catch (final Throwable t) {
                throw new CompassException(t);
            }
        }
    }

    private boolean isFileExistent(final File file) {
        return file.exists() && file.isFile();
    }
}