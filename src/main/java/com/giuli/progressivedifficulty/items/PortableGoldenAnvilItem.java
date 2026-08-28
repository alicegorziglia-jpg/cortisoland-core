package com.giuli.progressivedifficulty.items;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Port of the plugin's "Portable Golden Anvil": fully repairs every piece of
 * worn armor and consumes one item from the stack used.
 */
public class PortableGoldenAnvilItem extends Item {
    public PortableGoldenAnvilItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        boolean repairedSomething = false;
        for (ItemStack armor : player.getInventory().armor) {
            if (!armor.isEmpty() && armor.isDamaged()) {
                armor.setDamageValue(0);
                repairedSomething = true;
            }
        }

        if (!repairedSomething) {
            player.displayClientMessage(Component.literal("Tu armadura no necesita repararse."), true);
            return InteractionResultHolder.fail(stack);
        }

        stack.shrink(1);
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResultHolder.success(stack);
    }
}
