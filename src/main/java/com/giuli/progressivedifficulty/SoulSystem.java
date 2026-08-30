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
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Port of the dedsafio plugin's per-user "soul" flag (User#hasSoul /
 * User#setSoul) and the /soul command. Everything else User/UserManager
 * tracked (dead, revived-times, alert-revive) belongs to the death/revival
 * system, which isn't ported yet - this only carries the one flag /soul
 * actually needs.
 */
public class SoulSystem {
    private static final String FILE_NAME = "progressive_difficulty_souls.properties";
    private static final Map<UUID, Boolean> SOULS = new HashMap<>();

    private SoulSystem() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("soul")
                .then(Commands.argument("jugador", EntityArgument.player())
                        .executes(context -> show(context.getSource(),
                                EntityArgument.getPlayer(context, "jugador"))))
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .then(Commands.argument("alma", BoolArgumentType.bool())
                                        .executes(context -> set(context.getSource(),
                                                EntityArgument.getPlayer(context, "jugador"),
                                                BoolArgumentType.getBool(context, "alma")))))));
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        boolean hasSoul = hasSoul(player.getUUID());
        source.sendSuccess(() -> Component.literal(
                "El jugador " + player.getName().getString()
                        + (hasSoul ? " tiene una alma" : " no tiene una alma")), false);
        return hasSoul ? 1 : 0;
    }

    private static int set(CommandSourceStack source, ServerPlayer player, boolean soul) {
        SOULS.put(player.getUUID(), soul);
        save(source.getServer());
        source.sendSuccess(() -> Component.literal(
                "El jugador " + player.getName().getString()
                        + (soul ? " ahora tiene alma" : " ahora no tiene alma")), true);
        return 1;
    }

    /** Defaults to true, matching the plugin's validateUserProfile default. */
    public static boolean hasSoul(UUID uuid) {
        return SOULS.getOrDefault(uuid, Boolean.TRUE);
    }

    public static void set(UUID uuid, boolean soul) {
        SOULS.put(uuid, soul);
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
                try {
                    SOULS.put(UUID.fromString(key), Boolean.parseBoolean(properties.getProperty(key)));
                } catch (IllegalArgumentException exception) {
                    ProgressiveDifficultyMod.LOGGER.warn("UUID invalido en {}: {}", file, key);
                }
            }
        } catch (IOException exception) {
            ProgressiveDifficultyMod.LOGGER.warn("No se pudo cargar {}", file, exception);
        }
    }

    public static void save(MinecraftServer server) {
        Path file = file(server);
        Properties properties = new Properties();
        SOULS.forEach((uuid, soul) -> properties.setProperty(uuid.toString(), Boolean.toString(soul)));

        try {
            Files.createDirectories(file.getParent());
            try (OutputStream output = Files.newOutputStream(file)) {
                properties.store(output, "Progressive Difficulty per-player soul flags");
            }
        } catch (IOException exception) {
            ProgressiveDifficultyMod.LOGGER.warn("No se pudo guardar {}", file, exception);
        }
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
    }
}
