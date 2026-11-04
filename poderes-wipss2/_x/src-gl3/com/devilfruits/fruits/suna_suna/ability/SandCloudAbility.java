package com.devilfruits.fruits.suna_suna.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.logia.LogiaMovement;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.suna_suna.SandTravel;
import com.devilfruits.fruits.suna_suna.SunaFx;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class SandCloudAbility extends SandTechnique {
   public SandCloudAbility() {
      super(
         "sand_cloud",
         CastType.TOGGLE,
         AbilityStats.builder()
            .energy(10)
            .cooldown(40)
            .mastery(2)
            .damage(0.0)
            .range(0.0)
            .knockback(0.0)
            .extra("speed", 0.6, 0.1, 2.0, "Flying speed of the cloud, blocks per tick (sprinting: half as fast again).")
            .extra("drainPerSecond", 4.0, 0.0, 100.0, "Energy per second while it flies.")
            .extra("maxSeconds", 30.0, 1.0, 600.0, "Longest flight in seconds.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults())
         .impact(PhysicsProfile.Impact.NONE)
         .reaction(PhysicsProfile.Reaction.NONE)
         .blockDamage(0.0)
         .manual()
         .build();
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return LogiaMovement.session(player) == null && LogiaManager.bodyReady(player) ? null : Component.translatable("message.devilfruits.suna_suna.body_busy");
   }

   @Override
   public void onToggle(AbilityContext ctx, boolean active) {
      if (active && SandTravel.begin(ctx, this, SandTravel.CLOUD)) {
         ServerFx.sendNear(
            ctx.level(),
            SunaFx.CLOUD,
            ctx.player().getId(),
            ctx.player().position().add(0.0, 0.9, 0.0),
            Vec3.ZERO,
            (float)ctx.extra("speed"),
            ctx.seed(),
            0,
            64.0
         );
      }
   }
}
