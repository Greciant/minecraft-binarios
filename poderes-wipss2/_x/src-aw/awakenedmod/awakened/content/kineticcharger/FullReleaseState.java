package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.core.Attrs;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public final class FullReleaseState {
   public static final int MAX_LEVEL = 10;
   public static final int STEP_PERCENT = 10;
   public static final Lab.Value STEP_COST = Lab.SERVER.def("kinetic.release.step_cost", 4.0, 0.0, 30.0, 0.5);
   private static final Lab.Value DRAIN = Lab.SERVER.def("kinetic.release.drain", 1.0, 0.0, 8.0, 0.05);
   private static final int TICKS_PER_SECOND = 20;
   private static final double SPEED_PER_LEVEL = 0.04;
   private static final double JUMP_PER_LEVEL = 0.06;
   private static final double DAMAGE_PER_LEVEL = 0.08;
   private static final double KNOCKBACK_PER_LEVEL = 0.1;
   private static final float RESIST_PER_LEVEL = 0.02F;
   private static final int ABILITY_BONUS_PERCENT = 5;
   private static final ResourceLocation SPEED_ID = AwId.of("kinetic_release_speed");
   private static final ResourceLocation JUMP_ID = AwId.of("kinetic_release_jump");
   private static final ResourceLocation DAMAGE_ID = AwId.of("kinetic_release_damage");
   private static final ResourceLocation KNOCKBACK_ID = AwId.of("kinetic_release_knockback");

   private FullReleaseState() {
   }

   public static int level(PowerData data) {
      return KineticBank.releaseLevel(data);
   }

   public static float resisted(PowerData data, DamageSource source, float amount) {
      int level = level(data);
      return level > 0 && !(amount <= 0.0F) && !source.is(DamageTypeTags.BYPASSES_RESISTANCE) ? amount * (1.0F - (float)level * 0.02F) : amount;
   }

   public static boolean step(ServerPlayer player, PowerData data, int delta) {
      return set(player, data, level(data) + delta);
   }

   public static boolean set(ServerPlayer player, PowerData data, int target) {
      int current = level(data);
      int next = Math.max(0, Math.min(10, target));
      if (next == current) {
         return false;
      } else {
         if (next > current) {
            float each = (float)STEP_COST.get();
            int affordable = each <= 0.0F ? 10 : (int)(KineticBank.value(data) / each);
            next = Math.min(next, current + affordable);
            if (next == current) {
               return false;
            }

            KineticBank.spend(player, (float)(next - current) * each);
         }

         KineticBank.setReleaseLevel(data, next);
         apply(player, next);
         KineticFx.releaseStep(player, next, next > current);
         PowerService.sync(player);
         return true;
      }
   }

   public static void tick(ServerPlayer player, PowerData data) {
      int level = level(data);
      if (level > 0) {
         float want = (float)((double)level * DRAIN.get() / 20.0);
         if (KineticBank.spend(player, want) < want - 1.0E-4F) {
            level = 0;
            KineticBank.setReleaseLevel(data, 0);
            KineticFx.releaseEmpty(player);
            PowerService.sync(player);
         } else {
            KineticFx.releaseTick(player, level);
         }
      }

      apply(player, level);
   }

   public static void clear(ServerPlayer player) {
      apply(player, 0);
   }

   private static void apply(ServerPlayer player, int level) {
      double progression = 1.0 + (double)MasteryScaling.progressionBonus(player);
      Attrs.set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, (double)level * 0.04 * progression, Operation.ADD_MULTIPLIED_TOTAL);
      Attrs.set(player, Attributes.JUMP_STRENGTH, JUMP_ID, (double)level * 0.06 * progression, Operation.ADD_MULTIPLIED_TOTAL);
      Attrs.set(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID, (double)level * 0.08, Operation.ADD_MULTIPLIED_TOTAL);
      Attrs.set(player, Attributes.ATTACK_KNOCKBACK, KNOCKBACK_ID, (double)level * 0.1, Operation.ADD_MULTIPLIED_TOTAL);
   }
}
