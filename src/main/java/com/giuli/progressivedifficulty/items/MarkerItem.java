package com.giuli.progressivedifficulty.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Port of the dedsafio plugin's MarkerItem: left-click a block to set
 * position 1, right-click a block to set position 2. Positions are stored
 * directly on the item stack (like the guaranteed totem's marker tag), so
 * the tool can be handed to someone else and it keeps its saved points.
 *
 * <p>Click handling itself lives in ProgressiveDifficultyEvents (right-click
 * reuses the shared PlayerInteractEvent.RightClickBlock handler, left-click
 * hooks PlayerInteractEvent.LeftClickBlock) - this class only holds the
 * read/write helpers so both call sites share one source of truth.
 */
public class MarkerItem extends Item {
    private static final String POS1_KEY = "progressivedifficulty_marker_pos1";
    private static final String POS2_KEY = "progressivedifficulty_marker_pos2";

    public MarkerItem(Properties properties) {
        super(properties);
    }

    public static void setPos1(ItemStack stack, BlockPos pos) {
        setPos(stack, POS1_KEY, pos);
    }

    public static void setPos2(ItemStack stack, BlockPos pos) {
        setPos(stack, POS2_KEY, pos);
    }

    public static BlockPos getPos1(ItemStack stack) {
        return getPos(stack, POS1_KEY);
    }

    public static BlockPos getPos2(ItemStack stack) {
        return getPos(stack, POS2_KEY);
    }

    private static void setPos(ItemStack stack, String key, BlockPos pos) {
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putIntArray(key, new int[] {pos.getX(), pos.getY(), pos.getZ()});
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static BlockPos getPos(ItemStack stack, String key) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(key)) {
            return null;
        }
        int[] coords = tag.getIntArray(key);
        if (coords.length != 3) {
            return null;
        }
        return new BlockPos(coords[0], coords[1], coords[2]);
    }

    public static String describePos(BlockPos pos) {
        return pos == null ? "(sin definir)" : pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
