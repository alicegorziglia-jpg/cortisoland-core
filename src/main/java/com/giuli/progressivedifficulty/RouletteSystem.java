package com.giuli.progressivedifficulty;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Port of the dedsafio plugin's /ruleta command, using the REAL animation
 * frames and icons from the plugin's own resourcepack (dedsafio-textures),
 * copied into this mod's own namespace/font so no separate resourcepack
 * install is required by players.
 *
 * <p>Not ported: the "Reviil"/"Nutria" secondary animations (red/pink's
 * second stage) - the alpha texturepack we were given doesn't include those
 * frame sets (only blue/cyan/green/orange/pink/purple/red/yellow/muerte).
 * That second stage still uses a plain text fallback below.
 */
public class RouletteSystem {
    private static final ResourceLocation RULETA_FONT =
            ResourceLocation.fromNamespaceAndPath(ProgressiveDifficultyMod.MOD_ID, "ruleta");
    private static final ResourceLocation ICONS_FONT =
            ResourceLocation.fromNamespaceAndPath(ProgressiveDifficultyMod.MOD_ID, "icons");
    private static final ResourceLocation RULETA_SOUND =
            ResourceLocation.fromNamespaceAndPath(ProgressiveDifficultyMod.MOD_ID, "ruleta");

    /** color key -> {startCodepoint, frameCount}, matching the copied font/ruleta.json providers. */
    private static final Map<String, int[]> FRAMES = new LinkedHashMap<>();
    static {
        FRAMES.put("blue", new int[] {0xE000, 292});
        FRAMES.put("cyan", new int[] {0xE000 + 292, 287});
        FRAMES.put("green", new int[] {0xE000 + 292 + 287, 290});
        FRAMES.put("orange", new int[] {0xE000 + 292 + 287 + 290, 287});
        FRAMES.put("pink", new int[] {0xE000 + 292 + 287 + 290 + 287, 294});
        FRAMES.put("purple", new int[] {0xE000 + 292 + 287 + 290 + 287 + 294, 294});
        FRAMES.put("red", new int[] {0xE000 + 292 + 287 + 290 + 287 + 294 + 294, 295});
        FRAMES.put("yellow", new int[] {0xE000 + 292 + 287 + 290 + 287 + 294 + 294 + 295, 288});
    }

    /** icon key -> codepoint, matching the copied font/icons.json providers. */
    private static final Map<String, Integer> ICONS = Map.of(
            "blue", 0xF000, "cyan", 0xF002, "green", 0xF003, "orange", 0xF005,
            "pink", 0xF006, "purple", 0xF007, "red", 0xF008, "yellow", 0xF009);

    private record ColorInfo(String spanishName, ChatFormatting formatting, String categoryTitle, String colorCode) {
    }

    private static final Map<String, ColorInfo> COLORS = new LinkedHashMap<>();
    static {
        COLORS.put("green", new ColorInfo("verde", ChatFormatting.DARK_GREEN, "NOTIFICACIONES", "&a"));
        COLORS.put("blue", new ColorInfo("azul", ChatFormatting.DARK_BLUE, "NOTIFICACIONES", "&9"));
        COLORS.put("red", new ColorInfo("roja", ChatFormatting.DARK_RED, "MOMENTO REVIL", "&c"));
        COLORS.put("purple", new ColorInfo("morada", ChatFormatting.DARK_PURPLE, "DESCALIFICACION", "&d"));
        COLORS.put("orange", new ColorInfo("naranja", ChatFormatting.GOLD, "CAMBIO DE DIFICULTAD", "&e"));
        COLORS.put("pink", new ColorInfo("rosada", ChatFormatting.LIGHT_PURPLE, "MOMENTO NUTRIA", "&d"));
        COLORS.put("cyan", new ColorInfo("turquesa", ChatFormatting.DARK_AQUA, "MISION DIARIA", "&b"));
        COLORS.put("yellow", new ColorInfo("amarilla", ChatFormatting.YELLOW, "DEDCICLOPEDIA", "&e"));
    }

    private RouletteSystem() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ruleta")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("tipo", StringArgumentType.word())
                        .then(Commands.argument("color", StringArgumentType.word())
                                .then(Commands.argument("jugadores", EntityArgument.players())
                                        .then(Commands.argument("mensaje", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    String type = StringArgumentType.getString(context, "tipo");
                                                    String colorKey = StringArgumentType.getString(context, "color");
                                                    Collection<ServerPlayer> players =
                                                            EntityArgument.getPlayers(context, "jugadores");
                                                    String message = StringArgumentType.getString(context, "mensaje");
                                                    return run(context.getSource(), type, colorKey, players, message);
                                                }))))));
    }

    private static int run(CommandSourceStack source, String type, String colorKey,
            Collection<ServerPlayer> players, String message) {
        if (!isValidType(type)) {
            source.sendFailure(Component.literal(
                    "Tipo invalido. Usa: title, subtitle, actionbar o sidebar (sidebar cae a actionbar)."));
            return 0;
        }

        ColorInfo color = COLORS.get(colorKey);
        if (color == null || !FRAMES.containsKey(colorKey)) {
            source.sendFailure(Component.literal(
                    "Color invalido. Colores validos: " + String.join(", ", COLORS.keySet())));
            return 0;
        }

        if (players.isEmpty()) {
            source.sendFailure(Component.literal("Debes especificar al menos un jugador."));
            return 0;
        }

        for (ServerPlayer player : players) {
            spin(player, type, colorKey);

            if (colorKey.equals("red") || colorKey.equals("pink")) {
                int secondDelayTicks = (colorKey.equals("red") ? 15 : 17) * 20;
                DelayedTaskScheduler.schedule(15 * 20, () -> {
                    showSpecialReveal(player, type, colorKey);
                    DelayedTaskScheduler.schedule(secondDelayTicks, () -> revealMessage(player, colorKey, color, message));
                });
            } else {
                DelayedTaskScheduler.schedule(15 * 20, () -> revealMessage(player, colorKey, color, message));
            }
        }

        source.sendSuccess(() -> Component.literal(
                "Ruleta lanzada para " + players.size() + " jugador(es), color " + colorKey + "."), true);
        return players.size();
    }

    private static boolean isValidType(String type) {
        return type.equals("title") || type.equals("subtitle") || type.equals("actionbar") || type.equals("sidebar");
    }

    /** Plays the real captured animation frames for this color, ~1 frame per tick like the original. */
    private static void spin(ServerPlayer player, String type, String colorKey) {
        int[] meta = FRAMES.get(colorKey);
        int start = meta[0];
        int count = meta[1];

        player.level().playSound(null, player.blockPosition(),
                SoundEvent.createVariableRangeEvent(RULETA_SOUND), SoundSource.MASTER, 4.0F, 1.0F);

        for (int i = 0; i < count; i++) {
            int codepoint = start + i;
            DelayedTaskScheduler.schedule(Math.max(i, 1), () ->
                    sendPositioned(player, type, frameComponent(codepoint)));
        }

        DelayedTaskScheduler.schedule(count + 2, () -> sendPositioned(player, type, Component.literal(" ")));
    }

    private static Component frameComponent(int codepoint) {
        return Component.literal(String.valueOf((char) codepoint)).withStyle(style -> style.withFont(RULETA_FONT));
    }

    /**
     * Fallback for the plugin's red/pink second-stage "Reviil"/"Nutria" reveal:
     * the alpha texturepack we ported from doesn't include those frame sets,
     * so this stays as a plain colored title until those assets show up.
     */
    private static void showSpecialReveal(ServerPlayer player, String type, String colorKey) {
        String title = colorKey.equals("red") ? "\u00a1MOMENTO REVIL!" : "\u00a1MOMENTO NUTRIA!";
        ChatFormatting formatting = colorKey.equals("red") ? ChatFormatting.DARK_RED : ChatFormatting.LIGHT_PURPLE;
        sendPositioned(player, type, Component.literal(title).withStyle(formatting, ChatFormatting.BOLD));
        player.level().playSound(null, player.blockPosition(),
                SoundEvent.createVariableRangeEvent(RULETA_SOUND), SoundSource.MASTER, 4.0F, 1.0F);
    }

    private static void revealMessage(ServerPlayer player, String colorKey, ColorInfo color, String message) {
        Integer iconCodepoint = ICONS.get(colorKey);
        MutableComponent line1 = Component.empty();
        if (iconCodepoint != null) {
            line1.append(Component.literal(String.valueOf((char) (int) iconCodepoint))
                    .withStyle(style -> style.withFont(ICONS_FONT)));
            line1.append(Component.literal(" "));
        }
        line1.append(legacyToComponent("&l\u25aa " + color.categoryTitle() + ":")
                .withStyle(color.formatting()));

        player.sendSystemMessage(line1);
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(legacyToComponent(color.colorCode() + message));
    }

    private static void sendPositioned(ServerPlayer player, String type, Component text) {
        switch (type) {
            case "title" -> {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 20, 5));
                player.connection.send(new ClientboundSetTitleTextPacket(text));
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
            }
            case "subtitle" -> {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 20, 5));
                player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
                player.connection.send(new ClientboundSetSubtitleTextPacket(text));
            }
            default -> player.displayClientMessage(text, true); // actionbar (also used for "sidebar" fallback)
        }
    }

    private static MutableComponent legacyToComponent(String legacyText) {
        return Component.literal(legacyText.replace('&', '\u00a7'));
    }
}
