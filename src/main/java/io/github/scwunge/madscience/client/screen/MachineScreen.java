package io.github.scwunge.madscience.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Base screen for the machines, drawn from the original 176x166 GUI textures. Subclasses draw their gauges with the
 * helpers here and add hover tooltips for them, matching the original GUIs.
 */
public abstract class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    protected final ResourceLocation texture;
    private final List<Tip> tips = new ArrayList<>();

    protected MachineScreen(MachineMenu menu, Inventory inventory, Component title, String textureName) {
        super(menu, inventory, title);
        this.texture = MadScience.id("textures/gui/" + textureName + ".png");
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        tips.clear();
        addTooltips();
        for (Tip tip : tips) {
            if (isHovering(tip.x, tip.y, tip.w, tip.h, mouseX, mouseY)) {
                graphics.renderComponentTooltip(font, tip.lines, mouseX, mouseY);
                break;
            }
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        drawGauges(graphics);
    }

    /** Draws progress bars, power and tanks over the background. */
    protected abstract void drawGauges(GuiGraphics graphics);

    /** Registers hover tooltips with {@link #tip}. */
    protected void addTooltips() {
    }

    protected void tip(int x, int y, int w, int h, Component... lines) {
        tips.add(new Tip(x, y, w, h, List.of(lines)));
    }

    protected int value(int index) {
        return menu.value(index);
    }

    /** Scales {@code value / max} to {@code pixels}. */
    protected static int scaled(long value, long max, int pixels) {
        return max <= 0 ? 0 : (int) Math.min(pixels, value * pixels / max);
    }

    /** Draws a sprite from the GUI texture that fills upwards from (x, y + h). */
    protected void blitUp(GuiGraphics graphics, int x, int y, int u, int v, int w, int h, int filled) {
        if (filled > 0) {
            graphics.blit(texture, leftPos + x, topPos + y + h - filled, u, v + h - filled, w, filled);
        }
    }

    /** Draws a sprite from the GUI texture that fills rightwards. */
    protected void blitRight(GuiGraphics graphics, int x, int y, int u, int v, int w, int h, int filled) {
        if (filled > 0) {
            graphics.blit(texture, leftPos + x, topPos + y, u, v, Math.min(w, filled), h);
        }
    }

    /** Draws a fluid column filling upwards inside (x, y, w, h), then the tank's scale marks from the GUI texture. */
    protected void drawTank(GuiGraphics graphics, int x, int y, int w, int h, Fluid fluid, int amount, int capacity, int overlayU, int overlayV) {
        int filled = scaled(amount, capacity, h);
        if (filled > 0 && fluid != null) {
            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
            FluidStack stack = new FluidStack(fluid, amount);
            TextureAtlasSprite sprite = minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ext.getStillTexture(stack));
            int tint = ext.getTintColor(stack);
            RenderSystem.setShaderColor(FastColor.ARGB32.red(tint) / 255F, FastColor.ARGB32.green(tint) / 255F, FastColor.ARGB32.blue(tint) / 255F, 1F);
            int drawn = 0;
            while (drawn < filled) {
                int piece = Math.min(16, filled - drawn);
                for (int dx = 0; dx < w; dx += 16) {
                    int pw = Math.min(16, w - dx);
                    graphics.blit(leftPos + x + dx, topPos + y + h - drawn - piece, 0, pw, piece, sprite);
                }
                drawn += piece;
            }
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        }
        graphics.blit(texture, leftPos + x, topPos + y, overlayU, overlayV, w, h);
    }

    protected static Component energyLine(int stored, int capacity) {
        return Component.translatable("gui.madscience.energy", stored, capacity);
    }

    protected static Component percent(String key, long value, long max) {
        return Component.translatable(key, scaled(value, max, 100));
    }

    private record Tip(int x, int y, int w, int h, List<Component> lines) {
    }
}
