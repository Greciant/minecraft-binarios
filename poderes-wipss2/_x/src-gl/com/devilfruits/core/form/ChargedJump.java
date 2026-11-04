package com.devilfruits.core.form;

import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.network.JumpInputPayload;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.registry.DFAttachments;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ChargedJump {
   public static final int CHARGE_TICKS = 30;
   public static final float MIN_CHARGE = 0.2F;
   private static final double MIN_HEIGHT = 3.0;
   private static final int COOLDOWN = 10;
   public static final int CHARGE = 0;
   public static final int LAUNCH = 1;
   public static final int LAND = 2;
   public static final int CANCEL = 3;
   private static final Map<UUID, ChargedJump.State> STATES = new HashMap<>();

   @Nullable
   private static FruitForm formOf(ServerPlayer player) {
      PlayerFruitData data = DFAttachments.data(player);
      FruitForm form = data.hasFruit() && !data.suppressed() ? data.form() : null;
      return form != null && form.chargedJumpHeight() > 0.0F && form.chargedJumpFx() != null ? form : null;
   }

   private static boolean standing(ServerPlayer player) {
      return player.onGround()
         && !player.isInWater()
         && !player.isInLava()
         && !player.isPassenger()
         && !player.isSleeping()
         && !player.isFallFlying()
         && !player.getAbilities().flying
         && !player.isSpectator();
   }

   public static void handle(ServerPlayer player, JumpInputPayload payload) {
      long now = player.level().getGameTime();
      ChargedJump.State st = STATES.computeIfAbsent(player.getUUID(), u -> new ChargedJump.State());
      switch (payload.action()) {
         case START:
            FruitForm form = formOf(player);
            if (form == null || st.start >= 0L || st.launched >= 0L || now < st.readyAt || !standing(player)) {
               return;
            }

            st.start = now;
            st.fx = form.chargedJumpFx();
            ServerFx.send(player, st.fx, player.position(), Vec3.ZERO, 30.0F, player.getRandom().nextLong(), 0);
            break;
         case RELEASE:
            release(player, st, now, payload.dx(), payload.dz());
            break;
         case CANCEL:
            cancel(player, st);
      }
   }

   private static void cancel(ServerPlayer player, ChargedJump.State st) {
      if (st.start >= 0L && st.fx != null) {
         ServerFx.send(player, st.fx, player.position(), Vec3.ZERO, 0.0F, 0L, 3);
      }

      st.start = -1L;
   }

   private static void release(ServerPlayer player, ChargedJump.State st, long now, float dx, float dz) {
      FruitForm form = formOf(player);
      if (st.start >= 0L) {
         float charge = Math.min(1.0F, (float)(now - st.start) / 30.0F);
         if (form != null && !(charge < 0.2F) && standing(player)) {
            st.start = -1L;
            double height = 3.0 + ((double)form.chargedJumpHeight() - 3.0) * (double)(charge - 0.2F) / 0.8F;
            double vy = takeoffSpeed(height, player.getGravity());
            double len = Math.sqrt((double)(dx * dx + dz * dz));
            Vec3 v = new Vec3(0.0, vy, 0.0);
            if (len > 0.1) {
               double speed = 0.6 + 2.5 * (double)charge;
               v = new Vec3((double)dx / len * speed, vy, (double)dz / len * speed);
            }

            Targeting.moveSelf(player, v);
            player.resetFallDistance();
            st.launched = now;
            st.peakY = player.getY();
            ServerFx.send(player, st.fx, player.position(), v, charge, player.getRandom().nextLong(), 1);
         } else {
            cancel(player, st);
         }
      }
   }

   public static void tick(ServerPlayer player) {
      ChargedJump.State st = STATES.get(player.getUUID());
      if (st != null) {
         long now = player.level().getGameTime();
         if (st.start >= 0L && (formOf(player) == null || !player.onGround() && now - st.start > 3L || now - st.start > 600L)) {
            cancel(player, st);
         }

         if (st.launched >= 0L) {
            st.peakY = Math.max(st.peakY, player.getY());
            boolean down = now - st.launched > 3L && (player.onGround() || player.isInWater() || player.isPassenger());
            if (down || now - st.launched > 200L || player.isSpectator()) {
               st.launched = -1L;
               st.readyAt = now + 10L;
               if (down && st.fx != null) {
                  ServerFx.send(
                     player, st.fx, player.position(), player.getDeltaMovement(), (float)(st.peakY - player.getY()), player.getRandom().nextLong(), 2
                  );
               }
            }
         }
      }
   }

   public static double takeoffSpeed(double height, double gravity) {
      double lo = 0.0;
      double hi = 5.0;

      for (int i = 0; i < 32; i++) {
         double mid = (lo + hi) * 0.5;
         if (peak(mid, gravity) < height) {
            lo = mid;
         } else {
            hi = mid;
         }
      }

      return hi;
   }

   public static double peak(double v, double gravity) {
      double y = 0.0;

      for (int i = 0; i < 400 && v > 0.0; i++) {
         y += v;
         v = (v - gravity) * 0.98;
      }

      return y;
   }

   public static boolean charging(ServerPlayer player) {
      ChargedJump.State st = STATES.get(player.getUUID());
      return st != null && st.start >= 0L;
   }

   public static void forget(ServerPlayer player) {
      STATES.remove(player.getUUID());
   }

   public static float charge(float ticks) {
      return Mth.clamp(ticks / 30.0F, 0.0F, 1.0F);
   }

   private ChargedJump() {
   }

   private static final class State {
      long start = -1L;
      long launched = -1L;
      long readyAt;
      double peakY;
      @Nullable
      ResourceLocation fx;
   }
}
