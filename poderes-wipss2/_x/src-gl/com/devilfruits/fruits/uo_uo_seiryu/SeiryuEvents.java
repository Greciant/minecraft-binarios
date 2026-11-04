package com.devilfruits.fruits.uo_uo_seiryu;

import com.devilfruits.core.body.SerpentBody;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsConfig;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.physics.StructureBlock;
import com.devilfruits.core.physics.StructureForces;
import com.devilfruits.fruits.uo_uo_seiryu.ability.HybridCombat;
import com.devilfruits.fruits.uo_uo_seiryu.entity.SeiryuDragonEntity;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

public final class SeiryuEvents {
   public static void register(IEventBus bus) {
      bus.addListener(SeiryuEvents::onIncomingDamage);
      bus.addListener(SeiryuEvents::onPlayerTick);
      bus.addListener(SeiryuEvents::onMount);
      bus.addListener(SeiryuEvents::onLogout);
   }

   private static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (!event.getEntity().level().isClientSide()) {
         DamageSource source = event.getSource();
         if (event.getEntity() instanceof ServerPlayer victim && UoUoSeiryu.isUser(victim)) {
            if (UoUoSeiryu.isIn(victim, "dragon") && (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_LIGHTNING))) {
               event.setCanceled(true);
               victim.clearFire();
               return;
            }

            if (UoUoSeiryu.isIn(victim, "hybrid") && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
               event.setAmount(event.getAmount() * (float)(1.0 - fruit().config().setting("hybridDamageReduction")));
               event.setAmount(event.getAmount() * HybridCombat.incomingFactor(victim));
            }
         }

         if (source.getEntity() instanceof ServerPlayer attacker && attacker != event.getEntity()) {
            event.setAmount(event.getAmount() * HybridCombat.outgoingFactor(attacker));
         }

         if (source.is(DamageTypes.PLAYER_ATTACK)
            && source.getEntity() instanceof ServerPlayer attacker
            && source.getDirectEntity() == attacker
            && UoUoSeiryu.isUser(attacker)
            && !UoUoSeiryu.isIn(attacker, "dragon")) {
            float bonus = (float)fruit().config().setting("meleeAuraDamage") * (UoUoSeiryu.isIn(attacker, "hybrid") ? 2.0F : 1.0F);
            event.setAmount(event.getAmount() + bonus);
            boolean flame = (double)attacker.getRandom().nextFloat() < fruit().config().setting("meleeIgniteChance");
            if (flame) {
               event.getEntity().igniteForSeconds(3.0F);
            }

            Entity target = event.getEntity();
            Vec3 at = target.getBoundingBox().getCenter();
            ServerFx.send(target, SeiryuFx.MELEE_SPARK, at, attacker.getLookAngle(), 1.0F, attacker.getRandom().nextLong(), flame ? 1 : 0);
         }
      }
   }

   private static void onPlayerTick(Post event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         HybridSize.tick(player);
         DragonShells.tick(player);
         if (UoUoSeiryu.isIn(player, "dragon") && player.isOnFire()) {
            player.clearFire();
         }

         if (player.tickCount % 5 == 0 && UoUoSeiryu.isIn(player, "dragon") && PhysicsWorld.active()) {
            SeiryuDragonEntity shell = DragonShells.get(player);
            if (shell != null && shell.body() != null) {
               restOnStructures(player, shell.body());
            }
         }
      }
   }

   private static void restOnStructures(ServerPlayer player, SerpentBody body) {
      double[] p = new double[3];
      int samples = 8;
      double weight = (Double)PhysicsConfig.DRAGON_SEGMENT_WEIGHT.get() * 5.0;

      for (int i = 0; i < samples; i++) {
         float t = ((float)i + 0.5F) / (float)samples;
         body.point((double)(body.neck + body.length * t), 1.0F, p);
         double r = (double)body.radiusAt(t);
         Vec3 under = new Vec3(p[0], p[1] - r - 0.3, p[2]);
         List<StructureBlock> contact = PhysicsWorld.blocksIn(player.level(), ImpactShape.sphere(under, 0.8), null, 1);
         if (!contact.isEmpty()) {
            StructureForces.weight(contact.get(0).structure(), under, weight * Math.max(0.3, r));
         }
      }
   }

   private static void onLogout(PlayerLoggedOutEvent event) {
      DragonShells.forget(event.getEntity().getUUID());
   }

   private static void onMount(EntityMountEvent event) {
      if (event.isMounting() && event.getEntityMounting() instanceof ServerPlayer player && UoUoSeiryu.isIn(player, "dragon")) {
         event.setCanceled(true);
      }
   }

   private static DevilFruit fruit() {
      return UoUoSeiryu.fruit();
   }

   private SeiryuEvents() {
   }
}
