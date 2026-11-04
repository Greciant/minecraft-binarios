package com.awakenedmod.awakened.content.limitless.domain;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;

public record VoidBody(int anchor, boolean member, boolean held, boolean caster) {
   public static final VoidBody NONE = new VoidBody(-1, false, false, false);
   public static final StreamCodec<ByteBuf, VoidBody> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      VoidBody::anchor,
      ByteBufCodecs.BOOL,
      VoidBody::member,
      ByteBufCodecs.BOOL,
      VoidBody::held,
      ByteBufCodecs.BOOL,
      VoidBody::caster,
      VoidBody::new
   );
   public static final AttachmentType<VoidBody> TYPE = AttachmentType.builder(() -> NONE).sync(STREAM_CODEC).build();

   public boolean inVoid() {
      return this.anchor >= 0;
   }

   public VoidBody withHeld(boolean on) {
      return new VoidBody(this.anchor, this.member, on, this.caster);
   }

   public static VoidBody of(Entity body) {
      return body.hasData(TYPE) ? (VoidBody)body.getData(TYPE) : NONE;
   }

   public static void set(Entity body, VoidBody next) {
      if (!next.equals(of(body))) {
         body.setData(TYPE, next);
      }
   }
}
