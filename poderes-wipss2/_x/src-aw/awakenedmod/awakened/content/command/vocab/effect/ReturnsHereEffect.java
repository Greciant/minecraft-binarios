package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.TeleportGuard;
import com.awakenedmod.awakened.core.TeleportUtil;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

public final class ReturnsHereEffect implements Effect {
   private static final double NUDGE_RADIUS = 3.0;
   private static final int NUDGE_DROP = 4;
   private static final HoldMemory<ReturnsHereEffect.Home> HOMES = new HoldMemory<>();
   private static final Map<UUID, Long> CUSHIONED = new ConcurrentHashMap<>();
   private static final long CUSHION_TICKS = 600L;

   @Override
   public String id() {
      return "returns_here";
   }

   @Override
   public int unlockLevel() {
      return 7;
   }

   @Override
   public double defaultWeight() {
      return 15.0;
   }

   @Override
   public boolean supports(TargetKind kind) {
      return kind != TargetKind.NEAR;
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      return HOMES.take(target, age, () -> new ReturnsHereEffect.Home(target.level().dimension(), target.position(), caster.getUUID()));
   }

   @Override
   public void release(LivingEntity target) {
      ReturnsHereEffect.Home home = HOMES.release(target);
      if (home != null
         && target.isAlive()
         && target.level() instanceof ServerLevel world
         && world.dimension().equals(home.dimension())
         && casterOnline(world.getServer(), home.caster())) {
         Vec3 spot = TeleportUtil.safeToStand(target, home.pos()) ? home.pos() : TeleportUtil.nearestSafeSpot(target, home.pos(), 3.0, 4);
         Vec3 from = target.position();
         if (spot != null) {
            if (target instanceof ServerPlayer player && TeleportGuard.crosses(player, from, spot)) {
               return;
            }

            target.stopRiding();
            TeleportUtil.moveBody(target, spot);
            CUSHIONED.put(target.getUUID(), world.getGameTime());
            Fx.burst(world, ParticleTypes.REVERSE_PORTAL, from.add(0.0, (double)target.getBbHeight() * 0.5, 0.0), 20, 0.3, 0.1);
            Fx.sound(world, spot, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 0.9F);
         }
      }
   }

   @Override
   public void forget(UUID bodyId) {
      HOMES.forget(bodyId);
      CUSHIONED.remove(bodyId);
   }

   public static void onFall(LivingFallEvent event) {
      Long since = CUSHIONED.remove(event.getEntity().getUUID());
      if (since != null && event.getEntity().level().getGameTime() - since <= 600L) {
         event.setDamageMultiplier(0.0F);
      }
   }

   private static boolean casterOnline(MinecraftServer server, UUID casterId) {
      ServerPlayer caster = server.getPlayerList().getPlayer(casterId);
      return caster != null && !caster.hasDisconnected();
   }

   public static void forgetCaster(UUID casterId) {
      HOMES.forgetIf(home -> home.caster().equals(casterId));
   }

   public static void forgetAll() {
      CUSHIONED.clear();
      HOMES.clear();
   }

   private static record Home(ResourceKey<Level> dimension, Vec3 pos, UUID caster) {
   }
}
