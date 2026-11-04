package com.devilfruits.core.haki;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public final class PlayerHakiData {
   public static final Codec<PlayerHakiData> CODEC = RecordCodecBuilder.create(
      i -> i.group(
               Codec.BOOL.optionalFieldOf("armament", false).forGetter(d -> d.armament),
               Codec.INT.optionalFieldOf("level", 1).forGetter(d -> d.level),
               Codec.FLOAT.optionalFieldOf("xp", 0.0F).forGetter(d -> d.xp),
               Codec.INT.optionalFieldOf("selected", 0).forGetter(d -> d.selected),
               Codec.BOOL.optionalFieldOf("active", false).forGetter(d -> d.active),
               Codec.INT.optionalFieldOf("mask", 0).forGetter(d -> d.mask),
               Codec.FLOAT.optionalFieldOf("spirit", -1.0F).forGetter(d -> d.spirit),
               Codec.LONG.optionalFieldOf("xp_day", 0L).forGetter(d -> d.xpDay),
               Codec.FLOAT.optionalFieldOf("xp_today", 0.0F).forGetter(d -> d.xpToday)
            )
            .apply(i, PlayerHakiData::new)
   );
   public static final StreamCodec<ByteBuf, PlayerHakiData> STREAM_CODEC = StreamCodec.of((buf, d) -> {
      buf.writeBoolean(d.armament);
      buf.writeByte(d.level);
      buf.writeFloat(d.xp);
      buf.writeShort(d.selected);
      buf.writeBoolean(d.active);
      buf.writeShort(d.mask);
      buf.writeFloat(d.spirit);
      buf.writeByte(d.ryuoCharge);
   }, buf -> {
      PlayerHakiData d = new PlayerHakiData();
      d.armament = buf.readBoolean();
      d.level = buf.readByte();
      d.xp = buf.readFloat();
      d.selected = buf.readShort();
      d.active = buf.readBoolean();
      d.mask = buf.readShort();
      d.spirit = buf.readFloat();
      d.ryuoCharge = buf.readByte();
      return d;
   });
   private boolean armament;
   private int level = 1;
   private float xp;
   private int selected;
   private boolean active;
   private int mask;
   private float spirit = -1.0F;
   private long xpDay;
   private float xpToday;
   private int ryuoCharge;
   private int regenDelay;
   private int syncTimer;
   final Map<ResourceLocation, ArrayDeque<Long>> recentKills = new HashMap<>();
   final Int2LongOpenHashMap lastClash = new Int2LongOpenHashMap();

   public PlayerHakiData() {
   }

   private PlayerHakiData(boolean armament, int level, float xp, int selected, boolean active, int mask, float spirit, long xpDay, float xpToday) {
      this.armament = armament;
      this.level = Math.max(1, Math.min(10, level));
      this.xp = xp;
      this.selected = selected;
      this.active = active;
      this.mask = mask;
      this.spirit = spirit;
      this.xpDay = xpDay;
      this.xpToday = xpToday;
   }

   public boolean armament() {
      return this.armament;
   }

   public int level() {
      return this.level;
   }

   public HakiTier tier() {
      return HakiTier.of(this.level);
   }

   public float xp() {
      return this.xp;
   }

   public int selected() {
      return this.selected;
   }

   public boolean active() {
      return this.active;
   }

   public int mask() {
      return this.active ? this.mask : 0;
   }

   public float spirit() {
      return this.spirit;
   }

   public int ryuoCharge() {
      return this.ryuoCharge;
   }

   void setArmament(boolean armament) {
      this.armament = armament;
   }

   void setLevel(int level) {
      this.level = Math.max(1, Math.min(10, level));
   }

   void setXp(float xp) {
      this.xp = Math.max(0.0F, xp);
   }

   void setSelected(int selected) {
      this.selected = selected;
   }

   void setCoat(boolean active, int mask) {
      this.active = active;
      this.mask = active ? mask : 0;
   }

   void setSpirit(float spirit) {
      this.spirit = spirit;
   }

   void setRyuoCharge(int ticks) {
      this.ryuoCharge = ticks;
   }

   int regenDelay() {
      return this.regenDelay;
   }

   void setRegenDelay(int ticks) {
      this.regenDelay = ticks;
   }

   boolean syncDue() {
      if (--this.syncTimer <= 0) {
         this.syncTimer = 10;
         return true;
      } else {
         return false;
      }
   }

   long xpDay() {
      return this.xpDay;
   }

   float xpToday() {
      return this.xpToday;
   }

   void setXpToday(long day, float amount) {
      this.xpDay = day;
      this.xpToday = amount;
   }

   void reset() {
      this.armament = false;
      this.level = 1;
      this.xp = 0.0F;
      this.selected = 0;
      this.active = false;
      this.mask = 0;
      this.spirit = -1.0F;
      this.ryuoCharge = 0;
      this.xpToday = 0.0F;
      this.recentKills.clear();
   }
}
