package com.devilfruits.fruits.kage_kage;

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
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModuleContext;
import com.devilfruits.core.fruit.FruitTheme;
import com.devilfruits.core.gacha.GachaTier;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.kage_kage.ability.DoppelmanAbility;
import com.devilfruits.fruits.kage_kage.client.KageClient;
import com.devilfruits.fruits.kage_kage.datagen.KageKageData;
import com.devilfruits.fruits.kage_kage.entity.Doppelman;
import com.devilfruits.fruits.kage_kage.entity.ShadowZombie;
import com.devilfruits.fruits.kage_kage.gametest.KageGameTests;
import com.devilfruits.fruits.kage_kage.shadow.Shadows;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFDamageTypes;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.bus.api.IEventBus;

public final class KageKage implements FruitModule {
   public static final ResourceLocation ID = DevilFruits.id("kage_kage");
   public static final String BASE = "base";
   public static final ResourceKey<DamageType> SHADOW_DAMAGE = DFDamageTypes.key("kage_kage/shadow");
   public static final int AEGIS = FormFlags.fruit(0);
   private static DevilFruit fruit;

   public static ResourceLocation id(String path) {
      return DevilFruits.id("kage_kage/" + path);
   }

   public static DevilFruit fruit() {
      return fruit;
   }

   public static boolean isUser(Player player) {
      return player.hasData(DFAttachments.PLAYER_DATA) && ID.equals(DFAttachments.data(player).fruitId());
   }

   public static boolean canUse(Player player) {
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
      KageItems.init();
      KageEntities.init(ctx.modBus());
      ctx.modBus().addListener(KageNetwork::register);
      KageSounds.init();
      KageTriggers.init();
      Shadows.init();
      fruit = ctx.registerFruit(
         DevilFruit.builder(ID, DevilFruitType.PARAMECIA)
            .item(KageItems.FRUIT)
            .form(
               FruitForm.builder("base")
                  .ability(KageAbilities.BRICK_BAT)
                  .ability(KageAbilities.SPEAR)
                  .ability(KageAbilities.COLLECTION)
                  .ability(KageAbilities.TRAP)
                  .ability(KageAbilities.HAND)
                  .ability(KageAbilities.DOPPELMAN)
            )
            .energy(new EnergyRules(150, 10, 3.0, 8.0))
            .gachaTier(GachaTier.UNCOMMON)
            .lootWeight(6)
            .theme(FruitTheme.sprites("kage_kage").colors(12868576, 15189234, 11554768, 14708991, 15189234, 14708991))
            .ultimate(KageAbilities.DOPPELMAN)
            .damageType(SHADOW_DAMAGE)
            .setting("maxShadows", 100.0, 0.0, 1000.0, "Most shadows one user can hold at once (Shadow Collection refuses more).")
            .setting("shadowDamageBonus", 0.003, 0.0, 0.1, "Kage technique damage added per held shadow (0.003 = +0.3 %).")
            .setting("shadowHealthBonus", 0.2, 0.0, 4.0, "Max health added per held shadow (0.2 = a tenth of a heart).")
            .setting("shadowSpeedBonus", 5.0E-4, 0.0, 0.01, "Movement speed added per held shadow (fraction of the base speed).")
            .setting("shadowSpeedCap", 0.03, 0.0, 0.5, "Most movement speed the held shadows add altogether (0.03 = +3 %).")
            .setting("sunburnSeconds", 8.0, 1.0, 60.0, "Seconds a shadowless creature burns each time direct sunlight catches it.")
            .setting("maxZombies", 6.0, 0.0, 32.0, "Most Shadow Zombies one user can command at once.")
            .setting("fleshPerKill", 1.0, 0.0, 8.0, "Cadaver Flesh dropped by a fleshy mob killed with Moria's scissors (Looting adds up to its level more).")
            .setting("captainExtraFlesh", 2.0, 0.0, 16.0, "Extra Cadaver Flesh a pirate captain drops.")
            .flag("takeFromPlayers", true, "Shadow Collection can take the shadows of other players (PvP rules still apply).")
            .flag("takeFromBosses", false, "Shadow Collection can take the shadows of bosses (#c:bosses).")
            .flag("helmetProtects", true, "A helmet keeps the sun off a shadowless head (the vanilla zombie rule; the helmet wears instead).")
            .flag("saltExpels", false, "Unverified canon detail: salt thrown at a Shadow Zombie drives its shadow out. Off by default.")
            .build()
      );
      KageEvents.register(ctx.gameBus());
      Shadows.onRelease(user -> {
         for (ServerLevel level : user.server.getAllLevels()) {
            for (ShadowZombie z : level.getEntities((EntityTypeTest)KageEntities.SHADOW_ZOMBIE.get(), zx -> user.getUUID().equals(zx.ownerId()))) {
               z.collapse();
            }
         }

         Doppelman d = DoppelmanAbility.doubleOf(user);
         if (d != null) {
            d.recall();
         }
      });
      FruitRules.onPowerRemoved((player, f) -> {
         if (f == fruit) {
            Shadows.releaseAll(player, Shadows.Release.POWER_LOST);
         }
      });
   }

   @Override
   public void registerClient(IEventBus modBus) {
      KageClient.init(modBus);
   }

   @Override
   public FruitDataContributor data() {
      return new KageKageData();
   }

   @Override
   public List<Class<?>> gameTests() {
      return List.of(KageGameTests.class);
   }
}
