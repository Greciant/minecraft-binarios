package com.awakenedmod.awakened.content.regenerator;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.AwakenScreenFxPayload;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AdamantiumBonding {
   public static final int MAX_COATING = 5;
   private static final double ARMOR_PER_TIER = 2.0;
   private static final double TOUGHNESS_PER_TIER = 1.0;
   private static final double KNOCKBACK_PER_TIER = 0.1;
   private static final double ATTACK_PER_TIER = 1.0;
   private static final String COAT_KEY = "adm_coat";
   private static final ResourceLocation ARMOR_ID = id("adamantium_armor");
   private static final ResourceLocation TOUGHNESS_ID = id("adamantium_toughness");
   private static final ResourceLocation KNOCKBACK_ID = id("adamantium_knockback");
   private static final ResourceLocation ATTACK_ID = id("adamantium_attack");

   private AdamantiumBonding() {
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("awakened", path);
   }

   public static int coating(PowerData data) {
      return Math.round(Math.max(data.bankedVar("adm_coat", 0.0F), data.var("adm_coat", 0.0F)));
   }

   public static boolean isMaxed(PowerData data) {
      return coating(data) >= 5;
   }

   public static int grantCoating(ServerPlayer player, PowerData data) {
      int tier = Math.min(5, coating(data) + 1);
      data.setBankedVar("adm_coat", (float)tier);
      data.removeVar("adm_coat");
      applyAttributes(player, data);
      QuestSystem.feat(player, "regen_bonded");
      ServerLevel level = player.serverLevel();
      Fx.burst(level, ParticleTypes.TOTEM_OF_UNDYING, player.position().add(0.0, 1.0, 0.0), 30, 0.6, 0.2);
      Fx.burst(level, ParticleTypes.ENCHANT, player.position().add(0.0, 1.2, 0.0), 50, 0.8, 0.4);
      Fx.sound(level, player.position(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0F, 0.9F);
      Fx.sound(level, player.position(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.0F);
      PacketDistributor.sendToPlayer(player, new AwakenScreenFxPayload(12109524, 45), new CustomPacketPayload[0]);
      player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 45, 15));
      player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("awakened.adamantium.title").withStyle(ChatFormatting.WHITE)));
      player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("awakened.adamantium.subtitle", new Object[]{tier})));
      player.displayClientMessage(Component.translatable("awakened.adamantium.bonded", new Object[]{tier}), true);
      PowerService.sync(player);
      return tier;
   }

   public static void applyAttributes(ServerPlayer player, PowerData data) {
      applyTier(player, coating(data));
   }

   public static void clearAttributes(ServerPlayer player) {
      applyTier(player, 0);
   }

   private static void applyTier(ServerPlayer player, int tier) {
      setModifier(player, Attributes.ARMOR, ARMOR_ID, 2.0 * (double)tier);
      setModifier(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, 1.0 * (double)tier);
      setModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 0.1 * (double)tier);
      setModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_ID, 1.0 * (double)tier);
   }

   public static void ensureAttributes(ServerPlayer player, PowerData data) {
      if (data.hasVar("adm_coat")) {
         data.setBankedVar("adm_coat", (float)coating(data));
         data.removeVar("adm_coat");
      }

      AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
      if (armor != null) {
         boolean present = armor.getModifier(ARMOR_ID) != null;
         boolean coated = coating(data) > 0;
         if (coated != present) {
            applyAttributes(player, data);
         }
      }
   }

   private static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount) {
      AttributeInstance instance = player.getAttribute(attribute);
      if (instance != null) {
         instance.removeModifier(id);
         if (amount > 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, Operation.ADD_VALUE));
         }
      }
   }
}
