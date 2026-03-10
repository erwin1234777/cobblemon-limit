package com.pokelantern

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for the bidirectional (UUID <-> position) registry logic
 * that backs LanternSavedData.  Uses a pure-Kotlin stand-in for BlockPos
 * so no Minecraft runtime is needed.
 */
data class Pos(val x: Int, val y: Int, val z: Int)

class LanternRegistry {
    private val playerToPos: MutableMap<String, Pos> = mutableMapOf()
    private val posToPlayer: MutableMap<Pos, String> = mutableMapOf()

    fun getLanternFor(uuid: String): Pos? = playerToPos[uuid]
    fun getOwnerOf(pos: Pos): String? = posToPlayer[pos]

    fun setLantern(uuid: String, pos: Pos) {
        playerToPos[uuid]?.let { old -> posToPlayer.remove(old) }
        playerToPos[uuid] = pos
        posToPlayer[pos] = uuid
    }

    fun removeLanternAt(pos: Pos) {
        val uuid = posToPlayer.remove(pos) ?: return
        playerToPos.remove(uuid)
    }

    fun getAllPositions(): Set<Pos> = posToPlayer.keys.toSet()
    fun size(): Int = playerToPos.size
}

class LanternRegistryTest {

    private lateinit var reg: LanternRegistry
    private val alice = "alice-uuid"
    private val bob = "bob-uuid"

    @BeforeEach
    fun setUp() {
        reg = LanternRegistry()
    }

    @Test
    fun `new registry is empty`() {
        assertEquals(0, reg.size())
    }

    @Test
    fun `setLantern stores entry for player`() {
        val pos = Pos(10, 64, 20)
        reg.setLantern(alice, pos)
        assertEquals(pos, reg.getLanternFor(alice))
    }

    @Test
    fun `getLanternFor unknown player returns null`() {
        assertNull(reg.getLanternFor(alice))
    }

    @Test
    fun `removeLanternAt clears both directions`() {
        val pos = Pos(10, 64, 20)
        reg.setLantern(alice, pos)
        reg.removeLanternAt(pos)
        assertNull(reg.getLanternFor(alice))
        assertNull(reg.getOwnerOf(pos))
    }

    @Test
    fun `removeLanternAt on unknown pos does nothing`() {
        reg.removeLanternAt(Pos(0, 0, 0))  // should not throw
        assertEquals(0, reg.size())
    }

    @Test
    fun `two players can each have their own lantern`() {
        val posA = Pos(0, 64, 0)
        val posB = Pos(200, 64, 0)
        reg.setLantern(alice, posA)
        reg.setLantern(bob, posB)
        assertEquals(posA, reg.getLanternFor(alice))
        assertEquals(posB, reg.getLanternFor(bob))
        assertEquals(2, reg.size())
    }

    @Test
    fun `player moving lantern cleans up old entry`() {
        val posA = Pos(0, 64, 0)
        val posB = Pos(50, 64, 50)
        reg.setLantern(alice, posA)
        reg.setLantern(alice, posB)  // replaces old
        assertEquals(posB, reg.getLanternFor(alice))
        assertNull(reg.getOwnerOf(posA))   // old pos should be gone
        assertEquals(1, reg.size())
    }

    @Test
    fun `getAllPositions returns all lantern positions`() {
        val posA = Pos(0, 64, 0)
        val posB = Pos(200, 64, 0)
        reg.setLantern(alice, posA)
        reg.setLantern(bob, posB)
        assertEquals(setOf(posA, posB), reg.getAllPositions())
    }

    @Test
    fun `after remove player can place new lantern`() {
        val posA = Pos(0, 64, 0)
        reg.setLantern(alice, posA)
        reg.removeLanternAt(posA)
        val posB = Pos(100, 64, 100)
        reg.setLantern(alice, posB)  // should succeed
        assertEquals(posB, reg.getLanternFor(alice))
    }
}
