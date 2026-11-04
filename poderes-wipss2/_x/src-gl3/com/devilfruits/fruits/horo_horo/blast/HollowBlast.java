package com.devilfruits.fruits.horo_horo.blast;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.AbilityEntity;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.DebrisEntity;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.horo_horo.HoroAbilities;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.devilfruits.fruits.horo_horo.HoroSounds;
import com.devilfruits.registry.DFDamageTypes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public final class HollowBlast {
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

   public static HollowBlast.Result detonate(ServerLevel level, Vec3 pos, HollowBlast.Spec spec, @Nullable ServerPlayer owner) {
      Ability ability = HoroAbilities.SPECIAL_HOLLOW;
      DamageSource source = DFDamageTypes.source(level, HoroHoro.BLAST_DAMAGE, owner);
      boolean breaks = owner != null && spec.blocksAllowed() && spec.blockPower() > 0.0F && TerrainDestruction.destructionAllowed(level);
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
         return new HollowBlast.Result(true, 0, 0, 0);
      } else {
         RandomSource random = level.random;
         if (breaks) {
            explosion.getToBlow().addAll(rayBlocks(level, explosion, pos, spec.blockPower(), random));
         }

         double reach = (double)spec.radius() * 1.5;
         List<Entity> entities = level.getEntities(owner, new AABB(pos.x - reach, pos.y - reach, pos.z - reach, pos.x + reach, pos.y + reach, pos.z + reach));
         EventHooks.onExplosionDetonate(level, explosion, entities, reach);
         CastBudget budget = new CastBudget(spec.maxBlocks(), (Integer)CoreConfig.MAX_DEBRIS_ENTITIES_PER_CAST.get());
         int broken = 0;
         if (breaks) {
            List<BlockPos> blocks = new ArrayList<>(explosion.getToBlow());
            blocks.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(pos)));

            for (BlockPos p : blocks) {
               if (!budget.hasBlocks()) {
                  break;
               }

               if (PhysicsWorld.structureAt(level, p) == null) {
                  Vec3 out = Vec3.atCenterOf(p).subtract(pos);
                  double d = out.length();
                  Vec3 fly = d < 0.001
                     ? UP.scale(0.4)
                     : out.scale(1.0 / d).scale(0.25 + 0.35 * Math.max(0.0, 1.0 - d / (double)spec.radius())).add(0.0, 0.3, 0.0);
                  if (TerrainDestruction.tearOut(level, owner, p, fly, budget, random.nextInt(40), true, false)) {
                     broken++;
                  }
               }
            }
         }

         int structureBlocks = 0;
         if (owner != null && PhysicsWorld.active()) {
            PhysicsProfile profile = ability.physics();
            double strength = profile.strength((double)spec.damage(), (double)spec.knockback());
            double chance = breaks ? profile.blockDamage() : 0.0;
            structureBlocks = BlockImpactService.apply(
               new BlockImpactService.Strike(owner, HoroHoro.ID, PhysicsProfile.Impact.RADIAL, strength, chance, true, budget, BlockImpactService.Decal.RING),
               ImpactShape.sphere(pos, (double)spec.radius()),
               UP
            );
         }

         int hit = hurtEntities(level, explosion, pos, spec, owner, source, entities);
         level.playSound(null, pos.x, pos.y, pos.z, HoroSounds.BLAST.get(), SoundSource.PLAYERS, 4.0F, 1.0F);
         return new HollowBlast.Result(false, broken, structureBlocks, hit);
      }
   }

   private static Set<BlockPos> rayBlocks(ServerLevel level, Explosion explosion, Vec3 c, float energy, RandomSource random) {
      Set<BlockPos> set = new HashSet<>();

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

   private static int hurtEntities(
      ServerLevel level, Explosion explosion, Vec3 c, HollowBlast.Spec spec, @Nullable ServerPlayer owner, DamageSource source, List<Entity> entities
   ) {
      int hit = 0;

      for (Entity e : entities) {
         if (e != owner && e.isAlive() && !e.isSpectator() && !(e instanceof AbilityEntity) && !(e instanceof DebrisEntity) && !e.ignoreExplosion(explosion)) {
            Vec3 aim = new Vec3(e.getX(), e.getY() + (double)e.getBbHeight() * 0.5, e.getZ());
            double d = aim.distanceTo(c);
            double f = 1.0 - d / (double)spec.radius();
            if (!(f <= 0.0)) {
               float seen = d < 1.5 ? 1.0F : Explosion.getSeenPercent(c, e);
               if (!(seen <= 0.0F)) {
                  if (e instanceof LivingEntity && spec.damage() > 0.0F) {
                     float amount = (float)((double)spec.damage() * (f * f + f) * 0.5 * (double)seen);
                     if (e instanceof Player) {
                        amount = Math.min(amount, spec.maxPlayerDamage());
                     }

                     boolean dealt = owner != null ? Targeting.hurt(owner, e, amount, HoroHoro.BLAST_DAMAGE) : e.hurt(source, amount);
                     if (dealt) {
                        hit++;
                     }
                  }

                  if (owner == null || Targeting.canPush(owner, e)) {
                     Vec3 dir = new Vec3(e.getX() - c.x, e.getEyeY() - c.y, e.getZ() - c.z);
                     if (dir.lengthSqr() < 1.0E-6) {
                        dir = UP;
                     }

                     double k = (double)spec.knockback() * f * (double)seen;
                     if (e instanceof LivingEntity living) {
                        k *= Math.max(0.0, 1.0 - living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE));
                     }

                     if (e instanceof ItemEntity) {
                        k *= 0.6;
                     }

                     Vec3 v = EventHooks.getExplosionKnockback(level, explosion, e, dir.normalize().scale(k).add(0.0, k * 0.3, 0.0));
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

   private HollowBlast() {
   }

   public static record Result(boolean cancelled, int blocks, int structureBlocks, int hit) {
   }

   public static record Spec(float radius, float blockPower, float damage, float knockback, int maxBlocks, float maxPlayerDamage, boolean blocksAllowed) {
   }
}
