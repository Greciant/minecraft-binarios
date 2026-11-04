package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class EventHorizonAbility extends Ability {
   public static final String ID = "event_horizon";
   public static final float COST = 100.0F;
   public static final int UNLOCK = 10;
   public static final double RANGE = 32.0;
   public static final String COLLAPSE_FEAT = "graviton_collapse";
   static final int SUSTAIN_TICKS = 120;
   static final double HEIGHT = 4.0;
   private static final double MIN_HEIGHT = 1.5;
   private static final double CEILING_CLEAR = 1.8;
   private static final double GROUND_SNAP = 2.0;
   private static final double AIR_CLEAR = 2.0;
   private final Map<UUID, EventHorizonField> holes = this.playerState();

   public EventHorizonAbility() {
      super("event_horizon", 100.0F, 10);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(12.0).build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.holes.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.graviton.horizon_busy", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         Vec3 center = aim(player, level);
         if (center == null) {
            return this.fail(player, "awakened.graviton.no_spot", new Object[0]);
         } else {
            float yaw = player.getYRot();
            EventHorizonEntity hole = EventHorizonVisuals.spawn(level, center, 120, yaw);
            String key = this.balanceKey();
            this.holes
               .put(
                  player.getUUID(),
                  new EventHorizonField(
                     level,
                     hole,
                     center,
                     MasteryScaling.scaledDamage(1.5F, player, key),
                     MasteryScaling.scaledDamage(2.5F, player, key),
                     MasteryScaling.scaledDamage(12.0F, player, key),
                     MasteryScaling.scaledDamage(40.0F, player, key)
                  )
               );
            return true;
         }
      }
   }

   @Nullable
   private static Vec3 aim(ServerPlayer player, ServerLevel level) {
      Vec3 look = player.getViewVector(1.0F);
      boolean struck = false;
      LivingEntity foe = AimUtil.aimedLivingEntity(player, 32.0, e -> Harm.allowed(player, e));
      Vec3 point;
      if (foe != null) {
         point = foe.position();
      } else {
         Vec3 eye = player.getEyePosition();
         Vec3 end = eye.add(look.scale(32.0));
         BlockHitResult hit = AimUtil.clipBlocks(level, player, eye, end);
         struck = hit.getType() != Type.MISS;
         point = struck ? hit.getLocation().subtract(look.scale(0.5)) : end;
      }

      Vec3 ground = AimUtil.groundedPositionIfFound(level, point).orElse(null);
      if (ground != null && point.y - ground.y <= 6.0) {
         return new Vec3(ground.x, ground.y + headroom(level, ground), ground.z);
      } else {
         Vec3 center = foe != null ? foe.getBoundingBox().getCenter() : (struck ? point.subtract(look.scale(1.5)) : point);
         BlockPos at = BlockPos.containing(center);
         return level.getBlockState(at).getCollisionShape(level, at).isEmpty() ? center : null;
      }
   }

   private static double headroom(ServerLevel level, Vec3 ground) {
      MutableBlockPos pos = BlockPos.containing(ground).mutable();
      int top = (int)Math.ceil(5.8);

      for (int dy = 0; dy <= top; dy++) {
         pos.setY((int)Math.floor(ground.y) + dy);
         if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            double ceiling = (double)pos.getY() - ground.y;
            return Math.max(1.5, Math.min(4.0, ceiling - 1.8));
         }
      }

      return 4.0;
   }

   @Override
   public void serverTick(ServerPlayer player) {
      EventHorizonField field = this.holes.get(player.getUUID());
      if (field != null) {
         if (!player.isAlive() || field.level() != player.serverLevel()) {
            field.release();
            this.holes.remove(player.getUUID());
         } else if (field.tick(player)) {
            StaminaSystem.suppressRegen(PowerService.data(player), player.serverLevel().getGameTime());
         } else {
            this.holes.remove(player.getUUID());
         }
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      EventHorizonField field = this.holes.remove(player.getUUID());
      if (field != null) {
         field.release();
      }

      super.clearPlayer(player);
   }
}
