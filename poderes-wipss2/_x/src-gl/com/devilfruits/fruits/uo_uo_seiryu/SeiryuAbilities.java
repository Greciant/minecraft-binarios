package com.devilfruits.fruits.uo_uo_seiryu;

import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.FormSwitchAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.BlastBreathAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.BoloBreathAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.DraconicAscensionAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.DraconicCataclysmAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.DraconicTailSlamAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.DraconicThunderfallAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.DragonDashAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.DragonTwisterAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.FlameCloudAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.HellfireRayAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.RoarAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.SerpentineFlightAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.TailWhipAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.ThunderBaguaAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.ThunderStormAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.ThundercladVeilAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.ThunderclawRushAbility;

public final class SeiryuAbilities {
   public static final ThunderBaguaAbility THUNDER_BAGUA = new ThunderBaguaAbility();
   public static final BlastBreathAbility BLAST_BREATH = new BlastBreathAbility();
   public static final FlameCloudAbility FLAME_CLOUD = new FlameCloudAbility();
   public static final DragonDashAbility DRAGON_DASH = new DragonDashAbility();
   public static final BoloBreathAbility BOLO_BREATH = new BoloBreathAbility();
   public static final ThundercladVeilAbility THUNDERCLAD_VEIL = new ThundercladVeilAbility();
   public static final HellfireRayAbility HELLFIRE_RAY = new HellfireRayAbility();
   public static final ThunderclawRushAbility THUNDERCLAW_RUSH = new ThunderclawRushAbility();
   public static final DraconicCataclysmAbility DRACONIC_CATACLYSM = new DraconicCataclysmAbility();
   public static final DraconicAscensionAbility DRACONIC_ASCENSION = new DraconicAscensionAbility();
   public static final DraconicThunderfallAbility DRACONIC_THUNDERFALL = new DraconicThunderfallAbility();
   public static final DraconicTailSlamAbility DRACONIC_TAIL_SLAM = new DraconicTailSlamAbility();
   public static final SerpentineFlightAbility SERPENTINE_FLIGHT = new SerpentineFlightAbility();
   public static final DragonTwisterAbility DRAGON_TWISTER = new DragonTwisterAbility();
   public static final ThunderStormAbility THUNDER_STORM = new ThunderStormAbility();
   public static final TailWhipAbility TAIL_WHIP = new TailWhipAbility();
   public static final RoarAbility ROAR = new RoarAbility();
   public static final FormSwitchAbility TRANSFORM_HYBRID = new FormSwitchAbility(
      UoUoSeiryu.id("transform_hybrid"), "hybrid", AbilityStats.builder().energy(0).cooldown(0).mastery(3).damage(0.0).range(1.0).knockback(0.0).build()
   );
   public static final FormSwitchAbility TRANSFORM_DRAGON = new FormSwitchAbility(
      UoUoSeiryu.id("transform_dragon"), "dragon", AbilityStats.builder().energy(0).cooldown(0).mastery(6).damage(0.0).range(1.0).knockback(0.0).build()
   );

   private SeiryuAbilities() {
   }
}
