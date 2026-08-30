package com.giuli.progressivedifficulty;

import com.giuli.progressivedifficulty.items.MarkerItem;
import com.giuli.progressivedifficulty.items.ModItems;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Port of the dedsafio plugin's FogataCommand: mark a resurrection-zone
 * boundary with the marker item, store it (reusing {@link ModConfigStore},
 * exactly like the plugin reused its own generic db.set()/db.get() for the
 * same two values), and let {@link ResurrectionSpoonSystem} check it.
 */
public class FogataSystem {
    private static final String POS_ONE_KEY = "fogata-pos-one";
    private static final String POS_TWO_KEY = "fogata-pos-two";

    private FogataSystem() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fogata")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(
                            "/fogata set | /fogata apply | /fogata remove | /fogata help"), false);
                    return 1;
                })
                .then(Commands.literal("help").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(
                            "Usa /fogata set para recibir la herramienta de marcacion, marca dos "
                                    + "esquinas del perimetro de tu fogata (clic izq/der), y despues "
                                    + "usa /fogata apply para guardarlo."), false);
                    return 1;
                }))
                .then(Commands.literal("set").executes(context -> set(context.getSource())))
                .then(Commands.literal("apply").executes(context -> apply(context.getSource())))
                .then(Commands.literal("remove").executes(context -> remove(context.getSource()))));
    }

    private static int set(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.MARKER_ITEM.get()));
        source.sendSuccess(() -> Component.literal(
                "Te dimos la herramienta de marcacion. Marca las dos esquinas y despues usa /fogata apply."), false);
        return 1;
    }

    private static int apply(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        ItemStack marker = findMarker(player);
        if (marker == null) {
            source.sendFailure(Component.literal("Deberias haber usado /fogata set primero."));
            return 0;
        }

        BlockPos pos1 = MarkerItem.getPos1(marker);
        BlockPos pos2 = MarkerItem.getPos2(marker);
        if (pos1 == null || pos2 == null) {
            source.sendFailure(Component.literal("No has definido la primera o segunda ubicacion."));
            return 0;
        }

        ModConfigStore.get().set(POS_ONE_KEY, pos1.getX() + "," + pos1.getY() + "," + pos1.getZ());
        ModConfigStore.get().set(POS_TWO_KEY, pos2.getX() + "," + pos2.getY() + "," + pos2.getZ());
        ModConfigStore.get().save(source.getServer());

        marker.shrink(1);

        source.sendSuccess(() -> Component.literal(
                "Fogata activada correctamente. Para desactivarla, usa /fogata remove."), false);
        return 1;
    }

    private static int remove(CommandSourceStack source) {
        ModConfigStore.get().set(POS_ONE_KEY, "");
        ModConfigStore.get().set(POS_TWO_KEY, "");
        ModConfigStore.get().save(source.getServer());
        source.sendSuccess(() -> Component.literal("Fogata desactivada."), false);
        return 1;
    }

    private static ItemStack findMarker(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.MARKER_ITEM)) {
                return stack;
            }
        }
        return null;
    }

    /** Returns false if no fogata zone is defined, or the two corners aren't both set. */
    public static boolean isWithinFogataBounds(BlockPos pos) {
        BlockPos pos1 = parse(ModConfigStore.get().get(POS_ONE_KEY));
        BlockPos pos2 = parse(ModConfigStore.get().get(POS_TWO_KEY));
        if (pos1 == null || pos2 == null) {
            return false;
        }

        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    private static BlockPos parse(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split(",");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
