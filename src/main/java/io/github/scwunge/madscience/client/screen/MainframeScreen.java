package io.github.scwunge.madscience.client.screen;

import io.github.scwunge.madscience.content.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;

public class MainframeScreen extends MachineScreen {
    public MainframeScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "mainframe");
    }

    @Override
    protected void drawGauges(GuiGraphics graphics) {
        blitUp(graphics, 54, 57, 176, 0, 14, 14, scaled(value(0), value(1), 14));
        blitRight(graphics, 78, 34, 176, 112, 65, 20, scaled(value(2), value(3), 65) + 1);
        if (value(4) > 0) {
            drawTank(graphics, 7, 7, 16, 58, Fluids.WATER, value(4), value(5), 176, 14);
        }
        // the thermometer: the cold overlay shrinks from the top as the heat rises
        int cold = 40 - scaled(value(6), value(7), 40);
        if (cold > 0) {
            graphics.blit(texture, leftPos + 52, topPos + 15, 176, 72, 18, cold);
        }
    }

    @Override
    protected void addTooltips() {
        tip(54, 57, 14, 14, percent("gui.madscience.energy_percent", value(0), value(1)), energyLine(value(0), value(1)));
        tip(78, 34, 65, 20, percent("gui.madscience.progress_percent", value(2), value(3)),
                Component.translatable("gui.madscience.progress", value(2), value(3)));
        tip(52, 15, 18, 40, percent("gui.madscience.heat_percent", value(6), value(7)),
                Component.translatable("gui.madscience.progress", value(6), value(7)));
        if (value(4) > 0) {
            tip(7, 7, 16, 58, Fluids.WATER.getFluidType().getDescription(), Component.translatable("gui.madscience.millibuckets", value(4)));
        }
    }
}
