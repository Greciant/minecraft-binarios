package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Harm;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class MarkHang {
   private static final ResourceLocation HANG_GRAVITY_ID = AwId.of("brawler_hang_gravity");
   private static final Map<Integer, MarkHang.Hang> HANGS = new ConcurrentHashMap<>();

   private MarkHang() {
   }

   public static void launch(ServerPlayer player, LivingEntity target, double height) {
      Vec3 look = player.getLookAngle();
      Harm.setMotion(player, target, look.x * 0.15, BrawlerTuning.launchSpeedFor(height), look.z * 0.15);
      target.resetFallDistance();
      applyGravity(target);
      HANGS.put(target.getId(), new MarkHang.Hang(target, height));
   }

   public static boolean hanging(LivingEntity target) {
      return HANGS.containsKey(target.getId());
   }

   public static int airTicks(LivingEntity target) {
      MarkHang.Hang hang = HANGS.get(target.getId());
      return hang == null ? -1 : hang.airTicks;
   }

   public static boolean rising(LivingEntity target) {
      MarkHang.Hang hang = HANGS.get(target.getId());
      return hang != null && hang.rising;
   }

   public static MarkHang.Step step(LivingEntity target) {
      MarkHang.Hang hang = HANGS.get(target.getId());
      if (hang == null) {
         return MarkHang.Step.NONE;
      } else if (target.isAlive() && !target.isRemoved()) {
         hang.airTicks++;
         target.resetFallDistance();
         if (hang.airTicks > 60) {
            return MarkHang.Step.LANDED;
         } else if (hang.rising) {
            if (hang.airTicks >= 2 && target.verticalCollision && !target.onGround()) {
               hang.rising = false;
               return MarkHang.Step.CEILING;
            } else if (!(target.getY() - hang.fromY >= hang.height) && !(target.getDeltaMovement().y <= 0.02) && hang.airTicks < 16) {
               return MarkHang.Step.RISING;
            } else {
               hang.rising = false;
               Vec3 v = target.getDeltaMovement();
               target.setDeltaMovement(v.x, 0.0, v.z);
               target.hurtMarked = true;
               return MarkHang.Step.APEX;
            }
         } else if (!target.onGround()) {
            return MarkHang.Step.HANGING;
         } else if (hang.bounced) {
            return MarkHang.Step.LANDED;
         } else {
            hang.bounced = true;
            hang.height *= 0.35;
            hang.fromY = target.getY();
            hang.rising = true;
            target.setDeltaMovement(target.getDeltaMovement().x, BrawlerTuning.launchSpeedFor(hang.height), target.getDeltaMovement().z);
            target.hurtMarked = true;
            target.resetFallDistance();
            return MarkHang.Step.BOUNCE;
         }
      } else {
         end(target);
         return MarkHang.Step.LANDED;
      }
   }

   public static void end(LivingEntity target) {
      if (HANGS.remove(target.getId()) != null) {
         removeGravity(target);
      }
   }

   private static void applyGravity(LivingEntity target) {
      AttributeInstance gravity = target.getAttribute(Attributes.GRAVITY);
      if (gravity != null) {
         gravity.removeModifier(HANG_GRAVITY_ID);
         gravity.addTransientModifier(new AttributeModifier(HANG_GRAVITY_ID, -0.75, Operation.ADD_MULTIPLIED_TOTAL));
      }
   }

   private static void removeGravity(LivingEntity target) {
      AttributeInstance gravity = target.getAttribute(Attributes.GRAVITY);
      if (gravity != null) {
         gravity.removeModifier(HANG_GRAVITY_ID);
      }
   }

   public static boolean ceilingWithin(LivingEntity target, double dist) {
      if (target.level() instanceof ServerLevel level) {
         Vec3 top = target.position().add(0.0, (double)target.getBbHeight() + 0.05, 0.0);
         Vec3 end = top.add(0.0, dist, 0.0);
         HitResult hit = level.clip(new ClipContext(top, end, Block.COLLIDER, Fluid.NONE, target));
         if (hit.getType() != Type.BLOCK) {
            return false;
         } else {
            BlockPos pos = BlockPos.containing(hit.getLocation().add(0.0, 0.05, 0.0));
            return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
         }
      } else {
         return false;
      }
   }

   @Nullable
   public static Vec3 nearestWall(LivingEntity target, double dist, @Nullable Vec3 preferred) {
      Vec3 best = null;
      double bestDist = Double.MAX_VALUE;
      List<Vec3> dirs = new ArrayList<>(5);
      if (preferred != null) {
         Vec3 flat = new Vec3(preferred.x, 0.0, preferred.z);
         if (flat.lengthSqr() > 1.0E-6) {
            dirs.add(flat.normalize());
         }
      }

      dirs.add(new Vec3(1.0, 0.0, 0.0));
      dirs.add(new Vec3(-1.0, 0.0, 0.0));
      dirs.add(new Vec3(0.0, 0.0, 1.0));
      dirs.add(new Vec3(0.0, 0.0, -1.0));

      for (Vec3 dir : dirs) {
         Vec3 hit = AimUtil.wallWithin(target, dir, dist);
         if (hit != null) {
            double d = hit.distanceTo(target.position());
            if (d < bestDist) {
               bestDist = d;
               best = dir;
            }
         }
      }

      return best;
   }

   public static void sweep(MinecraftServer server) {
      Iterator<Entry<Integer, MarkHang.Hang>> it = HANGS.entrySet().iterator();

      while (it.hasNext()) {
         LivingEntity target = it.next().getValue().target;
         if (!target.isAlive() || target.isRemoved()) {
            it.remove();
            removeGravity(target);
         }
      }
   }

   public static void clearAll() {
      for (MarkHang.Hang hang : HANGS.values()) {
         removeGravity(hang.target);
      }

      HANGS.clear();
   }

   private static final class Hang {
      final LivingEntity target;
      double height;
      double fromY;
      boolean rising = true;
      boolean bounced;
      int airTicks;

      Hang(LivingEntity target, double height) {
         this.target = target;
         this.height = height;
         this.fromY = target.getY();
      }
   }

   public static enum Step {
      NONE,
      RISING,
      APEX,
      CEILING,
      BOUNCE,
      HANGING,
      LANDED;
   }
}
