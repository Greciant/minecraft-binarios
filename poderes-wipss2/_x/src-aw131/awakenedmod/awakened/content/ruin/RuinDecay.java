package com.awakenedmod.awakened.content.ruin;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public record RuinDecay(
   int stacks,
   @Nullable UUID applier,
   long lastApplyTick,
   float originX,
   float originY,
   float originZ,
   int pulseSeq,
   byte pulseKind,
   long pulseTick,
   int pulseAmount
) {
   public static final byte PULSE_NONE = 0;
   public static final byte PULSE_ADD = 1;
   public static final byte PULSE_PAYOUT = 2;
   public static final byte PULSE_EXPIRE = 3;
   public static final byte PULSE_CLEAR = 4;
   public static final RuinDecay NONE = new RuinDecay(0, null, 0L, 0.0F, 0.0F, 0.0F, 0, (byte)0, 0L, 0);
   public static final StreamCodec<FriendlyByteBuf, RuinDecay> STREAM_CODEC = StreamCodec.of(
      (buf, d) -> {
         buf.writeVarInt(d.stacks);
         buf.writeBoolean(d.applier != null);
         if (d.applier != null) {
            buf.writeUUID(d.applier);
         }

         buf.writeVarLong(d.lastApplyTick);
         buf.writeFloat(d.originX);
         buf.writeFloat(d.originY);
         buf.writeFloat(d.originZ);
         buf.writeVarInt(d.pulseSeq);
         buf.writeByte(d.pulseKind);
         buf.writeVarLong(d.pulseTick);
         buf.writeVarInt(d.pulseAmount);
      },
      buf -> new RuinDecay(
            buf.readVarInt(),
            buf.readBoolean() ? buf.readUUID() : null,
            buf.readVarLong(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readVarInt(),
            buf.readByte(),
            buf.readVarLong(),
            buf.readVarInt()
         )
   );

   public boolean active() {
      return this.stacks > 0;
   }

   public long expiresAt() {
      return this.lastApplyTick + 160L;
   }

   public boolean appliedBy(Player viewer) {
      return this.applier != null && this.applier.equals(viewer.getUUID());
   }

   public int stage() {
      if (this.stacks >= 20) {
         return 4;
      } else if (this.stacks >= 10) {
         return 3;
      } else if (this.stacks >= 5) {
         return 2;
      } else {
         return this.stacks >= 1 ? 1 : 0;
      }
   }

   public Vec3 origin() {
      return new Vec3((double)this.originX, (double)this.originY, (double)this.originZ);
   }

   public Vec3 originWorld(Entity body, float bodyYawDeg) {
      return body.position().add(fromBodyFrame(this.origin(), bodyYawDeg));
   }

   public static Vec3 toBodyFrame(Vec3 offset, float bodyYawDeg) {
      double r = Math.toRadians((double)bodyYawDeg);
      double cos = Math.cos(r);
      double sin = Math.sin(r);
      return new Vec3(offset.x * cos + offset.z * sin, offset.y, -offset.x * sin + offset.z * cos);
   }

   public static Vec3 fromBodyFrame(Vec3 local, float bodyYawDeg) {
      double r = Math.toRadians((double)bodyYawDeg);
      double cos = Math.cos(r);
      double sin = Math.sin(r);
      return new Vec3(local.x * cos - local.z * sin, local.y, local.x * sin + local.z * cos);
   }

   RuinDecay withPulse(byte kind, long tick, int amount) {
      return new RuinDecay(this.stacks, this.applier, this.lastApplyTick, this.originX, this.originY, this.originZ, this.pulseSeq + 1, kind, tick, amount);
   }

   RuinDecay withStacks(int count) {
      return new RuinDecay(
         count, this.applier, this.lastApplyTick, this.originX, this.originY, this.originZ, this.pulseSeq, this.pulseKind, this.pulseTick, this.pulseAmount
      );
   }
}
