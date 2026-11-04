package com.awakenedmod.awakened.content.gamma;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.net.GammaRagePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GammaFx {
   public static final int GREEN = 7790650;
   public static final int RAGE_BROADCAST_INTERVAL = 8;
   private static final ResourceLocation GROWTH_ID = AwId.of("gamma_growth");
   public static final float GROWTH_MAX = 0.5F;

   private GammaFx() {
   }

   public static void broadcastRage(ServerPlayer player, float rage) {
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new GammaRagePayload(player.getId(), rage), new CustomPacketPayload[0]);
   }

   public static void ambientTick(ServerLevel level, ServerPlayer player, float rage) {
      if (!(rage < 0.45F)) {
         long time = level.getGameTime();
         int beatEvery = Math.max(10, 34 - (int)(rage * 22.0F));
         if (time % (long)beatEvery == 0L) {
            Fx.sound(level, player.position(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.35F + 0.45F * rage, 0.75F);
         }

         if (rage >= 0.7F && time % 10L == 0L && player.onGround()) {
            double a = level.random.nextDouble() * Math.PI * 2.0;
            double r = 0.4 + level.random.nextDouble() * 0.6;
            double x = player.getX() + Math.cos(a) * r;
            double z = player.getZ() + Math.sin(a) * r;
            Fx.Floor floor = Fx.floorState(level, x, player.getY(), z);
            if (floor != null) {
               level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), x, floor.topY() + 0.05, z, 3, 0.12, 0.05, 0.12, 0.08);
            }
         }
      }
   }

   public static void applyGrowth(ServerPlayer player, float rage) {
      AttributeInstance inst = player.getAttribute(Attributes.SCALE);
      if (inst != null) {
         inst.removeModifier(GROWTH_ID);
         double amount = (double)(0.5F * rage);
         if (amount > 0.001) {
            inst.addTransientModifier(new AttributeModifier(GROWTH_ID, amount, Operation.ADD_VALUE));
         }
      }
   }

   public static void clearGrowth(ServerPlayer player) {
      applyGrowth(player, 0.0F);
   }

   public static void provokePound(ServerLevel level, Player player, float frac) {
      Vec3 head = player.position().add(0.0, (double)player.getBbHeight() + 0.15, 0.0);
      Vec3 chest = player.position().add(0.0, (double)player.getBbHeight() * 0.6, 0.0);
      Vec3 feet = player.position().add(0.0, 0.1, 0.0);
      Vfx.ring(level, chest, 0.6 + 0.7 * (double)frac, 7790650, 7);
      Fx.burst(level, ParticleTypes.CLOUD, head, 2 + (int)(frac * 4.0F), 0.3, 0.02);
      Fx.burst(level, ParticleTypes.ANGRY_VILLAGER, chest, 2 + (int)(frac * 3.0F), 0.4, 0.0);
      Fx.burst(level, Fx.dust(7790650, 1.0F + frac), chest, 4 + (int)(frac * 4.0F), 0.4, 0.04);
      Fx.burst(level, ParticleTypes.POOF, feet, 3, 0.3, 0.02);
      Fx.sound(level, player.position(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.5F + 0.3F * frac, 0.7F);
      Fx.sound(level, player.position(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.35F + 0.4F * frac, 0.75F + 0.5F * frac);
      Fx.shake(level, player.position(), 8.0, 0.05F + 0.1F * frac, 2, 4);
   }

   public static void provoke(ServerLevel level, Player player, float rage) {
      Vec3 chest = player.position().add(0.0, (double)player.getBbHeight() * 0.6, 0.0);
      Vfx.dome(level, chest, 1.1 + 0.5 * (double)rage, 7790650, 8);
      Vfx.ring(level, player.position().add(0.0, 0.1, 0.0), 2.5 + 2.0 * (double)rage, 7790650, 10);
      Fx.burst(level, ParticleTypes.ANGRY_VILLAGER, chest, 3 + (int)(rage * 4.0F), 0.5, 0.02);
      Fx.burst(level, Fx.dust(7790650, 1.3F), chest, 14, 0.5, 0.06);
      Fx.sound(level, player.position(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 1.15F - 0.25F * rage);
      Fx.shake(level, player.position(), 14.0, 0.12F + 0.1F * rage, 2, 6);
   }

   public static void thunderclap(ServerLevel level, Player player, double reach, float rage) {
      Vec3 look = player.getLookAngle();
      Vec3 flat = new Vec3(look.x, 0.0, look.z).normalize();
      Vec3 chest = player.position().add(0.0, (double)player.getBbHeight() * 0.55, 0.0);
      Vec3 front = chest.add(flat.scale(reach * 0.22));
      Vfx.sphere(level, front, 0.35 + 0.06 * reach, 7790650, 6);
      Vfx.crescent(level, front.add(flat.scale(reach * 0.08)), reach * 0.55, 7790650, 9, -player.getYRot(), 0.0F);
      Vfx.ring(level, front.add(flat.scale(reach * 0.16)), reach * 0.35, 7790650, 7, -player.getYRot(), 90.0F);
      Vfx.effect(VfxEntity.Kind.RING, front.add(flat.scale(reach * 0.44)))
         .scale(reach * 0.5)
         .color(7790650)
         .lifetime(8)
         .orient(-player.getYRot(), 90.0F)
         .delay(3)
         .spawn(level);
      Fx.groundShockwave(level, player.position().add(flat.scale(reach * 0.2)), 0.6, reach, 8, false, 7790650);
      Fx.burst(level, ParticleTypes.EXPLOSION, front, 1, 0.0, 0.0);
      Fx.sound(level, front, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8F, 1.5F);
      Fx.sound(level, front, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 1.3F);
      Fx.shake(level, player.position(), 20.0, 0.18F + 0.14F * rage, 2, 10);
   }

   public static void leapChargeTick(ServerLevel level, Player player, float charge) {
      if (level.getGameTime() % 3L == 0L) {
         Fx.burst(level, ParticleTypes.CLOUD, player.position().add(0.0, 0.1, 0.0), 1 + (int)(charge * 3.0F), 0.4, 0.01);
         if (charge >= 1.0F && level.getGameTime() % 6L == 0L) {
            Fx.burst(level, Fx.dust(7790650, 1.2F), player.position().add(0.0, 0.3, 0.0), 4, 0.4, 0.03);
         }
      }
   }

   public static void leapLaunch(ServerLevel level, Player player, float rage) {
      Vec3 feet = player.position().add(0.0, 0.1, 0.0);
      Vfx.ring(level, feet, 1.8 + 1.0 * (double)rage, 7790650, 8);
      Vfx.pillar(level, feet, 0.6 + 0.3 * (double)rage, 1.4 + 0.6 * (double)rage, 7790650, 8);
      Fx.groundImpact(level, feet, 1.4 + 0.8 * (double)rage, false);
      Fx.burst(level, ParticleTypes.CLOUD, feet, 22, 0.8, 0.06);
      Fx.sound(level, feet, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.3F, 0.55F);
      Fx.sound(level, feet, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.55F);
      Fx.sound(level, feet, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
   }

   public static void leapLand(ServerLevel level, Vec3 pos, float rage, double power) {
      double radius = (2.2 + 2.0 * (double)rage) * power;
      Fx.groundImpact(level, pos, radius, true);
      Fx.groundShockwave(level, pos, 0.6, radius * 1.5, 9, false, 7790650);
      Vfx.ring(level, pos.add(0.0, 0.15, 0.0), radius * 1.2, 7790650, 10);
      Vfx.dome(level, pos, radius * 0.5, 7790650, 8);
      Fx.sound(level, pos, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 0.65F);
      Fx.shake(level, pos, (double)((int)(22.0 * power)), 0.24F + 0.16F * rage, 3, (int)(12.0 * power));
   }

   public static Block boulderMaterial(ServerLevel level, Vec3 groundPos) {
      MutableBlockPos pos = BlockPos.containing(groundPos.x, groundPos.y + 1.0, groundPos.z).mutable();

      for (int i = 0; i < 7; i++) {
         BlockState state = level.getBlockState(pos);
         if (!state.isAir() && state.getFluidState().isEmpty() && state.isCollisionShapeFullBlock(level, pos)) {
            return state.getBlock();
         }

         pos.move(0, -1, 0);
      }

      return Blocks.COBBLESTONE;
   }

   public static void boulderRip(ServerLevel level, Vec3 groundPos, Block material) {
      Fx.groundImpact(level, groundPos, 1.6, false);
      Fx.burst(level, new BlockParticleOption(ParticleTypes.BLOCK, material.defaultBlockState()), groundPos.add(0.0, 0.3, 0.0), 18, 0.6, 0.12);
      Fx.sound(level, groundPos, SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.3F, 0.55F);
      Fx.sound(level, groundPos, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.3F, 0.6F);
      Fx.sound(level, groundPos, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
   }

   public static void boulderShedTick(ServerLevel level, Vec3 chunkPos, Block material) {
      level.sendParticles(
         new BlockParticleOption(ParticleTypes.BLOCK, material.defaultBlockState()), chunkPos.x, chunkPos.y - 0.3, chunkPos.z, 2, 0.35, 0.1, 0.35, 0.02
      );
   }

   public static void boulderHurl(ServerLevel level, Vec3 pos) {
      Fx.sound(level, pos, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.3F, 0.5F);
      Fx.sound(level, pos, SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.5F, 1.4F);
   }

   public static void boulderFlightTick(ServerLevel level, Vec3 pos, Block material) {
      Fx.burst(level, new BlockParticleOption(ParticleTypes.BLOCK, material.defaultBlockState()), pos, 2, 0.2, 0.02);
      Fx.burst(level, ParticleTypes.CLOUD, pos, 1, 0.1, 0.01);
   }

   public static void boulderShatter(ServerLevel level, Vec3 pos, Block material, float rage) {
      BlockParticleOption spall = new BlockParticleOption(ParticleTypes.BLOCK, material.defaultBlockState());
      Fx.debrisFan(level, spall, pos.add(0.0, 0.2, 0.0), 32 + (int)(rage * 16.0F), 12.0, 65.0, 0.25, 0.6);
      Fx.burst(level, ParticleTypes.LARGE_SMOKE, pos, 10, 0.6, 0.03);
      Vfx.ring(level, pos, 2.0 + 1.2 * (double)rage, 7790650, 7);
      Fx.sound(level, pos, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 0.75F);
      Fx.sound(level, pos, SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.5F, 0.55F);
      Fx.shake(level, pos, 20.0, 0.2F + 0.12F * rage, 3, 10);
   }

   public static void slamGrab(ServerLevel level, LivingEntity victim) {
      Vec3 at = victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0);
      Fx.burst(level, ParticleTypes.CLOUD, at, 10, (double)victim.getBbWidth() * 0.6, 0.05);
      Fx.burst(level, Fx.dust(7790650, 1.2F), at, 8, 0.4, 0.04);
      Fx.sound(level, at, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.7F);
      Fx.sound(level, at, SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.6F, 1.3F);
   }

   public static void slamImpact(ServerLevel level, Vec3 pos, float rage) {
      Fx.groundImpact(level, pos, 1.6 + 1.2 * (double)rage, false);
      Vfx.ring(level, pos.add(0.0, 0.1, 0.0), 1.8 + 1.2 * (double)rage, 7790650, 6);
      Fx.sound(level, pos, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 0.7F);
      Fx.shake(level, pos, 18.0, 0.2F + 0.15F * rage, 2, 8);
   }

   public static void slamFinish(ServerLevel level, Vec3 pos, float rage) {
      double radius = 2.5 + 2.0 * (double)rage;
      Fx.groundImpact(level, pos, radius, true);
      Fx.groundShockwave(level, pos, 0.8, radius * 1.6, 10, false, 7790650);
      Vfx.dome(level, pos, radius * 0.7, 7790650, 10);
      Fx.shake(level, pos, 26.0, 0.3F + 0.15F * rage, 4, 14);
   }
}
