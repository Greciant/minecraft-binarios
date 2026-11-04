package com.awakenedmod.awakened.core.data;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ExoVisual(
   boolean suited,
   boolean flying,
   boolean jet,
   boolean afterburner,
   boolean failsafeArmed,
   boolean failsafeReady,
   boolean analyzing,
   boolean chipInstalled,
   int builtMask,
   boolean barrageActive,
   boolean handCannonDeploy,
   boolean visorOpen,
   int flightSpeed,
   int boostCd,
   int brakeLevel,
   List<Integer> taggedIds,
   List<Integer> revealedIds
) {
   public static final ExoVisual STOWED = new ExoVisual(
      false, false, false, false, false, false, false, false, 0, false, false, false, 0, 0, 0, List.of(), List.of()
   );
   public static final int SPUTTER_SENTINEL = 255;
   private static final int BIT_SUITED = 1;
   private static final int BIT_FLYING = 2;
   private static final int BIT_JET = 4;
   private static final int BIT_FS_ARMED = 8;
   private static final int BIT_FS_READY = 16;
   private static final int BIT_ANALYZING = 32;
   private static final int BIT_CHIP = 64;
   private static final int BIT_AFTERBURNER = 128;
   private static final int MASK_PIECE_BITS = 63;
   private static final int BIT_BARRAGE = 64;
   private static final int BIT_HANDCANNON = 128;
   private static final StreamCodec<ByteBuf, ExoVisual.TagSets> TAG_SETS_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()),
      ExoVisual.TagSets::tagged,
      ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()),
      ExoVisual.TagSets::revealed,
      ExoVisual.TagSets::new
   );
   public static final StreamCodec<ByteBuf, ExoVisual> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BYTE,
      ExoVisual::packedFlags,
      ByteBufCodecs.BYTE,
      ExoVisual::packedMask,
      ByteBufCodecs.BYTE,
      ExoVisual::packedSpeed,
      ByteBufCodecs.BYTE,
      v -> (byte)v.boostCd,
      ByteBufCodecs.BYTE,
      v -> (byte)v.brakeLevel,
      TAG_SETS_CODEC,
      v -> new ExoVisual.TagSets(v.taggedIds, v.revealedIds),
      ExoVisual::fromWire
   );
   private static final int BIT_VISOR = 128;
   private static final int SPEED_BITS = 127;

   public boolean boostReady() {
      return !this.afterburner && this.boostCd == 0;
   }

   public boolean sputtering() {
      return !this.jet && this.brakeLevel == 255;
   }

   private byte packedFlags() {
      return (byte)(
         (this.suited ? 1 : 0)
            | (this.flying ? 2 : 0)
            | (this.jet ? 4 : 0)
            | (this.afterburner ? 128 : 0)
            | (this.failsafeArmed ? 8 : 0)
            | (this.failsafeReady ? 16 : 0)
            | (this.analyzing ? 32 : 0)
            | (this.chipInstalled ? 64 : 0)
      );
   }

   private byte packedMask() {
      return (byte)(this.builtMask & 63 | (this.barrageActive ? 64 : 0) | (this.handCannonDeploy ? 128 : 0));
   }

   private byte packedSpeed() {
      return (byte)(Math.min(this.flightSpeed, 127) | (this.visorOpen ? 128 : 0));
   }

   private static ExoVisual fromWire(byte flags, byte mask, byte speed, byte boostCd, byte brake, ExoVisual.TagSets tags) {
      return new ExoVisual(
         (flags & 1) != 0,
         (flags & 2) != 0,
         (flags & 4) != 0,
         (flags & 128) != 0,
         (flags & 8) != 0,
         (flags & 16) != 0,
         (flags & 32) != 0,
         (flags & 64) != 0,
         mask & 63,
         (mask & 64) != 0,
         (mask & 128) != 0,
         (speed & 128) != 0,
         speed & 127,
         boostCd & 255,
         brake & 255,
         tags.tagged(),
         tags.revealed()
      );
   }

   private static record TagSets(List<Integer> tagged, List<Integer> revealed) {
   }
}
