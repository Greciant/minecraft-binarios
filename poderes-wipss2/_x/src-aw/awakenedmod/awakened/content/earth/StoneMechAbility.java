package com.awakenedmod.awakened.content.earth;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.summoning.MinionSupport;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class StoneMechAbility extends Ability {
   private static final float DRAIN_PER_TICK = 0.15F;
   private static final int SYNC_EVERY = 4;

   public StoneMechAbility() {
      super("stone_mech", 40.0F, 10);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public boolean requiresFootingWhenRealistic() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (StoneMechControl.isPiloting(player)) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         Vec3 spot = MinionSupport.safeSpawnSpot(level, player.position(), AwakenedEntities.STONE_MECH.get());
         if (spot == null) {
            return this.fail(player, "ability.awakened.stone_mech.blocked", new Object[0]);
         } else {
            StoneMechEntity mech = new StoneMechEntity(AwakenedEntities.STONE_MECH.get(), level);
            mech.setPos(spot.x, spot.y, spot.z);
            float yaw = player.getYRot();
            mech.setYRot(yaw);
            mech.setYBodyRot(yaw);
            mech.setYHeadRot(yaw);
            mech.yRotO = yaw;
            mech.yBodyRotO = yaw;
            mech.yHeadRotO = yaw;
            level.addFreshEntity(mech);
            EarthFx.mechAssemble(level, spot);
            StoneMechControl.begin(player, mech);
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      if (StoneMechControl.isPiloting(player)) {
         StoneMechControl.tick(player);
         if (StoneMechControl.isPiloting(player)) {
            if (!StoneMechControl.isAssembling(player)) {
               PowerData data = PowerService.data(player);
               if (!data.infiniteStamina()) {
                  float s = data.stamina() - this.billed(0.15F);
                  if (s <= 0.0F) {
                     StoneMechControl.crumble(player);
                  } else {
                     data.setStamina(s);
                     if (player.tickCount % 4 == 0) {
                        PowerService.sync(player);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public boolean isActive(ServerPlayer player) {
      return StoneMechControl.isPiloting(player);
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      StoneMechControl.crumble(player);
      super.clearPlayer(player);
   }
}
