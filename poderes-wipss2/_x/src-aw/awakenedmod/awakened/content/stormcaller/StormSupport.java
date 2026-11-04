package com.awakenedmod.awakened.content.stormcaller;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class StormSupport {
   private StormSupport() {
   }

   public static int strike(ServerLevel level, ServerPlayer cause, Vec3 pos, float damage, double radius) {
      return strikeResult(level, cause, pos, damage, radius).hits();
   }

   public static StormSupport.Result strikeResult(ServerLevel level, ServerPlayer cause, Vec3 pos, float damage, double radius) {
      LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
      bolt.moveTo(pos.x, pos.y, pos.z);
      bolt.setVisualOnly(true);
      bolt.setCause(cause);
      level.addFreshEntity(bolt);
      StormFx.strikeImpact(level, pos, radius);
      boolean thunderhead = level.isThundering();
      int hits = 0;
      int threats = 0;

      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(radius), e -> e != cause && e.isAlive())) {
         if (Dmg.hurt(target, level, Dmg.by(cause, DamageTypes.LIGHTNING_BOLT), damage)) {
            target.hurtMarked = true;
            hits++;
            if (Foes.counts(cause, target)) {
               threats++;
            }

            if (thunderhead && Foes.defeatedBy(target, cause) && level.canSeeSky(target.blockPosition())) {
               QuestSystem.feat(cause, "storm_thunderhead");
            }
         }
      }

      return new StormSupport.Result(hits, threats);
   }

   public static record Result(int hits, int threats) {
   }
}
