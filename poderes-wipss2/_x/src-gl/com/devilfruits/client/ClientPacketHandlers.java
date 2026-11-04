package com.devilfruits.client;

import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.fx.ChargeVisuals;
import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.logia.LogiaClient;
import com.devilfruits.client.render.FormRenderers;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.logia.LogiaState;
import com.devilfruits.core.network.AbilityTablePayload;
import com.devilfruits.core.network.AnimationPayload;
import com.devilfruits.core.network.ChargePayload;
import com.devilfruits.core.network.FormStatePayload;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.core.network.LogiaHitPayload;
import com.devilfruits.core.network.LogiaStatePayload;
import com.devilfruits.registry.DFAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class ClientPacketHandlers {
   public static void handleFx(FxPayload payload) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         ClientFxRegistry.dispatch(level, payload);
      }
   }

   public static void handleAnimation(AnimationPayload payload) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         Entity entity = level.getEntity(payload.entityId());
         if (entity != null) {
            if (payload.stop()) {
               AnimationController.stop(entity);
            } else {
               AnimationController.play(entity, payload.animation());
            }
         }
      }
   }

   public static void handleAbilityTable(AbilityTablePayload payload) {
      ClientFruitState.setTable(payload);
   }

   public static void handleCharge(ChargePayload payload) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         ChargeVisuals.handle(level, payload);
      }
   }

   public static void handleFormState(FormStatePayload payload) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.getEntity(payload.entityId()) instanceof Player player) {
         PlayerFruitData data = (PlayerFruitData)player.getData(DFAttachments.PLAYER_DATA);
         FruitForm previous = data.form();
         data.setFruit(payload.fruit().orElse(null));
         data.setForm(payload.form());
         data.setFlags(payload.flags() & -129);
         data.clearDirty();
         if ((payload.flags() & 128) != 0 && previous != null && previous != data.form()) {
            FormRenderers.keepDuringDeath(player, previous);
         }

         FruitForm form = data.form();
         player.setForcedPose(form == null ? null : form.forcedPose());
         player.refreshDimensions();
      }
   }

   public static void handleLogiaState(LogiaStatePayload payload) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.getEntity(payload.entityId()) instanceof Player player) {
         PlayerFruitData data = (PlayerFruitData)player.getData(DFAttachments.PLAYER_DATA);
         LogiaState before = data.logia().state();
         data.logia().applyRemote(LogiaState.byId(payload.state()), payload.limbs(), payload.counters(), payload.mode());
         LogiaClient.onState(player, before, data.logia().state());
      }
   }

   public static void handleLogiaHit(LogiaHitPayload payload) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.getEntity(payload.victimId()) instanceof Player player) {
         LogiaClient.onHits(player, payload.hits());
      }
   }

   private ClientPacketHandlers() {
   }
}
