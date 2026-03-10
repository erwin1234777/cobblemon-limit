package com.pokelantern.client

import com.pokelantern.network.PacketHandler
import com.pokelantern.network.ToggleCurioPacket
import net.minecraft.client.KeyMapping
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.event.TickEvent
import org.lwjgl.glfw.GLFW

object ClientSetup {

    val TOGGLE_KEY = KeyMapping(
        "key.pokelantern.toggle_curio",
        GLFW.GLFW_KEY_K,
        "key.categories.pokelantern"
    )

    fun onRegisterKeyMappings(event: RegisterKeyMappingsEvent) {
        event.register(TOGGLE_KEY)
    }

    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        while (TOGGLE_KEY.consumeClick()) {
            PacketHandler.CHANNEL.sendToServer(ToggleCurioPacket())
        }
    }
}
