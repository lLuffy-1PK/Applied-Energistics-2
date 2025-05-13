package appeng.util;

import appeng.api.networking.security.IActionSource;
import appeng.core.AEConfig;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInfoToast;
import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Limiter {
    private static Limiter instance;
    private final List<ResourceLocation> entries = Lists.newArrayList();

    public static void init() {
        instance = new Limiter();
        instance.populateEntries();
    }

    public static Limiter instance() {
        return instance;
    }

    private void populateEntries() {
        Arrays.asList(AEConfig.instance().getLimiterBannedItems()).forEach(entry -> {
            String[] tokens = entry.split(":");
            if (tokens.length == 2) {
                entries.add(new ResourceLocation(tokens[0], tokens[1]));
            }
        });
    }

    public boolean test(ItemStack stack, IActionSource src) {
        ResourceLocation registryName = stack.getItem().getRegistryName();

        if (Objects.nonNull(registryName) && entries.contains(registryName)) {
            src.player().ifPresent(player -> NetworkHandler.instance().sendTo(new PacketInfoToast("toast.me_system.banned_item"), (EntityPlayerMP) player));
            return true;
        }

        try {
            (new PacketBuffer(Unpooled.buffer())).writeItemStack(stack).readItemStack();
        } catch (Exception e) {
            src.player().ifPresent(player -> NetworkHandler.instance().sendTo(new PacketInfoToast("toast.me_system.too_big_nbt"), (EntityPlayerMP) player));
            return true;
        }

        return false;
    }
}