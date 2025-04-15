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

package appeng.worldgen;


import appeng.api.AEApi;
import appeng.api.definitions.IBlockDefinition;
import appeng.api.definitions.IBlocks;
import appeng.api.definitions.IMaterials;
import appeng.block.storage.BlockSkyChest;
import appeng.core.AEConfig;
import appeng.core.features.AEFeature;
import appeng.core.worlddata.WorldData;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.worldgen.meteorite.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;


public final class MeteoritePlacer {
    private static final int SKYSTONE_SPAWN_LIMIT = 12;

    private static final long SEED_OFFSET_CHEST_LOOT = 1;
    private static final long SEED_OFFSET_DECAY = 2;

    private static final Collection<Block> validSpawn = new HashSet<>();
    private static final Collection<Block> invalidSpawn = new HashSet<>();
    private static IBlockDefinition skyChestDefinition;
    private static IBlockDefinition skyStoneDefinition;

    private final IMeteoriteWorld world;
    private final long seed;
    private final int x, y, z;
    private final int skyMode;
    private final double meteoriteSize;
    private final double craterSize;
    private final double squaredMeteoriteSize;
    private final double squaredCraterSize;
    private final MeteoriteBlockPutter putter = new MeteoriteBlockPutter();
    private final NBTTagCompound settings;
    private Fallout type;

    private static void initializeSpawnLists() {
        if (validSpawn.isEmpty()) {
            final IBlocks blocks = AEApi.instance().definitions().blocks();
            skyChestDefinition = blocks.skyStoneChest();
            skyStoneDefinition = blocks.skyStoneBlock();

            validSpawn.clear();
            validSpawn.add(Blocks.STONE);
            validSpawn.add(Blocks.COBBLESTONE);
            validSpawn.add(Blocks.GRASS);
            validSpawn.add(Blocks.SAND);
            validSpawn.add(Blocks.DIRT);
            validSpawn.add(Blocks.GRAVEL);
            validSpawn.add(Blocks.NETHERRACK);
            validSpawn.add(Blocks.IRON_ORE);
            validSpawn.add(Blocks.GOLD_ORE);
            validSpawn.add(Blocks.DIAMOND_ORE);
            validSpawn.add(Blocks.REDSTONE_ORE);
            validSpawn.add(Blocks.HARDENED_CLAY);
            validSpawn.add(Blocks.ICE);
            validSpawn.add(Blocks.SNOW);
            validSpawn.add(Blocks.STAINED_HARDENED_CLAY);

            invalidSpawn.clear();
            skyStoneDefinition.maybeBlock().ifPresent(invalidSpawn::add);
            invalidSpawn.add(Blocks.PLANKS);
            invalidSpawn.add(Blocks.IRON_DOOR);
            invalidSpawn.add(Blocks.IRON_BARS);
            invalidSpawn.add(Blocks.OAK_DOOR);
            invalidSpawn.add(Blocks.ACACIA_DOOR);
            invalidSpawn.add(Blocks.BIRCH_DOOR);
            invalidSpawn.add(Blocks.DARK_OAK_DOOR);
            invalidSpawn.add(Blocks.JUNGLE_DOOR);
            invalidSpawn.add(Blocks.SPRUCE_DOOR);
            invalidSpawn.add(Blocks.BRICK_BLOCK);
            invalidSpawn.add(Blocks.CLAY);
            invalidSpawn.add(Blocks.WATER);
            invalidSpawn.add(Blocks.LOG);
            invalidSpawn.add(Blocks.LOG2);
        }
    }

    public MeteoritePlacer(final IMeteoriteWorld world, final long seed, final int x, final int y, final int z) {
        initializeSpawnLists();
        this.seed = seed;
        Random rng = new Random(seed);
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.meteoriteSize = (rng.nextDouble() * 6.0) + 2;
        this.craterSize = this.meteoriteSize * 2 + 5;
        this.squaredMeteoriteSize = this.meteoriteSize * this.meteoriteSize;
        this.squaredCraterSize = this.craterSize * this.craterSize;

        this.type = new Fallout(this.putter, skyStoneDefinition);

        int skyMode = 0;
        for (int i = x - 15; i < x + 15; i++) {
            for (int j = y - 15; j < y + 11; j++) {
                for (int k = z - 15; k < z + 15; k++) {
                    if (world.canBlockSeeTheSky(i, j, k)) {
                        skyMode++;
                    }
                }
            }
        }
        boolean solid = true;
        for (int j = y - 15; j < y - 1; j++) {
            if (world.getBlock(x, j, z) == Platform.AIR_BLOCK) {
                solid = false;
            }
        }
        if (!solid) {
            skyMode = 0;
        }
        this.skyMode = skyMode;

        Block blk = world.getBlock(x, y, z);
        this.settings = new NBTTagCompound();
        this.settings.setLong("seed", seed);
        this.settings.setInteger("x", x);
        this.settings.setInteger("y", y);
        this.settings.setInteger("z", z);
        this.settings.setInteger("blk", Block.getIdFromBlock(blk));
        this.settings.setInteger("skyMode", skyMode);

        this.settings.setDouble("real_sizeOfMeteorite", this.meteoriteSize);
        this.settings.setDouble("realCrater", this.craterSize);
        this.settings.setDouble("sizeOfMeteorite", this.squaredMeteoriteSize);
        this.settings.setDouble("crater", this.squaredCraterSize);

        this.settings.setBoolean("lava", rng.nextFloat() > 0.9F);
    }

    public MeteoritePlacer(final IMeteoriteWorld world, final NBTTagCompound meteoriteBlob) {
        initializeSpawnLists();
        Random rng = new Random();
        this.settings = meteoriteBlob;
        long dataSeed = meteoriteBlob.getLong("seed");
        // Meteor generated without a pre-set seed, from an older version
        if (dataSeed == 0) {
            // Generate a position-based seed
            Platform.seedFromGrid(
                    rng, world.getWorld().getSeed(), meteoriteBlob.getInteger("x"), meteoriteBlob.getInteger("z"));
            while (dataSeed == 0) {
                dataSeed = rng.nextLong();
            }
        }
        this.seed = dataSeed;
        rng.setSeed(dataSeed);

        this.world = world;
        this.x = this.settings.getInteger("x");
        this.y = this.settings.getInteger("y");
        this.z = this.settings.getInteger("z");

        this.type = new Fallout(this.putter, skyStoneDefinition);

        this.meteoriteSize = this.settings.getDouble("real_sizeOfMeteorite");
        this.craterSize = this.settings.getDouble("realCrater");
        this.squaredMeteoriteSize = this.settings.getDouble("sizeOfMeteorite");
        this.squaredCraterSize = this.settings.getDouble("crater");
        this.skyMode = this.settings.getInteger("skyMode");
    }

    void spawnMeteorite() {
        final Block blk = Block.getBlockById(this.settings.getInteger("blk"));

        if (blk == Blocks.SAND) {
            this.type = new FalloutSand(world, x, y, z, this.putter, skyStoneDefinition);
        } else if (blk == Blocks.HARDENED_CLAY) {
            this.type = new FalloutCopy(world, x, y, z, this.putter, skyStoneDefinition);
        } else if (blk == Blocks.ICE || blk == Blocks.SNOW) {
            this.type = new FalloutSnow(world, x, y, z, this.putter, skyStoneDefinition);
        }

        // Crater
        if (skyMode > 10) {
            this.placeCrater(world, x, y, z);
        }

        this.placeMeteorite(world, x, y, z);

        // collapse blocks...
        if (skyMode > 3) {
            this.decay(world, x, y, z);
        }

        world.done();
    }

    private void placeCrater(final IMeteoriteWorld w, final int x, final int y, final int z) {
        final boolean lava = this.settings.getBoolean("lava");

        final int maxY = 255;
        final int minX = w.minX(x - 200);
        final int maxX = w.maxX(x + 200);
        final int minZ = w.minZ(z - 200);
        final int maxZ = w.maxZ(z + 200);

        for (int j = y - 5; j < maxY; j++) {
            boolean changed = false;

            for (int i = minX; i < maxX; i++) {
                for (int k = minZ; k < maxZ; k++) {
                    final double dx = i - x;
                    final double dz = k - z;
                    final double h = y - this.meteoriteSize + 1 + this.type.adjustCrater();

                    final double distanceFrom = dx * dx + dz * dz;

                    if (j > h + distanceFrom * 0.02) {
                        if (lava && j < y && w.getBlockState(i, j, k).getMaterial().isSolid()) {
                            if (j > h + distanceFrom * 0.02) {
                                this.putter.put(w, i, j, k, Blocks.LAVA);
                            }
                        } else {
                            changed = this.putter.put(w, i, j, k, Platform.AIR_BLOCK) || changed;
                        }
                    }
                }
            }
        }

        for (final Object o : w.getWorld()
                .getEntitiesWithinAABB(EntityItem.class,
                        new AxisAlignedBB(
                                w.minX(x - 30), y - 5, w.minZ(z - 30), w.maxX(x + 30), y + 30, w.maxZ(z + 30)))) {
            final Entity e = (Entity) o;
            e.setDead();
        }
    }

    private void placeMeteorite(final IMeteoriteWorld w, final int x, final int y, final int z) {

        // spawn meteor
        skyStoneDefinition.maybeBlock().ifPresent(block -> this.placeMeteoriteSkyStone(w, x, y, z, block));

        if (AEConfig.instance().isFeatureEnabled(AEFeature.SPAWN_PRESSES_IN_METEORITES)) {
            skyChestDefinition.maybeBlock().ifPresent(block -> this.putter.put(w, x, y, z,
                            block.getDefaultState().withProperty(BlockSkyChest.NATURAL, true)));

            final TileEntity te = w.getTileEntity(x, y, z);
            final InventoryAdaptor ap = InventoryAdaptor.getAdaptor(te, EnumFacing.UP);
            if (ap != null) {
                final Random lootRng = new Random(this.seed + SEED_OFFSET_CHEST_LOOT);

                final ArrayList<ItemStack> pressTypes = new ArrayList<>(4);
                final IMaterials materials = AEApi.instance().definitions().materials();
                materials.calcProcessorPress().maybeStack(1).ifPresent(pressTypes::add);
                materials.engProcessorPress().maybeStack(1).ifPresent(pressTypes::add);
                materials.logicProcessorPress().maybeStack(1).ifPresent(pressTypes::add);
                materials.siliconPress().maybeStack(1).ifPresent(pressTypes::add);

                final int pressCount = 1 + lootRng.nextInt(3);
                final int removeCount = Math.max(0, pressTypes.size() - pressCount);

                // Make pressTypes contain pressCount random presses
                for (int zz = 0; zz < removeCount; zz++) {
                    pressTypes.remove(lootRng.nextInt(pressTypes.size()));
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
                final int secondaryCount = 1 + lootRng.nextInt(3);
                for (int zz = 0; zz < secondaryCount; zz++) {
                    switch (lootRng.nextInt(3)) {
                        case 0:
                            final int amount = 1 + lootRng.nextInt(SKYSTONE_SPAWN_LIMIT);
                            skyStoneDefinition.maybeStack(amount).ifPresent(ap::addItems);
                            break;
                        case 1:
                            ItemStack nugget = nuggetLoot.get(lootRng.nextInt(nuggetLoot.size()));
                            if (nugget != null) {
                                nugget = nugget.copy();
                                nugget.setCount(1 + lootRng.nextInt(12));
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
    }

    private void placeMeteoriteSkyStone(IMeteoriteWorld w, int x, int y, int z, Block block) {
        final int meteorXLength = w.minX(x - 8);
        final int meteorXHeight = w.maxX(x + 8);
        final int meteorZLength = w.minZ(z - 8);
        final int meteorZHeight = w.maxZ(z + 8);

        for (int i = meteorXLength; i < meteorXHeight; i++) {
            for (int j = y - 8; j < y + 8; j++) {
                for (int k = meteorZLength; k < meteorZHeight; k++) {
                    final double dx = i - x;
                    final double dy = j - y;
                    final double dz = k - z;

                    if (dx * dx * 0.7 + dy * dy * (j > y ? 1.4 : 0.8) + dz * dz * 0.7 < this.squaredMeteoriteSize) {
                        this.putter.put(w, i, j, k, block);
                    }
                }
            }
        }
    }

    private void decay(final IMeteoriteWorld w, final int x, final int y, final int z) {
        final Random decayRng = new Random(this.seed + SEED_OFFSET_DECAY);
        double randomShit = 0;

        final int meteorXLength = w.minX(x - 30);
        final int meteorXHeight = w.maxX(x + 30);
        final int meteorZLength = w.minZ(z - 30);
        final int meteorZHeight = w.maxZ(z + 30);

        for (int i = meteorXLength; i < meteorXHeight; i++) {
            for (int k = meteorZLength; k < meteorZHeight; k++) {
                for (int j = y - 9; j < y + 30; j++) {
                    Block blk = w.getBlock(i, j, k);
                    if (blk == Blocks.LAVA) {
                        continue;
                    }

                    if (blk.isReplaceable(w.getWorld(), new BlockPos(i, j, k))) {
                        blk = Platform.AIR_BLOCK;
                        final Block blk_b = w.getBlock(i, j + 1, k);

                        if (blk_b != blk) {
                            final IBlockState meta_b = w.getBlockState(i, j + 1, k);

                            w.setBlock(i, j, k, meta_b, 3);
                        } else if (randomShit < 100 * this.squaredCraterSize) {
                            final double dx = i - x;
                            final double dy = j - y;
                            final double dz = k - z;
                            final double dist = dx * dx + dy * dy + dz * dz;

                            final Block xf = w.getBlock(i, j - 1, k);
                            if (!xf.isReplaceable(w.getWorld(), new BlockPos(i, j - 1, k))) {
                                final double extraRange = decayRng.nextDouble() * 0.6;
                                final double height = this.squaredCraterSize * (extraRange + 0.2)
                                        - Math.abs(dist - this.squaredCraterSize * 1.7);

                                if (xf != blk && height > 0 && decayRng.nextFloat() > 0.6F) {
                                    randomShit++;
                                    this.type.getRandomFall(decayRng.nextDouble(), w, i, j, k);
                                }
                            }
                        }
                    } else {
                        // decay.
                        final Block blk_b = w.getBlock(i, j + 1, k);
                        if (blk_b == Platform.AIR_BLOCK) {
                            if (decayRng.nextFloat() > 0.4F) {
                                final double dx = i - x;
                                final double dy = j - y;
                                final double dz = k - z;

                                if (dx * dx + dy * dy + dz * dz < this.squaredCraterSize * 1.6) {
                                    this.type.getRandomInset(decayRng.nextDouble(), w, i, j, k);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean spawnMeteoriteCenter() {

        if (!world.isNether()) {
            return false;
        }

        Block blk = world.getBlock(x, y, z);
        if (!validSpawn.contains(blk)) {
            return false; // must spawn on a valid block..
        }

        if (blk == Blocks.SAND) {
            this.type = new FalloutSand(world, x, y, z, this.putter, skyStoneDefinition);
        } else if (blk == Blocks.HARDENED_CLAY) {
            this.type = new FalloutCopy(world, x, y, z, this.putter, skyStoneDefinition);
        } else if (blk == Blocks.ICE || blk == Blocks.SNOW) {
            this.type = new FalloutSnow(world, x, y, z, this.putter, skyStoneDefinition);
        }

        int realValidBlocks = 0;

        for (int i = x - 6; i < x + 6; i++) {
            for (int j = y - 6; j < y + 6; j++) {
                for (int k = z - 6; k < z + 6; k++) {
                    blk = world.getBlock(i, j, k);
                    if (validSpawn.contains(blk)) {
                        realValidBlocks++;
                    }
                }
            }
        }

        int validBlocks = 0;
        for (int i = x - 15; i < x + 15; i++) {
            for (int j = y - 15; j < y + 15; j++) {
                for (int k = z - 15; k < z + 15; k++) {
                    blk = world.getBlock(i, j, k);
                    if (invalidSpawn.contains(blk)) {
                        return false;
                    }
                    if (validSpawn.contains(blk)) {
                        validBlocks++;
                    }
                }
            }
        }

        final int minBlocks = 200;
        if (validBlocks > minBlocks && realValidBlocks > 80) {
            // We can spawn here!
            // Crater
            if (skyMode > 10) {
                this.placeCrater(world, x, y, z);
            }

            this.placeMeteorite(world, x, y, z);

            // collapse blocks...
            if (skyMode > 3) {
                this.decay(world, x, y, z);
            }

            world.done();

            WorldData.instance()
                    .spawnData()
                    .addNearByMeteorites(world.getWorld().provider.getDimension(), x >> 4, z >> 4, this.settings);
            return true;
        }
        return false;
    }

    NBTTagCompound getSettings() {
        return this.settings;
    }
}
