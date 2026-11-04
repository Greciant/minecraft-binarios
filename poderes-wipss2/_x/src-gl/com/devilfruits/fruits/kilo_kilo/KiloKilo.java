package com.devilfruits.fruits.kilo_kilo;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.DevilFruitType;
import com.devilfruits.core.fruit.EnergyRules;
import com.devilfruits.core.fruit.FruitFx;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModuleContext;
import com.devilfruits.core.fruit.FruitTheme;
import com.devilfruits.core.physics.EntityMass;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.kilo_kilo.client.KiloClient;
import com.devilfruits.fruits.kilo_kilo.datagen.KiloKiloData;
import com.devilfruits.fruits.kilo_kilo.gametest.KiloGameTests;
import com.devilfruits.fruits.kilo_kilo.weight.BodyWeight;
import com.devilfruits.fruits.kilo_kilo.weight.WeightManager;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFDamageTypes;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;

public final class KiloKilo implements FruitModule {
   public static final ResourceLocation ID = DevilFruits.id("kilo_kilo");
   public static final String BASE = "base";
   public static final ResourceKey<DamageType> PRESS_DAMAGE = DFDamageTypes.key("kilo_kilo/press");
   public static final Supplier<AttachmentType<BodyWeight>> WEIGHT = DFAttachments.ATTACHMENTS
      .register(
         "kilo_kilo/weight",
         () -> AttachmentType.builder(BodyWeight::new).serialize(BodyWeight.CODEC).copyOnDeath().sync((holder, to) -> true, BodyWeight.STREAM_CODEC).build()
      );
   private static DevilFruit fruit;

   public static ResourceLocation id(String path) {
      return DevilFruits.id("kilo_kilo/" + path);
   }

   public static DevilFruit fruit() {
      return fruit;
   }

   public static boolean isUser(Player player) {
      return player.hasData(DFAttachments.PLAYER_DATA) && ID.equals(DFAttachments.data(player).fruitId());
   }

   public static BodyWeight weight(Player player) {
      return (BodyWeight)player.getData(WEIGHT);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }

   @Override
   public void register(FruitModuleContext ctx) {
      KiloItems.init();
      KiloSounds.init();
      KiloParticles.init();
      KiloTriggers.init();
      fruit = ctx.registerFruit(
         DevilFruit.builder(ID, DevilFruitType.PARAMECIA)
            .item(KiloItems.FRUIT)
            .form(FruitForm.builder("base").ability(KiloAbilities.ONE_KILO).ability(KiloAbilities.PRESS))
            .energy(new EnergyRules(150, 15, 2.5, 7.0))
            .lootWeight(6)
            .theme(FruitTheme.sprites("kilo_kilo").colors(14472942, 15131378, 12893912, 16766282, 15131378, 15657718))
            .fx(FruitFx.DEFAULT.withAwaken(KiloFx.AWAKEN))
            .ultimate(KiloAbilities.PRESS)
            .damageType(PRESS_DAMAGE)
            .setting(
               "rampSecondsFullRange", 3.0, 0.2, 30.0, "Seconds the weight takes from 1 kg to 10,000 kg (log scale; shorter ramps take proportionally less)."
            )
            .setting("lightJumpBlocks", 50.0, 2.0, 200.0, "How high a jump rises at 1 kg (blocks).")
            .setting("lightTerminalSpeed", 0.1, 0.02, 1.0, "Fall speed at 1 kg (blocks per tick): a slow, floaty fall.")
            .setting("glideFallSpeed", 0.05, 0.01, 1.0, "Fall speed while gliding at 1 kg (hold jump while falling).")
            .setting("airControl", 1.0, 0.0, 5.0, "Extra steering in the air at 1 kg (1 = three times vanilla).")
            .setting("windDrift", 0.012, 0.0, 0.2, "Sideways drift of a light body in a thunderstorm (blocks per tick^2 at 1 kg; rain half of it).")
            .setting("heavyGravityMultiplier", 6.0, 1.0, 12.0, "Gravity at 10,000 kg (x vanilla).")
            .setting("maxFallSpeed", 3.0, 0.5, 3.9, "Hard fall-speed cap of a heavy body (blocks per tick; keeps chunk loading and block sweeps sane).")
            .setting("lightSpeedBonus", 0.2, 0.0, 2.0, "Extra walking speed at 1 kg (0.2 = +20%).")
            .setting("heavySpeedMultiplier", 0.45, 0.05, 1.0, "Walking speed at 10,000 kg (x normal).")
            .setting("lightStepHeight", 1.0, 0.6, 2.0, "Step height at 1 kg (blocks).")
            .setting("maxKnockbackMultiplier", 8.0, 1.0, 70.0, "Cap of the knockback a light body takes (70 / weight x normal).")
            .setting("lightDrainPerSecond", 3.0, 0.0, 100.0, "Energy per second at 1 kg (none near 70 kg, growing on a log scale).")
            .setting("heavyDrainPerSecond", 4.0, 0.0, 100.0, "Energy per second at 10,000 kg.")
            .setting("drainDeadZone", 0.08, 0.0, 0.9, "No drain within this fraction of the log range around 70 kg.")
            .setting("exhaustedReturnSeconds", 2.0, 0.1, 30.0, "Seconds the weight takes back to normal when the energy runs out.")
            .setting("footstepMinKg", 1000.0, 70.0, 10000.0, "From this weight up every step cracks the ground.")
            .setting("blockEnergyKJ", 120.0, 1.0, 100000.0, "Kinetic energy (kJ) one block of hardness ~2 costs a body smashing through it.")
            .setting("maxSmashDepth", 10.0, 0.0, 64.0, "Deepest a heavy body smashes in one go, in blocks along its travel from the first block it breaks.")
            .setting(
               "thinFloorMaxBlocks", 1.0, 0.0, 4.0, "A heavy body standing on a layer this thick (blocks) with nothing under it goes through; 0 turns it off."
            )
            .setting("thinFloorMinKg", 5000.0, 70.0, 10000.0, "From this weight up a thin floor gives way under the body.")
            .setting(
               "thinFloorSeconds",
               0.4,
               0.05,
               10.0,
               "How long a thin floor of soft blocks creaks before it gives way (harder blocks and lighter bodies take longer)."
            )
            .setting("impactMinFall", 2.5, 0.0, 64.0, "Shortest fall (blocks) that lands with a shockwave.")
            .setting("impactMinEnergyKJ", 250.0, 0.0, 100000.0, "Least kinetic energy (kJ) of a landing with a shockwave.")
            .setting("crushDamageCap", 60.0, 0.0, 1000.0, "Most damage a body directly underneath a landing takes.")
            .setting("contactMinEnergyKJ", 40.0, 0.0, 100000.0, "Least kinetic energy (kJ) of the body that strikes what it runs into or falls on.")
            .setting("contactCooldownTicks", 10.0, 1.0, 200.0, "Ticks before the same body can be struck on contact again.")
            .setting(
               "pressBurstRadius",
               3.0,
               0.0,
               16.0,
               "Radius (blocks) of the 10,000 Kilo Press's burst round its first contact of a fall; the least radius of its landing."
            )
            .flag("heavyIgnoresPushing", true, "A heavy body (from 3,000 kg) is not pushed by water currents or pistons.")
            .flag("keepWeightWithKeepFruit", false, "With the keepFruit game rule, the weight survives death, relogging and changing dimension.")
            .build()
      );
      EntityMass.register(WeightManager.MASS);
      KiloEvents.register(ctx.gameBus());
   }

   @Override
   public void registerClient(IEventBus modBus) {
      KiloClient.init(modBus);
   }

   @Override
   public FruitDataContributor data() {
      return new KiloKiloData();
   }

   @Override
   public List<Class<?>> gameTests() {
      return List.of(KiloGameTests.class);
   }
}
