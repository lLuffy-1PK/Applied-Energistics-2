/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

package appeng.me.cache;


import appeng.api.AEApi;
import appeng.api.networking.*;
import appeng.api.networking.events.MENetworkBootingStatusChange;
import appeng.api.networking.events.MENetworkChannelChanged;
import appeng.api.networking.events.MENetworkControllerChange;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.pathing.ControllerState;
import appeng.api.networking.pathing.IPathingGrid;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.core.features.AEFeature;
import appeng.core.stats.IAdvancementTrigger;
import appeng.me.GridConnection;
import appeng.me.GridNode;
import appeng.me.pathfinding.*;
import appeng.tile.networking.TileController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.*;


public class PathGridCache implements IPathingGrid {

    private static final int BASE_TICKS = 20;
    private static final double POWER_DIVIDER = 128.0;
    private static final int COMPRESSED_CHANNEL_LIMIT = 9;

    private final List<PathSegment> active = new ArrayList<>();
    private final Set<TileController> controllers = new HashSet<>();
    private final Set<IGridNode> requireChannels = new HashSet<>();
    private final Set<IGridNode> blockDense = new HashSet<>();
    private final IGrid myGrid;

    private int channelsInUse = 0;
    private int channelsByBlocks = 0;
    private double channelPowerUsage = 0.0;
    private boolean recalculateControllerNextTick = true;
    private boolean updateNetwork = true;
    private boolean booting = false;
    private ControllerState controllerState = ControllerState.NO_CONTROLLER;
    private int ticksUntilReady = BASE_TICKS;
    private int lastChannels = 0;
    private Set<IPathItem> semiOpen = new HashSet<>();

    public PathGridCache(final IGrid grid) {
        this.myGrid = grid;
    }

    @Override
    public void onUpdateTick() {
        if (recalculateControllerNextTick) {
            recalcController();
        }
        if (updateNetwork) {
            updateNetworkStatus();
        }
        processActiveSegments();
    }

    /**
     * Обновление состояния сети.
     */
    private void updateNetworkStatus() {
        if (!booting) {
            myGrid.postEvent(new MENetworkBootingStatusChange());
        }
        booting = true;
        updateNetwork = false;
        setChannelsInUse(0);

        if (controllerState == ControllerState.NO_CONTROLLER) {
            int requiredChannels = calculateRequiredChannels();
            int usedChannels = requiredChannels;
            AEConfig config = AEConfig.instance();
            if (config.isFeatureEnabled(AEFeature.CHANNELS) && requiredChannels > config.getNormalChannelCapacity()) {
                usedChannels = 0;
            }
            int nodesCount = myGrid.getNodes().size();
            setChannelsInUse(usedChannels);
            ticksUntilReady = BASE_TICKS + Math.max(0, nodesCount / 100 - BASE_TICKS);
            setChannelsByBlocks(nodesCount * usedChannels);
            setChannelPowerUsage(getChannelsByBlocks() / POWER_DIVIDER);

            IGridNode pivot = myGrid.getPivot();
            if (pivot != null) {
                pivot.beginVisit(new AdHocChannelUpdater(usedChannels));
            }
        } else if (controllerState == ControllerState.CONTROLLER_CONFLICT) {
            ticksUntilReady = BASE_TICKS;
            IGridNode pivot = myGrid.getPivot();
            if (pivot != null) {
                pivot.beginVisit(new AdHocChannelUpdater(0));
            }
        } else { // CONTROLLER_ONLINE
            int nodesCount = myGrid.getNodes().size();
            ticksUntilReady = BASE_TICKS + Math.max(0, nodesCount / 100 - BASE_TICKS);
            Set<IPathItem> closedList = new HashSet<>();
            semiOpen = new HashSet<>();

            for (final IGridNode node : myGrid.getMachines(TileController.class)) {
                closedList.add((IPathItem) node);
                for (final IGridConnection connection : node.getConnections()) {
                    GridConnection gc = (GridConnection) connection;
                    if (!(gc.getOtherSide(node).getMachine() instanceof TileController)) {
                        List<IPathItem> openList = new ArrayList<>();
                        closedList.add(gc);
                        openList.add(gc);
                        gc.setControllerRoute((GridNode) node, true);
                        active.add(new PathSegment(this, openList, semiOpen, closedList));
                    }
                }
            }
        }
    }

    /**
     * Обработка активных сегментов путей.
     */
    private void processActiveSegments() {
        if (!active.isEmpty() || ticksUntilReady > 0) {
            Iterator<PathSegment> iterator = active.iterator();
            while (iterator.hasNext()) {
                PathSegment segment = iterator.next();
                if (segment.step()) {
                    segment.setDead(true);
                    iterator.remove();
                }
            }
            ticksUntilReady--;

            if (active.isEmpty() && ticksUntilReady <= 0) {
                if (controllerState == ControllerState.CONTROLLER_ONLINE && !controllers.isEmpty()) {
                    TileController controller = controllers.iterator().next();
                    IGridNode gridNode = controller.getGridNode(AEPartLocation.INTERNAL);
                    if (gridNode != null) {
                        gridNode.beginVisit(new ControllerChannelUpdater());
                    }
                }
                achievementPost();
                booting = false;
                setChannelPowerUsage(getChannelsByBlocks() / POWER_DIVIDER);
                myGrid.postEvent(new MENetworkBootingStatusChange());
            }
        }
    }

    @Override
    public void removeNode(final IGridNode gridNode, final IGridHost machine) {
        if (machine instanceof TileController) {
            controllers.remove(machine);
            recalculateControllerNextTick = true;
        }

        EnumSet<GridFlags> flags = gridNode.getGridBlock().getFlags();
        if (flags.contains(GridFlags.REQUIRE_CHANNEL)) {
            requireChannels.remove(gridNode);
        }
        if (flags.contains(GridFlags.CANNOT_CARRY_COMPRESSED)) {
            blockDense.remove(gridNode);
        }
        repath();
    }

    @Override
    public void addNode(final IGridNode gridNode, final IGridHost machine) {
        if (machine instanceof TileController) {
            controllers.add((TileController) machine);
            recalculateControllerNextTick = true;
        }

        EnumSet<GridFlags> flags = gridNode.getGridBlock().getFlags();
        if (flags.contains(GridFlags.REQUIRE_CHANNEL)) {
            requireChannels.add(gridNode);
        }
        if (flags.contains(GridFlags.CANNOT_CARRY_COMPRESSED)) {
            blockDense.add(gridNode);
        }
        repath();
    }

    @Override
    public void onSplit(final IGridStorage storageB) {

    }

    @Override
    public void onJoin(final IGridStorage storageB) {

    }

    @Override
    public void populateGridStorage(final IGridStorage storage) {

    }

    /**
     * Пересчёт состояния контроллера.
     */
    private void recalcController() {
        recalculateControllerNextTick = false;
        ControllerState oldState = controllerState;

        if (controllers.isEmpty()) {
            controllerState = ControllerState.NO_CONTROLLER;
        } else {
            TileController firstController = controllers.iterator().next();
            IGridNode startingNode = firstController.getGridNode(AEPartLocation.INTERNAL);
            if (startingNode == null) {
                controllerState = ControllerState.CONTROLLER_CONFLICT;
                return;
            }
            DimensionalCoord dc = startingNode.getGridBlock().getLocation();
            ControllerValidator validator = new ControllerValidator(dc.x, dc.y, dc.z);
            startingNode.beginVisit(validator);
            if (validator.isValid() && validator.getFound() == controllers.size()) {
                controllerState = ControllerState.CONTROLLER_ONLINE;
            } else {
                controllerState = ControllerState.CONTROLLER_CONFLICT;
            }
        }

        if (oldState != controllerState) {
            myGrid.postEvent(new MENetworkControllerChange());
        }
    }

    /**
     * Расчёт требуемого количества каналов.
     */
    private int calculateRequiredChannels() {
        semiOpen.clear();
        int depth = 0;
        for (final IGridNode node : requireChannels) {
            if (!semiOpen.contains((IPathItem) node)) {
                IGridBlock gridBlock = node.getGridBlock();
                EnumSet<GridFlags> flags = gridBlock.getFlags();
                if (flags.contains(GridFlags.COMPRESSED_CHANNEL) && !blockDense.isEmpty()) {
                    return COMPRESSED_CHANNEL_LIMIT;
                }
                depth++;
                if (flags.contains(GridFlags.MULTIBLOCK)) {
                    IGridMultiblock multiblock = (IGridMultiblock) gridBlock;
                    Iterator<IGridNode> it = multiblock.getMultiblockNodes();
                    while (it.hasNext()) {
                        semiOpen.add((IPathItem) it.next());
                    }
                }
            }
        }
        return depth;
    }

    /**
     * Обработка достижений.
     */
    private void achievementPost() {
        AEConfig config = AEConfig.instance();
        if (lastChannels != getChannelsInUse() && config.isFeatureEnabled(AEFeature.CHANNELS)) {
            IAdvancementTrigger currentBracket = getAchievementBracket(getChannelsInUse());
            IAdvancementTrigger lastBracket = getAchievementBracket(lastChannels);
            if (currentBracket != lastBracket && currentBracket != null) {
                for (final IGridNode node : requireChannels) {
                    EntityPlayer player = AEApi.instance().registries().players().findPlayer(node.getPlayerID());
                    if (player instanceof EntityPlayerMP) {
                        currentBracket.trigger((EntityPlayerMP) player);
                    }
                }
            }
        }
        lastChannels = getChannelsInUse();
    }

    private IAdvancementTrigger getAchievementBracket(final int channels) {
        if (channels < 8) {
            return null;
        }
        if (channels < 128) {
            return AppEng.instance().getAdvancementTriggers().getNetworkApprentice();
        }
        if (channels < 2048) {
            return AppEng.instance().getAdvancementTriggers().getNetworkEngineer();
        }
        return AppEng.instance().getAdvancementTriggers().getNetworkAdmin();
    }

    @MENetworkEventSubscribe
    void updateNodReq(final MENetworkChannelChanged ev) {
        final IGridNode gridNode = ev.node;
        if (gridNode.getGridBlock().getFlags().contains(GridFlags.REQUIRE_CHANNEL)) {
            requireChannels.add(gridNode);
        } else {
            requireChannels.remove(gridNode);
        }
        repath();
    }

    @Override
    public boolean isNetworkBooting() {
        return !booting && !active.isEmpty();
    }

    @Override
    public ControllerState getControllerState() {
        return this.controllerState;
    }

    @Override
    public void repath() {
        active.clear();
        setChannelsByBlocks(0);
        updateNetwork = true;
    }

    double getChannelPowerUsage() {
        return channelPowerUsage;
    }

    private void setChannelPowerUsage(final double usage) {
        channelPowerUsage = usage;
    }

    public int getChannelsByBlocks() {
        return channelsByBlocks;
    }

    public void setChannelsByBlocks(final int channels) {
        channelsByBlocks = channels;
    }

    public int getChannelsInUse() {
        return channelsInUse;
    }

    public void setChannelsInUse(final int channels) {
        channelsInUse = channels;
    }
}
