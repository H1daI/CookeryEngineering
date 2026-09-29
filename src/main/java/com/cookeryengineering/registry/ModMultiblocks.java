package com.cookeryengineering.registry;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import com.cookeryengineering.CookeryEngineering;
import com.cookeryengineering.content.multiblock.ArthropodCultivationChamberLogic;
import com.cookeryengineering.content.multiblock.ArthropodCultivationChamberMultiblock;
import com.cookeryengineering.content.multiblock.IndustrialMaturationVesselLogic;
import com.cookeryengineering.content.multiblock.IndustrialMaturationVesselMultiblock;
import com.cookeryengineering.content.multiblock.IndustrialThresherLogic;
import com.cookeryengineering.content.multiblock.IndustrialThresherMultiblock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMultiblocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            BuiltInRegistries.BLOCK, CookeryEngineering.MOD_ID
    );
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            BuiltInRegistries.ITEM, CookeryEngineering.MOD_ID
    );
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, CookeryEngineering.MOD_ID
    );

    public static MultiblockRegistration<IndustrialMaturationVesselLogic.State> INDUSTRIAL_MATURATION_VESSEL;
    public static IndustrialMaturationVesselMultiblock INDUSTRIAL_MATURATION_VESSEL_STRUCTURE;
    public static MultiblockRegistration<ArthropodCultivationChamberLogic.State> ARTHROPOD_CULTIVATION_CHAMBER;
    public static ArthropodCultivationChamberMultiblock ARTHROPOD_CULTIVATION_CHAMBER_STRUCTURE;
    public static MultiblockRegistration<IndustrialThresherLogic.State> INDUSTRIAL_THRESHER;
    public static IndustrialThresherMultiblock INDUSTRIAL_THRESHER_STRUCTURE;

    private ModMultiblocks() {
    }

    public static void register(IEventBus modEventBus) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                CookeryEngineering.MOD_ID, "industrial_maturation_vessel"
        );

        INDUSTRIAL_MATURATION_VESSEL = MultiblockRegistration
                .builder(new IndustrialMaturationVesselLogic(), id)
                .notMirrored()
                .defaultBlock(BLOCKS, ITEMS, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .sound(SoundType.METAL)
                        .strength(3.0F, 15.0F)
                        .noOcclusion())
                .defaultBEs(BLOCK_ENTITIES)
                .structure(() -> INDUSTRIAL_MATURATION_VESSEL_STRUCTURE)
                .redstoneAware()
                .build(listener -> listener.accept(modEventBus));

        ResourceLocation chamberId = ResourceLocation.fromNamespaceAndPath(
                CookeryEngineering.MOD_ID, "arthropod_cultivation_chamber"
        );
        ARTHROPOD_CULTIVATION_CHAMBER = MultiblockRegistration
                .builder(new ArthropodCultivationChamberLogic(), chamberId)
                .notMirrored()
                .defaultBlock(BLOCKS, ITEMS, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .sound(SoundType.METAL)
                        .strength(3.0F, 15.0F)
                        .noOcclusion())
                .defaultBEs(BLOCK_ENTITIES)
                .structure(() -> ARTHROPOD_CULTIVATION_CHAMBER_STRUCTURE)
                .redstoneAware()
                .build(listener -> listener.accept(modEventBus));

        INDUSTRIAL_THRESHER = MultiblockRegistration
                .builder(new IndustrialThresherLogic(), ResourceLocation.fromNamespaceAndPath(
                        CookeryEngineering.MOD_ID, "industrial_thresher"))
                .notMirrored()
                .defaultBlock(BLOCKS, ITEMS, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL).sound(SoundType.METAL)
                        .strength(3.0F, 15.0F).noOcclusion())
                .defaultBEs(BLOCK_ENTITIES)
                .structure(() -> INDUSTRIAL_THRESHER_STRUCTURE)
                .redstoneAware()
                .build(listener -> listener.accept(modEventBus));

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        modEventBus.addListener(ModMultiblocks::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            INDUSTRIAL_MATURATION_VESSEL_STRUCTURE =
                    new IndustrialMaturationVesselMultiblock(INDUSTRIAL_MATURATION_VESSEL);
            MultiblockHandler.registerMultiblock(INDUSTRIAL_MATURATION_VESSEL_STRUCTURE);
            ARTHROPOD_CULTIVATION_CHAMBER_STRUCTURE =
                    new ArthropodCultivationChamberMultiblock(ARTHROPOD_CULTIVATION_CHAMBER);
            MultiblockHandler.registerMultiblock(ARTHROPOD_CULTIVATION_CHAMBER_STRUCTURE);
            INDUSTRIAL_THRESHER_STRUCTURE = new IndustrialThresherMultiblock(INDUSTRIAL_THRESHER);
            MultiblockHandler.registerMultiblock(INDUSTRIAL_THRESHER_STRUCTURE);
        });
    }
}
