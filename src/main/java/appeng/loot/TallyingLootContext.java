package appeng.loot;

import appeng.core.AELog;
import appeng.core.AppEng;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTableManager;

import javax.annotation.Nullable;

public class TallyingLootContext extends LootContext implements ILootTallyer {
    // Key is an identifier for a specific item entry.
    private final Int2IntMap talliedItems = new Int2IntArrayMap();

    public TallyingLootContext(float luckIn, WorldServer worldIn, LootTableManager lootTableManagerIn,
                               @Nullable Entity lootedEntityIn, @Nullable EntityPlayer playerIn,
                               @Nullable DamageSource damageSourceIn) {
        super(luckIn, worldIn, lootTableManagerIn, lootedEntityIn, playerIn, damageSourceIn);
    }

    @Nullable
    public static ILootTallyer functionHasContext(LootContext context, String lootFunctionName) {
        if (!(context instanceof ILootTallyer tallyContext)) {
            AELog.warn("Using LootFunction %s:%s without a TallyingLootContext! Skipping function.",
                    AppEng.MOD_ID, lootFunctionName);
            return null;
        }
        return tallyContext;
    }

    @Nullable
    public static ILootTallyer conditionHasContext(LootContext context, String lootConditionName) {
        if (!(context instanceof ILootTallyer tallyContext)) {
            AELog.warn("Using LootCondition %s:%s without a TallyingLootContext! Skipping condition.",
                    AppEng.MOD_ID, lootConditionName);
            return null;
        }
        return tallyContext;
    }

    private int hash(String name, int contextId) {
        return name.hashCode() + contextId;
    }

    @Override
    public boolean canRoll(int max, String itemName, int contextId) {
        return talliedItems.get(hash(itemName, contextId)) < max;
    }

    @Override
    public void tally(String itemName, int contextId) {
        var hash = hash(itemName, contextId);
        talliedItems.put(hash, talliedItems.get(hash) + 1);
    }

    public static class Builder {
        protected final WorldServer world;
        protected float luck;
        protected Entity lootedEntity;
        protected EntityPlayer player;
        protected DamageSource damageSource;

        public Builder(WorldServer worldIn)
        {
            this.world = worldIn;
        }

        public Builder withLuck(float luckIn)
        {
            this.luck = luckIn;
            return this;
        }

        public Builder withLootedEntity(Entity entityIn)
        {
            this.lootedEntity = entityIn;
            return this;
        }

        public Builder withPlayer(EntityPlayer playerIn)
        {
            this.player = playerIn;
            return this;
        }

        public Builder withDamageSource(DamageSource dmgSource)
        {
            this.damageSource = dmgSource;
            return this;
        }

        public TallyingLootContext build()
        {
            return new TallyingLootContext(this.luck, this.world, this.world.getLootTableManager(),
                    this.lootedEntity, this.player, this.damageSource);
        }
    }
}
