package com.devilfruits.fruits.bomu_bomu;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.DevilFruitType;
import com.devilfruits.core.fruit.EnergyRules;
import com.devilfruits.core.fruit.FruitFx;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModuleContext;
import com.devilfruits.core.fruit.FruitTheme;
import com.devilfruits.core.fx.ExplosionProfile;
import com.devilfruits.core.physics.PhysicsCommand;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.bomu_bomu.client.BomuClient;
import com.devilfruits.fruits.bomu_bomu.datagen.BomuBomuData;
import com.devilfruits.fruits.bomu_bomu.explosion.BombExplosionService;
import com.devilfruits.fruits.bomu_bomu.explosion.ExplosionSpec;
import com.devilfruits.fruits.bomu_bomu.gametest.BomuGameTests;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFDamageTypes;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;

public final class BomuBomu implements FruitModule {
   public static final ResourceLocation ID = DevilFruits.id("bomu_bomu");
   public static final String BASE = "base";
   public static final ResourceKey<DamageType> EXPLOSION_DAMAGE = DFDamageTypes.key("bomu_bomu/explosion");
   public static final int ARMED = FormFlags.fruit(0);
   public static final int VANISHED = FormFlags.fruit(1);
   private static DevilFruit fruit;

   public static ResourceLocation id(String path) {
      return DevilFruits.id("bomu_bomu/" + path);
   }

   public static DevilFruit fruit() {
      return fruit;
   }

   public static boolean isUser(Player player) {
      return player.hasData(DFAttachments.PLAYER_DATA) && ID.equals(DFAttachments.data(player).fruitId());
   }

   public static boolean immune(Player player) {
      return isUser(player) && !player.isSpectator() && !Weakness.isSuppressed(player);
   }

   public static double setting(String name) {
      return fruit.config().setting(name);
   }

   public static boolean flag(String name) {
      return fruit.config().flag(name);
   }

   @Nullable
   public static AbilityContext context(ServerPlayer player, Ability ability) {
      PlayerFruitData data = DFAttachments.data(player);
      FormAbility entry = AbilityManager.entryFor(data, ability);
      return entry == null ? null : new AbilityContext(entry, player, data, 1.0F);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }

   @Override
   public void register(FruitModuleContext ctx) {
      BomuItems.init();
      BomuSounds.init();
      BomuEntities.init();
      BomuTriggers.init();
      fruit = ctx.registerFruit(
         DevilFruit.builder(ID, DevilFruitType.PARAMECIA)
            .item(BomuItems.FRUIT)
            .form(
               FruitForm.builder("base")
                  .ability(BomuAbilities.PUNCH)
                  .ability(BomuAbilities.CANNON)
                  .ability(BomuAbilities.SHOT)
                  .ability(BomuAbilities.SELF_DESTRUCT)
            )
            .energy(new EnergyRules(140, 10, 3.0, 8.0))
            .lootWeight(6)
            .theme(FruitTheme.sprites("bomu_bomu").colors(15764076, 16174526, 15037263, 16766282, 16174526, 16757866))
            .fx(FruitFx.DEFAULT.withAwaken(BomuFx.AWAKEN))
            .ultimate(BomuAbilities.SELF_DESTRUCT)
            .damageType(EXPLOSION_DAMAGE)
            .setting("maxBlocksPerExplosion", 220.0, 0.0, 4096.0, "Most terrain blocks one Bomu explosion destroys (structure blocks have the Sable cap).")
            .setting("maxDebrisPerExplosion", 14.0, 0.0, 128.0, "Most flying debris entities one explosion throws (the rest break into particles).")
            .setting("maxExplosionsPerTickPerPlayer", 4.0, 1.0, 64.0, "Most Bomu explosions one player sets off in a single tick (the rest fizzle).")
            .setting("absorbFxCooldownTicks", 30.0, 0.0, 400.0, "Shortest gap between two 'explosion absorbed' shimmers on the user.")
            .setting("pistolTradeChance", 0.3, 0.0, 1.0, "Chance that a journeyman weaponsmith sells the Bomb Pistol (decided once per villager).")
            .setting("pistolPriceEmeralds", 24.0, 1.0, 64.0, "Emeralds the weaponsmith asks for the Bomb Pistol.")
            .setting("pistolPriceIron", 3.0, 0.0, 64.0, "Iron ingots the weaponsmith asks on top.")
            .setting("pistolLootChance", 0.03, 0.0, 1.0, "Chance of a Bomb Pistol in a shipwreck / buried treasure / dungeon chest (read on /reload).")
            .setting("pistolRounds", 6.0, 1.0, 12.0, "Rounds of breath one blow fills the Bomb Pistol's cylinder with.")
            .setting("pistolReloadSeconds", 2.0, 0.4, 10.0, "Seconds of the loading drill: the cylinder out, spun, blown into, shut.")
            .setting("gustKnockback", 0.6, 0.0, 4.0, "Shove of a shot of plain air (the pistol loaded by anyone but a Bomu Bomu user), blocks/tick.")
            .flag("immuneToKnockback", true, "Explosions do not knock the Bomu user back either (only damage is cancelled when off).")
            .flag(
               "permanentCraters",
               false,
               "Bomu craters in the terrain stay, like vanilla explosions; off: they follow the core debrisMode (TEMPORARY heals them, like every fruit's damage)."
            )
            .configUpdate(
               1,
               "abilities.nose_fancy_cannon.cooldownTicks",
               "abilities.explosive_shot.range",
               "abilities.explosive_shot.damage",
               "abilities.explosive_shot.energyCost",
               "abilities.explosive_shot.maxRadius",
               "abilities.explosive_shot.maxPlayerDamage",
               "settings.pistolReloadSeconds"
            )
            .configUpdate(2, "settings.permanentCraters")
            .build()
      );
      BomuEvents.register(ctx.gameBus());
      PhysicsCommand.registerBlast(
         (player, at, power) -> BombExplosionService.detonate(
                  player.serverLevel(),
                  at,
                  new ExplosionSpec(
                     (float)power,
                     (float)power,
                     1.0F,
                     (float)power * 3.0F,
                     (float)power * 0.3F,
                     0.0F,
                     power >= 8.0 ? ExplosionProfile.LARGE : (power >= 3.0 ? ExplosionProfile.MEDIUM : ExplosionProfile.SMALL),
                     BomuAbilities.SELF_DESTRUCT.id()
                  ),
                  player
               )
               .structureBlocks()
      );
   }

   @Override
   public void registerClient(IEventBus modBus) {
      BomuClient.init(modBus);
   }

   @Override
   public FruitDataContributor data() {
      return new BomuBomuData();
   }

   @Override
   public List<Class<?>> gameTests() {
      return List.of(BomuGameTests.class);
   }
}
