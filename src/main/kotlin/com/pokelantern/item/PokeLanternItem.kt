package com.pokelantern.item

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block

class PokeLanternItem(block: Block, props: Properties) : BlockItem(block, props) {

    override fun appendHoverText(stack: ItemStack, level: Level?, lines: MutableList<Component>, flags: TooltipFlag) {
        lines.add(Component.literal("Creates a Pokémon spawn zone").withStyle(ChatFormatting.AQUA))
        lines.add(Component.literal("Place it to allow Pokémon to spawn within its radius.").withStyle(ChatFormatting.GRAY))
        lines.add(Component.literal("One placed lantern per player in survival.").withStyle(ChatFormatting.DARK_GRAY))
    }
}
