package vectorwing.blockbox;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import vectorwing.blockbox.common.event.CommonEvents;
import vectorwing.blockbox.common.registry.*;

// Port Forge 1.20.1: antes recibia IEventBus/ModContainer por constructor (NeoForge);
// en Forge 1.20.1 se obtienen de FMLJavaModLoadingContext. Se quito el config
// "addItemsToVanillaTabs": el port no inserta items en las pestanas vanilla
// (ver LEEME-port.txt), asi que esa opcion no tenia nada que controlar.
@Mod(BlockBox.MODID)
public class BlockBox
{
	public static final String MODID = "blockbox";
	public static final Logger LOGGER = LogUtils.getLogger();

	public BlockBox() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		ModBlocks.BLOCKS.register(modEventBus);
		ModItems.ITEMS.register(modEventBus);
		ModEntityTypes.ENTITY_TYPES.register(modEventBus);
		ModSounds.SOUNDS.register(modEventBus);
		ModParticleTypes.PARTICLE_TYPES.register(modEventBus);
		ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

		modEventBus.addListener(CommonEvents::commonSetup);
	}
}
