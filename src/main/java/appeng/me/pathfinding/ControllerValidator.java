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

package appeng.me.pathfinding;


import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridVisitor;
import appeng.core.AEConfig;
import appeng.tile.networking.TileController;
import net.minecraft.util.math.BlockPos;


public class ControllerValidator implements IGridVisitor {

    private boolean valid = true;
    private int found = 0;
    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;

    private final int maxSizeX;
    private final int maxSizeY;
    private final int maxSizeZ;

    public ControllerValidator(int x, int y, int z) {
        this.minX = this.maxX = x;
        this.minY = this.maxY = y;
        this.minZ = this.maxZ = z;
        maxSizeX = AEConfig.instance().getMaxControllerSizeX();
        maxSizeY = AEConfig.instance().getMaxControllerSizeY();
        maxSizeZ = AEConfig.instance().getMaxControllerSizeZ();
    }

    @Override
    public boolean visitNode(final IGridNode node) {
        if (!valid) return false;

        IGridHost host = node.getMachine();
        if (!(host instanceof TileController)) return false;

        TileController controller = (TileController) host;
        updateBounds(controller.getPos());

        if (!isWithinBounds()) {
            valid = false;
            return false;
        }
        found++;
        return true;
    }

    private void updateBounds(BlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        minX = Math.min(x, minX);
        maxX = Math.max(x, maxX);
        minY = Math.min(y, minY);
        maxY = Math.max(y, maxY);
        minZ = Math.min(z, minZ);
        maxZ = Math.max(z, maxZ);
    }

    private boolean isWithinBounds() {
        return (maxX - minX < maxSizeX) &&
                (maxY - minY < maxSizeY) &&
                (maxZ - minZ < maxSizeZ);
    }

    public boolean isValid() {
        return valid;
    }

    public int getFound() {
        return found;
    }
}

