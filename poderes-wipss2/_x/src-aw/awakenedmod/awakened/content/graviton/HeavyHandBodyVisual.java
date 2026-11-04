package com.awakenedmod.awakened.content.graviton;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.attachment.AttachmentType;

public record HeavyHandBodyVisual(int phase, long start, int length) {
   public static final int NONE_PHASE = 0;
   public static final int PIN = 1;
   public static final int FLIGHT = 2;
   public static final int DRAG = 3;
   public static final int SLIDE = 4;
   public static final HeavyHandBodyVisual NONE = new HeavyHandBodyVisual(0, 0L, 0);
   public static final StreamCodec<ByteBuf, HeavyHandBodyVisual> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      HeavyHandBodyVisual::phase,
      ByteBufCodecs.VAR_LONG,
      HeavyHandBodyVisual::start,
      ByteBufCodecs.VAR_INT,
      HeavyHandBodyVisual::length,
      HeavyHandBodyVisual::new
   );
   public static final AttachmentType<HeavyHandBodyVisual> TYPE = AttachmentType.builder(() -> NONE).sync(STREAM_CODEC).build();

   public float remaining(double gameTime) {
      return this.length <= 0 ? 0.0F : (float)Math.max(0.0, Math.min(1.0, 1.0 - (gameTime - (double)this.start) / (double)this.length));
   }

   public float age(double gameTime) {
      return (float)Math.max(0.0, gameTime - (double)this.start);
   }

   public static HeavyHandBodyVisual of(LivingEntity body) {
      return (HeavyHandBodyVisual)body.getData(TYPE);
   }

   static void begin(LivingEntity body, int phase, int length) {
      boolean weak = phase == 3 || phase == 4;
      if (!weak || !body.hasData(TYPE) || ((HeavyHandBodyVisual)body.getData(TYPE)).phase() == 0) {
         body.setData(TYPE, new HeavyHandBodyVisual(phase, body.level().getGameTime(), length));
      }
   }

   static void end(LivingEntity body, int phase) {
      if (body.hasData(TYPE) && ((HeavyHandBodyVisual)body.getData(TYPE)).phase() == phase) {
         body.setData(TYPE, NONE);
      }
   }
}
