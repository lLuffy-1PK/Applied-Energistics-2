/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.me;


import appeng.api.AEApi;
import appeng.api.networking.*;
import appeng.api.networking.events.MENetworkEvent;
import appeng.api.networking.events.MENetworkPostCacheConstruction;
import appeng.api.util.IReadOnlyCollection;
import appeng.core.worlddata.WorldData;
import appeng.hooks.TickHandler;
import appeng.tile.networking.TileController;
import appeng.util.ReadOnlyCollection;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;


public class Grid implements IGrid {
    private static final AtomicLong GRID_ID = new AtomicLong(0L);
    private final NetworkEventBus eventBus = new NetworkEventBus();
    private final Map<Class<? extends IGridHost>, MachineSet> machines = new Reference2ObjectOpenHashMap<>();
    private final Map<Class<? extends IGridCache>, GridCacheWrapper> caches = new Reference2ObjectOpenHashMap<>();
    private final long id;
    private GridNode pivot;
    private int priority; // how import is this network?
    private GridStorage myStorage;
    private GridCacheWrapper cachedResultLast;
    private GridCacheWrapper cachedResult;

    public Grid(final GridNode center) {
        this.id = GRID_ID.incrementAndGet();
        this.cachedResultLast = null;
        this.cachedResult = null;
        this.pivot = center;

        final Map<Class<? extends IGridCache>, IGridCache> myCaches = AEApi.instance().registries().gridCache().createCacheInstance(this);
        for (final Entry<Class<? extends IGridCache>, IGridCache> c : myCaches.entrySet()) {
            final Class<? extends IGridCache> key = c.getKey();
            final IGridCache value = c.getValue();
            final Class<? extends IGridCache> valueClass = value.getClass();

            this.eventBus.readClass(key, valueClass);
            this.caches.put(key, new GridCacheWrapper(value));
        }

        this.postEvent(new MENetworkPostCacheConstruction());

        TickHandler.INSTANCE.addNetwork(this);
        center.setGrid(this);
    }

    int getPriority() {
        return this.priority;
    }

    IGridStorage getMyStorage() {
        return this.myStorage;
    }

    Map<Class<? extends IGridCache>, GridCacheWrapper> getCaches() {
        return this.caches;
    }

    public Iterable<Class<? extends IGridHost>> getMachineClasses() {
        return this.machines.keySet();
    }

    int size() {
        int out = 0;
        for (final Collection<?> x : this.machines.values()) {
            out += x.size();
        }
        return out;
    }

    void remove(final GridNode gridNode) {
        for (final IGridCache c : this.caches.values()) {
            final IGridHost machine = gridNode.getMachine();
            c.removeNode(gridNode, machine);
        }

        final Class<? extends IGridHost> machineClass = gridNode.getMachineClass();
        final Set<IGridNode> nodes = this.machines.get(machineClass);
        if (nodes != null) {
            nodes.remove(gridNode);
        }

        gridNode.setGridStorage(null);

        if (this.pivot == gridNode) {
            IMachineSet controllers = this.getMachines(TileController.class);
            if (!controllers.isEmpty()) {
                this.pivot = (GridNode)controllers.iterator().next();
                return;
            }
            final Iterator<IGridNode> n = this.getNodes().iterator();
            if (n.hasNext()) {
                this.pivot = (GridNode) n.next();
            } else {
                this.pivot = null;
                TickHandler.INSTANCE.removeNetwork(this);
                this.myStorage.remove();
            }
        }
    }

    void add(final GridNode gridNode) {
        final Class<? extends IGridHost> mClass = gridNode.getMachineClass();

        MachineSet nodes = this.machines.get(mClass);
        if (nodes == null) {
            nodes = new MachineSet(mClass);
            this.machines.put(mClass, nodes);
            this.eventBus.readClass(mClass, mClass);
        }

        // handle loading grid storages.
        if (gridNode.getGridStorage() != null) {
            final GridStorage gs = gridNode.getGridStorage();
            final IGrid grid = gs.getGrid();

            if (grid == null) {
                this.myStorage = gs;
                this.myStorage.setGrid(this);

                for (final IGridCache gc : this.caches.values()) {
                    gc.onJoin(this.myStorage);
                }
            } else if (grid != this) {
                if (this.myStorage == null) {
                    this.myStorage = WorldData.instance().storageData().getNewGridStorage();
                    this.myStorage.setGrid(this);
                }

                final IGridStorage tmp = new GridStorage();
                if (!gs.hasDivided(this.myStorage)) {
                    gs.addDivided(this.myStorage);

                    for (final IGridCache gc : ((Grid) grid).caches.values()) {
                        gc.onSplit(tmp);
                    }

                    for (final IGridCache gc : this.caches.values()) {
                        gc.onJoin(tmp);
                    }
                }
            }
        } else if (this.myStorage == null) {
            this.myStorage = WorldData.instance().storageData().getNewGridStorage();
            this.myStorage.setGrid(this);
        }

        // update grid node...
        gridNode.setGridStorage(this.myStorage);

        // track node.
        nodes.add(gridNode);

        if (this.pivot != null && !(this.pivot.getMachine() instanceof TileController) && gridNode.getMachine() instanceof TileController) {
            this.pivot = gridNode;
        }

        for (final IGridCache cache : this.caches.values()) {
            final IGridHost machine = gridNode.getMachine();
            cache.addNode(gridNode, machine);
        }

        gridNode.getGridProxy().gridChanged();
        // postEventTo( gridNode, networkChanged );
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends IGridCache> C getCache(Class<? extends IGridCache> iface) {
        if (this.cachedResult != null && this.cachedResult.typeEquals(iface)) {
            return (C) this.cachedResult.getCache();
        } else if (this.cachedResultLast != null && this.cachedResultLast.typeEquals(iface)) {
            return (C) this.cachedResultLast.getCache();
        } else {
            this.cachedResultLast = this.cachedResult;
            this.cachedResult = (GridCacheWrapper) this.caches.get(iface);
            return this.cachedResult != null ? (C) this.cachedResult.getCache() : null;
        }
    }

    @Override
    public MENetworkEvent postEvent(final MENetworkEvent ev) {
        final MENetworkEvent ret = this.eventBus.postEvent(this, ev);
        return ret;
    }

    @Override
    public MENetworkEvent postEventTo(final IGridNode node, final MENetworkEvent ev) {
        return this.eventBus.postEventTo(this, (GridNode) node, ev);
    }

    @Override
    public IReadOnlyCollection<Class<? extends IGridHost>> getMachinesClasses() {
        final Set<Class<? extends IGridHost>> machineKeys = this.machines.keySet();

        return new ReadOnlyCollection<>(machineKeys);
    }

    @Override
    public IMachineSet getMachines(final Class<? extends IGridHost> c) {
        final MachineSet s = this.machines.get(c);
        if (s == null) {
            return new MachineSet(c);
        }
        return s;
    }

    @Override
    public IReadOnlyCollection<IGridNode> getNodes() {
        return new GridNodeCollection(this.machines);
    }

    @Override
    public boolean isEmpty() {
        return this.pivot == null;
    }

    @Override
    public IGridNode getPivot() {
        return this.pivot;
    }

    void setPivot(final GridNode pivot) {
        this.pivot = pivot;
        if (!(pivot.getMachine() instanceof TileController)) {
            IMachineSet controllers = this.getMachines(TileController.class);
            if (!controllers.isEmpty()) {
                this.pivot = (GridNode)controllers.iterator().next();
            }
        }
    }

    public void update() {
        for (final IGridCache gc : this.caches.values()) {
            // are there any nodes left?
            if (this.pivot != null) {
                gc.onUpdateTick();
            }
        }
    }

    void saveState() {
        for (final IGridCache c : this.caches.values()) {
            c.populateGridStorage(this.myStorage);
        }
    }

    public void setImportantFlag(final int i, final boolean publicHasPower) {
        final int flag = 1 << i;
        this.priority = (this.priority & ~flag) | (publicHasPower ? flag : 0);
    }

    public long getId() {
        return this.id;
    }
}
