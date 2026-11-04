package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.SpiderForceStopPayload;
import com.awakenedmod.awakened.core.net.SpiderStatePayload;
import com.awakenedmod.awakened.core.net.SpiderSwingPayload;
import com.awakenedmod.awakened.core.net.SpiderTrickPayload;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WebSwingAbility extends Ability {
   public static final String ID = "web_swing";
   private static final float COST = 4.0F;
   private static final float SWING_DRAIN = 0.3F;
   private static final float HANG_DRAIN = 0.04F;
   private static final int FALL_GRACE = 240;
   private static final float TRICK_REFUND = 1.0F;
   private static final double TRICK_MASTERY = 0.4;
   private static final long TRICK_MASTERY_COOLDOWN = 200L;
   private final Map<UUID, WebSwingAbility.SwingState> swings = this.activeState();
   private final Map<UUID, Integer> fallGrace = this.playerState();
   private final SwingTricks tricks = new SwingTricks(this.playerState());
   private final Map<UUID, Long> trickMasteryAt = this.playerState();

   public WebSwingAbility() {
      super("web_swing", 4.0F, 0);
   }

   @Override
   public boolean clientDriven() {
      return true;
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      return false;
   }

   public static void handleClientSwing(ServerPlayer player, SpiderSwingPayload payload) {
      WebSwingAbility swing = SpiderClientFire.resolve(player, payload.slot(), WebSwingAbility.class);
      if (swing != null) {
         swing.handle(player, payload);
      }
   }

   public static void handleClientTrick(ServerPlayer player, SpiderTrickPayload payload) {
      WebSwingAbility swing = SpiderClientFire.resolve(player, payload.slot(), WebSwingAbility.class);
      SpiderTrick trick = SpiderTrick.byId(payload.trick());
      if (swing != null && trick != null) {
         swing.tricks.start(player, trick);
      }
   }

   private void handle(ServerPlayer player, SpiderSwingPayload payload) {
      UUID id = player.getUUID();
      switch (payload.action()) {
         case 0:
         case 5:
            if (this.swings.containsKey(id)) {
               return;
            }

            if (WebWingsAbility.isGliding(player)) {
               return;
            }

            PowerData data = PowerService.data(player);
            float cost = this.billed(4.0F);
            if (!StaminaSystem.canAfford(data, cost)) {
               PacketDistributor.sendToPlayer(player, SpiderForceStopPayload.INSTANCE, new CustomPacketPayload[0]);
               this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
               return;
            }

            if (!data.infiniteStamina()) {
               data.setStamina(data.stamina() - cost);
            }

            data.setLastSpendTime(player.serverLevel().getGameTime());
            this.payTrickCombo(player, data, this.tricks.cashIn(player));
            boolean startHung = payload.action() == 5;
            boolean fromDive = SpiderVisualState.get(player).diving();
            WebSwingAbility.SwingState statexx = new WebSwingAbility.SwingState(payload.left());
            statexx.hanging = startHung;
            this.swings.put(id, statexx);
            MasterySystem.awardUse(player);
            PowerService.playAnimation(player, startHung ? "web_hang" : gripClip(payload.left(), fromDive));
            PacketDistributor.sendToPlayer(
               player, new SpiderStatePayload(true, startHung, payload.ax(), payload.ay(), payload.az(), payload.left()), new CustomPacketPayload[0]
            );
            SpiderVisualState.setSwing(player, startHung, payload.left(), new Vec3(payload.ax(), payload.ay(), payload.az()));
            PowerService.sync(player);
            break;
         case 1:
            WebSwingAbility.SwingState statex = this.swings.get(id);
            if (statex != null) {
               statex.hanging = true;
               SpiderVisualState.setHanging(player, true);
               PowerService.stopAnimation(player);
               PowerService.playAnimation(player, "web_hang");
            }
            break;
         case 2:
            WebSwingAbility.SwingState state = this.swings.get(id);
            if (state != null) {
               state.hanging = false;
               SpiderVisualState.setHanging(player, false);
               PowerService.stopAnimation(player);
               PowerService.playAnimation(player, gripClip(state.left, false));
            }
            break;
         case 3:
         case 4:
            if (this.swings.remove(id) != null) {
               this.fallGrace.put(id, 240);
               if (payload.action() == 3) {
                  this.tricks.begin(player);
               }

               PowerService.stopAnimation(player);
               PowerService.playAnimation(player, payload.action() == 4 ? "web_dive" : "web_swing_release");
               PacketDistributor.sendToPlayer(player, SpiderStatePayload.IDLE, new CustomPacketPayload[0]);
               SpiderVisualState.clearTether(player);
               PowerService.sync(player);
            }
         case 6:
         case 7:
         default:
            break;
         case 8:
            switch (SpiderFreefall.start(player)) {
               case STARTED:
                  this.tricks.cancelCurrent(player);
                  return;
               case TOO_LOW:
                  this.fail(player, "awakened.spider.dive_too_low", new Object[0]);
                  return;
               default:
                  return;
            }
         case 9:
            SpiderFreefall.clear(player);
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.tickFallGrace(player);
      this.tricks.tick(player);
      WebSwingAbility.SwingState state = this.swings.get(player.getUUID());
      if (state != null) {
         player.resetFallDistance();
         PowerData data = PowerService.data(player);
         long gameTime = player.serverLevel().getGameTime();
         float drain = this.billed(state.hanging ? 0.04F : 0.3F);
         if (!data.infiniteStamina()) {
            if (data.stamina() < drain) {
               this.endServerSide(player);
               return;
            }

            StaminaSystem.drainSustained(data, gameTime, drain);
         }

         if (player.tickCount % 3 == 0) {
            PowerService.sync(player);
         }
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.swings.containsKey(player.getUUID())) {
         PowerService.stopAnimation(player);
         PacketDistributor.sendToPlayer(player, SpiderForceStopPayload.INSTANCE, new CustomPacketPayload[0]);
         PacketDistributor.sendToPlayer(player, SpiderStatePayload.IDLE, new CustomPacketPayload[0]);
      }

      SpiderVisualState.clearTether(player);
      SpiderVisualState.setGliding(player, false);
      SpiderFreefall.clear(player);
      SpiderVisualState.setGauntlets(player, SpiderTypeSync.hasGauntlets(player));
      super.clearPlayer(player);
   }

   private void endServerSide(ServerPlayer player) {
      if (this.swings.remove(player.getUUID()) != null) {
         this.fallGrace.put(player.getUUID(), 240);
         SpiderFx.twang(player.serverLevel(), player.getEyePosition());
         PowerService.stopAnimation(player);
         PacketDistributor.sendToPlayer(player, SpiderForceStopPayload.INSTANCE, new CustomPacketPayload[0]);
         PacketDistributor.sendToPlayer(player, SpiderStatePayload.IDLE, new CustomPacketPayload[0]);
         SpiderVisualState.clearTether(player);
         PowerService.sync(player);
      }
   }

   private void tickFallGrace(ServerPlayer player) {
      Integer left = this.fallGrace.get(player.getUUID());
      if (left != null) {
         if (!player.onGround() && left > 0) {
            player.resetFallDistance();
            this.fallGrace.put(player.getUUID(), left - 1);
         } else {
            this.fallGrace.remove(player.getUUID());
         }
      }
   }

   private void payTrickCombo(ServerPlayer player, PowerData data, int points) {
      if (points > 0) {
         if (!data.infiniteStamina()) {
            data.setStamina(Math.min(StaminaSystem.max(player), data.stamina() + 1.0F * (float)points));
         }

         long now = player.serverLevel().getGameTime();
         Long last = this.trickMasteryAt.get(player.getUUID());
         if (points >= 2 && (last == null || now - last >= 200L)) {
            this.trickMasteryAt.put(player.getUUID(), now);
            MasterySystem.grant(player, 0.4 * (double)(points - 1));
         }
      }
   }

   public static String gripClip(boolean left) {
      return left ? "web_swing_left" : "web_swing_right";
   }

   public static String catchClip(boolean left) {
      return left ? "web_swing_catch_left" : "web_swing_catch_right";
   }

   private static String gripClip(boolean left, boolean fromDive) {
      return fromDive ? catchClip(left) : gripClip(left);
   }

   private static final class SwingState {
      final boolean left;
      boolean hanging;

      SwingState(boolean left) {
         this.left = left;
      }
   }
}
