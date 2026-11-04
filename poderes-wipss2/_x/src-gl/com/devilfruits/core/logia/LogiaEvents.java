package com.devilfruits.core.logia;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.haki.HakiCombat;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFTriggers;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

@EventBusSubscriber(
   modid = "devilfruits"
)
public final class LogiaEvents {
   private static final Map<Long, Long> PASSED = new HashMap<>();

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof ServerPlayer victim && LogiaManager.isLogia(victim)) {
         DamageSource source = event.getSource();
         LogiaManager.trigger(victim, CounterTrigger.Kind.DAMAGE_TYPE, source);
         LogiaDamage.Decision decision = LogiaDamage.decide(victim, source);
         if (decision.bypass() == LogiaBypassSource.HAKI) {
            if (decision.factor() < 1.0F) {
               event.setAmount(event.getAmount() * decision.factor());
            }

            LogiaHits.add(victim, source, decision);
            if (source.getEntity() instanceof ServerPlayer attacker) {
               DFTriggers.HAKI_LOGIA_HIT.get().trigger(attacker);
            }

            return;
         }

         if (!decision.negated()) {
            return;
         }

         PlayerFruitData data = DFAttachments.data(victim);
         float amount = event.getAmount();
         float cost = LogiaDamage.cost(decision, amount);
         boolean free = AbilityManager.freeEnergy(victim);
         if (!free && data.energy() < cost) {
            return;
         }

         if (!free) {
            data.setEnergy(data.energy() - cost);
         }

         data.setLastCombatTick(victim.level().getGameTime());
         LogiaHits.add(victim, source, decision);
         if (decision.rule() == HitRule.PASS) {
            event.setCanceled(true);
            DFTriggers.LOGIA_PASSED.get().trigger(victim, data.fruitId());
         } else {
            event.setAmount(amount * decision.factor());
         }

         return;
      }
   }

   @SubscribeEvent
   public static void onProjectileImpact(ProjectileImpactEvent event) {
      Projectile projectile = event.getProjectile();
      if (projectile.level() instanceof ServerLevel level) {
         HitResult var15 = event.getRayTraceResult();
         if (projectile instanceof ThrownPotion potion) {
            PotionContents contents = (PotionContents)potion.getItem().get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.is(Potions.WATER)) {
               AABB area = new AABB(var15.getLocation(), var15.getLocation()).inflate(4.0, 2.0, 4.0);

               for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area, LogiaManager::isLogia)) {
                  LogiaManager.trigger(player, CounterTrigger.Kind.SPLASH_WATER, null);
               }
            }
         } else {
            if (var15 instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof ServerPlayer victim && LogiaManager.isLogia(victim)) {
               long key = (long)projectile.getId() << 32 | (long)victim.getId() & 4294967295L;
               long now = level.getGameTime();
               if (PASSED.containsKey(key) && now - PASSED.get(key) < 40L) {
                  event.setCanceled(true);
                  return;
               }

               LogiaDamage.Decision decision = LogiaDamage.decide(victim, sourceOf(level, projectile));
               if (decision.rule() == HitRule.CONNECT) {
                  return;
               }

               PlayerFruitData data = DFAttachments.data(victim);
               float cost = LogiaDamage.cost(decision, 2.0F);
               if (!AbilityManager.freeEnergy(victim)) {
                  if (data.energy() < cost) {
                     return;
                  }

                  data.setEnergy(data.energy() - cost);
               }

               PASSED.put(key, now);
               if (PASSED.size() > 256) {
                  Iterator<Entry<Long, Long>> it = PASSED.entrySet().iterator();

                  while (it.hasNext()) {
                     if (now - it.next().getValue() > 40L) {
                        it.remove();
                     }
                  }
               }

               LogiaHits.add(victim, projectile.position(), projectile.getDeltaMovement(), decision, projectile.getId(), projectile.getBbWidth());
               DFTriggers.LOGIA_PASSED.get().trigger(victim, data.fruitId());
               if (decision.rule() == HitRule.PARTIAL && (projectile instanceof Fireball || projectile.isOnFire())) {
                  victim.igniteForSeconds(2.0F);
               }

               event.setCanceled(true);
               return;
            }
         }
      } else {
         if (event.getRayTraceResult() instanceof EntityHitResult hit
            && hit.getEntity() instanceof Player victim
            && LogiaManager.bodyReady(victim)
            && victim.hasData(DFAttachments.PLAYER_DATA)
            && (DFAttachments.data(victim).logia().intangible() || DFAttachments.data(victim).logia().state() == LogiaState.ELEMENTAL_BODY)) {
            event.setCanceled(true);
         }
      }
   }

   private static DamageSource sourceOf(ServerLevel level, Projectile projectile) {
      Entity owner = projectile.getOwner();
      if (projectile instanceof AbstractArrow arrow && !(projectile instanceof ThrownTrident)) {
         return level.damageSources().arrow(arrow, owner);
      }

      if (projectile instanceof ThrownTrident) {
         return level.damageSources().trident(projectile, owner);
      } else if (projectile instanceof Fireball fireball) {
         return level.damageSources().fireball(fireball, owner);
      } else {
         return owner instanceof LivingEntity living
            ? level.damageSources().mobProjectile(projectile, living)
            : level.damageSources().thrown(projectile, owner);
      }
   }

   @SubscribeEvent
   public static void onEntityInteract(EntityInteract event) {
      if (!event.getLevel().isClientSide() && event.getTarget() instanceof ServerPlayer target && LogiaManager.isLogia(target)) {
         if (event.getItemStack().is(Items.WATER_BUCKET)) {
            LogiaManager.trigger(target, CounterTrigger.Kind.WATER_BUCKET, null);
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
         }
      }
   }

   @SubscribeEvent
   public static void onAttack(AttackEntityEvent event) {
      if (LogiaManager.state(event.getEntity()) == LogiaState.ELEMENTAL_BODY && !HakiCombat.coatedHand(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onUseItem(RightClickItem event) {
      if (LogiaManager.state(event.getEntity()) == LogiaState.ELEMENTAL_BODY) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onUseBlock(RightClickBlock event) {
      if (LogiaManager.state(event.getEntity()) == LogiaState.ELEMENTAL_BODY) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onServerTick(Post event) {
      LogiaHits.flush();
   }

   private LogiaEvents() {
   }
}
