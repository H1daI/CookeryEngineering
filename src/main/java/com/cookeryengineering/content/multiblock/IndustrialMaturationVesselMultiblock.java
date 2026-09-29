package com.cookeryengineering.content.multiblock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.cookeryengineering.CookeryEngineering;
import com.cookeryengineering.client.FormedMultiblockManualData;

import blusunrize.immersiveengineering.api.multiblocks.BlockMatcher;
import blusunrize.immersiveengineering.api.multiblocks.ClientMultiblocks.MultiblockManualData;
import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import blusunrize.immersiveengineering.api.utils.DirectionUtils;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public final class IndustrialMaturationVesselMultiblock extends IETemplateMultiblock {
    public static final BlockPos MASTER = new BlockPos(1, 1, 1);
    public static final BlockPos TRIGGER = MASTER;
    public static final BlockPos REDSTONE_PORT = new BlockPos(2, 1, 2);
    public static final List<BlockPos> ITEM_INPUT_PORTS = List.of(
            new BlockPos(1, 1, 0),
            new BlockPos(2, 1, 0),
            new BlockPos(2, 1, 1)
    );
    public static final BlockPos FLUID_INPUT_PORT = new BlockPos(0, 0, 1);
    public static final BlockPos ENERGY_INPUT_PORT = new BlockPos(0, 1, 2);
    public static final BlockPos CONTAINER_AND_OUTPUT_PORT = new BlockPos(1, 0, 2);
    public static final Vec3i SIZE = new Vec3i(3, 3, 3);

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            CookeryEngineering.MOD_ID, "multiblocks/industrial_maturation_vessel"
    );

    private final List<StructureBlockInfo> structure;
    private TemplateData templateData;

    public IndustrialMaturationVesselMultiblock(
            MultiblockRegistration<IndustrialMaturationVesselLogic.State> registration
    ) {
        super(ID, MASTER, TRIGGER, new BlockPos(SIZE), registration);
        this.structure = createStructureDefinition();
    }

    @Override
    public boolean isBlockTrigger(BlockState state, Direction side, @Nonnull Level level) {
        return BlockMatcher.matches(block("sheetmetal_iron"), state, level, null, List.of()).isAllow();
    }

    @Override
    public boolean createStructure(Level level, BlockPos clickedPos, Direction side, Player player) {
        Rotation rotation = DirectionUtils.getRotationBetweenFacings(Direction.NORTH, side.getOpposite());
        if (rotation == null) {
            return false;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        BlockPos rotatedTrigger = StructureTemplate.calculateRelativePosition(settings, TRIGGER);
        BlockPos origin = clickedPos.subtract(rotatedTrigger);

        for (StructureBlockInfo info : structure) {
            BlockPos rotatedRelative = StructureTemplate.calculateRelativePosition(settings, info.pos());
            BlockPos actualPos = origin.offset(rotatedRelative);
            BlockState expected = applyToState(info.state(), Mirror.NONE, rotation);
            BlockState found = level.getBlockState(actualPos);
            if (!BlockMatcher.matches(expected, found, level, actualPos, List.of()).isAllow()) {
                return false;
            }
        }

        if (!level.isClientSide) {
            form(level, origin, rotation, Mirror.NONE, side);
        }
        return true;
    }

    @Override
    public List<StructureBlockInfo> getStructure(@Nonnull Level level) {
        return structure;
    }

    @Nonnull
    @Override
    public TemplateData getTemplate(@Nonnull Level level) {
        if (templateData == null) {
            StructureTemplate template = new StructureTemplate();
            template.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), createTemplateTag());
            templateData = new TemplateData(template, structure, structure.stream()
                    .filter(info -> info.pos().equals(TRIGGER))
                    .findFirst()
                    .orElseThrow()
                    .state());
        }
        return templateData;
    }

    @Override
    public Vec3i getSize(Level level) {
        return SIZE;
    }

    @Override
    public boolean canBeMirrored() {
        return false;
    }

    @Override
    public float getManualScale() {
        return 12.0F;
    }

    @Override
    public void initializeClient(Consumer<MultiblockManualData> consumer) {
        consumer.accept(new FormedMultiblockManualData(this));
    }

    private static List<StructureBlockInfo> createStructureDefinition() {
        List<StructureBlockInfo> blocks = new ArrayList<>(20);

        // 底层
        add(blocks, 0, 0, 0, block("steel_scaffolding_standard"));
        add(blocks, 1, 0, 0, block("steel_scaffolding_standard"));
        add(blocks, 2, 0, 0, block("steel_scaffolding_standard"));
        add(blocks, 0, 0, 1, block("fluid_pipe"));
        add(blocks, 1, 0, 1, block("fluid_pipe"));
        add(blocks, 2, 0, 1, block("steel_scaffolding_standard"));
        add(blocks, 0, 0, 2, block("heavy_engineering"));
        add(blocks, 1, 0, 2, block("fluid_pipe"));
        add(blocks, 2, 0, 2, block("steel_scaffolding_standard"));

        // 中层
        add(blocks, 1, 1, 0, block("sheetmetal_iron"));
        add(blocks, 2, 1, 0, block("sheetmetal_iron"));
        add(blocks, 0, 1, 1, block("light_engineering"));
        add(blocks, 1, 1, 1, block("sheetmetal_iron"));
        add(blocks, 2, 1, 1, block("sheetmetal_iron"));
        add(blocks, 0, 1, 2, block("light_engineering"));
        add(blocks, 2, 1, 2, block("rs_engineering"));

        // 顶层
        BlockState slab = block("slab_treated_wood_horizontal")
                .setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                .setValue(BlockStateProperties.WATERLOGGED, false);
        add(blocks, 1, 2, 0, slab);
        add(blocks, 2, 2, 0, slab);
        add(blocks, 0, 2, 1, block("steel_fence"));
        add(blocks, 1, 2, 1, block("light_engineering"));
        add(blocks, 2, 2, 1, slab);

        return List.copyOf(blocks);
    }

    private static BlockState block(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("immersiveengineering", path);
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).defaultBlockState();
    }

    private static void add(List<StructureBlockInfo> blocks, int x, int y, int z, BlockState state) {
        if (state.is(Blocks.AIR)) {
            throw new IllegalStateException("Missing Immersive Engineering block for maturation vessel");
        }
        blocks.add(new StructureBlockInfo(new BlockPos(x, y, z), state, null));
    }

    private CompoundTag createTemplateTag() {
        CompoundTag root = new CompoundTag();
        root.put("size", ints(SIZE.getX(), SIZE.getY(), SIZE.getZ()));

        Map<BlockState, Integer> paletteIndices = new LinkedHashMap<>();
        ListTag palette = new ListTag();
        for (StructureBlockInfo info : structure) {
            paletteIndices.computeIfAbsent(info.state(), state -> {
                palette.add(NbtUtils.writeBlockState(state));
                return palette.size() - 1;
            });
        }
        root.put("palette", palette);

        ListTag blocks = new ListTag();
        for (StructureBlockInfo info : structure) {
            CompoundTag entry = new CompoundTag();
            entry.put("pos", ints(info.pos().getX(), info.pos().getY(), info.pos().getZ()));
            entry.putInt("state", paletteIndices.get(info.state()));
            blocks.add(entry);
        }
        root.put("blocks", blocks);
        root.put("entities", new ListTag());
        return root;
    }

    private static ListTag ints(int... values) {
        ListTag result = new ListTag();
        for (int value : values) {
            result.add(IntTag.valueOf(value));
        }
        return result;
    }
}
