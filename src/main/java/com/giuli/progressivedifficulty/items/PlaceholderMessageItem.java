package com.giuli.progressivedifficulty.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Matches the dedsafio plugin's Fork/Spoon items: right-clicking just tells
 * the player the item isn't implemented yet.
 */
public class PlaceholderMessageItem extends Item {
    private final String message;

    public PlaceholderMessageItem(Properties properties, String message) {
        super(properties);
        this.message = message;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal(message), false);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
