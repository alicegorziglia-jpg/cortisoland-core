package com.giuli.progressivedifficulty;

import java.util.List;
import java.util.UUID;

import com.giuli.progressivedifficulty.items.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Port of ResurrectionSpoonItem's onRightClick + FogataMenu's revive click
 * handler, plus the fork as a soul-free equivalent (per request: "el tenedor
 * tiene casi las mismas funciones que la cuchara" - same revival flow, minus
 * the soul cost). Opens a real player-head menu (ReviveMenu) - clicking a
 * head revives that player, consuming the triggering item and (for the
 * spoon only) a soul.
 */
public class ResurrectionSpoonSystem {
    ResurrectionSpoonSystem() {
    }

    public static void register(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        // Nothing to register as a command anymore - revival happens entirely
        // through clicking a head in the menu now.
    }

    @net.neoforged.bus.api.SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        handle(event.getEntity(), event.getEntity().blockPosition(), event.getHand());
    }

    @net.neoforged.bus.api.SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        handle(event.getEntity(), event.getPos(), event.getHand());
    }

    private static void handle(Player player, net.minecraft.core.BlockPos targetPos, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean isSpoon = held.is(ModItems.RESURRECTION_SPOON);
        boolean isFork = held.is(ModItems.FORK);
        if (!isSpoon && !isFork) {
            return;
        }

        attemptOpen(player, targetPos, hand, isSpoon);
    }

    private static void attemptOpen(Player player, net.minecraft.core.BlockPos targetPos,
            InteractionHand hand, boolean consumesSoul) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!FogataSystem.isWithinFogataBounds(targetPos)) {
            player.displayClientMessage(Component.literal("Debes estar cerca de la fogata de resurreccion.")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (consumesSoul && !SoulSystem.hasSoul(player.getUUID())) {
            player.displayClientMessage(Component.literal("Debes tener una alma para poder resucitar a un jugador.")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        java.util.Map<UUID, String> deadEntries = DeathSystem.getDeadEntries();

        if (deadEntries.isEmpty()) {
            player.displayClientMessage(Component.literal("No hay jugadores muertos para revivir."), false);
            return;
        }

        serverPlayer.openMenu(new SimpleMenuProvider(
                (windowId, inventory, p) -> ReviveMenu.create(windowId, inventory, serverPlayer, deadEntries, consumesSoul, hand),
                Component.literal("Revivir jugador")));
    }

    /** Called by ReviveMenu when a head is clicked. */
    public static void performRevive(ServerPlayer viewer, UUID targetUuid, boolean consumesSoul, InteractionHand hand) {
        if (targetUuid.equals(viewer.getUUID())) {
            viewer.displayClientMessage(Component.literal("No podes revivirte a vos mismo.")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (!DeathSystem.isDead(targetUuid)) {
            viewer.displayClientMessage(Component.literal("Ese jugador ya no esta muerto.")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        ItemStack held = viewer.getItemInHand(hand);
        if (held.isEmpty() || !(held.is(ModItems.RESURRECTION_SPOON) || held.is(ModItems.FORK))) {
            viewer.displayClientMessage(Component.literal("Ya no tenes el item necesario en la mano.")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (consumesSoul) {
            if (!SoulSystem.hasSoul(viewer.getUUID())) {
                viewer.displayClientMessage(Component.literal("Ya no tenes alma.")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            SoulSystem.set(viewer.getUUID(), false);
            SoulSystem.save(viewer.getServer());
        }

        String targetName = DeathSystem.getDeadEntries().getOrDefault(targetUuid, "el jugador");

        DeathSystem.reviveWithAlert(targetUuid);
        DeathSystem.save(viewer.getServer());

        held.shrink(1);

        viewer.displayClientMessage(Component.literal("Has revivido a " + targetName + "!")
                .withStyle(ChatFormatting.GREEN), false);
    }
}
