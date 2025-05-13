package appeng.client.gui.toasts;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nonnull;

public class GuiInfoToast implements IToast {
    private final ItemStack item;
    private final String title;
    private final String subtitle;

    private GuiInfoToast(ItemStack item, String subtitleKey) {
        this.item = item;
        this.title = new TextComponentTranslation("toast.me_system.title").getFormattedText();
        this.subtitle = new TextComponentTranslation(subtitleKey).getFormattedText();
    }

    public static void queue(ItemStack item, String subtitleKey) {
        Minecraft.getMinecraft().getToastGui().add(new GuiInfoToast(item, subtitleKey));
    }

    @Nonnull
    @Override
    public Visibility draw(GuiToast manager, long delta) {
        manager.getMinecraft().getTextureManager().bindTexture(TEXTURE_TOASTS);
        GlStateManager.color(1.0F, 1.0F, 1.0F);
        manager.drawTexturedModalRect(0, 0, 0, 0, 160, 32);

        manager.getMinecraft().fontRenderer.drawString(title, 30, 7, -256);
        manager.getMinecraft().fontRenderer.drawString(subtitle, 30, 18, -1);

        RenderHelper.enableGUIStandardItemLighting();
        manager.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(item, 8, 8);

        return delta >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }
}