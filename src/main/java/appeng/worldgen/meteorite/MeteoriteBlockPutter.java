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

package appeng.worldgen.meteorite;


import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.BlockFlags;


public class MeteoriteBlockPutter {
    public boolean put(final World w, final BlockPos pos, final Block blk) {
        Block original = w.getBlockState(pos).getBlock();

        if (original == Blocks.BEDROCK || original == blk) {
            return false;
        }

        w.setBlockState(pos, blk.getDefaultState(), BlockFlags.SEND_TO_CLIENTS | BlockFlags.NO_OBSERVERS);
        return true;
    }

    public void put(final World w, final BlockPos pos, final IBlockState state) {
        if (w.getBlockState(pos) == Blocks.BEDROCK) {
            return;
        }

        w.setBlockState(pos, state, BlockFlags.SEND_TO_CLIENTS | BlockFlags.NO_OBSERVERS);
    }
}
