package io.github.scwunge.madscience.client.screen;

import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.content.machine.sanitizer.SanitizerBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;

public class SanitizerScreen extends MachineScreen {
    public SanitizerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "sanitizer");
    }

    @Override
    protected void drawGauges(GuiGraphics graphics) {
        blitUp(graphics, 74, 52, 176, 0, 14, 14, scaled(value(0), value(1), 14));
        blitRight(graphics, 96, 34, 176, 14, 24, 17, scaled(value(2), value(3), 24) + 1);
        if (value(4) > 0) {
            drawTank(graphics, 8, 9, 16, 58, Fluids.WATER, value(4), value(5), 176, 31);
        }
    }

    @Override
    protected void addTooltips() {
        tip(74, 52, 14, 14, percent("gui.madscience.energy_percent", value(0), value(1)), energyLine(value(0), value(1)));
        tip(96, 34, 24, 17, percent("gui.madscience.progress_percent", value(2), value(3)),
                Component.translatable("gui.madscience.progress", value(2), value(3)));
        if (value(4) > 0) {
            tip(8, 9, 16, 58, Fluids.WATER.getFluidType().getDescription(), Component.translatable("gui.madscience.millibuckets", value(4)));
        }
        if (menu.machine().items().getStackInSlot(SanitizerBlockEntity.WATER_IN).isEmpty()) {
            tip(31, 34, 18, 18, Component.translatable("gui.madscience.place_water_bucket"));
        }
    }
}
