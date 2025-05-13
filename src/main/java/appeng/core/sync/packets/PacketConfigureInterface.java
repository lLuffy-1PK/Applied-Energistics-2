package appeng.core.sync.packets;

import appeng.api.util.AEPartLocation;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.helpers.IPriorityHost;
import appeng.parts.AEBasePart;
import appeng.util.Platform;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;

public class PacketConfigureInterface extends AppEngPacket {

    private final long id;

    // automatic.
    public PacketConfigureInterface(final ByteBuf stream) {
        this.id = stream.readLong();
    }

    // api
    public PacketConfigureInterface(final long id) {
        this.id = id;

        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());
        data.writeLong(id);

        this.configureWrite(data);
    }

    @Override
    public void serverPacketData(INetworkInfo manager, AppEngPacket packet, EntityPlayer player) {
        var container = player.openContainer;
        if (container instanceof ContainerInterfaceTerminal terminal) {
            var byId = terminal.getById(this.id);
            if (byId != null) {
                var host = byId.getHost();

                // I don't know why this is in IPriorityHost.
                if (host instanceof IPriorityHost guiHandler) {
                    var bridge = guiHandler.getGuiBridge();
                    var side = host instanceof AEBasePart part ? part.getSide() : AEPartLocation.INTERNAL;
                    Platform.openGUI(player, host.getTile(), side, bridge);
                }
            }
        }
    }
}
