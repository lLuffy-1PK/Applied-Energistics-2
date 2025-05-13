package appeng.me.pathfinding;

import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridVisitor;
import appeng.core.AEConfig;
import appeng.tile.networking.TileController;
import net.minecraft.util.math.BlockPos;

public class ControllerValidator implements IGridVisitor {

    private boolean isValid = true;
    private int found = 0;
    private int minX, minY, minZ, maxX, maxY, maxZ;
    private final int maxControllerSizeX, maxControllerSizeY, maxControllerSizeZ;

    public ControllerValidator(final int x, final int y, final int z) {
        this.minX = this.maxX = x;
        this.minY = this.maxY = y;
        this.minZ = this.maxZ = z;
        this.maxControllerSizeX = AEConfig.instance().getMaxControllerSizeX();
        this.maxControllerSizeY = AEConfig.instance().getMaxControllerSizeY();
        this.maxControllerSizeZ = AEConfig.instance().getMaxControllerSizeZ();
    }

    @Override
    public boolean visitNode(final IGridNode n) {
        final IGridHost host = n.getMachine();
        if (!(host instanceof TileController controller)) {
            return false;
        }

        final BlockPos pos = controller.getPos();

        this.minX = Math.min(pos.getX(), this.minX);
        this.maxX = Math.max(pos.getX(), this.maxX);
        this.minY = Math.min(pos.getY(), this.minY);
        this.maxY = Math.max(pos.getY(), this.maxY);
        this.minZ = Math.min(pos.getZ(), this.minZ);
        this.maxZ = Math.max(pos.getZ(), this.maxZ);

        if (this.maxX - this.minX > this.maxControllerSizeX ||
                this.maxY - this.minY > this.maxControllerSizeY ||
                this.maxZ - this.minZ > this.maxControllerSizeZ) {
            this.isValid = false;
            return false;
        }

        this.found++;
        return true;
    }

    public boolean isValid() {
        return this.isValid;
    }

    public int getFound() {
        return this.found;
    }
}
