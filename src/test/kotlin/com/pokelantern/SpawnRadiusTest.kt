package com.pokelantern

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for the spawn-radius decision logic.
 * Uses plain Int triples to mirror BlockPos without requiring Minecraft on the classpath.
 */
class SpawnRadiusTest {

    private fun distSqr(lx: Int, ly: Int, lz: Int, sx: Int, sy: Int, sz: Int): Double {
        val dx = (lx - sx).toDouble()
        val dy = (ly - sy).toDouble()
        val dz = (lz - sz).toDouble()
        return dx * dx + dy * dy + dz * dz
    }

    private fun isWithinRadius(
        lanternX: Int, lanternY: Int, lanternZ: Int,
        spawnX: Int, spawnY: Int, spawnZ: Int,
        radius: Int
    ): Boolean = distSqr(lanternX, lanternY, lanternZ, spawnX, spawnY, spawnZ) <= (radius * radius).toDouble()

    @Test
    fun `spawn at lantern position is allowed`() {
        assertTrue(isWithinRadius(0, 64, 0, 0, 64, 0, 64))
    }

    @Test
    fun `spawn exactly at radius boundary is allowed`() {
        // 64 blocks away on X axis — should be allowed (<=)
        assertTrue(isWithinRadius(0, 64, 0, 64, 64, 0, 64))
    }

    @Test
    fun `spawn one block beyond radius is blocked`() {
        // 65 blocks away on X axis — should be blocked
        assertFalse(isWithinRadius(0, 64, 0, 65, 64, 0, 64))
    }

    @Test
    fun `spawn diagonally within radius is allowed`() {
        // sqrt(45^2 + 45^2) ≈ 63.6 < 64
        assertTrue(isWithinRadius(0, 64, 0, 45, 64, 45, 64))
    }

    @Test
    fun `spawn diagonally beyond radius is blocked`() {
        // sqrt(46^2 + 46^2) ≈ 65.1 > 64
        assertFalse(isWithinRadius(0, 64, 0, 46, 64, 46, 64))
    }

    @Test
    fun `radius=1 allows only immediate neighbours`() {
        assertTrue(isWithinRadius(10, 10, 10, 11, 10, 10, 1))
        assertFalse(isWithinRadius(10, 10, 10, 12, 10, 10, 1))
    }

    @Test
    fun `spawn near second lantern is allowed when first is far`() {
        // Two lanterns: (0,64,0) and (200,64,0). Spawn at (200,64,5).
        val lanterns = listOf(Triple(0, 64, 0), Triple(200, 64, 0))
        val spawnX = 200; val spawnY = 64; val spawnZ = 5
        val radius = 64
        val within = lanterns.any { (lx, ly, lz) -> isWithinRadius(lx, ly, lz, spawnX, spawnY, spawnZ, radius) }
        assertTrue(within)
    }

    @Test
    fun `spawn far from all lanterns is blocked`() {
        val lanterns = listOf(Triple(0, 64, 0), Triple(200, 64, 0))
        val spawnX = 100; val spawnY = 64; val spawnZ = 0  // equidistant, 100 blocks from each
        val radius = 64
        val within = lanterns.any { (lx, ly, lz) -> isWithinRadius(lx, ly, lz, spawnX, spawnY, spawnZ, radius) }
        assertFalse(within)
    }
}
