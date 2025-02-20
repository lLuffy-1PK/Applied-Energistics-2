package appeng.core.sync.packets;

import appeng.api.AEApi;
import appeng.client.gui.toasts.GuiInfoToast;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.Optional;

public class PacketInfoToast extends AppEngPacket {
    private final String subtitle;

    // automatic
    public PacketInfoToast(ByteBuf stream) {
        this.subtitle = ByteBufUtils.readUTF8String(stream);
    }

    // api
    public PacketInfoToast(String subtitle) {
        this.subtitle = subtitle;

        ByteBuf data = Unpooled.buffer();
        data.writeInt(getPacketID());
        ByteBufUtils.writeUTF8String(data, subtitle);
        configureWrite(data);
    }

    @Override
    public void clientPacketData(INetworkInfo network, AppEngPacket packet, EntityPlayer player) {
        Optional<Block> block = AEApi.instance().definitions().blocks().controller().maybeBlock();
        GuiInfoToast.queue(new ItemStack(block.orElse(Blocks.AIR)), subtitle);
    }
}