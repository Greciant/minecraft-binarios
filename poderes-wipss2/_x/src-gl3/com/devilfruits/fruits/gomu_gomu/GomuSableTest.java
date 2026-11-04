package com.devilfruits.fruits.gomu_gomu;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.physics.PhysicsCommand;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.StructureBlock;
import com.devilfruits.registry.DFAttachments;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class GomuSableTest {
   static void register() {
      PhysicsCommand.registerTest("gomu_pistol", (p, s, b) -> cast(p, s, b, GomuAbilities.PISTOL));
      PhysicsCommand.registerTest("gomu_rocket", (p, s, b) -> cast(p, s, b, GomuAbilities.ROCKET));
      PhysicsCommand.registerTest("gomu_axe", (p, s, b) -> cast(p, s, b, GomuAbilities.AXE));
   }

   private static Component cast(ServerPlayer player, PhysicsStructure s, StructureBlock nearest, Ability ability) {
      player.lookAt(Anchor.EYES, nearest.worldCenter());
      AbilityContext ctx = new AbilityContext(new FormAbility(ability), player, DFAttachments.data(player), 1.0F);
      ability.activate(ctx);
      return Component.literal(
         ability.id().getPath()
            + " at structure "
            + s.id().toString().substring(0, 8)
            + " ("
            + String.format("%.1f", s.mass())
            + " kg, "
            + String.format("%.2f", s.velocityAt(s.centerOfMassWorld()).length())
            + " m/s before)"
      );
   }

   private GomuSableTest() {
   }
}
