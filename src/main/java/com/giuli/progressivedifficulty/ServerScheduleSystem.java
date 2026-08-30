package com.giuli.progressivedifficulty;

import java.time.LocalTime;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

/**
 * Port of the dedsafio plugin's TimeController: kicks non-op players and
 * enables the whitelist once the configured close time is reached, with a
 * countdown boss bar in the minutes before closing. Checked once per
 * in-game minute (1200 ticks) rather than Bukkit's real-time scheduler.
 */
public class ServerScheduleSystem {
    private static LocalTime openTime = LocalTime.of(0, 0);
    private static LocalTime closeTime = LocalTime.of(23, 59);
    private static boolean enabled = false;
    private static int bossBarWindowSeconds = 300;

    private static ServerBossEvent closeAlertBossEvent;
    private static boolean countdownRunning = false;

    private ServerScheduleSystem() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("horario")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("activar").executes(context -> setEnabled(context.getSource(), true)))
                .then(Commands.literal("desactivar").executes(context -> setEnabled(context.getSource(), false)))
                .then(Commands.literal("abrir")
                        .then(Commands.argument("hora", StringArgumentType.string())
                                .executes(context -> setOpen(context.getSource(),
                                        StringArgumentType.getString(context, "hora")))))
                .then(Commands.literal("cerrar")
                        .then(Commands.argument("hora", StringArgumentType.string())
                                .executes(context -> setClose(context.getSource(),
                                        StringArgumentType.getString(context, "hora"))))));
    }

    private static int setEnabled(CommandSourceStack source, boolean value) {
        enabled = value;
        source.sendSuccess(() -> Component.literal(
                "Horario del servidor: " + (value ? "ACTIVADO" : "DESACTIVADO")), true);
        return 1;
    }

    private static int setOpen(CommandSourceStack source, String hhmm) {
        openTime = parse(hhmm);
        source.sendSuccess(() -> Component.literal("Hora de apertura: " + openTime), true);
        return 1;
    }

    private static int setClose(CommandSourceStack source, String hhmm) {
        closeTime = parse(hhmm);
        source.sendSuccess(() -> Component.literal("Hora de cierre: " + closeTime), true);
        return 1;
    }

    private static LocalTime parse(String hhmm) {
        String[] parts = hhmm.split(":");
        return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    /** Called once per in-game minute from ProgressiveDifficultyEvents' server tick handler. */
    public static void checkSchedule(MinecraftServer server) {
        if (!enabled) {
            return;
        }

        LocalTime now = LocalTime.now();

        if (now.isAfter(openTime) && now.isBefore(closeTime)) {
            long secondsUntilClose = java.time.Duration.between(now, closeTime).getSeconds();
            if (secondsUntilClose > 0 && secondsUntilClose <= bossBarWindowSeconds && !countdownRunning) {
                startCountdown(server, secondsUntilClose);
            }
        } else if (!now.isBefore(closeTime) && countdownRunning) {
            kickPlayersAndEnableWhitelist(server);
        }
    }

    private static void startCountdown(MinecraftServer server, long initialSeconds) {
        countdownRunning = true;
        closeAlertBossEvent = new ServerBossEvent(
                Component.literal("El servidor cierra pronto"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        tickCountdown(server, initialSeconds, initialSeconds);
    }

    private static void tickCountdown(MinecraftServer server, long secondsLeft, long totalSeconds) {
        if (!countdownRunning || closeAlertBossEvent == null) {
            return;
        }

        if (secondsLeft <= 0) {
            kickPlayersAndEnableWhitelist(server);
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!closeAlertBossEvent.getPlayers().contains(player)) {
                closeAlertBossEvent.addPlayer(player);
            }
        }

        closeAlertBossEvent.setName(Component.literal("El servidor cierra en " + secondsLeft + "s"));
        closeAlertBossEvent.setProgress((float) secondsLeft / totalSeconds);

        DelayedTaskScheduler.schedule(20, () -> tickCountdown(server, secondsLeft - 1, totalSeconds));
    }

    private static void kickPlayersAndEnableWhitelist(MinecraftServer server) {
        server.getPlayerList().setUsingWhiteList(true);

        for (ServerPlayer player : java.util.List.copyOf(server.getPlayerList().getPlayers())) {
            if (!server.getPlayerList().isOp(player.getGameProfile())) {
                player.connection.disconnect(Component.literal("El servidor esta cerrado en este horario."));
            }
        }

        if (closeAlertBossEvent != null) {
            closeAlertBossEvent.removeAllPlayers();
        }
        countdownRunning = false;
    }
}
