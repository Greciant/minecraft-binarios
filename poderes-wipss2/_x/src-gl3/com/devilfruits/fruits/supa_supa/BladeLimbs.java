package com.devilfruits.fruits.supa_supa;

import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.registry.DFAttachments;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class BladeLimbs {
   public static final int RIGHT_ARM = FormFlags.fruit(0);
   public static final int LEFT_ARM = FormFlags.fruit(1);
   public static final int LEGS = FormFlags.fruit(2);
   public static final int STEEL = FormFlags.fruit(3);
   private static final int MODE_SHIFT = 12;
   private static final int MODE_MASK = 28672;
   private static final int ALL = RIGHT_ARM | LEFT_ARM | LEGS | STEEL | 28672;
   private static final Map<UUID, Long> ARMS_UNTIL = new HashMap<>();

   public static int flags(BladeLimbs.ArmMode mode, boolean right, boolean left, boolean legs, boolean steel) {
      boolean arms = mode != BladeLimbs.ArmMode.NONE && (right || left);
      return (arms ? mode.ordinal() << 12 | (right ? RIGHT_ARM : 0) | (left ? LEFT_ARM : 0) : 0) | (legs ? LEGS : 0) | (steel ? STEEL : 0);
   }

   public static BladeLimbs.State of(Player player) {
      if (!player.hasData(DFAttachments.PLAYER_DATA)) {
         return BladeLimbs.State.NONE;
      } else {
         PlayerFruitData data = DFAttachments.data(player);
         return SupaSupa.ID.equals(data.fruitId()) ? BladeLimbs.State.of(data.flags()) : BladeLimbs.State.NONE;
      }
   }

   public static void arms(ServerPlayer player, BladeLimbs.ArmMode mode, boolean right, boolean left, int holdTicks) {
      boolean on = mode != BladeLimbs.ArmMode.NONE && (right || left);
      int bits = on ? mode.ordinal() << 12 | (right ? RIGHT_ARM : 0) | (left ? LEFT_ARM : 0) : 0;
      apply(player, RIGHT_ARM | LEFT_ARM | 28672, bits);
      if (on && holdTicks > 0) {
         ARMS_UNTIL.put(player.getUUID(), player.level().getGameTime() + (long)holdTicks);
      } else {
         ARMS_UNTIL.remove(player.getUUID());
      }
   }

   public static void clearArms(ServerPlayer player, BladeLimbs.ArmMode mode) {
      if (of(player).mode() == mode) {
         arms(player, BladeLimbs.ArmMode.NONE, false, false, 0);
      }
   }

   public static void legs(ServerPlayer player, boolean on) {
      apply(player, LEGS, on ? LEGS : 0);
   }

   public static void steel(ServerPlayer player, boolean on) {
      apply(player, STEEL, on ? STEEL : 0);
   }

   public static void clear(ServerPlayer player) {
      ARMS_UNTIL.remove(player.getUUID());
      apply(player, ALL, 0);
   }

   static void tick(ServerPlayer player) {
      Long until = ARMS_UNTIL.get(player.getUUID());
      if (until != null && player.level().getGameTime() >= until) {
         ARMS_UNTIL.remove(player.getUUID());
         apply(player, RIGHT_ARM | LEFT_ARM | 28672, 0);
      }
   }

   static void forget(UUID player) {
      ARMS_UNTIL.remove(player);
   }

   private static void apply(ServerPlayer player, int mask, int bits) {
      PlayerFruitData data = DFAttachments.data(player);
      if (data.setFlags(data.flags() & ~mask | bits & mask)) {
         ServerFx.formState(player);
      }
   }

   private BladeLimbs() {
   }

   public static enum ArmMode {
      NONE,
      CLAWS,
      FOREARM,
      X_BLADES,
      SAW;

      private static final BladeLimbs.ArmMode[] VALUES = values();

      static BladeLimbs.ArmMode of(int ordinal) {
         return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : NONE;
      }
   }

   public static record State(boolean right, boolean left, BladeLimbs.ArmMode mode, boolean legs, boolean steel) {
      public static final BladeLimbs.State NONE = new BladeLimbs.State(false, false, BladeLimbs.ArmMode.NONE, false, false);

      public boolean any() {
         return this.right || this.left || this.legs || this.steel;
      }

      public static BladeLimbs.State of(int flags) {
         BladeLimbs.ArmMode mode = BladeLimbs.ArmMode.of((flags & 28672) >> 12);
         boolean arms = mode != BladeLimbs.ArmMode.NONE;
         return new BladeLimbs.State(
            arms && (flags & BladeLimbs.RIGHT_ARM) != 0,
            arms && (flags & BladeLimbs.LEFT_ARM) != 0,
            mode,
            (flags & BladeLimbs.LEGS) != 0,
            (flags & BladeLimbs.STEEL) != 0
         );
      }
   }
}
