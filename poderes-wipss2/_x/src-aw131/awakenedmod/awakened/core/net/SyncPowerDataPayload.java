package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.data.PowerData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SyncPowerDataPayload(
   String powerId,
   boolean awakened,
   float stamina,
   float staminaMax,
   int activeSlot,
   int masteryLevel,
   float masteryProgress,
   boolean infiniteStamina,
   float resourceFraction,
   int resourceTier,
   float resourceMark,
   float staminaBonus,
   float abilityPower,
   int resourceCooldownSeconds,
   float staminaLocked,
   int[] cooldownLeft,
   int[] cooldownLength
) implements CustomPacketPayload {
   public static final Type<SyncPowerDataPayload> TYPE = new Type(AwId.of("sync_power_data"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SyncPowerDataPayload> STREAM_CODEC = StreamCodec.of(
      SyncPowerDataPayload::encode, SyncPowerDataPayload::decode
   );

   private static void encode(RegistryFriendlyByteBuf buf, SyncPowerDataPayload p) {
      buf.writeUtf(p.powerId);
      buf.writeBoolean(p.awakened);
      buf.writeFloat(p.stamina);
      buf.writeFloat(p.staminaMax);
      buf.writeVarInt(p.activeSlot);
      buf.writeVarInt(p.masteryLevel);
      buf.writeFloat(p.masteryProgress);
      buf.writeBoolean(p.infiniteStamina);
      buf.writeFloat(p.resourceFraction);
      buf.writeVarInt(p.resourceTier);
      buf.writeFloat(p.resourceMark);
      buf.writeFloat(p.staminaBonus);
      buf.writeFloat(p.abilityPower);
      buf.writeVarInt(p.resourceCooldownSeconds);
      buf.writeFloat(p.staminaLocked);
      buf.writeVarInt(p.cooldownLeft.length);

      for (int i = 0; i < p.cooldownLeft.length; i++) {
         buf.writeVarInt(p.cooldownLeft[i]);
         buf.writeVarInt(p.cooldownLength[i]);
      }
   }

   private static SyncPowerDataPayload decode(RegistryFriendlyByteBuf buf) {
      String powerId = buf.readUtf();
      boolean awakened = buf.readBoolean();
      float stamina = buf.readFloat();
      float staminaMax = buf.readFloat();
      int activeSlot = buf.readVarInt();
      int masteryLevel = buf.readVarInt();
      float masteryProgress = buf.readFloat();
      boolean infiniteStamina = buf.readBoolean();
      float resourceFraction = buf.readFloat();
      int resourceTier = buf.readVarInt();
      float resourceMark = buf.readFloat();
      float staminaBonus = buf.readFloat();
      float abilityPower = buf.readFloat();
      int resourceCooldownSeconds = buf.readVarInt();
      float staminaLocked = buf.readFloat();
      int slots = buf.readVarInt();
      int[] cooldownLeft = new int[slots];
      int[] cooldownLength = new int[slots];

      for (int i = 0; i < slots; i++) {
         cooldownLeft[i] = buf.readVarInt();
         cooldownLength[i] = buf.readVarInt();
      }

      return new SyncPowerDataPayload(
         powerId,
         awakened,
         stamina,
         staminaMax,
         activeSlot,
         masteryLevel,
         masteryProgress,
         infiniteStamina,
         resourceFraction,
         resourceTier,
         resourceMark,
         staminaBonus,
         abilityPower,
         resourceCooldownSeconds,
         staminaLocked,
         cooldownLeft,
         cooldownLength
      );
   }

   public static SyncPowerDataPayload of(
      PowerData data,
      float staminaMax,
      float resourceFraction,
      int resourceTier,
      float resourceMark,
      float staminaBonus,
      float abilityPower,
      int resourceCooldownSeconds,
      float staminaLocked,
      int[] cooldownLeft,
      int[] cooldownLength
   ) {
      return new SyncPowerDataPayload(
         data.powerId(),
         data.awakened(),
         data.stamina(),
         staminaMax,
         data.activeSlot(),
         data.masteryLevel(),
         data.hasPower() ? (float)data.currentMastery().progress() : 0.0F,
         data.infiniteStamina(),
         resourceFraction,
         resourceTier,
         resourceMark,
         staminaBonus,
         abilityPower,
         resourceCooldownSeconds,
         staminaLocked,
         cooldownLeft,
         cooldownLength
      );
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
