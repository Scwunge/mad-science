package io.github.scwunge.madscience.content.item;

import io.github.scwunge.madscience.registry.ModItems;
import net.minecraft.util.RandomSource;

import java.util.Locale;

/**
 * A villager's recorded memories, made by the Cryogenic Tube. The better the profession, the higher the neural
 * activity it allows when used to grow the next subject (and so the more power it makes).
 */
public class MemoryReelItem extends TwoToneItem {
    private final Memory memory;

    public MemoryReelItem(Properties properties, Memory memory) {
        super(properties.stacksTo(1), 0x563C33, memory.color);
        this.memory = memory;
    }

    public Memory memory() {
        return memory;
    }

    /** Neural activity ceiling for a subject grown with this memory. */
    public int level() {
        return memory.level;
    }

    public enum Memory {
        PRIEST(32, 0xA34BA5), FARMER(64, 0x9E6E3A), BUTCHER(128, 0xE5E5E5), BLACKSMITH(256, 0x3A3A3A), LIBRARIAN(512, 0xFFFFFF);

        private final int level;
        private final int color;

        Memory(int level, int color) {
            this.level = level;
            this.color = color;
        }

        public int level() {
            return level;
        }

        public String id() {
            return "memory_" + name().toLowerCase(Locale.ROOT);
        }

        public MemoryReelItem item() {
            return ModItems.memory(this);
        }

        /** A random profession, as the original picked for a fresh subject on an empty reel. */
        public static Memory random(RandomSource random) {
            return values()[random.nextInt(values().length)];
        }

        public static Memory forLevel(int level) {
            for (Memory memory : values()) {
                if (memory.level == level) {
                    return memory;
                }
            }
            return PRIEST;
        }
    }
}
