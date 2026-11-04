package com.devilfruits.fruits.bomu_bomu;

import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.logia.DamageCategory;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.fruits.bomu_bomu.ability.ExplosivePunchAbility;
import com.devilfruits.fruits.bomu_bomu.ability.SelfDestructAbility;
import com.devilfruits.fruits.bomu_bomu.explosion.BombExplosionService;
import com.devilfruits.fruits.bomu_bomu.trade.PistolTrade;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent.Detonate;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

public final class BomuEvents {
   private static final Set<ResourceLocation> PISTOL_CHESTS = Set.of(
      BuiltInLootTables.SHIPWRECK_TREASURE.location(), BuiltInLootTables.BURIED_TREASURE.location(), BuiltInLootTables.SIMPLE_DUNGEON.location()
   );
   private static final Map<UUID, Long> LAST_ABSORB = new HashMap<>();

   static void register(IEventBus bus) {
      bus.addListener(EventPriority.HIGH, BomuEvents::onIncomingDamage);
      bus.addListener(BomuEvents::onDetonate);
      bus.addListener(BomuEvents::onKnockback);
      bus.addListener(BomuEvents::onAttack);
      bus.addListener(BomuEvents::onLeftClickBlock);
      bus.addListener(BomuEvents::onPlayerTick);
      bus.addListener(BomuEvents::onTrades);
      bus.addListener(BomuEvents::onLootLoad);
      bus.addListener(BomuEvents::onLogout);
      bus.addListener(BomuEvents::onServerStopped);
   }

   static boolean explosive(DamageSource source) {
      if (source.is(DamageTypeTags.IS_EXPLOSION)) {
         return true;
      } else {
         Entity direct = source.getDirectEntity();
         return direct instanceof LargeFireball || direct instanceof WitherSkull || direct instanceof FireworkRocketEntity;
      }
   }

   private static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (!(event.getEntity() instanceof ServerPlayer player) || !BomuBomu.isUser(player)) {
         return;
      }

      if (SelfDestructAbility.invulnerable(player) && DamageCategory.of(event.getSource()) != DamageCategory.ALWAYS) {
         event.setCanceled(true);
      } else {
         if (BomuBomu.immune(player) && explosive(event.getSource())) {
            event.setCanceled(true);
            Entity direct = event.getSource().getDirectEntity();
            absorbed(player, direct != null ? direct.position() : player.position());
         }
      }
   }

   private static void onDetonate(Detonate event) {
      Explosion explosion = event.getExplosion();
      Vec3 c = explosion.center();
      double reach = (double)explosion.radius() * 2.0;
      boolean noKnockback = BomuBomu.flag("immuneToKnockback");
      event.getAffectedEntities().removeIf(e -> {
         if (e instanceof ServerPlayer player && BomuBomu.immune(player) && !(e.distanceToSqr(c) > reach * reach)) {
            absorbed(player, c);
            if (explosion.getDirectSourceEntity() instanceof PrimedTnt) {
               BomuTriggers.TNT_UNTOUCHED.get().trigger(player);
            }

            return noKnockback;
         }

         return false;
      });
   }

   private static void onKnockback(ExplosionKnockbackEvent event) {
      if (event.getAffectedEntity() instanceof Player player && BomuBomu.immune(player) && BomuBomu.flag("immuneToKnockback")) {
         event.setKnockbackVelocity(Vec3.ZERO);
      }
   }

   public static void absorbed(ServerPlayer player, Vec3 from) {
      long now = player.level().getGameTime();
      Long last = LAST_ABSORB.get(player.getUUID());
      if (last == null || !((double)(now - last) < BomuBomu.setting("absorbFxCooldownTicks"))) {
         LAST_ABSORB.put(player.getUUID(), now);
         ServerFx.send(player, BomuFx.ABSORB, from, Vec3.ZERO, 1.0F, player.getRandom().nextLong(), 0);
      }
   }

   private static void onAttack(AttackEntityEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && ExplosivePunchAbility.armed(player)) {
         Entity target = event.getTarget();
         Vec3 eye = player.getEyePosition();
         Vec3 contact = target.getBoundingBox().inflate(0.1).clip(eye, eye.add(player.getLookAngle().scale(8.0))).orElse(target.getBoundingBox().getCenter());
         Vec3 normal = eye.subtract(contact);
         ExplosivePunchAbility.detonate(player, contact, normal.lengthSqr() > 1.0E-6 ? normal.normalize() : new Vec3(0.0, 1.0, 0.0), target);
         return;
      }
   }

   private static void onLeftClickBlock(LeftClickBlock event) {
      if (event.getAction() == Action.START && event.getEntity() instanceof ServerPlayer player && ExplosivePunchAbility.armed(player)) {
         BlockPos pos = event.getPos();
         PhysicsStructure hull = PhysicsWorld.structureAt(player.level(), pos);
         Vec3 centre = hull != null ? hull.toWorld(Vec3.atCenterOf(pos)) : Vec3.atCenterOf(pos);
         Direction face = event.getFace() != null
            ? event.getFace()
            : Direction.getNearest(
               hull != null ? hull.toLocal(player.getEyePosition()).subtract(Vec3.atCenterOf(pos)) : player.getEyePosition().subtract(centre)
            );
         Vec3 n = Vec3.atLowerCornerOf(face.getNormal());
         if (hull != null) {
            n = hull.dirToWorld(n);
         }

         ExplosivePunchAbility.detonate(player, centre.add(n.scale(0.62)), n, null);
      }
   }

   private static void onPlayerTick(Post event) {
      if (event.getEntity() instanceof ServerPlayer player && BomuBomu.isUser(player)) {
         ExplosivePunchAbility.tick(player);
         SelfDestructAbility.tick(player);
      }
   }

   private static void onLogout(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         ExplosivePunchAbility.forget(player.getUUID());
         SelfDestructAbility.forget(player);
         BombExplosionService.forget(player.getUUID());
         LAST_ABSORB.remove(player.getUUID());
      }
   }

   private static void onServerStopped(ServerStoppedEvent event) {
      ExplosivePunchAbility.clear();
      SelfDestructAbility.clear();
      BombExplosionService.clear();
      LAST_ABSORB.clear();
   }

   private static void onTrades(VillagerTradesEvent event) {
      if (event.getType() == VillagerProfession.WEAPONSMITH) {
         ((List)event.getTrades().get(3)).add(new PistolTrade());
      }
   }

   private static void onLootLoad(LootTableLoadEvent event) {
      float chance = (float)BomuBomu.setting("pistolLootChance");
      if (chance > 0.0F && PISTOL_CHESTS.contains(event.getName())) {
         event.getTable()
            .addPool(
               LootPool.lootPool()
                  .name("devilfruits_bomb_pistol")
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem((ItemLike)BomuItems.BOMB_PISTOL.get()))
                  .when(LootItemRandomChanceCondition.randomChance(chance))
                  .build()
            );
      }
   }

   private BomuEvents() {
   }
}
