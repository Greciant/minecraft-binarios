package com.devilfruits.core.fx;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.network.AnimationPayload;
import com.devilfruits.core.network.ChargePayload;
import com.devilfruits.core.network.DFNetwork;
import com.devilfruits.core.network.FormStatePayload;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.registry.DFAttachments;
import java.util.Optional;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ServerFx {
   public static void send(Entity anchor, ResourceLocation fx, Vec3 pos, Vec3 dir, float power, long seed, int extra) {
      if (anchor.level() instanceof ServerLevel) {
         PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            anchor, new FxPayload(fx, anchor.getId(), pos, dir, power, seed, extra), new CustomPacketPayload[0]
         );
      }
   }

   public static void sendNear(ServerLevel level, ResourceLocation fx, int entityId, Vec3 pos, Vec3 dir, float power, long seed, int extra, double radius) {
      PacketDistributor.sendToPlayersNear(
         level, null, pos.x, pos.y, pos.z, radius, new FxPayload(fx, entityId, pos, dir, power, seed, extra), new CustomPacketPayload[0]
      );
   }

   public static void sendTo(ServerPlayer player, ResourceLocation fx, Entity subject, Vec3 pos, Vec3 dir, float power, long seed, int extra) {
      DFNetwork.sendTo(player, new FxPayload(fx, subject.getId(), pos, dir, power, seed, extra));
   }

   public static void simple(Entity anchor, ResourceLocation fx) {
      send(anchor, fx, anchor.position(), Vec3.ZERO, 1.0F, anchor.getRandom().nextLong(), 0);
   }

   public static void sendAround(
      ServerLevel level, Vec3 around, double radius, ResourceLocation fx, int entityId, Vec3 pos, Vec3 dir, float power, long seed, int extra
   ) {
      PacketDistributor.sendToPlayersNear(
         level, null, around.x, around.y, around.z, radius, new FxPayload(fx, entityId, pos, dir, power, seed, extra), new CustomPacketPayload[0]
      );
   }

   public static void blockBurst(ServerLevel level, Vec3 pos, BlockState state, float intensity) {
      blockBurst(level, pos, state, intensity, Vec3.ZERO);
   }

   public static void blockBurst(ServerLevel level, Vec3 pos, BlockState state, float intensity, Vec3 carried) {
      sendNear(level, CoreFx.BLOCK_BURST, -1, pos, carried, intensity, 0L, Block.getId(state), 64.0);
   }

   public static void shake(ServerLevel level, Vec3 pos, float strength, int radius) {
      sendNear(level, CoreFx.SHAKE, -1, pos, Vec3.ZERO, strength, 0L, radius, (double)radius);
   }

   public static void animate(ServerPlayer player, ResourceLocation animation) {
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new AnimationPayload(player.getId(), animation, false), new CustomPacketPayload[0]);
   }

   public static void stopAnimation(ServerPlayer player) {
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(
         player, new AnimationPayload(player.getId(), ResourceLocation.withDefaultNamespace("empty"), true), new CustomPacketPayload[0]
      );
   }

   public static void chargeStart(ServerPlayer player, Ability ability, float duration, long seed) {
      chargeStart(player, ability.id(), duration, seed);
   }

   public static void chargeStart(ServerPlayer player, ResourceLocation id, float duration, long seed) {
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(
         player, new ChargePayload(player.getId(), id, true, duration, seed, false), new CustomPacketPayload[0]
      );
   }

   public static void chargeStop(ServerPlayer player, Ability ability, boolean released) {
      chargeStop(player, ability.id(), released);
   }

   public static void chargeStop(ServerPlayer player, ResourceLocation id, boolean released) {
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new ChargePayload(player.getId(), id, false, 0.0F, 0L, released), new CustomPacketPayload[0]);
   }

   public static void chargeTo(ServerPlayer tracker, ServerPlayer user, Ability ability, float duration, long seed) {
      DFNetwork.sendTo(tracker, new ChargePayload(user.getId(), ability.id(), true, duration, seed, false));
   }

   public static void formState(ServerPlayer player) {
      formState(player, 0);
   }

   public static void formState(ServerPlayer player, int extraFlags) {
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, formPayload(player, extraFlags), new CustomPacketPayload[0]);
   }

   public static void formStateTo(ServerPlayer tracker, ServerPlayer user) {
      DFNetwork.sendTo(tracker, formPayload(user, 0));
   }

   private static FormStatePayload formPayload(ServerPlayer player, int extraFlags) {
      PlayerFruitData data = DFAttachments.data(player);
      return new FormStatePayload(player.getId(), Optional.ofNullable(data.fruitId()), data.formName(), data.flags() | extraFlags);
   }

   private ServerFx() {
   }
}
