package com.devilfruits.core.haki;

import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsConfig;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.physics.StructureBlock;
import com.devilfruits.core.physics.StructureForces;
import com.devilfruits.core.physics.StructureHit;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.TerrainDestruction;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class HakiStructures {
   static void ryuo(ServerPlayer player, float charge, @Nullable LivingEntity target) {
      if (PhysicsWorld.active() && PhysicsConfig.enabledFor(null)) {
         ServerLevel level = player.serverLevel();
         Vec3 look = player.getLookAngle();
         double strength = 40.0
            * (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (Double)HakiConfig.RYUO_MULTIPLIER.get() + 6.0)
            * (0.5 + (double)charge);
         if (target == null) {
            Vec3 eye = player.getEyePosition();
            Optional<StructureHit> hit = PhysicsWorld.bridge().raycast(level, eye, eye.add(look.scale(player.blockInteractionRange() + 0.5)), player);
            if (!hit.isEmpty() && BlockImpactService.allowed(player, hit.get().structure())) {
               StructureHit h = hit.get();
               StructureBlock block = new StructureBlock(h.structure(), h.localPos(), level.getBlockState(h.localPos()), h.worldPoint(), 1.0);
               StructureForces.strike(
                  player, h.structure(), List.of(block), ImpactShape.sphere(h.worldPoint(), 1.0), look, PhysicsProfile.Impact.DIRECTIONAL, strength
               );
               if (TerrainDestruction.destructionAllowed(level) && (Boolean)PhysicsConfig.BLOCK_DAMAGE.get()) {
                  double chance = (0.3 + 0.4 * (double)charge) * (1.0 + (Double)PhysicsConfig.ARMAMENT_BLOCK_BONUS.get());
                  BlockImpactService.apply(
                     new BlockImpactService.Strike(player, null, PhysicsProfile.Impact.NONE, 0.0, chance, false, new CastBudget(8, 4), null),
                     ImpactShape.sphere(h.worldPoint(), 1.5 + (double)charge),
                     look
                  );
               }

               BlockImpactService.decal(
                  level, h.structure(), h.localPos(), h.structure().dirToLocal(look.scale(-1.0)), BlockImpactService.Decal.HAKI, 1.5F + 2.0F * charge
               );
            }
         } else {
            PhysicsStructure under = PhysicsWorld.structureUnder(target);
            if (under != null && (Boolean)PhysicsConfig.IMPULSES.get() && BlockImpactService.allowed(player, under)) {
               Vec3 flat = look.multiply(1.0, 0.0, 1.0).lengthSqr() < 1.0E-6 ? Vec3.ZERO : look.multiply(1.0, 0.0, 1.0).normalize();
               Vec3 j = flat.add(0.0, -0.6, 0.0).normalize().scale(strength * 0.5 * (Double)PhysicsConfig.IMPULSE_MULTIPLIER.get());
               StructureForces.apply(under, target.position(), j);
               BlockPos local = BlockPos.containing(under.toLocal(target.position().add(0.0, -0.3, 0.0)));
               BlockImpactService.decal(level, under, local, under.dirToLocal(new Vec3(0.0, 1.0, 0.0)), BlockImpactService.Decal.HAKI, 1.5F + 2.0F * charge);
            }
         }
      }
   }

   static void clash(ServerLevel level, Player attacker, Player victim, float strength) {
      if (PhysicsWorld.active() && PhysicsConfig.enabledFor(null) && (Boolean)PhysicsConfig.IMPULSES.get()) {
         for (Player p : List.of(attacker, victim)) {
            PhysicsStructure s = PhysicsWorld.structureUnder(p);
            if (s != null) {
               Player other = p == attacker ? victim : attacker;
               Vec3 away = p.position().subtract(other.position()).multiply(1.0, 0.0, 1.0);
               away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
               double j = 160.0 * Math.max(0.2, (double)strength) * (Double)PhysicsConfig.IMPULSE_MULTIPLIER.get();
               StructureForces.apply(s, p.position(), away.scale(j).add(0.0, -j * 0.5, 0.0));
               BlockPos local = BlockPos.containing(s.toLocal(p.position().add(0.0, -0.3, 0.0)));
               BlockImpactService.decal(level, s, local, s.dirToLocal(new Vec3(0.0, 1.0, 0.0)), BlockImpactService.Decal.HAKI, 1.2F + strength);
            }
         }
      }
   }

   private HakiStructures() {
   }
}
