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


import appeng.api.AEApi;
import appeng.api.definitions.IBlockDefinition;
import appeng.api.definitions.IMaterials;
import appeng.block.storage.BlockSkyChest;
import appeng.core.AEConfig;
import appeng.core.features.AEFeature;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.StructureBoundingBoxUtils.BoundingBoxClamper;
import appeng.worldgen.meteorite.fallout.*;
import appeng.worldgen.meteorite.settings.CraterType;
import appeng.worldgen.meteorite.settings.PlacedMeteoriteSettings;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public final class MeteoritePlacer {
    private static final double METEOR_UPPER_CURVATURE = 1.4D;
    private static final double METEOR_LOWER_CURVATURE = 0.8D;
    private static final double CRATER_CURVATURE = 0.02D;
    private static final int CRATER_RADIUS = 5;
    private static final int SKYSTONE_SPAWN_LIMIT = 12;

    private final IBlockDefinition skyStoneDefinition;
    private final MeteoriteBlockPutter putter = new MeteoriteBlockPutter();
    private final BoundingBoxClamper clamper;
    private final StructureBoundingBox boundingBox;
    private final World world;
    private final Random random;
    private final int x;
    private final int y;
    private final int z;
    private final double meteoriteSize;
    private final double squaredMeteoriteSize;
    private final double squaredCraterSize;
    private final boolean placeCrater;
    private final CraterType craterType;
    private final boolean pureCrater;
    private final boolean craterLake;
    private final Fallout type;

    public MeteoritePlacer(World world, PlacedMeteoriteSettings settings,
                           Random random, StructureBoundingBox structureBB) {
        this.skyStoneDefinition = AEApi.instance().definitions().blocks().skyStoneBlock();
        this.clamper = new BoundingBoxClamper(structureBB);
        this.boundingBox = structureBB;
        this.world = world;
        this.random = random;
        this.x = settings.getPos().getX();
        this.y = settings.getPos().getY();
        this.z = settings.getPos().getZ();
        this.meteoriteSize = settings.getMeteoriteRadius();
        this.placeCrater = settings.shouldPlaceCrater();
        this.craterType = settings.getCraterType();
        this.pureCrater = settings.isPureCrater();
        this.craterLake = settings.isCraterLake();
        this.squaredMeteoriteSize = this.meteoriteSize * this.meteoriteSize;

        double craterSize = this.meteoriteSize * 2 + CRATER_RADIUS;
        this.squaredCraterSize = craterSize * craterSize;

        var localCenter = new BlockPos(
                boundingBox.minX + boundingBox.getXSize() / 2,
                boundingBox.minY + boundingBox.getYSize() / 2,
                boundingBox.minZ + boundingBox.getZSize() / 2);
        this.type = getFallout(world, localCenter, settings.getFallout());
    }

    public static void place(World world, PlacedMeteoriteSettings settings,
                             Random random, StructureBoundingBox structureBB) {
        var placer = new MeteoritePlacer(world, settings, random, structureBB);
        placer.place();
    }

    private void place() {
        if (placeCrater) {
            this.placeCrater();
        }

        this.placeMeteorite();

        // collapse blocks...
        if (placeCrater) {
            this.decay();
        }
        if (craterLake) {
            this.placeCraterLake();
        }
    }

    /**
     * Place crater blocks for this bounding box.
     * Crater carving equation:
     * y = (y_0 - meteoriteSize + 1 + falloutAdjustment) + CRATER_CURVATURE_FACTOR * ((x - x_0)^2 + (z - z_0)^2)
     */
    private void placeCrater() {
        final int maxY = 255;
        var pos = new MutableBlockPos();
        var filler = craterType.getFiller().getDefaultState();

        for (int j = y - CRATER_RADIUS; j <= maxY; j++) {
            pos.setY(j);

            for (int i = boundingBox.minX; i <= boundingBox.maxX; i++) {
                pos.setPos(i, pos.getY(), pos.getZ());

                for (int k = boundingBox.minZ; k <= boundingBox.maxZ; k++) {
                    pos.setPos(pos.getX(), pos.getY(), k);
                    final double dx = i - x;
                    final double dz = k - z;
                    final double h = y - this.meteoriteSize + 1 + this.type.adjustCrater();

                    final double distanceFrom = dx * dx + dz * dz;

                    if (j > h + CRATER_CURVATURE * distanceFrom) {
                        var currBlock = world.getBlockState(pos);
                        if (craterType != CraterType.NORMAL && j < y && currBlock.getMaterial().isSolid()) {
                            this.putter.put(world, pos, filler);
                        } else {
                            this.putter.put(world, pos, Platform.AIR_BLOCK);
                        }
                    }
                }
            }
        }

        for (final var e : world.getEntitiesWithinAABB(EntityItem.class,
                        new AxisAlignedBB(
                                clamper.minX(x - 30), y - CRATER_RADIUS, clamper.minZ(z - 30),
                                clamper.maxX(x + 30), y + 30, clamper.maxZ(z + 30)))) {
            e.setDead();
        }
    }

    private void placeMeteorite() {
        // spawn meteor
        skyStoneDefinition.maybeBlock().ifPresent(this::placeMeteoriteSkyStone);

        // If the meteorite's center is within the BB of the current placer, place the chest
        var chestPos = new BlockPos(x, y, z);
        if (boundingBox.isVecInside(chestPos)) {
            placeChest(chestPos);
        }
    }

    private void placeChest(BlockPos pos) {
        if (!AEConfig.instance().isFeatureEnabled(AEFeature.SPAWN_PRESSES_IN_METEORITES)) {
            return;
        }
        final var chest = AEApi.instance().definitions().blocks().skyStoneChest().maybeBlock();
        chest.ifPresent(block -> this.putter.put(world, pos,
                block.getDefaultState().withProperty(BlockSkyChest.NATURAL, true)));

        final TileEntity te = world.getTileEntity(pos);
        final InventoryAdaptor ap = InventoryAdaptor.getAdaptor(te, EnumFacing.UP);
        if (ap != null) {
            final ArrayList<ItemStack> pressTypes = new ArrayList<>(4);
            final IMaterials materials = AEApi.instance().definitions().materials();
            materials.calcProcessorPress().maybeStack(1).ifPresent(pressTypes::add);
            materials.engProcessorPress().maybeStack(1).ifPresent(pressTypes::add);
            materials.logicProcessorPress().maybeStack(1).ifPresent(pressTypes::add);
            materials.siliconPress().maybeStack(1).ifPresent(pressTypes::add);

            final int pressCount = 1 + random.nextInt(3);
            final int removeCount = Math.max(0, pressTypes.size() - pressCount);

            // Make pressTypes contain pressCount random presses
            for (int zz = 0; zz < removeCount; zz++) {
                pressTypes.remove(random.nextInt(pressTypes.size()));
            }

            for (ItemStack toAdd : pressTypes) {
                ap.addItems(toAdd);
            }

            final List<ItemStack> nuggetLoot = new ArrayList<>();
            nuggetLoot.addAll(OreDictionary.getOres("nuggetIron"));
            nuggetLoot.addAll(OreDictionary.getOres("nuggetCopper"));
            nuggetLoot.addAll(OreDictionary.getOres("nuggetTin"));
            nuggetLoot.addAll(OreDictionary.getOres("nuggetSilver"));
            nuggetLoot.addAll(OreDictionary.getOres("nuggetLead"));
            nuggetLoot.addAll(OreDictionary.getOres("nuggetPlatinum"));
            nuggetLoot.addAll(OreDictionary.getOres("nuggetNickel"));
            nuggetLoot.addAll(OreDictionary.getOres("nuggetAluminium"));
            nuggetLoot.addAll(OreDictionary.getOres("nuggetElectrum"));
            nuggetLoot.add(new ItemStack(net.minecraft.init.Items.GOLD_NUGGET));
            final int secondaryCount = 1 + random.nextInt(3);
            for (int zz = 0; zz < secondaryCount; zz++) {
                switch (random.nextInt(3)) {
                    case 0:
                        final int amount = 1 + random.nextInt(SKYSTONE_SPAWN_LIMIT);
                        skyStoneDefinition.maybeStack(amount).ifPresent(ap::addItems);
                        break;
                    case 1:
                        ItemStack nugget = nuggetLoot.get(random.nextInt(nuggetLoot.size()));
                        if (nugget != null) {
                            nugget = nugget.copy();
                            nugget.setCount(1 + random.nextInt(12));
                            ap.addItems(nugget);
                        }
                        break;
                    case 2:
                    default:
                        // Add nothing
                        break;
                }
            }
        }
    }

    /**
     * Place meteor blocks for this bounding box.
     * Meteor shape equations -
     * Lower half: 0.7 * (x - x_0)^2 + METEOR_UPPER_CURVATURE * (y - y_0)^2 + 0.7 * (z - z_0)^2
     * Upper half: 0.7 * (x - x_0)^2 + METEOR_LOWER_CURVATURE * (y - y_0)^2 + 0.7 * (z - z_0)^2
     */
    private void placeMeteoriteSkyStone(Block block) {
        final int meteorXLength = clamper.minX(x - Constants.MAX_METEOR_RADIUS);
        final int meteorXHeight = clamper.maxX(x + Constants.MAX_METEOR_RADIUS);
        final int meteorZLength = clamper.minZ(z - Constants.MAX_METEOR_RADIUS);
        final int meteorZHeight = clamper.maxZ(z + Constants.MAX_METEOR_RADIUS);

        MutableBlockPos pos = new MutableBlockPos();
        for (int i = meteorXLength; i <= meteorXHeight; i++) {
            pos.setPos(i, pos.getY(), pos.getZ());

            for (int j = y - Constants.MAX_METEOR_RADIUS; j < y + Constants.MAX_METEOR_RADIUS; j++) {
                pos.setY(j);

                for (int k = meteorZLength; k <= meteorZHeight; k++) {
                    pos.setPos(pos.getX(), pos.getY(), k);
                    final double dx = i - x;
                    final double dy = j - y;
                    final double dz = k - z;

                    if (dx * dx * 0.7
                            + dy * dy * (j > y ? METEOR_UPPER_CURVATURE : METEOR_LOWER_CURVATURE)
                            + dz * dz * 0.7 < this.squaredMeteoriteSize) {
                        this.putter.put(world, pos, block);
                    }
                }
            }
        }
    }

    private void decay() {
        double randomShit = 0;

        final int meteorXLength = clamper.minX(x - 30);
        final int meteorXHeight = clamper.maxX(x + 30);
        final int meteorZLength = clamper.minZ(z - 30);
        final int meteorZHeight = clamper.maxZ(z + 30);

        MutableBlockPos pos = new MutableBlockPos();
        MutableBlockPos posUp = new MutableBlockPos();
        MutableBlockPos posDown = new MutableBlockPos();

        for (int i = meteorXLength; i <= meteorXHeight; i++) {
            pos.setPos(i, pos.getY(), pos.getZ());
            posUp.setPos(i, posUp.getY(), posUp.getZ());
            posDown.setPos(i, posDown.getY(), posDown.getZ());
            for (int k = meteorZLength; k <= meteorZHeight; k++) {
                pos.setPos(pos.getX(), pos.getY(), k);
                posUp.setPos(posUp.getX(), posUp.getY(), k);
                posDown.setPos(posDown.getX(), posDown.getY(), k);
                for (int j = y - Constants.MAX_METEOR_RADIUS + 1; j < y + 30; j++) {
                    pos.setY(j);
                    posUp.setY(j + 1);
                    posDown.setY(j - 1);
                    var state = world.getBlockState(pos);

                    if (this.pureCrater && state.getBlock() == craterType.getFiller()) {
                        continue;
                    }

                    final var upperBlockState = world.getBlockState(posUp);
                    if (state.getMaterial().isReplaceable()) {
                        if (upperBlockState.getMaterial() != Material.AIR) {
                            world.setBlockState(pos, upperBlockState);
                        } else if (randomShit < 100 * this.squaredCraterSize) {
                            final double dx = i - x;
                            final double dy = j - y;
                            final double dz = k - z;
                            final double dist = dx * dx + dy * dy + dz * dz;

                            final var lowerBlockState = world.getBlockState(posDown);
                            if (!lowerBlockState.getMaterial().isReplaceable()) {
                                final double extraRange = random.nextDouble() * 0.6;
                                final double height = this.squaredCraterSize * (extraRange + 0.2)
                                        - Math.abs(dist - this.squaredCraterSize * 1.7);

                                if (lowerBlockState.getMaterial() != Material.AIR
                                        && height > 0 && random.nextDouble() > 0.6) {
                                    randomShit++;
                                    this.type.getRandomFall(world, pos);
                                }
                            }
                        }
                    } else if (upperBlockState.getMaterial() == Material.AIR && random.nextDouble() > 0.4) {
                        // decay.
                        final double dx = i - x;
                        final double dy = j - y;
                        final double dz = k - z;
                        final double dist = dx * dx + dy * dy + dz * dz;

                        if (dist < this.squaredCraterSize * 1.6) {
                            this.type.getRandomInset(world, pos);
                        }
                    }
                }
            }
        }
    }

    /**
     * If it found any water at or below sea level, it will replace any air blocks below the sea level with water.
     */
    private void placeCraterLake() {
        final int maxY = world.getSeaLevel() - 1;
        MutableBlockPos pos = new MutableBlockPos();
        Chunk currentChunk;

        for (int currentX = boundingBox.minX; currentX <= boundingBox.maxX; currentX++) {
            pos.setPos(currentX, pos.getY(), pos.getZ());

            for (int currentZ = boundingBox.minZ; currentZ <= boundingBox.maxZ; currentZ++) {
                pos.setPos(pos.getX(), pos.getY(), currentZ);
                currentChunk = world.getChunk(pos);

                for (int currentY = y - CRATER_RADIUS; currentY <= maxY; currentY++) {
                    pos.setY(currentY);

                    final double dx = currentX - x;
                    final double dz = currentZ - z;
                    final double h = y - this.meteoriteSize + 1 + this.type.adjustCrater();

                    final double distanceFrom = dx * dx + dz * dz;

                    if (currentY > h + distanceFrom * 0.02) {
                        var currentBlock = currentChunk.getBlockState(pos);
                        if (currentBlock.getMaterial() == Material.AIR) {
                            this.putter.put(world, pos, Blocks.WATER);

                            if (currentY == maxY) {
                                world.scheduleUpdate(pos, Blocks.WATER, 0);
                            }
                        }
                    } else if (maxY + (maxY - currentY) * 2 + 2 > h + distanceFrom * 0.02) {
                        pillarDownSlopeBlocks(currentChunk, pos);
                    }
                }
            }
        }
    }

    private void pillarDownSlopeBlocks(Chunk currentChunk, BlockPos blockPos) {
        MutableBlockPos enclosingBlockPos = new MutableBlockPos(blockPos);

        for (int i = 0; i < 20; i++) {
            if (placeEnclosingBlock(currentChunk, enclosingBlockPos)) {
                break;
            }
            enclosingBlockPos.move(EnumFacing.DOWN);
        }
    }

    private boolean placeEnclosingBlock(Chunk currentChunk, MutableBlockPos enclosingBlockPos) {
        var currentState = currentChunk.getBlockState(enclosingBlockPos);
        if (currentState.getMaterial() == Material.AIR
                || (!currentState.getMaterial().isLiquid()
                    && currentState.getMaterial().isReplaceable())) {
            if (craterType == CraterType.LAVA && random.nextFloat() < 0.075f) {
                this.putter.put(world, enclosingBlockPos, Blocks.MAGMA);
            } else {
                this.type.getRandomFall(world, enclosingBlockPos);
            }
        } else {
            return true;
        }
        return false;
    }

    private Fallout getFallout(World world, BlockPos pos, FalloutMode mode) {
        return switch (mode) {
            case SAND -> new FalloutSand(world, pos, this.putter, skyStoneDefinition, random);
            case TERRACOTTA -> new FalloutCopy(world, pos, this.putter, skyStoneDefinition, random);
            case ICE_SNOW -> new FalloutSnow(world, pos, this.putter, skyStoneDefinition, random);
            default -> new Fallout(this.putter, skyStoneDefinition, random);
        };
    }
}
