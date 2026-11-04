package com.devilfruits.fruits.yomi_yomi;

import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.FormSwitchAbility;
import com.devilfruits.fruits.yomi_yomi.ability.AubadeAbility;
import com.devilfruits.fruits.yomi_yomi.ability.ColdSoulAbility;
import com.devilfruits.fruits.yomi_yomi.ability.EarthCrushingAbility;
import com.devilfruits.fruits.yomi_yomi.ability.FubukiGiriAbility;
import com.devilfruits.fruits.yomi_yomi.ability.GavotteAbility;
import com.devilfruits.fruits.yomi_yomi.ability.GhostStrikeAbility;
import com.devilfruits.fruits.yomi_yomi.ability.PolkaRemiseAbility;
import com.devilfruits.fruits.yomi_yomi.ability.SoulDashAbility;
import com.devilfruits.fruits.yomi_yomi.ability.SoulKingRoarAbility;
import com.devilfruits.fruits.yomi_yomi.ability.SoulProjectionAbility;

public final class YomiAbilities {
   public static final AubadeAbility AUBADE = new AubadeAbility();
   public static final GavotteAbility GAVOTTE = new GavotteAbility();
   public static final FubukiGiriAbility FUBUKI_GIRI = new FubukiGiriAbility();
   public static final PolkaRemiseAbility POLKA = new PolkaRemiseAbility();
   public static final SoulProjectionAbility SOUL_PROJECTION = new SoulProjectionAbility();
   public static final SoulDashAbility SOUL_DASH = new SoulDashAbility();
   public static final ColdSoulAbility COLD_SOUL = new ColdSoulAbility();
   public static final FormSwitchAbility SOUL_KING = new FormSwitchAbility(
      YomiYomi.id("soul_king"), "soul_king", AbilityStats.builder().energy(0).cooldown(0).mastery(9).damage(0.0).range(1.0).knockback(0.0).build()
   );
   public static final GhostStrikeAbility KING_STRIKE = new GhostStrikeAbility();
   public static final EarthCrushingAbility EARTH_CRUSHING = new EarthCrushingAbility();
   public static final SoulKingRoarAbility SOUL_KING_ROAR = new SoulKingRoarAbility();

   private YomiAbilities() {
   }
}
