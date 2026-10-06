package io.github.scwunge.madscience.content.sign;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** The warning signs, in the original's order, with each one's 32x32 cell in the sign texture. */
public enum WarningSignType implements StringRepresentable {
    MAGNETIC_FIELD_1(0, 0),
    EXPLOSIBLE_REGION(32, 0),
    WARNING_AUGER(0, 32),
    CORROSIVE(32, 32),
    FLAMMABLE(64, 32),
    COMPRESSED_GAS(96, 0),
    HOT_SURFACE(96, 32),
    LASER_BEAM(0, 64),
    MAGNETIC_FIELD_2(0, 96),
    OPTICAL_RADIATION(32, 64),
    EXPLOSIVE(64, 64),
    POISONOUS(32, 96),
    RADIOACTIVE(64, 96),
    OXIDISING(96, 64),
    ELECTROMAGNETIC_RADIATION(96, 96),
    FALLING(128, 0),
    BIOHAZARD(160, 0),
    BATTERY(128, 32),
    REMOTE_START(192, 64),
    FINGER_SANDWICH(160, 32),
    SLIPPING(128, 64),
    LOW_CEILING(160, 64),
    POINTY(128, 96),
    CONVEYOR(160, 96),
    ENTANGLEMENT(224, 64),
    GENERIC_WARNING(64, 0);

    public final int u;
    public final int v;

    WarningSignType(int u, int v) {
        this.u = u;
        this.v = v;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static WarningSignType byName(String name) {
        for (WarningSignType type : values()) {
            if (type.getSerializedName().equals(name)) {
                return type;
            }
        }
        return GENERIC_WARNING;
    }

    public static WarningSignType byId(int id) {
        WarningSignType[] values = values();
        return values[Math.floorMod(id, values.length)];
    }

    public WarningSignType next() {
        return byId(ordinal() + 1);
    }

    /** "Warning: ..." as the original printed it. */
    public Component description() {
        return Component.translatable("warning_sign.madscience.prefix", Component.translatable("warning_sign.madscience." + getSerializedName()));
    }
}
