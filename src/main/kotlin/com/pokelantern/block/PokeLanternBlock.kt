package com.pokelantern.block

import com.pokelantern.LanternSavedData
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.LanternBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState

class PokeLanternBlock(props: BlockBehaviour.Properties) : LanternBlock(props) {

    // Registration (addLantern) is done in BlockEvent.EntityPlaceEvent in PokeLantern.kt
    // because setPlacedBy fires BEFORE EntityPlaceEvent in Forge 1.20.1, which caused
    // the guard to find the newly-added entry and block the placement.

    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        isMoving: Boolean
    ) {
        if (!level.isClientSide && newState.block != this) {
            (level as? ServerLevel)?.let { LanternSavedData.get(it).removeLanternAt(level.dimension(), pos) }
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }
}
