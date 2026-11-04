package com.awakenedmod.awakened.content.stormcaller;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class LightningStrikeAbility extends Ability {
   private static final float COST = 30.0F;
   private static final int TELL_TICKS = 12;
   private static final double AIM_RANGE = 48.0;
   private static final float DAMAGE = 10.0F;
   private static final double HIT_RADIUS = 2.5;
   private final Map<UUID, LightningStrikeAbility.Tell> tells = this.activeState();

   public LightningStrikeAbility() {
      super("lightning_strike", 30.0F, 0);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(10.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.tells.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         LivingEntity locked = AimUtil.aimedLivingEntity(player, 48.0);
         Vec3 target = locked != null ? locked.position() : AimUtil.aimedPosition(player, 48.0);
         this.tells.put(player.getUUID(), new LightningStrikeAbility.Tell(12, target, locked));
         Fx.sound(player.serverLevel(), player.position(), (SoundEvent)SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.5F, 1.6F);
         PowerService.playAnimation(player, "lightning_strike");
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      LightningStrikeAbility.Tell tell = this.tells.get(player.getUUID());
      if (tell != null) {
         ServerLevel level = player.serverLevel();
         Vec3 aimPoint = tell.resolveTarget();
         StormFx.chargeOrb(level, aimPoint.add(0.0, 1.0, 0.0), 1.0F - (float)tell.ticksLeft / 12.0F);
         if (--tell.ticksLeft <= 0) {
            this.tells.remove(player.getUUID());
            StormSupport.Result struck = StormSupport.strikeResult(
               level, player, tell.resolveTarget(), MasteryScaling.scaledDamage(10.0F, player, this.balanceKey()), 2.5
            );
            if (struck.threats() > 0) {
               MasterySystem.award(player, MasteryReason.HIT);
            }
         }
      }
   }

   private static final class Tell {
      private int ticksLeft;
      private Vec3 target;
      private final LivingEntity locked;

      private Tell(int ticksLeft, Vec3 target, LivingEntity locked) {
         this.ticksLeft = ticksLeft;
         this.target = target;
         this.locked = locked;
      }

      private Vec3 resolveTarget() {
         if (this.locked != null && this.locked.isAlive()) {
            this.target = this.locked.position();
         }

         return this.target;
      }
   }
}
