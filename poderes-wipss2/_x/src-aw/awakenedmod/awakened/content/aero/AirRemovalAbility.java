package com.awakenedmod.awakened.content.aero;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class AirRemovalAbility extends Ability {
   private static final float COST = 30.0F;
   private static final double RANGE = 20.0;
   private static final float BASE_TICK_DAMAGE_PER_SEC = 4.0F;
   private final Map<UUID, AirRemovalZone> activeDome = this.playerState();

   public AirRemovalAbility() {
      super("aero_air_removal", 30.0F, 10);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(4.0).build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      AirRemovalZone zone = this.activeDome.get(player.getUUID());
      super.clearPlayer(player);
      if (zone != null) {
         zone.end();
      }
   }

   @Override
   public boolean activate(ServerPlayer player) {
      UUID id = player.getUUID();
      if (this.activeDome.containsKey(id)) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         Vec3 center = AimUtil.aimedPosition(player, 20.0);
         float tickDamage = MasteryScaling.scaledDamage(4.0F, player, this.balanceKey());
         PowerService.playAnimation(player, "aero_air_removal_cast");
         AeroVfx.airRemoval(level, center);
         AirRemovalZone zone = new AirRemovalZone(level, center, player, tickDamage, () -> this.activeDome.remove(id));
         this.activeDome.put(id, zone);
         AeroAmbience.add(zone);
         return true;
      }
   }
}
