package appeng.services.compass.converter;


import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public final class OldCompassReader {
    static final Pattern regionNameFormat = Pattern.compile("([-0-9]*)_([-0-9]*)_([-0-9]*)\\.dat");

    private final int dimId;
    private final File worldCompassFolder;

    public OldCompassReader(final int dimId, @Nonnull final File worldCompassFolder) {
        Preconditions.checkNotNull(worldCompassFolder);
        Preconditions.checkArgument(worldCompassFolder.isDirectory());

        this.dimId = dimId;
        this.worldCompassFolder = worldCompassFolder;
    }

    Stream<OldCompassRegion> loadRegions() {
        var names = getRegionNames();
        return names.stream()
                .filter(name -> regionNameFormat.matcher(name).matches())
                .filter(name -> name.startsWith(Integer.toString(this.dimId)))
                .map(name -> new OldCompassRegion(worldCompassFolder, name));
    }

    private Collection<String> getRegionNames() {
        var fileNames = worldCompassFolder.list();
        return fileNames == null ? Collections.emptyList() : Arrays.asList(fileNames);
    }
}
