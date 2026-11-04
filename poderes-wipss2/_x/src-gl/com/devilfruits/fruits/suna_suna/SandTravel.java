package com.devilfruits.fruits.suna_suna;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.logia.LogiaMovement;
import com.devilfruits.core.logia.LogiaMovementMode;
import com.devilfruits.registry.DFAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;

public final class SandTravel {
   private static final ResourceLocation FLIGHT_ID = SunaSuna.id("sand_cloud_flight");
   public static final String CONTEXT = "context";
   public static final LogiaMovementMode CLOUD = new LogiaMovementMode() {
      @Override
      public ResourceLocation id() {
         return SunaSuna.id("sand_cloud");
      }

      @Override
      public void start(ServerPlayer player, LogiaMovement.Session session) {
         AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
         if (flight != null) {
            flight.addOrUpdateTransientModifier(new AttributeModifier(SandTravel.FLIGHT_ID, 1.0, Operation.ADD_VALUE));
         }
      }

      @Override
      public boolean tick(ServerPlayer player, LogiaMovement.Session session) {
         player.fallDistance = 0.0F;
         return SandTravel.toggled(player, SunaAbilities.SAND_CLOUD);
      }

      @Override
      public void end(ServerPlayer player, LogiaMovement.Session session, boolean interrupted) {
         AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
         if (flight != null) {
            flight.removeModifier(SandTravel.FLIGHT_ID);
         }

         if (player.getAbilities().flying && !player.mayFly()) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
         }

         ServerFx.sendNear(player.serverLevel(), SunaFx.CLOUD_END, player.getId(), player.position(), Vec3.ZERO, 1.0F, 0L, 0, 64.0);
         AbilityManager.stopToggle(player, SunaAbilities.SAND_CLOUD);
      }

      @Override
      public float drainPerSecond() {
         return (float)SunaAbilities.SAND_CLOUD.config().extra("drainPerSecond");
      }

      @Override
      public int maxTicks() {
         return (int)(SunaAbilities.SAND_CLOUD.config().extra("maxSeconds") * 20.0);
      }

      @Override
      public boolean forcesBody() {
         return true;
      }
   };

   public static boolean begin(AbilityContext ctx, Ability ability, LogiaMovementMode mode) {
      ServerPlayer player = ctx.player();
      if (!LogiaMovement.start(player, mode)) {
         AbilityManager.stopToggle(player, ability);
         return false;
      } else {
         LogiaMovement.session(player).data.put("context", ctx);
         return true;
      }
   }

   public static void register() {
      LogiaMovement.register(CLOUD);
   }

   private static boolean toggled(ServerPlayer player, Ability ability) {
      return DFAttachments.data(player).isToggleActive(ability.id());
   }

   private SandTravel() {
   }
}
