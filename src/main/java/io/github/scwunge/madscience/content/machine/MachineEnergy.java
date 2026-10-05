package io.github.scwunge.madscience.content.machine;

import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * FE buffer for a machine. Pipes and cables may insert up to {@code maxReceive} per call; machines that generate power
 * (the Cryogenic Tube) also allow extraction. The owning block entity spends or adds energy directly.
 */
public class MachineEnergy extends EnergyStorage {
    private final Runnable onChanged;

    public MachineEnergy(int capacity, int maxReceive, int maxExtract, Runnable onChanged) {
        super(capacity, maxReceive, maxExtract);
        this.onChanged = onChanged;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int received = super.receiveEnergy(toReceive, simulate);
        if (received > 0 && !simulate) {
            onChanged.run();
        }
        return received;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int extracted = super.extractEnergy(toExtract, simulate);
        if (extracted > 0 && !simulate) {
            onChanged.run();
        }
        return extracted;
    }

    /** Spends up to {@code amount}; like the original, a machine with any energy left keeps running this tick. */
    public void consume(int amount) {
        if (energy > 0) {
            energy = Math.max(0, energy - amount);
            onChanged.run();
        }
    }

    /** Adds generated energy, capped at capacity. */
    public void produce(int amount) {
        if (energy < capacity) {
            energy = (int) Math.min(capacity, (long) energy + amount);
            onChanged.run();
        }
    }

    public void setEnergy(int value) {
        energy = Math.max(0, Math.min(capacity, value));
    }

    public void setCapacity(int capacity, int maxReceive, int maxExtract) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        energy = Math.min(energy, capacity);
    }

    public boolean hasEnergy() {
        return energy > 0;
    }
}
