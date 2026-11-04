package com.devilfruits.fruits.bomu_bomu.explosion;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.fx.BombExplosionPayload;
import com.devilfruits.core.haki.HakiCombat;
import com.devilfruits.core.haki.HakiConfig;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.physics.StructureBlock;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.AbilityEntity;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.DebrisEntity;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import com.devilfruits.fruits.bomu_bomu.BomuEvents;
import com.devilfruits.fruits.bomu_bomu.BomuTriggers;
import com.devilfruits.registry.DFDamageTypes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public final class BombExplosionService {
   private static final Map<UUID, long[]> RATE = new HashMap<>();
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

   public static BombExplosionService.Result detonate(ServerLevel level, Vec3 pos, ExplosionSpec spec, @Nullable ServerPlayer owner) {
      return detonate(level, pos, UP, spec, owner);
   }

   public static BombExplosionService.Result detonate(ServerLevel level, Vec3 pos, Vec3 normal, ExplosionSpec spec, @Nullable ServerPlayer owner) {
      if (owner != null && !takeRate(owner, level.getGameTime())) {
         return BombExplosionService.Result.CANCELLED;
      } else {
         Ability ability = spec.ability();
         DamageSource source = DFDamageTypes.source(level, BomuBomu.EXPLOSION_DAMAGE, owner);
         boolean breaks = owner != null
            && spec.blockDamageScale() > 0.0F
            && (ability == null || ability.config().blockDamage())
            && TerrainDestruction.destructionAllowed(level);
         Explosion explosion = new Explosion(
            level,
            owner,
            source,
            null,
            pos.x,
            pos.y,
            pos.z,
            spec.radius(),
            false,
            breaks ? BlockInteraction.DESTROY : BlockInteraction.KEEP,
            ParticleTypes.EXPLOSION,
            ParticleTypes.EXPLOSION_EMITTER,
            SoundEvents.GENERIC_EXPLODE
         );
         if (EventHooks.onExplosionStart(level, explosion)) {
            return BombExplosionService.Result.CANCELLED;
         } else {
            RandomSource random = level.random;
            if (breaks) {
               explosion.getToBlow().addAll(rayBlocks(level, explosion, pos, spec.power() * spec.blockDamageScale(), random));
            }

            double reach = (double)(spec.radius() * 2.0F);
            List<Entity> entities = level.getEntities(owner, new AABB(pos.x - reach, pos.y - reach, pos.z - reach, pos.x + reach, pos.y + reach, pos.z + reach));
            EventHooks.onExplosionDetonate(level, explosion, entities, reach);
            Vec3 n = normal.lengthSqr() < 1.0E-6 ? UP : normal.normalize();
            BlockState struck = level.getBlockState(BlockPos.containing(pos.subtract(n.scale(0.6))));
            if (struck.isAir() && PhysicsWorld.active()) {
               List<StructureBlock> hull = PhysicsWorld.blocksIn(level, ImpactShape.sphere(pos.subtract(n.scale(0.6)), 0.9), null, 1);
               if (!hull.isEmpty()) {
                  struck = level.getBlockState(hull.get(0).localPos());
               }
            }

            if (struck.isAir()) {
               struck = level.getBlockState(BlockPos.containing(pos.x, pos.y - 1.0, pos.z));
            }

            int maxBlocks = (int)BomuBomu.setting("maxBlocksPerExplosion");
            CastBudget budget = new CastBudget(
               maxBlocks, Math.min((int)BomuBomu.setting("maxDebrisPerExplosion"), (Integer)CoreConfig.MAX_DEBRIS_ENTITIES_PER_CAST.get())
            );
            int broken = breaks ? breakTerrain(level, explosion, pos, spec, owner, budget, random) : 0;
            int structureBlocks = 0;
            if (owner != null && PhysicsWorld.active()) {
               PhysicsProfile profile = ability != null ? ability.physics() : PhysicsProfile.NONE;
               double strength = profile.strength((double)spec.entityDamage(), (double)spec.knockback());
               double chance = breaks ? (double)spec.blockDamageScale() * profile.blockDamage() : 0.0;
               structureBlocks = BlockImpactService.apply(
                  new BlockImpactService.Strike(
                     owner, BomuBomu.ID, PhysicsProfile.Impact.RADIAL, strength, chance, true, budget, BlockImpactService.Decal.SCORCH
                  ),
                  ImpactShape.sphere(pos, (double)spec.radius()),
                  n
               );
               if (structureBlocks > 0 && spec.fireChance() > 0.0F && breaks) {
                  BlockImpactService.ignite(owner, BomuBomu.ID, ImpactShape.sphere(pos, (double)spec.radius() * 0.7), (double)spec.fireChance(), budget);
               }

               if (structureBlocks >= 4 && chance > 0.0) {
                  BomuTriggers.SHIP_HOLE.get().trigger(owner);
               }
            }

            if (breaks && spec.fireChance() > 0.0F) {
               igniteTerrain(level, pos, spec, owner, random);
            }

            int hit = hurtEntities(level, explosion, pos, spec, owner, ability, entities);
            new BombExplosionPayload(pos, n, spec.vfxProfile(), spec.power(), random.nextLong(), Block.getId(struck))
               .send(level, owner != null ? owner.getId() : -1);
            return new BombExplosionService.Result(false, broken, structureBlocks, hit);
         }
      }
   }

   static Set<BlockPos> rayBlocks(ServerLevel level, Explosion explosion, Vec3 c, float energy, RandomSource random) {
      Set<BlockPos> set = new HashSet<>();
      if (energy <= 0.0F) {
         return set;
      } else {
         for (int j = 0; j < 16; j++) {
            for (int k = 0; k < 16; k++) {
               for (int l = 0; l < 16; l++) {
                  if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15) {
                     double dx = (double)j / 15.0 * 2.0 - 1.0;
                     double dy = (double)k / 15.0 * 2.0 - 1.0;
                     double dz = (double)l / 15.0 * 2.0 - 1.0;
                     double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                     dx /= len;
                     dy /= len;
                     dz /= len;
                     float f = energy * (0.7F + random.nextFloat() * 0.6F);
                     double x = c.x;
                     double y = c.y;
                     double z = c.z;

                     for (MutableBlockPos pos = new MutableBlockPos(); f > 0.0F; f -= 0.22500001F) {
                        pos.set(x, y, z);
                        if (!level.isInWorldBounds(pos)) {
                           break;
                        }

                        BlockState state = level.getBlockState(pos);
                        FluidState fluid = level.getFluidState(pos);
                        if (!state.isAir() || !fluid.isEmpty()) {
                           float resistance = Math.max(state.getExplosionResistance(level, pos, explosion), fluid.getExplosionResistance(level, pos, explosion));
                           f -= (resistance + 0.3F) * 0.3F;
                        }

                        if (f > 0.0F && !state.isAir()) {
                           set.add(pos.immutable());
                        }

                        x += dx * 0.3;
                        y += dy * 0.3;
                        z += dz * 0.3;
                     }
                  }
               }
            }
         }

         return set;
      }
   }

   private static int breakTerrain(
      ServerLevel level, Explosion explosion, Vec3 c, ExplosionSpec spec, ServerPlayer owner, CastBudget budget, RandomSource random
   ) {
      List<BlockPos> blocks = new ArrayList<>(explosion.getToBlow());
      blocks.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(c)));
      boolean permanent = BomuBomu.flag("permanentCraters");
      int broken = 0;

      for (BlockPos pos : blocks) {
         if (!budget.hasBlocks()) {
            break;
         }

         if (PhysicsWorld.structureAt(level, pos) == null) {
            Vec3 at = Vec3.atCenterOf(pos);
            Vec3 out = at.subtract(c);
            double d = out.length();
            Vec3 fly = d < 0.001
               ? UP.scale(0.4)
               : out.scale(1.0 / d).scale(0.25 + 0.35 * Math.max(0.0, 1.0 - d / (double)Math.max(1.0F, spec.radius()))).add(0.0, 0.25, 0.0);
            if (permanent) {
               BlockState state = level.getBlockState(pos);
               if (TerrainDestruction.canAffect(level, owner, pos, state, true) && budget.takeBlock()) {
                  if ((Boolean)CoreConfig.DROP_BLOCKS.get() && random.nextFloat() < 1.0F / Math.max(1.0F, spec.radius())) {
                     Block.dropResources(state, level, pos, null, owner, ItemStack.EMPTY);
                  }

                  state.onBlockExploded(level, pos, explosion);
                  if (budget.takeDebris() && random.nextFloat() < 0.6F) {
                     level.addFreshEntity(new DebrisEntity(level, at.x, (double)pos.getY(), at.z, state, fly));
                  }

                  broken++;
               }
            } else if (TerrainDestruction.tearOut(level, owner, pos, fly, budget, random.nextInt(40), true, false)) {
               broken++;
            }
         }
      }

      return broken;
   }

   private static void igniteTerrain(ServerLevel level, Vec3 c, ExplosionSpec spec, ServerPlayer owner, RandomSource random) {
      int r = Mth.ceil(spec.radius());
      int tries = Math.min(96, r * r * 3);

      for (int i = 0; i < tries; i++) {
         BlockPos pos = BlockPos.containing(
            c.x + (random.nextDouble() * 2.0 - 1.0) * (double)r,
            c.y + (random.nextDouble() * 2.0 - 1.0) * (double)r * 0.6,
            c.z + (random.nextDouble() * 2.0 - 1.0) * (double)r
         );
         if (random.nextFloat() < spec.fireChance() && level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
            TerrainDestruction.ignite(level, owner, pos, random.nextInt(40));
         }
      }
   }

   private static int hurtEntities(
      ServerLevel level, Explosion explosion, Vec3 c, ExplosionSpec spec, @Nullable ServerPlayer owner, @Nullable Ability ability, List<Entity> entities
   ) {
      double maxPlayer = ability != null ? ability.config().extra("maxPlayerDamage") : Double.MAX_VALUE;
      boolean coat = owner != null && ability != null && ability.coatable() && HakiCombat.coatsTechnique(owner);
      DamageSource source = DFDamageTypes.source(level, BomuBomu.EXPLOSION_DAMAGE, owner);
      int hit = 0;

      for (Entity e : entities) {
         if (e != owner && e.isAlive() && !e.isSpectator() && !(e instanceof AbilityEntity) && !(e instanceof DebrisEntity) && !e.ignoreExplosion(explosion)) {
            Vec3 aim = new Vec3(e.getX(), e.getY() + (double)e.getBbHeight() * 0.5, e.getZ());
            double d = aim.distanceTo(c);
            double push = 1.0 - d / ((double)spec.radius() * 1.25);
            if (!(push <= 0.0)) {
               float seen = Explosion.getSeenPercent(c, e);
               if (!(seen <= 0.0F)) {
                  boolean var10000;
                  label85: {
                     if (e instanceof Player p && BomuBomu.immune(p)) {
                        var10000 = true;
                        break label85;
                     }

                     var10000 = false;
                  }

                  boolean immune = var10000;
                  double f = Math.max(0.0, 1.0 - d / (double)spec.radius());
                  if (!immune && f > 0.0 && e instanceof LivingEntity && spec.entityDamage() > 0.0F) {
                     float amount = (float)((double)spec.entityDamage() * (f * f + f) * 0.5 * (double)seen);
                     if (e instanceof Player) {
                        amount = (float)Math.min((double)amount, maxPlayer);
                     }

                     if (hurt(owner, e, amount, source, coat)) {
                        hit++;
                     }
                  }

                  if (immune) {
                     BomuEvents.absorbed((ServerPlayer)e, c);
                     if (BomuBomu.flag("immuneToKnockback")) {
                        continue;
                     }
                  }

                  if (owner == null || Targeting.canPush(owner, e)) {
                     Vec3 dir = new Vec3(e.getX() - c.x, e.getEyeY() - c.y, e.getZ() - c.z);
                     if (dir.lengthSqr() < 1.0E-6) {
                        dir = UP;
                     }

                     double k = (double)spec.knockback() * push * (double)seen;
                     if (e instanceof LivingEntity living) {
                        k *= Math.max(0.0, 1.0 - living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE));
                     }

                     if (e instanceof ItemEntity) {
                        k *= 0.6;
                     }

                     Vec3 v = EventHooks.getExplosionKnockback(level, explosion, e, dir.normalize().scale(k).add(0.0, k * 0.25, 0.0));
                     if (!(v.lengthSqr() < 1.0E-8)) {
                        e.setDeltaMovement(e.getDeltaMovement().add(v));
                        e.hasImpulse = true;
                        if (e instanceof ServerPlayer sp) {
                           sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
                        } else {
                           e.hurtMarked = true;
                        }
                     }
                  }
               }
            }
         }
      }

      return hit;
   }

   private static boolean hurt(@Nullable ServerPlayer owner, Entity target, float amount, DamageSource source, boolean coat) {
      if (amount <= 0.0F) {
         return false;
      } else if (owner == null) {
         return target.hurt(source, amount);
      } else if (coat) {
         float coated = amount * ((Double)HakiConfig.ABILITY_MULTIPLIER.get()).floatValue();
         return HakiCombat.techniqueHurt(owner, () -> Targeting.hurt(owner, target, coated, BomuBomu.EXPLOSION_DAMAGE));
      } else {
         return Targeting.hurt(owner, target, amount, BomuBomu.EXPLOSION_DAMAGE);
      }
   }

   private static boolean takeRate(ServerPlayer owner, long now) {
      long[] r = RATE.computeIfAbsent(owner.getUUID(), k -> new long[2]);
      if (r[0] != now) {
         r[0] = now;
         r[1] = 0L;
      }

      if (r[1] >= (long)((int)BomuBomu.setting("maxExplosionsPerTickPerPlayer"))) {
         return false;
      } else {
         r[1]++;
         return true;
      }
   }

   public static void forget(UUID player) {
      RATE.remove(player);
   }

   public static void clear() {
      RATE.clear();
   }

   private BombExplosionService() {
   }

   public static record Result(boolean cancelled, int blocks, int structureBlocks, int entitiesHit) {
      public static final BombExplosionService.Result CANCELLED = new BombExplosionService.Result(true, 0, 0, 0);
   }
}
