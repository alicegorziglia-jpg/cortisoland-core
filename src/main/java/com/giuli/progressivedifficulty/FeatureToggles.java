package com.giuli.progressivedifficulty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Properties;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Holds the on/off state of every optional danger feature added on top of the
 * base progressive difficulty system. Each feature is independent and is
 * persisted per-world, the same way {@link DifficultyState} is.
 */
public class FeatureToggles {
    private static final String FILE_NAME = "progressive_difficulty_features.properties";
    private static final FeatureToggles INSTANCE = new FeatureToggles();

    public enum Feature {
        LAVA("lava_purifica_remueve_resistencia_fuego"),
        DMG_DOORS("puertas_danio"),
        DMG_BUTTONS("botones_danio"),
        PHANTOM_SHUFFLE("phantom_ciega_mezcla_hotbar"),
        PLAGUE_CONTROL("control_de_plaga_animales"),
        RADIATION("radiacion_cultivos_no_crecen"),
        FIEBRE_TEJEDORA("fiebre_tejedora_telaranias"),
        FIEBRE_TARACNIDA("fiebre_taracnida_invoca_aranias"),
        FUEGO_ETERNO("fuego_eterno"),
        NO_BREATHING("puertas_no_ayudan_a_respirar"),
        INFERNUS("infernus_danio_fuego_doble"),
        LAVA_MORTAL("lava_mortal_instantanea"),
        PIES_DEBILES("pies_debiles_mas_danio_caida"),
        TOTEM_MESSAGES("mensajes_de_totem"),
        TOTEM_WEAK("totems_normales_al_50_por_ciento"),

        // --- Portados del plugin dedsafio (events/changes) ---
        DOORS_INSTAKILL("puertas_matan_al_instante"),
        BUTTONS_INSTAKILL("botones_matan_al_instante"),
        DISABLE_NETHER("nether_desactivado"),
        ELECTRIC_CREEPERS("creepers_siempre_cargados"),
        NO_VILLAGER_BREEDING("aldeanos_no_se_reproducen"),
        ENDERPEARL_HALF_HEALTH("enderpearl_quita_mitad_de_vida"),
        PIGLINS_DROP_NUGGETS("piglins_sueltan_pepitas_de_oro"),
        GOLEMS_REPLACED_BY_WARDENS("golems_de_hierro_reemplazados_por_wardens"),
        DEATH_SYSTEM("sistema_de_muerte_eliminacion");

        private final String key;

        Feature(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private final EnumMap<Feature, Boolean> states = new EnumMap<>(Feature.class);

    private FeatureToggles() {
        for (Feature feature : Feature.values()) {
            states.put(feature, Boolean.FALSE);
        }
    }

    public static FeatureToggles get() {
        return INSTANCE;
    }

    public boolean isEnabled(Feature feature) {
        return states.getOrDefault(feature, Boolean.FALSE);
    }

    public boolean toggle(Feature feature) {
        boolean newState = !isEnabled(feature);
        states.put(feature, newState);
        return newState;
    }

    public void set(Feature feature, boolean value) {
        states.put(feature, value);
    }

    public void load(MinecraftServer server) {
        Path file = file(server);
        if (!Files.exists(file)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
            for (Feature feature : Feature.values()) {
                String value = properties.getProperty(feature.key());
                if (value != null) {
                    states.put(feature, Boolean.parseBoolean(value));
                }
            }
        } catch (IOException exception) {
            ProgressiveDifficultyMod.LOGGER.warn("No se pudo cargar {}", file, exception);
        }
    }

    public void save(MinecraftServer server) {
        Path file = file(server);
        Properties properties = new Properties();
        for (Feature feature : Feature.values()) {
            properties.setProperty(feature.key(), Boolean.toString(isEnabled(feature)));
        }

        try {
            Files.createDirectories(file.getParent());
            try (OutputStream output = Files.newOutputStream(file)) {
                properties.store(output, "Progressive Difficulty feature toggles");
            }
        } catch (IOException exception) {
            ProgressiveDifficultyMod.LOGGER.warn("No se pudo guardar {}", file, exception);
        }
    }

    private Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
    }
}
