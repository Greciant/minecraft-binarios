package vectorwing.blockbox.client.event;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vectorwing.blockbox.BlockBox;
import vectorwing.blockbox.client.particle.SparkleParticle;
import vectorwing.blockbox.common.entity.SeatEntity;
import vectorwing.blockbox.common.registry.ModEntityTypes;
import vectorwing.blockbox.common.registry.ModParticleTypes;

@Mod.EventBusSubscriber(modid = BlockBox.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetupEvents
{
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void registerParticles(RegisterParticleProvidersEvent event) {
		Minecraft.getInstance().particleEngine.register(ModParticleTypes.SPARKLE.get(), SparkleParticle.Provider::new);
	}

	@SubscribeEvent
	public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ModEntityTypes.SEAT.get(), SeatEntity.Renderer::new);
	}
}
