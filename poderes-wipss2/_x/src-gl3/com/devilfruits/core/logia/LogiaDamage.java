package com.devilfruits.core.logia;

import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.config.LogiaFruitConfig;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFMobEffects;
import com.devilfruits.registry.DFTags;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class LogiaDamage {
   public static LogiaDamage.Decision decide(ServerPlayer victim, DamageSource source) {
      DamageCategory category = DamageCategory.of(source);
      LogiaProfile profile = LogiaManager.profile(victim);
      if (profile != null && (Boolean)CoreConfig.LOGIA_INTANGIBILITY.get() && category != DamageCategory.ALWAYS) {
         LogiaBypass.Result bypass = LogiaBypass.check(victim, source);
         if (bypass != null) {
            return new LogiaDamage.Decision(
               bypass.fraction() >= 1.0F ? HitRule.CONNECT : HitRule.PARTIAL, category, Math.min(1.0F, bypass.fraction()), bypass.source()
            );
         } else if (bypasses(source)) {
            return connect(category);
         } else {
            LogiaInteractionRegistry.Interaction interaction = LogiaInteractionRegistry.Interaction.NEUTRAL;
            if (source.getEntity() instanceof Player attacker && attacker != victim) {
               LogiaProfile theirs = LogiaManager.profile(attacker);
               if (theirs != null) {
                  interaction = LogiaInteractionRegistry.get(theirs.element(), profile.element());
               }
            }

            if (interaction != LogiaInteractionRegistry.Interaction.BYPASS && LogiaManager.bodyReady(victim)) {
               LogiaState state = DFAttachments.data(victim).logia().state();
               if (!DFAttachments.data(victim).logia().intangible() && state != LogiaState.ELEMENTAL_BODY) {
                  return connect(category);
               } else if (interaction == LogiaInteractionRegistry.Interaction.RESISTED) {
                  return new LogiaDamage.Decision(HitRule.PASS, category, 0.0F);
               } else {
                  HitRule rule = rule(victim, profile, category);
                  if (rule == HitRule.PARTIAL && state == LogiaState.ELEMENTAL_BODY) {
                     rule = HitRule.PASS;
                  }
                  return switch (rule) {
                     case PASS -> new LogiaDamage.Decision(HitRule.PASS, category, 0.0F);
                     case PARTIAL -> new LogiaDamage.Decision(HitRule.PARTIAL, category, ((Double)CoreConfig.LOGIA_PARTIAL_FACTOR.get()).floatValue());
                     case CONNECT -> connect(category);
                  };
               }
            } else {
               return connect(category);
            }
         }
      } else {
         return connect(category);
      }
   }

   public static HitRule rule(ServerPlayer victim, LogiaProfile profile, DamageCategory category) {
      LogiaFruitConfig config = DFAttachments.data(victim).fruit().config().logia();
      HitRule fromConfig = config == null ? profile.override(category) : config.rule(category);
      return fromConfig != null ? fromConfig : CoreConfig.logiaRule(category);
   }

   public static boolean bypasses(DamageSource source) {
      if (source.is(DFTags.LOGIA_BYPASS_DAMAGE)) {
         return true;
      } else {
         List<? extends String> listed = (List<? extends String>)CoreConfig.LOGIA_BYPASS_DAMAGE_TYPES.get();
         if (!listed.isEmpty()) {
            ResourceLocation type = source.typeHolder().unwrapKey().map(k -> k.location()).orElse(null);
            if (type != null && listed.contains(type.toString())) {
               return true;
            }
         }

         Entity attacker = source.getEntity();
         if (attacker instanceof LivingEntity living && (Boolean)CoreConfig.LOGIA_ARMAMENT_BYPASS.get() && living.hasEffect(DFMobEffects.ARMAMENT_COATING)) {
            return true;
         }

         ItemStack weapon = source.getWeaponItem();
         if (weapon == null || weapon.isEmpty()) {
            ItemStack var10000;
            label47: {
               if (attacker instanceof LivingEntity living && source.getDirectEntity() == attacker) {
                  var10000 = living.getMainHandItem();
                  break label47;
               }

               var10000 = ItemStack.EMPTY;
            }

            weapon = var10000;
         }

         return !weapon.isEmpty()
            && (weapon.is(DFTags.LOGIA_BYPASS_ITEMS) || (Boolean)CoreConfig.LOGIA_SEASTONE_WEAPONS_BYPASS.get() && weapon.is(DFTags.SEASTONE_WEAPONS));
      }
   }

   public static float cost(LogiaDamage.Decision decision, float amount) {
      float full = (float)((Double)CoreConfig.LOGIA_NEGATE_ENERGY.get() + (Double)CoreConfig.LOGIA_NEGATE_ENERGY_PER_DAMAGE.get() * (double)amount);
      return decision.rule() == HitRule.PASS ? full : full * 0.5F;
   }

   private static LogiaDamage.Decision connect(DamageCategory category) {
      return new LogiaDamage.Decision(HitRule.CONNECT, category, 1.0F);
   }

   private LogiaDamage() {
   }

   public static record Decision(HitRule rule, DamageCategory category, float factor, @Nullable LogiaBypassSource bypass) {
      public Decision(HitRule rule, DamageCategory category, float factor) {
         this(rule, category, factor, null);
      }

      public boolean negated() {
         return this.rule != HitRule.CONNECT;
      }
   }
}
