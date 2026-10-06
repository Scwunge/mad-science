package io.github.scwunge.madscience.client.screen;

import io.github.scwunge.madscience.content.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Screens for the machines whose GUI is just power and a progress bar. */
public final class SimpleMachineScreens {
    private SimpleMachineScreens() {
    }

    public static class Freezer extends MachineScreen {
        public Freezer(MachineMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, "freezer");
        }

        @Override
        protected void drawGauges(GuiGraphics graphics) {
            blitUp(graphics, 10, 56, 176, 0, 14, 14, scaled(value(0), value(1), 14));
            // the snowflake fills downwards as the fuel is used
            int filled = scaled(value(2), value(3), 16) + 1;
            if (value(2) > 0) {
                graphics.blit(texture, leftPos + 10, topPos + 14, 176, 14, 14, filled);
            }
        }

        @Override
        protected void addTooltips() {
            tip(10, 56, 14, 14, percent("gui.madscience.energy_percent", value(0), value(1)), energyLine(value(0), value(1)));
            tip(10, 14, 14, 16, percent("gui.madscience.progress_percent", value(2), value(3)),
                    Component.translatable("gui.madscience.progress", value(2), value(3)));
        }
    }

    public static class Duplicator extends MachineScreen {
        public Duplicator(MachineMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, "duplicator");
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

    public static class ClayFurnace extends MachineScreen {
        public ClayFurnace(MachineMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, "clay_furnace");
        }

        @Override
        protected void drawGauges(GuiGraphics graphics) {
            blitUp(graphics, 72, 15, 176, 0, 28, 28, scaled(value(0), value(1), 28));
        }

        @Override
        protected void addTooltips() {
            tip(72, 15, 28, 28, Component.translatable("gui.madscience.clay_furnace_hint"));
        }
    }

    public static class Soniclocator extends MachineScreen {
        public Soniclocator(MachineMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, "soniclocator");
        }

        @Override
        protected void drawGauges(GuiGraphics graphics) {
            blitUp(graphics, 86, 60, 176, 0, 14, 14, scaled(value(0), value(1), 14));
            int cold = 40 - scaled(value(2), value(3), 40);
            if (cold > 0) {
                graphics.blit(texture, leftPos + 88, topPos + 18, 176, 14, 18, cold);
            }
        }

        @Override
        protected void addTooltips() {
            tip(86, 62, 14, 14, percent("gui.madscience.energy_percent", value(0), value(1)), energyLine(value(0), value(1)));
            tip(88, 18, 18, 40, percent("gui.madscience.progress_percent", value(2), value(3)),
                    Component.translatable("gui.madscience.soniclocator_targets", value(4), value(5)));
        }
    }
    public static class MeatCube extends MachineScreen {
        public MeatCube(MachineMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, "meat_cube");
        }

        @Override
        protected void drawGauges(GuiGraphics graphics) {
            drawTank(graphics, 67, 18, 16, 58, io.github.scwunge.madscience.registry.ModFluids.MUTANT_DNA.source.get(), value(2), value(3), 176, 0);
        }

        @Override
        protected void addTooltips() {
            tip(67, 18, 16, 58, io.github.scwunge.madscience.registry.ModFluids.MUTANT_DNA.type.get().getDescription(),
                    Component.translatable("gui.madscience.millibuckets", value(2)),
                    Component.translatable("gui.madscience.meat_left", value(4), value(5)));
        }
    }
}