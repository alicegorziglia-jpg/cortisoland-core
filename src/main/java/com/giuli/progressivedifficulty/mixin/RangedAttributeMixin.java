package com.giuli.progressivedifficulty.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;

@Mixin(RangedAttribute.class)
public abstract class RangedAttributeMixin {
    private static final double PROGRESSIVE_DIFFICULTY_MAX_HEALTH_CAP = 1_000_000.0D;

    @Shadow
    @Final
    @Mutable
    private double maxValue;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void progressiveDifficulty$expandMaxHealthCap(String descriptionId, double defaultValue, double minValue,
            double maxValue, CallbackInfo ci) {
        if ("attribute.name.generic.max_health".equals(descriptionId)
                || "attribute.name.max_health".equals(descriptionId)) {
            this.maxValue = Math.max(this.maxValue, PROGRESSIVE_DIFFICULTY_MAX_HEALTH_CAP);
        }
    }
}
