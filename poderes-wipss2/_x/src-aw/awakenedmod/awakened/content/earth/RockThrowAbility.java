package com.awakenedmod.awakened.content.earth;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.Terrain;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class RockThrowAbility extends Ability {
   private static final double FORWARD = 1.5;
   private static final double START_SINK = 0.6;
   private static final double HOVER_SPREAD = 0.9;
   private static final float HOVER_SIZE = 0.85F;
   private static final int MAX_HOVER = 3;
   private static final double AIM_RANGE = 26.0;
   private static final double MAX_REACH = 6.0;
   private static final int GRAVEL_PELLETS = 6;
   private static final double PUNCH_DIST = 3.5;
   private static final double PUNCH_FLAT_DOT = 0.6;
   private static final double FRONT_DOT = 0.5;
   private static final double[] RELOC_FORWARD = new double[]{1.5, 1.2, 0.95};
   private static final double[] RELOC_RISE = new double[]{0.0, -0.45, 0.45};
   private static final double MAX_LAUNCH_PITCH_DEG = 48.0;
   private static final double MAX_LAUNCH_TAN = Math.tan(Math.toRadians(48.0));
   private static final float FLAT_DAMAGE = 12.0F;
   private final Map<UUID, List<HoverRockEntity>> rocks = this.playerState();

   public RockThrowAbility() {
      super("rock_throw", 16.0F, 0);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(12.0).build();
   }

   @Override
   public boolean usesGripControls() {
      return true;
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return "rock_summon";
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (!EarthMaterial.standingOnEarth(player)) {
         return this.fail(player, "ability.awakened.rock_throw.airborne", new Object[0]);
      } else {
         List<HoverRockEntity> list = this.rocks.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
         prune(list);
         if (list.size() >= 3) {
            return this.fail(player, "ability.awakened.rock_throw.full", new Object[0]);
         } else {
            ServerLevel level = player.serverLevel();
            EarthMaterial mat = EarthMaterial.under(player);
            Vec3 fwd = flatLook(player);
            Vec3 right = new Vec3(-fwd.z, 0.0, fwd.x);
            int i = list.size();
            double lateral = (double)((i % 2 == 0 ? 1 : -1) * ((i + 1) / 2)) * 0.9;
            Vec3 hover = resolveHover(level, player, fwd, right, lateral);
            if (hover == null) {
               return this.fail(player, "ability.awakened.rock_throw.blocked", new Object[0]);
            } else {
               double gy = Terrain.floorNear(level, hover.x, hover.z, player.getY(), 1, 3);
               double groundY = Double.isNaN(gy) ? player.getY() : Math.min(gy, player.getY() + 1.0);
               double startY = Math.min(groundY, hover.y) - 0.6;
               Vec3 spawnPos = new Vec3(hover.x, startY, hover.z);
               list.add(HoverRockEntity.spawn(level, spawnPos, hover, mat, 0.85F, player.getUUID()));
               EarthFx.rockStamp(level, new Vec3(hover.x, groundY, hover.z), mat);
               Fx.shake(level, new Vec3(hover.x, groundY, hover.z), 8.0, 0.06F, 0, 6);
               return true;
            }
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      List<HoverRockEntity> list = this.rocks.get(player.getUUID());
      if (list != null && !list.isEmpty()) {
         ServerLevel level = player.serverLevel();
         double maxSq = 36.0;
         Vec3 eye = player.getEyePosition();
         list.removeIf(r -> {
            if (r != null && !r.isRemoved() && !r.launched()) {
               if (r.position().distanceToSqr(eye) > maxSq) {
                  EarthFx.rockCrumble(level, r.position(), r.material());
                  r.discard();
                  return true;
               } else {
                  return false;
               }
            } else {
               return true;
            }
         });
      }
   }

   @Override
   public void onSecondaryAction(ServerPlayer player, int action) {
      if (action == 0) {
         List<HoverRockEntity> list = this.rocks.get(player.getUUID());
         if (list != null) {
            prune(list);
            if (!list.isEmpty()) {
               Vec3 eye = player.getEyePosition();
               Vec3 look = player.getLookAngle();
               Vec3 flatLook = new Vec3(look.x, 0.0, look.z);
               if (!(flatLook.lengthSqr() < 1.0E-4)) {
                  flatLook = flatLook.normalize();
                  HoverRockEntity best = null;
                  double bestAlign = -Double.MAX_VALUE;

                  for (HoverRockEntity r : list) {
                     Vec3 to = r.position().subtract(eye);
                     Vec3 flatTo = new Vec3(to.x, 0.0, to.z);
                     double align = flatTo.lengthSqr() < 1.0E-4 ? 1.0 : flatTo.normalize().dot(flatLook);
                     if (align > bestAlign) {
                        bestAlign = align;
                        best = r;
                     }
                  }

                  if (best != null) {
                     if (!(bestAlign <= 0.6) && !(best.position().distanceToSqr(eye) > 12.25)) {
                        list.remove(best);
                        EarthMaterial mat = best.material();
                        Vec3 from = best.position();
                        Vec3 aim = AimUtil.aimedPosition(player, 26.0);
                        Vec3 dir = aim.subtract(from);
                        if (dir.lengthSqr() < 1.0E-4) {
                           dir = player.getLookAngle();
                        }

                        dir = clampLaunchPitch(dir.normalize(), player);
                        float dmg = MasteryScaling.scaledDamage(12.0F, player, this.balanceKey());
                        if (mat.pellets() > 0) {
                           fireShotgun(player.serverLevel(), player, from, dir, mat, dmg);
                           EarthFx.rockFling(player.serverLevel(), from, mat);
                           best.discard();
                        } else {
                           best.launch(dir, dmg, mat.launchSpeed(), this.balanceKey());
                        }

                        player.swing(InteractionHand.MAIN_HAND);
                        PowerService.playAnimation(player, "rock_punch");
                        Fx.shake(player.serverLevel(), player.position(), 6.0, 0.05F, 0, 6);
                     }
                  }
               }
            }
         }
      }
   }

   private static void fireShotgun(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 dir, EarthMaterial mat, float dmg) {
      for (int i = 0; i < 6; i++) {
         KineticProjectile pellet = new KineticProjectile(AwakenedEntities.KINETIC_CHARGE.get(), player, level, new ItemStack(mat.item()));
         pellet.setPos(from.x, from.y, from.z);
         pellet.configure(dmg * 0.33F, dmg * 0.12F, 1.5F, mat.knockback() * 0.6F);
         pellet.setGravity(0.04);
         pellet.shoot(dir.x, dir.y, dir.z, (float)mat.launchSpeed(), 7.0F);
         level.addFreshEntity(pellet);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      List<HoverRockEntity> list = this.rocks.remove(player.getUUID());
      if (list != null) {
         list.forEach(r -> {
            if (r != null && !r.launched()) {
               r.discard();
            }
         });
      }

      super.clearPlayer(player);
   }

   private static void prune(List<HoverRockEntity> list) {
      list.removeIf(r -> r == null || r.isRemoved() || r.launched());
   }

   private static Vec3 resolveHover(ServerLevel level, ServerPlayer player, Vec3 fwd, Vec3 right, double lateral) {
      Vec3 eye = player.getEyePosition();
      double baseY = player.getEyeY();

      for (double f : RELOC_FORWARD) {
         double cx = player.getX() + fwd.x * f + right.x * lateral;
         double cz = player.getZ() + fwd.z * f + right.z * lateral;

         for (double dy : RELOC_RISE) {
            Vec3 cand = new Vec3(cx, baseY + dy, cz);
            if (clearAndPunchable(level, player, eye, fwd, cand)) {
               return cand;
            }
         }
      }

      return null;
   }

   private static boolean clearAndPunchable(ServerLevel level, ServerPlayer player, Vec3 eye, Vec3 fwd, Vec3 cand) {
      double h = 0.425F;
      AABB box = new AABB(cand.x - h, cand.y - h, cand.z - h, cand.x + h, cand.y + h, cand.z + h);
      if (!level.noCollision(box)) {
         return false;
      } else {
         Vec3 to = cand.subtract(eye);
         double dist = to.length();
         if (!(dist < 1.0E-4) && !(dist > 3.5)) {
            double horiz = Math.sqrt(to.x * to.x + to.z * to.z);
            if (!(horiz < 1.0E-4) && !((to.x * fwd.x + to.z * fwd.z) / horiz < 0.5)) {
               ClipContext ctx = new ClipContext(eye, cand, Block.COLLIDER, Fluid.NONE, player);
               return level.clip(ctx).getType() != Type.BLOCK;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   private static Vec3 clampLaunchPitch(Vec3 dir, ServerPlayer player) {
      double h = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
      if (dir.y <= h * MAX_LAUNCH_TAN) {
         return dir;
      } else if (h < 1.0E-4) {
         Vec3 flat = flatLook(player);
         return new Vec3(flat.x, MAX_LAUNCH_TAN, flat.z).normalize();
      } else {
         return new Vec3(dir.x, h * MAX_LAUNCH_TAN, dir.z).normalize();
      }
   }

   private static Vec3 flatLook(ServerPlayer player) {
      Vec3 look = player.getLookAngle();
      Vec3 fwd = new Vec3(look.x, 0.0, look.z);
      return fwd.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : fwd.normalize();
   }
}
