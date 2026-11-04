package com.awakenedmod.awakened.content.graviton;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.AttachmentType;

public record GravityWellVisual(
   boolean live, Vec3 point, Direction face, boolean surface, int chain, boolean arrived, int opens, int ends, int endKind, float lean, int planet
) {
   public static final int END_ARRIVED = 0;
   public static final int END_LET_GO = 1;
   public static final GravityWellVisual NONE = new GravityWellVisual(false, Vec3.ZERO, Direction.UP, false, 0, false, 0, 0, 0, 0.0F, -1);
   private static final int WRAP = 16384;
   private static final int F_LIVE = 1;
   private static final int F_SURFACE = 2;
   private static final int F_ARRIVED = 4;
   public static final StreamCodec<ByteBuf, GravityWellVisual> STREAM_CODEC = StreamCodec.of((buf, v) -> {
      buf.writeByte((v.live ? 1 : 0) | (v.surface ? 2 : 0) | (v.arrived ? 4 : 0));
      buf.writeDouble(v.point.x);
      buf.writeDouble(v.point.y);
      buf.writeDouble(v.point.z);
      buf.writeByte(v.face.get3DDataValue());
      ByteBufCodecs.VAR_INT.encode(buf, v.chain);
      ByteBufCodecs.VAR_INT.encode(buf, v.opens);
      ByteBufCodecs.VAR_INT.encode(buf, v.ends);
      buf.writeByte(v.endKind);
      buf.writeFloat(v.lean);
      buf.writeInt(v.planet);
   }, buf -> {
      int flags = buf.readByte();
      Vec3 point = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
      Direction face = Direction.from3DDataValue(buf.readByte());
      int chain = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
      int opens = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
      int ends = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
      int endKind = buf.readByte();
      float lean = buf.readFloat();
      int planet = buf.readInt();
      return new GravityWellVisual((flags & 1) != 0, point, face, (flags & 2) != 0, chain, (flags & 4) != 0, opens, ends, endKind, lean, planet);
   });
   public static final AttachmentType<GravityWellVisual> TYPE = AttachmentType.builder(() -> NONE).sync(STREAM_CODEC).build();

   public GravityWellVisual opened(GravityWellMotion.Target target, int chain, Vec3 from, int planet) {
      Vec3 back = from.subtract(target.point());
      float lean = (float)Math.atan2(back.z, back.x);
      return new GravityWellVisual(true, target.point(), target.face(), target.surface(), chain, false, bump(this.opens), this.ends, this.endKind, lean, planet);
   }

   public static int bump(int counter) {
      return (counter + 1) % 16384;
   }

   public GravityWellVisual arrived(boolean now) {
      return new GravityWellVisual(this.live, this.point, this.face, this.surface, this.chain, now, this.opens, this.ends, this.endKind, this.lean, this.planet);
   }

   public GravityWellVisual ended(int kind) {
      return new GravityWellVisual(
         false, this.point, this.face, this.surface, this.chain, this.arrived, this.opens, bump(this.ends), kind, this.lean, this.planet
      );
   }

   public GravityWellVisual withPlanet(int next) {
      return new GravityWellVisual(
         this.live, this.point, this.face, this.surface, this.chain, this.arrived, this.opens, this.ends, this.endKind, this.lean, next
      );
   }

   public Vec3 planetCenter(Level level, double radius) {
      return GravityWellPlanet.center(level, this.point, this.face, this.surface, radius);
   }

   public Vec3 throat() {
      return throat(this.point, this.face);
   }

   public static Vec3 axis(Direction face) {
      return new Vec3((double)face.getStepX(), (double)face.getStepY(), (double)face.getStepZ());
   }

   public static Vec3 throat(Vec3 point, Direction face) {
      return switch (face) {
         case UP -> point.add(0.0, 0.04, 0.0);
         case DOWN -> point.add(0.0, 1.81, 0.0);
         default -> point.add(0.0, 0.9, 0.0).subtract(axis(face).scale(0.41000000000000003));
      };
   }

   public static GravityWellVisual of(Player player) {
      return (GravityWellVisual)player.getData(TYPE);
   }

   public static void set(Player player, GravityWellVisual next) {
      if (!next.equals(player.getData(TYPE))) {
         player.setData(TYPE, next);
      }
   }
}
