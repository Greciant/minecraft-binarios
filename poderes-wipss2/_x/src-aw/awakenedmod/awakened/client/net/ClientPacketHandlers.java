package com.awakenedmod.awakened.client.net;

import com.awakenedmod.awakened.client.AeroClientState;
import com.awakenedmod.awakened.client.ArbiterRootWarnFx;
import com.awakenedmod.awakened.client.AwakenScreenFx;
import com.awakenedmod.awakened.client.AwakenedAnimations;
import com.awakenedmod.awakened.client.BrawlerComboState;
import com.awakenedmod.awakened.client.ClientFreeze;
import com.awakenedmod.awakened.client.ClientHandsBusy;
import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.client.ClientTeamState;
import com.awakenedmod.awakened.client.ExoClientState;
import com.awakenedmod.awakened.client.FaultSteerState;
import com.awakenedmod.awakened.client.ForcedCamera;
import com.awakenedmod.awakened.client.GammaRageState;
import com.awakenedmod.awakened.client.InfusionLie;
import com.awakenedmod.awakened.client.LoopSoundClient;
import com.awakenedmod.awakened.client.MeldHidden;
import com.awakenedmod.awakened.client.PassiveToggleClient;
import com.awakenedmod.awakened.client.PossessCamera;
import com.awakenedmod.awakened.client.ReaperAuraState;
import com.awakenedmod.awakened.client.ReaperDriftState;
import com.awakenedmod.awakened.client.ReaperHauntFx;
import com.awakenedmod.awakened.client.ScalerClientState;
import com.awakenedmod.awakened.client.ScreenShakeFx;
import com.awakenedmod.awakened.client.SolarHealthState;
import com.awakenedmod.awakened.client.SpiderClientState;
import com.awakenedmod.awakened.client.SpiderSenseFx;
import com.awakenedmod.awakened.client.SpiderSpin;
import com.awakenedmod.awakened.client.SpiderSwingController;
import com.awakenedmod.awakened.client.StoneMechPilot;
import com.awakenedmod.awakened.client.VelocityLaneTransitClient;
import com.awakenedmod.awakened.client.chrono.ChronoFxClient;
import com.awakenedmod.awakened.client.render.VelocityScreenFx;
import com.awakenedmod.awakened.client.screen.CatalystSelectScreen;
import com.awakenedmod.awakened.client.screen.CatalystSpinnerScreen;
import com.awakenedmod.awakened.client.screen.InfusionScreen;
import com.awakenedmod.awakened.client.screen.PowerTradeScreen;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.BodyPinState;
import com.awakenedmod.awakened.core.RealisticMovement;
import com.awakenedmod.awakened.core.net.AbilityFeedbackPayload;
import com.awakenedmod.awakened.core.net.AeroFlightSyncPayload;
import com.awakenedmod.awakened.core.net.ArbiterRootWarnPayload;
import com.awakenedmod.awakened.core.net.AwakenScreenFxPayload;
import com.awakenedmod.awakened.core.net.BrawlerComboPayload;
import com.awakenedmod.awakened.core.net.CatalystOfferClosedPayload;
import com.awakenedmod.awakened.core.net.CatalystSpinPayload;
import com.awakenedmod.awakened.core.net.ChronoFxPayload;
import com.awakenedmod.awakened.core.net.ExoFlightPrefSyncPayload;
import com.awakenedmod.awakened.core.net.FaultSteerPayload;
import com.awakenedmod.awakened.core.net.ForcedCameraPayload;
import com.awakenedmod.awakened.core.net.GammaRagePayload;
import com.awakenedmod.awakened.core.net.HandsBusyPayload;
import com.awakenedmod.awakened.core.net.HeldRootPayload;
import com.awakenedmod.awakened.core.net.InfusionPosePayload;
import com.awakenedmod.awakened.core.net.LaneTransitPayload;
import com.awakenedmod.awakened.core.net.LoopSoundPayload;
import com.awakenedmod.awakened.core.net.MeldHidePayload;
import com.awakenedmod.awakened.core.net.OpenCatalystSelectPayload;
import com.awakenedmod.awakened.core.net.OpenInfusionScreenPayload;
import com.awakenedmod.awakened.core.net.PassiveToggleSyncPayload;
import com.awakenedmod.awakened.core.net.PlayAnimationPayload;
import com.awakenedmod.awakened.core.net.ReaperAuraPayload;
import com.awakenedmod.awakened.core.net.ReaperDriftPayload;
import com.awakenedmod.awakened.core.net.ReaperHauntPayload;
import com.awakenedmod.awakened.core.net.ScalerStatePayload;
import com.awakenedmod.awakened.core.net.ScreenShakePayload;
import com.awakenedmod.awakened.core.net.SolarHealthFxPayload;
import com.awakenedmod.awakened.core.net.SpiderForceStopPayload;
import com.awakenedmod.awakened.core.net.SpiderSenseTinglePayload;
import com.awakenedmod.awakened.core.net.SpiderSpinPayload;
import com.awakenedmod.awakened.core.net.SpiderStatePayload;
import com.awakenedmod.awakened.core.net.SpiderTypePayload;
import com.awakenedmod.awakened.core.net.StartMechPilotPayload;
import com.awakenedmod.awakened.core.net.StartPossessPayload;
import com.awakenedmod.awakened.core.net.StopAnimationPayload;
import com.awakenedmod.awakened.core.net.SyncPowerDataPayload;
import com.awakenedmod.awakened.core.net.SyncProgressPayload;
import com.awakenedmod.awakened.core.net.SyncQuestPayload;
import com.awakenedmod.awakened.core.net.SyncRealisticMovementPayload;
import com.awakenedmod.awakened.core.net.SyncRelicPayload;
import com.awakenedmod.awakened.core.net.SyncSkillPayload;
import com.awakenedmod.awakened.core.net.SyncTeamPayload;
import com.awakenedmod.awakened.core.net.SyncTradeRulePayload;
import com.awakenedmod.awakened.core.net.ThunderfallAimPayload;
import com.awakenedmod.awakened.core.net.TimeFrozenPayload;
import com.awakenedmod.awakened.core.net.TradeScreenPayload;
import com.awakenedmod.awakened.core.net.VelocityScreenFxPayload;
import com.awakenedmod.awakened.core.trade.TradeRule;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ClientPacketHandlers {
   private ClientPacketHandlers() {
   }

   public static void register(PayloadRegistrar registrar) {
      registrar.playToClient(
         SyncPowerDataPayload.TYPE, SyncPowerDataPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPowerState.apply(payload))
      );
      registrar.playToClient(
         SyncProgressPayload.TYPE, SyncProgressPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPowerState.applyProgress(payload))
      );
      registrar.playToClient(
         SyncSkillPayload.TYPE, SyncSkillPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPowerState.applySkill(payload))
      );
      registrar.playToClient(
         SyncQuestPayload.TYPE, SyncQuestPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPowerState.applyQuests(payload))
      );
      registrar.playToClient(
         SyncRelicPayload.TYPE, SyncRelicPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPowerState.applyRelics(payload))
      );
      registrar.playToClient(
         AbilityFeedbackPayload.TYPE,
         AbilityFeedbackPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> ClientPowerState.onFeedback(payload))
      );
      registrar.playToClient(
         SyncRealisticMovementPayload.TYPE,
         SyncRealisticMovementPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> RealisticMovement.setClient(payload.enabled()))
      );
      registrar.playToClient(
         PlayAnimationPayload.TYPE,
         PlayAnimationPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> AwakenedAnimations.play(payload.entityId(), AwId.of(payload.animation())))
      );
      registrar.playToClient(
         AwakenScreenFxPayload.TYPE,
         AwakenScreenFxPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> AwakenScreenFx.trigger(payload.color(), payload.duration(), payload.shakeOnly()))
      );
      registrar.playToClient(
         OpenInfusionScreenPayload.TYPE,
         OpenInfusionScreenPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(
               () -> Minecraft.getInstance().setScreen(new InfusionScreen(payload.pos(), payload.chargeFraction(), payload.tier(), payload.ready()))
            )
      );
      registrar.playToClient(
         ChronoFxPayload.TYPE, ChronoFxPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ChronoFxClient.accept(payload))
      );
      registrar.playToClient(
         VelocityScreenFxPayload.TYPE,
         VelocityScreenFxPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> VelocityScreenFx.set(payload.kind(), payload.on()))
      );
      registrar.playToClient(
         LaneTransitPayload.TYPE, LaneTransitPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> VelocityLaneTransitClient.set(payload))
      );
      registrar.playToClient(
         TimeFrozenPayload.TYPE, TimeFrozenPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientFreeze.setFrozen(payload.frozen()))
      );
      registrar.playToClient(
         HandsBusyPayload.TYPE, HandsBusyPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientHandsBusy.setBusy(payload.busy()))
      );
      registrar.playToClient(
         ArbiterRootWarnPayload.TYPE,
         ArbiterRootWarnPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> ArbiterRootWarnFx.set(payload.inside(), payload.ticksToBurst()))
      );
      registrar.playToClient(
         ThunderfallAimPayload.TYPE,
         ThunderfallAimPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> ClientPowerState.setThunderfallAiming(payload.aiming()))
      );
      registrar.playToClient(
         InfusionPosePayload.TYPE,
         InfusionPosePayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> InfusionLie.set(payload.active(), payload.bodyYaw()))
      );
      registrar.playToClient(
         StartPossessPayload.TYPE, StartPossessPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> PossessCamera.start(payload.entityId()))
      );
      registrar.playToClient(
         StartMechPilotPayload.TYPE,
         StartMechPilotPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> StoneMechPilot.start(payload.entityId()))
      );
      registrar.playToClient(
         StopAnimationPayload.TYPE,
         StopAnimationPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> AwakenedAnimations.stop(payload.entityId()))
      );
      registrar.playToClient(
         MeldHidePayload.TYPE,
         MeldHidePayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> MeldHidden.set(payload.entityId(), payload.hidden()))
      );
      registrar.playToClient(
         SolarHealthFxPayload.TYPE, SolarHealthFxPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> SolarHealthState.markBenign())
      );
      registrar.playToClient(
         LoopSoundPayload.TYPE, LoopSoundPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> LoopSoundClient.accept(payload))
      );
      registrar.playToClient(
         ScreenShakePayload.TYPE, ScreenShakePayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ScreenShakeFx.add(payload))
      );
      registrar.playToClient(
         ScalerStatePayload.TYPE,
         ScalerStatePayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> ScalerClientState.set(payload.rayShifted(), payload.seized(), payload.seizing()))
      );
      registrar.playToClient(
         SpiderStatePayload.TYPE,
         SpiderStatePayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(
               () -> SpiderClientState.set(payload.swinging(), payload.hanging(), payload.anchorX(), payload.anchorY(), payload.anchorZ(), payload.leftArm())
            )
      );
      registrar.playToClient(
         SpiderTypePayload.TYPE,
         SpiderTypePayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> SpiderClientState.setLoadedType(payload.typeKey(), payload.hasGauntlets()))
      );
      registrar.playToClient(
         ExoFlightPrefSyncPayload.TYPE,
         ExoFlightPrefSyncPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> ExoClientState.setDefaultJet(payload.jet()))
      );
      registrar.playToClient(
         PassiveToggleSyncPayload.TYPE,
         PassiveToggleSyncPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> PassiveToggleClient.apply(payload.states()))
      );
      registrar.playToClient(
         SyncTeamPayload.TYPE, SyncTeamPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientTeamState.apply(payload))
      );
      registrar.playToClient(
         SpiderForceStopPayload.TYPE, SpiderForceStopPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> SpiderSwingController.forceStop())
      );
      registrar.playToClient(
         SpiderSenseTinglePayload.TYPE,
         SpiderSenseTinglePayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(
               () -> SpiderSenseFx.trigger(payload.bearingDeg(), payload.intensity(), payload.directional(), payload.kind(), payload.masteryLevel())
            )
      );
      registrar.playToClient(
         SpiderSpinPayload.TYPE,
         SpiderSpinPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> SpiderSpin.start(payload.ticks(), payload.yawPerTick()))
      );
      registrar.playToClient(
         ForcedCameraPayload.TYPE,
         ForcedCameraPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> ForcedCamera.force(payload.ticks(), payload.orbitEntityId()))
      );
      registrar.playToClient(HeldRootPayload.TYPE, HeldRootPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
               BodyPinState.setClient(mc.player.getId(), payload.rooted());
            }
         }));
      registrar.playToClient(
         CatalystSpinPayload.TYPE, CatalystSpinPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> CatalystSpinnerScreen.open(payload))
      );
      registrar.playToClient(
         CatalystOfferClosedPayload.TYPE,
         CatalystOfferClosedPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(CatalystSpinnerScreen::offerClosed)
      );
      registrar.playToClient(
         SyncTradeRulePayload.TYPE, SyncTradeRulePayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> TradeRule.setClient(payload.allowed()))
      );
      registrar.playToClient(
         TradeScreenPayload.TYPE, TradeScreenPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> PowerTradeScreen.apply(payload))
      );
      registrar.playToClient(
         OpenCatalystSelectPayload.TYPE,
         OpenCatalystSelectPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> CatalystSelectScreen.open(payload))
      );
      registrar.playToClient(
         ReaperAuraPayload.TYPE,
         ReaperAuraPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> ReaperAuraState.applyLocalAware(payload.entityId(), payload.tier(), payload.shownPct()))
      );
      registrar.playToClient(
         GammaRagePayload.TYPE,
         GammaRagePayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> GammaRageState.put(payload.entityId(), payload.rage()))
      );
      registrar.playToClient(
         BrawlerComboPayload.TYPE, BrawlerComboPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> BrawlerComboState.apply(payload))
      );
      registrar.playToClient(
         ReaperHauntPayload.TYPE,
         ReaperHauntPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> ReaperHauntFx.setRemaining(payload.remainingTicks()))
      );
      registrar.playToClient(
         FaultSteerPayload.TYPE, FaultSteerPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> FaultSteerState.set(payload.live()))
      );
      registrar.playToClient(
         ReaperDriftPayload.TYPE, ReaperDriftPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ReaperDriftState.set(payload.drifting()))
      );
      registrar.playToClient(
         AeroFlightSyncPayload.TYPE,
         AeroFlightSyncPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> AeroClientState.set(payload.armed(), payload.flying(), payload.gliding(), payload.forcedGliding()))
      );
   }
}
