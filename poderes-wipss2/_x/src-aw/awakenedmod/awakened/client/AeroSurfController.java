package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.aero.GaleControl;
import com.awakenedmod.awakened.content.aero.GaleWindsAbility;
import com.awakenedmod.awakened.core.net.ActivateAbilityPayload;
import com.awakenedmod.awakened.core.net.AeroSurfPayload;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class AeroSurfController {
   private static final int TAP_TICKS = 4;
   private static final int LAND_TICKS = 4;
   private static final int SERVER_ECHO_TICKS = 10;
   private static AeroSurfController.State state = AeroSurfController.State.IDLE;
   private static int pressTicks;
   private static boolean dashEligible;
   private static int groundedTicks;
   private static int surfTicks;

   private AeroSurfController() {
   }

   public static boolean ridingLocal() {
      return state == AeroSurfController.State.SURFING || AeroClientState.forcedGliding();
   }

   @SubscribeEvent
   public static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null) {
         AeroClientState.reset();
         reset();
      } else if (!player.isAlive()) {
         reset();
      } else {
         ClientPowerState st = ClientPowerState.get();
         boolean aero = st.awakened() && "aerokinetic".equals(st.powerId());
         int galeSlot = aero ? galeSlot(st) : -1;
         boolean galeActive = galeSlot >= 0 && galeSlot == AbilityKeyInput.drivingSlot();
         boolean down = AbilityKeyInput.heldFor(galeSlot);
         if (aero && state != AeroSurfController.State.SURFING && AeroClientState.forcedGliding() && !displaced(player)) {
            drive(player);
         }

         if (!galeActive) {
            if (state == AeroSurfController.State.SURFING) {
               send(new AeroSurfPayload(1));
            }

            reset();
         } else {
            switch (state) {
               case IDLE:
                  if (AbilityKeyInput.pressedFor(galeSlot)) {
                     onPress(player, st, galeSlot);
                  }
                  break;
               case PRESSED:
                  tickPressed(player, st, down);
                  break;
               case SURFING:
                  tickSurfing(player, down);
            }
         }
      }
   }

   private static void onPress(LocalPlayer player, ClientPowerState st, int galeSlot) {
      boolean flying = AeroClientState.flying();
      boolean airborne = !player.onGround();
      boolean canSurf = !flying && st.masteryLevel() >= 5;
      if (flying || !airborne || !canSurf) {
         send(new ActivateAbilityPayload(galeSlot));
      }

      if (canSurf) {
         state = AeroSurfController.State.PRESSED;
         pressTicks = 0;
         dashEligible = airborne;
      }
   }

   private static void tickPressed(LocalPlayer player, ClientPowerState st, boolean down) {
      pressTicks++;
      if (!down) {
         if (dashEligible && pressTicks <= 4 && !player.onGround() && !AeroClientState.flying()) {
            dash(player, st);
         }

         state = AeroSurfController.State.IDLE;
      } else if (AeroClientState.flying()) {
         state = AeroSurfController.State.IDLE;
      } else {
         if (pressTicks > 4 && !player.onGround() && player.getDeltaMovement().y <= 0.0) {
            state = AeroSurfController.State.SURFING;
            surfTicks = 0;
            groundedTicks = 0;
            send(new AeroSurfPayload(0));
         }
      }
   }

   private static void tickSurfing(LocalPlayer player, boolean down) {
      surfTicks++;
      groundedTicks = player.onGround() ? groundedTicks + 1 : 0;
      boolean landed = groundedTicks >= 4;
      boolean serverEnded = surfTicks > 10 && !AeroClientState.gliding();
      if (down && !landed && !serverEnded && !displaced(player)) {
         drive(player);
      } else {
         if (!serverEnded) {
            send(new AeroSurfPayload(1));
         }

         state = AeroSurfController.State.IDLE;
      }
   }

   private static boolean displaced(LocalPlayer player) {
      return AeroClientState.flying() || player.isPassenger() || player.isInWater() || player.getAbilities().flying;
   }

   private static void dash(LocalPlayer player, ClientPowerState st) {
      boolean affordable = st.infiniteStamina() || st.stamina() >= GaleControl.dashCost();
      if (affordable) {
         Vec3 look = player.getLookAngle();
         player.setDeltaMovement(player.getDeltaMovement().add(look.scale(1.7)));
      }

      send(new AeroSurfPayload(2));
   }

   private static void drive(LocalPlayer player) {
      float speedMult = 1.0F + ClientPowerState.get().abilityPower();
      player.setDeltaMovement(GaleControl.surfVelocity(player.getDeltaMovement(), player.getLookAngle(), speedMult));
   }

   private static int galeSlot(ClientPowerState st) {
      Power power = PowerRegistry.get(st.powerId());
      if (power == null) {
         return -1;
      } else {
         for (int slot = 0; slot < power.abilityCount(); slot++) {
            if (power.ability(slot) instanceof GaleWindsAbility) {
               return slot;
            }
         }

         return -1;
      }
   }

   private static void reset() {
      state = AeroSurfController.State.IDLE;
      pressTicks = 0;
      dashEligible = false;
      groundedTicks = 0;
      surfTicks = 0;
   }

   private static void send(CustomPacketPayload payload) {
      PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
   }

   private static enum State {
      IDLE,
      PRESSED,
      SURFING;
   }
}
