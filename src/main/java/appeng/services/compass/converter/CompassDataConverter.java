package appeng.services.compass.converter;

import appeng.core.AELog;
import appeng.core.worlddata.CompassMetadata;
import appeng.core.worlddata.IOnWorldStartable;
import appeng.core.worlddata.IOnWorldStoppable;
import appeng.hooks.TickHandler;
import appeng.services.compass.CompassRegion;
import appeng.services.compass.ServerCompassService;
import com.google.common.base.Stopwatch;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * A class to convert compass data from the old format (in the AE2/compass folder) to the new format
 * {@link CompassRegion}. The converter will only run if the detected {@link CompassMetadata} is outdated.
 * <p>
 * A new instance will be registered to the event bus when a save is loaded, and unregistered when exited.
 */
public final class CompassDataConverter implements IOnWorldStartable, IOnWorldStoppable {
    private final File worldCompassFolder;

    public CompassDataConverter(File worldCompassFolder) {
        this.worldCompassFolder = worldCompassFolder;
    }

    @SubscribeEvent
    public void convertOldCompassData(final WorldEvent.Load event) {
        var world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        var metadata = CompassMetadata.get((WorldServer) world);
        if (metadata.isUpToDate(world)) {
            // New worlds are considered up-to-date, but need to update the metadata
            metadata.setVersion(CompassMetadata.CURRENT_VERSION);
            return;
        }

        var watch = Stopwatch.createStarted();
        // Process all old compass regions
        var dimId = world.provider.getDimension();
        AELog.info("Found outdated compass metadata [version=%d] in dimension [%d] "
                        + "- converting old compass data...",
                metadata.getVersion(),
                dimId);
        var cr = new OldCompassReader(dimId, worldCompassFolder);
        cr.loadRegions().forEach(region -> {
            for (ChunkPos pos : region.getBeacons()) {
                // Runs the update once the world is fully loaded
                TickHandler.INSTANCE.addCallable(world, w -> {
                    ServerCompassService.updateArea((WorldServer) w, pos);
                    return null;
                });
            }
            region.close();
        });
        AELog.info("Finished converting old compass data in " + watch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        // Make sure to update the metadata
        metadata.setVersion(CompassMetadata.CURRENT_VERSION);
    }

    @Override
    public void onWorldStart() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onWorldStop() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }
}
