package com.devilfruits.mobs.pirate;

import com.devilfruits.core.npc.Faction;
import com.devilfruits.core.npc.FactionMemberKilledEvent;
import com.devilfruits.core.npc.Locomotion;
import com.devilfruits.mobs.pirate.entity.PirateCaptain;
import com.devilfruits.mobs.pirate.entity.PirateDeckhand;
import com.devilfruits.mobs.pirate.entity.PirateGunner;
import com.devilfruits.mobs.pirate.goal.CaptainGoal;
import com.devilfruits.mobs.pirate.item.FlintlockItem;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class PirateGameTests {
   private static final String PLATFORM = "platform";
   private static final AtomicReference<FactionMemberKilledEvent> LAST_KILL = new AtomicReference<>();
   private static boolean listening;

   @GameTest(
      template = "platform",
      batch = "pirates"
   )
   public static void spawnAndSaveRoundTrip(GameTestHelper helper) {
      PirateCaptain captain = (PirateCaptain)helper.spawnWithNoFreeWill(Pirates.CAPTAIN.get(), new BlockPos(1, 2, 1));
      PirateGunner gunner = (PirateGunner)helper.spawnWithNoFreeWill(Pirates.GUNNER.get(), new BlockPos(3, 2, 3));
      gunner.setVariant(3);
      gunner.setFlag(8, true);
      gunner.setLeader(captain);
      CompoundTag tag = gunner.saveWithoutId(new CompoundTag());
      PirateGunner copy = (PirateGunner)Pirates.GUNNER.get().create(helper.getLevel());
      helper.assertTrue(copy != null, "the gunner type creates");
      copy.load(tag);
      helper.assertTrue(copy.variant() == 3 && copy.hasFlag(8), "variant and loaded state survive saving");
      helper.assertTrue(tag.hasUUID("Leader"), "the leader is saved");
      helper.assertTrue(captain.bounty() > 0 && gunner.bounty() == 0, "only the captain carries a bounty");
      helper.assertTrue(captain.getHakiLevel() == 0 && gunner.getHakiLevel() == 0, "pirates have no Haki yet");
      helper.assertTrue(gunner.getGroundSpeedRelativeToSupport().equals(Vec3.ZERO), "standing still has no ground speed");
      helper.assertTrue(captain.getScale() > 1.3F, "the captain is drawn and collides big");
      ItemStack musket = new ItemStack((ItemLike)Pirates.MUSKET.get());
      helper.assertTrue(!FlintlockItem.isLoaded(musket), "a new musket is empty");
      FlintlockItem.setLoaded(musket, true);
      helper.assertTrue(FlintlockItem.isLoaded(musket), "the loaded component sticks");
      helper.succeed();
   }

   @GameTest(
      template = "platform",
      batch = "pirates"
   )
   public static void factionsAndFriendlyFire(GameTestHelper helper) {
      PirateDeckhand deckhand = (PirateDeckhand)helper.spawnWithNoFreeWill(Pirates.DECKHAND.get(), new BlockPos(1, 2, 1));
      PirateGunner gunner = (PirateGunner)helper.spawnWithNoFreeWill(Pirates.GUNNER.get(), new BlockPos(3, 2, 1));
      Husk husk = (Husk)helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 3));
      helper.assertTrue(Faction.PIRATE.hostileTo(Faction.MARINE) && !Faction.PIRATE.hostileTo(Faction.PIRATE), "pirates fight marines, not pirates");
      helper.assertTrue(deckhand.isAlliedTo(gunner) && !deckhand.canHarm(gunner), "crewmates are allies (friendly fire off)");
      helper.assertTrue(deckhand.canHarm(husk), "anyone else can be harmed");
      helper.succeed();
   }

   @GameTest(
      template = "platform",
      batch = "pirates"
   )
   public static void gaitFollowsSpeedWithHysteresis(GameTestHelper helper) {
      Locomotion.Speeds s = new Locomotion.Speeds(2.2F, 4.6F);
      helper.assertTrue(Locomotion.state(0.0F, Locomotion.Gait.IDLE, s).gait() == Locomotion.Gait.IDLE, "still is idle");
      Locomotion.State walk = Locomotion.state(2.2F, Locomotion.Gait.IDLE, s);
      helper.assertTrue(walk.gait() == Locomotion.Gait.WALK && Math.abs(walk.rate() - 1.0F) < 0.01F, "the authored walk speed plays at rate 1");
      Locomotion.State run = Locomotion.state(4.6F, Locomotion.Gait.WALK, s);
      helper.assertTrue(run.gait() == Locomotion.Gait.RUN && Math.abs(run.rate() - 1.0F) < 0.01F, "the authored run speed plays at rate 1");
      float mid = (float)Math.sqrt(10.12F);
      helper.assertTrue(
         Locomotion.state(mid, Locomotion.Gait.WALK, s).gait() == Locomotion.Gait.WALK
            && Locomotion.state(mid, Locomotion.Gait.RUN, s).gait() == Locomotion.Gait.RUN,
         "the walk / run boundary keeps the current gait"
      );
      float creep = 0.308F;
      helper.assertTrue(
         Locomotion.state(creep, Locomotion.Gait.IDLE, s).gait() == Locomotion.Gait.IDLE
            && Locomotion.state(creep, Locomotion.Gait.WALK, s).gait() == Locomotion.Gait.WALK,
         "the idle / walk boundary keeps the current gait"
      );
      helper.succeed();
   }

   @GameTest(
      template = "platform",
      batch = "pirates"
   )
   public static void clipFramesFitTheirClips(GameTestHelper helper) {
      int n = PirateClips.NAMES.length;
      helper.assertTrue(PirateClips.LENGTH.length == n && PirateClips.HIT.length == n && PirateClips.MOVE.length == n, "one entry per action");

      for (int i = 0; i < n; i++) {
         helper.assertTrue(
            PirateClips.LENGTH[i] > 0 && PirateClips.HIT[i] < PirateClips.LENGTH[i] && PirateClips.MOVE[i] < PirateClips.LENGTH[i],
            "frames of " + PirateClips.NAMES[i] + " fall inside the clip"
         );
         helper.assertTrue(PirateClips.byName(PirateClips.NAMES[i]) == i, "name lookup of " + PirateClips.NAMES[i]);
      }

      helper.assertTrue(PirateClips.MOVE[3] < PirateClips.HIT[3] && PirateClips.MOVE[12] < PirateClips.HIT[12], "dashes start before their blows land");

      for (int id : new int[]{6, 8}) {
         int[] drill = new int[]{
            PirateClips.LEFT_FROM[id],
            PirateClips.HALF_COCK[id],
            PirateClips.BITE[id],
            PirateClips.PRIME[id],
            PirateClips.PAN_SHUT[id],
            PirateClips.POUR[id],
            PirateClips.ROD_OUT[id],
            PirateClips.RAM_1[id],
            PirateClips.RAM_2[id],
            PirateClips.ROD_IN[id],
            PirateClips.LEFT_TO[id],
            PirateClips.HIT[id]
         };

         for (int i = 1; i < drill.length; i++) {
            helper.assertTrue(drill[i - 1] >= 0 && drill[i - 1] < drill[i], "the loading drill of " + PirateClips.NAMES[id] + " runs in order");
         }

         helper.assertTrue(
            PirateClips.reloadState(id, 0.0F) == 0.0F
               && PirateClips.reloadState(id, (float)PirateClips.HALF_COCK[id]) == 0.25F
               && PirateClips.reloadState(id, (float)PirateClips.POUR[id]) == 0.5F
               && PirateClips.reloadState(id, (float)PirateClips.ROD_OUT[id]) == 0.75F
               && PirateClips.reloadState(id, (float)PirateClips.HIT[id]) == 0.0F,
            "the gun model follows the drill"
         );
      }

      helper.succeed();
   }

   @GameTest(
      template = "platform",
      batch = "pirates",
      timeoutTicks = 140
   )
   public static void gunnerReloadsThenFiresEmpty(GameTestHelper helper) {
      PirateGunner gunner = (PirateGunner)helper.spawnWithNoFreeWill(Pirates.GUNNER.get(), new BlockPos(2, 2, 2));
      int reload = 6;
      int fire = 5;
      gunner.setFlag(8, false);
      gunner.playAction(reload, PirateClips.LENGTH[reload]);
      helper.startSequence()
         .thenExecuteAfter(PirateClips.HIT[reload] / 2, () -> helper.assertTrue(!gunner.hasFlag(8), "not loaded mid-reload"))
         .thenExecuteAfter(PirateClips.LENGTH[reload] - PirateClips.HIT[reload] / 2 + 2, () -> {
            helper.assertTrue(gunner.hasFlag(8) && !gunner.isActing(), "loaded once the reload clip ends");
            gunner.playAction(fire, PirateClips.LENGTH[fire]);
         })
         .thenExecuteAfter(PirateClips.LENGTH[fire] + 2, () -> helper.assertTrue(!gunner.hasFlag(8), "a shot empties the gun"))
         .thenSucceed();
   }

   @GameTest(
      template = "platform",
      batch = "pirates",
      timeoutTicks = 260
   )
   public static void captainSlamWaitsForItsCooldown(GameTestHelper helper) {
      PirateCaptain captain = (PirateCaptain)helper.spawnWithNoFreeWill(Pirates.CAPTAIN.get(), new BlockPos(2, 2, 1));
      Husk husk = (Husk)helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 3));
      husk.setHealth(husk.getMaxHealth());
      husk.setInvulnerable(true);
      captain.setTarget(husk);
      CaptainGoal goal = new CaptainGoal(captain);
      goal.start();
      helper.assertTrue(goal.cooldowns()[0] > 0.0F, "no slam the moment the fight starts");
      AtomicInteger slams = new AtomicInteger();
      AtomicInteger seq = new AtomicInteger(captain.actionSeq());
      helper.onEachTick(() -> {
         goal.tick();
         if (captain.actionSeq() != seq.get()) {
            seq.set(captain.actionSeq());
            if (captain.currentAction() == 11) {
               slams.incrementAndGet();
            }
         }
      });
      helper.startSequence()
         .thenExecuteAfter(30, () -> helper.assertTrue(slams.get() == 0, "the opening delay holds the slam back"))
         .thenExecuteAfter(120, () -> helper.assertTrue(slams.get() == 1, "one slam, then the cooldown: " + slams.get()))
         .thenExecuteAfter(80, () -> helper.assertTrue(slams.get() == 2, "the next slam once the cooldown (and a cutlass swing) is over: " + slams.get()))
         .thenSucceed();
   }

   @GameTest(
      template = "platform",
      batch = "pirates"
   )
   public static void lootTablesAndDeathEvent(GameTestHelper helper) {
      for (EntityType<?> type : new EntityType[]{Pirates.DECKHAND.get(), Pirates.GUNNER.get(), Pirates.CAPTAIN.get()}) {
         LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(type.getDefaultLootTable());
         helper.assertTrue(table != LootTable.EMPTY, "loot table of " + EntityType.getKey(type));
      }

      if (!listening) {
         listening = true;
         NeoForge.EVENT_BUS.addListener(FactionMemberKilledEvent.class, LAST_KILL::set);
      }

      LAST_KILL.set(null);
      PirateDeckhand deckhand = (PirateDeckhand)helper.spawnWithNoFreeWill(Pirates.DECKHAND.get(), new BlockPos(2, 2, 2));
      deckhand.hurt(helper.getLevel().damageSources().generic(), 1000.0F);
      FactionMemberKilledEvent event = LAST_KILL.get();
      helper.assertTrue(event != null && event.victim() == deckhand, "the death of a faction member is posted");
      helper.assertTrue(event.faction() == Faction.PIRATE && event.rank() == 0 && event.killer() == null, "with its faction, rank and killer");
      helper.succeed();
   }

   private PirateGameTests() {
   }
}
