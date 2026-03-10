package com.pokelantern

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import top.theillusivec4.curios.api.CuriosApi

object SpawnHandler {

    private fun isActiveCurio(stack: ItemStack) =
        stack.item == ModRegistry.CURIO_LANTERN_ITEM.get() &&
        stack.tag?.getBoolean("active") == true

    fun register() {
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.NORMAL) { event ->
            val pokemon = event.entity.pokemon

            // Shinies bypass the check if configured
            if (Config.ALLOW_SHINIES.get() && pokemon.shiny) return@subscribe

            // Legendaries bypass the check if configured
            if (Config.ALLOW_LEGENDARIES.get() && pokemon.form.labels.contains("legendary")) return@subscribe

            // Species-level bypass list
            val bypassList = Config.BYPASS_SPECIES.get()
            if (bypassList.any { (it as? String)?.equals(pokemon.species.name, ignoreCase = true) == true }) return@subscribe

            val spawnPos = event.entity.blockPosition()
            val level = event.entity.level() as? ServerLevel ?: return@subscribe
            val data = LanternSavedData.get(level)
            val radius = Config.SPAWN_RADIUS.get()
            val radiusSq = (radius * radius).toDouble()

            val withinAny = data.getAllInDim(level.dimension()).any { entry ->
                entry.pos.distSqr(spawnPos) <= radiusSq
            }

            val withinPlayer = !withinAny && level.server.playerList.players.any { player ->
                if (player.level().dimension() != level.dimension()) return@any false
                val ppos = player.blockPosition()

                // Feature 1: holding regular Poke Lantern in either hand
                val holdsLantern = player.mainHandItem.`is`(ModRegistry.POKE_LANTERN_ITEM.get()) ||
                                   player.offhandItem.`is`(ModRegistry.POKE_LANTERN_ITEM.get())
                if (holdsLantern && ppos.distSqr(spawnPos) <= radiusSq) return@any true

                // Feature 2: active Curio Poke Lantern anywhere in inventory
                val activeInInv = player.inventory.items.any { isActiveCurio(it) } ||
                                  isActiveCurio(player.offhandItem)
                val activeInCurio = CuriosApi.getCuriosInventory(player).map { handler ->
                    handler.curios.values.any { sh ->
                        (0 until sh.stacks.slots).any { i -> isActiveCurio(sh.stacks.getStackInSlot(i)) }
                    }
                }.orElse(false)
                (activeInInv || activeInCurio) && ppos.distSqr(spawnPos) <= radiusSq
            }

            if (!withinAny && !withinPlayer) {
                event.cancel()
            }
        }
    }
}
