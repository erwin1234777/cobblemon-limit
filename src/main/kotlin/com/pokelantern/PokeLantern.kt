package com.pokelantern

import com.mojang.brigadier.context.CommandContext
import com.pokelantern.block.PokeLanternBlock
import com.pokelantern.client.ClientSetup
import com.pokelantern.network.PacketHandler
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.KotlinModLoadingContext

@Mod(PokeLantern.MOD_ID)
class PokeLantern {

    companion object {
        const val MOD_ID = "pokelantern"
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)
    }

    init {
        val modBus = KotlinModLoadingContext.get().getKEventBus()

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC, "pokelantern-server.toml")

        ModRegistry.BLOCKS.register(modBus)
        ModRegistry.ITEMS.register(modBus)

        modBus.addListener(::onBuildCreativeTab)

        PacketHandler.register()

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT) { Runnable {
            modBus.addListener(ClientSetup::onRegisterKeyMappings)
            MinecraftForge.EVENT_BUS.addListener(ClientSetup::onClientTick)
        }}

        MinecraftForge.EVENT_BUS.register(this)

        LOGGER.info("[pokelantern] Poke Lantern mod initializing.")
    }

    private fun onBuildCreativeTab(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ItemStack(ModRegistry.POKE_LANTERN_ITEM.get()))
            event.accept(ItemStack(ModRegistry.CURIO_LANTERN_ITEM.get()))
        }
    }

    // -------------------------------------------------------------------------
    // Block break — primary cleanup hook, fires before the block is actually
    // removed so we can reliably clear the saved data entry.
    // -------------------------------------------------------------------------
    @SubscribeEvent
    fun onBlockBreak(event: BlockEvent.BreakEvent) {
        if (event.state.block !is PokeLanternBlock) return
        val serverLevel = event.level as? ServerLevel ?: return
        LOGGER.debug("[pokelantern] onBlockBreak at ${event.pos}")
        LanternSavedData.get(serverLevel).removeLanternAt(serverLevel.dimension(), event.pos)
    }

    // -------------------------------------------------------------------------
    // Block placement guard + registration.
    // NOTE: setPlacedBy fires BEFORE this event in Forge 1.20.1, so we do
    // registration HERE (not in setPlacedBy) to avoid a false-positive where
    // the guard would find the entry just added for the current placement.
    // -------------------------------------------------------------------------
    @SubscribeEvent
    fun onBlockPlace(event: BlockEvent.EntityPlaceEvent) {
        if (event.placedBlock.block !is PokeLanternBlock) return
        val player = event.entity as? net.minecraft.world.entity.player.Player ?: return
        val serverLevel = event.level as? ServerLevel ?: return
        val data = LanternSavedData.get(serverLevel)

        if (!player.isCreative) {
            // Snapshot to avoid ConcurrentModification while we remove stale entries
            val entries = data.getLanternsFor(player.uuid).toList()
            for (entry in entries) {
                val entryLevel = serverLevel.server.getLevel(entry.dim) ?: continue
                val chunkLoaded = entryLevel.isLoaded(entry.pos)
                if (chunkLoaded && entryLevel.getBlockState(entry.pos).block !is PokeLanternBlock) {
                    LOGGER.warn("[pokelantern] Stale lantern entry for ${player.name.string} at ${entry.pos} — auto-clearing")
                    data.removeLanternAt(entry.dim, entry.pos)
                    continue
                }
                // Chunk unloaded (assume valid) or block is present — block the placement.
                event.isCanceled = true
                player.sendSystemMessage(
                    Component.literal(
                        "You already have a Poke Lantern at X:${entry.pos.x} Y:${entry.pos.y} Z:${entry.pos.z}" +
                        " (${entry.dim.location()}). Remove it first!"
                    )
                )
                return
            }
        }

        // Placement approved — register the lantern now.
        data.addLantern(player.uuid, serverLevel.dimension(), event.pos)
    }

    // -------------------------------------------------------------------------
    // Commands — requires op level 2
    // /pokelantern removeLantern <player>
    // /pokelantern listLanterns <player>
    // -------------------------------------------------------------------------
    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        event.dispatcher.register(
            Commands.literal("pokelantern")
                .requires { src -> src.hasPermission(2) }
                .then(
                    Commands.literal("removeLantern")
                        .then(
                            Commands.argument("target", EntityArgument.player())
                                .executes { ctx -> executeRemoveLantern(ctx) }
                        )
                )
                .then(
                    Commands.literal("listLanterns")
                        .then(
                            Commands.argument("target", EntityArgument.player())
                                .executes { ctx -> executeListLanterns(ctx) }
                        )
                )
        )
    }

    private fun executeRemoveLantern(ctx: CommandContext<CommandSourceStack>): Int {
        val target: ServerPlayer = EntityArgument.getPlayer(ctx, "target")
        val server = ctx.source.server
        val data = LanternSavedData.get(server)
        val entries = data.getLanternsFor(target.uuid).toList()

        if (entries.isEmpty()) {
            ctx.source.sendFailure(
                Component.literal("${target.name.string} has no active Poke Lantern(s)")
            )
            return 0
        }

        // Destroy the actual blocks for loaded chunks; they will trigger onBlockBreak → data cleanup
        for (entry in entries) {
            val entryLevel = server.getLevel(entry.dim) ?: continue
            if (entryLevel.isLoaded(entry.pos) &&
                entryLevel.getBlockState(entry.pos).block is PokeLanternBlock) {
                entryLevel.destroyBlock(entry.pos, false)
            }
        }
        // Wipe any remaining data (unloaded chunks, stale entries)
        data.removeAllLanternsFor(target.uuid)

        ctx.source.sendSuccess(
            { Component.literal(
                "Removed ${entries.size} Poke Lantern(s) for ${target.name.string}"
            ) },
            true
        )
        return 1
    }

    private fun executeListLanterns(ctx: CommandContext<CommandSourceStack>): Int {
        val target: ServerPlayer = EntityArgument.getPlayer(ctx, "target")
        val data = LanternSavedData.get(ctx.source.server)
        val entries = data.getLanternsFor(target.uuid)

        if (entries.isEmpty()) {
            ctx.source.sendFailure(
                Component.literal("${target.name.string} has no active Poke Lantern(s)")
            )
            return 0
        }

        ctx.source.sendSuccess(
            { Component.literal("${target.name.string} has ${entries.size} Poke Lantern(s):") },
            false
        )
        for (entry in entries) {
            ctx.source.sendSuccess(
                { Component.literal(
                    "  - X:${entry.pos.x} Y:${entry.pos.y} Z:${entry.pos.z} (${entry.dim.location()})"
                ) },
                false
            )
        }
        return entries.size
    }

    // -------------------------------------------------------------------------
    // Server lifecycle
    // -------------------------------------------------------------------------
    @SubscribeEvent
    fun onServerStarting(event: ServerStartingEvent) {
        SpawnHandler.register()
        LOGGER.info("[pokelantern] Spawn handler registered. Pokemon spawns are now restricted to Poke Lantern zones.")
    }
}
