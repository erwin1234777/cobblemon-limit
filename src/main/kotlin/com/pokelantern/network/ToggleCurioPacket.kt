package com.pokelantern.network

import com.pokelantern.ModRegistry
import net.minecraft.ChatFormatting
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraftforge.network.NetworkEvent
import top.theillusivec4.curios.api.CuriosApi
import java.util.function.Supplier

class ToggleCurioPacket {

    companion object {
        fun encode(packet: ToggleCurioPacket, buf: FriendlyByteBuf) {}
        fun decode(buf: FriendlyByteBuf) = ToggleCurioPacket()

        fun handle(packet: ToggleCurioPacket, ctx: Supplier<NetworkEvent.Context>) {
            ctx.get().enqueueWork {
                val player = ctx.get().sender ?: return@enqueueWork
                val item = ModRegistry.CURIO_LANTERN_ITEM.get()

                // Collect every curio lantern stack from all inventory locations
                val stacks = (player.inventory.items + player.inventory.offhand)
                    .filter { it.item == item }
                    .toMutableList()

                CuriosApi.getCuriosInventory(player).ifPresent { handler ->
                    handler.curios.values.forEach { slotHandler ->
                        for (i in 0 until slotHandler.stacks.slots) {
                            val stack = slotHandler.stacks.getStackInSlot(i)
                            if (stack.item == item) stacks.add(stack)
                        }
                    }
                }

                if (stacks.isEmpty()) return@enqueueWork

                // Master toggle: any active → deactivate all; none active → activate all
                val newState = !stacks.any { it.tag?.getBoolean("active") == true }
                stacks.forEach { it.orCreateTag.putBoolean("active", newState) }

                if (newState) {
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
            ctx.get().packetHandled = true
        }
    }
}
