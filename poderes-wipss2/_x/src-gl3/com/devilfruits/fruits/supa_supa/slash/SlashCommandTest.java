package com.devilfruits.fruits.supa_supa.slash;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.StructureBlock;
import com.devilfruits.fruits.supa_supa.SupaAbilities;
import com.devilfruits.registry.DFAttachments;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class SlashCommandTest {
   private SlashCommandTest() {
   }

   public static Component run(ServerPlayer player, PhysicsStructure s, StructureBlock nearest) {
      AbilityContext ctx = new AbilityContext(new FormAbility(SupaAbilities.SUPER_SLASH), player, DFAttachments.data(player), 1.0F);
      Vec3 target = nearest.worldCenter();
      Vec3 flat = new Vec3(target.x - player.getX(), 0.0, target.z - player.getZ());
      Vec3 dir = flat.lengthSqr() < 1.0E-4 ? ctx.horizontalLook() : flat.normalize();
      int height = (int)ctx.extra("height");
      Vec3 base = new Vec3(player.getX(), Math.floor(target.y) - (double)(height / 2), player.getZ()).add(dir.scale(1.2));
      SlashStructures.Survey v = SlashStructures.survey(player, ctx, s, new Vec3(target.x, base.y, target.z), dir, new Vec3(0.0, 1.0, 0.0));
      SuperSlash.launch(ctx, 1.0F, base, dir);
      SlashPlane p = v.plane();
      String axis = p.along().getName().toUpperCase(Locale.ROOT) + (p.sign() > 0 ? "+" : "-");
      return Component.translatable(
         "commands.devilfruits.supa_supa.sable_test",
         new Object[]{
            s.id().toString().substring(0, 8),
            String.format("%.1f", s.mass()),
            v.blocks(),
            v.minus(),
            v.plus(),
            axis,
            p.across().getName(),
            p.boundary(),
            v.refusal().name().toLowerCase(Locale.ROOT)
         }
      );
   }
}
