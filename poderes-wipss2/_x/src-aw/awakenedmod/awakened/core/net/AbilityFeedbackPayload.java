package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record AbilityFeedbackPayload(int kind, String reasonKey, List<String> reasonArgs) implements CustomPacketPayload {
   public static final int FAIL_STAMINA = 0;
   public static final int FAIL_LOCKED = 1;
   public static final int FAIL_NO_EFFECT = 2;
   public static final int FAIL_SEALED = 3;
   public static final int FAIL_REASON = 4;
   public static final Type<AbilityFeedbackPayload> TYPE = new Type(AwId.of("ability_feedback"));
   public static final StreamCodec<RegistryFriendlyByteBuf, AbilityFeedbackPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      AbilityFeedbackPayload::kind,
      ByteBufCodecs.STRING_UTF8,
      AbilityFeedbackPayload::reasonKey,
      ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
      AbilityFeedbackPayload::reasonArgs,
      AbilityFeedbackPayload::new
   );

   public AbilityFeedbackPayload(int kind) {
      this(kind, "", List.of());
   }

   public static AbilityFeedbackPayload reason(String langKey, Object... args) {
      List<String> wired = new ArrayList<>(args.length);

      for (Object arg : args) {
         wired.add(arg instanceof Component component ? component.getString() : String.valueOf(arg));
      }

      return new AbilityFeedbackPayload(4, langKey, List.copyOf(wired));
   }

   public Component reasonComponent() {
      return this.reasonKey.isEmpty() ? null : Component.translatable(this.reasonKey, this.reasonArgs.toArray());
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
