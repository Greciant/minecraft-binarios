package com.awakenedmod.awakened.content.reaper;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.AwakenScreenFxPayload;
import com.awakenedmod.awakened.core.net.SolarHealthFxPayload;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ReaperState {
   private static final String K_LAST_MS = "reaper_last_ms";
   private static final ResourceLocation HEALTH_ID = id("reaper_health");
   private static final Map<UUID, Integer> LAST_HP = new ConcurrentHashMap<>();

   private ReaperState() {
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("awakened", path);
   }

   public static void update(ServerPlayer player, PowerData data) {
      if (player.isAlive()) {
         long souls = ReaperAttachments.souls(player).souls();
         adjustMaxHealth(player, souls);
         detectMilestone(player, data, souls);
      }
   }

   private static void adjustMaxHealth(ServerPlayer player, long souls) {
      int targetMaxHp = (int)(Math.round(ReaperScaling.maxHealthHp(souls) / 2.0) * 2L);
      int last = LAST_HP.getOrDefault(player.getUUID(), 20);
      if (targetMaxHp != last) {
         int delta = targetMaxHp - last;
         notifyBenignHealth(player);
         float before = player.getHealth();
         setModifier(player, Attributes.MAX_HEALTH, HEALTH_ID, (double)targetMaxHp - 20.0, Operation.ADD_VALUE);
         player.setHealth(Mth.clamp(before + (float)delta, 1.0F, player.getMaxHealth()));
         LAST_HP.put(player.getUUID(), targetMaxHp);
      }
   }

   private static void detectMilestone(ServerPlayer player, PowerData data, long souls) {
      int current = ReaperScaling.milestoneIndex(souls);
      int lastIdx = Math.round(data.var("reaper_last_ms", -1.0F));
      if (lastIdx < 0) {
         data.setVar("reaper_last_ms", (float)current);
      } else {
         if (current > lastIdx) {
            data.setVar("reaper_last_ms", (float)current);
            fireMilestoneCinematic(player, current);
         } else if (current < lastIdx) {
            data.setVar("reaper_last_ms", (float)current);
         }
      }
   }

   private static void fireMilestoneCinematic(ServerPlayer player, int idx) {
      Component title = Component.translatable("awakened.reaper.milestone_title", new Object[]{Component.translatable(ReaperScaling.milestoneTitleKey(idx))})
         .withStyle(ChatFormatting.AQUA);
      player.connection.send(new ClientboundSetTitlesAnimationPacket(8, 30, 22));
      player.connection.send(new ClientboundSetTitleTextPacket(title));
      PacketDistributor.sendToPlayer(player, new AwakenScreenFxPayload(6277344, 30), new CustomPacketPayload[0]);
      ServerLevel level = player.serverLevel();
      Vec3 center = player.position().add(0.0, 1.0, 0.0);
      ReaperFx.milestoneIgnite(level, center, player.position().add(0.0, 0.1, 0.0));
      Fx.sound(level, player.position(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.3F, 0.5F);
   }

   private static void notifyBenignHealth(ServerPlayer player) {
      if (player.connection != null) {
         PacketDistributor.sendToPlayer(player, new SolarHealthFxPayload(), new CustomPacketPayload[0]);
      }
   }

   private static void setModifier(ServerPlayer player, Holder<Attribute> attr, ResourceLocation id, double amount, Operation op) {
      AttributeInstance inst = player.getAttribute(attr);
      if (inst != null) {
         inst.removeModifier(id);
         if (Math.abs(amount) > 1.0E-4) {
            inst.addTransientModifier(new AttributeModifier(id, amount, op));
         }
      }
   }

   public static void clear(ServerPlayer player) {
      Integer last = LAST_HP.remove(player.getUUID());
      if (last != null && player.connection != null) {
         notifyBenignHealth(player);
      }

      AttributeInstance inst = player.getAttribute(Attributes.MAX_HEALTH);
      if (inst != null) {
         boolean owed = last != null && inst.hasModifier(HEALTH_ID) && player.isAlive();
         float before = player.getHealth();
         inst.removeModifier(HEALTH_ID);
         if (owed) {
            float lent = (float)((double)last.intValue() - 20.0);
            player.setHealth(Mth.clamp(before - lent, 1.0F, player.getMaxHealth()));
         }
      }

      ReaperSouls.forget(player.getUUID());
      PowerService.data(player).setVar("reaper_last_ms", -1.0F);
   }
}
