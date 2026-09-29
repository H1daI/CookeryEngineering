package com.cookeryengineering.content.multiblock;

import blusunrize.immersiveengineering.api.multiblocks.BlockMatcher;
import blusunrize.immersiveengineering.api.multiblocks.ClientMultiblocks.MultiblockManualData;
import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.RelativeBlockFace;
import blusunrize.immersiveengineering.api.utils.DirectionUtils;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import com.cookeryengineering.CookeryEngineering;
import com.cookeryengineering.client.FormedMultiblockManualData;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


public final class IndustrialThresherMultiblock extends IETemplateMultiblock {
    public static final BlockPos MASTER = new BlockPos(0, 1, 0);
    public static final BlockPos TRIGGER = MASTER;
    public static final BlockPos REDSTONE_PORT = new BlockPos(0, 1, 3);
    public static final BlockPos ENERGY_PORT = new BlockPos(0, 1, 0);
    public static final BlockPos INPUT_PORT = new BlockPos(2, 1, 1);
    public static final RelativeBlockFace INPUT_FACE = RelativeBlockFace.UP;
    public static final BlockPos OUTPUT_PORT = new BlockPos(2, 0, 3);
    public static final RelativeBlockFace OUTPUT_FACE = RelativeBlockFace.BACK;
    public static final Vec3i SIZE = new Vec3i(4, 3, 4);

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            CookeryEngineering.MOD_ID, "multiblocks/industrial_thresher"
    );

    private final List<StructureBlockInfo> structure;
    private TemplateData templateData;

    public IndustrialThresherMultiblock(
            MultiblockRegistration<IndustrialThresherLogic.State> registration
    ) {
        super(ID, MASTER, TRIGGER, new BlockPos(SIZE), registration);
        this.structure = createStructureDefinition();
    }

    @Override
    public boolean isBlockTrigger(BlockState state, Direction side, @Nonnull Level level) {
        return BlockMatcher.matches(ieBlock("light_engineering"), state, level, null, List.of()).isAllow();
    }

    @Override
    public boolean createStructure(Level level, BlockPos clickedPos, Direction side, Player player) {
        Rotation rotation = DirectionUtils.getRotationBetweenFacings(Direction.NORTH, side);
        if (rotation == null) {
            return false;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        BlockPos rotatedTrigger = StructureTemplate.calculateRelativePosition(settings, TRIGGER);
        BlockPos origin = clickedPos.subtract(rotatedTrigger);

        for (int y = 0; y < SIZE.getY(); y++) {
            for (int z = 0; z < SIZE.getZ(); z++) {
                for (int x = 0; x < SIZE.getX(); x++) {
                    BlockPos relative = new BlockPos(x, y, z);
                    if (structure.stream().noneMatch(info -> info.pos().equals(relative))
                            && !level.isEmptyBlock(origin.offset(StructureTemplate.calculateRelativePosition(settings, relative)))) {
                        return false;
                    }
                }
            }
        }

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
            form(level, origin, rotation, Mirror.NONE, side.getOpposite());
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
        List<StructureBlockInfo> blocks = new ArrayList<>(48);
        String[][] layers = {
                {"BAAA", "BAAA", "AAAA", "AABA"},
                {"B0C0", "BCCC", "00C0", "R000"},
                {"0CCC", "0C0C", "0CCC", "0000"}
        };
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 4; z++) {
                for (int x = 0; x < 4; x++) {
                    BlockState state = switch (layers[y][z].charAt(x)) {
                        case 'A' -> ieBlock("steel_scaffolding_standard");
                        case 'B' -> ieBlock("light_engineering");
                        case 'C' -> ieBlock("sheetmetal_iron");
                        case 'R' -> ieBlock("rs_engineering");
                        default -> Blocks.AIR.defaultBlockState();
                    };
                    if (!state.isAir()) {
                        blocks.add(new StructureBlockInfo(new BlockPos(x, y, z), state, null));
                    }
                }
            }
        }

        return List.copyOf(blocks);
    }

    private static BlockState ieBlock(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("immersiveengineering", path);
        BlockState state = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).defaultBlockState();
        if (state.isAir()) throw new IllegalStateException("Missing thresher component: " + id);
        return state;
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
