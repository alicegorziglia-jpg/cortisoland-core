package com.giuli.progressivedifficulty.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.giuli.progressivedifficulty.FeatureToggles;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Targets every vanilla block whose randomTick() makes it grow (wheat-style
 * crops, cocoa, nether wart and pumpkin/melon stems) and cancels that tick
 * while the "radiation" feature is enabled, so nothing grows on its own.
 */
@Mixin({CropBlock.class, CocoaBlock.class, NetherWartBlock.class, StemBlock.class})
public abstract class RadiationCropGrowthMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void progressiveDifficulty$stopGrowthDuringRadiation(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random, CallbackInfo ci) {
        if (FeatureToggles.get().isEnabled(FeatureToggles.Feature.RADIATION)) {
            ci.cancel();
        }
    }
}
