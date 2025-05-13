package appeng.util.item;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

abstract class ItemVariantList {

    public void add(final IAEItemStack option) {
        AESharedItemStack sharedStack = ((AEItemStack) option).getSharedStack();
        final IAEItemStack st = this.getRecords().get(sharedStack);

        if (st != null) {
            st.add(option);
            return;
        }

        final IAEItemStack opt = option.copy();
        this.putItemRecord(opt, sharedStack);
    }

    public IAEItemStack findPrecise(final IAEItemStack itemStack) {
        return this.getRecords().get(((AEItemStack) itemStack).getSharedStack());
    }

    public void addStorage(final IAEItemStack option) {
        AESharedItemStack sharedStack = ((AEItemStack) option).getSharedStack();
        final IAEItemStack st = this.getRecords().get(sharedStack);

        if (st != null) {
            st.incStackSize(option.getStackSize());
            return;
        }

        final IAEItemStack opt = option.copy();

        this.putItemRecord(opt, sharedStack);
    }

    public void addCrafting(final IAEItemStack option) {
        AESharedItemStack sharedStack = ((AEItemStack) option).getSharedStack();
        final IAEItemStack st = this.getRecords().get(sharedStack);

        if (st != null) {
            st.setCraftable(true);
            return;
        }

        final IAEItemStack opt = option.copy();
        opt.setStackSize(0);
        opt.setCraftable(true);

        this.putItemRecord(opt, sharedStack);
    }

    public void addRequestable(final IAEItemStack option) {
        AESharedItemStack sharedStack = ((AEItemStack) option).getSharedStack();
        final IAEItemStack st = this.getRecords().get(sharedStack);

        if (st != null) {
            st.setCountRequestable(st.getCountRequestable() + option.getCountRequestable());
            return;
        }

        final IAEItemStack opt = option.copy();
        opt.setStackSize(0);
        opt.setCraftable(false);
        opt.setCountRequestable(option.getCountRequestable());

        this.putItemRecord(opt, sharedStack);
    }

    public int size() {
        return (int) getRecords().values().stream()
                .filter(IAEItemStack::isMeaningful)
                .count();
    }

    public Iterator<IAEItemStack> iterator() {
        return new MeaningfulItemIterator<>(this.getRecords().values());
    }

    private void putItemRecord(final IAEItemStack itemStack, final AESharedItemStack sharedStack) {
        this.getRecords().put(sharedStack, itemStack);
    }

    abstract Map<AESharedItemStack, IAEItemStack> getRecords();

    public abstract Collection<IAEItemStack> findFuzzy(final IAEItemStack filter, final FuzzyMode fuzzy);

}
