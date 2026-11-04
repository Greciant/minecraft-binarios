package com.awakenedmod.awakened.content.pyrokinetic.npc;

import com.awakenedmod.awakened.content.pyrokinetic.BarrageEngine;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.npc.NpcCaster;
import com.awakenedmod.awakened.core.npc.NpcContext;
import com.awakenedmod.awakened.core.npc.NpcMove;
import com.awakenedmod.awakened.core.npc.PoweredNpc;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

final class BarrageMove extends NpcMove {
   private static final double MIN_RANGE = 8.0;
   private static final double MAX_RANGE = 28.0;
   private static final int COOLDOWN = 140;
   private static final double TOO_CLOSE = 7.0;
   private static final int COVER_MEMORY = 100;
   private static final int VOLLEY_MIN = 40;
   private static final int VOLLEY_MAX = 60;
   private static final int BUNCH = 2;
   private static final double BUNCH_RADIUS = 4.0;
   private static final double CAMPER_SPEED = 0.06;
   private static final double CAMPER_RANGE = 12.0;
   private static final float COVER_SCORE = 1.8F;
   private static final float BUNCH_SCORE = 1.4F;
   private static final float CAMPER_SCORE = 0.6F;
   private static final float HOT_STRAIN = 0.85F;
   private static final float LOW_STAMINA = 0.3F;
   private static final double OPEN_MIN = 6.0;
   private static final double OPEN_MAX = 18.0;
   private static final int LEAD_TICKS = 28;
   private static final double MAX_LEAD = 2.5;
   private static final float TURN = 12.0F;
   @Nullable
   private BarrageEngine.Volley volley;
   private int age;
   private int limit;
   private boolean coverShot;

   BarrageMove() {
      super("barrage", 7, 8.0, 28.0, 140);
   }

   @Override
   public float staminaCost() {
      return 6.0F;
   }

   @Override
   public String balanceKey() {
      return BarrageEngine.BALANCE_KEY;
   }

   @Override
   public boolean needsLineOfSight() {
      return false;
   }

   @Override
   public boolean inRange(NpcContext ctx) {
      Vec3 point = aimPoint(ctx);
      if (point == null) {
         return false;
      } else {
         double d = ctx.npc().position().distanceTo(point);
         return d >= this.minRange() && d <= this.maxRange();
      }
   }

   @Override
   public boolean canStart(NpcContext ctx) {
      Vec3 point = aimPoint(ctx);
      if (point != null && !(ctx.distance() < 7.0)) {
         NpcCaster caster = ctx.caster();
         caster.holdAim(point);
         boolean room = BarrageEngine.hasRoom(caster);
         caster.holdAim(null);
         return room;
      } else {
         return false;
      }
   }

   @Override
   public boolean usable(NpcContext ctx) {
      return ctx.target() != null && ctx.strain() < 0.85F && ctx.stamina() >= 0.3F && ctx.distance() >= 7.0;
   }

   @Override
   public float score(NpcContext ctx) {
      LivingEntity target = ctx.target();
      if (!this.usable(ctx) || target == null) {
         return 0.0F;
      } else if (!ctx.lineOfSight()) {
         return ctx.lastSeenPos() != null && ctx.unseenTicks() <= 100 ? 1.8F : 0.0F;
      } else if (ctx.hostilesNear(target.position(), 4.0) >= 2) {
         return 1.4F;
      } else {
         return ctx.targetSpeed() < 0.06 && ctx.distance() >= 12.0 ? 0.6F : 0.0F;
      }
   }

   @Override
   public void start(NpcContext ctx) {
      this.coverShot = !ctx.tracking();
      this.volley = new BarrageEngine.Volley();
      this.age = 0;
      float t = Mth.clamp((float)(ctx.npc().mastery() - this.unlockLevel()) / 5.0F, 0.0F, 1.0F);
      this.limit = Math.round(Mth.lerp(t, 40.0F, 60.0F));
      aim(ctx);
      BarrageEngine.open(ctx.caster());
      ctx.npc().showClip(PyroNpcKit.BARRAGE);
   }

   @Override
   public boolean tick(NpcContext ctx) {
      PoweredNpc npc = ctx.npc();
      if (this.volley == null || ++this.age > this.limit || !aim(ctx) || ctx.strain() >= 1.0F) {
         return false;
      } else if (ctx.target() != null && ctx.distance() < 7.0) {
         return false;
      } else if (this.coverShot && ctx.lineOfSight() && ctx.distance() >= 6.0 && ctx.distance() <= 18.0) {
         return false;
      } else if (!npc.stamina().canAfford(0.6F, this.balanceKey())) {
         return false;
      } else {
         switch (BarrageEngine.tick(ctx.caster(), this.volley)) {
            case END:
               return false;
            case BILL:
               npc.stamina().drain(0.6F, this.balanceKey(), npc.level().getGameTime());
            case SKIP:
            default:
               return true;
         }
      }
   }

   @Override
   public void stop(NpcContext ctx, boolean interrupted) {
      if (this.volley != null) {
         BarrageEngine.close(ctx.caster());
         this.volley = null;
      }

      ctx.caster().holdAim(null);
      super.stop(ctx, interrupted);
   }

   @Override
   public double footworkWhileRunning() {
      return 0.25;
   }

   private static boolean aim(NpcContext ctx) {
      Vec3 point = aimPoint(ctx);
      if (point == null) {
         return false;
      } else {
         ctx.caster().holdAim(point);
         ctx.npc().aimAt(point, 12.0F);
         return true;
      }
   }

   @Nullable
   private static Vec3 aimPoint(NpcContext ctx) {
      LivingEntity target = ctx.target();
      Vec3 spot;
      if (target != null && ctx.tracking()) {
         double lead = (double)(28.0F * ctx.skill().leadAccuracy());
         Vec3 v = ctx.targetVelocity();
         Vec3 ahead = new Vec3(v.x * lead, 0.0, v.z * lead);
         if (ahead.lengthSqr() > 6.25) {
            ahead = ahead.normalize().scale(2.5);
         }

         spot = target.position().add(ahead);
      } else {
         if (ctx.lastSeenPos() == null || ctx.unseenTicks() > 160) {
            return null;
         }

         spot = ctx.lastSeenPos();
      }

      ServerLevel level = ctx.caster().level();
      return AimUtil.groundedPositionIfFound(level, spot.add(0.0, 1.0, 0.0)).orElse(spot);
   }
}
