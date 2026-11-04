package com.awakenedmod.awakened.client.nominate;

import com.awakenedmod.awakened.client.ClientConfig;
import com.awakenedmod.awakened.core.rules.ServerRules;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.Util;

public final class NominationCampaign {
   public static final String URL = "https://overwolfdevs.typeform.com/to/S3mAnc9s?utm_source=cf_site&utm_medium=cta&utm_campaign=moty2026";
   public static final String PROJECT_URL = "https://www.curseforge.com/minecraft/mc-mods/the-awakening";
   static final LocalDate FIRST_LAST_DAY = LocalDate.of(2026, 11, 6);
   static final LocalDate SECOND_LAST_DAY = LocalDate.of(2026, 11, 5);
   static final LocalDate LAST_CHANCE_FROM = LocalDate.of(2026, 11, 7);
   static final LocalDate LAST_DAY = LocalDate.of(2026, 11, 8);
   static final int MASTERY_TO_QUALIFY = 3;
   static final int RETURNING_SESSIONS = 2;
   static final int DAYS_BEFORE_SECOND = 3;
   static final int DAYS_BETWEEN_ANY = 2;
   static final int FALLBACK_MINUTES = 20;
   static final int LAST_CHANCE_SESSION_MINUTES = 5;
   static final int CREATIVE_SESSIONS = 2;
   static final int CREATIVE_ACTIVE_MINUTES = 15;
   static final int CREATIVE_ABILITIES = 3;
   static final int CREATIVE_SESSION_MINUTES = 10;
   public static Supplier<LocalDate> today = LocalDate::now;
   public static Consumer<String> opener = url -> Util.getPlatform().openUri(url);

   private NominationCampaign() {
   }

   public static boolean live(LocalDate day) {
      return !day.isAfter(LAST_DAY);
   }

   public static boolean allowed() {
      return live(today.get()) && (Boolean)ClientConfig.NOMINATION_REMINDER.get() && ServerRules.nominationReminder();
   }

   public static boolean linkVisible() {
      return allowed();
   }

   public static NominationCampaign.Kind due(NominationLedger ledger, NominationCampaign.Moment m) {
      if (!ledger.stopped() && live(m.today()) && m.mode() != NominationCampaign.Mode.SPECTATOR) {
         long gap = ledger.daysSinceShown(m.today());
         if (!m.today().isBefore(LAST_CHANCE_FROM)) {
            boolean qualified = m.survivalQualified() || creativeQualified(ledger);
            return !ledger.lastChanceShown() && qualified && gap >= 2L && m.sessionMinutes() >= 5 ? NominationCampaign.Kind.LAST_CHANCE : null;
         } else {
            int shown = ledger.regularShown();
            if (!ledger.clicked() && shown < 2 && !m.today().isAfter(shown == 0 ? FIRST_LAST_DAY : SECOND_LAST_DAY) && (shown != 1 || gap >= 3L)) {
               if (m.mode() == NominationCampaign.Mode.CREATIVE) {
                  boolean ready = !ledger.creativeShown() && creativeQualified(ledger) && m.sessionMinutes() >= 10;
                  return ready ? (shown == 0 ? NominationCampaign.Kind.CREATIVE : NominationCampaign.Kind.SECOND) : null;
               } else {
                  boolean moment = m.highPoint() || m.survivalMinutes() >= 20;
                  return m.survivalQualified() && moment ? (shown == 0 ? NominationCampaign.Kind.FIRST : NominationCampaign.Kind.SECOND) : null;
               }
            } else {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   public static boolean qualifies(int bestMastery, NominationLedger ledger) {
      return bestMastery >= 3 || ledger.sessions() >= 2;
   }

   static boolean creativeQualified(NominationLedger ledger) {
      return ledger.sessions() >= 2 && ledger.activeMinutes() >= 15 && ledger.abilitiesUsed() >= 3;
   }

   static long daysBetween(LocalDate from, LocalDate to) {
      return ChronoUnit.DAYS.between(from, to);
   }

   public static enum Kind {
      FIRST("awakened.nominate.first"),
      CREATIVE("awakened.nominate.creative"),
      SECOND("awakened.nominate.second"),
      LAST_CHANCE("awakened.nominate.last");

      public final String key;

      private Kind(String key) {
         this.key = key;
      }
   }

   public static enum Mode {
      SURVIVAL,
      CREATIVE,
      SPECTATOR;
   }

   public static record Moment(
      LocalDate today, NominationCampaign.Mode mode, boolean survivalQualified, boolean highPoint, int survivalMinutes, int sessionMinutes
   ) {
   }
}
