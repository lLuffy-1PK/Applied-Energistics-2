package appeng.server.tracker;

import appeng.api.networking.IGrid;
import appeng.me.Grid;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.Map;

public class PerformanceTracker {
    public static final PerformanceTracker INSTANCE = new PerformanceTracker();
    private final Map<IGrid, Tracker> trackers = new Reference2ObjectOpenHashMap();
    private Tracker currentTracking = null;

    private PerformanceTracker() {

    }

    public void resetTracker() {
        trackers.clear();
    }

    public Map<IGrid, Tracker> getTrackers() {
        return this.trackers;
    }

    public void startTracking(IGrid g) {
        Tracker tracker = this.trackers.computeIfAbsent(g, (v) -> new Tracker(g));
        tracker.startTracking();
        this.currentTracking = tracker;
    }

    public void endTracking(IGrid g) {
        Tracker tracker = this.trackers.computeIfAbsent(g, (v) -> new Tracker(g));
        tracker.endTracking();
        this.currentTracking = null;
    }

    public void startSubTracking(String name) {
        if (this.currentTracking != null) {
            this.currentTracking.startSubTracking(name);
        }
    }

    public void endSubTracking() {
        if (this.currentTracking != null) {
            this.currentTracking.endSubTracking();
        }
    }
}
