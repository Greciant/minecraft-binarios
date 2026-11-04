package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoVisual;
import com.awakenedmod.awakened.core.data.PowerData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class ExoVisualState {
   private static final int MAX_HUD_TAGS = 16;

   private ExoVisualState() {
   }

   public static ExoVisual get(Player player) {
      return (ExoVisual)player.getData(AwakenedAttachments.EXO_VISUAL);
   }

   public static void sync(ServerPlayer player, PowerData data) {
      if (player != null) {
         ExoVisual desired = snapshot(player, data);
         if (!get(player).equals(desired)) {
            player.setData(AwakenedAttachments.EXO_VISUAL, desired);
         }
      }
   }

   public static void clear(ServerPlayer player) {
      if (player != null) {
         if (!get(player).equals(ExoVisual.STOWED)) {
            player.setData(AwakenedAttachments.EXO_VISUAL, ExoVisual.STOWED);
         }
      }
   }

   private static ExoVisual snapshot(ServerPlayer player, PowerData data) {
      boolean suited = ExoSuit.isSuited(player);
      if (!suited) {
         boolean chip = ExoLoadout.chipInstalled(player);
         boolean fsArmed = Failsafe.unlocked(data) && Failsafe.enabled(data) && chip;
         if (!fsArmed && !chip) {
            return ExoVisual.STOWED;
         } else {
            boolean fsReady = fsArmed && Failsafe.ready(player, data);
            return new ExoVisual(false, false, false, false, fsArmed, fsReady, false, chip, 0, false, false, false, 0, 0, 0, List.of(), List.of());
         }
      } else {
         boolean flying = ExoFlight.isFlying(data);
         boolean jet = flying && ExoFlight.isJet(data);
         boolean afterburner = jet && ExoFlight.isAfterburner(data);
         boolean fsArmed = Failsafe.unlocked(data) && Failsafe.enabled(data) && ExoLoadout.chipInstalled(player);
         boolean fsReady = fsArmed && Failsafe.ready(player, data);
         boolean analyzing = ExoAnalysis.analyzing(player);
         boolean chipInstalled = ExoLoadout.chipInstalled(player);
         int builtMask = ExoLoadout.equippedMask(player);
         int flightSpeed = Mth.clamp(Math.round(ExoFlight.flownSpeed(data) * 20.0F), 0, 255);
         int boostCd = Mth.clamp(Math.round(ExoFlight.boostRechargeRemaining(data) * 255.0F), 0, 255);
         int brakeLevel = jet ? Mth.clamp(Math.round(ExoFlight.brakeLevel(data) * 254.0F), 0, 254) : (ExoFlight.isSputtering(data) ? 255 : 0);
         boolean barrageActive = ExoDeploy.barraging(player);
         boolean handCannonDeploy = ExoDeploy.handCannonDeploying(player);
         boolean visorOpen = ExoVisor.isOpen(player) && (builtMask & ExoPiece.HELMET.bit()) != 0;
         return new ExoVisual(
            true,
            flying,
            jet,
            afterburner,
            fsArmed,
            fsReady,
            analyzing,
            chipInstalled,
            builtMask,
            barrageActive,
            handCannonDeploy,
            visorOpen,
            flightSpeed,
            boostCd,
            brakeLevel,
            taggedIds(player),
            revealedIds(player)
         );
      }
   }

   private static List<Integer> taggedIds(ServerPlayer player) {
      return toIds(ExoTags.tagged(player));
   }

   private static List<Integer> revealedIds(ServerPlayer player) {
      return toIds(ScanReveal.revealed(player));
   }

   private static List<Integer> toIds(List<LivingEntity> foes) {
      if (foes.isEmpty()) {
         return List.of();
      } else {
         List<Integer> ids = new ArrayList<>(Math.min(foes.size(), 16));

         for (LivingEntity foe : foes) {
            ids.add(foe.getId());
         }

         ids.sort(null);
         return (List<Integer>)(ids.size() > 16 ? new ArrayList<>(ids.subList(0, 16)) : ids);
      }
   }
}
