package appeng.services.compass;

import appeng.core.localization.PlayerMessages;
import appeng.server.ISubCommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;

public class TestCompassCommand implements ISubCommand {
    private static final int SECTION_SIZE = 16;

    @Override
    public String getHelp(MinecraftServer srv) {
        return "commands.ae2.Compass";
    }

    @Override
    public void call(MinecraftServer srv, String[] args, ICommandSender sender) {
        var world = sender.getEntityWorld();
        var chunkPos = new ChunkPos(sender.getPosition());
        var compassRegion = CompassRegion.get((WorldServer) world, chunkPos);

        for (var i = 0; i <= world.getHeight() / SECTION_SIZE; i++) {
            var hasSkyStone = compassRegion.hasCompassTarget(chunkPos.x, chunkPos.z, i);
            var yMin = i * SECTION_SIZE;
            var yMax = (i + 1) * SECTION_SIZE - 1;
            sender.sendMessage(PlayerMessages.CompassTestSection.get(yMin, yMax, i, hasSkyStone));
        }
    }
}
