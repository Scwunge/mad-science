package io.github.scwunge.madscience.client.screen;

import io.github.scwunge.madscience.content.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SequencerScreen extends MachineScreen {
    public SequencerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "sequencer");
    }

    @Override
    protected void drawGauges(GuiGraphics graphics) {
        blitUp(graphics, 55, 54, 176, 0, 14, 14, scaled(value(0), value(1), 14));
        blitRight(graphics, 78, 35, 176, 14, 43, 17, scaled(value(2), value(3), 43) + 1);
    }

    @Override
    protected void addTooltips() {
        tip(55, 54, 14, 14, percent("gui.madscience.energy_percent", value(0), value(1)), energyLine(value(0), value(1)));
        tip(78, 35, 43, 17, percent("gui.madscience.progress_percent", value(2), value(3)),
                Component.translatable("gui.madscience.progress", value(2), value(3)));
    }
}
