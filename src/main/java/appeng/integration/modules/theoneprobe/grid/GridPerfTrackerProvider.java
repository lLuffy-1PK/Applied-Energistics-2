package appeng.integration.modules.theoneprobe.grid;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.me.Grid;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.server.tracker.PerformanceTracker;
import appeng.server.tracker.Tracker;
import appeng.tile.networking.TileController;
import java.util.Map;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class GridPerfTrackerProvider implements IProbeInfoProvider {

    private static final int GREEN_THRESHOLD_MICROSECOND = 500;
    private static final int YELLOW_THRESHOLD_MICROSECOND = 1000;
    private static final int RED_THRESHOLD_MICROSECOND = 2000;

    private static final int GREEN_THRESHOLD_MAX_MILLISECOND = 5;
    private static final int YELLOW_THRESHOLD_MAX_MILLISECOND = 10;
    private static final int RED_THRESHOLD_MAX_MILLISECOND = 20;

    private static final int GREEN_THRESHOLD_GRID_NODES = 2000;
    private static final int YELLOW_THRESHOLD_GRID_NODES = 3000;
    private static final int RED_THRESHOLD_GRID_NODES = 4000;

    private static final float SUB_TRACKER_GREEN_PERCENT = 0.3F;
    private static final float SUB_TRACKER_YELLOW_PERCENT = 0.5F;

    @Override
    public String getID() {
        return "appliedenergistics2:GridPerfTrackerProvider";
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo info, EntityPlayer player, World world, IBlockState state, IProbeHitData data) {
        TileEntity tile = world.getTileEntity(data.getPos());
        IGrid grid;

        if (!player.isCreative()) return;

        try {
            if (tile instanceof IGridProxyable) {
                IGridProxyable proxyable = (IGridProxyable) tile;
                AENetworkProxy proxy = proxyable.getProxy();
                grid = proxy.getGrid();
            } else if (tile instanceof IPartHost) {
                IPartHost host = (IPartHost) tile;
                IPart internal = host.getPart(AEPartLocation.INTERNAL);
                if (internal == null) {
                    return;
                }
                IGridNode node = internal.getGridNode();
                if (node == null) {
                    return;
                }
                grid = node.getGrid();
            } else {
                return;
            }
        } catch (GridAccessException e) {
            return;
        }

        if (grid instanceof Grid) {
            IGridNode pivot = grid.getPivot();
            Tracker tracker = PerformanceTracker.INSTANCE.getTrackers().get(grid);
            if (tracker != null) {
                int gridTime = tracker.getTimeUsageAvg();
                float gridMaxTime = tracker.getMaxTimeUsage() / 1000.0F;
                DimensionalCoord location = pivot.getGridBlock().getLocation();
                boolean isController = pivot.getMachine() instanceof TileController;

                info.text(String.format("Grid Pivot Pos: %sX:%s%s %sY:%s%s %sZ:%s%s %s(C)%s",
                        TextFormatting.RED, location.x, TextFormatting.RESET,
                        TextFormatting.GREEN, location.y, TextFormatting.RESET,
                        TextFormatting.BLUE, location.z, TextFormatting.RESET,
                        isController ? TextFormatting.GREEN : TextFormatting.RED, TextFormatting.RESET));

                info.text(String.format("Grid ID: %s%s%s",
                        TextFormatting.BLUE, ((Grid) grid).getId(), TextFormatting.RESET));

                int gridNodes = grid.getNodes().size();
                TextFormatting nodesColor;
                if (gridNodes < GREEN_THRESHOLD_GRID_NODES) {
                    nodesColor = TextFormatting.GREEN;
                } else if (gridNodes < YELLOW_THRESHOLD_GRID_NODES) {
                    nodesColor = TextFormatting.YELLOW;
                } else if (gridNodes < RED_THRESHOLD_GRID_NODES) {
                    nodesColor = TextFormatting.RED;
                } else {
                    nodesColor = TextFormatting.DARK_RED;
                }
                info.text(String.format("Grid Nodes: %s%s%s", nodesColor, gridNodes, TextFormatting.RESET));

                TextFormatting cpuColor;
                if (gridTime < GREEN_THRESHOLD_MICROSECOND) {
                    cpuColor = TextFormatting.GREEN;
                } else if (gridTime < YELLOW_THRESHOLD_MICROSECOND) {
                    cpuColor = TextFormatting.YELLOW;
                } else if (gridTime < RED_THRESHOLD_MICROSECOND) {
                    cpuColor = TextFormatting.RED;
                } else {
                    cpuColor = TextFormatting.DARK_RED;
                }

                TextFormatting maxCpuColor;
                if (gridMaxTime < GREEN_THRESHOLD_MAX_MILLISECOND) {
                    maxCpuColor = TextFormatting.GREEN;
                } else if (gridMaxTime < YELLOW_THRESHOLD_MAX_MILLISECOND) {
                    maxCpuColor = TextFormatting.YELLOW;
                } else if (gridMaxTime < RED_THRESHOLD_MAX_MILLISECOND) {
                    maxCpuColor = TextFormatting.RED;
                } else {
                    maxCpuColor = TextFormatting.DARK_RED;
                }

                info.text(String.format("Grid CPU Avg/Max: %s%d%sμs / %s%.2f%sms",
                        cpuColor, gridTime, TextFormatting.RESET,
                        maxCpuColor, gridMaxTime, TextFormatting.RESET));

                if (gridTime > 0) {
                    IProbeInfo horizontal = info.horizontal();
                    IProbeInfo leftInfo = newVertical(horizontal);
                    IProbeInfo rightInfo = newVertical(horizontal);

                    int subGridCacheUsageAvg = 0;
                    Map<String, Tracker.SubTracker> subTrackers = tracker.getSubTrackers();
                    for (Map.Entry<String, Tracker.SubTracker> entry : subTrackers.entrySet()) {
                        int timeUsageAvg = entry.getValue().getTimeUsageAvg();
                        subGridCacheUsageAvg += timeUsageAvg;
                        float percent = (float) timeUsageAvg / gridTime;
                        TextFormatting subColor;
                        if (percent < SUB_TRACKER_GREEN_PERCENT) {
                            subColor = TextFormatting.GREEN;
                        } else if (percent < SUB_TRACKER_YELLOW_PERCENT) {
                            subColor = TextFormatting.YELLOW;
                        } else {
                            subColor = TextFormatting.RED;
                        }
                        leftInfo.text(entry.getKey() + ": ");
                        rightInfo.text(String.format("%s%d%sμs", subColor, timeUsageAvg, TextFormatting.RESET));
                    }

                    int miscUsageAvg = gridTime - subGridCacheUsageAvg;
                    float miscPercent = (float) miscUsageAvg / gridTime;
                    TextFormatting miscColor;
                    if (miscPercent < SUB_TRACKER_GREEN_PERCENT) {
                        miscColor = TextFormatting.GREEN;
                    } else if (miscPercent < SUB_TRACKER_YELLOW_PERCENT) {
                        miscColor = TextFormatting.YELLOW;
                    } else {
                        miscColor = TextFormatting.RED;
                    }
                    leftInfo.text("Misc: ");
                    rightInfo.text(String.format("%s%d%sμs", miscColor, miscUsageAvg, TextFormatting.RESET));
                }
            }
        }
    }

    private static IProbeInfo newVertical(IProbeInfo probeInfo) {
        return probeInfo.vertical(probeInfo.defaultLayoutStyle().spacing(0));
    }
}
