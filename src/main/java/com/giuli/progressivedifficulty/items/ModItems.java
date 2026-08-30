package com.giuli.progressivedifficulty.items;

import com.giuli.progressivedifficulty.ProgressiveDifficultyMod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom items ported from the dedsafio plugin. Not added to a creative tab
 * yet - obtain them with /give while that's pending, e.g.:
 * /give @s progressivedifficulty:ghost_sword
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, ProgressiveDifficultyMod.MOD_ID);

    public static final DeferredHolder<Item, Item> SUNBLOCK =
            ITEMS.register("sunblock", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> GHOST_SWORD =
            ITEMS.register("ghost_sword", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<Item, Item> BLUE_CAPSULE =
            ITEMS.register("blue_capsule", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> FORK =
            ITEMS.register("fork", () -> new PlaceholderMessageItem(new Item.Properties(),
                    "Este item aun no se encuentra disponible"));

    public static final DeferredHolder<Item, Item> SPOON =
            ITEMS.register("spoon", () -> new PlaceholderMessageItem(new Item.Properties(),
                    "Este item aun no se encuentra disponible"));

    public static final DeferredHolder<Item, Item> INFERNAL_SWORD =
            ITEMS.register("infernal_sword", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<Item, Item> ENDER_BAG =
            ITEMS.register("ender_bag", () -> new EnderBagItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> SPAWN_STICK =
            ITEMS.register("spawn_stick", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<Item, Item> PORTABLE_GOLDEN_ANVIL =
            ITEMS.register("portable_golden_anvil", () -> new PortableGoldenAnvilItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> MARKER_ITEM =
            ITEMS.register("marker_item", () -> new MarkerItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> RESURRECTION_SPOON =
            ITEMS.register("resurrection_spoon", () -> new ResurrectionSpoonItem(new Item.Properties().rarity(Rarity.EPIC)));
}
