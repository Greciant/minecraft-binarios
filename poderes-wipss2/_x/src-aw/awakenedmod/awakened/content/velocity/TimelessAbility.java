package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.awakenedmod.awakened.core.net.VelocityScreenFxPayload;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TimelessAbility extends VelocityHoldAbility {
   public static final double RADIUS = 24.0;
   public static final double WIDE_RADIUS = 32.0;
   private static final int RING_TICKS = 8;
   private static final double MIN_RING_RADIUS = 1.0;
   private static final int GEAR = 3;
   private static final float DRAIN_PER_SECOND = 4.0F;
   private static final float STANDING_DRAIN_PER_SECOND = 6.0F;
   private static final int CAP_TICKS = 12000;
   private static final int MASTERY_MIN_TICKS = 40;
   private static final int MASTERY_BEAT = 20;

   public TimelessAbility() {
      super("timeless", 7, 12000, VelocityHoldAbility.Input.TOGGLE);
   }

   @Override
   protected VelocityHoldAbility.Hold newHold() {
      return new TimelessAbility.Field();
   }

   @Override
   protected float drainPerSecond() {
      return 4.0F;
   }

   @Override
   protected float drainPerSecond(Player player) {
      return player != null && player.isSprinting() ? 4.0F : 6.0F;
   }

   @Override
   protected int requiredGear() {
      return 0;
   }

   @Override
   protected boolean holdsMomentum() {
      return true;
   }

   @Override
   protected boolean begin(ServerPlayer player, VelocityHoldAbility.Hold hold) {
      PowerData data = PowerService.data(player);
      if (!OverdriveAbility.isOn(data)) {
         return this.fail(player, "awakened.velocity.timeless_needs_overdrive", new Object[0]);
      } else if (!canHold(player)) {
         return this.fail(player, "awakened.velocity.timeless_needs_gear", new Object[]{3});
      } else {
         TimelessAbility.Field field = (TimelessAbility.Field)hold;
         field.gear = gear(player);
         VelocityVisualState.setTimeless(player, true);
         PacketDistributor.sendToPlayer(player, VelocityScreenFxPayload.timeless(true), new CustomPacketPayload[0]);
         VelocityFx.timelessOpen(player, radius(field.gear));
         return true;
      }
   }

   @Override
   protected boolean close(ServerPlayer player) {
      this.startClosing(player);
      return true;
   }

   @Override
   protected void tick(ServerPlayer player, VelocityHoldAbility.Hold hold) {
      TimelessAbility.Field field = (TimelessAbility.Field)hold;
      if (field.closing < 0 && !canHold(player)) {
         this.startClosing(player);
      }

      if (field.closing >= 0 && ++field.closing > 8) {
         this.stop(player, VelocityHoldAbility.End.RELEASED);
      } else {
         if (field.closing < 0 && field.opened < 8) {
            field.opened++;
         }

         if (field.closing < 0) {
            field.gear = gear(player);
         }

         double radius = radius(field.gear) * edge(field);
         VelocityVisualState.setTimelessField(player, radius, field.closing >= 0);
         field.slow.sweep(player, radius, divisor(field.gear));
         if (hold.ticks >= 40 && hold.ticks % 20 == 0) {
            MasterySystem.award(player, MasteryReason.HIT, field.slow.caught());
         }
      }
   }

   @Override
   protected void end(ServerPlayer player, VelocityHoldAbility.Hold hold, VelocityHoldAbility.End end) {
      TimelessAbility.Field field = (TimelessAbility.Field)hold;
      field.slow.releaseAll();
      VelocityVisualState.setTimeless(player, false);
      PacketDistributor.sendToPlayer(player, VelocityScreenFxPayload.timeless(false), new CustomPacketPayload[0]);
      if (end != VelocityHoldAbility.End.TORN_DOWN) {
         exitRing(player, field);
      }
   }

   @Override
   public boolean occupiesHands(ServerPlayer player) {
      return false;
   }

   @Override
   protected boolean heldByViewer(VelocityVisual visual) {
      return visual.timeless();
   }

   private void startClosing(ServerPlayer player) {
      if (this.hold(player) instanceof TimelessAbility.Field field && field.closing < 0) {
         field.closing = 0;
         exitRing(player, field);
         return;
      }
   }

   private static void exitRing(ServerPlayer player, TimelessAbility.Field field) {
      if (!field.exitRing) {
         field.exitRing = true;
         double radius = radius(field.gear) * edge(field);
         if (radius >= 1.0) {
            VelocityFx.timelessClose(player, radius);
         }
      }
   }

   private static boolean canHold(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      return OverdriveAbility.isOn(data) && Gearbox.effectiveGear(player, data) >= 3;
   }

   private static int gear(ServerPlayer player) {
      return Gearbox.effectiveGear(player, PowerService.data(player));
   }

   public static int divisor(int gear) {
      return gear >= 4 ? 5 : 3;
   }

   private static double radius(int gear) {
      return gear >= 5 ? 32.0 : 24.0;
   }

   private static double edge(TimelessAbility.Field field) {
      double open = (double)VelocityFx.ringFront((float)field.opened / 8.0F);
      return field.closing >= 0 ? open * (double)VelocityFx.ringFront(1.0F - (float)field.closing / 8.0F) : open;
   }

   private static final class Field extends VelocityHoldAbility.Hold {
      private final SlowField slow = new SlowField();
      private int opened;
      private int closing = -1;
      private int gear;
      private boolean exitRing;
   }
}
