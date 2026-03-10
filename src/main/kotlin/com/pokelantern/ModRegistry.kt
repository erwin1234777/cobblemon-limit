package com.pokelantern

import com.pokelantern.block.PokeLanternBlock
import com.pokelantern.item.CurioLanternItem
import com.pokelantern.item.PokeLanternItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModRegistry {

    val BLOCKS: DeferredRegister<Block> = DeferredRegister.create(ForgeRegistries.BLOCKS, PokeLantern.MOD_ID)
    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, PokeLantern.MOD_ID)

    val POKE_LANTERN_BLOCK: RegistryObject<Block> = BLOCKS.register("poke_lantern") {
        PokeLanternBlock(
            BlockBehaviour.Properties.of()
                .strength(0.5f)
                .lightLevel { 15 }
                .sound(SoundType.LANTERN)
                .noOcclusion()
        )
    }

    val POKE_LANTERN_ITEM: RegistryObject<Item> = ITEMS.register("poke_lantern") {
        PokeLanternItem(POKE_LANTERN_BLOCK.get(), Item.Properties())
    }

    val CURIO_LANTERN_ITEM: RegistryObject<Item> = ITEMS.register("curio_poke_lantern") {
        CurioLanternItem(Item.Properties().stacksTo(1))
    }
}
