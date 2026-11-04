package com.devilfruits.fruits.uo_uo_seiryu.gametest;

import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.CastResult;
import com.devilfruits.core.body.BodyFrames;
import com.devilfruits.core.body.SerpentBody;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.form.SafeLanding;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.gacha.GachaTier;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.gura_gura.GuraGura;
import com.devilfruits.fruits.gura_gura.GuraItems;
import com.devilfruits.fruits.uo_uo_seiryu.DragonShells;
import com.devilfruits.fruits.uo_uo_seiryu.HybridSize;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuAbilities;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuItems;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.uo_uo_seiryu.ability.TailWhipAbility;
import com.devilfruits.fruits.uo_uo_seiryu.entity.SeiryuBodyPart;
import com.devilfruits.fruits.uo_uo_seiryu.entity.SeiryuDragonEntity;
import com.devilfruits.registry.DFAttachments;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class SeiryuGameTests {
   private static final String PLATFORM = "platform";

   private static DevilFruit seiryu() {
      return UoUoSeiryu.fruit();
   }

   private static FakePlayer user(GameTestHelper helper, BlockPos relative, int mastery) {
      FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "seiryu_test"));
      BlockPos pos = helper.absolutePos(relative);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      FruitRules.grantPower(player, seiryu(), false);
      PlayerFruitData data = DFAttachments.data(player);
      data.setMastery(mastery);
      data.setEnergy(1000.0F);
      return player;
   }

   private static void done(GameTestHelper helper, FakePlayer... players) {
      for (FakePlayer p : players) {
         AbilityManager.cancelAll(p);
         AbilityManager.forget(p.getUUID());
         FormManager.forget(p);
         FruitRules.removePower(p);
      }

      for (DevilFruit fruit : FruitRegistry.all()) {
         WorldFruitData.get(helper.getLevel().getServer()).setFree(fruit);
      }

      helper.succeed();
   }

   private static List<String> names(FruitForm form) {
      return form.abilities().stream().map(e -> e.ability().name()).toList();
   }

   @GameTest(
      template = "platform"
   )
   public static void formListsShareAbilityIds(GameTestHelper helper) {
      FruitForm human = seiryu().baseForm();
      FruitForm hybrid = seiryu().form("hybrid");
      FruitForm dragon = seiryu().form("dragon");
      helper.assertTrue("human".equals(human.name()) && hybrid != null && dragon != null, "three forms");
      helper.assertTrue(
         names(human).equals(List.of("thunder_bagua", "blast_breath", "flame_cloud", "dragon_dash", "transform_hybrid", "transform_dragon")),
         "human list " + names(human)
      );
      helper.assertTrue(
         names(hybrid)
            .equals(
               List.of(
                  "thunder_bagua",
                  "hellfire_ray",
                  "thunderclaw_rush",
                  "draconic_cataclysm",
                  "thunderclad_veil",
                  "draconic_ascension",
                  "draconic_thunderfall",
                  "draconic_tail_slam",
                  "blast_breath"
               )
            ),
         "hybrid list " + names(hybrid)
      );
      helper.assertTrue(
         names(dragon)
            .equals(List.of("serpentine_flight", "blast_breath", "bolo_breath", "dragon_twister", "thunder_storm", "tail_whip", "roar", "transform_hybrid")),
         "dragon list " + names(dragon)
      );
      FormAbility humanBreath = human.find(SeiryuAbilities.BLAST_BREATH.id());
      FormAbility dragonBreath = dragon.find(SeiryuAbilities.BLAST_BREATH.id());
      helper.assertTrue(
         dragonBreath != null
            && humanBreath.ability() == dragonBreath.ability()
            && humanBreath.modifiers().isDefault()
            && "dragon".equals(dragonBreath.modifiers().variant())
            && dragonBreath.modifiers().damage() > 3.0F,
         "Blast Breath is one move with a much stronger dragon variant"
      );
      FormAbility humanBagua = human.find(SeiryuAbilities.THUNDER_BAGUA.id());
      FormAbility hybridBagua = hybrid.find(SeiryuAbilities.THUNDER_BAGUA.id());
      helper.assertTrue(
         humanBagua.ability() == hybridBagua.ability()
            && humanBagua.modifiers().isDefault()
            && "hybrid".equals(hybridBagua.modifiers().variant())
            && hybridBagua.modifiers().damage() > 2.0F,
         "Raimei Hakke is one move with a much stronger hybrid variant"
      );
      FormAbility hybridBreath = hybrid.find(SeiryuAbilities.BLAST_BREATH.id());
      helper.assertTrue(
         "hybrid".equals(hybridBreath.modifiers().variant()) && dragonBreath.modifiers().damage() > hybridBreath.modifiers().damage(),
         "the Hybrid breathes a beam, the dragon's is still the strongest"
      );
      helper.assertTrue(
         human.ultimate() == SeiryuAbilities.THUNDER_BAGUA
            && hybrid.ultimate() == SeiryuAbilities.DRACONIC_CATACLYSM
            && dragon.ultimate() == SeiryuAbilities.THUNDER_STORM,
         "per-form ultimates"
      );
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void formsUnlockByMastery(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 2), 2);
      PlayerFruitData data = DFAttachments.data(player);
      helper.assertTrue(FormManager.check(player, "hybrid") == FormManager.Result.LOCKED, "hybrid needs mastery 3");
      data.setMastery(3);
      helper.assertTrue(FormManager.check(player, "hybrid") == FormManager.Result.SWITCHED, "hybrid unlocks at mastery 3");
      helper.assertTrue(FormManager.check(player, "dragon") == FormManager.Result.LOCKED, "dragon needs mastery 6");
      data.setMastery(6);
      FormManager.Result dragon = FormManager.check(player, "dragon");
      helper.assertTrue(dragon == FormManager.Result.SWITCHED, "dragon unlocks at mastery 6, was " + dragon);
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 100
   )
   public static void hybridTransformationTakesItsCastTime(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 2), 10);
      PlayerFruitData data = DFAttachments.data(player);
      double scale = player.getAttributeValue(Attributes.SCALE);
      helper.assertTrue(FormManager.request(player, "hybrid", FormManager.Reason.PLAYER) == FormManager.Result.WINDUP, "the hybrid winds up");
      helper.assertTrue(data.formName().isEmpty(), "still human during the wind-up");
      helper.runAfterDelay(32L, () -> {
         CoreEvents.tickServer(player, data);
         helper.assertTrue("hybrid".equals(data.formName()), "hybrid after the cast time, was '" + data.formName() + "'");
         helper.assertTrue(player.getAttributeValue(Attributes.SCALE) > scale + 0.5, "the hybrid is much bigger");
         helper.assertTrue(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER) <= 1.0E-6, "the hybrid takes no fall damage");
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void dragonNeedsRoom(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 2), 10);

      for (int x = 0; x < 5; x++) {
         for (int z = 0; z < 5; z++) {
            helper.setBlock(new BlockPos(x, 4, z), Blocks.STONE);
         }
      }

      helper.assertTrue(FormManager.check(player, "dragon") == FormManager.Result.CONDITION, "no room under a 2-block ceiling");

      for (int x = 0; x < 5; x++) {
         for (int z = 0; z < 5; z++) {
            helper.setBlock(new BlockPos(x, 4, z), Blocks.OAK_LEAVES);
         }
      }

      FormManager.Result leaves = FormManager.check(player, "dragon");
      helper.assertTrue(leaves == FormManager.Result.SWITCHED, "leaves do not block the dragon, was " + leaves);
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void switchingFormsKeepsHealth(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 2), 10);
      FormManager.request(player, "hybrid", FormManager.Reason.COMMAND);
      player.setHealth(player.getMaxHealth());
      float hybridHealth = player.getHealth();
      helper.assertTrue(hybridHealth > 30.0F, "the hybrid has more health, was " + hybridHealth);
      FormManager.request(player, "dragon", FormManager.Reason.COMMAND);
      helper.assertTrue(UoUoSeiryu.isIn(player, "dragon"), "dragon now");
      helper.assertTrue(player.getHealth() == hybridHealth, "hybrid -> dragon keeps every health point, was " + player.getHealth());
      FormManager.revert(player, FormManager.Reason.COMMAND);
      helper.assertTrue(player.getHealth() == player.getMaxHealth(), "back to human: capped to the human maximum");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void secondFruitKillsASeiryuUser(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 2), 1);
      FruitRules.EatOutcome outcome = FruitRules.eat(player, GuraGura.fruit(), new ItemStack((ItemLike)GuraItems.FRUIT.get()));
      helper.assertTrue(outcome == FruitRules.EatOutcome.KILLED, "a second fruit kills, was " + outcome);
      helper.assertTrue(new ItemStack((ItemLike)SeiryuItems.FRUIT.get()).getItem() != null, "the fruit item exists");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void formSurvivesSerialization(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 2), 10);
      PlayerFruitData data = DFAttachments.data(player);
      FormManager.request(player, "hybrid", FormManager.Reason.COMMAND);
      Tag tag = (Tag)PlayerFruitData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
      PlayerFruitData decoded = (PlayerFruitData)PlayerFruitData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
      helper.assertTrue(
         UoUoSeiryu.ID.equals(decoded.fruitId()) && "hybrid".equals(decoded.formName()) && decoded.mastery() == 10, "fruit, form and mastery survive: " + tag
      );
      done(helper, player);
   }

   private static FakePlayer facingSouth(GameTestHelper helper, int mastery) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 0), mastery);
      BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 0));
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      return player;
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60
   )
   public static void thunderBaguaStrikesAhead(GameTestHelper helper) {
      FakePlayer player = facingSouth(helper, 1);
      Mob target = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 2));
      float health = target.getHealth();
      helper.assertTrue(AbilityManager.press(player, SeiryuAbilities.THUNDER_BAGUA) == CastResult.SUCCESS, "Thunder Bagua casts");
      helper.runAfterDelay(
         (long)SeiryuAbilities.THUNDER_BAGUA.config().extra("windupTicks") + 3L,
         () -> {
            helper.assertTrue(target.getHealth() < health, "the lightning blow hits the mob in front");
            helper.assertTrue(
               target.getDeltaMovement().horizontalDistanceSqr() > 0.0 || target.getZ() > (double)helper.absolutePos(new BlockPos(2, 2, 2)).getZ() + 0.5,
               "and knocks it back"
            );
            done(helper, player);
         }
      );
   }

   @GameTest(
      template = "platform"
   )
   public static void blastBreathBurnsTarget(GameTestHelper helper) {
      FakePlayer player = facingSouth(helper, 1);
      Mob target = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 3));
      float health = target.getHealth();
      helper.assertTrue(AbilityManager.press(player, SeiryuAbilities.BLAST_BREATH) == CastResult.CHARGING, "the breath charges");
      helper.assertTrue(AbilityManager.release(player, SeiryuAbilities.BLAST_BREATH) == CastResult.SUCCESS, "a quick tap still fires");
      helper.assertTrue(target.getHealth() < health && target.isOnFire(), "the fire hurts and ignites the target");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void flameCloudIgnitesWhatItTouches(GameTestHelper helper) {
      FakePlayer player = facingSouth(helper, 1);
      Mob target = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 1));
      helper.assertTrue(AbilityManager.press(player, SeiryuAbilities.FLAME_CLOUD) == CastResult.SUCCESS, "Flame Cloud Strike casts");
      helper.runAfterDelay(2L, () -> {
         helper.assertTrue(target.isOnFire(), "the burning cloud ignites the mob it touches");
         done(helper, player);
      });
   }

   private static FakePlayer hybrid(GameTestHelper helper, BlockPos relative, float pitch) {
      FakePlayer player = user(helper, relative, 10);
      FormManager.request(player, "hybrid", FormManager.Reason.COMMAND);
      BlockPos pos = helper.absolutePos(relative);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, pitch);
      return player;
   }

   @GameTest(
      template = "platform"
   )
   public static void hybridShrinksUnderALowCeiling(GameTestHelper helper) {
      FakePlayer player = hybrid(helper, new BlockPos(2, 2, 2), 0.0F);
      double full = player.getAttributeValue(Attributes.SCALE);

      for (int x = 0; x < 5; x++) {
         for (int z = 0; z < 5; z++) {
            helper.setBlock(new BlockPos(x, 5, z), Blocks.STONE);
         }
      }

      for (int i = 0; i < 20; i++) {
         HybridSize.tick(player);
      }

      double shrunk = player.getAttributeValue(Attributes.SCALE);
      helper.assertTrue(shrunk < full - 0.01 && shrunk >= 1.0, "shrinks but stays at least human-sized, was " + shrunk + " of " + full);
      helper.assertTrue(HybridSize.fits(player, shrunk), "and fits under the ceiling");

      for (int x = 0; x < 5; x++) {
         for (int z = 0; z < 5; z++) {
            helper.setBlock(new BlockPos(x, 5, z), Blocks.AIR);
         }
      }

      for (int i = 0; i < 40; i++) {
         HybridSize.tick(player);
      }

      helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.SCALE) - full) < 1.0E-6, "grows back to full size outside");
      FormManager.revert(player, FormManager.Reason.COMMAND);
      HybridSize.tick(player);
      helper.assertTrue(player.getAttribute(Attributes.SCALE).getModifier(HybridSize.MODIFIER) == null, "no size modifier outside the Hybrid");
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 80
   )
   public static void cataclysmSmashesEverythingAround(GameTestHelper helper) {
      FakePlayer player = hybrid(helper, new BlockPos(2, 2, 2), 0.0F);
      player.setOnGround(true);
      Mob target = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(4, 2, 2));
      float health = target.getHealth();
      helper.assertTrue(AbilityManager.press(player, SeiryuAbilities.DRACONIC_CATACLYSM) == CastResult.SUCCESS, "Draconic Cataclysm casts in Hybrid form");
      helper.runAfterDelay(10L, () -> helper.assertTrue(player.getDeltaMovement().y > 0.5, "the user leaps"));
      helper.runAfterDelay(34L, () -> {
         helper.assertTrue(target.getHealth() < health, "the smash hits the mob beside the user");
         helper.assertTrue(target.getDeltaMovement().y > 0.0 || target.getY() > (double)helper.absolutePos(new BlockPos(4, 2, 2)).getY(), "and throws it up");
         helper.assertTrue(player.fallDistance == 0.0F, "the leap causes no fall damage");
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void serpentBodyFollowsTheHeadsPath(GameTestHelper helper) {
      SerpentBody body = new SerpentBody(60.0F, 1.3F);
      body.reset(0.0, 100.0, 0.0, 0.0F, true);

      for (int t = 1; t <= 160; t++) {
         body.tick(0.0, 100.0, (double)t * 0.5, 0.0F, true);
      }

      BodyFrames f = new BodyFrames(64);
      body.sample(f, 64, 1.0F);
      double anchorZ = 80.0;
      double tailZ = f.z[f.count - 1];
      double expected = anchorZ - (double)body.neck - (double)body.length;
      helper.assertTrue(Math.abs(tailZ - expected) < 2.5, "the tail tip trails the whole length behind, z " + tailZ + " vs " + expected);

      for (int i = 0; i < f.count; i++) {
         helper.assertTrue(
            Math.abs(f.x[i]) < 2.6 && Math.abs(f.y[i] - (double)(100.0F + body.anchorHeight)) < 2.0,
            "the body stays on the path (with its wave), section " + i + " at " + f.x[i] + ", " + f.y[i]
         );
         float tl = f.tx[i] * f.tx[i] + f.ty[i] * f.ty[i] + f.tz[i] * f.tz[i];
         float ul = f.ux[i] * f.ux[i] + f.uy[i] * f.uy[i] + f.uz[i] * f.uz[i];
         float dot = f.tx[i] * f.ux[i] + f.ty[i] * f.uy[i] + f.tz[i] * f.uz[i];
         helper.assertTrue(
            (double)Math.abs(tl - 1.0F) < 0.001 && (double)Math.abs(ul - 1.0F) < 0.001 && (double)Math.abs(dot) < 0.001, "orthonormal frame at " + i
         );
         helper.assertTrue((double)Math.abs(f.sy[i]) < 0.001 && f.uy[i] > 0.0F, "the body never rolls about its axis, section " + i);
         if (i > 0) {
            double step = Math.sqrt(Math.pow(f.x[i] - f.x[i - 1], 2.0) + Math.pow(f.y[i] - f.y[i - 1], 2.0) + Math.pow(f.z[i] - f.z[i - 1], 2.0));
            helper.assertTrue(step > 0.5 && step < 1.4, "sections evenly spread, step " + step + " at " + i);
         }
      }

      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void serpentBodyCoilsAtRest(GameTestHelper helper) {
      SerpentBody body = new SerpentBody(60.0F, 1.3F);
      body.reset(0.0, 100.0, 0.0, 0.0F, false);

      for (int t = 1; t <= 120; t++) {
         body.tick(0.0, 100.0, (double)t * 0.5, 0.0F, false);
      }

      helper.assertTrue(body.coil(1.0F) == 0.0F, "uncoiled while moving");

      for (int t = 0; t < 120; t++) {
         body.tick(0.0, 100.0, 60.0, 0.0F, false);
      }

      helper.assertTrue(body.coil(1.0F) >= 1.0F, "coiled after resting, was " + body.coil(1.0F));
      BodyFrames f = new BodyFrames(48);
      body.sample(f, 48, 1.0F);

      for (int i = 0; i < f.count; i++) {
         double dx = f.x[i];
         double dz = f.z[i] - 60.0;
         helper.assertTrue(Math.sqrt(dx * dx + dz * dz) < 16.0, "the coil lies around the head, section " + i);
         helper.assertTrue(
            f.y[i] >= 100.0 && f.y[i] <= (double)(100.0F + body.thickness) + 0.3, "a slithering dragon coils on the ground, section " + i + " at " + f.y[i]
         );
      }

      helper.succeed();
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 40
   )
   public static void dragonBodyShellFollowsTheDragon(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 2, 2), 10);
      FormManager.request(player, "dragon", FormManager.Reason.COMMAND);
      DragonShells.tick(player);
      SeiryuDragonEntity shell = DragonShells.get(player);
      helper.assertTrue(shell != null, "entering the dragon form creates the body shell");
      helper.assertTrue(FormFlags.has(player, 2), "the dragon flies by default");
      helper.runAfterDelay(3L, () -> {
         helper.assertTrue(!shell.isRemoved() && shell.body() != null, "the shell keeps a body");
         int active = 0;

         for (PartEntity<?> part : shell.getParts()) {
            if (((SeiryuBodyPart)part).active()) {
               active++;
            }
         }

         helper.assertTrue(active == shell.partCount(), "every configured hitbox is placed, was " + active);
         helper.assertTrue(shell.getParts()[0].getBoundingBox().getSize() > 1.0, "body hitboxes are as thick as the body");
         FormManager.revert(player, FormManager.Reason.COMMAND);
      });
      helper.runAfterDelay(6L, () -> {
         helper.assertTrue(shell.isRemoved(), "leaving the dragon form removes the shell");
         DragonShells.tick(player);
         helper.assertTrue(DragonShells.get(player) == null, "and no new one is made");
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 80
   )
   public static void hitsOnTheBodyHurtTheOwnerOnly(GameTestHelper helper) {
      FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "seiryu_dragon_test")) {
         public boolean isInvulnerableTo(DamageSource source) {
            return false;
         }
      };
      BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      FruitRules.grantPower(player, seiryu(), false);
      PlayerFruitData data = DFAttachments.data(player);
      data.setMastery(10);
      data.setEnergy(1000.0F);
      FormManager.request(player, "dragon", FormManager.Reason.COMMAND);
      DragonShells.tick(player);
      boolean[] checked = new boolean[]{false};
      helper.onEachTick(() -> {
         SeiryuDragonEntity body = DragonShells.get(player);
         if (!checked[0] && body != null && ((SeiryuBodyPart)body.getParts()[3]).active()) {
            checked[0] = true;
            SeiryuDragonEntity shell = DragonShells.get(player);
            SeiryuBodyPart part = (SeiryuBodyPart)shell.getParts()[3];
            player.invulnerableTime = 0;
            float before = player.getHealth();
            helper.assertTrue(part.hurt(player.damageSources().fellOutOfWorld(), 6.0F), "something else can hit the body");
            helper.assertTrue(player.getHealth() < before, "and the owner takes the damage");
            player.invulnerableTime = 0;
            float after = player.getHealth();
            helper.assertFalse(part.hurt(player.damageSources().playerAttack(player), 6.0F), "the owner cannot hit its own body");
            helper.assertTrue(player.getHealth() == after, "and is not hurt by trying");
            FormManager.revert(player, FormManager.Reason.COMMAND);
            DragonShells.forget(player.getUUID());
            done(helper, player);
         }
      });
      helper.runAfterDelay(60L, () -> helper.assertTrue(checked[0], "the dragon has a body in place"));
   }

   @GameTest(
      template = "platform"
   )
   public static void seiryuIsALegendaryCasketFruit(GameTestHelper helper) {
      DevilFruit fruit = seiryu();
      helper.assertTrue(fruit.defaultGachaTier() == GachaTier.LEGENDARY, "a Mythical Zoan is legendary");
      helper.assertTrue(fruit.config().lootWeight() > 0, "and a casket does roll it (weight in its tier)");
      helper.succeed();
   }

   private static FakePlayer dragon(GameTestHelper helper, BlockPos relative, boolean slither) {
      FakePlayer player = user(helper, relative, 10);
      FormManager.request(player, "dragon", FormManager.Reason.COMMAND);
      if (slither) {
         FormFlags.set(player, DragonShells.SLITHER, true);
      }

      DragonShells.tick(player);
      return player;
   }

   private static Mob dummy(GameTestHelper helper, BlockPos relative) {
      Mob husk = helper.spawnWithNoFreeWill(EntityType.HUSK, relative);
      husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500.0);
      husk.setHealth(500.0F);
      return husk;
   }

   private static void doneDragon(GameTestHelper helper, FakePlayer player, Entity... mobs) {
      for (Entity mob : mobs) {
         mob.discard();
      }

      DragonShells.forget(player.getUUID());
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void tailSweepSwingsTheTailRound(GameTestHelper helper) {
      SerpentBody body = new SerpentBody(60.0F, 1.3F);
      body.reset(0.0, 100.0, 0.0, 0.0F, true);

      for (int t = 1; t <= 160; t++) {
         body.tick(0.0, 100.0, (double)t * 0.5, 0.0F, true);
      }

      double[] front = new double[3];
      double[] tip = new double[3];
      double[] pivot = new double[3];
      double dFront = (double)body.neck + (double)body.length * 0.2;
      double dTip = (double)body.neck + (double)body.length * 0.98;
      body.point(dFront, 1.0F, front);
      double frontX = front[0];
      double frontZ = front[2];
      body.sweep(26, 1.0F);

      for (int t = 0; t < 10; t++) {
         body.tick(0.0, 100.0, 80.0, 0.0F, true);
      }

      body.point((double)(body.neck + body.length * 0.35F), 0.0F, pivot);
      body.point(dTip, 0.0F, tip);
      helper.assertTrue(tip[0] < pivot[0] - 12.0, "the tail swung to the head's right, tip x " + tip[0] + ", pivot x " + pivot[0]);
      helper.assertTrue(tip[2] > pivot[2] + 15.0, "and round past the pivot, tip z " + tip[2] + ", pivot z " + pivot[2]);
      body.point(dFront, 0.0F, front);
      helper.assertTrue(Math.abs(front[0] - frontX) < 2.5 && Math.abs(front[2] - frontZ) < 2.5, "the front of the body stays where it was");

      for (int t = 0; t < 20; t++) {
         body.tick(0.0, 100.0, 80.0, 0.0F, true);
      }

      helper.assertTrue(body.action() == 0, "the sweep ends");
      body.point(dTip, 1.0F, tip);
      helper.assertTrue(tip[2] < pivot[2] - 25.0, "with the tail back behind, tip z " + tip[2]);
      helper.succeed();
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60,
      batch = "seiryu_dragon_tail_whip"
   )
   public static void tailWhipHitsWhatTheTailSweepsThrough(GameTestHelper helper) {
      FakePlayer player = dragon(helper, new BlockPos(2, 2, 2), true);
      helper.runAfterDelay(2L, () -> {
         SeiryuDragonEntity shell = DragonShells.get(player);
         helper.assertTrue(shell != null && shell.body() != null, "the dragon has a body");
         SerpentBody body = shell.body();
         double[] pivot = new double[3];
         double[] tip = new double[3];
         body.point((double)(body.neck + body.length * 0.35F), 1.0F, pivot);
         body.point((double)body.neck + (double)body.length * 0.9, 1.0F, tip);
         Vec3 at = null;

         for (int side = 1; side >= -1 && at == null; side -= 2) {
            double a = Math.toRadians(50.0) * (double)side;
            double c = Math.cos(a);
            double sn = Math.sin(a);
            double vx = tip[0] - pivot[0];
            double vz = tip[2] - pivot[2];
            Vec3 spot = new Vec3(pivot[0] + vx * c + vz * sn, tip[1] - 1.0, pivot[2] - vx * sn + vz * c);
            boolean right = player.getX() - spot.x >= 0.0;
            if (right == side > 0) {
               at = spot;
            }
         }

         helper.assertTrue(at != null, "a spot on the tail's path");
         Mob husk = dummy(helper, new BlockPos(0, 2, 0));
         husk.setNoGravity(true);
         husk.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
         Vec3 placed = at;
         float health = husk.getHealth();
         CastResult r2960 = AbilityManager.press(player, SeiryuAbilities.TAIL_WHIP);
         helper.assertTrue(r2960 == CastResult.COMBO_STEP, "the tail whip casts: " + r2960);
         helper.runAfterDelay(14L, () -> {
            helper.assertTrue(husk.getHealth() < health, "the sweeping tail hits the mob on its path");
            helper.assertTrue(husk.position().distanceTo(placed) > 0.5 || husk.getDeltaMovement().horizontalDistance() > 0.3, "and flings it");
            doneDragon(helper, player, husk);
         });
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 140,
      batch = "seiryu_dragon_coil_crush"
   )
   public static void coilCrushHoldsAndSqueezesItsVictim(GameTestHelper helper) {
      FakePlayer player = dragon(helper, new BlockPos(2, 2, 1), true);
      Mob husk = dummy(helper, new BlockPos(2, 2, 4));
      helper.runAfterDelay(2L, () -> {
         CastResult r8290 = AbilityManager.press(player, SeiryuAbilities.TAIL_WHIP);
         helper.assertTrue(r8290 == CastResult.COMBO_STEP, "the whip: " + r8290);
         CastResult r5308 = AbilityManager.press(player, SeiryuAbilities.TAIL_WHIP);
         helper.assertTrue(r5308 == CastResult.SUCCESS, "then the coil crush: " + r5308);
      });
      float[] health = new float[1];
      helper.runAfterDelay(22L, () -> health[0] = husk.getHealth());
      helper.runAfterDelay(46L, () -> {
         helper.assertTrue(TailWhipAbility.isHeld(husk), "the victim is held in the coil");
         SeiryuDragonEntity shell = DragonShells.get(player);
         helper.assertTrue(shell != null && shell.body().action() == 2, "the body is wrapped around it");
         helper.assertTrue(husk.getHealth() < health[0], "and squeezes it");
      });
      helper.runAfterDelay(86L, () -> {
         helper.assertFalse(TailWhipAbility.isHeld(husk), "and lets it go in the end");
         doneDragon(helper, player, husk);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 40,
      batch = "seiryu_dragon_roar"
   )
   public static void roarBlowsBackAndStuns(GameTestHelper helper) {
      FakePlayer player = dragon(helper, new BlockPos(2, 2, 1), true);
      Mob husk = dummy(helper, new BlockPos(2, 2, 4));
      float health = husk.getHealth();
      double y = husk.getY();
      double[] highest = new double[]{y};
      helper.onEachTick(() -> highest[0] = Math.max(highest[0], husk.getY()));
      CastResult r9130 = AbilityManager.press(player, SeiryuAbilities.ROAR);
      helper.assertTrue(r9130 == CastResult.SUCCESS, "the roar: " + r9130);
      helper.runAfterDelay(9L, () -> {
         helper.assertTrue(husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && husk.hasEffect(MobEffects.WEAKNESS), "stuns the mob in front");
         helper.assertTrue(husk.getHealth() < health, "hurts it");
         helper.assertTrue(highest[0] > y + 0.5, "and blows it away, rose " + (highest[0] - y));
         doneDragon(helper, player, husk);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 80,
      batch = "seiryu_dragon_storm"
   )
   public static void thunderStormStrikesEnemiesUnderTheCloud(GameTestHelper helper) {
      FakePlayer player = dragon(helper, new BlockPos(2, 2, 1), true);
      Mob husk = dummy(helper, new BlockPos(2, 2, 4));
      float health = husk.getHealth();
      CastResult r6 = AbilityManager.press(player, SeiryuAbilities.THUNDER_STORM);
      helper.assertTrue(r6 == CastResult.SUCCESS, "the storm is called: " + r6);
      helper.runAfterDelay(30L, () -> {
         helper.assertTrue(husk.getHealth() < health, "lightning from the cloud strikes the mob under it");
         helper.assertTrue(UoUoSeiryu.isIn(player, "dragon"), "the dragon itself is never struck down");
         doneDragon(helper, player, husk);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60,
      batch = "seiryu_dragon_twister"
   )
   public static void dragonTwisterPullsInAndLifts(GameTestHelper helper) {
      FakePlayer player = dragon(helper, new BlockPos(2, 2, 2), false);
      player.setPos(player.getX(), player.getY() + 5.0, player.getZ());
      Mob husk = dummy(helper, new BlockPos(0, 2, 2));
      float health = husk.getHealth();
      double y = husk.getY();
      helper.onEachTick(() -> CoreEvents.tickServer(player, DFAttachments.data(player)));
      CastResult r7076 = AbilityManager.press(player, SeiryuAbilities.DRAGON_TWISTER);
      helper.assertTrue(r7076 == CastResult.CHANNELLING, "the twister spins up: " + r7076);
      helper.assertTrue(FormFlags.has(player, DragonShells.TWISTING), "the body winds into its spiral");
      helper.runAfterDelay(14L, () -> {
         helper.assertTrue(husk.getY() > y + 0.3, "the vortex lifts the mob, y " + husk.getY() + " from " + y);
         helper.assertTrue(husk.getHealth() < health, "and batters it");
         SeiryuDragonEntity shell = DragonShells.get(player);
         helper.assertTrue(shell != null && shell.body().twist(1.0F) > 0.5F, "the body spins with it");
         AbilityManager.release(player, SeiryuAbilities.DRAGON_TWISTER);
         helper.assertFalse(FormFlags.has(player, DragonShells.TWISTING), "letting go ends the spin");
      });
      helper.runAfterDelay(18L, () -> doneDragon(helper, player, husk));
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 160,
      batch = "seiryu_dragon_meteors"
   )
   public static void meteorBarrageRainsOnTheAimedArea(GameTestHelper helper) {
      FakePlayer player = dragon(helper, new BlockPos(2, 2, 1), true);
      player.setXRot(35.0F);
      Mob husk = dummy(helper, new BlockPos(2, 2, 3));
      float health = husk.getHealth();
      player.getRandom().setSeed(20261009L);
      CastResult charging = AbilityManager.press(player, SeiryuAbilities.BOLO_BREATH);
      helper.assertTrue(charging == CastResult.CHARGING, "the barrage charges: " + charging);
      BooleanValue griefing = (BooleanValue)helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
      boolean before = griefing.get();
      griefing.set(false, helper.getLevel().getServer());
      Runnable restore = () -> griefing.set(before, helper.getLevel().getServer());
      helper.onEachTick(() -> CoreEvents.tickServer(player, DFAttachments.data(player)));
      helper.runAfterDelay(24L, () -> {
         CastResult fired = AbilityManager.release(player, SeiryuAbilities.BOLO_BREATH);
         if (fired != CastResult.SUCCESS) {
            restore.run();
         }

         helper.assertTrue(fired == CastResult.SUCCESS, "and is spat into the sky on release: " + fired);
      });
      helper.runAfterDelay(36L, () -> {
         if (husk.getHealth() != health) {
            restore.run();
         }

         helper.assertTrue(husk.getHealth() == health, "the meteors take a moment to fall");
      });
      helper.runAfterDelay(
         120L,
         () -> {
            restore.run();
            helper.assertTrue(
               husk.getHealth() < health && husk.isOnFire(),
               "then they rain down on the mob in the aimed area (health "
                  + husk.getHealth()
                  + " of "
                  + health
                  + ", burning "
                  + husk.isOnFire()
                  + ", at "
                  + husk.blockPosition()
                  + ")"
            );
            doneDragon(helper, player, husk);
         }
      );
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60,
      batch = "seiryu_dragon_time"
   )
   public static void theDragonIsLimitedByTimeNotByTechniques(GameTestHelper helper) {
      FakePlayer player = dragon(helper, new BlockPos(2, 2, 2), true);
      PlayerFruitData data = DFAttachments.data(player);
      int max = Mastery.maxEnergy(data);
      data.setEnergy((float)max);
      CastResult roar = AbilityManager.press(player, SeiryuAbilities.ROAR);
      helper.assertTrue(roar == CastResult.SUCCESS, "a technique casts: " + roar);
      helper.assertTrue(data.energy() == (float)max, "for free: " + data.energy() + " of " + max);
      helper.onEachTick(() -> CoreEvents.tickServer(player, DFAttachments.data(player)));
      helper.runAfterDelay(20L, () -> {
         float spent = (float)max - data.energy();
         helper.assertTrue(Math.abs(spent - (float)max / 60.0F) < (float)max / 60.0F * 0.15F, "the form spends a sixtieth of the bar a second: " + spent);
         player.setGameMode(GameType.CREATIVE);
         float before = data.energy();
         helper.runAfterDelay(20L, () -> {
            float creative = before - data.energy();
            helper.assertTrue(Math.abs(creative - (float)max / 60.0F) < (float)max / 60.0F * 0.15F, "the clock runs in creative too: " + creative);
            player.setGameMode(GameType.SURVIVAL);
            doneDragon(helper, player);
         });
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 30,
      batch = "seiryu_dragon_forwarding"
   )
   public static void areaHitsOnTheBodyReachTheDragon(GameTestHelper helper) {
      FakePlayer player = dragon(helper, new BlockPos(2, 2, 2), true);
      FakePlayer other = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "seiryu_rival"));
      helper.runAfterDelay(
         3L,
         () -> {
            SeiryuDragonEntity shell = DragonShells.get(player);
            helper.assertTrue(shell != null, "the dragon has a body");
            Entity far = null;

            for (PartEntity<?> part : shell.getParts()) {
               if (((SeiryuBodyPart)part).active() && (far == null || part.distanceToSqr(player) > far.distanceToSqr(player))) {
                  far = part;
               }
            }

            Vec3 at = far.getBoundingBox().getCenter();
            helper.assertTrue(at.distanceTo(player.getBoundingBox().getCenter()) > 3.0, "a part away from the head");
            List<Entity> hits = Targeting.inRadius(helper.getLevel(), at, 1.5, e -> e != other);
            helper.assertTrue(hits.contains(player), "a blast at the body reaches the dragon itself");
            helper.assertTrue(hits.stream().noneMatch(e -> e instanceof PartEntity || e instanceof SeiryuDragonEntity), "never its parts");
            helper.assertFalse(
               Targeting.inRadius(helper.getLevel(), at, 1.5, e -> Targeting.canHurt(player, e)).contains(player),
               "the dragon's own blasts never reach it through its body"
            );
            doneDragon(helper, player);
         }
      );
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60,
      batch = "seiryu_dragon_drain"
   )
   public static void theDragonRevertsWhenItsEnergyRunsOut(GameTestHelper helper) {
      FakePlayer player = dragon(helper, new BlockPos(2, 2, 2), false);
      player.setPos(player.getX(), player.getY() + 4.0, player.getZ());
      PlayerFruitData data = DFAttachments.data(player);
      data.setEnergy(1.5F);
      helper.onEachTick(() -> {
         if (data.fruitId() != null) {
            CoreEvents.tickServer(player, data);
         }
      });
      helper.runAfterDelay(20L, () -> {
         helper.assertFalse(UoUoSeiryu.isIn(player, "dragon"), "the dragon form drains its energy and ends with it");
         helper.assertTrue(SafeLanding.isActive(player), "the fall from the sky is softened");
      });
      helper.runAfterDelay(24L, () -> {
         helper.assertTrue(DragonShells.get(player) == null, "and the body is gone");
         SafeLanding.forget(player.getUUID());
         doneDragon(helper, player);
      });
   }
}
