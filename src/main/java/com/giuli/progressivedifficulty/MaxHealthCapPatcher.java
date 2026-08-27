package com.giuli.progressivedifficulty;

import java.lang.reflect.Field;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

final class MaxHealthCapPatcher {
    private static final double EXTENDED_MAX_HEALTH_CAP = 1_000_000.0D;

    private MaxHealthCapPatcher() {
    }

    static void apply() {
        try {
            Attribute maxHealthAttribute = Attributes.MAX_HEALTH.value();
            if (!(maxHealthAttribute instanceof RangedAttribute rangedAttribute)) {
                ProgressiveDifficultyMod.LOGGER.warn("No se pudo extender max_health: atributo no es RangedAttribute.");
                return;
            }

            Field maxValueField = RangedAttribute.class.getDeclaredField("maxValue");
            maxValueField.setAccessible(true);
            double currentMax = maxValueField.getDouble(rangedAttribute);
            if (currentMax < EXTENDED_MAX_HEALTH_CAP) {
                maxValueField.setDouble(rangedAttribute, EXTENDED_MAX_HEALTH_CAP);
                ProgressiveDifficultyMod.LOGGER.info("max_health extendido de {} a {}.", currentMax,
                        EXTENDED_MAX_HEALTH_CAP);
            }
        } catch (ReflectiveOperationException exception) {
            ProgressiveDifficultyMod.LOGGER.error("Fallo al extender el limite de max_health.", exception);
        }
    }
}
