package appeng.util.item;

import com.google.common.base.Preconditions;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Objects;

final class AESharedItemStack implements Comparable<AESharedItemStack> {

    private static final NBTTagCompound LOW_TAG = new NBTTagCompound();
    private static final NBTTagCompound HIGH_TAG = new NBTTagCompound();

    private final ItemStack itemStack;
    private final int itemId;
    private final int itemDamage;
    private final int hashCode;
    private final NBTTagCompound tagCompound;

    public AESharedItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.itemId = Item.getIdFromItem(itemStack.getItem());
        this.itemDamage = itemStack.getItemDamage();
        this.tagCompound = itemStack.getTagCompound();
        this.hashCode = Objects.hash(this.itemId, this.itemDamage, this.tagCompound != null ? this.tagCompound : 0);
    }

    ItemStack getDefinition() {
        return this.itemStack;
    }

    int getItemDamage() {
        return this.itemDamage;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AESharedItemStack)) {
            return false;
        }

        final AESharedItemStack other = (AESharedItemStack) obj;

        if (this.itemStack == other.itemStack) {
            return true;
        }
        return ItemStack.areItemStacksEqual(this.itemStack, other.itemStack);
    }

    @Override
    public int compareTo(final AESharedItemStack itemStack) {
        if (this.itemStack == itemStack.getDefinition()) {
            return 0;
        }

        final int id = this.itemId - itemStack.itemId;
        if (id != 0) {
            return id;
        }

        final int damageValue = this.itemDamage - itemStack.itemDamage;
        if (damageValue != 0) {
            return damageValue;
        }

        final int nbt = this.compareNBT(itemStack.getDefinition());
        if (nbt != 0) {
            return nbt;
        }

        if (!this.itemStack.areCapsCompatible(itemStack.getDefinition())) {
            return System.identityHashCode(this.itemStack) - System.identityHashCode(itemStack.getDefinition());
        }
        return 0;
    }

    private int compareNBT(final ItemStack itemStack) {
        final NBTTagCompound otherTagCompound = itemStack.getTagCompound();
        if (this.tagCompound == otherTagCompound) {
            return 0;
        }
        if (this.tagCompound == LOW_TAG || otherTagCompound == HIGH_TAG) {
            return -1;
        }
        if (this.tagCompound == HIGH_TAG || otherTagCompound == LOW_TAG) {
            return 1;
        }
        return System.identityHashCode(this.tagCompound) - System.identityHashCode(otherTagCompound);
    }

}