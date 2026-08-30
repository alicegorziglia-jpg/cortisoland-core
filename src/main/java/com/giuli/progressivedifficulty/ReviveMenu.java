package com.giuli.progressivedifficulty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

/**
 * Player-head revival menu, port of FogataMenu (without pagination, since
 * the death system realistically won't have more than ~27 dead players at
 * once). Extends the vanilla ChestMenu so it gets the standard chest GUI
 * for free (texture, slot layout, network sync) - only clicked() is
 * overridden so clicking a head revives that player instead of moving the
 * item around.
 */
public class ReviveMenu extends ChestMenu {
    private final List<UUID> slotToPlayer;
    private final ServerPlayer viewer;
    private final boolean consumesSoul;
    private final InteractionHand triggeringHand;

    public static ReviveMenu create(int windowId, Inventory playerInventory, ServerPlayer viewer,
            List<ServerPlayer> deadPlayers, boolean consumesSoul, InteractionHand triggeringHand) {
        SimpleContainer container = new SimpleContainer(27);
        List<UUID> mapping = new ArrayList<>();

        int slot = 0;
        for (ServerPlayer dead : deadPlayers) {
            if (slot >= 27) {
                break;
            }
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, new ResolvableProfile(dead.getGameProfile()));
            head.set(DataComponents.CUSTOM_NAME, Component.literal(dead.getName().getString()));
            container.setItem(slot, head);
            mapping.add(dead.getUUID());
            slot++;
        }

        return new ReviveMenu(windowId, playerInventory, container, mapping, viewer, consumesSoul, triggeringHand);
    }

    private ReviveMenu(int windowId, Inventory playerInventory, Container container, List<UUID> mapping,
            ServerPlayer viewer, boolean consumesSoul, InteractionHand triggeringHand) {
        super(MenuType.GENERIC_9x3, windowId, playerInventory, container, 3);
        this.slotToPlayer = mapping;
        this.viewer = viewer;
        this.consumesSoul = consumesSoul;
        this.triggeringHand = triggeringHand;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slotToPlayer.size()) {
            UUID target = slotToPlayer.get(slotId);
            ResurrectionSpoonSystem.performRevive(viewer, target, consumesSoul, triggeringHand);
            viewer.closeContainer();
            return;
        }
        // Ignore every other slot (player's own inventory, empty catalog
        // slots) - nothing can be taken out of or dropped into this menu.
    }
}
