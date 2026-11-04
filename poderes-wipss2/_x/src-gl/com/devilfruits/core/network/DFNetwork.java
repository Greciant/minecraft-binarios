package com.devilfruits.core.network;

import com.devilfruits.client.ClientPacketHandlers;
import com.devilfruits.client.gacha.CasketRevealClient;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AimHelper;
import com.devilfruits.core.form.ChargedJump;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.gacha.CasketRevealPayload;
import com.devilfruits.core.haki.HakiManager;
import com.devilfruits.core.logia.LogiaDash;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.logia.LogiaState;
import com.devilfruits.core.poneglyph.PoneglyphNetwork;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class DFNetwork {
   public static final String PROTOCOL = "10";

   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("10");
      registrar.playToServer(AbilityInputPayload.TYPE, AbilityInputPayload.STREAM_CODEC, (payload, ctx) -> {
         if (ctx.player() instanceof ServerPlayer player) {
            AbilityManager.handleInput(player, payload);
         }
      });
      registrar.playToServer(FormInputPayload.TYPE, FormInputPayload.STREAM_CODEC, (payload, ctx) -> {
         if (ctx.player() instanceof ServerPlayer player) {
            switch (payload.action()) {
               case CYCLE:
                  FormManager.cycle(player);
                  break;
               case REVERT:
                  FormManager.revert(player, FormManager.Reason.PLAYER);
                  break;
               case SET:
                  FormManager.request(player, payload.form(), FormManager.Reason.PLAYER);
                  break;
               case BOOST_START:
               case BOOST_STOP:
                  FormManager.setBoosting(player, payload.action() == FormInputPayload.Action.BOOST_START);
            }
         }
      });
      registrar.playToServer(LogiaInputPayload.TYPE, LogiaInputPayload.STREAM_CODEC, (payload, ctx) -> {
         if (ctx.player() instanceof ServerPlayer player) {
            switch (payload.action()) {
               case ELEMENTAL_ON:
                  LogiaManager.setElemental(player, true);
                  break;
               case ELEMENTAL_OFF:
                  LogiaManager.setElemental(player, false);
                  break;
               case ELEMENTAL_TOGGLE:
                  LogiaManager.setElemental(player, LogiaManager.state(player) != LogiaState.ELEMENTAL_BODY);
                  break;
               case DASH:
                  LogiaDash.dash(player);
                  break;
               case INTANGIBILITY_TOGGLE:
                  LogiaManager.setIntangible(player, !LogiaManager.intangible(player));
            }
         }
      });
      registrar.playToServer(AimPayload.TYPE, AimPayload.STREAM_CODEC, (payload, ctx) -> {
         if (ctx.player() instanceof ServerPlayer player) {
            AimHelper.receive(player, payload);
         }
      });
      registrar.playToServer(JumpInputPayload.TYPE, JumpInputPayload.STREAM_CODEC, (payload, ctx) -> {
         if (ctx.player() instanceof ServerPlayer player) {
            ChargedJump.handle(player, payload);
         }
      });
      registrar.playToServer(HakiInputPayload.TYPE, HakiInputPayload.STREAM_CODEC, (payload, ctx) -> {
         if (ctx.player() instanceof ServerPlayer player) {
            switch (payload.action()) {
               case TOGGLE:
                  HakiManager.toggle(player);
                  break;
               case SELECT:
                  HakiManager.select(player, payload.mask());
                  break;
               case RYUO_START:
                  HakiManager.ryuoStart(player);
                  break;
               case RYUO_RELEASE:
                  HakiManager.ryuoRelease(player);
            }
         }
      });
      PoneglyphNetwork.register(registrar);
      registrar.playToClient(FxPayload.TYPE, FxPayload.STREAM_CODEC, (payload, ctx) -> ClientPacketHandlers.handleFx(payload));
      registrar.playToClient(AnimationPayload.TYPE, AnimationPayload.STREAM_CODEC, (payload, ctx) -> ClientPacketHandlers.handleAnimation(payload));
      registrar.playToClient(AbilityTablePayload.TYPE, AbilityTablePayload.STREAM_CODEC, (payload, ctx) -> ClientPacketHandlers.handleAbilityTable(payload));
      registrar.playToClient(ChargePayload.TYPE, ChargePayload.STREAM_CODEC, (payload, ctx) -> ClientPacketHandlers.handleCharge(payload));
      registrar.playToClient(FormStatePayload.TYPE, FormStatePayload.STREAM_CODEC, (payload, ctx) -> ClientPacketHandlers.handleFormState(payload));
      registrar.playToClient(LogiaStatePayload.TYPE, LogiaStatePayload.STREAM_CODEC, (payload, ctx) -> ClientPacketHandlers.handleLogiaState(payload));
      registrar.playToClient(LogiaHitPayload.TYPE, LogiaHitPayload.STREAM_CODEC, (payload, ctx) -> ClientPacketHandlers.handleLogiaHit(payload));
      registrar.playToClient(CasketRevealPayload.TYPE, CasketRevealPayload.STREAM_CODEC, (payload, ctx) -> CasketRevealClient.handle(payload));
   }

   public static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
      if (!(player instanceof FakePlayer) && player.connection != null && player.connection.hasChannel(payload)) {
         PacketDistributor.sendToPlayer(player, payload, new CustomPacketPayload[0]);
      }
   }

   private DFNetwork() {
   }
}
