package vectorwing.blockbox.common.registry;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import vectorwing.blockbox.BlockBox;
import vectorwing.blockbox.common.block.*;

import javax.annotation.Nullable;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public class ModBlocks
{
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BlockBox.MODID);

	// Port Forge 1.20.1: equivalente minimo a DeferredRegister.Blocks#registerSimpleBlock de NeoForge.
	private static Supplier<Block> registerSimpleBlock(String name, BlockBehaviour.Properties properties) {
		return BLOCKS.register(name, () -> new Block(properties));
	}

	public static final BlockBehaviour.Properties PROPERTIES_PACKED_SNOW = BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.6F).sound(SoundType.SNOW);
	public static final BlockBehaviour.Properties PROPERTIES_PACKED_ICE = BlockBehaviour.Properties.copy(Blocks.PACKED_ICE).strength(0.4F).requiresCorrectToolForDrops();
	public static final BlockBehaviour.Properties PROPERTIES_IRON_PLATE = BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0F, 6.0F).sound(SoundType.NETHERITE_BLOCK);
	public static final BlockBehaviour.Properties PROPERTIES_PALISADE = BlockBehaviour.Properties.of().strength(2.0F).instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).ignitedByLava();
	public static final BlockBehaviour.Properties PROPERTIES_SKY_LANTERN = BlockBehaviour.Properties.of().strength(0.2F).instrument(NoteBlockInstrument.GUITAR).noOcclusion().lightLevel((state) -> 15).ignitedByLava().sound(SoundType.WOOL);

	public static final Supplier<Block> GRANITE_BRICKS = registerSimpleBlock("granite_bricks", BlockBehaviour.Properties.copy(Blocks.GRANITE));
	public static final Supplier<Block> GRANITE_BRICK_STAIRS = BLOCKS.register("granite_brick_stairs", () -> stair(ModBlocks.GRANITE_BRICKS.get(), BlockBehaviour.Properties.copy(Blocks.GRANITE)));
	public static final Supplier<Block> GRANITE_BRICK_SLAB = BLOCKS.register("granite_brick_slab", () -> slab(BlockBehaviour.Properties.copy(Blocks.GRANITE)));
	public static final Supplier<Block> GRANITE_BRICK_WALL = BLOCKS.register("granite_brick_wall", () -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.GRANITE).forceSolidOn()));
	public static final Supplier<Block> DIORITE_BRICKS = registerSimpleBlock("diorite_bricks", BlockBehaviour.Properties.copy(Blocks.DIORITE));
	public static final Supplier<Block> DIORITE_BRICK_STAIRS = BLOCKS.register("diorite_brick_stairs", () -> stair(ModBlocks.DIORITE_BRICKS.get(), BlockBehaviour.Properties.copy(Blocks.DIORITE)));
	public static final Supplier<Block> DIORITE_BRICK_SLAB = BLOCKS.register("diorite_brick_slab", () -> slab(BlockBehaviour.Properties.copy(Blocks.DIORITE)));
	public static final Supplier<Block> DIORITE_BRICK_WALL = BLOCKS.register("diorite_brick_wall", () -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.DIORITE).forceSolidOn()));
	public static final Supplier<Block> ANDESITE_BRICKS = registerSimpleBlock("andesite_bricks", BlockBehaviour.Properties.copy(Blocks.ANDESITE));
	public static final Supplier<Block> ANDESITE_BRICK_STAIRS = BLOCKS.register("andesite_brick_stairs", () -> stair(ModBlocks.ANDESITE_BRICKS.get(), BlockBehaviour.Properties.copy(Blocks.ANDESITE)));
	public static final Supplier<Block> ANDESITE_BRICK_SLAB = BLOCKS.register("andesite_brick_slab", () -> slab(BlockBehaviour.Properties.copy(Blocks.ANDESITE)));
	public static final Supplier<Block> ANDESITE_BRICK_WALL = BLOCKS.register("andesite_brick_wall", () -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.ANDESITE).forceSolidOn()));

	public static final Supplier<Block> SANDSTONE_BRICKS = registerSimpleBlock("sandstone_bricks", BlockBehaviour.Properties.copy(Blocks.SANDSTONE));
	public static final Supplier<Block> SANDSTONE_BRICK_STAIRS = BLOCKS.register("sandstone_brick_stairs", () -> stair(ModBlocks.SANDSTONE_BRICKS.get(), BlockBehaviour.Properties.copy(Blocks.SANDSTONE)));
	public static final Supplier<Block> SANDSTONE_BRICK_SLAB = BLOCKS.register("sandstone_brick_slab", () -> slab(BlockBehaviour.Properties.copy(ModBlocks.SANDSTONE_BRICKS.get())));
	public static final Supplier<Block> RED_SANDSTONE_BRICKS = registerSimpleBlock("red_sandstone_bricks", BlockBehaviour.Properties.copy(Blocks.RED_SANDSTONE));
	public static final Supplier<Block> RED_SANDSTONE_BRICK_STAIRS = BLOCKS.register("red_sandstone_brick_stairs", () -> stair(ModBlocks.RED_SANDSTONE_BRICKS.get(), BlockBehaviour.Properties.copy(Blocks.RED_SANDSTONE)));
	public static final Supplier<Block> RED_SANDSTONE_BRICK_SLAB = BLOCKS.register("red_sandstone_brick_slab", () -> slab(BlockBehaviour.Properties.copy(ModBlocks.RED_SANDSTONE_BRICKS.get())));

	public static final Supplier<Block> TILES = registerSimpleBlock("tiles", BlockBehaviour.Properties.copy(Blocks.BRICKS));
	public static final Supplier<Block> TILE_STAIRS = BLOCKS.register("tile_stairs", () -> stair(ModBlocks.TILES.get(), BlockBehaviour.Properties.copy(Blocks.BRICKS)));
	public static final Supplier<Block> TILE_SLAB = BLOCKS.register("tile_slab", () -> slab(BlockBehaviour.Properties.copy(Blocks.BRICKS)));
	public static final Supplier<Block> BROKEN_TILE_MOSAIC = registerSimpleBlock("broken_tile_mosaic", BlockBehaviour.Properties.copy(Blocks.BRICKS));

	public static final Supplier<Block> PACKED_SNOW = BLOCKS.register("packed_snow", () -> new PackedSnowBlock(PROPERTIES_PACKED_SNOW));
	public static final Supplier<Block> CARVED_SNOW = BLOCKS.register("carved_snow", () -> new CarvedSnowBlock(PROPERTIES_PACKED_SNOW));
	public static final Supplier<Block> SNOW_BRICKS = registerSimpleBlock("snow_bricks", PROPERTIES_PACKED_SNOW);
	public static final Supplier<Block> SNOW_BRICK_STAIRS = BLOCKS.register("snow_brick_stairs", () -> stair(ModBlocks.SNOW_BRICKS.get(), PROPERTIES_PACKED_SNOW));
	public static final Supplier<Block> SNOW_BRICK_SLAB = BLOCKS.register("snow_brick_slab", () -> slab(PROPERTIES_PACKED_SNOW));
	public static final Supplier<Block> SNOW_BRICK_WALL = BLOCKS.register("snow_brick_wall", () -> new WallBlock(PROPERTIES_PACKED_SNOW.forceSolidOn()));
	public static final Supplier<Block> POLISHED_PACKED_ICE = registerSimpleBlock("polished_packed_ice", PROPERTIES_PACKED_ICE);
	public static final Supplier<Block> PACKED_ICE_BRICKS = registerSimpleBlock("packed_ice_bricks", PROPERTIES_PACKED_ICE);
	public static final Supplier<Block> PACKED_ICE_BRICK_STAIRS = BLOCKS.register("packed_ice_brick_stairs", () -> stair(ModBlocks.PACKED_ICE_BRICKS.get(), PROPERTIES_PACKED_ICE));
	public static final Supplier<Block> PACKED_ICE_BRICK_SLAB = BLOCKS.register("packed_ice_brick_slab", () -> slab(PROPERTIES_PACKED_ICE));
	public static final Supplier<Block> PACKED_ICE_BRICK_WALL = BLOCKS.register("packed_ice_brick_wall", () -> new WallBlock(PROPERTIES_PACKED_ICE.forceSolidOn()));

	public static final Supplier<Block> POLISHED_OBSIDIAN = BLOCKS.register("polished_obsidian", () -> new PortalFrameBlock(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN)));

	public static final Supplier<Block> ROUGH_GLASS = BLOCKS.register("rough_glass", () -> new GlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS)));
	public static final Supplier<Block> ROUGH_GLASS_PANE = BLOCKS.register("rough_glass_pane", () -> new IronBarsBlock(BlockBehaviour.Properties.copy(Blocks.GLASS_PANE)));

	public static final Supplier<Block> COPPER_BARS = BLOCKS.register("copper_bars", () -> new WeatheringCopperBarsBlock(WeatheringCopper.WeatherState.UNAFFECTED, BlockBehaviour.Properties.copy(Blocks.COPPER_BLOCK)));
	public static final Supplier<Block> EXPOSED_COPPER_BARS = BLOCKS.register("exposed_copper_bars", () -> new WeatheringCopperBarsBlock(WeatheringCopper.WeatherState.EXPOSED, BlockBehaviour.Properties.copy(Blocks.EXPOSED_COPPER)));
	public static final Supplier<Block> WEATHERED_COPPER_BARS = BLOCKS.register("weathered_copper_bars", () -> new WeatheringCopperBarsBlock(WeatheringCopper.WeatherState.WEATHERED, BlockBehaviour.Properties.copy(Blocks.WEATHERED_COPPER)));
	public static final Supplier<Block> OXIDIZED_COPPER_BARS = BLOCKS.register("oxidized_copper_bars", () -> new WeatheringCopperBarsBlock(WeatheringCopper.WeatherState.OXIDIZED, BlockBehaviour.Properties.copy(Blocks.OXIDIZED_COPPER)));
	public static final Supplier<Block> WAXED_COPPER_BARS = BLOCKS.register("waxed_copper_bars", () -> new IronBarsBlock(BlockBehaviour.Properties.copy(Blocks.COPPER_BLOCK)));
	public static final Supplier<Block> WAXED_EXPOSED_COPPER_BARS = BLOCKS.register("waxed_exposed_copper_bars", () -> new IronBarsBlock(BlockBehaviour.Properties.copy(Blocks.EXPOSED_COPPER)));
	public static final Supplier<Block> WAXED_WEATHERED_COPPER_BARS = BLOCKS.register("waxed_weathered_copper_bars", () -> new IronBarsBlock(BlockBehaviour.Properties.copy(Blocks.WEATHERED_COPPER)));
	public static final Supplier<Block> WAXED_OXIDIZED_COPPER_BARS = BLOCKS.register("waxed_oxidized_copper_bars", () -> new IronBarsBlock(BlockBehaviour.Properties.copy(Blocks.OXIDIZED_COPPER)));

	public static final Supplier<Block> COPPER_PILLAR = BLOCKS.register("copper_pillar", () -> new WeatheringCopperPillarBlock(WeatheringCopper.WeatherState.UNAFFECTED, BlockBehaviour.Properties.copy(Blocks.COPPER_BLOCK)));
	public static final Supplier<Block> EXPOSED_COPPER_PILLAR = BLOCKS.register("exposed_copper_pillar", () -> new WeatheringCopperPillarBlock(WeatheringCopper.WeatherState.EXPOSED, BlockBehaviour.Properties.copy(Blocks.EXPOSED_COPPER)));
	public static final Supplier<Block> WEATHERED_COPPER_PILLAR = BLOCKS.register("weathered_copper_pillar", () -> new WeatheringCopperPillarBlock(WeatheringCopper.WeatherState.WEATHERED, BlockBehaviour.Properties.copy(Blocks.WEATHERED_COPPER)));
	public static final Supplier<Block> OXIDIZED_COPPER_PILLAR = BLOCKS.register("oxidized_copper_pillar", () -> new WeatheringCopperPillarBlock(WeatheringCopper.WeatherState.OXIDIZED, BlockBehaviour.Properties.copy(Blocks.OXIDIZED_COPPER)));
	public static final Supplier<Block> WAXED_COPPER_PILLAR = BLOCKS.register("waxed_copper_pillar", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.COPPER_BLOCK)));
	public static final Supplier<Block> WAXED_EXPOSED_COPPER_PILLAR = BLOCKS.register("waxed_exposed_copper_pillar", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.EXPOSED_COPPER)));
	public static final Supplier<Block> WAXED_WEATHERED_COPPER_PILLAR = BLOCKS.register("waxed_weathered_copper_pillar", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.WEATHERED_COPPER)));
	public static final Supplier<Block> WAXED_OXIDIZED_COPPER_PILLAR = BLOCKS.register("waxed_oxidized_copper_pillar", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OXIDIZED_COPPER)));

	public static final Supplier<Block> IRON_PLATE = registerSimpleBlock("iron_plate", PROPERTIES_IRON_PLATE);
	public static final Supplier<Block> IRON_TREAD_PLATE = registerSimpleBlock("iron_tread_plate", PROPERTIES_IRON_PLATE);
	public static final Supplier<Block> IRON_TREAD_PLATE_STAIRS = BLOCKS.register("iron_tread_plate_stairs", () -> stair(ModBlocks.IRON_TREAD_PLATE.get(), PROPERTIES_IRON_PLATE));
	public static final Supplier<Block> IRON_TREAD_PLATE_SLAB = BLOCKS.register("iron_tread_plate_slab", () -> slab(PROPERTIES_IRON_PLATE));
	public static final Supplier<Block> CORRUGATED_IRON_PLATE = registerSimpleBlock("corrugated_iron_plate", PROPERTIES_IRON_PLATE);
	public static final Supplier<Block> CORRUGATED_IRON_PLATE_STAIRS = BLOCKS.register("corrugated_iron_plate_stairs", () -> stair(ModBlocks.CORRUGATED_IRON_PLATE.get(), PROPERTIES_IRON_PLATE));
	public static final Supplier<Block> CORRUGATED_IRON_PLATE_SLAB = BLOCKS.register("corrugated_iron_plate_slab", () -> slab(PROPERTIES_IRON_PLATE));
	public static final Supplier<Block> IRON_PLATE_PILLAR = BLOCKS.register("iron_plate_pillar",
			() -> new RotatedPillarBlock(PROPERTIES_IRON_PLATE));
	public static final Supplier<Block> IRON_PLATE_DOOR = BLOCKS.register("iron_plate_door",
			() -> new DoorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_DOOR), ModBlockSets.IRON_PLATE.get()));
	public static final Supplier<Block> IRON_PLATE_TRAPDOOR = BLOCKS.register("iron_plate_trapdoor",
			() -> new TrapDoorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_TRAPDOOR), ModBlockSets.IRON_PLATE.get()));

	public static final Supplier<Block> CHISELED_GOLD = registerSimpleBlock("chiseled_gold", BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK));
	public static final Supplier<Block> GOLDEN_TILES = registerSimpleBlock("golden_tiles", BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK));
	public static final Supplier<Block> GOLDEN_BRICKS = registerSimpleBlock("golden_bricks", BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK));
	public static final Supplier<Block> GOLDEN_BRICK_STAIRS = BLOCKS.register("golden_brick_stairs", () -> stair(ModBlocks.GOLDEN_BRICKS.get(), BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK)));
	public static final Supplier<Block> GOLDEN_BRICK_SLAB = BLOCKS.register("golden_brick_slab", () -> slab(BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK)));
	public static final Supplier<Block> GOLDEN_PILLAR = BLOCKS.register("golden_pillar",
			() -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK)));
	public static final Supplier<Block> GOLDEN_DOOR = BLOCKS.register("golden_door",
			() -> new DoorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_DOOR), ModBlockSets.GOLD.get()));
	public static final Supplier<Block> GOLDEN_TRAPDOOR = BLOCKS.register("golden_trapdoor",
			() -> new TrapDoorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_TRAPDOOR), ModBlockSets.GOLD.get()));
	public static final Supplier<Block> GOLDEN_BARS = BLOCKS.register("golden_bars",
			() -> new IronBarsBlock(BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK)));

	public static final Supplier<Block> POLISHED_AMETHYST = registerSimpleBlock("polished_amethyst", BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK));
	public static final Supplier<Block> CUT_AMETHYST = registerSimpleBlock("cut_amethyst", BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK));
	public static final Supplier<Block> CUT_AMETHYST_STAIRS = BLOCKS.register("cut_amethyst_stairs", () -> stair(ModBlocks.CUT_AMETHYST.get(), BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)));
	public static final Supplier<Block> CUT_AMETHYST_SLAB = BLOCKS.register("cut_amethyst_slab", () -> slab(BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)));
	public static final Supplier<Block> AMETHYST_MOSAIC = registerSimpleBlock("amethyst_mosaic", BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK));
	public static final Supplier<Block> AMETHYST_MOSAIC_STAIRS = BLOCKS.register("amethyst_mosaic_stairs", () -> stair(ModBlocks.AMETHYST_MOSAIC.get(), BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)));
	public static final Supplier<Block> AMETHYST_MOSAIC_SLAB = BLOCKS.register("amethyst_mosaic_slab", () -> slab(BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)));

	public static final Supplier<Block> LAPIS_LAZULI_BRICKS = registerSimpleBlock("lapis_lazuli_bricks", BlockBehaviour.Properties.copy(Blocks.LAPIS_BLOCK));
	public static final Supplier<Block> LAPIS_LAZULI_BRICK_STAIRS = BLOCKS.register("lapis_lazuli_brick_stairs", () -> stair(ModBlocks.LAPIS_LAZULI_BRICKS.get(), BlockBehaviour.Properties.copy(Blocks.LAPIS_BLOCK)));
	public static final Supplier<Block> LAPIS_LAZULI_BRICK_SLAB = BLOCKS.register("lapis_lazuli_brick_slab", () -> slab(BlockBehaviour.Properties.copy(Blocks.LAPIS_BLOCK)));
	public static final Supplier<Block> LAPIS_LAZULI_MOSAIC = registerSimpleBlock("lapis_lazuli_mosaic", BlockBehaviour.Properties.copy(Blocks.LAPIS_BLOCK));
	public static final Supplier<Block> LAPIS_LAZULI_MOSAIC_STAIRS = BLOCKS.register("lapis_lazuli_mosaic_stairs", () -> stair(ModBlocks.LAPIS_LAZULI_MOSAIC.get(), BlockBehaviour.Properties.copy(Blocks.LAPIS_BLOCK)));
	public static final Supplier<Block> LAPIS_LAZULI_MOSAIC_SLAB = BLOCKS.register("lapis_lazuli_mosaic_slab", () -> slab(BlockBehaviour.Properties.copy(Blocks.LAPIS_BLOCK)));

	public static final Supplier<Block> OAK_SEAT = BLOCKS.register("oak_seat", () -> new SeatBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)));
	public static final Supplier<Block> SPRUCE_SEAT = BLOCKS.register("spruce_seat", () -> new SeatBlock(BlockBehaviour.Properties.copy(Blocks.SPRUCE_PLANKS)));
	public static final Supplier<Block> BIRCH_SEAT = BLOCKS.register("birch_seat", () -> new SeatBlock(BlockBehaviour.Properties.copy(Blocks.BIRCH_PLANKS)));
	public static final Supplier<Block> JUNGLE_SEAT = BLOCKS.register("jungle_seat", () -> new SeatBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_PLANKS)));
	public static final Supplier<Block> ACACIA_SEAT = BLOCKS.register("acacia_seat", () -> new SeatBlock(BlockBehaviour.Properties.copy(Blocks.ACACIA_PLANKS)));
	public static final Supplier<Block> DARK_OAK_SEAT = BLOCKS.register("dark_oak_seat", () -> new SeatBlock(BlockBehaviour.Properties.copy(Blocks.DARK_OAK_PLANKS)));
	public static final Supplier<Block> MANGROVE_SEAT = BLOCKS.register("mangrove_seat", () -> new SeatBlock(BlockBehaviour.Properties.copy(Blocks.MANGROVE_PLANKS)));
	public static final Supplier<Block> CHERRY_SEAT = BLOCKS.register("cherry_seat", () -> new SeatBlock(BlockBehaviour.Properties.copy(Blocks.CHERRY_PLANKS)));
	public static final Supplier<Block> BAMBOO_SEAT = BLOCKS.register("bamboo_seat", () -> new SeatBlock(BlockBehaviour.Properties.copy(Blocks.BAMBOO_PLANKS)));
	public static final Supplier<Block> CRIMSON_SEAT = BLOCKS.register("crimson_seat", () -> new SeatBlock(BlockBehaviour.Properties.copy(Blocks.CRIMSON_PLANKS)));
	public static final Supplier<Block> WARPED_SEAT = BLOCKS.register("warped_seat", () -> new SeatBlock(BlockBehaviour.Properties.copy(Blocks.WARPED_PLANKS)));

	public static final Supplier<Block> OAK_PALISADE = BLOCKS.register("oak_palisade", () -> palisade(ModBlocks.SPIKED_OAK_PALISADE, ModBlocks.STRIPPED_OAK_PALISADE, MapColor.WOOD));
	public static final Supplier<Block> SPIKED_OAK_PALISADE = BLOCKS.register("spiked_oak_palisade", () -> spikedPalisade(ModBlocks.STRIPPED_SPIKED_OAK_PALISADE, MapColor.WOOD));
	public static final Supplier<Block> SPRUCE_PALISADE = BLOCKS.register("spruce_palisade", () -> palisade(ModBlocks.SPIKED_SPRUCE_PALISADE, ModBlocks.STRIPPED_SPRUCE_PALISADE, MapColor.PODZOL));
	public static final Supplier<Block> SPIKED_SPRUCE_PALISADE = BLOCKS.register("spiked_spruce_palisade", () -> spikedPalisade(ModBlocks.STRIPPED_SPIKED_SPRUCE_PALISADE, MapColor.PODZOL));
	public static final Supplier<Block> BIRCH_PALISADE = BLOCKS.register("birch_palisade", () -> palisade(ModBlocks.SPIKED_BIRCH_PALISADE, ModBlocks.STRIPPED_BIRCH_PALISADE, MapColor.SAND));
	public static final Supplier<Block> SPIKED_BIRCH_PALISADE = BLOCKS.register("spiked_birch_palisade", () -> spikedPalisade(ModBlocks.STRIPPED_SPIKED_BIRCH_PALISADE, MapColor.SAND));
	public static final Supplier<Block> JUNGLE_PALISADE = BLOCKS.register("jungle_palisade", () -> palisade(ModBlocks.SPIKED_JUNGLE_PALISADE, ModBlocks.STRIPPED_JUNGLE_PALISADE, MapColor.DIRT));
	public static final Supplier<Block> SPIKED_JUNGLE_PALISADE = BLOCKS.register("spiked_jungle_palisade", () -> spikedPalisade(ModBlocks.STRIPPED_SPIKED_JUNGLE_PALISADE, MapColor.DIRT));
	public static final Supplier<Block> ACACIA_PALISADE = BLOCKS.register("acacia_palisade", () -> palisade(ModBlocks.SPIKED_ACACIA_PALISADE, ModBlocks.STRIPPED_ACACIA_PALISADE, MapColor.COLOR_ORANGE));
	public static final Supplier<Block> SPIKED_ACACIA_PALISADE = BLOCKS.register("spiked_acacia_palisade", () -> spikedPalisade(ModBlocks.STRIPPED_SPIKED_ACACIA_PALISADE, MapColor.COLOR_ORANGE));
	public static final Supplier<Block> DARK_OAK_PALISADE = BLOCKS.register("dark_oak_palisade", () -> palisade(ModBlocks.SPIKED_DARK_OAK_PALISADE, ModBlocks.STRIPPED_DARK_OAK_PALISADE, MapColor.COLOR_BROWN));
	public static final Supplier<Block> SPIKED_DARK_OAK_PALISADE = BLOCKS.register("spiked_dark_oak_palisade", () -> spikedPalisade(ModBlocks.STRIPPED_SPIKED_DARK_OAK_PALISADE, MapColor.COLOR_BROWN));
	public static final Supplier<Block> MANGROVE_PALISADE = BLOCKS.register("mangrove_palisade", () -> palisade(ModBlocks.SPIKED_MANGROVE_PALISADE, ModBlocks.STRIPPED_MANGROVE_PALISADE, MapColor.COLOR_RED));
	public static final Supplier<Block> SPIKED_MANGROVE_PALISADE = BLOCKS.register("spiked_mangrove_palisade", () -> spikedPalisade(ModBlocks.STRIPPED_SPIKED_MANGROVE_PALISADE, MapColor.COLOR_RED));
	public static final Supplier<Block> CHERRY_PALISADE = BLOCKS.register("cherry_palisade", () -> palisade(ModBlocks.SPIKED_CHERRY_PALISADE, ModBlocks.STRIPPED_CHERRY_PALISADE, MapColor.TERRACOTTA_WHITE, SoundType.CHERRY_WOOD));
	public static final Supplier<Block> SPIKED_CHERRY_PALISADE = BLOCKS.register("spiked_cherry_palisade", () -> spikedPalisade(ModBlocks.STRIPPED_SPIKED_CHERRY_PALISADE, MapColor.TERRACOTTA_WHITE, SoundType.CHERRY_WOOD));
	public static final Supplier<Block> CRIMSON_PALISADE = BLOCKS.register("crimson_palisade", () -> netherPalisade(ModBlocks.SPIKED_CRIMSON_PALISADE, ModBlocks.STRIPPED_CRIMSON_PALISADE, MapColor.CRIMSON_STEM));
	public static final Supplier<Block> SPIKED_CRIMSON_PALISADE = BLOCKS.register("spiked_crimson_palisade", () -> netherSpikedPalisade(ModBlocks.STRIPPED_SPIKED_CRIMSON_PALISADE, MapColor.CRIMSON_STEM));
	public static final Supplier<Block> WARPED_PALISADE = BLOCKS.register("warped_palisade", () -> netherPalisade(ModBlocks.SPIKED_WARPED_PALISADE, ModBlocks.STRIPPED_WARPED_PALISADE, MapColor.WARPED_STEM));
	public static final Supplier<Block> SPIKED_WARPED_PALISADE = BLOCKS.register("spiked_warped_palisade", () -> netherSpikedPalisade(ModBlocks.STRIPPED_SPIKED_WARPED_PALISADE, MapColor.WARPED_STEM));

	public static final Supplier<Block> STRIPPED_OAK_PALISADE = BLOCKS.register("stripped_oak_palisade", () -> palisade(ModBlocks.STRIPPED_SPIKED_OAK_PALISADE, null, MapColor.WOOD));
	public static final Supplier<Block> STRIPPED_SPIKED_OAK_PALISADE = BLOCKS.register("stripped_spiked_oak_palisade", () -> spikedPalisade(null, MapColor.WOOD));
	public static final Supplier<Block> STRIPPED_SPRUCE_PALISADE = BLOCKS.register("stripped_spruce_palisade", () -> palisade(ModBlocks.STRIPPED_SPIKED_SPRUCE_PALISADE, null, MapColor.PODZOL));
	public static final Supplier<Block> STRIPPED_SPIKED_SPRUCE_PALISADE = BLOCKS.register("stripped_spiked_spruce_palisade", () -> spikedPalisade(null, MapColor.PODZOL));
	public static final Supplier<Block> STRIPPED_BIRCH_PALISADE = BLOCKS.register("stripped_birch_palisade", () -> palisade(ModBlocks.STRIPPED_SPIKED_BIRCH_PALISADE, null, MapColor.SAND));
	public static final Supplier<Block> STRIPPED_SPIKED_BIRCH_PALISADE = BLOCKS.register("stripped_spiked_birch_palisade", () -> spikedPalisade(null, MapColor.SAND));
	public static final Supplier<Block> STRIPPED_JUNGLE_PALISADE = BLOCKS.register("stripped_jungle_palisade", () -> palisade(ModBlocks.STRIPPED_SPIKED_JUNGLE_PALISADE, null, MapColor.DIRT));
	public static final Supplier<Block> STRIPPED_SPIKED_JUNGLE_PALISADE = BLOCKS.register("stripped_spiked_jungle_palisade", () -> spikedPalisade(null, MapColor.DIRT));
	public static final Supplier<Block> STRIPPED_ACACIA_PALISADE = BLOCKS.register("stripped_acacia_palisade", () -> palisade(ModBlocks.STRIPPED_SPIKED_ACACIA_PALISADE, null, MapColor.COLOR_ORANGE));
	public static final Supplier<Block> STRIPPED_SPIKED_ACACIA_PALISADE = BLOCKS.register("stripped_spiked_acacia_palisade", () -> spikedPalisade(null, MapColor.COLOR_ORANGE));
	public static final Supplier<Block> STRIPPED_DARK_OAK_PALISADE = BLOCKS.register("stripped_dark_oak_palisade", () -> palisade(ModBlocks.STRIPPED_SPIKED_DARK_OAK_PALISADE, null, MapColor.COLOR_BROWN));
	public static final Supplier<Block> STRIPPED_SPIKED_DARK_OAK_PALISADE = BLOCKS.register("stripped_spiked_dark_oak_palisade", () -> spikedPalisade(null, MapColor.COLOR_BROWN));
	public static final Supplier<Block> STRIPPED_MANGROVE_PALISADE = BLOCKS.register("stripped_mangrove_palisade", () -> palisade(ModBlocks.STRIPPED_SPIKED_MANGROVE_PALISADE, null, MapColor.COLOR_RED));
	public static final Supplier<Block> STRIPPED_SPIKED_MANGROVE_PALISADE = BLOCKS.register("stripped_spiked_mangrove_palisade", () -> spikedPalisade(null, MapColor.COLOR_RED));
	public static final Supplier<Block> STRIPPED_CHERRY_PALISADE = BLOCKS.register("stripped_cherry_palisade", () -> palisade(ModBlocks.STRIPPED_SPIKED_CHERRY_PALISADE, null, MapColor.TERRACOTTA_WHITE, SoundType.CHERRY_WOOD));
	public static final Supplier<Block> STRIPPED_SPIKED_CHERRY_PALISADE = BLOCKS.register("stripped_spiked_cherry_palisade", () -> spikedPalisade(null, MapColor.TERRACOTTA_WHITE, SoundType.CHERRY_WOOD));
	public static final Supplier<Block> STRIPPED_CRIMSON_PALISADE = BLOCKS.register("stripped_crimson_palisade", () -> netherPalisade(ModBlocks.STRIPPED_SPIKED_CRIMSON_PALISADE, null, MapColor.CRIMSON_STEM));
	public static final Supplier<Block> STRIPPED_SPIKED_CRIMSON_PALISADE = BLOCKS.register("stripped_spiked_crimson_palisade", () -> netherSpikedPalisade(null, MapColor.CRIMSON_STEM));
	public static final Supplier<Block> STRIPPED_WARPED_PALISADE = BLOCKS.register("stripped_warped_palisade", () -> netherPalisade(ModBlocks.STRIPPED_SPIKED_WARPED_PALISADE, null, MapColor.WARPED_STEM));
	public static final Supplier<Block> STRIPPED_SPIKED_WARPED_PALISADE = BLOCKS.register("stripped_spiked_warped_palisade", () -> netherSpikedPalisade(null, MapColor.WARPED_STEM));


	public static final Supplier<Block> BRAZIER = BLOCKS.register("brazier", () ->  new BrazierBlock(1, BlockBehaviour.Properties.copy(Blocks.LANTERN)
			.lightLevel(litBlockEmission(15))
	));
	public static final Supplier<Block> SOUL_BRAZIER = BLOCKS.register("soul_brazier", () ->  new BrazierBlock(2, BlockBehaviour.Properties.copy(Blocks.SOUL_LANTERN)
			.lightLevel(litBlockEmission(10))
	));

	public static final Supplier<Block> WHITE_SKY_LANTERN = BLOCKS.register("white_sky_lantern", () ->  new SkyLanternBlock(PROPERTIES_SKY_LANTERN.mapColor(MapColor.SNOW)));
	public static final Supplier<Block> LIGHT_GRAY_SKY_LANTERN = BLOCKS.register("light_gray_sky_lantern", () ->  new SkyLanternBlock(PROPERTIES_SKY_LANTERN.mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Supplier<Block> GRAY_SKY_LANTERN = BLOCKS.register("gray_sky_lantern", () ->  new SkyLanternBlock(PROPERTIES_SKY_LANTERN.mapColor(MapColor.COLOR_GRAY)));
	public static final Supplier<Block> BLACK_SKY_LANTERN = BLOCKS.register("black_sky_lantern", () ->  new SkyLanternBlock(PROPERTIES_SKY_LANTERN.mapColor(MapColor.COLOR_BLACK)));
	public static final Supplier<Block> BROWN_SKY_LANTERN = BLOCKS.register("brown_sky_lantern", () ->  new SkyLanternBlock(PROPERTIES_SKY_LANTERN.mapColor(MapColor.COLOR_BROWN)));
	public static final Supplier<Block> RED_SKY_LANTERN = BLOCKS.register("red_sky_lantern", () ->  new SkyLanternBlock(PROPERTIES_SKY_LANTERN.mapColor(MapColor.COLOR_RED)));
	public static final Supplier<Block> ORANGE_SKY_LANTERN = BLOCKS.register("orange_sky_lantern", () ->  new SkyLanternBlock(PROPERTIES_SKY_LANTERN.mapColor(MapColor.COLOR_ORANGE)));
	public static final Supplier<Block> YELLOW_SKY_LANTERN = BLOCKS.register("yellow_sky_lantern", () ->  new SkyLanternBlock(PROPERTIES_SKY_LANTERN.mapColor(MapColor.COLOR_YELLOW)));
	public static final Supplier<Block> LIME_SKY_LANTERN = BLOCKS.register("lime_sky_lantern", () ->  new SkyLanternBlock(PROPERTIES_SKY_LANTERN.mapColor(MapColor.COLOR_LIGHT_GREEN)));
	public static final Supplier<Block> GREEN_SKY_LANTERN = BLOCKS.register("green_sky_lantern", () ->  new SkyLanternBlock(PROPERTIES_SKY_LANTERN.mapColor(MapColor.COLOR_GREEN)));
	public static final Supplier<Block> CYAN_SKY_LANTERN = BLOCKS.register("cyan_sky_lantern", () ->  new SkyLanternBlock(PROPERTIES_SKY_LANTERN.mapColor(MapColor.COLOR_CYAN)));
	public static final Supplier<Block> LIGHT_BLUE_SKY_LANTERN = BLOCKS.register("light_blue_sky_lantern", () ->  new SkyLanternBlock(PROPERTIES_SKY_LANTERN.mapColor(MapColor.COLOR_LIGHT_BLUE)));
	public static final Supplier<Block> BLUE_SKY_LANTERN = BLOCKS.register("blue_sky_lantern", () ->  new SkyLanternBlock(PROPERTIES_SKY_LANTERN.mapColor(MapColor.COLOR_BLUE)));
	public static final Supplier<Block> PURPLE_SKY_LANTERN = BLOCKS.register("purple_sky_lantern", () ->  new SkyLanternBlock(PROPERTIES_SKY_LANTERN.mapColor(MapColor.COLOR_PURPLE)));
	public static final Supplier<Block> MAGENTA_SKY_LANTERN = BLOCKS.register("magenta_sky_lantern", () ->  new SkyLanternBlock(PROPERTIES_SKY_LANTERN.mapColor(MapColor.COLOR_MAGENTA)));
	public static final Supplier<Block> PINK_SKY_LANTERN = BLOCKS.register("pink_sky_lantern", () ->  new SkyLanternBlock(PROPERTIES_SKY_LANTERN.mapColor(MapColor.COLOR_PINK)));

	private static ToIntFunction<BlockState> litBlockEmission(int lightValue) {
		return state -> state.getValue(BlockStateProperties.LIT) ? lightValue : 0;
	}

	private static Block stair(Block baseBlock, BlockBehaviour.Properties properties) {
		return new StairBlock(baseBlock.defaultBlockState(), properties);
	}

	private static Block slab(BlockBehaviour.Properties properties) {
		return new SlabBlock(properties);
	}

	private static Block palisade(Supplier<Block> spikedForm, @Nullable Supplier<Block> strippedForm, MapColor mapColor) {
		return palisade(spikedForm, strippedForm, mapColor, SoundType.WOOD);
	}

	private static Block palisade(Supplier<Block> spikedForm, @Nullable Supplier<Block> strippedForm, MapColor mapColor, SoundType soundType) {
		return new PalisadeBlock(spikedForm, strippedForm, PROPERTIES_PALISADE.mapColor(mapColor).sound(soundType).ignitedByLava());
	}

	private static Block netherPalisade(Supplier<Block> spikedForm, @Nullable Supplier<Block> strippedForm, MapColor mapColor) {
		return new PalisadeBlock(spikedForm, strippedForm, PROPERTIES_PALISADE.mapColor(mapColor).sound(SoundType.STEM));
	}

	private static Block spikedPalisade(@Nullable Supplier<Block> strippedForm, MapColor mapColor) {
		return spikedPalisade(strippedForm, mapColor, SoundType.WOOD);
	}

	private static Block spikedPalisade(@Nullable Supplier<Block> strippedForm, MapColor mapColor, SoundType soundType) {
		return new SpikedPalisadeBlock(strippedForm, PROPERTIES_PALISADE.mapColor(mapColor).sound(soundType).ignitedByLava());
	}

	private static Block netherSpikedPalisade(@Nullable Supplier<Block> strippedForm, MapColor mapColor) {
		return new SpikedPalisadeBlock(strippedForm, PROPERTIES_PALISADE.mapColor(mapColor).sound(SoundType.STEM));
	}
}
