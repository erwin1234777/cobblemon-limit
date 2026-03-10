package com.pokelantern.item

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import top.theillusivec4.curios.api.type.capability.ICurioItem

class CurioLanternItem(props: Properties) : Item(props), ICurioItem {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (player.isShiftKeyDown) {
            if (!level.isClientSide) {
                val active = !(stack.orCreateTag.getBoolean("active"))
                stack.orCreateTag.putBoolean("active", active)
                if (active) {
                    player.sendSystemMessage(
                        Component.literal("◆ Curio Poke Lantern ").withStyle(ChatFormatting.AQUA)
                            .append(Component.literal("activated").withStyle(ChatFormatting.GREEN))
                            .append(Component.literal(" — Pokémon will spawn nearby.").withStyle(ChatFormatting.GRAY))
                    )
                } else {
                    player.sendSystemMessage(
                        Component.literal("◆ Curio Poke Lantern ").withStyle(ChatFormatting.AQUA)
                            .append(Component.literal("deactivated").withStyle(ChatFormatting.DARK_GRAY))
                            .append(Component.literal(".").withStyle(ChatFormatting.GRAY))
                    )
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
        }
        return InteractionResultHolder.pass(stack)
    }

    override fun appendHoverText(stack: ItemStack, level: Level?, lines: MutableList<Component>, flags: TooltipFlag) {
        val active = stack.tag?.getBoolean("active") == true
        lines.add(Component.literal("Portable Pokémon spawn zone").withStyle(ChatFormatting.AQUA))
        lines.add(Component.literal("Works from anywhere in your inventory while active.").withStyle(ChatFormatting.GRAY))
        lines.add(Component.literal("Equip in the Curios slot for hands-free use.").withStyle(ChatFormatting.DARK_GRAY))
        lines.add(Component.empty())
        if (active) {
            lines.add(
                Component.literal("● Active  ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal("Shift+RClick to deactivate").withStyle(ChatFormatting.GRAY))
            )
        } else {
            lines.add(
                Component.literal("● Inactive  ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal("Shift+RClick to activate").withStyle(ChatFormatting.GRAY))
            )
        }
    }
}
