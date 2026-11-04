package com.devilfruits.fruits.kage_kage;

import com.devilfruits.fruits.kage_kage.ability.BrickBatAbility;
import com.devilfruits.fruits.kage_kage.entity.Doppelman;
import com.devilfruits.fruits.kage_kage.item.MoriaScissorsItem;
import com.devilfruits.fruits.kage_kage.shadow.Shadows;
import com.devilfruits.fruits.kage_kage.shadow.Sunburn;
import com.devilfruits.mobs.pirate.Pirates;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent.Post;

public final class KageEvents {
   public static void register(IEventBus bus) {
      bus.addListener(KageEvents::onJoin);
      bus.addListener(KageEvents::onTick);
      bus.addListener(KageEvents::onDeath);
      bus.addListener(KageEvents::onDrops);
      bus.addListener(KageEvents::onDamage);
      bus.addListener(KageEvents::onLogin);
      bus.addListener(KageEvents::onRespawn);
      bus.addListener(KageEvents::onLogout);
      bus.addListener(KageEvents::onContainer);
      bus.addListener(KageEvents::onProjectile);
      bus.addListener(KageEvents::onTarget);
   }

   private static void onTarget(LivingChangeTargetEvent event) {
      if (event.getNewAboutToBeSetTarget() instanceof Doppelman && event.getEntity() instanceof AbstractGolem) {
         event.setCanceled(true);
      }
   }

   private static void onProjectile(ProjectileImpactEvent event) {
      if (event.getRayTraceResult() instanceof EntityHitResult hit
         && hit.getEntity() instanceof ServerPlayer player
         && BrickBatAbility.intercept(player, event.getProjectile())) {
         event.setCanceled(true);
      }
   }

   private static void onJoin(EntityJoinLevelEvent event) {
      if (!event.getLevel().isClientSide()) {
         Shadows.onJoin(event.getEntity());
      }
   }

   private static void onTick(Post event) {
      if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide() && Shadows.isShadowless(living)) {
         Sunburn.tick(living);
      }
   }

   private static void onDeath(LivingDeathEvent event) {
      LivingEntity dead = event.getEntity();
      if (!dead.level().isClientSide()) {
         if (dead instanceof ServerPlayer player) {
            MoriaScissorsItem.join(player);
            if (KageKage.isUser(player)) {
               Shadows.releaseAll(player, Shadows.Release.DEATH);
            }
         }

         Shadows.onVictimDeath(dead);
      }
   }

   private static void onDrops(LivingDropsEvent event) {
      LivingEntity dead = event.getEntity();
      if (event.getSource().getEntity() instanceof ServerPlayer killer && dead.level() instanceof ServerLevel level && dead.getType().is(KageTags.FLESHY)) {
         ItemStack weapon = killer.getMainHandItem();
         if (!(weapon.getItem() instanceof MoriaScissorsItem)) {
            return;
         }

         int count = fleshCount(level, dead, weapon);
         if (count > 0) {
            event.getDrops()
               .add(new ItemEntity(level, dead.getX(), dead.getY() + 0.5, dead.getZ(), new ItemStack((ItemLike)KageItems.CADAVER_FLESH.get(), count)));
         }

         return;
      }
   }

   public static int fleshCount(ServerLevel level, LivingEntity dead, ItemStack weapon) {
      int looting = weapon.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING));
      int count = (int)KageKage.setting("fleshPerKill") + (looting > 0 ? dead.getRandom().nextInt(looting + 1) : 0);
      if (dead.getType() == Pirates.CAPTAIN.get()) {
         count += (int)KageKage.setting("captainExtraFlesh");
      }

      return count;
   }

   private static void onDamage(LivingIncomingDamageEvent event) {
      if (event.getSource().is(KageKage.SHADOW_DAMAGE) && event.getSource().getEntity() instanceof ServerPlayer user && KageKage.isUser(user)) {
         event.setAmount(event.getAmount() * Shadows.damageMultiplier(user));
      }
   }

   private static void onLogin(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         Shadows.applyBonuses(player);
      }
   }

   private static void onRespawn(PlayerRespawnEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         Shadows.applyBonuses(player);
      }
   }

   private static void onLogout(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         MoriaScissorsItem.join(player);
      }
   }

   private static void onContainer(Open event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         MoriaScissorsItem.join(player);
      }
   }

   private KageEvents() {
   }
}
