package com.giuli.progressivedifficulty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Port of the dedsafio plugin's /config get/set: a free-form string
 * key/value store, persisted per world. Unlike {@link FeatureToggles} this
 * has no fixed set of keys - it's meant for ad-hoc values (thresholds,
 * messages, whatever a future feature wants to read back later).
 */
public class ModConfigStore {
    private static final String FILE_NAME = "progressive_difficulty_config.properties";
    private static final ModConfigStore INSTANCE = new ModConfigStore();

    private final Map<String, String> values = new HashMap<>();

    private ModConfigStore() {
    }

    public static ModConfigStore get() {
        return INSTANCE;
    }

    public String get(String key) {
        return values.get(key);
    }

    public void set(String key, String value) {
        values.put(key, value);
    }

    public void load(MinecraftServer server) {
        Path file = file(server);
        if (!Files.exists(file)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
            for (String key : properties.stringPropertyNames()) {
                values.put(key, properties.getProperty(key));
            }
        } catch (IOException exception) {
            ProgressiveDifficultyMod.LOGGER.warn("No se pudo cargar {}", file, exception);
        }
    }

    public void save(MinecraftServer server) {
        Path file = file(server);
        Properties properties = new Properties();
        values.forEach(properties::setProperty);

        try {
            Files.createDirectories(file.getParent());
            try (OutputStream output = Files.newOutputStream(file)) {
                properties.store(output, "Progressive Difficulty free-form config values");
            }
        } catch (IOException exception) {
            ProgressiveDifficultyMod.LOGGER.warn("No se pudo guardar {}", file, exception);
        }
    }

    private Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
    }
}
