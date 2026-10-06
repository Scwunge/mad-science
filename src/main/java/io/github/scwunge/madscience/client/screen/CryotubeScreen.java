package io.github.scwunge.madscience.client.screen;

import io.github.scwunge.madscience.content.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CryotubeScreen extends MachineScreen {
    public CryotubeScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "cryotube");
    }

    /** The original drew "empty" covers that shrink from the top as each gauge fills. */
    private void cover(GuiGraphics graphics, int x, int y, int u, int v, int w, int h, int filled) {
        int empty = h - filled;
        if (empty > 0) {
            graphics.blit(texture, leftPos + x, topPos + y, u, v, w, empty);
        }
    }

    @Override
    protected void drawGauges(GuiGraphics graphics) {
        cover(graphics, 112, 17, 176, 56, 18, 32, scaled(value(0), value(1), 32));
        blitRight(graphics, 35, 37, 176, 0, 26, 10, scaled(value(2), value(3), 26) + 1);
        cover(graphics, 68, 17, 176, 10, 11, 46, scaled(value(4), value(5), 46));
        cover(graphics, 91, 17, 187, 10, 11, 46, scaled(value(6), value(7), 46));
    }

    @Override
    protected void addTooltips() {
        tip(112, 17, 18, 32, percent("gui.madscience.energy_percent", value(0), value(1)), energyLine(value(0), value(1)));
        tip(35, 37, 26, 10, percent("gui.madscience.hatching_percent", value(2), value(3)));
        tip(68, 17, 11, 46, percent("gui.madscience.health_percent", value(4), value(5)));
        tip(91, 17, 11, 46, percent("gui.madscience.neural_percent", value(6), value(7)));
    }
}
