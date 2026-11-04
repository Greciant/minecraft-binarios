package com.awakenedmod.awakened.content.teleporter;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class PhantomFlurryAbility extends Ability {
   private static final float COST = 55.0F;
   private static final int UNLOCK = 10;
   private static final int DURATION_TICKS = 44;
   private static final int STRIKE_INTERVAL = 4;
   private static final double RANGE = 8.0;
   private static final double STRIKE_OFFSET = 1.1;
   private static final float DAMAGE = 4.0F;
   private static final double KNOCKBACK = 0.2;
   private final Map<UUID, PhantomFlurryAbility.FlurryState> flurries = this.activeState();

   public PhantomFlurryAbility() {
      super("phantom_flurry", 55.0F, 10);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(4.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.flurries.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         this.flurries.put(player.getUUID(), new PhantomFlurryAbility.FlurryState(44));
         ServerLevel level = player.serverLevel();
         PowerService.playAnimation(player, "phantom_flurry");
         TeleportSupport.puff(level, player.getEyePosition());
         Fx.sound(level, player.position(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.4F);
         player.invulnerableTime = Math.max(player.invulnerableTime, 49);
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      PhantomFlurryAbility.FlurryState flurry = this.flurries.get(player.getUUID());
      if (flurry != null) {
         ServerLevel level = player.serverLevel();
         player.invulnerableTime = Math.max(player.invulnerableTime, 4);
         Fx.burst(level, ParticleTypes.WITCH, player.position().add(0.0, 1.0, 0.0), 3, 0.3, 0.02);
         if (--flurry.strikeCooldown <= 0) {
            strike(player, level);
            flurry.strikeCooldown = 4;
         }

         if (--flurry.ticksLeft <= 0) {
            this.flurries.remove(player.getUUID());
            PowerService.stopAnimation(player);
            TeleportSupport.puff(level, player.getEyePosition());
         }
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.flurries.remove(player.getUUID()) != null) {
         PowerService.stopAnimation(player);
      }

      super.clearPlayer(player);
   }

   private static void strike(ServerPlayer player, ServerLevel level) {
      List<LivingEntity> targets = level.getEntitiesOfClass(
         LivingEntity.class, player.getBoundingBox().inflate(8.0), e -> e != player && e.isAlive() && Harm.allowed(player, e)
      );
      if (!targets.isEmpty()) {
         LivingEntity target = targets.get(player.getRandom().nextInt(targets.size()));
         Vec3 spot = null;

         for (int attempt = 0; attempt < 6; attempt++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
            Vec3 candidate = target.position().add(Math.cos(angle) * 1.1, 0.0, Math.sin(angle) * 1.1);
            if (TeleportSupport.fits(player, candidate)) {
               spot = candidate;
               break;
            }
         }

         if (spot != null) {
            TeleFx.ghost(level, player);
            player.teleportTo(spot.x, spot.y, spot.z);
            player.resetFallDistance();
         }

         Dmg.hurt(target, level, player.damageSources().playerAttack(player), MasteryScaling.scaledDamage(4.0F, player, "PhantomFlurryAbility"));
         target.knockback(0.2, player.getX() - target.getX(), player.getZ() - target.getZ());
         target.hurtMarked = true;
         player.swing(InteractionHand.MAIN_HAND);
         TeleFx.rift(level, target.position().add(0.0, 1.0, 0.0));
         Fx.sound(level, target.position(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, 1.6F);
         MasterySystem.award(player, MasteryReason.HIT, target);
      }
   }

   private static final class FlurryState {
      private int ticksLeft;
      private int strikeCooldown = 1;

      private FlurryState(int ticksLeft) {
         this.ticksLeft = ticksLeft;
      }
   }
}
