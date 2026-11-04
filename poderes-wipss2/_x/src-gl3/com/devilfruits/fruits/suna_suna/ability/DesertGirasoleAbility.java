package com.devilfruits.fruits.suna_suna.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.AimHelper;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.suna_suna.Desiccation;
import com.devilfruits.fruits.suna_suna.SandFunnel;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import com.devilfruits.fruits.suna_suna.SunaTriggers;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class DesertGirasoleAbility extends SandTechnique {
   private static final Map<UUID, SandFunnel> FUNNELS = new HashMap<>();

   public DesertGirasoleAbility() {
      super(
         "desert_girasole",
         CastType.HELD,
         AbilityStats.builder()
            .energy(25)
            .cooldown(300)
            .mastery(4)
            .damage(2.0)
            .range(24.0)
            .knockback(0.0)
            .blockDamage(true)
            .extra("radius", 20.0, 3.0, 48.0, "Radius of the funnel once it has spread out, in blocks.")
            .extra("throat", 2.5, 1.0, 6.0, "Radius of the quicksand throat at the centre.")
            .extra("throatDepth", 3.0, 1.0, 6.0, "Depth of the quicksand throat in blocks.")
            .extra("pull", 0.055, 0.0, 0.5, "Pull towards the centre: velocity added each tick (strongest near the throat).")
            .extra("growSeconds", 1.5, 0.1, 10.0, "Seconds the funnel takes to spread out to its full radius.")
            .extra("maxSeconds", 10.0, 1.0, 60.0, "Longest time the funnel stays open.")
            .extra("drainPerSecond", 5.0, 0.0, 100.0, "Energy drained per second while the funnel is held open.")
            .extra("dehydrateSeconds", 4.0, 0.0, 60.0, "Seconds of Dehydrated for whoever is caught in the throat (up to level II).")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.TANGENTIAL).force(0.6).blockDamage(0.0).manual().build();
   }

   @Override
   public int maxChannelTicks() {
      return (int)(this.config().extra("maxSeconds") * 20.0);
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return ground(player, this.config().range()) == null ? Component.translatable("message.devilfruits.suna_suna.no_ground") : null;
   }

   @Nullable
   public static Vec3 ground(ServerPlayer player, double range) {
      ServerLevel level = player.serverLevel();
      Vec3 eye = player.getEyePosition();
      Vec3 end = eye.add(AimHelper.look(player).scale(range));
      BlockHitResult hit = PhysicsWorld.clip(level, new ClipContext(eye, end, Block.COLLIDER, Fluid.NONE, player));
      Vec3 at = hit.getType() == Type.MISS ? end : hit.getLocation();
      BlockPos surface = TerrainDestruction.findSurface(level, Mth.floor(at.x), Mth.floor(at.y), Mth.floor(at.z), 2, 6);
      return surface == null ? null : new Vec3(at.x, (double)surface.getY() + 1.0, at.z);
   }

   @Override
   public void onChannelStart(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      Vec3 centre = ground(player, (double)ctx.range());
      if (centre != null) {
         SandFunnel.Shape shape = new SandFunnel.Shape(
            ctx.extra("radius"), ctx.extra("throat"), (int)ctx.extra("throatDepth"), ctx.extra("pull"), Math.max(1, (int)(ctx.extra("growSeconds") * 20.0))
         );
         FUNNELS.put(player.getUUID(), SandFunnel.open(ctx.level(), player, centre, shape, Desiccation.budget(), ctx.seed(), this.maxChannelTicks()));
         ServerFx.animate(player, ctx.animation(SunaSuna.id("desert_girasole")));
         SunaTriggers.OPENED_GIRASOLE.get().trigger(player);
         sandArm(player, 20);
         ServerFx.shake(ctx.level(), centre, 0.25F, (int)(shape.radius() + 12.0));
      }
   }

   @Override
   public boolean channelTick(AbilityContext ctx, int ticks) {
      ServerPlayer player = ctx.player();
      SandFunnel funnel = FUNNELS.get(player.getUUID());
      if (funnel != null && player.level() == ctx.level()) {
         PlayerFruitData data = ctx.data();
         if (!AbilityManager.freeEnergy(player)) {
            data.setEnergy(Math.max(0.0F, data.energy() - (float)ctx.extra("drainPerSecond") / 20.0F));
            if (data.energy() <= 0.0F) {
               return false;
            }
         }

         if (ticks % 10 == 0) {
            sandArm(player, 20);
         }

         funnel.tick(ctx.damage(), (int)ctx.extra("dehydrateSeconds"));
         double r = Math.max(1.0, funnel.radius());
         BlockImpactService.push(
            ctx,
            ImpactShape.cylinder(funnel.centre().add(0.0, (double)(-funnel.shape().depth()), 0.0), r, (double)funnel.shape().depth() + 4.0),
            new Vec3(0.0, -1.0, 0.0),
            0.04,
            PhysicsProfile.Impact.TANGENTIAL
         );
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void onChannelEnd(AbilityContext ctx, int ticks, boolean interrupted) {
      SandFunnel funnel = FUNNELS.remove(ctx.player().getUUID());
      if (funnel != null) {
         funnel.close();
      }

      ServerFx.stopAnimation(ctx.player());
   }

   @Override
   public void onStartTracking(ServerPlayer user, ServerPlayer tracker) {
      SandFunnel funnel = FUNNELS.get(user.getUUID());
      if (funnel != null && !funnel.closed()) {
         funnel.send(tracker);
      }
   }

   @Nullable
   public static SandFunnel funnel(ServerPlayer player) {
      return FUNNELS.get(player.getUUID());
   }
}
