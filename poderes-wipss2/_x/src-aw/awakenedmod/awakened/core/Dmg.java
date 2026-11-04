package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.core.team.Teams;
import com.awakenedmod.awakened.mixin.LivingHurtCooldownAccessor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(
   modid = "awakened"
)
public final class Dmg {
   private static int suppressKnockbackFor = -1;
   private static Dmg.HitKind hitKind = Dmg.HitKind.NONE;
   private static volatile Dmg.Watcher watcher;
   private static final int VANILLA_HURT_WINDOW = 20;

   private Dmg() {
   }

   public static DamageSource by(ServerPlayer source, ResourceKey<DamageType> type) {
      return by((Entity)source, type);
   }

   public static DamageSource by(Entity source, ResourceKey<DamageType> type) {
      Holder<DamageType> holder = source.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type);
      return new DamageSource(holder, null, source);
   }

   public static boolean hurt(Entity target, ServerLevel level, DamageSource src, float amount) {
      Dmg.HitKind kind = hitKind == Dmg.HitKind.NONE && src.getEntity() instanceof Player ? Dmg.HitKind.ABILITY : hitKind;
      return within(kind, () -> apply(target, level, src, amount));
   }

   public static boolean weaponHurt(Entity target, ServerLevel level, DamageSource src, float amount) {
      return within(Dmg.HitKind.WEAPON, () -> apply(target, level, src, amount));
   }

   public static void ridingHit(Runnable rider) {
      within(hitKind == Dmg.HitKind.NONE ? Dmg.HitKind.WEAPON : hitKind, () -> {
         rider.run();
         return null;
      });
   }

   public static boolean inAbilityHit() {
      return hitKind == Dmg.HitKind.ABILITY;
   }

   public static void asAbilityHit(Runnable drop) {
      within(Dmg.HitKind.ABILITY, () -> {
         drop.run();
         return null;
      });
   }

   private static <T> T within(Dmg.HitKind kind, Supplier<T> body) {
      Dmg.HitKind outer = hitKind;
      hitKind = kind;

      Object var3;
      try {
         var3 = body.get();
      } finally {
         hitKind = outer;
      }

      return (T)var3;
   }

   public static void watch(Dmg.Watcher installed) {
      watcher = installed;
   }

   private static boolean apply(Entity target, ServerLevel level, DamageSource src, float amount) {
      if (Teams.harmForbidden(src.getEntity(), target)) {
         return false;
      } else {
         Dmg.Watcher watching = watcher;
         Consumer<Boolean> after = watching == null ? null : watching.before(target, src, amount);
         boolean landed = hurtVanilla(target, level, src, amount);
         if (after != null) {
            after.accept(landed);
         }

         return landed;
      }
   }

   private static boolean hurtVanilla(Entity target, ServerLevel level, DamageSource src, float amount) {
      return target.hurt(src, amount);
   }

   public static float weaponBonus(ServerLevel level, ItemStack weapon, Entity target, DamageSource src, float base) {
      return EnchantmentHelper.modifyDamage(level, weapon, target, src, base);
   }

   public static boolean hurtRapid(Entity target, ServerLevel level, DamageSource src, float amount) {
      if (Teams.harmForbidden(src.getEntity(), target)) {
         return false;
      } else {
         if (target instanceof LivingEntity living) {
            ((LivingHurtCooldownAccessor)living).awakened$setLastHurt(0.0F);
         }

         return hurt(target, level, src, amount);
      }
   }

   public static boolean hurtThrough(Entity target, ServerLevel level, DamageSource src, float amount) {
      if (Teams.harmForbidden(src.getEntity(), target)) {
         return false;
      } else {
         int had = target.invulnerableTime;
         target.invulnerableTime = 0;
         boolean dealt = hurtNoKnockback(target, level, src, amount);
         if (!dealt) {
            target.invulnerableTime = Math.max(target.invulnerableTime, had);
         }

         return dealt;
      }
   }

   public static boolean hurtTick(LivingEntity target, ServerLevel level, DamageSource src, float amount) {
      if (Teams.harmForbidden(src.getEntity(), target)) {
         return false;
      } else {
         LivingHurtCooldownAccessor cooldown = (LivingHurtCooldownAccessor)target;
         int hadWindow = target.invulnerableTime;
         float hadBaseline = cooldown.awakened$getLastHurt();
         target.invulnerableTime = 0;

         boolean var7;
         try {
            var7 = hurtNoKnockback(target, level, src, amount);
         } finally {
            int after = target.invulnerableTime;
            target.invulnerableTime = after > 20 ? Math.max(after, hadWindow) : hadWindow;
            cooldown.awakened$setLastHurt(hadBaseline);
         }

         return var7;
      }
   }

   public static boolean hurtNoKnockback(Entity target, ServerLevel level, DamageSource src, float amount) {
      int outer = suppressKnockbackFor;
      suppressKnockbackFor = target.getId();

      boolean var5;
      try {
         var5 = hurt(target, level, src, amount);
      } finally {
         suppressKnockbackFor = outer;
      }

      return var5;
   }

   public static void creditTo(LivingEntity victim, Player player) {
      victim.setLastHurtByPlayer(player);
   }

   @SubscribeEvent
   public static void onKnockBack(LivingKnockBackEvent event) {
      if (suppressKnockbackFor >= 0 && event.getEntity().getId() == suppressKnockbackFor) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onEnchantmentLevel(GetEnchantmentLevelEvent event) {
      if (hitKind == Dmg.HitKind.ABILITY && event.isTargetting(Enchantments.LOOTING) && onServerThread()) {
         event.getHolder(Enchantments.LOOTING).ifPresent(looting -> event.getEnchantments().set(looting, 0));
      }
   }

   private static boolean onServerThread() {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      return server != null && server.isSameThread();
   }

   public static ServerLevel serverLevel(ServerPlayer player) {
      return player.serverLevel();
   }

   private static enum HitKind {
      NONE,
      ABILITY,
      WEAPON;
   }

   @FunctionalInterface
   public interface Watcher {
      Consumer<Boolean> before(Entity var1, DamageSource var2, float var3);
   }
}
