package com.devilfruits.fruits.kage_kage.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureRig;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.fruits.kage_kage.KageAbilities;
import com.devilfruits.fruits.kage_kage.KageEntities;
import com.devilfruits.fruits.kage_kage.KageItems;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.ability.DoppelmanAbility;
import com.devilfruits.fruits.kage_kage.client.vfx.SwallowedView;
import com.devilfruits.fruits.kage_kage.entity.HollowCorpse;
import com.devilfruits.fruits.kage_kage.entity.ShadowServant;
import com.devilfruits.fruits.kage_kage.item.MoriaScissorsItem;
import com.devilfruits.fruits.kage_kage.shadow.ShadowVictims;
import com.devilfruits.fruits.kage_kage.shadow.Shadows;
import com.devilfruits.fruits.kage_kage.shadow.StolenShadow;
import com.devilfruits.mobs.pirate.entity.PirateFighter;
import com.devilfruits.registry.DFAttachments;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

public final class KageCapture extends CaptureStage {
   private static final int RATE = 8;
   private static final int CLIP_RATE = 5;
   private static final String BRICK_BAT = "devilfruits:kage_kage/brick_bat";
   private static final String SPEAR = "devilfruits:kage_kage/shadow_spear";
   private static final String TRAP = "devilfruits:kage_kage/shadow_trap";
   private static final String HAND = "devilfruits:kage_kage/shadow_hand";
   private static final String DOPPELMAN = "devilfruits:kage_kage/doppelman";
   private static final String COLLECTION = "devilfruits:kage_kage/shadow_collection";
   private final String stage;

   public KageCapture(String prefix) {
      this(prefix, "m3");
   }

   public KageCapture(String prefix, String stage) {
      super(prefix);
      this.stage = stage;
   }

   @Override
   protected ResourceLocation fruit() {
      return KageKage.ID;
   }

   @Override
   protected void onReset(ServerPlayer p) {
      PlayerFruitData data = DFAttachments.data(p);
      data.setToggle(KageAbilities.BRICK_BAT.id(), false);
      FormFlags.set(p, KageKage.AEGIS, false);
      p.serverLevel().getEntitiesOfClass(ShadowServant.class, p.getBoundingBox().inflate(64.0)).forEach(Entity::discard);
      p.serverLevel().getEntitiesOfClass(HollowCorpse.class, p.getBoundingBox().inflate(64.0)).forEach(Entity::discard);
      Shadows.releaseAll(p, Shadows.Release.COMMAND);
      data.clearCooldowns();
      data.setEnergy(10000.0F);
      p.serverLevel().getEntitiesOfClass(Villager.class, p.getBoundingBox().inflate(64.0)).forEach(Entity::discard);
      p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      ServerFx.stopAnimation(p);

      for (String type : new String[]{"arrow", "small_fireball", "zombie", "husk", "skeleton", "spider", "creeper"}) {
         command(p, "kill @e[type=minecraft:" + type + "]");
      }
   }

   @Override
   protected void setup(ServerPlayer p) {
      command(p, "gamerule doMobSpawning false");
      command(p, "time set 6000");
   }

   private static Consumer<ServerPlayer> night(Consumer<ServerPlayer> then) {
      return p -> {
         command(p, "time set 15000");
         then.accept(p);
      };
   }

   private static Consumer<ServerPlayer> day(Consumer<ServerPlayer> then) {
      return p -> {
         command(p, "time set 6000");
         then.accept(p);
      };
   }

   private static Consumer<ServerPlayer> aegisUp() {
      return p -> castAbility(p, "devilfruits:kage_kage/brick_bat", 1);
   }

   private static void arrow(ServerPlayer p, double ahead, double side) {
      ServerLevel level = p.serverLevel();
      Vec3 from = p.position().add(side, 1.4, -ahead);
      Arrow a = new Arrow(level, from.x, from.y, from.z, new ItemStack(Items.ARROW), null);
      Vec3 to = p.getBoundingBox().getCenter().subtract(from);
      a.shoot(to.x, to.y, to.z, 2.2F, 0.0F);
      level.addFreshEntity(a);
   }

   private static void fireball(ServerPlayer p) {
      ServerLevel level = p.serverLevel();
      Vec3 from = p.position().add(-1.5, 1.2, -6.0);
      Vec3 to = p.getBoundingBox().getCenter().subtract(from).normalize();
      level.addFreshEntity(new SmallFireball(level, from.x, from.y, from.z, to.scale(0.6)));
   }

   private static Consumer<Minecraft> onDummy(double dist, float yaw, float pitch, double lift) {
      return mc -> {
         mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
         if (mc.level != null && mc.player != null) {
            mc.level
               .getEntitiesOfClass(PirateFighter.class, mc.player.getBoundingBox().inflate(24.0))
               .stream()
               .findFirst()
               .ifPresent(d -> CaptureRig.follow(d, dist, yaw, pitch, lift));
         }
      };
   }

   private static void scissors(ServerPlayer p, boolean split) {
      p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)KageItems.MORIA_SCISSORS.get()));
      p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      if (split) {
         MoriaScissorsItem.split(p);
      }
   }

   private static void victim(ServerPlayer p) {
      Villager v = (Villager)EntityType.VILLAGER.create(p.serverLevel());
      if (v != null) {
         v.moveTo(p.getX(), p.getY(), p.getZ() - 3.2, 0.0F, 0.0F);
         v.setNoAi(true);
         p.serverLevel().addFreshEntity(v);
      }
   }

   private static void corpseAhead(ServerPlayer p) {
      UUID victim = UUID.randomUUID();
      ShadowVictims.get(p.server).put(victim, p.getUUID());
      p.setData(Shadows.HELD, new Shadows.Held(List.of(new StolenShadow(victim, DevilFruits.id("pirate/deckhand"), 24.0F, 4.0F, "Deckhand"))));
      HollowCorpse c = (HollowCorpse)KageEntities.HOLLOW_CORPSE.get().create(p.serverLevel());
      if (c != null) {
         c.moveTo(p.getX(), p.getY(), p.getZ() - 3.0, 0.0F, 0.0F);
         p.serverLevel().addFreshEntity(c);
      }
   }

   private static void inject(ServerPlayer p) {
      p.serverLevel().getEntitiesOfClass(HollowCorpse.class, p.getBoundingBox().inflate(6.0)).forEach(c -> c.inject(p));
   }

   private int m6(AutoTest s, int t, boolean zombiesOnly) {
      if (!zombiesOnly) {
         t = this.take(s, t, 8, day(p -> scissors(p, false)), 6, none(), rig(3.2, 330.0F, 6.0F, 0.2), 14, c -> {
            this.stills(s, c, "scissors_joined", 2);
            s.add(c + 4, firstPerson());
            this.stills(s, c, "scissors_joined_fp", 8);
         });
         t = this.take(s, t, 8, day(p -> scissors(p, true)), 6, none(), rig(3.2, 330.0F, 6.0F, 0.2), 30, c -> {
            this.stills(s, c, "scissors_split", 2);
            s.add(c + 4, rig(3.2, 250.0F, 4.0F, 0.2));
            this.stills(s, c, "scissors_split_side", 8);
            s.add(c + 12, mc -> {
               firstPerson().accept(mc);
               mc.options.hideGui = false;
            });
            this.stills(s, c, "scissors_split_fp", 18);
            s.add(c + 20, mc -> mc.options.hideGui = true);
         });
         t = this.take(
            s,
            t,
            8,
            day(p -> {
               scissors(p, false);
               victim(p);
            }),
            10,
            p -> castAbility(p, "devilfruits:kage_kage/shadow_collection", 46),
            rig(5.5, 245.0F, 10.0F, 0.4),
            80,
            c -> this.stills(s, c, "collect", 4, 12, 24, 40, 42, 44, 46, 48, 50, 53, 56, 60, 64, 68, 72)
         );
         t = this.take(
            s,
            t,
            5,
            day(p -> {
               scissors(p, false);
               victim(p);
            }),
            10,
            p -> castAbility(p, "devilfruits:kage_kage/shadow_collection", 46),
            rig(5.5, 245.0F, 10.0F, 0.4),
            80,
            c -> s.add(c - 2, mc -> VfxCapture.clip(this.prefix + "clip_collect", 120, 0.6666666666666666))
         );
      }

      t = this.take(
         s,
         t,
         8,
         day(KageCapture::corpseAhead),
         8,
         KageCapture::inject,
         rig(5.0, 245.0F, 12.0F, 0.0),
         64,
         c -> this.stills(s, c, "zombie_rise", 2, 6, 10, 14, 20, 24, 27, 30, 36, 44, 54, 62)
      );
      t = this.take(s, t, 8, day(KageCapture::corpseAhead), 8, KageCapture::inject, rig(9.0, 120.0F, 14.0F, 0.6), 110, c -> {
         s.add(c + 60, mc -> s.server(mc, p -> p.serverLevel().getEntitiesOfClass(PirateFighter.class, p.getBoundingBox().inflate(16.0)).forEach(d -> {
                  d.hurt(p.damageSources().playerAttack(p), 1.0F);
                  p.setLastHurtMob(d);
               })));
         this.stills(s, c, "zombie_fight", 62, 70, 80, 90, 100, 108);
      });
      return this.take(
         s,
         t,
         5,
         day(KageCapture::corpseAhead),
         8,
         KageCapture::inject,
         rig(5.0, 245.0F, 12.0F, 0.0),
         62,
         c -> s.add(c - 2, mc -> VfxCapture.clip(this.prefix + "clip_zombie", 93, 0.6666666666666666))
      );
   }

   private int m5(AutoTest s, int t) {
      Consumer<ServerPlayer> summoned = p -> castAbility(p, "devilfruits:kage_kage/doppelman", 1);
      t = this.take(
         s, t, 8, day(none()), 0, summoned, rig(4.8, 330.0F, 8.0F, 0.2), 50, c -> this.stills(s, c, "doppel_rise", 2, 6, 10, 14, 18, 22, 26, 30, 36, 44)
      );
      t = this.take(
         s, t, 8, day(summoned), 46, summoned, onDummy(7.0, 100.0F, 10.0F, 0.6), 60, c -> this.stills(s, c, "doppel_attack", 4, 10, 16, 22, 28, 34, 40, 46, 54)
      );
      t = this.take(s, t, 8, night(summoned), 46, p -> {
         p.setXRot(-80.0F);
         castAbility(p, "devilfruits:kage_kage/doppelman", 1);
      }, rig(4.8, 330.0F, 8.0F, 0.2), 30, c -> this.stills(s, c, "doppel_recall", 2, 6, 10, 14, 18, 24));
      return this.take(
         s,
         t,
         5,
         day(none()),
         0,
         summoned,
         rig(4.8, 330.0F, 8.0F, 0.2),
         50,
         c -> s.add(c - 2, mc -> VfxCapture.clip(this.prefix + "clip_doppel", 75, 0.6666666666666666))
      );
   }

   private int m4(AutoTest s, int t, boolean trapOnly) {
      t = this.take(
         s,
         t,
         8,
         day(none()),
         0,
         p -> castAbility(p, "devilfruits:kage_kage/shadow_trap", 1),
         onDummy(8.0, 140.0F, 14.0F, 1.0),
         145,
         c -> this.stills(s, c, "trap", 6, 12, 18, 24, 30, 36, 40, 42, 44, 47, 50, 62, 80, 104, 107, 109, 114, 122, 132)
      );
      t = this.take(
         s,
         t,
         8,
         night(none()),
         0,
         p -> castAbility(p, "devilfruits:kage_kage/shadow_trap", 1),
         onDummy(6.0, 220.0F, 10.0F, 1.0),
         125,
         c -> this.stills(s, c, "trap_night", 38, 42, 45, 50, 66, 107, 110)
      );
      t = this.take(s, t, 8, night(none()), 0, none(), firstPerson(), 70, c -> {
         s.add(c, mc -> VfxDirector.spawn(new SwallowedView(mc.player, 50, 7L)));
         this.stills(s, c, "swallowed", 3, 8, 16, 30, 46, 62);
      });
      if (trapOnly) {
         return t;
      } else {
         t = this.take(
            s,
            t,
            8,
            day(none()),
            0,
            p -> castAbility(p, "devilfruits:kage_kage/shadow_hand", 1),
            onDummy(11.0, 95.0F, 10.0F, 2.0),
            90,
            c -> this.stills(s, c, "hand_side", 4, 8, 11, 15, 20, 26, 32, 36, 40, 44, 46, 48, 49, 51, 55, 62, 70, 80)
         );
         t = this.take(
            s,
            t,
            8,
            day(none()),
            0,
            p -> castAbility(p, "devilfruits:kage_kage/shadow_hand", 1),
            rig(6.0, 195.0F, 14.0F, 1.5),
            70,
            c -> this.stills(s, c, "hand_back", 20, 36, 46, 48, 50, 60)
         );
         t = this.take(
            s,
            t,
            5,
            day(none()),
            0,
            p -> castAbility(p, "devilfruits:kage_kage/shadow_trap", 1),
            onDummy(8.0, 140.0F, 14.0F, 1.0),
            142,
            c -> s.add(c - 2, mc -> VfxCapture.clip(this.prefix + "clip_trap", 213, 0.6666666666666666))
         );
         return this.take(
            s,
            t,
            5,
            day(none()),
            0,
            p -> castAbility(p, "devilfruits:kage_kage/shadow_hand", 1),
            onDummy(11.0, 95.0F, 10.0F, 2.0),
            88,
            c -> s.add(c - 2, mc -> VfxCapture.clip(this.prefix + "clip_hand", 132, 0.6666666666666666))
         );
      }
   }

   private void slashes(AutoTest s, int c, String name) {
      for (int k = 0; k < 3; k++) {
         int at = 6 + k * 14;
         s.add(c + at, mc -> KeyMapping.click(mc.options.keyAttack.getKey()));
         this.stills(s, c, name + k, at + 2, at + 3, at + 5);
      }
   }

   private int fixes(AutoTest s, int t, boolean all) {
      t = this.take(s, t, 8, day(p -> scissors(p, true)), 6, none(), rig(3.4, 330.0F, 6.0F, 0.2), 52, c -> {
         this.stills(s, c, "guard", 2);
         this.slashes(s, c, "slash");
      });
      t = this.take(s, t, 8, day(p -> scissors(p, true)), 6, none(), rig(3.4, 200.0F, 6.0F, 0.2), 8, c -> this.stills(s, c, "guard_back", 2));
      t = this.take(s, t, 8, day(p -> scissors(p, true)), 6, none(), rig(2.8, 270.0F, 4.0F, 0.3), 8, c -> this.stills(s, c, "guard_side_a", 2));
      t = this.take(s, t, 8, day(p -> scissors(p, true)), 6, none(), rig(2.8, 90.0F, 4.0F, 0.3), 8, c -> this.stills(s, c, "guard_side_b", 2));
      t = this.take(s, t, 8, day(p -> scissors(p, true)), 6, none(), mc -> {
         firstPerson().accept(mc);
         mc.options.hideGui = false;
      }, 52, c -> {
         this.stills(s, c, "fp_guard", 2);
         this.slashes(s, c, "fp_slash");
         s.add(c + 51, mc -> mc.options.hideGui = true);
      });
      if (!all) {
         return t;
      } else {
         t = this.take(
            s,
            t,
            8,
            day(none()),
            0,
            p -> castAbility(p, "devilfruits:kage_kage/shadow_trap", 1),
            onDummy(4.2, 200.0F, 8.0F, 1.2),
            70,
            c -> this.stills(s, c, "casket", 36, 52, 66)
         );
         t = this.take(
            s,
            t,
            8,
            day(none()),
            0,
            p -> castAbility(p, "devilfruits:kage_kage/shadow_spear", 1),
            onDummy(3.0, 160.0F, 4.0F, 0.8),
            30,
            c -> this.stills(s, c, "spear", 18, 22)
         );
         Consumer<ServerPlayer> summoned = p -> castAbility(p, "devilfruits:kage_kage/doppelman", 1);
         return this.take(s, t, 8, day(summoned), 46, DoppelmanAbility::swap, rig(6.5, 300.0F, 14.0F, 0.4), 20, c -> {
            this.shot(s, c - 2, "swap_before");
            this.stills(s, c, "swap", 1, 3, 8, 16);
         });
      }
   }

   private int rims(AutoTest s, int t) {
      t = this.take(
         s,
         t,
         8,
         day(none()),
         0,
         p -> castAbility(p, "devilfruits:kage_kage/shadow_trap", 1),
         onDummy(5.0, 200.0F, 8.0F, 1.2),
         70,
         c -> this.stills(s, c, "casket", 30, 52)
      );
      t = this.take(
         s,
         t,
         8,
         night(none()),
         0,
         p -> castAbility(p, "devilfruits:kage_kage/shadow_trap", 1),
         onDummy(5.0, 140.0F, 8.0F, 1.2),
         70,
         c -> this.stills(s, c, "casket_night", 30, 52)
      );
      t = this.take(
         s,
         t,
         8,
         day(none()),
         0,
         p -> castAbility(p, "devilfruits:kage_kage/shadow_spear", 1),
         onDummy(3.6, 140.0F, 6.0F, 0.6),
         40,
         c -> this.stills(s, c, "spear", 16, 30)
      );
      t = this.take(
         s,
         t,
         8,
         night(none()),
         0,
         p -> castAbility(p, "devilfruits:kage_kage/shadow_spear", 1),
         onDummy(3.6, 220.0F, 6.0F, 0.6),
         40,
         c -> this.stills(s, c, "spear_night", 16, 30)
      );
      t = this.take(
         s,
         t,
         8,
         day(none()),
         0,
         p -> castAbility(p, "devilfruits:kage_kage/shadow_hand", 1),
         onDummy(11.0, 95.0F, 10.0F, 2.0),
         60,
         c -> this.stills(s, c, "hand", 20, 36, 50)
      );
      t = this.take(
         s,
         t,
         8,
         night(none()),
         0,
         p -> castAbility(p, "devilfruits:kage_kage/shadow_hand", 1),
         rig(6.0, 195.0F, 14.0F, 1.5),
         60,
         c -> this.stills(s, c, "hand_night", 20, 36, 50)
      );
      t = this.take(
         s,
         t,
         8,
         day(none()),
         0,
         p -> castAbility(p, "devilfruits:kage_kage/doppelman", 1),
         rig(4.8, 330.0F, 8.0F, 0.2),
         60,
         c -> this.stills(s, c, "doppel", 20, 50)
      );
      return this.take(
         s,
         t,
         8,
         night(none()),
         0,
         p -> castAbility(p, "devilfruits:kage_kage/doppelman", 1),
         rig(4.8, 330.0F, 8.0F, 0.2),
         60,
         c -> this.stills(s, c, "doppel_night", 50)
      );
   }

   private void stills(AutoTest s, int c, String name, int... ticks) {
      for (int k : ticks) {
         this.shot(s, c + k, name + "_" + (k < 10 ? "0" : "") + k);
      }
   }

   @Override
   protected int takes(AutoTest s, int t) {
      if (this.stage.equals("m6")) {
         return this.m6(s, t, false);
      } else if (this.stage.equals("zombie")) {
         return this.m6(s, t, true);
      } else if (this.stage.equals("rims")) {
         return this.rims(s, t);
      } else if (this.stage.equals("blades") || this.stage.equals("fixes")) {
         return this.fixes(s, t, this.stage.equals("fixes"));
      } else if (this.stage.equals("m5")) {
         return this.m5(s, t);
      } else if (this.stage.equals("m4") || this.stage.equals("trap")) {
         return this.m4(s, t, this.stage.equals("trap"));
      } else if (this.stage.equals("spear")) {
         t = this.take(
            s,
            t,
            8,
            day(none()),
            0,
            p -> castAbility(p, "devilfruits:kage_kage/shadow_spear", 1),
            onDummy(3.6, 140.0F, 6.0F, 0.6),
            60,
            c -> this.stills(s, c, "spear_day", 16, 18, 19, 22, 30)
         );
         return this.take(
            s,
            t,
            8,
            night(none()),
            0,
            p -> castAbility(p, "devilfruits:kage_kage/shadow_spear", 1),
            onDummy(3.6, 220.0F, 6.0F, 0.6),
            60,
            c -> this.stills(s, c, "spear_night", 18, 19, 22, 30)
         );
      } else {
         t = this.take(s, t, 8, none(), 0, aegisUp(), rig(4.2, 330.0F, 10.0F, 0.1), 40, c -> this.stills(s, c, "aegis_rise", 2, 4, 7, 10, 15, 30));
         t = this.take(s, t, 8, aegisUp(), 24, p -> arrow(p, 7.0, 0.4), rig(5.0, 270.0F, 6.0F, 0.1), 40, c -> {
            s.add(c + 6, mc -> s.server(mc, p -> arrow(p, 7.0, -0.8)));
            s.add(c + 12, mc -> s.server(mc, KageCapture::fireball));
            this.stills(s, c, "aegis_catch", 1, 2, 3, 4, 6, 9, 14, 18, 22, 26);
         });
         t = this.take(
            s,
            t,
            8,
            aegisUp(),
            24,
            p -> castAbility(p, "devilfruits:kage_kage/brick_bat", 1),
            rig(7.5, 215.0F, 16.0F, 1.2),
            44,
            c -> this.stills(s, c, "swarm_wide", 2, 4, 6, 8, 11, 14, 17, 20, 23, 26, 30, 34)
         );
         t = this.take(
            s,
            t,
            8,
            aegisUp(),
            24,
            p -> castAbility(p, "devilfruits:kage_kage/brick_bat", 1),
            onDummy(5.0, 95.0F, 10.0F, 0.3),
            44,
            c -> this.stills(s, c, "swarm_hit", 14, 18, 21, 23, 25, 27, 30, 34)
         );
         t = this.take(
            s,
            t,
            8,
            night(none()),
            0,
            p -> castAbility(p, "devilfruits:kage_kage/shadow_spear", 1),
            onDummy(5.5, 130.0F, 10.0F, 0.3),
            90,
            c -> this.stills(s, c, "spear", 3, 6, 9, 11, 13, 15, 17, 18, 19, 21, 25, 40, 68, 71, 75, 80)
         );
         t = this.take(
            s,
            t,
            8,
            day(none()),
            0,
            p -> castAbility(p, "devilfruits:kage_kage/shadow_spear", 1),
            rig(4.5, 195.0F, 10.0F, 0.7),
            40,
            c -> this.stills(s, c, "spear_fp", 4, 9, 15, 18, 22, 30)
         );
         t = this.take(
            s,
            t,
            5,
            day(aegisUp()),
            24,
            p -> castAbility(p, "devilfruits:kage_kage/brick_bat", 1),
            rig(7.0, 225.0F, 14.0F, 1.0),
            44,
            c -> s.add(c - 2, mc -> VfxCapture.clip(this.prefix + "clip_swarm", 66, 0.6666666666666666))
         );
         return this.take(
            s,
            t,
            5,
            night(none()),
            0,
            p -> castAbility(p, "devilfruits:kage_kage/shadow_spear", 1),
            onDummy(5.5, 130.0F, 10.0F, 0.3),
            92,
            c -> s.add(c - 2, mc -> VfxCapture.clip(this.prefix + "clip_spear", 138, 0.6666666666666666))
         );
      }
   }
}
