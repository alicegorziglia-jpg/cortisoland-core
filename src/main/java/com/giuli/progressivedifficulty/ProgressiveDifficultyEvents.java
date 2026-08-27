package com.giuli.progressivedifficulty;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public class ProgressiveDifficultyEvents {
    private static final ResourceLocation HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            ProgressiveDifficultyMod.MOD_ID, "enemy_health_scale");
    private static final ResourceLocation BOSS_HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            ProgressiveDifficultyMod.MOD_ID, "boss_health_per_level");
    private static final double BOSS_HEALTH_PER_LEVEL = 100.0D;

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        DifficultyState.get().load(event.getServer());
        applyToLoadedEnemies(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        DifficultyState.get().save(event.getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dificultadprogresiva")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("ver")
                        .executes(context -> show(context.getSource())))
                .then(Commands.literal("nivel")
                        .then(Commands.argument("nivel", IntegerArgumentType.integer(0, 1000))
                                .executes(context -> {
                                    int level = IntegerArgumentType.getInteger(context, "nivel");
                                    DifficultyState.get().setLevel(level);
                                    DifficultyState.get().save(context.getSource().getServer());
                                    applyToLoadedEnemies(context.getSource().getServer());
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "Nivel de dificultad progresiva cambiado a " + level + "."), true);
                                    return level;
                                })))
                .then(Commands.literal("configurar")
                        .then(Commands.literal("vida_por_nivel")
                                .then(Commands.argument("porcentaje", DoubleArgumentType.doubleArg(0.0D, 1000.0D))
                                        .executes(context -> {
                                            double percent = DoubleArgumentType.getDouble(context, "porcentaje");
                                            DifficultyState.get().setHealthPerLevel(percent / 100.0D);
                                            DifficultyState.get().save(context.getSource().getServer());
                                            applyToLoadedEnemies(context.getSource().getServer());
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                    "Vida extra por nivel: " + formatPercent(percent / 100.0D) + "."), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("danio_por_nivel")
                                .then(Commands.argument("porcentaje", DoubleArgumentType.doubleArg(0.0D, 1000.0D))
                                        .executes(context -> {
                                            double percent = DoubleArgumentType.getDouble(context, "porcentaje");
                                            DifficultyState.get().setDamagePerLevel(percent / 100.0D);
                                            DifficultyState.get().save(context.getSource().getServer());
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                    "Danio extra por nivel: " + formatPercent(percent / 100.0D) + "."), true);
                                            return 1;
                                        })))));
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            applyHealthScale(living);
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Enemy) {
            event.setAmount(event.getAmount() * (float) DifficultyState.get().damageMultiplier());
        }
    }

    private static int show(net.minecraft.commands.CommandSourceStack source) {
        DifficultyState state = DifficultyState.get();
        source.sendSuccess(() -> Component.literal(
                "Nivel " + state.level()
                        + " | vida x" + formatMultiplier(state.healthMultiplier())
                        + " (" + formatPercent(state.healthPerLevel()) + " por nivel)"
                        + " | danio x" + formatMultiplier(state.damageMultiplier())
                        + " (" + formatPercent(state.damagePerLevel()) + " por nivel)"), false);
        return state.level();
    }

    private static void applyToLoadedEnemies(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living) {
                    applyHealthScale(living);
                }
            }
        }
    }

    private static void applyHealthScale(LivingEntity entity) {
        boolean isEnemy = entity instanceof Enemy;
        boolean isBoss = isBoss(entity);
        if (!isEnemy && !isBoss) {
            return;
        }

        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        double healthBefore = entity.getHealth();
        double maxBefore = entity.getMaxHealth();
        maxHealth.removeModifier(HEALTH_MODIFIER_ID);
        maxHealth.removeModifier(BOSS_HEALTH_MODIFIER_ID);

        if (isEnemy) {
            double extraHealth = DifficultyState.get().healthMultiplier() - 1.0D;
            if (extraHealth > 0.0D) {
                maxHealth.addPermanentModifier(new AttributeModifier(
                        HEALTH_MODIFIER_ID,
                        extraHealth,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }

        if (isBoss) {
            double appliedBossExtraHealth = DifficultyState.get().level() * BOSS_HEALTH_PER_LEVEL;
            if (appliedBossExtraHealth > 0.0D) {
                maxHealth.addPermanentModifier(new AttributeModifier(
                        BOSS_HEALTH_MODIFIER_ID,
                        appliedBossExtraHealth,
                        AttributeModifier.Operation.ADD_VALUE));
            }
        }

        double maxAfter = entity.getMaxHealth();
        float scaledHealth = maxBefore <= 0.0D
                ? (float) maxAfter
                : (float) Math.max(1.0D, healthBefore / maxBefore * maxAfter);
        entity.setHealth(Math.min(scaledHealth, entity.getMaxHealth()));
    }

    private static String formatMultiplier(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String formatPercent(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f%%", value * 100.0D);
    }

    private static boolean isBoss(LivingEntity entity) {
        return entity.getType().is(Tags.EntityTypes.BOSSES);
    }
}
