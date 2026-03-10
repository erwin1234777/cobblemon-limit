package com.pokelantern

import net.minecraftforge.common.ForgeConfigSpec

object Config {
    private val BUILDER = ForgeConfigSpec.Builder()

    val ALLOW_SHINIES: ForgeConfigSpec.BooleanValue
    val ALLOW_LEGENDARIES: ForgeConfigSpec.BooleanValue
    val SPAWN_RADIUS: ForgeConfigSpec.IntValue
    val BYPASS_SPECIES: ForgeConfigSpec.ConfigValue<List<*>>

    val SERVER_SPEC: ForgeConfigSpec

    init {
        BUILDER.push("spawning")

        ALLOW_SHINIES = BUILDER
            .comment("If true, shiny Pokemon can spawn anywhere regardless of lantern placement.")
            .define("allowShinies", true)

        ALLOW_LEGENDARIES = BUILDER
            .comment("If true, legendary Pokemon can spawn anywhere regardless of lantern placement.")
            .define("allowLegendaries", true)

        SPAWN_RADIUS = BUILDER
            .comment("Radius in blocks around a Poke Lantern where Pokemon can spawn.")
            .defineInRange("spawnRadius", 64, 1, 512)

        BYPASS_SPECIES = BUILDER
            .comment(
                "List of Pokemon species names that always bypass the spawn radius check.",
                "Use the internal species name (e.g. pikachu, rayquaza, eevee).",
                "Useful for Pokemon added by datapacks or other mods that should always be allowed to spawn."
            )
            .defineList("bypassSpeciesList", emptyList<String>()) { it is String }

        BUILDER.pop()
        SERVER_SPEC = BUILDER.build()
    }
}
