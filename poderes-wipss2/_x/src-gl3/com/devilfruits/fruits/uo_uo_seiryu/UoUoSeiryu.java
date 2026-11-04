package com.devilfruits.fruits.uo_uo_seiryu;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.AbilityModifiers;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.DevilFruitType;
import com.devilfruits.core.fruit.EnergyRules;
import com.devilfruits.core.fruit.FruitFx;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModuleContext;
import com.devilfruits.core.fruit.FruitTheme;
import com.devilfruits.core.haki.HakiFormParts;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.uo_uo_seiryu.client.SeiryuClient;
import com.devilfruits.fruits.uo_uo_seiryu.datagen.SeiryuData;
import com.devilfruits.fruits.uo_uo_seiryu.gametest.HybridMovesetGameTests;
import com.devilfruits.fruits.uo_uo_seiryu.gametest.SeiryuGameTests;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFCreativeTabs;
import com.devilfruits.registry.DFDamageTypes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;

public final class UoUoSeiryu implements FruitModule {
   public static final ResourceLocation ID = DevilFruits.id("uo_uo_seiryu");
   public static final String HUMAN = "human";
   public static final String HYBRID = "hybrid";
   public static final String DRAGON = "dragon";
   public static final ResourceKey<DamageType> THUNDER_DAMAGE = DFDamageTypes.key("uo_uo_seiryu/thunder");
   public static final ResourceKey<DamageType> FLAME_DAMAGE = DFDamageTypes.key("uo_uo_seiryu/flame");
   public static final ResourceKey<DamageType> DRAGON_DAMAGE = DFDamageTypes.key("uo_uo_seiryu/dragon");
   public static final TagKey<Item> CLUBS = TagKey.create(Registries.ITEM, id("clubs"));
   private static DevilFruit fruit;

   public static ResourceLocation id(String path) {
      return DevilFruits.id("uo_uo_seiryu/" + path);
   }

   public static ResourceLocation anim(String name) {
      return id(name);
   }

   public static DevilFruit fruit() {
      return fruit;
   }

   public static boolean isUser(Player player) {
      return player.hasData(DFAttachments.PLAYER_DATA) && ID.equals(DFAttachments.data(player).fruitId());
   }

   public static boolean isIn(Player player, String form) {
      if (!player.hasData(DFAttachments.PLAYER_DATA)) {
         return false;
      } else {
         PlayerFruitData data = DFAttachments.data(player);
         return ID.equals(data.fruitId()) && form.equals(data.formName().isEmpty() ? "human" : data.formName());
      }
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }

   @Override
   public void register(FruitModuleContext ctx) {
      registerHakiParts();
      SeiryuItems.init();
      SeiryuSounds.init();
      SeiryuParticles.init();
      SeiryuEntities.init();
      SeiryuTriggers.init();
      fruit = ctx.registerFruit(
         DevilFruit.builder(ID, DevilFruitType.MYTHICAL_ZOAN)
            .item(SeiryuItems.FRUIT)
            .form(
               FruitForm.builder("human")
                  .ability(SeiryuAbilities.THUNDER_BAGUA)
                  .ability(SeiryuAbilities.BLAST_BREATH)
                  .ability(SeiryuAbilities.FLAME_CLOUD)
                  .ability(SeiryuAbilities.DRAGON_DASH)
                  .ability(SeiryuAbilities.TRANSFORM_HYBRID)
                  .ability(SeiryuAbilities.TRANSFORM_DRAGON)
                  .ultimate(SeiryuAbilities.THUNDER_BAGUA)
            )
            .form(
               FruitForm.builder("hybrid")
                  .ability(
                     SeiryuAbilities.THUNDER_BAGUA,
                     AbilityModifiers.variant("hybrid")
                        .damage(2.5F)
                        .range(1.8F)
                        .knockback(1.5F)
                        .energyCost(1.4F)
                        .animation(anim("hybrid/thunder_bagua"))
                        .build()
                  )
                  .ability(SeiryuAbilities.HELLFIRE_RAY)
                  .ability(SeiryuAbilities.THUNDERCLAW_RUSH)
                  .ability(SeiryuAbilities.DRACONIC_CATACLYSM)
                  .ability(SeiryuAbilities.THUNDERCLAD_VEIL)
                  .ability(SeiryuAbilities.DRACONIC_ASCENSION)
                  .ability(SeiryuAbilities.DRACONIC_THUNDERFALL)
                  .ability(SeiryuAbilities.DRACONIC_TAIL_SLAM)
                  .ability(
                     SeiryuAbilities.BLAST_BREATH,
                     AbilityModifiers.variant("hybrid").damage(2.0F).range(1.6F).energyCost(1.3F).animation(anim("hybrid/blast_breath")).build()
                  )
                  .ultimate(SeiryuAbilities.DRACONIC_CATACLYSM)
                  .attribute(Attributes.SCALE, 1.0, Operation.ADD_VALUE)
                  .attribute(Attributes.MAX_HEALTH, 20.0, Operation.ADD_VALUE)
                  .attribute(Attributes.ATTACK_DAMAGE, 4.0, Operation.ADD_VALUE)
                  .attribute(Attributes.ARMOR, 6.0, Operation.ADD_VALUE)
                  .attribute(Attributes.ARMOR_TOUGHNESS, 3.0, Operation.ADD_VALUE)
                  .attribute(Attributes.KNOCKBACK_RESISTANCE, 0.4, Operation.ADD_VALUE)
                  .attribute(Attributes.STEP_HEIGHT, 0.6, Operation.ADD_VALUE)
                  .attribute(Attributes.ENTITY_INTERACTION_RANGE, 1.5, Operation.ADD_VALUE)
                  .attribute(Attributes.BLOCK_INTERACTION_RANGE, 1.0, Operation.ADD_VALUE)
                  .attribute(Attributes.FALL_DAMAGE_MULTIPLIER, -1.0, Operation.ADD_MULTIPLIED_TOTAL)
                  .renderer(id("hybrid"))
                  .fireImmune()
                  .chargedJump(30.0F, SeiryuFx.CHARGED_JUMP)
                  .castTime(30)
                  .windupAnimation(anim("transform_hybrid"))
                  .enterAnimation(anim("hybrid/enter"))
                  .regen(0.35)
                  .enterFx(SeiryuFx.TRANSFORM_HYBRID)
                  .exitFx(SeiryuFx.REVERT)
                  .stats(3, 20, 0.0, 60)
            )
            .form(
               FruitForm.builder("dragon")
                  .ability(SeiryuAbilities.SERPENTINE_FLIGHT)
                  .ability(
                     SeiryuAbilities.BLAST_BREATH,
                     AbilityModifiers.variant("dragon").damage(3.5F).range(3.0F).energyCost(1.6F).animation(anim("dragon/blast_breath")).build()
                  )
                  .ability(
                     SeiryuAbilities.BOLO_BREATH,
                     AbilityModifiers.variant("dragon").damage(1.5F).range(1.5F).energyCost(1.4F).animation(anim("dragon/bolo_breath")).build()
                  )
                  .ability(SeiryuAbilities.DRAGON_TWISTER)
                  .ability(SeiryuAbilities.THUNDER_STORM)
                  .ability(SeiryuAbilities.TAIL_WHIP)
                  .ability(SeiryuAbilities.ROAR)
                  .ability(SeiryuAbilities.TRANSFORM_HYBRID)
                  .ultimate(SeiryuAbilities.THUNDER_STORM)
                  .attribute(Attributes.MAX_HEALTH, 60.0, Operation.ADD_VALUE)
                  .attribute(Attributes.ATTACK_DAMAGE, 8.0, Operation.ADD_VALUE)
                  .attribute(Attributes.ARMOR, 10.0, Operation.ADD_VALUE)
                  .attribute(Attributes.ARMOR_TOUGHNESS, 4.0, Operation.ADD_VALUE)
                  .attribute(Attributes.KNOCKBACK_RESISTANCE, 1.0, Operation.ADD_VALUE)
                  .attribute(Attributes.ENTITY_INTERACTION_RANGE, 3.0, Operation.ADD_VALUE)
                  .attribute(Attributes.BLOCK_INTERACTION_RANGE, 2.0, Operation.ADD_VALUE)
                  .attribute(Attributes.FALL_DAMAGE_MULTIPLIER, -1.0, Operation.ADD_MULTIPLIED_TOTAL)
                  .dimensions(2.4F, 2.2F, 1.3F)
                  .forcePose(Pose.STANDING)
                  .flight()
                  .boost(0.0)
                  .timeLimit(60)
                  .freeAbilities()
                  .passThroughLeaves()
                  .fireImmune()
                  .cameraDistance(14.0F)
                  .renderer(id("dragon"))
                  .castTime(50)
                  .windupAnimation(anim("transform_dragon"))
                  .regen(0.0)
                  .condition(SeiryuRules::dragonHasRoom)
                  .enterFx(SeiryuFx.TRANSFORM_DRAGON)
                  .exitFx(SeiryuFx.REVERT)
                  .stats(6, 0, 0.0, 100)
            )
            .energy(new EnergyRules(150, 15, 3.0, 8.0))
            .lootWeight(4)
            .theme(FruitTheme.sprites("uo_uo_seiryu").colors(16769162, 12576511, 7325951, 16766282, 9427199, 16771496))
            .fx(FruitFx.DEFAULT.withAwaken(SeiryuFx.AWAKEN).withLevelUp(SeiryuFx.LEVEL_UP).withReincarnate(SeiryuFx.REINCARNATE))
            .damageType(DRAGON_DAMAGE)
            .setting("meleeAuraDamage", 2.0, 0.0, 50.0, "Bonus damage of melee blows (doubled in Hybrid form): lightning and flame crackle on hit.")
            .setting("meleeIgniteChance", 0.25, 0.0, 1.0, "Chance that a melee blow sets the target on fire.")
            .setting(
               "hybridBaseHit",
               6.0,
               0.0,
               100.0,
               "Hybrid moveset: damage of a weight-1 hit at mastery 1 (each technique's 'damage' is its weight, e.g. 0.25 per Hellfire Ray hit)."
            )
            .setting("hybridDamageReduction", 0.15, 0.0, 0.9, "Hybrid form: fraction of incoming damage ignored (on top of its armor).")
            .flag("hybridAdaptiveSize", true, "The Hybrid form shrinks to fit doors and tunnels and grows back outside.")
            .setting("dragonBodyLength", 140.0, 12.0, 300.0, "Length of the Azure Dragon's body in blocks (head to tail tip).")
            .setting(
               "dragonHitboxParts",
               42.0,
               4.0,
               64.0,
               "Number of hitboxes along the dragon's body (attacks can hit the body); about one per 3.3 blocks of body leaves no gaps."
            )
            .setting("dragonFlightSpeed", 2.7, 0.2, 6.0, "Cruising flight speed of the dragon in blocks per tick.")
            .setting("dragonBoostSpeed", 5.1, 0.2, 8.0, "Flight speed while boosting in blocks per tick.")
            .setting("dragonGroundSpeed", 0.42, 0.1, 2.0, "Slithering speed on the ground in blocks per tick.")
            .build()
      );
      ctx.registerClientConfig(ID, SeiryuClientConfig.SPEC);
      DFCreativeTabs.addExtra(SeiryuItems.HASSAIKAI);
      SeiryuEvents.register(ctx.gameBus());
   }

   @Override
   public void registerClient(IEventBus modBus) {
      SeiryuClient.init(modBus);
   }

   @Override
   public FruitDataContributor data() {
      return new SeiryuData();
   }

   @Override
   public List<Class<?>> gameTests() {
      return List.of(SeiryuGameTests.class, HybridMovesetGameTests.class);
   }

   private static void registerHakiParts() {
      Map<Integer, List<String>> hybrid = new HashMap<>();
      hybrid.put(1, List.of("right_hand"));
      hybrid.put(2, List.of("left_hand"));
      hybrid.put(4, List.of("right_arm", "right_forearm", "right_hand"));
      hybrid.put(8, List.of("left_arm", "left_forearm", "left_hand"));
      hybrid.put(16, List.of("right_leg", "right_knee", "right_ankle", "right_foot"));
      hybrid.put(32, List.of("left_leg", "left_knee", "left_ankle", "left_foot"));
      hybrid.put(64, List.of("hips", "body", "neck", "tail_0", "tail_1", "tail_2", "tail_3", "tail_4", "tail_5", "tail_6", "tail_7", "tail_8"));
      hybrid.put(128, List.of("head", "jaw", "crown", "horn_r", "horn_l"));
      HakiFormParts.register(ID, "hybrid", hybrid);
      Map<Integer, List<String>> dragon = new HashMap<>();
      dragon.put(1, List.of("leg_front"));
      dragon.put(2, List.of("leg_front"));
      dragon.put(4, List.of("leg_front"));
      dragon.put(8, List.of("leg_front"));
      dragon.put(16, List.of("leg_back"));
      dragon.put(32, List.of("leg_back"));
      dragon.put(128, List.of("head", "jaw"));
      dragon.put(256, List.of("tail", "tail_tip"));
      dragon.put(64, List.of("body"));
      HakiFormParts.register(ID, "dragon", dragon);
   }
}
