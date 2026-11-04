package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GathersEffect implements Effect {
   private static final double DRIFT_SPEED = 0.35;
   private static final double ARRIVED = 0.6;

   @Override
   public String id() {
      return "gathers";
   }

   @Override
   public int unlockLevel() {
      return 2;
   }

   @Override
   public double defaultWeight() {
      return 5.0;
   }

   @Override
   public boolean hasLevel() {
      return true;
   }

   @Override
   public String numberUnit() {
      return "awakened.command.unit.blocks";
   }

   @Override
   public boolean supports(TargetKind kind) {
      return kind != TargetKind.NEAR;
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      double radius = (double)Math.max(1, level);
      Vec3 centre = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
      AABB box = new AABB(centre, centre).inflate(radius);
      double radiusSq = radius * radius;
      boolean moved = false;

      for (Entity loose : target.level().getEntities(target, box, GathersEffect::gatherable)) {
         Vec3 toBody = centre.subtract(loose.position());
         double distSq = toBody.lengthSqr();
         if (!(distSq > radiusSq) && !(distSq < 0.36)) {
            double dist = Math.sqrt(distSq);
            double speed = Math.min(0.35, dist * 0.5);
            loose.setDeltaMovement(toBody.scale(speed / dist));
            loose.hurtMarked = true;
            moved = true;
         }
      }

      return moved;
   }

   private static boolean gatherable(Entity entity) {
      return entity.isAlive() && (entity instanceof ItemEntity || entity instanceof ExperienceOrb);
   }
}
