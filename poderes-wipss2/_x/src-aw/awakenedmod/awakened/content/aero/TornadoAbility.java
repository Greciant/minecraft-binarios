package com.awakenedmod.awakened.content.aero;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class TornadoAbility extends Ability {
   private static final float COST = 22.0F;
   private static final double RANGE = 20.0;
   private static final double FLING_BASE = 1.35;
   private static final float TICK_DAMAGE_BASE = 2.0F;
   private static final float FLING_DAMAGE_BASE = 4.0F;
   private final Map<UUID, List<TornadoZone>> zones = this.playerState();

   public TornadoAbility() {
      super("aero_tornado", 22.0F, 7);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(4.0).value("fling", AbilityScaling.Unit.RAW, 1.35).build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      List<TornadoZone> active = this.zones.get(player.getUUID());
      super.clearPlayer(player);
      if (active != null) {
         for (TornadoZone zone : active) {
            zone.end();
         }
      }
   }

   @Override
   public boolean activate(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 target = AimUtil.groundedPosition(player, 20.0, 24);
      if (target == null) {
         return this.fail(player, "awakened.feedback.aero_no_ground", new Object[0]);
      } else {
         double fling = MasteryScaling.scaled(1.35, player);
         float tickDamage = MasteryScaling.scaledDamage(2.0F, player, this.balanceKey());
         float flingDamage = MasteryScaling.scaledDamage(4.0F, player, this.balanceKey());
         PowerService.playAnimation(player, "aero_tornado_cast");
         AeroVfx.tornado(level, target);
         UUID id = player.getUUID();
         List<TornadoZone> active = this.zones.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>());
         TornadoZone zone = new TornadoZone(level, target, player, fling, tickDamage, flingDamage, ended -> {
            active.remove(ended);
            if (active.isEmpty()) {
               this.zones.remove(id, active);
            }
         });
         active.add(zone);
         AeroAmbience.add(zone);
         return true;
      }
   }
}
