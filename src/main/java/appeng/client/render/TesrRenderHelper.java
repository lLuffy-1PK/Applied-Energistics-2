/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.client.render;


import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.IWideReadableNumberConverter;
import appeng.util.ReadableNumberConverter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

import java.util.Locale;


/**
 * Helper methods for rendering TESRs.
 */
public class TesrRenderHelper {

    private static final IWideReadableNumberConverter NUMBER_CONVERTER = ReadableNumberConverter.INSTANCE;

    /**
     * Move the current coordinate system to the center of the given block face, assuming that the origin is currently
     * at the center of a block.
     */
    public static void moveToFace(EnumFacing face) {
        GlStateManager.translate(face.getXOffset() * 0.50, face.getYOffset() * 0.50, face.getZOffset() * 0.50);
    }

    /**
     * Rotate the current coordinate system so it is on the face of the given block side. This can be used to render on
     * the given face as if it was
     * a 2D canvas.
     */
    public static void rotateToFace(EnumFacing face, byte spin) {
        switch (face) {
            case UP:
                GlStateManager.scale(1.0f, -1.0f, 1.0f);
                GlStateManager.rotate(90.0f, 1.0f, 0.0f, 0.0f);
                GlStateManager.rotate(spin * 90.0F, 0, 0, 1);
                break;

            case DOWN:
                GlStateManager.scale(1.0f, -1.0f, 1.0f);
                GlStateManager.rotate(-90.0f, 1.0f, 0.0f, 0.0f);
                GlStateManager.rotate(spin * -90.0F, 0, 0, 1);
                break;

            case EAST:
                GlStateManager.scale(-1.0f, -1.0f, -1.0f);
                GlStateManager.rotate(-90.0f, 0.0f, 1.0f, 0.0f);
                break;

            case WEST:
                GlStateManager.scale(-1.0f, -1.0f, -1.0f);
                GlStateManager.rotate(90.0f, 0.0f, 1.0f, 0.0f);
                break;

            case NORTH:
                GlStateManager.scale(-1.0f, -1.0f, -1.0f);
                break;

            case SOUTH:
                GlStateManager.scale(-1.0f, -1.0f, -1.0f);
                GlStateManager.rotate(180.0f, 0.0f, 1.0f, 0.0f);
                break;

            default:
                break;
        }
    }

    /**
     * Render an item in 2D.
     */
    public static void renderItem2d(ItemStack itemStack, float scale) {
        if (!itemStack.isEmpty()) {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.f, 240.0f);

            GlStateManager.pushMatrix();

            // The Z-scaling by 0.0001 causes the model to be visually "flattened"
            // This cannot replace a proper projection, but it's cheap and gives the desired
            // effect at least from head-on
            GlStateManager.scale(scale / 32.0f, scale / 32.0f, 0.0001f);
            // Position the item icon at the top middle of the panel
            GlStateManager.translate(-8, -11, 0);

            RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
            renderItem.renderItemAndEffectIntoGUI(itemStack, 0, 0);

            GlStateManager.popMatrix();
        }
    }

    /**
     * Render an item in 2D with customizable offset.
     *
     * @param itemStack The item to render
     * @param scale The scale factor for rendering
     * @param offsetX X-axis offset from the default position
     * @param offsetY Y-axis offset from the default position
     */
    public static void renderItem2d(ItemStack itemStack, float scale, float offsetX, float offsetY) {
        if (!itemStack.isEmpty()) {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.f, 240.0f);

            GlStateManager.pushMatrix();

            // The Z-scaling by 0.0001 causes the model to be visually "flattened"
            // This cannot replace a proper projection, but it's cheap and gives the desired
            // effect at least from head-on
            GlStateManager.scale(scale / 32.0f, scale / 32.0f, 0.0001f);
            // Position the item icon with the provided offsets
            GlStateManager.translate(-8 + offsetX, -11 + offsetY, 0);

            RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
            renderItem.renderItemAndEffectIntoGUI(itemStack, 0, 0);

            GlStateManager.popMatrix();
        }
    }

    public static void renderFluid2d(FluidStack fluidStack, float scale) {
        if (fluidStack != null) {
            GlStateManager.pushMatrix();
            int color = fluidStack.getFluid().getColor(fluidStack);
            float r = (color >> 16 & 255) / 255.0f;
            float g = (color >> 8 & 255) / 255.0f;
            float b = (color & 255) / 255.0f;
            TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(fluidStack.getFluid().getStill(fluidStack).toString());
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableAlpha();
            GlStateManager.disableLighting();
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();

            float width = 0.4f;
            float height = 0.4f;
            float alpha = 1.0f;
            float z = 0.0001f;
            float x = -0.20f;
            float y = -0.25f;

            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            double uMin = sprite.getInterpolatedU(16D - width * 16D), uMax = sprite.getInterpolatedU(width * 16D);
            double vMin = sprite.getMinV(), vMax = sprite.getInterpolatedV(height * 16D);
            buf.pos(x, y, z).tex(uMin, vMin).color(r, g, b, alpha).endVertex();
            buf.pos(x, y + height, z).tex(uMin, vMax).color(r, g, b, alpha).endVertex();
            buf.pos(x + width, y + height, z).tex(uMax, vMax).color(r, g, b, alpha).endVertex();
            buf.pos(x + width, y, z).tex(uMax, vMin).color(r, g, b, alpha).endVertex();

            tess.draw();
            GlStateManager.enableLighting();
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.popMatrix();

        }
    }

    /**
     * Render an item in 2D and the given text below it.
     *
     * @param spacing Specifies how far apart the item and the item stack amount are rendered.
     */
    public static void renderItem2dWithAmount(IAEItemStack itemStack, float itemScale, float spacing) {
        final ItemStack renderStack = itemStack.asItemStackRepresentation();

        TesrRenderHelper.renderItem2d(renderStack, itemScale);

        final long stackSize = itemStack.getStackSize();
        final String renderedStackSize = NUMBER_CONVERTER.toWideReadableForm(stackSize);

        // Render the item count
        final FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        final int width = fr.getStringWidth(renderedStackSize);
        GlStateManager.translate(0.0f, spacing, 0);
        GlStateManager.scale(1.0f / 62.0f, 1.0f / 62.0f, 1.0f / 62.0f);
        GlStateManager.translate(-0.5f * width, 0.0f, 0.5f);
        fr.drawString(renderedStackSize, 0, 0, 0);

    }

    /**
     * Render an item in 2D with both the amount and the rate of change information below it.
     *
     * @param itemStack The IAEItemStack to render
     * @param itemScale Scale factor for the item rendering
     * @param spacing Spacing between elements
     * @param itemNumsChange The rate of change to display
     * @param timeMode The time mode label
     */
    public static void renderItem2dWithAmountAndChange(IAEItemStack itemStack, float itemScale, float spacing,
                                                       double itemNumsChange, String timeMode) {
        final ItemStack renderStack = itemStack.asItemStackRepresentation();

        // Render the item
        TesrRenderHelper.renderItem2d(renderStack, itemScale, 0f, -5.0f);

        // Get stack size information
        final long stackSize = itemStack.getStackSize();
        final String renderedStackSize = NUMBER_CONVERTER.toWideReadableForm(stackSize);
        final String renderedStackSizeChange = formatChangeRate(itemNumsChange) + timeMode;

        // Get font renderer
        final FontRenderer fr = Minecraft.getMinecraft().fontRenderer;

        // Render the item count
        final int width = fr.getStringWidth(renderedStackSize);

        GlStateManager.translate(0.0f, spacing, 0);
        GlStateManager.scale(0.8f / 62.0f, 0.8f / 62.0f, 0.8f / 62.0f);
        GlStateManager.translate(-0.5f * width, 0.0f, 0.5f);
        fr.drawString(renderedStackSize, 0, 0, 0);

        // Render the change rate
        final int changeWidth = fr.getStringWidth(renderedStackSizeChange);
        GlStateManager.translate(0.5f * width, fr.FONT_HEIGHT + 1, 0);
        GlStateManager.translate(-0.5f * changeWidth, 0, 0);

        // Determine color based on change rate
        int color = 0;
        if (itemNumsChange < 0) {
            color = 0xFF0000; // Red for negative changes
        } else if (itemNumsChange > 0) {
            color = 0x17B66C; // Green for positive changes
        }

        fr.drawString(renderedStackSizeChange, 0, 0, color);
    }

    private static String formatChangeRate(double itemNumsChange) {
        double abs = Math.abs(itemNumsChange);
        if (abs < 1.0e-9) {
            return "0";
        }

        String sign = itemNumsChange > 0 ? "+" : itemNumsChange < 0 ? "-" : "";
        String formatted;
        if (abs >= 1000) {
            formatted = NUMBER_CONVERTER.toWideReadableForm(Math.round(abs));
        } else if (abs >= 100) {
            formatted = String.format(Locale.ROOT, "%.0f", abs);
        } else if (abs >= 10) {
            formatted = String.format(Locale.ROOT, "%.1f", abs);
        } else if (abs >= 1) {
            formatted = String.format(Locale.ROOT, "%.2f", abs);
        } else if (abs >= 0.01) {
            formatted = String.format(Locale.ROOT, "%.3f", abs);
        } else {
            return sign.isEmpty() ? "<0.01" : sign + "<0.01";
        }

        return sign + formatted;
    }

    public static void renderFluid2dWithAmount(IAEFluidStack fluidStack, float scale, float spacing) {
        final FluidStack renderStack = fluidStack.getFluidStack();

        TesrRenderHelper.renderFluid2d(renderStack, scale);

        final long stackSize = fluidStack.getStackSize() / 1000;
        final String renderedStackSize = NUMBER_CONVERTER.toWideReadableForm(stackSize) + "B";

        // Render the item count
        final FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        final int width = fr.getStringWidth(renderedStackSize);
        GlStateManager.translate(0.0f, spacing, 0);
        GlStateManager.scale(1.0f / 62.0f, 1.0f / 62.0f, 1.0f / 62.0f);
        GlStateManager.translate(-0.5f * width, 0.0f, 0.5f);
        fr.drawString(renderedStackSize, 0, 0, 0);

    }

}
