package com.pokelantern.network

import com.pokelantern.PokeLantern
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.simple.SimpleChannel

object PacketHandler {

    private const val PROTOCOL = "1"

    val CHANNEL: SimpleChannel = NetworkRegistry.newSimpleChannel(
        ResourceLocation(PokeLantern.MOD_ID, "main"),
        { PROTOCOL },
        PROTOCOL::equals,
        PROTOCOL::equals
    )

    fun register() {
        CHANNEL.registerMessage(
            0,
            ToggleCurioPacket::class.java,
            ToggleCurioPacket::encode,
            ToggleCurioPacket::decode,
            ToggleCurioPacket::handle
        )
    }
}
