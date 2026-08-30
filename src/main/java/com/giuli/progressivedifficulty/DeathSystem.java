package com.giuli.progressivedifficulty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Port of the dedsafio plugin's elimination "death system": User#isDead(),
 * User#getAlertRevive()/setAlertRevive(), User#addRevivedTimes(), the
 * on-death spectator+kick flow, and /revive. Not ported: the exact death
 * respawn-location pinning (cosmetic, player is kicked seconds later
 * anyway) and real vanilla ban-list integration (we use our own persisted
 * "dead" flag + a kick-on-join check instead, which is simpler and
 * functionally equivalent for this use case).
 */
public class DeathSystem {
    private static final String FILE_NAME = "progressive_difficulty_death_system.properties";
    private static final int BAN_AFTER_SECONDS = 10;
    private static final int DEFAULT_LIVES = 5;

    private static final Map<UUID, Boolean> DEAD = new HashMap<>();
    private static final Map<UUID, Boolean> ALERT_REVIVE = new HashMap<>();
    private static final Map<UUID, Integer> REVIVED_TIMES = new HashMap<>();
    private static final Map<UUID, Integer> DEATH_COUNT = new HashMap<>();
    private static int livesBeforeElimination = DEFAULT_LIVES;

    private DeathSystem() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("revive")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("jugador", GameProfileArgument.gameProfile())
                        .then(Commands.argument("avisar", BoolArgumentType.bool())
                                .executes(context -> revive(context.getSource(),
                                        GameProfileArgument.getGameProfiles(context, "jugador")
                                                .iterator().next().getId(),
                                        GameProfileArgument.getGameProfiles(context, "jugador")
                                                .iterator().next().getName(),
                                        BoolArgumentType.getBool(context, "avisar"))))));

        dispatcher.register(Commands.literal("vidas")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("ver")
                        .then(Commands.argument("jugador", GameProfileArgument.gameProfile())
                                .executes(context -> {
                                    com.mojang.authlib.GameProfile profile = GameProfileArgument
                                            .getGameProfiles(context, "jugador").iterator().next();
                                    int count = getDeathCount(profile.getId());
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            profile.getName() + ": " + count + "/" + livesBeforeElimination
                                                    + " muertes"), false);
                                    return count;
                                })))
                .then(Commands.literal("resetear")
                        .then(Commands.argument("jugador", GameProfileArgument.gameProfile())
                                .executes(context -> {
                                    com.mojang.authlib.GameProfile profile = GameProfileArgument
                                            .getGameProfiles(context, "jugador").iterator().next();
                                    DEATH_COUNT.put(profile.getId(), 0);
                                    save(context.getSource().getServer());
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "Contador de muertes de " + profile.getName() + " reiniciado a 0."), true);
                                    return 1;
                                })))
                .then(Commands.literal("maximo")
                        .then(Commands.argument("cantidad", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    livesBeforeElimination = com.mojang.brigadier.arguments.IntegerArgumentType
                                            .getInteger(context, "cantidad");
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "Ahora se necesitan " + livesBeforeElimination
                                                    + " muertes para ser eliminado."), true);
                                    return livesBeforeElimination;
                                }))));
    }

    private static int revive(CommandSourceStack source, UUID uuid, String name, boolean alertUsers) {
        if (!isDead(uuid)) {
            source.sendFailure(Component.literal(name + " ya esta vivo."));
            return 0;
        }

        DEAD.put(uuid, false);
        if (!alertUsers) {
            ALERT_REVIVE.put(uuid, true);
            REVIVED_TIMES.merge(uuid, 1, Integer::sum);
        }
        save(source.getServer());

        source.sendSuccess(() -> Component.literal("Reviviste a " + name + " exitosamente."), true);
        return 1;
    }

    public static boolean isDead(UUID uuid) {
        return DEAD.getOrDefault(uuid, false);
    }

    /** Revives uuid and flags an alert-on-next-join, matching /revive's alertUsers=false path. */
    public static void reviveWithAlert(UUID uuid) {
        DEAD.put(uuid, false);
        ALERT_REVIVE.put(uuid, true);
        REVIVED_TIMES.merge(uuid, 1, Integer::sum);
    }

    /**
     * Counts a death towards uuid's total and reports whether this death
     * should trigger the full elimination flow (spectator/kick/animation).
     * Returns true only once the configured threshold is reached or passed.
     */
    public static boolean registerDeathAndCheckElimination(UUID uuid) {
        int newCount = DEATH_COUNT.merge(uuid, 1, Integer::sum);
        return newCount >= livesBeforeElimination;
    }

    public static int getDeathCount(UUID uuid) {
        return DEATH_COUNT.getOrDefault(uuid, 0);
    }

    public static int getLivesBeforeElimination() {
        return livesBeforeElimination;
    }

    public static void markDead(ServerPlayer player) {
        DEAD.put(player.getUUID(), true);
        save(player.getServer());
    }

    public static boolean consumeAlertRevive(UUID uuid) {
        boolean alert = ALERT_REVIVE.getOrDefault(uuid, false);
        if (alert) {
            ALERT_REVIVE.put(uuid, false);
        }
        return alert;
    }

    public static int getRevivedTimes(UUID uuid) {
        return REVIVED_TIMES.getOrDefault(uuid, 0);
    }

    public static int banAfterSeconds() {
        return BAN_AFTER_SECONDS;
    }

    public static void load(MinecraftServer server) {
        Path file = file(server);
        if (!Files.exists(file)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
            for (String key : properties.stringPropertyNames()) {
                String[] parts = key.split(":", 2);
                if (parts.length != 2) {
                    continue;
                }
                UUID uuid;
                try {
                    uuid = UUID.fromString(parts[0]);
                } catch (IllegalArgumentException exception) {
                    continue;
                }
                String value = properties.getProperty(key);
                switch (parts[1]) {
                    case "dead" -> DEAD.put(uuid, Boolean.parseBoolean(value));
                    case "alertRevive" -> ALERT_REVIVE.put(uuid, Boolean.parseBoolean(value));
                    case "revivedTimes" -> REVIVED_TIMES.put(uuid, Integer.parseInt(value));
                    case "deathCount" -> DEATH_COUNT.put(uuid, Integer.parseInt(value));
                    default -> {
                    }
                }
            }

            String livesValue = properties.getProperty("livesBeforeElimination");
            if (livesValue != null) {
                try {
                    livesBeforeElimination = Integer.parseInt(livesValue);
                } catch (NumberFormatException exception) {
                    // keep default
                }
            }
        } catch (IOException exception) {
            ProgressiveDifficultyMod.LOGGER.warn("No se pudo cargar {}", file, exception);
        }
    }

    public static void save(MinecraftServer server) {
        Path file = file(server);
        Properties properties = new Properties();
        DEAD.forEach((uuid, value) -> properties.setProperty(uuid + ":dead", Boolean.toString(value)));
        ALERT_REVIVE.forEach((uuid, value) -> properties.setProperty(uuid + ":alertRevive", Boolean.toString(value)));
        REVIVED_TIMES.forEach((uuid, value) -> properties.setProperty(uuid + ":revivedTimes", Integer.toString(value)));
        DEATH_COUNT.forEach((uuid, value) -> properties.setProperty(uuid + ":deathCount", Integer.toString(value)));
        properties.setProperty("livesBeforeElimination", Integer.toString(livesBeforeElimination));

        try {
            Files.createDirectories(file.getParent());
            try (OutputStream output = Files.newOutputStream(file)) {
                properties.store(output, "Progressive Difficulty death/revive system state");
            }
        } catch (IOException exception) {
            ProgressiveDifficultyMod.LOGGER.warn("No se pudo guardar {}", file, exception);
        }
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
    }
}
