package com.awakenedmod.awakened.client.nominate;

import com.awakenedmod.awakened.Awakened;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;
import net.neoforged.fml.loading.FMLPaths;

public final class NominationLedger {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final long SESSION_GAP_MS = 14400000L;
   private static NominationLedger instance;
   private final Path file;
   private NominationLedger.Data data;

   private NominationLedger(Path file, NominationLedger.Data data) {
      this.file = file;
      this.data = data;
   }

   public static NominationLedger get() {
      if (instance == null) {
         Path file = FMLPaths.CONFIGDIR.get().resolve("awakened-nomination.json");
         instance = new NominationLedger(file, read(file));
      }

      return instance;
   }

   private static NominationLedger.Data read(Path file) {
      try {
         if (Files.exists(file)) {
            NominationLedger.Data d = (NominationLedger.Data)GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), NominationLedger.Data.class);
            if (d != null) {
               if (d.abilities == null) {
                  d.abilities = new LinkedHashSet<>();
               }

               return d;
            }
         }
      } catch (Exception var2) {
         Awakened.LOGGER.warn("Could not read {}, starting fresh: {}", file, var2.toString());
      }

      return new NominationLedger.Data();
   }

   public void save() {
      try {
         Files.writeString(this.file, GSON.toJson(this.data), StandardCharsets.UTF_8);
      } catch (IOException var2) {
         Awakened.LOGGER.warn("Could not write {}: {}", this.file, var2.toString());
      }
   }

   public boolean stopped() {
      return this.data.stopped;
   }

   public boolean clicked() {
      return this.data.clicked;
   }

   public int regularShown() {
      return this.data.regularShown;
   }

   public boolean creativeShown() {
      return this.data.creativeShown;
   }

   public boolean lastChanceShown() {
      return this.data.lastChanceShown;
   }

   public int sessions() {
      return this.data.sessions;
   }

   public int activeMinutes() {
      return this.data.activeMinutes;
   }

   public int abilitiesUsed() {
      return this.data.abilities.size();
   }

   public long daysSinceShown(LocalDate today) {
      if (this.data.lastShownDay == null) {
         return Long.MAX_VALUE;
      } else {
         try {
            return NominationCampaign.daysBetween(LocalDate.parse(this.data.lastShownDay), today);
         } catch (Exception var3) {
            return Long.MAX_VALUE;
         }
      }
   }

   public void beginSession(long nowMillis) {
      long last = this.data.lastPlayMillis;
      boolean newDay = last == 0L || !dayOf(last).equals(dayOf(nowMillis));
      if (last == 0L || newDay || nowMillis - last >= 14400000L) {
         this.data.sessions++;
      }

      this.data.lastPlayMillis = nowMillis;
      this.save();
   }

   public void playing(long nowMillis) {
      this.data.lastPlayMillis = nowMillis;
   }

   public boolean abilityUsed(String abilityKey, long nowMillis) {
      boolean changed = this.data.abilities.add(abilityKey);
      long minute = nowMillis / 60000L;
      if (minute != this.data.lastActiveMinute) {
         this.data.lastActiveMinute = minute;
         this.data.activeMinutes++;
         changed = true;
      }

      return changed;
   }

   public void shown(NominationCampaign.Kind kind, boolean inCreative, LocalDate today) {
      if (kind == NominationCampaign.Kind.LAST_CHANCE) {
         this.data.lastChanceShown = true;
      } else {
         this.data.regularShown++;
         this.data.creativeShown |= inCreative;
      }

      this.data.lastShownDay = today.toString();
      this.save();
   }

   public void click() {
      if (!this.data.clicked) {
         this.data.clicked = true;
         this.save();
      }
   }

   public void stop() {
      if (!this.data.stopped) {
         this.data.stopped = true;
         this.save();
      }
   }

   public void reset() {
      this.data = new NominationLedger.Data();
      this.save();
   }

   public void sessions(int sessions) {
      this.data.sessions = sessions;
      this.save();
   }

   public String describe() {
      return "stopped="
         + this.data.stopped
         + " clicked="
         + this.data.clicked
         + " regular="
         + this.data.regularShown
         + " creative="
         + this.data.creativeShown
         + " last="
         + this.data.lastChanceShown
         + " lastShown="
         + this.data.lastShownDay
         + " sessions="
         + this.data.sessions
         + " activeMinutes="
         + this.data.activeMinutes
         + " abilities="
         + this.data.abilities.size();
   }

   private static LocalDate dayOf(long millis) {
      return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
   }

   private static final class Data {
      boolean stopped;
      boolean clicked;
      int regularShown;
      boolean creativeShown;
      boolean lastChanceShown;
      String lastShownDay;
      int sessions;
      long lastPlayMillis;
      int activeMinutes;
      long lastActiveMinute;
      Set<String> abilities = new LinkedHashSet<>();
   }
}
