package com.awakenedmod.awakened.client.graviton.move;

import com.awakenedmod.awakened.client.AbilityKeyInput;
import com.awakenedmod.awakened.client.AwakenedAnimations;
import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.client.OwnAbilityKey;
import com.awakenedmod.awakened.content.graviton.GravityWellAbility;
import com.awakenedmod.awakened.content.graviton.GravityWellMotion;
import com.awakenedmod.awakened.content.graviton.GravityWellPlanet;
import com.awakenedmod.awakened.content.graviton.GravityWellVisual;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.PulledBody;
import com.awakenedmod.awakened.core.net.AbilityActionPayload;
import com.awakenedmod.awakened.core.net.ActivateAbilityPayload;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class GravityWellClient {
   public static final String ANIM_DIVE = "gravity_well_dive";
   public static final String ANIM_HOVER = "gravity_well_hover";
   private static final int PLANET_WAIT = 8;
   private static final int CONFIRM_TICKS = 10;
   private static final int LOCAL_AFTERGLOW = 40;
   private static final double FOV_FROM = 0.4;
   private static final double FOV_FULL = 2.4;
   private static final float FOV_MAX = 0.12F;
   private static Vec3 well;
   private static int age;
   private static int chain;
   private static boolean arrived;
   private static GravityWellVisual visual = GravityWellVisual.NONE;
   private static int sinceEnd = Integer.MAX_VALUE;
   private static GravityWellMotion.Target aim;
   private static float fov;
   private static int awaitPlanet;
   private static int expectOpens;

   private GravityWellClient() {
   }

   public static void declareClips() {
      AwakenedAnimations.holdLoop(AwId.of("gravity_well_dive"), p -> {
         GravityWellVisual v = GravityWellVisual.of(p);
         return v.live() && !v.arrived() && !p.onGround();
      });
      AwakenedAnimations.holdLoop(AwId.of("gravity_well_hover"), p -> {
         GravityWellVisual v = GravityWellVisual.of(p);
         return v.live() && v.arrived() && !p.onGround();
      });
      AwakenedAnimations.vanillaFirstPerson(AwId.of("gravity_well_dive"), p -> false);
      AwakenedAnimations.vanillaFirstPerson(AwId.of("gravity_well_hover"), p -> false);
      AwakenedAnimations.vanillaFirstPerson(AwId.of("gravity_well_land"), p -> false);
   }

   public static GravityWellVisual visualFor(Player p) {
      boolean local = p == Minecraft.getInstance().player && (well != null || sinceEnd < 40);
      return local ? visual : GravityWellVisual.of(p);
   }

   public static GravityWellMotion.Target aim() {
      return aim;
   }

   @SubscribeEvent
   public static void onTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      int slot = p == null ? -1 : OwnAbilityKey.slot("gravity_well");
      aim = null;
      if (slot < 0) {
         if (well != null) {
            end(1);
         }

         sinceEnd = Integer.MAX_VALUE;
         fov = 0.0F;
      } else {
         if (sinceEnd < Integer.MAX_VALUE) {
            sinceEnd++;
         }

         learnPlanet(p);
         if (AbilityKeyInput.pressedFor(slot)) {
            press(p, slot);
         }

         tickFov(p);
         if (well != null) {
            age++;
            boolean sneak = mc.options.keyShift.isDown() && mc.screen == null;
            boolean blocked = PulledBody.mustLetGo(p);
            if (!sneak && !blocked && age <= GravityWellMotion.LIFE.getI() && (!arrived || !p.onGround())) {
               if (age > 10 && !GravityWellMotion.serverLive(p)) {
                  end(1);
               } else {
                  if (!arrived && GravityWellMotion.arrived(p.position(), well)) {
                     arrived = true;
                     visual = visual.arrived(true);
                  }

                  p.setDeltaMovement(GravityWellMotion.step(p.position(), p.getDeltaMovement(), well, p.onGround(), p.isInWater(), arrived, chain));
                  p.resetFallDistance();
                  if (!arrived) {
                     aim = GravityWellMotion.target(p).orElse(null);
                  }
               }
            } else {
               close(p, slot);
            }
         }
      }
   }

   private static void press(LocalPlayer p, int slot) {
      if (well != null && arrived) {
         close(p, slot);
      } else {
         ClientPowerState st = ClientPowerState.get();
         boolean affordable = st.infiniteStamina() || st.stamina() >= StaminaSystem.effectiveCost(7.0F, GravityWellAbility.class.getSimpleName());
         Optional<GravityWellMotion.Target> target = GravityWellMotion.target(p);
         send(new ActivateAbilityPayload(slot));
         if (affordable && !target.isEmpty() && !PulledBody.carried(p)) {
            chain = well != null ? chain + 1 : 0;
            well = target.get().point();
            visual = visual.opened(target.get(), chain, p.position(), -1);
            expectOpens = GravityWellVisual.bump(awaitPlanet > 0 ? expectOpens : GravityWellVisual.of(p).opens());
            awaitPlanet = 8;
            age = 0;
            arrived = false;
            if (p.onGround()) {
               Vec3 v = p.getDeltaMovement();
               p.setDeltaMovement(v.x, Math.max(v.y, GravityWellMotion.HOP.get()), v.z);
            }
         }
      }
   }

   private static void learnPlanet(LocalPlayer p) {
      if (awaitPlanet > 0) {
         GravityWellVisual synced = GravityWellVisual.of(p);
         if (synced.opens() == expectOpens && GravityWellPlanet.known(synced.planet())) {
            visual = visual.withPlanet(synced.planet());
            awaitPlanet = 0;
         } else if (--awaitPlanet == 0) {
            visual = visual.withPlanet(GravityWellPlanet.roll(p.getRandom(), -1));
         }
      }
   }

   private static void close(LocalPlayer p, int slot) {
      end(GravityWellMotion.arrived(p.position(), well) ? 0 : 1);
      send(new AbilityActionPayload(slot, 110));
   }

   private static void end(int kind) {
      well = null;
      visual = visual.ended(kind);
      sinceEnd = 0;
   }

   private static void tickFov(LocalPlayer p) {
      float target = 0.0F;
      if (well != null && !arrived) {
         double speed = p.getDeltaMovement().length();
         target = 0.12F * (float)Mth.clamp((speed - 0.4) / 2.0, 0.0, 1.0);
      }

      fov = fov + (target - fov) * 0.25F;
   }

   @SubscribeEvent
   public static void onComputeFov(ComputeFovModifierEvent event) {
      if (fov > 0.001F && event.getPlayer() == Minecraft.getInstance().player) {
         event.setNewFovModifier(event.getNewFovModifier() * (1.0F + fov));
      }
   }

   private static void send(CustomPacketPayload payload) {
      PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
   }
}
