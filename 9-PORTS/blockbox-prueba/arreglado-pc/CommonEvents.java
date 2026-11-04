package vectorwing.blockbox.common.event;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import vectorwing.blockbox.BlockBox;
import vectorwing.blockbox.common.registry.ModBlocks;
import vectorwing.blockbox.common.tag.ModTags;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

/**
 * Port Forge 1.20.1: el original (NeoForge) agrega items a las pestanas vanilla con
 * BuildCreativeModeTabContentsEvent (no existe en 1.20.1) -> no se porto, los items del
 * mod se ven solo en su propia pestana (ver LEEME-port.txt). Los "data maps" de NeoForge
 * para waxables/oxidizables/furnace_fuels (data/neoforge/data_maps/...) tampoco existen en
 * 1.20.1: se registran aqui a mano en commonSetup() y con FurnaceFuelBurnTimeEvent.
 */
public class CommonEvents
{
	public static void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			BiMap<Block, Block> waxables = mutable(HoneycombItem.WAXABLES);
			waxables.put(ModBlocks.COPPER_BARS.get(), ModBlocks.WAXED_COPPER_BARS.get());
			waxables.put(ModBlocks.COPPER_PILLAR.get(), ModBlocks.WAXED_COPPER_PILLAR.get());
			waxables.put(ModBlocks.EXPOSED_COPPER_BARS.get(), ModBlocks.WAXED_EXPOSED_COPPER_BARS.get());
			waxables.put(ModBlocks.EXPOSED_COPPER_PILLAR.get(), ModBlocks.WAXED_EXPOSED_COPPER_PILLAR.get());
			waxables.put(ModBlocks.OXIDIZED_COPPER_BARS.get(), ModBlocks.WAXED_OXIDIZED_COPPER_BARS.get());
			waxables.put(ModBlocks.OXIDIZED_COPPER_PILLAR.get(), ModBlocks.WAXED_OXIDIZED_COPPER_PILLAR.get());
			waxables.put(ModBlocks.WEATHERED_COPPER_BARS.get(), ModBlocks.WAXED_WEATHERED_COPPER_BARS.get());
			waxables.put(ModBlocks.WEATHERED_COPPER_PILLAR.get(), ModBlocks.WAXED_WEATHERED_COPPER_PILLAR.get());

			BiMap<Block, Block> nextOxidation = mutable(WeatheringCopper.NEXT_BY_BLOCK);
			nextOxidation.put(ModBlocks.COPPER_BARS.get(), ModBlocks.EXPOSED_COPPER_BARS.get());
			nextOxidation.put(ModBlocks.COPPER_PILLAR.get(), ModBlocks.EXPOSED_COPPER_PILLAR.get());
			nextOxidation.put(ModBlocks.EXPOSED_COPPER_BARS.get(), ModBlocks.WEATHERED_COPPER_BARS.get());
			nextOxidation.put(ModBlocks.EXPOSED_COPPER_PILLAR.get(), ModBlocks.WEATHERED_COPPER_PILLAR.get());
			nextOxidation.put(ModBlocks.WEATHERED_COPPER_BARS.get(), ModBlocks.OXIDIZED_COPPER_BARS.get());
			nextOxidation.put(ModBlocks.WEATHERED_COPPER_PILLAR.get(), ModBlocks.OXIDIZED_COPPER_PILLAR.get());
		});
	}

	/**
	 * En 1.20.1 WAXABLES y NEXT_BY_BLOCK son Suppliers.memoize(...) de un ImmutableBiMap: put()
	 * lanza UnsupportedOperationException. Se cambia el valor guardado en el supplier por una
	 * copia mutable (los mapas inversos de vanilla se calculan despues sobre esta misma copia).
	 */
	private static BiMap<Block, Block> mutable(Supplier<BiMap<Block, Block>> supplier) {
		BiMap<Block, Block> current = supplier.get();
		if (!(current instanceof ImmutableBiMap)) return current;
		BiMap<Block, Block> copy = HashBiMap.create(current);
		for (Class<?> c = supplier.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers())) continue;
				try {
					f.setAccessible(true);
					if (f.get(supplier) == current) {
						f.set(supplier, copy);
						return copy;
					}
				} catch (ReflectiveOperationException | RuntimeException ignored) {}
			}
		}
		BlockBox.LOGGER.warn("No se pudo hacer mutable un mapa de cobre de vanilla; el encerado/oxidacion de Block Box no funcionara");
		return copy;
	}

	@Mod.EventBusSubscriber(modid = BlockBox.MODID)
	public static class ForgeBusEvents
	{
		@SubscribeEvent
		public static void onFurnaceFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
			if (event.getItemStack().is(ModTags.PALISADE_ITEMS) || event.getItemStack().is(ModTags.SPIKED_PALISADE_ITEMS)) {
				event.setBurnTime(150);
			} else if (event.getItemStack().is(ModTags.WOODEN_SEAT_ITEMS)) {
				event.setBurnTime(200);
			}
		}
	}
}
