package com.giuli.progressivedifficulty;

import java.util.HashMap;
import java.util.Map;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

/**
 * Port of the dedsafio plugin's BossBarManager + /timer command: a named
 * countdown boss bar shown to every online player, ticking down once per
 * second until it hits zero (or is removed early).
 */
public class BossBarSystem {
    private static final Map<String, ActiveTimer> TIMERS = new HashMap<>();

    private record ActiveTimer(ServerBossEvent bossEvent, int totalSeconds, net.minecraft.server.MinecraftServer server) {
    }

    private BossBarSystem() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("timer")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("segundos", IntegerArgumentType.integer(1))
                                .then(Commands.argument("color", StringArgumentType.word())
                                        .then(Commands.argument("estilo", StringArgumentType.word())
                                                .then(Commands.argument("nombre", StringArgumentType.greedyString())
                                                        .executes(context -> {
                                                            int seconds = IntegerArgumentType.getInteger(context, "segundos");
                                                            String color = StringArgumentType.getString(context, "color");
                                                            String style = StringArgumentType.getString(context, "estilo");
                                                            String name = StringArgumentType.getString(context, "nombre");
                                                            return add(context.getSource(), seconds, color, style, name);
                                                        }))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("nombre", StringArgumentType.greedyString())
                                .executes(context -> remove(context.getSource(),
                                        StringArgumentType.getString(context, "nombre"))))));
    }

    private static int add(CommandSourceStack source, int seconds, String colorKey, String styleKey, String name) {
        if (TIMERS.containsKey(name)) {
            source.sendFailure(Component.literal("Ya existe un timer con ese nombre."));
            return 0;
        }

        ServerBossEvent bossEvent = new ServerBossEvent(
                Component.literal(name), toColor(colorKey), toOverlay(styleKey));
        TIMERS.put(name, new ActiveTimer(bossEvent, seconds, source.getServer()));
        tick(name, seconds);

        source.sendSuccess(() -> Component.literal("Timer '" + name + "' iniciado (" + seconds + "s)."), true);
        return seconds;
    }

    private static int remove(CommandSourceStack source, String name) {
        ActiveTimer timer = TIMERS.remove(name);
        if (timer == null) {
            source.sendFailure(Component.literal("No existe un timer con ese nombre."));
            return 0;
        }

        timer.bossEvent().removeAllPlayers();
        source.sendSuccess(() -> Component.literal("Timer '" + name + "' detenido."), true);
        return 1;
    }

    private static void tick(String name, int secondsLeft) {
        ActiveTimer timer = TIMERS.get(name);
        if (timer == null) {
            return; // removed early
        }

        if (secondsLeft <= 0) {
            timer.bossEvent().removeAllPlayers();
            TIMERS.remove(name);
            return;
        }

        for (ServerPlayer player : timer.server().getPlayerList().getPlayers()) {
            if (!timer.bossEvent().getPlayers().contains(player)) {
                timer.bossEvent().addPlayer(player);
            }
        }

        timer.bossEvent().setName(Component.literal(name + " (" + secondsLeft + "s)"));
        timer.bossEvent().setProgress((float) secondsLeft / timer.totalSeconds());

        DelayedTaskScheduler.schedule(20, () -> tick(name, secondsLeft - 1));
    }

    private static BossEvent.BossBarColor toColor(String key) {
        return switch (key) {
            case "pink" -> BossEvent.BossBarColor.PINK;
            case "blue" -> BossEvent.BossBarColor.BLUE;
            case "green" -> BossEvent.BossBarColor.GREEN;
            case "yellow" -> BossEvent.BossBarColor.YELLOW;
            case "purple" -> BossEvent.BossBarColor.PURPLE;
            case "white" -> BossEvent.BossBarColor.WHITE;
            default -> BossEvent.BossBarColor.RED;
        };
    }

    private static BossEvent.BossBarOverlay toOverlay(String key) {
        return switch (key) {
            case "6" -> BossEvent.BossBarOverlay.NOTCHED_6;
            case "10" -> BossEvent.BossBarOverlay.NOTCHED_10;
            case "12" -> BossEvent.BossBarOverlay.NOTCHED_12;
            case "20" -> BossEvent.BossBarOverlay.NOTCHED_20;
            default -> BossEvent.BossBarOverlay.PROGRESS;
        };
    }
}
