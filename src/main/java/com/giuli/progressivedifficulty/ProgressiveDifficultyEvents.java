package com.giuli.progressivedifficulty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class ProgressiveDifficultyEvents {
    private static final ResourceLocation HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            ProgressiveDifficultyMod.MOD_ID, "enemy_health_scale");
    private static final ResourceLocation BOSS_HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            ProgressiveDifficultyMod.MOD_ID, "boss_health_per_level");
    private static final double BOSS_HEALTH_PER_LEVEL = 100.0D;

    private static final String GUARANTEED_TOTEM_KEY = "progressivedifficulty_guaranteed_totem";
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        DifficultyState.get().load(event.getServer());
        FeatureToggles.get().load(event.getServer());
        applyToLoadedEnemies(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        DifficultyState.get().save(event.getServer());
        FeatureToggles.get().save(event.getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("dificultadprogresiva")
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

        // --- Comandos de peligros toggleables ---
        registerToggleCommand(dispatcher, "lava", FeatureToggles.Feature.LAVA,
                "Lava purificadora (quita resistencia al fuego)");
        registerToggleCommand(dispatcher, "dmgdoors", FeatureToggles.Feature.DMG_DOORS,
                "Danio al abrir puertas");
        registerToggleCommand(dispatcher, "dmgbuttons", FeatureToggles.Feature.DMG_BUTTONS,
                "Danio al usar botones");
        registerToggleCommand(dispatcher, "phantomshuffle", FeatureToggles.Feature.PHANTOM_SHUFFLE,
                "Phantom ciega y mezcla la hotbar");
        registerToggleCommand(dispatcher, "plaguecontrol", FeatureToggles.Feature.PLAGUE_CONTROL,
                "Control de plaga de animales");
        registerToggleCommand(dispatcher, "radiation", FeatureToggles.Feature.RADIATION,
                "Radiacion (los cultivos no crecen)");
        registerToggleCommand(dispatcher, "fiebretejedora", FeatureToggles.Feature.FIEBRE_TEJEDORA,
                "Fiebre tejedora (las aranias tejen tela en tus pies)");
        registerToggleCommand(dispatcher, "fiebretaracnida", FeatureToggles.Feature.FIEBRE_TARACNIDA,
                "Fiebre taracnida (destruir telaranias invoca aranias de cueva)");
        registerToggleCommand(dispatcher, "fuegoeterno", FeatureToggles.Feature.FUEGO_ETERNO,
                "Fuego eterno (el fuego no se apaga salvo bajo el agua)");
        registerToggleCommand(dispatcher, "nobreathing", FeatureToggles.Feature.NO_BREATHING,
                "Las puertas ya no ayudan a respirar bajo el agua");
        registerToggleCommand(dispatcher, "infernus", FeatureToggles.Feature.INFERNUS,
                "Infernus (el danio de fuego se duplica)");
        registerToggleCommand(dispatcher, "lavamortal", FeatureToggles.Feature.LAVA_MORTAL,
                "Lava mortal (mata al instante)");
        registerToggleCommand(dispatcher, "piesdebiles", FeatureToggles.Feature.PIES_DEBILES,
                "Pies debiles (mucho mas danio de caida)");
        registerToggleCommand(dispatcher, "totemmesagges", FeatureToggles.Feature.TOTEM_MESSAGES,
                "Notificaciones al consumir un totem");
        registerToggleCommand(dispatcher, "totemdebil", FeatureToggles.Feature.TOTEM_WEAK,
                "Totems normales al 50% de efectividad");

        // --- Comando para dar totems que siempre funcionan ---
        dispatcher.register(Commands.literal("totemverdadero")
                .requires(source -> source.hasPermission(2))
                .executes(context -> giveGuaranteedTotem(context.getSource(), 1))
                .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 64))
                        .executes(context -> giveGuaranteedTotem(context.getSource(),
                                IntegerArgumentType.getInteger(context, "cantidad")))));
    }

    private static void registerToggleCommand(CommandDispatcher<CommandSourceStack> dispatcher, String name,
            FeatureToggles.Feature feature, String label) {
        dispatcher.register(Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    boolean enabled = FeatureToggles.get().toggle(feature);
                    FeatureToggles.get().save(context.getSource().getServer());
                    context.getSource().sendSuccess(() -> Component.literal(
                            label + ": " + (enabled ? "\u00a7aACTIVADO" : "\u00a7cDESACTIVADO")), true);
                    return enabled ? 1 : 0;
                }));
    }

    private static int giveGuaranteedTotem(CommandSourceStack source, int amount) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        ItemStack totem = new ItemStack(Items.TOTEM_OF_UNDYING, amount);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(GUARANTEED_TOTEM_KEY, true);
        totem.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        totem.set(DataComponents.CUSTOM_NAME, Component.literal("Totem Verdadero")
                .withStyle(style -> style.withColor(ChatFormatting.LIGHT_PURPLE).withItalic(false)));

        player.getInventory().placeItemBackInInventory(totem);
        source.sendSuccess(() -> Component.literal(
                "Recibiste " + amount + " Totem(es) Verdadero(s): siempre funcionan al 100%."), true);
        return amount;
    }

    private static boolean isGuaranteedTotem(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.copyTag().getBoolean(GUARANTEED_TOTEM_KEY);
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof LivingEntity living) {
            applyHealthScale(living);
        }

        if (!event.loadedFromDisk()
                && event.getEntity() instanceof Animal animal
                && FeatureToggles.get().isEnabled(FeatureToggles.Feature.PLAGUE_CONTROL)) {
            boolean tamed = animal instanceof TamableAnimal tamable && tamable.isTame();
            boolean baby = animal instanceof AgeableMob ageable && ageable.isBaby();
            boolean named = animal.hasCustomName();
            boolean inBoat = animal.getVehicle() instanceof Boat;

            if (!tamed && !baby && !named && !inBoat) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();

        if (attacker instanceof Enemy) {
            event.setAmount(event.getAmount() * (float) DifficultyState.get().damageMultiplier());
        }

        if (attacker instanceof Spider
                && FeatureToggles.get().isEnabled(FeatureToggles.Feature.FIEBRE_TEJEDORA)
                && event.getEntity().level() instanceof ServerLevel serverLevel) {
            BlockPos feet = event.getEntity().blockPosition();
            if (serverLevel.getBlockState(feet).isAir()) {
                serverLevel.setBlockAndUpdate(feet, Blocks.COBWEB.defaultBlockState());
            }
        }

        if (FeatureToggles.get().isEnabled(FeatureToggles.Feature.LAVA_MORTAL)
                && event.getSource().is(DamageTypes.LAVA)) {
            event.setAmount(Math.max(event.getAmount(), 10000.0F));
        } else if (FeatureToggles.get().isEnabled(FeatureToggles.Feature.INFERNUS)
                && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setAmount(event.getAmount() * 2.0F);
        }

        if (FeatureToggles.get().isEnabled(FeatureToggles.Feature.PIES_DEBILES)
                && event.getSource().is(DamageTypes.FALL)) {
            event.setAmount(event.getAmount() * 3.0F);
        }

        if (attacker instanceof net.minecraft.world.entity.monster.Phantom
                && FeatureToggles.get().isEnabled(FeatureToggles.Feature.PHANTOM_SHUFFLE)) {
            LivingEntity target = event.getEntity();
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            shuffleHotbar(target);
        }
    }

    private static void shuffleHotbar(LivingEntity target) {
        if (!(target instanceof ServerPlayer player)) {
            return;
        }

        Inventory inventory = player.getInventory();
        List<ItemStack> hotbar = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            hotbar.add(inventory.getItem(i));
        }
        Collections.shuffle(hotbar, RANDOM);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, hotbar.get(i));
        }
        player.inventoryMenu.broadcastChanges();
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        BlockState state = event.getLevel().getBlockState(event.getPos());
        Player player = event.getEntity();

        if (state.getBlock() instanceof DoorBlock
                && FeatureToggles.get().isEnabled(FeatureToggles.Feature.DMG_DOORS)) {
            player.hurt(player.damageSources().generic(), 2.0F);
        }

        if (state.getBlock() instanceof ButtonBlock
                && FeatureToggles.get().isEnabled(FeatureToggles.Feature.DMG_BUTTONS)) {
            player.hurt(player.damageSources().generic(), 1.0F);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!FeatureToggles.get().isEnabled(FeatureToggles.Feature.FIEBRE_TARACNIDA)) {
            return;
        }
        if (!event.getState().is(Blocks.COBWEB)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        CaveSpider caveSpider = EntityType.CAVE_SPIDER.create(serverLevel);
        if (caveSpider != null) {
            BlockPos pos = event.getPos();
            caveSpider.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
            serverLevel.addFreshEntity(caveSpider);
        }
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (entity.level().isClientSide()) {
            return;
        }

        FeatureToggles toggles = FeatureToggles.get();

        if (toggles.isEnabled(FeatureToggles.Feature.LAVA)
                && entity.isInLava()
                && entity.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            entity.removeEffect(MobEffects.FIRE_RESISTANCE);
        }

        if (toggles.isEnabled(FeatureToggles.Feature.FUEGO_ETERNO)
                && entity.isOnFire()
                && !entity.isUnderWater()) {
            entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 40));
        }

        if (toggles.isEnabled(FeatureToggles.Feature.NO_BREATHING)
                && entity instanceof Player player
                && player.isEyeInFluid(FluidTags.WATER)
                && isNearDoor(player)) {
            player.setAirSupply(Math.max(-20, player.getAirSupply() - 1));
        }
    }

    private static boolean isNearDoor(Player player) {
        BlockPos base = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-1, -1, -1), base.offset(1, 1, 1))) {
            if (player.level().getBlockState(pos).getBlock() instanceof DoorBlock) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onUseTotem(LivingUseTotemEvent event) {
        ItemStack totem = event.getTotem();
        boolean guaranteed = isGuaranteedTotem(totem);

        if (!guaranteed && FeatureToggles.get().isEnabled(FeatureToggles.Feature.TOTEM_WEAK)) {
            if (RANDOM.nextDouble() >= 0.5D) {
                event.setCanceled(true);
            }
        }

        if (FeatureToggles.get().isEnabled(FeatureToggles.Feature.TOTEM_MESSAGES)
                && event.getEntity().level() instanceof ServerLevel serverLevel) {
            boolean saved = !event.isCanceled();
            Component message = Component.literal(event.getEntity().getName().getString()
                    + (saved ? " uso un Totem de la Inmortalidad y sobrevivio."
                            : " uso un Totem de la Inmortalidad, pero fallo."));
            serverLevel.getServer().getPlayerList().broadcastSystemMessage(message, false);
        }
    }

    private static int show(CommandSourceStack source) {
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
