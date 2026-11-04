package com.devilfruits.fruits.gomu_gomu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.gomu_gomu.GomuFx;
import com.devilfruits.fruits.gomu_gomu.GomuGomu;
import com.devilfruits.fruits.gomu_gomu.GomuSounds;
import com.devilfruits.fruits.gomu_gomu.GomuTriggers;
import com.devilfruits.fruits.gomu_gomu.strike.GatlingPath;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class GatlingAbility extends Ability {
   private static final ResourceLocation SLOW = GomuGomu.id("gatling_slow");
   private static final Map<UUID, GatlingAbility.Channel> CHANNELS = new ConcurrentHashMap<>();

   public GatlingAbility() {
      super(
         GomuGomu.id("gatling"),
         CastType.HELD,
         AbilityStats.builder()
            .energy(40)
            .cooldown(800)
            .mastery(10)
            .damage(8.0)
            .range(4.0)
            .knockback(2.4)
            .extra("maxChannelTicks", 120.0, 20.0, 600.0, "Longest Gatling in ticks.")
            .extra("gatlingHitDamage", 1.2, 0.0, 20.0, "Damage of each punch of the barrage.")
            .extra("gatlingDrainPerSecond", 12.0, 0.0, 200.0, "Energy the barrage burns every second.")
            .extra("gatlingSlow", 0.7, 0.0, 1.0, "How much the barrage slows the user (0.7: to 30 % of the walking speed).")
            .extra("gatlingChipBlocks", 0.0, 0.0, 1.0, "1: the punches chip blocks a little (like a Pistol's push only by default).")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).blockDamage(0.0).manual().build();
   }

   @Override
   public int maxChannelTicks() {
      return (int)this.config().extra("maxChannelTicks");
   }

   @Override
   public void onChannelStart(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      GatlingAbility.Channel channel = new GatlingAbility.Channel(ctx.level().getGameTime());
      CHANNELS.put(player.getUUID(), channel);
      AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null) {
         speed.removeModifier(SLOW);
         speed.addTransientModifier(new AttributeModifier(SLOW, -ctx.extra("gatlingSlow"), Operation.ADD_MULTIPLIED_TOTAL));
      }

      ServerFx.animate(player, ctx.animation(GomuGomu.id("gatling")));
      ServerFx.send(player, GomuFx.GATLING, player.position(), ctx.look(), 1.0F, channel.seed, 0);
   }

   @Override
   public boolean channelTick(AbilityContext ctx, int ticks) {
      ServerPlayer player = ctx.player();
      PlayerFruitData data = ctx.data();
      GatlingAbility.Channel channel = CHANNELS.get(player.getUUID());
      if (channel == null) {
         return false;
      } else {
         if (!AbilityManager.freeEnergy(player)) {
            data.setEnergy(Math.max(0.0F, data.energy() - (float)ctx.extra("gatlingDrainPerSecond") / 20.0F));
            if (data.energy() <= 0.0F) {
               return false;
            }
         }

         Vec3 aim = ctx.look();

         for (int k = 0; k < 2; k++) {
            int index = GatlingPath.first(ticks) + k;
            channel.launches.put(index, new GatlingAbility.Launch(GatlingPath.centre(player, player.position()), aim));
            int landing = index - 4;
            if (channel.launches.containsKey(landing)) {
               land(ctx, channel, landing, ctx.extra("gatlingHitDamage"), false);
               channel.launches.remove(landing - 8);
            }
         }

         if (ticks % 18 == 0) {
            ServerLevel level = ctx.level();
            level.playSound(null, player.getX(), player.getY() + 1.2, player.getZ(), GomuSounds.GATLING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
         }

         return true;
      }
   }

   @Override
   public void onChannelEnd(AbilityContext ctx, int ticks, boolean interrupted) {
      ServerPlayer player = ctx.player();
      GatlingAbility.Channel channel = CHANNELS.remove(player.getUUID());
      AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null) {
         speed.removeModifier(SLOW);
      }

      if (channel != null) {
         Vec3 aim = ctx.look();
         int index = GatlingPath.first(ticks + 1);
         channel.launches.put(index, new GatlingAbility.Launch(GatlingPath.centre(player, player.position()), aim));
         Vec3 at = land(ctx, channel, index, (double)ctx.damage(), true);
         if (channel.finisherLanded) {
            GomuTriggers.GATLING_FINISHED.get().trigger(player);
         }

         ServerFx.animate(player, ctx.animation(GomuGomu.id("gatling_finish")));
         ServerFx.send(player, GomuFx.GATLING_FINISH, at, aim, 1.0F, channel.seed, index);
         ctx.level().playSound(null, at.x, at.y, at.z, GomuSounds.BAZOOKA_HIT.get(), SoundSource.PLAYERS, 1.2F, 1.1F);
      }
   }

   static Vec3 land(AbilityContext ctx, GatlingAbility.Channel channel, int index, double damage, boolean finisher) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      GatlingAbility.Launch launch = channel.launches.get(index);
      Vec3 centre = launch.centre();
      Vec3 from = finisher ? centre : GatlingPath.origin(channel.seed, index, centre, launch.aim());
      Vec3 to = finisher ? centre.add(launch.aim().scale(4.0)) : GatlingPath.target(channel.seed, index, centre, launch.aim());
      BlockHitResult block = PhysicsWorld.clip(level, new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, player));
      Vec3 end = block.getType() == Type.BLOCK ? PhysicsWorld.worldLocation(level, block) : to;
      Entity target = null;
      double best = Double.MAX_VALUE;

      for (Entity e : Targeting.inCapsule(level, player, from, end, finisher ? 0.8 : 0.45)) {
         double d = e.getBoundingBox().getCenter().distanceToSqr(from);
         if (d < best) {
            best = d;
            target = e;
         }
      }

      if (target != null) {
         channel.finisherLanded |= finisher;
         target.invulnerableTime = 0;
         if (ctx.hurt(target, (float)damage)) {
            Vec3 push = launch.aim().multiply(1.0, 0.2, 1.0).normalize();
            Targeting.launch(target, finisher ? push.scale((double)ctx.knockback()).add(0.0, 0.45, 0.0) : target.getDeltaMovement().add(push.scale(0.06)));
         }

         target.invulnerableTime = 0;
         end = target.getBoundingBox().clip(from, end).orElse(target.getBoundingBox().getCenter());
      }

      if (block.getType() == Type.BLOCK || finisher) {
         channel.pushed += (float)damage;
         if (finisher || channel.pushed > 8.0F) {
            BlockImpactService.push(ctx, ImpactShape.sphere(end, finisher ? 1.4 : 0.8), launch.aim(), (double)channel.pushed * 0.5);
            channel.pushed = 0.0F;
         }
      }

      return end;
   }

   public static void forget(UUID player) {
      CHANNELS.remove(player);
   }

   public static boolean channeling(UUID player) {
      return CHANNELS.containsKey(player);
   }

   @Nullable
   static GatlingAbility.Channel channel(UUID player) {
      return CHANNELS.get(player);
   }

   static final class Channel {
      final long seed;
      final Map<Integer, GatlingAbility.Launch> launches = new HashMap<>();
      float pushed;
      boolean finisherLanded;

      Channel(long seed) {
         this.seed = seed;
      }
   }

   static record Launch(Vec3 centre, Vec3 aim) {
   }
}
