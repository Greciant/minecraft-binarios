package com.devilfruits.fruits.kage_kage.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.combat.Stun;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.fruits.kage_kage.KageAbilities;
import com.devilfruits.fruits.kage_kage.KageAim;
import com.devilfruits.fruits.kage_kage.KageEntities;
import com.devilfruits.fruits.kage_kage.KageFx;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.KageSounds;
import com.devilfruits.fruits.kage_kage.entity.Doppelman;
import com.devilfruits.registry.DFAttachments;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class DoppelmanAbility extends Ability {
   private static final Map<UUID, UUID> DOUBLES = new HashMap<>();
   private static final Map<UUID, Long> NEXT_SWAP = new HashMap<>();

   public DoppelmanAbility() {
      super(
         KageKage.id("doppelman"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(50)
            .cooldown(1200)
            .mastery(8)
            .damage(3.5)
            .range(30.0)
            .knockback(0.4)
            .extra("health", 80.0, 1.0, 1000.0, "The double's health.")
            .extra("leashBlocks", 24.0, 8.0, 128.0, "Farther than this from the user the double steps back to them through a pool.")
            .extra("commandEnergy", 2.0, 0.0, 50.0, "Energy an order (attack / recall) costs.")
            .extra("aimConeDegrees", 8.0, 0.0, 45.0, "Aim assist for an order: a creature this close to the crosshair is the one to fight.")
            .extra("swapEnergy", 6.0, 0.0, 100.0, "Energy a swap of places with the double (the swap key, Y) costs.")
            .extra("swapCooldownTicks", 30.0, 0.0, 1200.0, "Ticks between two swaps.")
            .build()
      );
   }

   @Override
   public float energyCost(float charge) {
      return 0.0F;
   }

   @Override
   public int cooldownTicks(float charge) {
      return 0;
   }

   @Nullable
   public static Doppelman doubleOf(ServerPlayer user) {
      UUID id = DOUBLES.get(user.getUUID());
      if (id != null && user.serverLevel().getEntity(id) instanceof Doppelman d && d.isAlive() && !d.isRemoved()) {
         return d;
      } else {
         DOUBLES.remove(user.getUUID());
         return null;
      }
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      boolean free = AbilityManager.freeEnergy(player);
      float cost = doubleOf(player) == null ? (float)this.config().energyCost() : (float)this.config().extra("commandEnergy");
      return !free && data.energy() < cost ? Component.translatable("message.devilfruits.kage_kage.doppelman.no_energy") : null;
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      PlayerFruitData data = ctx.data();
      boolean free = AbilityManager.freeEnergy(player);
      Doppelman out = doubleOf(player);
      if (out != null) {
         if (!free) {
            data.setEnergy(data.energy() - (float)ctx.extra("commandEnergy"));
         }

         LivingEntity target = KageAim.aimed(player, (double)ctx.range(), ctx.extra("aimConeDegrees"));
         if (target != null && out.mayFight(target)) {
            out.command(target);
            ServerFx.send(player, KageFx.DOPPEL_ORDER, target.getBoundingBox().getCenter(), Vec3.ZERO, 1.0F, ctx.seed(), out.getId());
         } else {
            out.recall();
         }
      } else {
         if (!free) {
            data.setEnergy(data.energy() - (float)this.config().energyCost());
         }

         summon(ctx);
      }
   }

   @Nullable
   public static Doppelman summon(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      Vec3 look = ctx.horizontalLook();
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      Doppelman d = (Doppelman)KageEntities.DOPPELMAN.get().create(level);
      if (d == null) {
         return null;
      } else {
         Vec3 spot = player.position();

         for (Vec3 off : new Vec3[]{right.scale(1.6), right.scale(-1.6), look.scale(1.6), look.scale(-1.6), Vec3.ZERO}) {
            Vec3 at = floor(player, player.position().add(off));
            if (at != null && level.noCollision(d, d.getType().getDimensions().makeBoundingBox(at))) {
               spot = at;
               break;
            }
         }

         d.moveTo(spot.x, spot.y, spot.z, player.getYRot(), 0.0F);
         d.setYHeadRot(player.getYRot());
         d.setYBodyRot(player.getYRot());
         d.setOwner(player, ctx.extra("leashBlocks"));
         d.getAttribute(Attributes.MAX_HEALTH).setBaseValue(ctx.extra("health"));
         d.setHealth(d.getMaxHealth());
         d.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(ctx.config().damage());
         level.addFreshEntity(d);
         DOUBLES.put(player.getUUID(), d.getUUID());
         ServerFx.send(d, KageFx.DOPPEL_POOL, spot, Vec3.ZERO, 1.3F, ctx.seed(), 48);
         ServerFx.animate(player, ctx.animation(KageKage.id("summon")));
         return d;
      }
   }

   public static boolean swap(ServerPlayer player) {
      if (KageKage.canUse(player) && !player.isPassenger() && !Stun.stunned(player)) {
         Doppelman d = doubleOf(player);
         if (d != null && d.standing() && !d.isPassenger()) {
            DoppelmanAbility ability = KageAbilities.DOPPELMAN;
            long now = player.level().getGameTime();
            Long next = NEXT_SWAP.get(player.getUUID());
            if (next != null && now < next) {
               return false;
            } else {
               PlayerFruitData data = DFAttachments.data(player);
               float cost = (float)ability.config().extra("swapEnergy");
               boolean free = AbilityManager.freeEnergy(player);
               if (!free && data.energy() < cost) {
                  player.displayClientMessage(Component.translatable("message.devilfruits.kage_kage.doppelman.no_energy"), true);
                  return false;
               } else {
                  if (!free) {
                     data.setEnergy(data.energy() - cost);
                  }

                  NEXT_SWAP.put(player.getUUID(), now + (long)ability.config().extra("swapCooldownTicks"));
                  Vec3 mine = player.position();
                  Vec3 its = d.position();
                  long seed = player.getRandom().nextLong();
                  ServerFx.send(player, KageFx.DOPPEL_POOL, mine, Vec3.ZERO, 1.0F, seed, 14);
                  ServerFx.send(player, KageFx.DOPPEL_POOL, its, Vec3.ZERO, 1.0F, seed ^ 25214903917L, 14);
                  player.stopRiding();
                  player.teleportTo(its.x, its.y, its.z);
                  player.setDeltaMovement(Vec3.ZERO);
                  player.hurtMarked = true;
                  player.resetFallDistance();
                  d.teleportTo(mine.x, mine.y, mine.z);
                  d.setDeltaMovement(Vec3.ZERO);
                  d.getNavigation().stop();
                  d.resetFallDistance();
                  player.level().playSound(null, mine.x, mine.y, mine.z, KageSounds.SHADOW_RETURN.get(), SoundSource.PLAYERS, 0.7F, 1.4F);
                  player.level().playSound(null, its.x, its.y, its.z, KageSounds.SHADOW_RETURN.get(), SoundSource.PLAYERS, 0.7F, 1.4F);
                  return true;
               }
            }
         } else {
            player.displayClientMessage(Component.translatable("message.devilfruits.kage_kage.doppelman.no_double"), true);
            return false;
         }
      } else {
         return false;
      }
   }

   @Nullable
   private static Vec3 floor(ServerPlayer player, Vec3 at) {
      HitResult hit = player.level().clip(new ClipContext(at.add(0.0, 1.5, 0.0), at.add(0.0, -3.0, 0.0), Block.COLLIDER, Fluid.NONE, player));
      return hit.getType() == Type.MISS ? null : hit.getLocation();
   }

   public static void onDeath(ServerPlayer owner) {
      DOUBLES.remove(owner.getUUID());
      PlayerFruitData data = DFAttachments.data(owner);
      DoppelmanAbility ability = KageAbilities.DOPPELMAN;
      int ticks = Math.round((float)ability.config().cooldownTicks() * Mastery.cooldownMultiplier(data.mastery()));
      data.startCooldown(ability.id(), owner.level().getGameTime(), ticks);
   }
}
