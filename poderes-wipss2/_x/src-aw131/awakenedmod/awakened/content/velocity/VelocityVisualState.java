package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class VelocityVisualState {
   public static final PassiveToggle TRAIL = new PassiveToggle(
      "velocity_trail", "awakened.screen.setting_velocity_trail", "awakened.screen.setting_velocity_trail_desc", true
   );
   private static final Map<UUID, VelocityVisualState.Flags> FLAGS = new ConcurrentHashMap<>();

   private VelocityVisualState() {
   }

   public static VelocityVisual of(Entity entity) {
      return (VelocityVisual)entity.getData(AwakenedAttachments.VELOCITY_VISUAL);
   }

   public static void setPhasing(ServerPlayer player, boolean on) {
      set(player, on, (f, v) -> f.phasing = v);
   }

   public static void setTimeless(ServerPlayer player, boolean on) {
      set(player, on, (f, v) -> {
         f.timeless = v;
         if (!v) {
            f.fieldRadiusHalves = 0;
            f.fieldClosing = false;
         }
      });
   }

   public static void setTimelessField(ServerPlayer player, double blocks, boolean closing) {
      VelocityVisualState.Flags f = FLAGS.get(player.getUUID());
      if (f != null) {
         f.fieldRadiusHalves = Math.max(0, Math.min(127, (int)Math.round(blocks * 2.0)));
         f.fieldClosing = closing;
         sync(player);
      }
   }

   public static void setTunnel(ServerPlayer player, boolean on) {
      set(player, on, (f, v) -> f.tunnel = v);
   }

   public static void setDiving(ServerPlayer player, boolean on) {
      set(player, on, (f, v) -> f.diving = v);
   }

   public static void setTransit(ServerPlayer player, boolean on) {
      set(player, on, (f, v) -> f.transit = v);
   }

   public static void setCharge(ServerPlayer player, int percent) {
      int p = Math.max(0, Math.min(100, percent));
      set(player, p > 0, (f, v) -> f.charge = v ? p : 0);
   }

   private static void set(ServerPlayer player, boolean on, BiConsumer<VelocityVisualState.Flags, Boolean> flag) {
      UUID id = player.getUUID();
      VelocityVisualState.Flags f = on ? FLAGS.computeIfAbsent(id, k -> new VelocityVisualState.Flags()) : FLAGS.get(id);
      if (f != null) {
         flag.accept(f, on);
         if (!f.any()) {
            FLAGS.remove(id);
         }
      }

      sync(player);
   }

   public static void sync(ServerPlayer player) {
      if (player != null) {
         PowerData data = PowerService.data(player);
         VelocityVisualState.Flags f = FLAGS.get(player.getUUID());
         boolean overdrive = OverdriveAbility.isOn(data);
         int gear = Gearbox.effectiveGear(player, data);
         apply(
            player,
            new VelocityVisual(
               overdrive,
               gear,
               Momentum.chosenGear(data),
               Momentum.earnedGear(data),
               Momentum.unlockedGear(data),
               player.isSprinting(),
               f != null && f.phasing,
               f != null && f.timeless,
               f != null && f.tunnel,
               f != null && f.diving,
               f != null && f.transit,
               Gearbox.forcing(player, data),
               Gearbox.billing(player, data),
               OverdriveControl.lavaCoyote(player),
               f == null ? 0 : f.charge,
               speedMult(player, gear, overdrive),
               VelocityColor.of(data).getId(),
               f == null ? 0 : f.fieldRadiusHalves,
               f != null && f.fieldClosing,
               TRAIL.enabled(data)
            )
         );
      }
   }

   private static float speedMult(ServerPlayer player, int gear, boolean overdrive) {
      return !overdrive ? 1.0F : (float)Math.round(OverdriveControl.sprintMultiplier(gear, player) * 100.0) / 100.0F;
   }

   public static void clear(ServerPlayer player) {
      if (player != null) {
         LaneTransit.deliver(player);
         FLAGS.remove(player.getUUID());
         apply(player, VelocityVisual.NONE);
      }
   }

   private static void apply(ServerPlayer player, VelocityVisual next) {
      if (!of(player).equals(next)) {
         player.setData(AwakenedAttachments.VELOCITY_VISUAL, next);
      }
   }

   private static final class Flags {
      boolean phasing;
      boolean timeless;
      boolean tunnel;
      boolean diving;
      boolean transit;
      int charge;
      int fieldRadiusHalves;
      boolean fieldClosing;

      boolean any() {
         return this.phasing || this.timeless || this.tunnel || this.diving || this.transit || this.charge > 0;
      }
   }
}
