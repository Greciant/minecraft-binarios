package com.devilfruits.fruits.suke_suke.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.fruits.suke_suke.SukeAbilities;
import com.devilfruits.fruits.suke_suke.SukeFx;
import com.devilfruits.fruits.suke_suke.SukeItems;
import com.devilfruits.fruits.suke_suke.SukeSuke;
import com.devilfruits.fruits.suke_suke.clear.Clear;
import com.devilfruits.fruits.suke_suke.clear.Cloak;
import com.devilfruits.fruits.suke_suke.clear.ShipCloak;
import com.devilfruits.fruits.suke_suke.client.Visibility;
import com.devilfruits.mobs.pirate.Pirates;
import com.devilfruits.mobs.pirate.entity.PirateDeckhand;
import com.devilfruits.mobs.pirate.entity.PirateFighter;
import com.devilfruits.registry.DFAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public final class SukeCapture extends CaptureStage {
   private static final int RATE = 8;
   private static final int CLIP_RATE = 5;
   private static final String INVISIBILITY = "devilfruits:suke_suke/invisibility";
   private static final String HIDDEN_WEAPON = "devilfruits:suke_suke/hidden_weapon";
   private final SukeCapture.Stage stage;
   private static final String SHIP = "devilfruits:suke_suke/ship_invisibility";
   private static int cannonDeck;

   public SukeCapture(String prefix, SukeCapture.Stage stage) {
      super(prefix);
      this.stage = stage;
   }

   @Override
   protected ResourceLocation fruit() {
      return SukeSuke.ID;
   }

   @Override
   protected void setup(ServerPlayer p) {
      if (this.stage == SukeCapture.Stage.M4) {
         buildShip(p);
      } else if (this.stage == SukeCapture.Stage.CANNON) {
         buildCannonShip(p);
      }

      equip(p);
   }

   private static void equip(ServerPlayer p) {
      command(p, "item replace entity @s armor.head with minecraft:iron_helmet");
      command(p, "item replace entity @s armor.chest with minecraft:iron_chestplate");
      command(p, "item replace entity @s armor.legs with minecraft:iron_leggings");
      command(p, "item replace entity @s armor.feet with minecraft:iron_boots");
      command(p, "item replace entity @s weapon.mainhand with minecraft:diamond_sword");
      command(p, "item replace entity @s weapon.offhand with minecraft:shield");
   }

   @Override
   protected void onReset(ServerPlayer p) {
      command(p, "time set 6000");
      command(p, "gamerule doMobSpawning false");
      PlayerFruitData data = DFAttachments.data(p);
      AbilityManager.stopToggle(p, SukeAbilities.INVISIBILITY);
      AbilityManager.stopToggle(p, SukeAbilities.GROUP_INVISIBILITY);
      AbilityManager.stopToggle(p, SukeAbilities.HIDDEN_WEAPON);
      AbilityManager.stopToggle(p, SukeAbilities.SHIP_INVISIBILITY);
      Cloak.end(p);
      p.removeData(Clear.STATE);
      ShipCloak.stopAll(p.getServer());
      p.removeAllEffects();
      p.clearFire();
      p.stopUsingItem();
      data.clearCooldowns();
      data.setEnergy(1000.0F);
      data.setMastery(10);
      equip(p);
   }

   private static Consumer<Minecraft> as(Visibility.Viewer viewer, boolean hitboxes, Consumer<Minecraft> camera) {
      return mc -> {
         Visibility.viewer(viewer);
         mc.getEntityRenderDispatcher().setRenderHitBoxes(hitboxes);
         camera.accept(mc);
      };
   }

   @Override
   protected int takes(AutoTest s, int t) {
      return switch (this.stage) {
         case M2 -> this.m2(s, t);
         case M3 -> this.m3(s, t);
         case M4 -> this.m4(s, t);
         case M5 -> this.m5(s, t);
         case CANNON -> this.cannon(s, t);
      };
   }

   private int m2(AutoTest s, int t) {
      t = this.take(s, t, 8, none(), 24, none(), as(Visibility.Viewer.REAL, true, rig(4.2, 150.0F, 10.0F, 0.9)), 12, c -> this.shot(s, c + 6, "baseline"));
      t = this.take(
         s,
         t,
         8,
         none(),
         4,
         p -> castAbility(p, "devilfruits:suke_suke/invisibility", 1),
         as(Visibility.Viewer.SELF, true, rig(4.2, 150.0F, 10.0F, 0.9)),
         40,
         c -> this.shot(s, c + 30, "self")
      );
      t = this.take(
         s,
         t,
         8,
         none(),
         4,
         p -> castAbility(p, "devilfruits:suke_suke/invisibility", 1),
         as(Visibility.Viewer.ALLY, true, rig(4.2, 150.0F, 10.0F, 0.9)),
         40,
         c -> this.shot(s, c + 30, "ally")
      );
      t = this.take(s, t, 8, p -> p.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400)), 4, p -> {
         castAbility(p, "devilfruits:suke_suke/invisibility", 1);
         p.igniteForSeconds(8.0F);
      }, as(Visibility.Viewer.STRANGER, true, rig(4.2, 150.0F, 10.0F, 0.9)), 40, c -> this.shot(s, c + 30, "stranger"));
      Consumer<ServerPlayer> bow = p -> {
         command(p, "item replace entity @s weapon.mainhand with minecraft:bow");
         command(p, "give @s minecraft:arrow 4");
         castAbility(p, "devilfruits:suke_suke/hidden_weapon", 1);
      };
      Consumer<ServerPlayer> draw = p -> p.startUsingItem(InteractionHand.MAIN_HAND);
      t = this.take(s, t, 8, bow, 6, draw, as(Visibility.Viewer.REAL, false, rig(3.8, 125.0F, 6.0F, 0.9)), 30, c -> this.shot(s, c + 16, "weapon_owner"));
      t = this.take(s, t, 8, bow, 6, draw, as(Visibility.Viewer.STRANGER, false, rig(3.8, 125.0F, 6.0F, 0.9)), 30, c -> this.shot(s, c + 16, "weapon_stranger"));
      t = this.take(
         s,
         t,
         8,
         none(),
         4,
         p -> castAbility(p, "devilfruits:suke_suke/invisibility", 1),
         as(Visibility.Viewer.REAL, false, firstPerson()),
         40,
         c -> this.shot(s, c + 30, "first_person")
      );
      s.add(t, mc -> {
         Visibility.viewer(Visibility.Viewer.REAL);
         mc.getEntityRenderDispatcher().setRenderHitBoxes(false);
      });
      return t + 4;
   }

   private static List<PirateFighter> crew(ServerPlayer p) {
      List<PirateFighter> out = new ArrayList<>();

      for (double dx : new double[]{-2.2, 2.4}) {
         PirateDeckhand mate = (PirateDeckhand)Pirates.DECKHAND.get().create(p.serverLevel());
         if (mate != null) {
            mate.moveTo(p.getX() + dx, p.getY(), p.getZ() - 0.8, 180.0F, 0.0F);
            mate.setYBodyRot(180.0F);
            mate.setYHeadRot(180.0F);
            mate.setNoAi(true);
            p.serverLevel().addFreshEntity(mate);
            out.add(mate);
         }
      }

      return out;
   }

   private static void groupCast(ServerPlayer p) {
      castAbility(p, "devilfruits:suke_suke/invisibility", 1);
      ServerFx.send(p, SukeFx.GROUP_CAST, p.position(), Vec3.ZERO, (float)SukeSuke.setting("groupRadius"), 0L, 0);

      for (PirateFighter mate : p.serverLevel().getEntitiesOfClass(PirateFighter.class, p.getBoundingBox().inflate(4.0))) {
         Clear.vanish(mate, p.getUUID(), false);
         ServerFx.send(mate, SukeFx.VANISH, p.position(), Vec3.ZERO, 0.0F, 0L, 0);
      }
   }

   private int m3(AutoTest s, int t) {
      int[] beats = new int[]{1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 26};
      t = this.take(
         s,
         t,
         8,
         none(),
         24,
         p -> castAbility(p, "devilfruits:suke_suke/invisibility", 1),
         as(Visibility.Viewer.STRANGER, false, rig(3.6, 150.0F, 8.0F, 0.9)),
         34,
         c -> {
            for (int k : beats) {
               this.shot(s, c + k, "vanish_" + k);
            }
         }
      );
      t = this.take(
         s,
         t,
         8,
         none(),
         4,
         p -> castAbility(p, "devilfruits:suke_suke/invisibility", 1),
         as(Visibility.Viewer.SELF, false, rig(3.6, 150.0F, 8.0F, 0.9)),
         34,
         c -> {
            for (int k : new int[]{6, 12, 18, 30}) {
               this.shot(s, c + k, "self_" + k);
            }
         }
      );
      t = this.take(
         s,
         t,
         8,
         p -> castAbility(p, "devilfruits:suke_suke/invisibility", 1),
         36,
         p -> castAbility(p, "devilfruits:suke_suke/invisibility", 1),
         as(Visibility.Viewer.STRANGER, false, rig(3.6, 150.0F, 8.0F, 0.9)),
         34,
         c -> {
            for (int k : new int[]{1, 4, 7, 10, 13, 16, 19, 22, 26}) {
               this.shot(s, c + k, "appear_" + k);
            }
         }
      );
      t = this.take(s, t, 8, p -> crew(p), 8, SukeCapture::groupCast, as(Visibility.Viewer.STRANGER, false, rig(7.0, 155.0F, 22.0F, 0.6)), 44, c -> {
         for (int k : new int[]{2, 5, 8, 11, 14, 18, 22, 28, 38}) {
            this.shot(s, c + k, "group_" + k);
         }
      });
      t = this.take(
         s,
         t,
         8,
         p -> command(p, "time set 18000"),
         24,
         p -> castAbility(p, "devilfruits:suke_suke/invisibility", 1),
         as(Visibility.Viewer.STRANGER, false, rig(3.6, 150.0F, 8.0F, 0.9)),
         30,
         c -> {
            for (int k : new int[]{3, 7, 11, 15, 19}) {
               this.shot(s, c + k, "night_" + k);
            }
         }
      );
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
      t = this.take(
         s,
         t + 2,
         8,
         none(),
         4,
         p -> castAbility(p, "devilfruits:suke_suke/invisibility", 1),
         as(Visibility.Viewer.STRANGER, false, rig(3.6, 150.0F, 8.0F, 0.9)),
         30,
         c -> {
            for (int k : new int[]{6, 12, 18}) {
               this.shot(s, c + k, "low_" + k);
            }
         }
      );
      s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
      t = this.take(
         s,
         t + 2,
         5,
         none(),
         4,
         p -> castAbility(p, "devilfruits:suke_suke/invisibility", 1),
         as(Visibility.Viewer.STRANGER, false, rig(3.8, 145.0F, 8.0F, 0.9)),
         70,
         c -> {
            s.add(c - 1, mc -> VfxCapture.clip(this.prefix + "clip", 150, 0.6666666666666666));
            s.add(c + 40, mc -> s.server(mc, p -> castAbility(p, "devilfruits:suke_suke/invisibility", 1)));
         }
      );
      s.add(t, mc -> Visibility.viewer(Visibility.Viewer.REAL));
      return t + 4;
   }

   private static void buildShip(ServerPlayer p) {
      if (!PhysicsWorld.active()) {
         DevilFruits.LOGGER.warn("suke capture: Sable not installed, no ship");
      } else {
         ServerLevel level = p.serverLevel();
         int y = (int)Math.floor(p.getY());
         BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
         BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
         BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
         BlockState sail = Blocks.WHITE_WOOL.defaultBlockState();
         Function<String, BlockState> block = id -> ((Block)BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id))).defaultBlockState();

         for (int x = -4; x <= 4; x++) {
            for (int z = -9; z <= -3; z++) {
               level.setBlockAndUpdate(new BlockPos(x, y, z), planks);
               boolean rim = x == -4 || x == 4 || z == -9 || z == -3;
               if (rim && (z != -3 || Math.abs(x) > 1)) {
                  level.setBlockAndUpdate(new BlockPos(x, y + 1, z), fence);
               }
            }
         }

         for (int dy = 1; dy <= 5; dy++) {
            level.setBlockAndUpdate(new BlockPos(0, y + dy, -7), log);
         }

         for (int x = -2; x <= 2; x++) {
            for (int dy = 3; dy <= 5; dy++) {
               if (x != 0) {
                  level.setBlockAndUpdate(new BlockPos(x, y + dy, -7), sail);
               }
            }
         }

         for (int zx = -8; zx <= -4; zx++) {
            level.setBlockAndUpdate(new BlockPos(-3, y + 1, zx), (BlockState)block.apply("create:shaft").setValue(BlockStateProperties.AXIS, Axis.Z));
         }

         level.setBlockAndUpdate(new BlockPos(2, y + 1, -8), block.apply("create:large_cogwheel"));
         level.setBlockAndUpdate(new BlockPos(2, y + 1, -5), block.apply("create:cogwheel"));
         level.setBlockAndUpdate(new BlockPos(0, y + 2, -9), block.apply("aeronautics:wooden_propeller"));
         command(p, String.format("sable assemble area %d %d %d %d %d %d", -4, y, -9, 4, y + 5, -3));
      }
   }

   private static void buildCannonShip(ServerPlayer p) {
      if (!PhysicsWorld.active()) {
         DevilFruits.LOGGER.warn("suke capture: Sable not installed, no ship");
      } else {
         int y = (int)Math.floor(p.getY());
         command(p, String.format("fill -3 %d -10 3 %d -3 minecraft:oak_planks", y, y));
         command(p, String.format("setblock 0 %d -6 createbigcannons:cannon_mount[facing=south,vertical_direction=down]", y + 1));
         command(p, String.format("setblock 0 %d -5 createbigcannons:cast_iron_cannon_end[facing=south]", y + 3));
         command(p, String.format("setblock 0 %d -6 createbigcannons:cast_iron_cannon_chamber[facing=south]", y + 3));
         command(p, String.format("setblock 0 %d -7 createbigcannons:cast_iron_cannon_barrel[facing=south]", y + 3));
         command(p, String.format("setblock 0 %d -8 createbigcannons:cast_iron_cannon_barrel[facing=south]", y + 3));
         command(p, String.format("setblock 0 %d -7 minecraft:redstone_block", y + 1));
         cannonDeck = y;
      }
   }

   private int cannon(AutoTest s, int t) {
      Consumer<ServerPlayer> deck = p -> p.teleportTo(p.serverLevel(), 2.5, p.getY() + 1.05, -4.5, 180.0F, 10.0F);
      s.add(t, mc -> s.server(mc, p -> command(p, String.format("sable assemble area %d %d %d %d %d %d", -3, cannonDeck, -10, 3, cannonDeck + 4, -3))));
      t += 20;
      Consumer<Minecraft> wide = as(Visibility.Viewer.STRANGER, false, rig(11.0, 145.0F, 22.0F, 1.2));
      t = this.take(s, t, 20, deck, 10, p -> {
      }, wide, 10, c -> this.shot(s, c + 4, "shown"));
      t = this.take(s, t, 20, deck, 10, hideFor(400), wide, 40, c -> {
         this.shot(s, c + 12, "stranger_early");
         this.shot(s, c + 30, "stranger");
         s.add(c + 32, as(Visibility.Viewer.REAL, false, rig(11.0, 145.0F, 22.0F, 1.2)));
         this.shot(s, c + 38, "caster");
      });
      s.add(t, mc -> Visibility.viewer(Visibility.Viewer.REAL));
      return t + 4;
   }

   private static Consumer<ServerPlayer> aboard() {
      return p -> p.teleportTo(p.serverLevel(), 0.5, p.getY() + 1.05, -4.5, 180.0F, 10.0F);
   }

   private static Consumer<ServerPlayer> hideFor(int ticks) {
      return p -> {
         ShipCloak.Ship ship = ShipCloak.target(p);
         if (ship != null) {
            ShipCloak.start(p, ship, ticks);
         } else {
            DevilFruits.LOGGER.warn("suke capture: no ship under the user");
         }
      };
   }

   private int m4(AutoTest s, int t) {
      Consumer<Minecraft> wide = as(Visibility.Viewer.STRANGER, false, rig(14.0, 145.0F, 22.0F, 1.2));
      t = this.take(s, t, 8, aboard(), 30, p -> castAbility(p, "devilfruits:suke_suke/ship_invisibility", 1), wide, 40, c -> {
         for (int k : new int[]{2, 5, 8, 11, 14, 17, 20, 24, 30, 38}) {
            this.shot(s, c + k, "ship_" + k);
         }
      });
      t = this.take(s, t, 8, aboard(), 10, hideFor(400), as(Visibility.Viewer.REAL, false, rig(12.0, 150.0F, 20.0F, 1.5)), 40, c -> {
         for (int k : new int[]{10, 30}) {
            this.shot(s, c + k, "aboard_" + k);
         }
      });
      t = this.take(s, t, 8, aboard(), 10, hideFor(90), wide, 92, c -> {
         for (int k : new int[]{34, 44, 52, 60, 66, 72, 78, 84, 88}) {
            this.shot(s, c + k, "warn_" + k);
         }
      });
      t = this.take(s, t, 8, aboard(), 10, hideFor(40), wide, 66, c -> {
         for (int k : new int[]{40, 43, 46, 50, 54, 60}) {
            this.shot(s, c + k, "return_" + k);
         }
      });
      t = this.take(s, t, 8, aboard(), 10, p -> castAbility(p, "devilfruits:suke_suke/ship_invisibility", 1), wide, 62, c -> {
         s.add(c + 40, mc -> s.server(mc, p -> castAbility(p, "devilfruits:suke_suke/ship_invisibility", 1)));

         for (int k : new int[]{30, 41, 44, 48, 54, 60}) {
            this.shot(s, c + k, "cancel_" + k);
         }
      });
      t = this.take(s, t, 5, aboard(), 10, hideFor(110), wide, 130, c -> s.add(c - 1, mc -> VfxCapture.clip(this.prefix + "clip", 260, 0.6666666666666666)));
      s.add(t, mc -> Visibility.viewer(Visibility.Viewer.REAL));
      return t + 4;
   }

   private static void fruitShowcase(ServerPlayer p) {
      ServerLevel level = p.serverLevel();
      ItemStack fruit = new ItemStack((ItemLike)SukeItems.FRUIT.get());
      p.setItemInHand(InteractionHand.MAIN_HAND, fruit.copy());
      p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      BlockPos wall = BlockPos.containing(p.getX() - 2.0, p.getY() + 1.0, p.getZ() - 2.0);
      level.setBlockAndUpdate(wall, Blocks.SPRUCE_PLANKS.defaultBlockState());
      ItemFrame frame = new ItemFrame(level, wall.south(), Direction.SOUTH);
      frame.setItem(fruit.copy());
      level.addFreshEntity(frame);
      ItemEntity dropped = new ItemEntity(level, p.getX() + 1.2, p.getY() + 0.1, p.getZ() - 1.4, fruit.copy());
      dropped.setNoPickUpDelay();
      dropped.setNeverPickUp();
      dropped.setUnlimitedLifetime();
      level.addFreshEntity(dropped);
   }

   private int m5(AutoTest s, int t) {
      t = this.take(
         s,
         t,
         8,
         SukeCapture::fruitShowcase,
         20,
         none(),
         as(Visibility.Viewer.REAL, false, rig(3.4, 200.0F, 14.0F, 0.9)),
         12,
         c -> this.shot(s, c + 6, "fruit_world")
      );
      t = this.take(s, t, 8, SukeCapture::fruitShowcase, 20, none(), mc -> {
         Visibility.viewer(Visibility.Viewer.REAL);
         firstPerson().accept(mc);
         mc.options.hideGui = false;
      }, 12, c -> this.shot(s, c + 6, "fruit_hand"));
      s.add(t, mc -> {
         if (mc.player != null) {
            mc.setScreen(new InventoryScreen(mc.player));
         }
      });
      s.add(t + 6, mc -> s.shot(mc, this.prefix + "fruit_inventory"));
      s.add(t + 8, mc -> {
         mc.setScreen(null);
         mc.options.hideGui = true;
      });
      t += 12;
      Consumer<Minecraft> front = as(Visibility.Viewer.REAL, false, rig(3.6, 160.0F, 6.0F, 0.9));
      t = this.take(s, t, 8, none(), 4, p -> castAbility(p, "devilfruits:suke_suke/invisibility", 1), front, 30, c -> {
         for (int k : new int[]{4, 8, 11, 13, 17, 24}) {
            this.shot(s, c + k, "clip_vanish_" + k);
         }
      });
      t = this.take(
         s,
         t,
         8,
         p -> castAbility(p, "devilfruits:suke_suke/invisibility", 1),
         36,
         p -> castAbility(p, "devilfruits:suke_suke/invisibility", 1),
         front,
         28,
         c -> {
            for (int k : new int[]{4, 8, 11, 14, 20}) {
               this.shot(s, c + k, "clip_reappear_" + k);
            }
         }
      );
      t = this.take(s, t, 8, p -> crew(p), 8, p -> {
         groupCast(p);
         ServerFx.animate(p, SukeSuke.id("group_cast"));
      }, front, 30, c -> {
         for (int k : new int[]{4, 9, 12, 14, 20}) {
            this.shot(s, c + k, "clip_group_" + k);
         }
      });
      t = this.take(s, t, 8, none(), 4, p -> castAbility(p, "devilfruits:suke_suke/invisibility", 1), mc -> {
         Visibility.viewer(Visibility.Viewer.REAL);
         firstPerson().accept(mc);
         mc.options.hideGui = false;
      }, 40, c -> {
         this.shot(s, c + 34, "hud");
         s.add(c + 36, mc -> mc.options.hideGui = true);
      });
      return t + 4;
   }

   public static enum Stage {
      M2,
      M3,
      M4,
      M5,
      CANNON;
   }
}
