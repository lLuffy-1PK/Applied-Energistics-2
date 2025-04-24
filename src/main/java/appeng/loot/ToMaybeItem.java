package appeng.loot;

import appeng.api.AEApi;
import appeng.api.definitions.IItemDefinition;
import appeng.api.definitions.IMaterials;
import appeng.core.AppEng;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Loot function to unwrap and generate an {@link appeng.api.definitions.IItemDefinition} of a press.
 * The item this function modifies is a placeholder, it will not be used!
 */
public class ToMaybeItem extends LootFunction {
    @Nullable
    private final IItemDefinition itemDefinition;

    public ToMaybeItem(LootCondition[] conditionsIn, String itemDefName) {
        super(conditionsIn);
        final IMaterials materials = AEApi.instance().definitions().materials();

        // Hard-coded, because there's no way to look up an IItemDefinition by name.
        var calcProcessorDef = materials.calcProcessorPress();
        var engProcessorDef = materials.engProcessorPress();
        var logicProcessorDef = materials.logicProcessorPress();
        var siliconPressDef = materials.siliconPress();
        var skyStoneDef = AEApi.instance().definitions().blocks().skyStoneBlock();

        if (itemDefName.equals(calcProcessorDef.identifier())) {
            itemDefinition = calcProcessorDef;
        } else if (itemDefName.equals(engProcessorDef.identifier())) {
            itemDefinition = engProcessorDef;
        } else if (itemDefName.equals(logicProcessorDef.identifier())) {
            itemDefinition = logicProcessorDef;
        } else if (itemDefName.equals(siliconPressDef.identifier())) {
            itemDefinition = siliconPressDef;
        } else if (itemDefName.equals(skyStoneDef.identifier())){
            itemDefinition = skyStoneDef;
        } else {
            itemDefinition = null;
        }
    }

    @NotNull
    @Override
    public ItemStack apply(@NotNull ItemStack placeholder, @NotNull Random rand, @NotNull LootContext context) {
        ItemStack outStack;
        if (itemDefinition != null) {
            outStack = itemDefinition.maybeStack(1).orElse(new ItemStack(Items.AIR));
        } else {
            outStack = new ItemStack(Items.AIR);
        }
        return outStack;
    }

    public static class Serializer extends LootFunction.Serializer<ToMaybeItem> {
        private static final String MAYBE_ITEM_FUNCTION_NAME = "transform_to_maybe_item";
        private static final String ITEM_NAME_KEY = "id";

        public Serializer() {
            super(new ResourceLocation(AppEng.MOD_ID, MAYBE_ITEM_FUNCTION_NAME), ToMaybeItem.class);
        }

        @Override
        public void serialize(@NotNull JsonObject object,
                              @NotNull ToMaybeItem functionClazz,
                              @NotNull JsonSerializationContext serializationContext) {
            IItemDefinition itemDefinition = functionClazz.itemDefinition;
            String itemDefName = "";
            if (itemDefinition != null) {
                itemDefName = itemDefinition.identifier();
            }
            object.addProperty(ITEM_NAME_KEY, itemDefName);
        }

        @NotNull
        @Override
        public ToMaybeItem deserialize(@NotNull JsonObject object,
                                       @NotNull JsonDeserializationContext deserializationContext,
                                       LootCondition @NotNull [] conditionsIn) {
            String itemDefName = JsonUtils.getString(object, ITEM_NAME_KEY);
            return new ToMaybeItem(conditionsIn, itemDefName);
        }
    }
}
