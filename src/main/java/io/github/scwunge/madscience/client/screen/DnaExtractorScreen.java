package io.github.scwunge.madscience.client.screen;

import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.content.machine.dnaextractor.DnaExtractorBlockEntity;
import io.github.scwunge.madscience.registry.ModFluids;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DnaExtractorScreen extends MachineScreen {
    public DnaExtractorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "dna_extractor");
    }

    @Override
    protected void drawGauges(GuiGraphics graphics) {
        int power = scaled(value(0), value(1), 14);
        blitUp(graphics, 10, 49, 176, 0, 14, 14, power);
        blitRight(graphics, 32, 31, 176, 14, 31, 17, scaled(value(2), value(3), 31) + 1);
        drawTank(graphics, 131, 19, 16, 58, ModFluids.MUTANT_DNA.source.get(), value(4), value(5), 176, 31);
    }

    @Override
    protected void addTooltips() {
        tip(10, 49, 14, 14, percent("gui.madscience.energy_percent", value(0), value(1)), energyLine(value(0), value(1)));
        tip(32, 31, 31, 17, percent("gui.madscience.progress_percent", value(2), value(3)),
                Component.translatable("gui.madscience.progress", value(2), value(3)));
        if (value(4) > 0) {
            tip(131, 19, 16, 58, ModFluids.MUTANT_DNA.type.get().getDescription(), Component.translatable("gui.madscience.millibuckets", value(4)));
        }
        if (menu.machine().items().getStackInSlot(DnaExtractorBlockEntity.BUCKET_IN).isEmpty()) {
            tip(152, 61, 18, 18, Component.translatable("gui.madscience.place_empty_bucket"));
        }
    }
}
