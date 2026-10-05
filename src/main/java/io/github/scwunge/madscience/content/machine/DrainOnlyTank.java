package io.github.scwunge.madscience.content.machine;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Exposes a tank to pipes for draining only. */
public record DrainOnlyTank(FluidTank tank) implements IFluidHandler {
    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int index) {
        return tank.getFluid();
    }

    @Override
    public int getTankCapacity(int index) {
        return tank.getCapacity();
    }

    @Override
    public boolean isFluidValid(int index, FluidStack stack) {
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return tank.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return tank.drain(maxDrain, action);
    }
}
