package com.awakenedmod.awakened.content.pyrokinetic.npc;

import com.awakenedmod.awakened.content.pyrokinetic.BackdraftEngine;
import com.awakenedmod.awakened.content.pyrokinetic.PyroBurningGround;
import com.awakenedmod.awakened.core.npc.NpcContext;
import com.awakenedmod.awakened.core.npc.NpcMove;
import com.awakenedmod.awakened.core.npc.PoweredNpc;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

final class BackdraftMove extends NpcMove {
   private static final int COOLDOWN = 700;
   private static final int CLIP_TICKS = 64;
   private static final double CROWD_RADIUS = 8.0;
   private static final int CROWD = 3;
   private static final float LAST_STAND_HEALTH = 0.35F;
   private static final double LAST_STAND_RANGE = 4.0;
   private static final float CROWD_SCORE = 2.6F;
   private static final float LAST_STAND_SCORE = 2.0F;
   private static final float FIRE_BONUS = 0.1F;
   private static final float FIRE_BONUS_CAP = 1.0F;
   private static final int FIRE_SCAN = 8;
   @Nullable
   private BackdraftEngine.Cast cast;
   private int age;

   BackdraftMove() {
      super("backdraft", 10, 0.0, 8.0, 700);
   }

   @Override
   public float staminaCost() {
      return 35.0F;
   }

   @Override
   public String balanceKey() {
      return BackdraftEngine.BALANCE_KEY;
   }

   @Override
   public boolean needsLineOfSight() {
      return false;
   }

   @Override
   public boolean canStart(NpcContext ctx) {
      return BackdraftEngine.hasAir(ctx.caster());
   }

   @Override
   public boolean committed() {
      return true;
   }

   @Override
   public boolean usable(NpcContext ctx) {
      return ctx.target() != null && ctx.distance() <= 8.0;
   }

   @Override
   public double footworkWhileRunning() {
      return 0.0;
   }

   @Override
   public float score(NpcContext ctx) {
      LivingEntity target = ctx.target();
      if (target == null) {
         return 0.0F;
      } else {
         PoweredNpc npc = ctx.npc();
         float base;
         if (ctx.hostilesNear(npc.position(), 8.0) >= 3) {
            base = 2.6F;
         } else {
            if (!(ctx.healthFraction() < 0.35F) || !(ctx.distance() <= 4.0)) {
               return 0.0F;
            }

            base = 2.0F;
         }

         return base + Math.min(1.0F, 0.1F * (float)fires(npc));
      }
   }

   @Override
   public void start(NpcContext ctx) {
      this.cast = BackdraftEngine.begin(ctx.caster());
      this.age = 0;
      ctx.npc().showClip(PyroNpcKit.BACKDRAFT);
   }

   @Override
   public boolean tick(NpcContext ctx) {
      this.age++;
      BackdraftEngine.Cast c = this.cast;
      if (c != null) {
         if (BackdraftEngine.tick(ctx.caster(), c)) {
            c.takeBlast();
         } else {
            this.cast = null;
         }
      }

      return this.age < 64;
   }

   @Override
   public void stop(NpcContext ctx, boolean interrupted) {
      this.cast = null;
      super.stop(ctx, interrupted);
   }

   private static int fires(PoweredNpc npc) {
      ServerLevel level = (ServerLevel)npc.level();
      Vec3 center = npc.position();
      BlockPos base = npc.blockPosition();
      int count = 0;

      for (BlockPos pos : BlockPos.betweenClosed(base.offset(-8, -4, -8), base.offset(8, 4, 8))) {
         if (level.getBlockState(pos).is(BlockTags.FIRE)) {
            count++;
         }
      }

      count += level.getEntitiesOfClass(LivingEntity.class, npc.getBoundingBox().inflate(8.0), e -> e != npc && e.isOnFire()).size();
      return count + PyroBurningGround.patchesNear(level, center, 8.0);
   }
}
