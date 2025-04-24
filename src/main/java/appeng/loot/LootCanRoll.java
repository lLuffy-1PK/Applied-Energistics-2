package appeng.loot;

import appeng.core.AppEng;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class LootCanRoll implements LootCondition {
    @NotNull
    private final String name;
    private final int contextId;
    private final int max;

    public LootCanRoll(@NotNull String name, int contextId, int max) {
        this.name = name;
        this.contextId = contextId;
        this.max = max;
    }

    @Override
    public boolean testCondition(@NotNull Random rand, @NotNull LootContext context) {
        ILootTallyer tallyer = TallyingLootContext.conditionHasContext(context, Serializer.LOOT_CAN_ROLL_NAME);
        return tallyer == null || tallyer.canRoll(max, name, contextId);
    }

    public static class Serializer extends LootCondition.Serializer<LootCanRoll> {
        private static final String LOOT_CAN_ROLL_NAME = "loot_can_roll";
        private static final String ITEM_NAME_KEY = "id";
        // Optional
        private static final String CONTEXT_ID_KEY = "context_id";
        private static final String MAX_KEY = "max";

        public Serializer() {
            super(new ResourceLocation(AppEng.MOD_ID, LOOT_CAN_ROLL_NAME), LootCanRoll.class);
        }

        @Override
        public void serialize(@NotNull JsonObject object,
                              @NotNull LootCanRoll conditionClazz,
                              @NotNull JsonSerializationContext serializationContext) {
            String name = conditionClazz.name;
            int contextId = conditionClazz.contextId;
            int max = conditionClazz.max;
            object.addProperty(ITEM_NAME_KEY, name);
            object.addProperty(CONTEXT_ID_KEY, contextId);
            object.addProperty(MAX_KEY, max);
        }

        @NotNull
        @Override
        public LootCanRoll deserialize(@NotNull JsonObject object,
                                     @NotNull JsonDeserializationContext deserializationContext) {
            String name = JsonUtils.getString(object, ITEM_NAME_KEY, "");
            int contextId = JsonUtils.getInt(object, CONTEXT_ID_KEY, 0);
            int max = JsonUtils.getInt(object, MAX_KEY);
            return new LootCanRoll(name, contextId, max);
        }
    }
}
