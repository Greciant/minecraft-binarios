package com.devilfruits.fruits.supa_supa.client.blade;

import com.devilfruits.fruits.supa_supa.BladeLimbs;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Iterator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class BladeLimbsClient {
   public static final int GROW_TICKS = 8;
   public static final int RETRACT_TICKS = 4;
   public static final int SWITCH_TICKS = 2;
   public static final int STEEL_TICKS = 12;
   private static final Int2ObjectMap<BladeLimbsClient.Limbs> LIMBS = new Int2ObjectOpenHashMap();

   public static int growTicks(BladeLimbs.ArmMode mode) {
      return switch (mode) {
         case X_BLADES -> 4;
         case CLAWS -> 5;
         case SAW -> 6;
         default -> 8;
      };
   }

   public static BladeLimbsClient.Limbs of(Player player) {
      return (BladeLimbsClient.Limbs)LIMBS.get(player.getId());
   }

   public static boolean armOut(Player player, boolean rightArm) {
      BladeLimbsClient.Limbs l = (BladeLimbsClient.Limbs)LIMBS.get(player.getId());
      return l != null && l.arm(rightArm).visible();
   }

   public static void tick(ClientLevel level) {
      BladeSkinMask.tick();
      Iterator<BladeLimbsClient.Limbs> it = LIMBS.values().iterator();

      while (it.hasNext()) {
         BladeLimbsClient.Limbs l = it.next();
         l.seen = false;
      }

      for (Player player : level.players()) {
         BladeLimbs.State state = BladeLimbs.of(player);
         BladeLimbsClient.Limbs l = (BladeLimbsClient.Limbs)LIMBS.get(player.getId());
         if (l == null) {
            if (!state.any()) {
               continue;
            }

            l = new BladeLimbsClient.Limbs();
            LIMBS.put(player.getId(), l);
         }

         l.seen = true;
         l.wanted = state.any();
         BladeLimbs.ArmMode arms = state.mode();
         beat(player, true, l.right.step(state.right() ? arms : BladeLimbs.ArmMode.NONE, growTicks(l.right.shown)), l.right);
         beat(player, false, l.left.step(state.left() ? arms : BladeLimbs.ArmMode.NONE, growTicks(l.left.shown)), l.left);
         int legs = l.legs.step(state.legs() ? BladeLimbs.ArmMode.FOREARM : BladeLimbs.ArmMode.NONE, 8);
         if (legs != 0) {
            BladeFx.legsBeat(player, legs);
         }

         int steel = l.steel.step(state.steel() ? BladeLimbs.ArmMode.FOREARM : BladeLimbs.ArmMode.NONE, 12);
         if (steel != 0) {
            BladeFx.steelBeat(player, steel);
         }
      }

      it = LIMBS.values().iterator();

      while (it.hasNext()) {
         BladeLimbsClient.Limbs lx = it.next();
         if (!lx.seen || !lx.wanted && lx.idle()) {
            it.remove();
         }
      }
   }

   private static void beat(Player player, boolean right, int beat, BladeLimbsClient.Limb limb) {
      if (beat != 0) {
         BladeFx.armBeat(player, right, limb.shown, beat);
      }
   }

   public static void clear() {
      LIMBS.clear();
      BladeSkinMask.clear();
   }

   private BladeLimbsClient() {
   }

   public static final class Limb {
      BladeLimbs.ArmMode shown = BladeLimbs.ArmMode.NONE;
      float growth;
      float prev;
      int age = -1;
      boolean settled;

      public BladeLimbs.ArmMode shown() {
         return this.shown;
      }

      public float growth(float partial) {
         return Mth.lerp(partial, this.prev, this.growth);
      }

      public float age(float partial) {
         return this.age < 0 ? -1.0F : (float)this.age + partial;
      }

      public boolean growing() {
         return this.age >= 0;
      }

      public boolean visible() {
         return this.shown != BladeLimbs.ArmMode.NONE && (this.growth > 0.0F || this.prev > 0.0F);
      }

      int step(BladeLimbs.ArmMode target, int growTicks) {
         this.prev = this.growth;
         int beat = 0;
         if (this.shown != target && this.shown != BladeLimbs.ArmMode.NONE && this.growth > 0.0F) {
            if (this.age >= 0) {
               beat = 3;
            }

            this.age = -1;
            this.growth = Math.max(0.0F, this.growth - 1.0F / (float)(target == BladeLimbs.ArmMode.NONE ? 4 : 2));
            return beat;
         } else if (this.shown != target) {
            this.shown = target;
            this.settled = false;
            this.age = target == BladeLimbs.ArmMode.NONE ? -1 : 0;
            return target == BladeLimbs.ArmMode.NONE ? 0 : 1;
         } else {
            if (target != BladeLimbs.ArmMode.NONE) {
               this.age = this.age < 0 ? 0 : this.age + 1;
               this.growth = Math.min(1.0F, this.growth + 1.0F / (float)growTicks);
               if (!this.settled && this.growth >= 0.55F) {
                  this.settled = true;
                  beat = 2;
               }
            }

            return beat;
         }
      }
   }

   public static final class Limbs {
      public final BladeLimbsClient.Limb right = new BladeLimbsClient.Limb();
      public final BladeLimbsClient.Limb left = new BladeLimbsClient.Limb();
      public final BladeLimbsClient.Limb legs = new BladeLimbsClient.Limb();
      public final BladeLimbsClient.Limb steel = new BladeLimbsClient.Limb();
      boolean seen;
      boolean wanted;

      public BladeLimbsClient.Limb arm(boolean rightArm) {
         return rightArm ? this.right : this.left;
      }

      boolean idle() {
         return !this.right.visible() && !this.left.visible() && !this.legs.visible() && !this.steel.visible();
      }
   }
}
