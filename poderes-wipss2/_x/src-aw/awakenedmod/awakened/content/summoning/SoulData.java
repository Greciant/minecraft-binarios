package com.awakenedmod.awakened.content.summoning;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class SoulData {
   public static final MapCodec<SoulData> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("souls", Map.of()).forGetter(d -> d.souls),
               Codec.STRING.optionalFieldOf("active", "").forGetter(d -> d.active),
               Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("loadout", Map.of()).forGetter(d -> d.loadout),
               Codec.BOOL.optionalFieldOf("bankLocked", false).forGetter(d -> d.bankLocked),
               Codec.STRING.listOf().optionalFieldOf("blacklist", List.of()).forGetter(d -> List.copyOf(d.blacklist)),
               Codec.STRING.listOf().optionalFieldOf("recent", List.of()).forGetter(d -> List.copyOf(d.recent)),
               Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("uses", Map.of()).forGetter(d -> d.uses)
            )
            .apply(instance, SoulData::new)
   );
   private static final StreamCodec<ByteBuf, Map<String, Integer>> COUNTS = ByteBufCodecs.map(
      i -> new LinkedHashMap(), ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT
   );
   private static final StreamCodec<ByteBuf, List<String>> TYPES = ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8);
   public static final StreamCodec<RegistryFriendlyByteBuf, SoulData> STREAM_CODEC = StreamCodec.of(
      (buf, data) -> {
         COUNTS.encode(buf, data.souls);
         ByteBufCodecs.STRING_UTF8.encode(buf, data.active);
         COUNTS.encode(buf, data.loadout);
         ByteBufCodecs.BOOL.encode(buf, data.bankLocked);
         TYPES.encode(buf, new ArrayList<>(data.blacklist));
         TYPES.encode(buf, data.recent);
         COUNTS.encode(buf, data.uses);
      },
      buf -> new SoulData(
            (Map<String, Integer>)COUNTS.decode(buf),
            (String)ByteBufCodecs.STRING_UTF8.decode(buf),
            (Map<String, Integer>)COUNTS.decode(buf),
            (Boolean)ByteBufCodecs.BOOL.decode(buf),
            (List<String>)TYPES.decode(buf),
            (List<String>)TYPES.decode(buf),
            (Map<String, Integer>)COUNTS.decode(buf)
         )
   );
   private final Map<String, Integer> souls;
   private String active;
   private final Map<String, Integer> loadout;
   private boolean bankLocked;
   private final Set<String> blacklist;
   private final List<String> recent;
   private final Map<String, Integer> uses;
   private static final int RECENT_MEMORY = 8;

   public SoulData() {
      this(Map.of(), "", Map.of(), false, List.of(), List.of(), Map.of());
   }

   private SoulData(
      Map<String, Integer> souls,
      String active,
      Map<String, Integer> loadout,
      boolean bankLocked,
      List<String> blacklist,
      List<String> recent,
      Map<String, Integer> uses
   ) {
      this.souls = new LinkedHashMap<>(souls);
      this.active = active;
      this.loadout = new LinkedHashMap<>(loadout);
      this.bankLocked = bankLocked;
      this.blacklist = new LinkedHashSet<>(blacklist);
      this.recent = new ArrayList<>(recent);
      this.uses = new LinkedHashMap<>(uses);
   }

   public List<String> recent() {
      return Collections.unmodifiableList(this.recent);
   }

   public int useCount(String type) {
      return this.uses.getOrDefault(type, 0);
   }

   public void recordUse(String type) {
      if (type != null && !type.isEmpty()) {
         this.uses.merge(type, 1, Integer::sum);
         this.recent.remove(type);
         this.recent.add(0, type);

         while (this.recent.size() > 8) {
            this.recent.remove(this.recent.size() - 1);
         }
      }
   }

   public Map<String, Integer> souls() {
      return this.souls;
   }

   public Map<String, Integer> loadout() {
      return this.loadout;
   }

   public int total() {
      int t = 0;

      for (int n : this.souls.values()) {
         t += n;
      }

      return t;
   }

   public int count(String type) {
      return this.souls.getOrDefault(type, 0);
   }

   public int add(String type, int amount, int cap) {
      int room = Math.max(0, cap - this.total());
      int stored = Math.min(amount, room);
      if (stored > 0) {
         this.souls.merge(type, stored, Integer::sum);
         if (this.active.isEmpty()) {
            this.active = type;
         }
      }

      return stored;
   }

   public boolean spendOne(String type) {
      int have = this.souls.getOrDefault(type, 0);
      if (have <= 0) {
         return false;
      } else {
         if (have == 1) {
            this.souls.remove(type);
         } else {
            this.souls.put(type, have - 1);
         }

         return true;
      }
   }

   public boolean bankLocked() {
      return this.bankLocked;
   }

   public void setBankLocked(boolean locked) {
      this.bankLocked = locked;
   }

   public Set<String> blacklist() {
      return Collections.unmodifiableSet(this.blacklist);
   }

   public boolean isBlacklisted(String type) {
      return this.blacklist.contains(type);
   }

   public void addBlacklist(String type) {
      this.blacklist.add(type);
   }

   public void removeBlacklist(String type) {
      this.blacklist.remove(type);
   }

   public String active() {
      return this.active;
   }

   public void setActive(String type) {
      this.active = type == null ? "" : type;
   }

   public boolean discard(String type, int amount) {
      int have = this.souls.getOrDefault(type, 0);
      if (have <= 0) {
         return false;
      } else {
         int left = amount <= 0 ? 0 : Math.max(0, have - amount);
         if (left <= 0) {
            this.souls.remove(type);
            this.loadout.remove(type);
            if (type.equals(this.active)) {
               this.active = "";
            }
         } else {
            this.souls.put(type, left);
         }

         return true;
      }
   }

   public void setLoadout(String type, int count) {
      if (count <= 0) {
         this.loadout.remove(type);
      } else {
         this.loadout.put(type, count);
      }
   }

   public int effectiveLoadout(String type) {
      return Math.min(this.loadout.getOrDefault(type, 0), this.count(type));
   }
}
