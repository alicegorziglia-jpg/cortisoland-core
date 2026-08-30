package com.giuli.progressivedifficulty;

import java.util.List;
import java.util.UUID;

import com.giuli.progressivedifficulty.items.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Port of ResurrectionSpoonItem's onRightClick + FogataMenu's revive click
 * handler. Simplified: instead of a paginated player-head GUI, right
 * clicking the spoon inside the fogata bounds (with a soul) lists every
 * dead player as a clickable chat line - clicking a name runs the internal
 * revive command. Functionally the same outcome (pick a dead player, revive
 * them, spend the spoon), much lower risk than building a custom container
 * menu with skull rendering and pagination from scratch.
 */
public class ResurrectionSpoonSystem {
    private ResurrectionSpoonSystem() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Hidden internal command the clickable chat entries run - not gated by
        // permission since any player holding a spoon may use it, but it
        // re-validates everything itself before doing anything.
        dispatcher.register(Commands.literal("cortisoland_revivir_interno")
                .then(Commands.argument("uuid", StringArgumentType.word())
                        .then(Commands.argument("nombre", StringArgumentType.word())
                                .executes(context -> confirmRevive(context.getSource(),
                                        UUID.fromString(StringArgumentType.getString(context, "uuid")),
                                        StringArgumentType.getString(context, "nombre"))))));
    }

    @net.neoforged.bus.api.SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!event.getItemStack().is(ModItems.RESURRECTION_SPOON)) {
            return;
        }
        attemptOpen(event.getEntity());
    }

    private static void attemptOpen(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!FogataSystem.isWithinFogataBounds(player.blockPosition())) {
            player.displayClientMessage(Component.literal("Debes estar cerca de la fogata de resurreccion.")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (!SoulSystem.hasSoul(player.getUUID())) {
            player.displayClientMessage(Component.literal("Debes tener una alma para poder resucitar a un jugador.")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        List<ServerPlayer> deadPlayers = serverPlayer.getServer().getPlayerList().getPlayers().stream()
                .filter(candidate -> DeathSystem.isDead(candidate.getUUID()))
                .toList();

        if (deadPlayers.isEmpty()) {
            player.displayClientMessage(Component.literal("No hay jugadores muertos para revivir."), false);
            return;
        }

        player.sendSystemMessage(Component.literal("Jugadores muertos - click para revivir:")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (ServerPlayer dead : deadPlayers) {
            MutableComponent line = Component.literal("  \u25b8 " + dead.getName().getString())
                    .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/cortisoland_revivir_interno " + dead.getUUID() + " " + dead.getName().getString())));
            player.sendSystemMessage(line);
        }
    }

    private static int confirmRevive(CommandSourceStack source, UUID targetUuid, String targetName) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        if (targetUuid.equals(player.getUUID())) {
            source.sendFailure(Component.literal("No podes revivirte a vos mismo."));
            return 0;
        }

        if (!FogataSystem.isWithinFogataBounds(player.blockPosition())) {
            source.sendFailure(Component.literal("Ya no estas cerca de la fogata."));
            return 0;
        }

        if (!SoulSystem.hasSoul(player.getUUID())) {
            source.sendFailure(Component.literal("Ya no tenes alma."));
            return 0;
        }

        if (!DeathSystem.isDead(targetUuid)) {
            source.sendFailure(Component.literal(targetName + " ya no esta muerto."));
            return 0;
        }

        int spoonSlot = findSpoonSlot(player);
        if (spoonSlot < 0) {
            source.sendFailure(Component.literal("Debes tener una cuchara de resurreccion en tu inventario."));
            return 0;
        }

        SoulSystem.set(player.getUUID(), false);
        SoulSystem.save(source.getServer());

        DeathSystem.reviveWithAlert(targetUuid);
        DeathSystem.save(source.getServer());

        ItemStack stack = player.getInventory().getItem(spoonSlot);
        stack.shrink(1);

        source.sendSuccess(() -> Component.literal("Has revivido a " + targetName + "!"), false);
        return 1;
    }

    private static int findSpoonSlot(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.RESURRECTION_SPOON)) {
                return i;
            }
        }
        return -1;
    }
}
