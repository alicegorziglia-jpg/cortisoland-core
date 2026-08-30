package com.giuli.progressivedifficulty;

import com.giuli.progressivedifficulty.items.ModItems;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Port of the dedsafio plugin's /items command + CustomItemsMenu: opens a
 * chest-style menu the player can pick items from. Since the backing
 * container is built fresh every time the command runs, taking an item out
 * never "uses up" the catalog for the next person who opens it.
 */
public class ItemsMenuSystem {
    private ItemsMenuSystem() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("items")
                .requires(source -> source.hasPermission(2))
                .executes(context -> open(context.getSource())));
    }

    private static int open(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        SimpleContainer container = new SimpleContainer(27);
        ItemStack[] catalog = {
                new ItemStack(ModItems.SUNBLOCK.get()),
                new ItemStack(ModItems.GHOST_SWORD.get()),
                new ItemStack(ModItems.BLUE_CAPSULE.get()),
                new ItemStack(ModItems.FORK.get()),
                new ItemStack(ModItems.SPOON.get()),
                new ItemStack(ModItems.INFERNAL_SWORD.get()),
                new ItemStack(ModItems.ENDER_BAG.get()),
                new ItemStack(ModItems.SPAWN_STICK.get()),
                new ItemStack(ModItems.PORTABLE_GOLDEN_ANVIL.get()),
                new ItemStack(ModItems.MARKER_ITEM.get()),
                new ItemStack(ModItems.RESURRECTION_SPOON.get()),
        };
        for (int i = 0; i < catalog.length; i++) {
            container.setItem(i, catalog[i]);
        }

        player.openMenu(new SimpleMenuProvider(
                (windowId, inventory, p) -> ChestMenu.threeRows(windowId, inventory, container),
                Component.literal("Items de Cortisoland")));
        return 1;
    }
}
