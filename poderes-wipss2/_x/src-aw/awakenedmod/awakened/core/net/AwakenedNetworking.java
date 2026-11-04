package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.client.net.ClientPacketHandlers;
import com.awakenedmod.awakened.content.FocusedCatalystItem;
import com.awakenedmod.awakened.content.aero.GaleControl;
import com.awakenedmod.awakened.content.block.InfusionTankBlock;
import com.awakenedmod.awakened.content.command.CommandRules;
import com.awakenedmod.awakened.content.earth.GeoSeismic;
import com.awakenedmod.awakened.content.earth.StoneMechControl;
import com.awakenedmod.awakened.content.exosavant.ExoFlight;
import com.awakenedmod.awakened.content.exosavant.ExoSkins;
import com.awakenedmod.awakened.content.exosavant.ExoSuit;
import com.awakenedmod.awakened.content.exosavant.ExoVisor;
import com.awakenedmod.awakened.content.exosavant.Failsafe;
import com.awakenedmod.awakened.content.reaper.ReaperAura;
import com.awakenedmod.awakened.content.scaler.RayEffects;
import com.awakenedmod.awakened.content.spider.SpiderTypeSync;
import com.awakenedmod.awakened.content.spider.WebStrikeAbility;
import com.awakenedmod.awakened.content.spider.WebSwingAbility;
import com.awakenedmod.awakened.content.spider.WebWingsAbility;
import com.awakenedmod.awakened.content.spider.WebZipAbility;
import com.awakenedmod.awakened.content.summoning.PossessControl;
import com.awakenedmod.awakened.content.summoning.SoulMenu;
import com.awakenedmod.awakened.content.velocity.Gearbox;
import com.awakenedmod.awakened.core.MoveKeys;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.admin.AdminAction;
import com.awakenedmod.awakened.core.admin.AdminService;
import com.awakenedmod.awakened.core.awaken.AwakeningCinematic;
import com.awakenedmod.awakened.core.power.PassiveToggleService;
import com.awakenedmod.awakened.core.power.PowerMelee;
import com.awakenedmod.awakened.core.quest.QuestBoard;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.relic.RelicSystem;
import com.awakenedmod.awakened.core.skill.SkillSystem;
import com.awakenedmod.awakened.core.team.TeamSync;
import com.awakenedmod.awakened.core.trade.PowerTrades;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AwakenedNetworking {
   private static final String VERSION = "50";

   private AwakenedNetworking() {
   }

   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("50");
      registrar.playToServer(QuestActionPayload.TYPE, QuestActionPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            ServerPlayer sender = asServerPlayer(context);
            if (sender != null) {
               QuestSystem.handle(sender, QuestBoard.Action.byIndex(payload.action()), payload.slot(), payload.questId());
            }
         }));
      registrar.playToServer(
         ActivateAbilityPayload.TYPE,
         ActivateAbilityPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> PowerService.activate(asServerPlayer(context), payload.slot()))
      );
      registrar.playToServer(
         ReleaseAbilityPayload.TYPE,
         ReleaseAbilityPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> PowerService.release(asServerPlayer(context), payload.slot()))
      );
      registrar.playToServer(
         SetActiveSlotPayload.TYPE,
         SetActiveSlotPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> PowerService.setActiveSlot(asServerPlayer(context), payload.slot()))
      );
      registrar.playToServer(
         AdminActionPayload.TYPE,
         AdminActionPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(
               () -> AdminService.handle(asServerPlayer(context), AdminAction.byId(payload.action()), payload.powerId(), payload.intArg())
            )
      );
      registrar.playToServer(
         AllocateStatPayload.TYPE,
         AllocateStatPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> SkillSystem.handleAllocate(asServerPlayer(context), payload.statId(), payload.delta()))
      );
      registrar.playToServer(
         AbilityActionPayload.TYPE,
         AbilityActionPayload.STREAM_CODEC,
         (payload, context) -> act(context, actor -> PowerService.abilityAction(actor, payload.slot(), payload.action()))
      );
      registrar.playToServer(
         OpenBeltPayload.TYPE, OpenBeltPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> RelicSystem.openBelt(asServerPlayer(context)))
      );
      registrar.playToServer(
         EquipBeltPayload.TYPE,
         EquipBeltPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> RelicSystem.handleBelt(asServerPlayer(context), payload.equip()))
      );
      registrar.playToServer(
         BeginInfusionPayload.TYPE,
         BeginInfusionPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> InfusionTankBlock.begin(asServerPlayer(context), payload.pos()))
      );
      registrar.playToServer(
         SoulMenuPayload.TYPE,
         SoulMenuPayload.STREAM_CODEC,
         (payload, context) -> owned(context, actor -> SoulMenu.handle(actor, payload.action(), payload.soulType(), payload.amount()))
      );
      registrar.playToServer(
         CommandRulePayload.TYPE,
         CommandRulePayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> CommandRules.decree(asServerPlayer(context), payload.slot(), payload.rule()))
      );
      registrar.playToServer(
         CommandRenamePayload.TYPE,
         CommandRenamePayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> CommandRules.rename(asServerPlayer(context), payload.slot(), payload.name()))
      );
      registrar.playToServer(
         PossessTeleportPayload.TYPE, PossessTeleportPayload.STREAM_CODEC, (payload, context) -> act(context, actor -> PossessControl.teleportBody(actor))
      );
      registrar.playToServer(
         PossessInputPayload.TYPE,
         PossessInputPayload.STREAM_CODEC,
         (payload, context) -> steer(
               context,
               actor -> PossessControl.input(actor, payload.yaw(), payload.pitch(), payload.forward(), payload.strafe(), payload.jump(), payload.sprint())
            )
      );
      registrar.playToServer(
         PossessActionPayload.TYPE,
         PossessActionPayload.STREAM_CODEC,
         (payload, context) -> steer(context, actor -> PossessControl.action(actor, payload.action()))
      );
      registrar.playToServer(
         MeleeSwingPayload.TYPE, MeleeSwingPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> PowerMelee.swing(asServerPlayer(context)))
      );
      registrar.playToServer(StrugglePayload.TYPE, StrugglePayload.STREAM_CODEC, (payload, context) -> steer(context, actor -> RayEffects.struggle(actor)));
      registrar.playToServer(
         FocusedRerollPayload.TYPE,
         FocusedRerollPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> FocusedCatalystItem.handleConfirm(asServerPlayer(context), payload.offHand(), payload.powerIds()))
      );
      registrar.playToServer(
         CatalystChoicePayload.TYPE,
         CatalystChoicePayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> PowerService.answerOffer(asServerPlayer(context), payload.take()))
      );
      registrar.playToServer(
         TradeActionPayload.TYPE,
         TradeActionPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> PowerTrades.handle(asServerPlayer(context), payload.action(), payload.partner()))
      );
      registrar.playToServer(
         SkipCatalystSpinPayload.TYPE,
         SkipCatalystSpinPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> AwakeningCinematic.skipDelay(asServerPlayer(context)))
      );
      registrar.playToServer(SpiderSwingPayload.TYPE, SpiderSwingPayload.STREAM_CODEC, (payload, context) -> steer(context, actor -> {
            if (payload.action() != 6 && payload.action() != 7) {
               WebSwingAbility.handleClientSwing(actor, payload);
            } else {
               WebWingsAbility.handleClientWings(actor, payload);
            }
         }));
      registrar.playToServer(
         SpiderTrickPayload.TYPE,
         SpiderTrickPayload.STREAM_CODEC,
         (payload, context) -> steer(context, actor -> WebSwingAbility.handleClientTrick(actor, payload))
      );
      registrar.playToServer(
         SpiderZipPayload.TYPE, SpiderZipPayload.STREAM_CODEC, (payload, context) -> steer(context, actor -> WebZipAbility.handleClientZip(actor, payload))
      );
      registrar.playToServer(
         SpiderStrikePayload.TYPE,
         SpiderStrikePayload.STREAM_CODEC,
         (payload, context) -> steer(context, actor -> WebStrikeAbility.handleClientStrike(actor, payload))
      );
      registrar.playToServer(
         SpiderSelectTypePayload.TYPE,
         SpiderSelectTypePayload.STREAM_CODEC,
         (payload, context) -> owned(context, actor -> SpiderTypeSync.handleSelect(actor, payload.typeKey()))
      );
      registrar.playToServer(
         ReaperToggleAuraPayload.TYPE, ReaperToggleAuraPayload.STREAM_CODEC, (payload, context) -> act(context, actor -> ReaperAura.toggle(actor))
      );
      registrar.playToServer(
         ReaperSetAuraPayload.TYPE,
         ReaperSetAuraPayload.STREAM_CODEC,
         (payload, context) -> owned(context, actor -> ReaperAura.setShown(actor, payload.shownPct()))
      );
      registrar.playToServer(
         ReaperHideAuraPayload.TYPE,
         ReaperHideAuraPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> ReaperAura.setSelfHidden(asServerPlayer(context), payload.hide()))
      );
      registrar.playToServer(
         GeoSeismicTogglePayload.TYPE, GeoSeismicTogglePayload.STREAM_CODEC, (payload, context) -> act(context, actor -> GeoSeismic.toggle(actor))
      );
      registrar.playToServer(
         VelocityGearPayload.TYPE, VelocityGearPayload.STREAM_CODEC, (payload, context) -> act(context, actor -> Gearbox.shift(actor, payload.delta()))
      );
      registrar.playToServer(ExoDeployPayload.TYPE, ExoDeployPayload.STREAM_CODEC, (payload, context) -> act(context, actor -> ExoSuit.toggle(actor)));
      registrar.playToServer(ExoFlightPayload.TYPE, ExoFlightPayload.STREAM_CODEC, (payload, context) -> act(context, actor -> ExoFlight.toggleFlight(actor)));
      registrar.playToServer(ExoVisorPayload.TYPE, ExoVisorPayload.STREAM_CODEC, (payload, context) -> owned(context, ExoVisor::toggle));
      registrar.playToServer(ExoFailsafePayload.TYPE, ExoFailsafePayload.STREAM_CODEC, (payload, context) -> owned(context, Failsafe::toggle));
      registrar.playToServer(
         ExoBoostPayload.TYPE, ExoBoostPayload.STREAM_CODEC, (payload, context) -> act(context, actor -> ExoFlight.igniteAfterburner(actor))
      );
      registrar.playToServer(ExoFlightPrefPayload.TYPE, ExoFlightPrefPayload.STREAM_CODEC, (payload, context) -> owned(context, ExoFlight::toggleDefaultFlight));
      registrar.playToServer(ExoSkinSelectPayload.TYPE, ExoSkinSelectPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            ServerPlayer actor = asServerPlayer(context);
            if (actor != null) {
               ExoSkins.select(actor, payload.id());
            }
         }));
      registrar.playToServer(
         AeroFlightArmPayload.TYPE, AeroFlightArmPayload.STREAM_CODEC, (payload, context) -> act(context, actor -> GaleControl.toggleArmed(actor))
      );
      registrar.playToServer(
         AeroFlightTogglePayload.TYPE, AeroFlightTogglePayload.STREAM_CODEC, (payload, context) -> act(context, actor -> GaleControl.toggleFlight(actor))
      );
      registrar.playToServer(
         AeroSurfPayload.TYPE, AeroSurfPayload.STREAM_CODEC, (payload, context) -> act(context, actor -> GaleControl.handleSurf(actor, payload.action()))
      );
      registrar.playToServer(
         SecondaryKeyPayload.TYPE,
         SecondaryKeyPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> PowerService.secondaryKey(asServerPlayer(context), payload.sneaking(), payload.held()))
      );
      registrar.playToServer(
         PassiveTogglePayload.TYPE,
         PassiveTogglePayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> PassiveToggleService.toggle(asServerPlayer(context), payload.key()))
      );
      registrar.playToServer(
         TeamActionPayload.TYPE,
         TeamActionPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> TeamSync.handle(asServerPlayer(context), payload.action(), payload.arg()))
      );
      registrar.playToServer(
         MoveKeysPayload.TYPE,
         MoveKeysPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> MoveKeys.relay(asServerPlayer(context), payload.keys()))
      );
      registrar.playToServer(
         StoneMechInputPayload.TYPE,
         StoneMechInputPayload.STREAM_CODEC,
         (payload, context) -> steer(
               context,
               actor -> StoneMechControl.input(actor, payload.yaw(), payload.pitch(), payload.forward(), payload.strafe(), payload.jump(), payload.sprint())
            )
      );
      registrar.playToServer(
         StoneMechActionPayload.TYPE,
         StoneMechActionPayload.STREAM_CODEC,
         (payload, context) -> steer(context, actor -> StoneMechControl.action(actor, payload.action()))
      );
      if (FMLEnvironment.dist == Dist.CLIENT) {
         ClientPacketHandlers.register(registrar);
      } else {
         serverToClient(registrar, SyncPowerDataPayload.TYPE, SyncPowerDataPayload.STREAM_CODEC);
         serverToClient(registrar, SyncProgressPayload.TYPE, SyncProgressPayload.STREAM_CODEC);
         serverToClient(registrar, SyncSkillPayload.TYPE, SyncSkillPayload.STREAM_CODEC);
         serverToClient(registrar, SyncQuestPayload.TYPE, SyncQuestPayload.STREAM_CODEC);
         serverToClient(registrar, SyncRelicPayload.TYPE, SyncRelicPayload.STREAM_CODEC);
         serverToClient(registrar, AbilityFeedbackPayload.TYPE, AbilityFeedbackPayload.STREAM_CODEC);
         serverToClient(registrar, SyncRealisticMovementPayload.TYPE, SyncRealisticMovementPayload.STREAM_CODEC);
         serverToClient(registrar, PlayAnimationPayload.TYPE, PlayAnimationPayload.STREAM_CODEC);
         serverToClient(registrar, AwakenScreenFxPayload.TYPE, AwakenScreenFxPayload.STREAM_CODEC);
         serverToClient(registrar, OpenInfusionScreenPayload.TYPE, OpenInfusionScreenPayload.STREAM_CODEC);
         serverToClient(registrar, InfusionPosePayload.TYPE, InfusionPosePayload.STREAM_CODEC);
         serverToClient(registrar, ChronoFxPayload.TYPE, ChronoFxPayload.STREAM_CODEC);
         serverToClient(registrar, TimeFrozenPayload.TYPE, TimeFrozenPayload.STREAM_CODEC);
         serverToClient(registrar, HandsBusyPayload.TYPE, HandsBusyPayload.STREAM_CODEC);
         serverToClient(registrar, ThunderfallAimPayload.TYPE, ThunderfallAimPayload.STREAM_CODEC);
         serverToClient(registrar, StartPossessPayload.TYPE, StartPossessPayload.STREAM_CODEC);
         serverToClient(registrar, StartMechPilotPayload.TYPE, StartMechPilotPayload.STREAM_CODEC);
         serverToClient(registrar, StopAnimationPayload.TYPE, StopAnimationPayload.STREAM_CODEC);
         serverToClient(registrar, MeldHidePayload.TYPE, MeldHidePayload.STREAM_CODEC);
         serverToClient(registrar, SolarHealthFxPayload.TYPE, SolarHealthFxPayload.STREAM_CODEC);
         serverToClient(registrar, ScreenShakePayload.TYPE, ScreenShakePayload.STREAM_CODEC);
         serverToClient(registrar, ScalerStatePayload.TYPE, ScalerStatePayload.STREAM_CODEC);
         serverToClient(registrar, SpiderStatePayload.TYPE, SpiderStatePayload.STREAM_CODEC);
         serverToClient(registrar, SpiderTypePayload.TYPE, SpiderTypePayload.STREAM_CODEC);
         serverToClient(registrar, SpiderForceStopPayload.TYPE, SpiderForceStopPayload.STREAM_CODEC);
         serverToClient(registrar, SpiderSenseTinglePayload.TYPE, SpiderSenseTinglePayload.STREAM_CODEC);
         serverToClient(registrar, SpiderSpinPayload.TYPE, SpiderSpinPayload.STREAM_CODEC);
         serverToClient(registrar, ForcedCameraPayload.TYPE, ForcedCameraPayload.STREAM_CODEC);
         serverToClient(registrar, CatalystSpinPayload.TYPE, CatalystSpinPayload.STREAM_CODEC);
         serverToClient(registrar, OpenCatalystSelectPayload.TYPE, OpenCatalystSelectPayload.STREAM_CODEC);
         serverToClient(registrar, TradeScreenPayload.TYPE, TradeScreenPayload.STREAM_CODEC);
         serverToClient(registrar, CatalystOfferClosedPayload.TYPE, CatalystOfferClosedPayload.STREAM_CODEC);
         serverToClient(registrar, SyncTradeRulePayload.TYPE, SyncTradeRulePayload.STREAM_CODEC);
         serverToClient(registrar, ReaperAuraPayload.TYPE, ReaperAuraPayload.STREAM_CODEC);
         serverToClient(registrar, GammaRagePayload.TYPE, GammaRagePayload.STREAM_CODEC);
         serverToClient(registrar, ReaperDriftPayload.TYPE, ReaperDriftPayload.STREAM_CODEC);
         serverToClient(registrar, ReaperHauntPayload.TYPE, ReaperHauntPayload.STREAM_CODEC);
         serverToClient(registrar, FaultSteerPayload.TYPE, FaultSteerPayload.STREAM_CODEC);
         serverToClient(registrar, ExoFlightPrefSyncPayload.TYPE, ExoFlightPrefSyncPayload.STREAM_CODEC);
         serverToClient(registrar, PassiveToggleSyncPayload.TYPE, PassiveToggleSyncPayload.STREAM_CODEC);
         serverToClient(registrar, HeldRootPayload.TYPE, HeldRootPayload.STREAM_CODEC);
         serverToClient(registrar, SyncTeamPayload.TYPE, SyncTeamPayload.STREAM_CODEC);
         serverToClient(registrar, AeroFlightSyncPayload.TYPE, AeroFlightSyncPayload.STREAM_CODEC);
         serverToClient(registrar, BrawlerComboPayload.TYPE, BrawlerComboPayload.STREAM_CODEC);
         serverToClient(registrar, ArbiterRootWarnPayload.TYPE, ArbiterRootWarnPayload.STREAM_CODEC);
         serverToClient(registrar, VelocityScreenFxPayload.TYPE, VelocityScreenFxPayload.STREAM_CODEC);
         serverToClient(registrar, LaneTransitPayload.TYPE, LaneTransitPayload.STREAM_CODEC);
         serverToClient(registrar, LoopSoundPayload.TYPE, LoopSoundPayload.STREAM_CODEC);
      }
   }

   public static <T extends CustomPacketPayload> void serverToClient(
      PayloadRegistrar registrar, Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec
   ) {
      registrar.playToClient(type, codec, (payload, context) -> {
      });
   }

   public static ServerPlayer asServerPlayer(IPayloadContext context) {
      return context.player() instanceof ServerPlayer player ? player : null;
   }

   private static void act(IPayloadContext context, Consumer<ServerPlayer> intent) {
      context.enqueueWork(() -> {
         ServerPlayer actor = asServerPlayer(context);
         if (actor != null && !actor.isSpectator() && !PowerService.refusedBySuppression(actor)) {
            intent.accept(actor);
         }
      });
   }

   private static void owned(IPayloadContext context, Consumer<ServerPlayer> intent) {
      context.enqueueWork(() -> {
         ServerPlayer actor = asServerPlayer(context);
         if (actor != null && !PowerService.refusedBySuppression(actor)) {
            intent.accept(actor);
         }
      });
   }

   private static void steer(IPayloadContext context, Consumer<ServerPlayer> intent) {
      context.enqueueWork(() -> {
         ServerPlayer actor = asServerPlayer(context);
         if (actor != null && !actor.isSpectator()) {
            intent.accept(actor);
         }
      });
   }
}
