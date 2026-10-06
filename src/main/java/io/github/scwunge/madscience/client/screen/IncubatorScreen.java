package io.github.scwunge.madscience.client.screen;

import io.github.scwunge.madscience.content.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class IncubatorScreen extends MachineScreen {
    public IncubatorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "incubator");
    }

    @Override
    protected void drawGauges(GuiGraphics graphics) {
        blitUp(graphics, 15, 57, 176, 0, 14, 14, scaled(value(0), value(1), 14));
        blitRight(graphics, 93, 38, 176, 14, 36, 17, scaled(value(2), value(3), 36) + 1);
        int cold = 40 - scaled(value(4), value(5), 40);
        if (cold > 0) {
            graphics.blit(texture, leftPos + 13, topPos + 15, 176, 31, 18, cold);
        }
    }

    @Override
    protected void addTooltips() {
        tip(15, 57, 14, 14, percent("gui.madscience.energy_percent", value(0), value(1)), energyLine(value(0), value(1)));
        tip(93, 38, 36, 17, percent("gui.madscience.progress_percent", value(2), value(3)),
                Component.translatable("gui.madscience.progress", value(2), value(3)));
        tip(13, 15, 18, 40, percent("gui.madscience.heat_percent", value(4), value(5)),
                Component.translatable("gui.madscience.progress", value(4), value(5)));
    }
}
