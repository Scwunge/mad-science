package io.github.scwunge.madscience.content.item;

import io.github.scwunge.madscience.MadScience;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/** The laboratory coat, leggings and safety goggles: no protection at all, purely so everyone can tell you're a scientist. */
public class LabCoatItem extends ArmorItem {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, MadScience.MODID);

    /** Zero defence, like the original; both armour layers use the original's single coat texture. */
    public static final Holder<ArmorMaterial> MATERIAL = MATERIALS.register("lab_coat", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(Type.class), map -> {
                for (Type type : Type.values()) {
                    map.put(type, 0);
                }
            }),
            0, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.WHITE_WOOL),
            List.of(new ArmorMaterial.Layer(MadScience.id("lab_coat"))), 0.0F, 0.0F));

    /** The original's durability: it practically never wears out. */
    public static final int DURABILITY = 200_000;

    public LabCoatItem(Type type, Properties properties) {
        super(MATERIAL, type, properties.durability(DURABILITY));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        TooltipItem.addLore(getDescriptionId(), tooltip);
    }
}
