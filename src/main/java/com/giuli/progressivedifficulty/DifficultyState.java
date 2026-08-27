package com.giuli.progressivedifficulty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public class DifficultyState {
    private static final String FILE_NAME = "progressive_difficulty.properties";
    private static final DifficultyState INSTANCE = new DifficultyState();

    private int level;
    private double healthPerLevel = 0.20D;
    private double damagePerLevel = 0.10D;

    public static DifficultyState get() {
        return INSTANCE;
    }

    public int level() {
        return level;
    }

    public double healthPerLevel() {
        return healthPerLevel;
    }

    public double damagePerLevel() {
        return damagePerLevel;
    }

    public double healthMultiplier() {
        return 1.0D + level * healthPerLevel;
    }

    public double damageMultiplier() {
        return 1.0D + level * damagePerLevel;
    }

    public void setLevel(int level) {
        this.level = Math.max(0, level);
    }

    public void setHealthPerLevel(double healthPerLevel) {
        this.healthPerLevel = Math.max(0.0D, healthPerLevel);
    }

    public void setDamagePerLevel(double damagePerLevel) {
        this.damagePerLevel = Math.max(0.0D, damagePerLevel);
    }

    public void load(MinecraftServer server) {
        Path file = file(server);
        if (!Files.exists(file)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
            setLevel(readInt(properties, "level", level));
            setHealthPerLevel(readDouble(properties, "healthPerLevel", healthPerLevel));
            setDamagePerLevel(readDouble(properties, "damagePerLevel", damagePerLevel));
        } catch (IOException exception) {
            ProgressiveDifficultyMod.LOGGER.warn("No se pudo cargar {}", file, exception);
        }
    }

    public void save(MinecraftServer server) {
        Path file = file(server);
        Properties properties = new Properties();
        properties.setProperty("level", Integer.toString(level));
        properties.setProperty("healthPerLevel", Double.toString(healthPerLevel));
        properties.setProperty("damagePerLevel", Double.toString(damagePerLevel));

        try {
            Files.createDirectories(file.getParent());
            try (OutputStream output = Files.newOutputStream(file)) {
                properties.store(output, "Progressive Difficulty world settings");
            }
        } catch (IOException exception) {
            ProgressiveDifficultyMod.LOGGER.warn("No se pudo guardar {}", file, exception);
        }
    }

    private Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
    }

    private static int readInt(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static double readDouble(Properties properties, String key, double fallback) {
        try {
            return Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
