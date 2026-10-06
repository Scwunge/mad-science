package io.github.scwunge.madscience.client.screen;

import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.content.machine.cnc.CncMachineBlockEntity;
import io.github.scwunge.madscience.content.machine.cnc.CncMachineBlockEntity.Status;
import io.github.scwunge.madscience.content.recipe.CncRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;

import java.util.Locale;

/** The CnC Machine's screen: water, power and progress, and a readout of what the book in it asks for. */
public class CncMachineScreen extends MachineScreen {
    private static final int RED = 0xFF5555, GREEN = 0x55FF55;

    public CncMachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "cnc_machine");
    }

    private Status status() {
        Status[] values = Status.values();
        return values[Math.min(values.length - 1, Math.max(0, value(6)))];
    }

    @Override
    protected void drawGauges(GuiGraphics graphics) {
        blitUp(graphics, 68, 62, 176, 0, 14, 14, scaled(value(0), value(1), 14));
        blitRight(graphics, 89, 45, 176, 14, 31, 14, scaled(value(2), value(3), 31) + 1);
        if (value(4) > 0) {
            drawTank(graphics, 8, 9, 16, 58, Fluids.WATER, value(4), value(5), 176, 28);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Status status = status();
        if (status == Status.OFF) {
            return;
        }
        Component readout;
        int colour = RED;
        if (status == Status.WORKING) {
            // the original decoded the book's binary and showed what it is cutting
            String page = CncMachineBlockEntity.firstPage(menu.machine().items().getStackInSlot(CncMachineBlockEntity.BOOK));
            readout = Component.literal(CncRecipe.decode(page).toUpperCase(Locale.ROOT));
            colour = GREEN;
        } else {
            readout = Component.translatable("gui.madscience.cnc." + status.name().toLowerCase(Locale.ROOT));
        }
        graphics.drawString(font, font.substrByWidth(readout, 82).getString(), 90, 21, colour, false);
    }

    @Override
    protected void addTooltips() {
        tip(68, 62, 14, 14, percent("gui.madscience.energy_percent", value(0), value(1)), energyLine(value(0), value(1)));
        tip(89, 45, 31, 14, percent("gui.madscience.progress_percent", value(2), value(3)),
                Component.translatable("gui.madscience.progress", value(2), value(3)));
        if (value(4) > 0) {
            tip(8, 9, 16, 58, Fluids.WATER.getFluidType().getDescription(), Component.translatable("gui.madscience.millibuckets", value(4)));
        }
        if (menu.machine().items().getStackInSlot(CncMachineBlockEntity.WATER_IN).isEmpty()) {
            tip(31, 34, 16, 16, Component.translatable("gui.madscience.input_water_bucket"));
        }
        if (menu.machine().items().getStackInSlot(CncMachineBlockEntity.IRON).isEmpty()) {
            tip(67, 44, 16, 16, Component.translatable("gui.madscience.input_iron_block"));
        }
        if (menu.machine().items().getStackInSlot(CncMachineBlockEntity.BOOK).isEmpty()) {
            tip(67, 17, 16, 16, Component.translatable("gui.madscience.input_cnc_book"));
        }
    }
}
