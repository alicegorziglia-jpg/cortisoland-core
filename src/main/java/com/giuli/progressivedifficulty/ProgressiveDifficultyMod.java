package com.giuli.progressivedifficulty;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import com.giuli.progressivedifficulty.items.ModItems;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(ProgressiveDifficultyMod.MOD_ID)
public class ProgressiveDifficultyMod {
    public static final String MOD_ID = "progressivedifficulty";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ProgressiveDifficultyMod(IEventBus modEventBus, ModContainer modContainer) {
        MaxHealthCapPatcher.apply();
        NeoForge.EVENT_BUS.register(new ProgressiveDifficultyEvents());
        ModItems.ITEMS.register(modEventBus);
    }
}
