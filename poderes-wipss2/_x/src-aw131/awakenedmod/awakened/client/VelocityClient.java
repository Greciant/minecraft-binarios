package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.render.VelocityFirstPersonCharge;
import com.awakenedmod.awakened.client.render.VelocityTunnelTubeRenderer;
import com.awakenedmod.awakened.content.velocity.Downstep;
import com.awakenedmod.awakened.content.velocity.SpeedTunnelAbility;
import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.PhaseState;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class VelocityClient {
   public static final String RUN_CLIP = "velocity_run";
   public static final String BOLT_HOLD_CLIP = "velocity_bolt_hold";
   private static VelocityWindSound wind;

   private VelocityClient() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(VelocityClient::onClientTick);
      PhaseState.registerClientSource(p -> VelocityVisualState.of(p).phasing() || SpeedTunnelAbility.running(p), p -> VelocityPalette.of(p).fog());
      PhaseState.registerClientFullSource(p -> VelocityVisualState.of(p).tunnelling());
      AwakenedAnimations.holdLoop(AwId.of("velocity_run"), p -> VelocityVisualState.of(p).running() || riding(p));
      AwakenedAnimations.holdLoop(AwId.of("velocity_bolt_hold"), p -> VelocityVisualState.of(p).charging());
      AwakenedAnimations.vanillaFirstPerson(AwId.of("velocity_run"), VelocityClient::armsOwnFirstPerson);
      AwakenedAnimations.addPoseModifier(VelocityRunPose::bone);
      AwakenedAnimations.addPoseModifier(VelocityLaneRunPose::bone);
      VelocityFirstPersonCharge.register(gameBus);
      VelocitySoundSag.register(gameBus);
      TimelessFieldClient.register(gameBus);
      TimelessColdBodies.register(gameBus);
      TimelessSound.register(gameBus);
      VelocityTunnelClient.register(gameBus);
      VelocityLaneTransitClient.register(gameBus);
      VelocityTunnelTubeRenderer.register(gameBus);
   }

   private static boolean armsOwnFirstPerson(Player player) {
      VelocityVisual vis = VelocityVisualState.of(player);
      return vis.tunnelling() || vis.charging() || riding(player);
   }

   private static boolean riding(Player player) {
      return VelocityVisualState.of(player).transit() || player == Minecraft.getInstance().player && VelocityLaneTransitClient.active();
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer local = mc.player;
      if (local != null) {
         Downstep.stepDown(local);
      }

      VelocityRunPose.tick(mc);
      VelocityHud.tickChime(local);
      if (wind != null && wind.isStopped()) {
         wind = null;
      }

      boolean running = local != null && VelocityVisualState.of(local).running();
      if (running && wind == null) {
         wind = new VelocityWindSound(local);
         mc.getSoundManager().play(wind);
      }

      VelocityStateSounds.tick(mc, local);
   }
}
