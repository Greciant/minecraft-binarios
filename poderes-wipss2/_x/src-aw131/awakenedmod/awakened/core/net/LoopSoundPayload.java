package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public record LoopSoundPayload(
   int entityId,
   double x,
   double y,
   double z,
   String key,
   Holder<SoundEvent> sound,
   SoundSource source,
   float volume,
   float pitch,
   float beamRange,
   int lengthTicks,
   boolean stop
) implements CustomPacketPayload {
   public static final Type<LoopSoundPayload> TYPE = new Type(AwId.of("loop_sound"));
   public static final StreamCodec<RegistryFriendlyByteBuf, LoopSoundPayload> STREAM_CODEC = StreamCodec.of(
      (buf, p) -> {
         buf.writeVarInt(p.entityId);
         buf.writeDouble(p.x);
         buf.writeDouble(p.y);
         buf.writeDouble(p.z);
         buf.writeUtf(p.key);
         SoundEvent.STREAM_CODEC.encode(buf, p.sound);
         buf.writeVarInt(p.source.ordinal());
         buf.writeFloat(p.volume);
         buf.writeFloat(p.pitch);
         buf.writeFloat(p.beamRange);
         buf.writeVarInt(p.lengthTicks);
         buf.writeBoolean(p.stop);
      },
      buf -> new LoopSoundPayload(
            buf.readVarInt(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readUtf(),
            (Holder<SoundEvent>)SoundEvent.STREAM_CODEC.decode(buf),
            SoundSource.values()[buf.readVarInt()],
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readVarInt(),
            buf.readBoolean()
         )
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
