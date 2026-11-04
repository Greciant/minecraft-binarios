package com.awakenedmod.awakened.content.stormcaller;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TempestAbility extends Ability {
   private static final float COST = 60.0F;
   private static final int UNLOCK = 9;
   private static final int DURATION_TICKS = 120;
   private static final int STRIKE_INTERVAL = 14;
   private static final double AREA_RADIUS = 10.0;
   private static final double CLOUD_HEIGHT = 8.0;
   private static final float DAMAGE = 7.0F;
   private static final double STRIKE_RADIUS = 2.0;
   private final Map<UUID, TempestAbility.TempestState> tempests = this.playerState();

   public TempestAbility() {
      super("tempest", 60.0F, 9);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(7.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.tempests.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         this.tempests.put(player.getUUID(), new TempestAbility.TempestState(120, player.position()));
         Fx.sound(player.serverLevel(), player.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 0.8F);
         PowerService.playAnimation(player, "tempest");
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      TempestAbility.TempestState tempest = this.tempests.get(player.getUUID());
      if (tempest != null) {
         ServerLevel level = player.serverLevel();
         StaminaSystem.suppressRegen(PowerService.data(player), level.getGameTime());
         tempest.center = player.position();
         Vec3 cloud = tempest.center.add(0.0, 8.0, 0.0);
         Fx.burst(level, ParticleTypes.LARGE_SMOKE, cloud, 12, 5.0, 0.01);
         Fx.burst(level, ParticleTypes.ELECTRIC_SPARK, cloud, 4, 4.0, 0.03);
         if (player.tickCount % 20 == 0) {
            StormFx.stormDome(level, tempest.center, 10.0, 8.0);
         }

         if ((120 - tempest.ticksLeft) % 100 == 0) {
            Fx.sound(level, tempest.center, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4F, 1.2F);
         }

         if (--tempest.strikeCooldown <= 0) {
            LivingEntity victim = randomEnemy(level, player, tempest.center);
            if (victim != null) {
               StormSupport.Result struck = StormSupport.strikeResult(
                  level, player, victim.position(), MasteryScaling.scaledDamage(7.0F, player, this.balanceKey()), 2.0
               );
               if (struck.threats() > 0) {
                  MasterySystem.award(player, MasteryReason.HIT);
               }
            }

            tempest.strikeCooldown = 14;
         }

         if (--tempest.ticksLeft <= 0) {
            this.tempests.remove(player.getUUID());
         }
      }
   }

   private static LivingEntity randomEnemy(ServerLevel level, ServerPlayer player, Vec3 center) {
      List<LivingEntity> foes = level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(10.0), e -> e != player && e.isAlive());
      return foes.isEmpty() ? null : foes.get(player.getRandom().nextInt(foes.size()));
   }

   private static final class TempestState {
      private int ticksLeft;
      private int strikeCooldown = 14;
      private Vec3 center;

      private TempestState(int ticksLeft, Vec3 center) {
         this.ticksLeft = ticksLeft;
         this.center = center;
      }
   }
}
