package appeng.parts.reporting;

import java.io.IOException;

import appeng.api.parts.IPartModel;
import appeng.api.storage.data.IAEStack;
import appeng.client.render.TesrRenderHelper;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.settings.TickRates;
import appeng.helpers.Reflected;
import appeng.util.IWideReadableNumberConverter;
import appeng.util.Platform;
import appeng.util.ReadableNumberConverter;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * @author MCTBL
 * @version rv3-beta-538-GTNH
 * @since rv3-beta-538-GTNH
 */
public class PartThroughputMonitor extends AbstractPartMonitor implements IGridTickable {

    private enum TimeUnit {

        Tick("/t", 1),
        Second("/s", 20),
        Minute("/m", 1_200),
        Hour("/h", 72_000);

        final String label;
        final int totalTicks;

        TimeUnit(String label, int totalTicks) {
            this.totalTicks = totalTicks;
            this.label = label;
        }

        public TimeUnit getNext() {
            if (this.ordinal() == TimeUnit.values().length - 1) {
                return Tick;
            }
            return TimeUnit.values()[this.ordinal() + 1];
        }

        public static TimeUnit fromOrdinal(int ordinal) {
            if (ordinal < 0 || ordinal >= TimeUnit.values().length) {
                return Tick;
            } else {
                return TimeUnit.values()[ordinal];
            }
        }
    }

    private static final IWideReadableNumberConverter NUMBER_CONVERTER = ReadableNumberConverter.INSTANCE;

    @PartModels
    private static final ResourceLocation MODEL_ON = new ResourceLocation(AppEng.MOD_ID, "part/throughput_monitor_on");
    @PartModels
    private static final ResourceLocation MODEL_OFF = new ResourceLocation(AppEng.MOD_ID, "part/throughput_monitor_off");
    @PartModels
    private static final ResourceLocation MODEL_LOCKED_OFF = new ResourceLocation(AppEng.MOD_ID, "part/throughput_monitor_locked_off");
    @PartModels
    private static final ResourceLocation MODEL_LOCKED_ON = new ResourceLocation(AppEng.MOD_ID, "part/throughput_monitor_locked_on");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    public static final IPartModel MODELS_LOCKED_OFF = new PartModel(MODEL_BASE, MODEL_LOCKED_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_LOCKED_ON = new PartModel(MODEL_BASE, MODEL_LOCKED_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_LOCKED_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_LOCKED_ON, MODEL_STATUS_HAS_CHANNEL);

    @Override
    public @NotNull IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL,
                MODELS_LOCKED_OFF, MODELS_LOCKED_ON, MODELS_LOCKED_HAS_CHANNEL);
    }

    private TimeUnit timeMode;
    private double itemNumsChange;
    private long lastStackSize;

    @Reflected
    public PartThroughputMonitor(final ItemStack is) {
        super(is);
        this.itemNumsChange = 0;
        this.lastStackSize = -1;
        this.timeMode = TimeUnit.Tick;
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        this.timeMode = TimeUnit.fromOrdinal(data.getInteger("timeMode"));
    }

    @Override
    public void writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("timeMode", this.timeMode.ordinal());
    }

    @Override
    public void writeToStream(final ByteBuf data) throws IOException {
        super.writeToStream(data);
        data.writeInt(this.timeMode.ordinal());
        data.writeDouble(this.itemNumsChange);
    }

    @Override
    public boolean readFromStream(final ByteBuf data) throws IOException {
        boolean needRedraw = super.readFromStream(data);
        this.timeMode = TimeUnit.fromOrdinal(data.readInt());
        this.itemNumsChange = data.readDouble();
        return needRedraw;
    }

    @Override
    public boolean onPartShiftActivate(final EntityPlayer player, EnumHand hand, final Vec3d pos) {
        if (Platform.isClient()) {
            return true;
        }

        if (!this.getProxy().isActive()) {
            return false;
        }

        if (!Platform.hasPermissions(this.getLocation(), player)) {
            return false;
        }

        this.timeMode = this.timeMode.getNext();
        this.getHost().markForUpdate();

        return true;
    }

    @Override
    public @NotNull TickingRequest getTickingRequest(@NotNull IGridNode node) {
        return new TickingRequest(
                TickRates.ThroughputMonitor.getMin(),
                TickRates.ThroughputMonitor.getMax(),
                false,
                false);
    }

    @Override
    public @NotNull TickRateModulation tickingRequest(@NotNull IGridNode node, int TicksSinceLastCall) {
        if (Platform.isClient()) {
            return TickRateModulation.SAME;
        }

        if (this.getDisplayed() == null) {
            this.lastStackSize = -1;
            this.getHost().markForUpdate();
            return TickRateModulation.IDLE;
        } else {
            long nowStackSize = this.getDisplayed().getStackSize();
            if (this.lastStackSize != -1) {
                long changeStackSize = nowStackSize - this.lastStackSize;
                this.itemNumsChange = (double) (changeStackSize * this.timeMode.totalTicks) / TicksSinceLastCall;
                this.getHost().markForUpdate();
            }
            this.lastStackSize = nowStackSize;
        }
        return TickRateModulation.FASTER;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderDynamic(double x, double y, double z, float partialTicks, int destroyStage) {
        // Check if the panel is powered and has a channel
        if ((this.getClientFlags() & (PartPanel.POWERED_FLAG | PartPanel.CHANNEL_FLAG)) != (PartPanel.POWERED_FLAG | PartPanel.CHANNEL_FLAG)) {
            return;
        }

        // Get the stack to be displayed
        IAEStack<?> ais = this.getDisplayed();

        if (ais == null) {
            return;
        }

        // Set up rendering transforms
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);

        EnumFacing facing = this.getSide().getFacing();

        TesrRenderHelper.moveToFace(facing);
        TesrRenderHelper.rotateToFace(facing, this.getSpin());

        // Render the appropriate stack type with throughput information
        if (ais instanceof IAEItemStack) {
            // Use our new method that shows throughput information
            TesrRenderHelper.renderItem2dWithAmountAndChange(
                    (IAEItemStack) ais,
                    0.6f,  // itemScale
                    0.05f, // spacing
                    this.itemNumsChange,
                    this.timeMode.label
            );
        }

        GlStateManager.popMatrix();
    }

}