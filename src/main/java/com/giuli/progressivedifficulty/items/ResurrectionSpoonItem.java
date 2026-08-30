package com.giuli.progressivedifficulty.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Port of the dedsafio plugin's ResurrectionSpoonItem. The actual
 * zone-check/soul-check/menu logic lives in ResurrectionSpoonSystem
 * (needs access to FogataSystem and SoulSystem, which items shouldn't
 * depend on directly) - this class just exists so the item is real and
 * `stack.is(ModItems.RESURRECTION_SPOON)` works from that system.
 */
public class ResurrectionSpoonItem extends Item {
    public ResurrectionSpoonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Handled by ResurrectionSpoonSystem's PlayerInteractEvent.RightClickItem /
        // RightClickBlock listener, so the zone/soul checks and the revive list
        // stay in one place alongside FogataSystem and SoulSystem.
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
