package com.awakenedmod.awakened.core.npc;

import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.caster.Caster;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class NpcCaster implements Caster {
   private final PoweredNpc npc;
   @Nullable
   private Vec3 heldAim;

   NpcCaster(PoweredNpc npc) {
      this.npc = npc;
   }

   public void holdAim(@Nullable Vec3 point) {
      this.heldAim = point;
   }

   public PoweredNpc npc() {
      return this.npc;
   }

   @Override
   public LivingEntity body() {
      return this.npc;
   }

   @Override
   public ServerLevel level() {
      return (ServerLevel)this.npc.level();
   }

   @Override
   public int masteryLevel() {
      return this.npc.mastery();
   }

   @Override
   public Vec3 aimDir() {
      return this.npc.getViewVector(1.0F);
   }

   @Override
   public float yaw() {
      return this.npc.getYHeadRot();
   }

   @Override
   public float pitch() {
      return this.npc.getXRot();
   }

   @Override
   public Vec3 aimPoint(double range) {
      return this.heldAim != null ? this.heldAim : Caster.super.aimPoint(range);
   }

   @Nullable
   @Override
   public LivingEntity aimTarget(double range) {
      if (this.heldAim != null) {
         return null;
      } else {
         LivingEntity target = this.npc.getTarget();
         return target != null && target.isAlive() && this.npc.distanceToSqr(target) <= range * range ? target : null;
      }
   }

   @Override
   public boolean canHarm(Entity target) {
      if (target != this.npc && target.isAlive()) {
         if (target instanceof PoweredNpc other && other.kit() == this.npc.kit()) {
            return false;
         }

         return Harm.allowed(this.npc, target);
      } else {
         return false;
      }
   }

   @Override
   public SoundSource soundSource() {
      return SoundSource.HOSTILE;
   }

   @Override
   public float var(String key, float fallback) {
      return this.npc.var(key, fallback);
   }

   @Override
   public void setVar(String key, float value) {
      this.npc.setVar(key, value);
   }
}
