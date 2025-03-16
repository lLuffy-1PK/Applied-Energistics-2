package appeng.tile.crafting;


import appeng.api.AEApi;
import appeng.api.definitions.IBlocks;
import appeng.block.crafting.BlockCraftingUnit;
import net.minecraft.item.ItemStack;

import java.util.Optional;


public class TileCraftingStorageTile extends TileCraftingTile {
    private static final int KILO_SCALAR = 1024;

    @Override
    protected ItemStack getItemFromTile(final Object obj) {
        final IBlocks blocks = AEApi.instance().definitions().blocks();
        final int storage = ((TileCraftingTile) obj).getStorageBytes() / KILO_SCALAR;

        Optional<ItemStack> is = switch (storage) {
            case 1 -> blocks.craftingStorage1k().maybeStack(1);
            case 4 -> blocks.craftingStorage4k().maybeStack(1);
            case 16 -> blocks.craftingStorage16k().maybeStack(1);
            case 64 -> blocks.craftingStorage64k().maybeStack(1);
            default -> Optional.empty();
        };

        return is.orElseGet(() -> super.getItemFromTile(obj));
    }

    @Override
    public boolean isAccelerator() {
        return false;
    }

    @Override
    public boolean isStorage() {
        return true;
    }

    @Override
    public int getStorageBytes() {
        if (this.world == null || this.notLoaded() || this.isInvalid()) {
            return 0;
        }

        final BlockCraftingUnit unit = (BlockCraftingUnit) this.world.getBlockState(this.pos).getBlock();
        return switch (unit.type) {
            case STORAGE_4K -> 4 * 1024;
            case STORAGE_16K -> 16 * 1024;
            case STORAGE_64K -> 64 * 1024;
            default -> 1024;
        };
    }
}
