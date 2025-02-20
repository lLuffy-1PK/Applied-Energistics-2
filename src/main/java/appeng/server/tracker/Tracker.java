package appeng.server.tracker;

import appeng.api.networking.IGrid;
import appeng.me.Grid;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import java.util.LinkedList;
import java.util.Map;

public class Tracker {
    private final IGrid grid;
    private final LinkedList<Integer> timeUsages = new LinkedList<>();
    private final Map<String, SubTracker> subTrackers = new Object2ObjectLinkedOpenHashMap<>();
    private SubTracker currentSubTracker = null;
    private int timeUsageCache = 0;

    private long startTime = 0;

    public Tracker(final IGrid grid) {
        this.grid = grid;
    }

    public void startTracking() {
        startTime = System.nanoTime() / 1000;
    }

    public void endTracking() {
        updateTimeUsage((System.nanoTime() / 1000) - startTime);
    }

    public IGrid getGrid() {
        return grid;
    }

    public void startSubTracking(String name) {
        this.currentSubTracker = this.subTrackers.computeIfAbsent(name, (k) -> new SubTracker(name));
        this.currentSubTracker.startTracking();
    }

    public void endSubTracking() {
        if (this.currentSubTracker != null) {
            this.currentSubTracker.endTracking();
            this.currentSubTracker = null;
        }
    }

    public Map<String, SubTracker> getSubTrackers() {
        return this.subTrackers;
    }

    public int getTimeUsageAvg() {
        if (timeUsages.isEmpty()) {
            return 0;
        }

        return timeUsageCache / timeUsages.size();
    }

    public int getMaxTimeUsage() {
        if (timeUsages.isEmpty()) {
            return 0;
        }

        return timeUsages.stream()
                .mapToInt(usage -> usage)
                .max().getAsInt();
    }

    private void updateTimeUsage(final long time) {
        int t = (int) time;
        timeUsages.addFirst(t);
        timeUsageCache += t;

        if (timeUsages.size() > 1200) {
            timeUsageCache -= timeUsages.pollLast();
        }
    }

    public static class SubTracker {
        private final String name;
        private final IntArrayFIFOQueue timeUsages = new IntArrayFIFOQueue();
        private int timeUsageCache = 0;
        private long startTime = 0L;

        public SubTracker(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public void startTracking() {
            this.startTime = System.nanoTime() / 1000L;
        }

        public void endTracking() {
            this.updateTimeUsage(System.nanoTime() / 1000L - this.startTime);
        }

        public int getTimeUsageAvg() {
            return this.timeUsages.isEmpty() ? 0 : this.timeUsageCache / this.timeUsages.size();
        }

        private void updateTimeUsage(long time) {
            int t = (int) time;
            this.timeUsages.enqueueFirst(t);
            this.timeUsageCache += t;
            if (this.timeUsages.size() > 600) {
                this.timeUsageCache -= this.timeUsages.dequeueLastInt();
            }

        }
    }
}
