package com.pokelantern

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID

/**
 * Global lantern registry stored on the overworld's DimensionDataStorage.
 * Using a single storage location guarantees every part of the code sees
 * the same instance regardless of which ServerLevel it currently holds.
 *
 * Each player may own multiple lanterns (creative users are unlimited;
 * survival users are capped at 1 by PokeLantern.kt's placement guard).
 * Dimension info is stored per-entry so the spawn handler can filter
 * by the current dimension without loading cross-dim chunks.
 */
class LanternSavedData : SavedData() {

    data class Entry(val dim: ResourceKey<Level>, val pos: BlockPos)

    // UUID → ordered list of entries (insertion order preserved)
    private val playerToEntries: MutableMap<UUID, MutableList<Entry>> = mutableMapOf()
    // (dim+pos) → UUID for O(1) reverse lookups in onBlockBreak / onRemove
    private val keyToPlayer: MutableMap<Pair<ResourceKey<Level>, BlockPos>, UUID> = mutableMapOf()

    // -------------------------------------------------------------------------
    // Read accessors
    // -------------------------------------------------------------------------

    fun getLanternsFor(uuid: UUID): List<Entry> =
        playerToEntries[uuid]?.toList() ?: emptyList()

    fun hasLantern(uuid: UUID): Boolean =
        playerToEntries[uuid]?.isNotEmpty() == true

    /** All lanterns in a specific dimension (used by the spawn handler). */
    fun getAllInDim(dim: ResourceKey<Level>): List<Entry> =
        playerToEntries.values.flatten().filter { it.dim == dim }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    fun addLantern(uuid: UUID, dim: ResourceKey<Level>, pos: BlockPos) {
        playerToEntries.getOrPut(uuid) { mutableListOf() }.add(Entry(dim, pos))
        keyToPlayer[Pair(dim, pos)] = uuid
        setDirty()
        PokeLantern.LOGGER.debug("[pokelantern] Added lantern for $uuid at $pos in $dim")
    }

    fun removeLanternAt(dim: ResourceKey<Level>, pos: BlockPos) {
        val key = Pair(dim, pos)
        val uuid = keyToPlayer.remove(key) ?: run {
            PokeLantern.LOGGER.debug("[pokelantern] removeLanternAt($pos) — no entry found")
            return
        }
        playerToEntries[uuid]?.removeIf { it.dim == dim && it.pos == pos }
        if (playerToEntries[uuid]?.isEmpty() == true) playerToEntries.remove(uuid)
        setDirty()
        PokeLantern.LOGGER.debug("[pokelantern] Removed lantern for $uuid at $pos in $dim")
    }

    fun removeAllLanternsFor(uuid: UUID) {
        val entries = playerToEntries.remove(uuid) ?: return
        for (e in entries) keyToPlayer.remove(Pair(e.dim, e.pos))
        setDirty()
        PokeLantern.LOGGER.debug("[pokelantern] Removed all ${entries.size} lantern(s) for $uuid")
    }

    // -------------------------------------------------------------------------
    // Serialisation
    // -------------------------------------------------------------------------

    override fun save(tag: CompoundTag): CompoundTag {
        val list = ListTag()
        for ((uuid, entries) in playerToEntries) {
            for (e in entries) {
                val entry = CompoundTag()
                entry.putUUID("uuid", uuid)
                entry.putString("dim", e.dim.location().toString())
                entry.putInt("x", e.pos.x)
                entry.putInt("y", e.pos.y)
                entry.putInt("z", e.pos.z)
                list.add(entry)
            }
        }
        tag.put("lanterns", list)
        return tag
    }

    // -------------------------------------------------------------------------
    // Singleton access — ALWAYS via the overworld's DimensionDataStorage
    // -------------------------------------------------------------------------

    companion object {
        const val DATA_NAME = "pokelantern_data"

        private fun load(tag: CompoundTag): LanternSavedData {
            val data = LanternSavedData()
            val list = tag.getList("lanterns", Tag.TAG_COMPOUND.toInt())
            for (i in 0 until list.size) {
                val entry = list.getCompound(i)
                val uuid = entry.getUUID("uuid")
                val dimKey = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation(entry.getString("dim"))
                )
                val pos = BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"))
                data.playerToEntries.getOrPut(uuid) { mutableListOf() }.add(LanternSavedData.Entry(dimKey, pos))
                data.keyToPlayer[Pair(dimKey, pos)] = uuid
            }
            PokeLantern.LOGGER.info("[pokelantern] Loaded ${data.keyToPlayer.size} lantern(s) from disk")
            return data
        }

        /** Single global instance — stored on the overworld so every call site
         *  gets the same object regardless of which level the caller holds. */
        fun get(server: MinecraftServer): LanternSavedData =
            server.overworld().dataStorage.computeIfAbsent(::load, ::LanternSavedData, DATA_NAME)

        fun get(level: ServerLevel): LanternSavedData = get(level.server)
    }
}
