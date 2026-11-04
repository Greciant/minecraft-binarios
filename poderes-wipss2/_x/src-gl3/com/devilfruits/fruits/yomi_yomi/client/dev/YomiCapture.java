package com.devilfruits.fruits.yomi_yomi.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureRig;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.yomi_yomi.Vessel;
import com.devilfruits.fruits.yomi_yomi.WaterRun;
import com.devilfruits.fruits.yomi_yomi.YomiItems;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.client.SoulKingClient;
import com.devilfruits.fruits.yomi_yomi.client.WaterRunClient;
import com.devilfruits.fruits.yomi_yomi.client.vfx.YomiCinematic;
import com.devilfruits.fruits.yomi_yomi.client.vfx.YomiRender;
import com.devilfruits.fruits.yomi_yomi.item.SoulCaneItem;
import com.devilfruits.fruits.yomi_yomi.soul.SoulPhase;
import com.devilfruits.fruits.yomi_yomi.soul.YomiSoul;
import com.devilfruits.fruits.yomi_yomi.soulking.SoulKing;
import com.devilfruits.mobs.pirate.entity.PirateFighter;
import com.devilfruits.registry.DFAttachments;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class YomiCapture extends CaptureStage {
   private static final int RATE = 8;
   private static final int CLIP_RATE = 5;
   private static final String AUBADE = "devilfruits:yomi_yomi/aubade";
   private static final String GAVOTTE = "devilfruits:yomi_yomi/gavotte";
   private static final String FUBUKI = "devilfruits:yomi_yomi/fubuki_giri";
   private static final String POLKA = "devilfruits:yomi_yomi/polka_remise";
   private final YomiCapture.Stage stage;
   private static final String PROJECTION = "devilfruits:yomi_yomi/soul_projection";
   private static final String SOUL_DASH = "devilfruits:yomi_yomi/soul_dash";
   private static final String COLD_SOUL = "devilfruits:yomi_yomi/cold_soul";
   private static final String SOUL_KING = "devilfruits:yomi_yomi/soul_king";
   private static final String STRIKE = "devilfruits:yomi_yomi/king_strike";
   private static final String EARTH = "devilfruits:yomi_yomi/earth_crushing";
   private static final String ROAR = "devilfruits:yomi_yomi/soul_king_roar";
   private boolean night;

   public YomiCapture(String prefix) {
      this(prefix, YomiCapture.Stage.M4);
   }

   public YomiCapture(String prefix, YomiCapture.Stage stage) {
      super(prefix);
      this.stage = stage;
   }

   @Override
   protected ResourceLocation fruit() {
      return YomiYomi.ID;
   }

   @Override
   protected void setup(ServerPlayer p) {
      p.setData(YomiSoul.ATTACHMENT, YomiSoul.NONE);
      SoulPhase.becomeVessel(p, Vessel.SKELETON);
      arm(p, true);
   }

   @Override
   protected void onReset(ServerPlayer p) {
      command(p, "time set " + (this.night ? 18000 : 6000));
      PlayerFruitData data = DFAttachments.data(p);
      data.clearCooldowns();
      data.setEnergy(1000.0F);
      Minecraft.getInstance().execute(() -> {
         for (int i = 0; i < 64; i++) {
            YomiCinematic c = VfxDirector.find(YomiCinematic.class, k -> !k.isDone());
            if (c == null) {
               break;
            }

            c.finish();
         }

         YomiRender.clear();
         SoulKingClient.clear();
         VfxParticles.clear();
      });
   }

   private Consumer<Minecraft> lighting(boolean dark) {
      return mc -> this.night = dark;
   }

   private static Consumer<Minecraft> handsInView() {
      return mc -> {
         CaptureRig.off();
         mc.options.setCameraType(CameraType.FIRST_PERSON);
         mc.options.hideGui = false;
      };
   }

   private static Consumer<Minecraft> guiOff() {
      return mc -> mc.options.hideGui = true;
   }

   private static void arm(ServerPlayer p, boolean drawn) {
      ItemStack cane = new ItemStack((ItemLike)YomiItems.SOUL_CANE.get());
      p.setItemInHand(InteractionHand.MAIN_HAND, cane);
      p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      if (drawn) {
         SoulCaneItem.draw(p);
      }
   }

   private static Consumer<Minecraft> onDummy(double dist, float yaw, float pitch) {
      return mc -> {
         if (mc.level != null) {
            for (Entity e : mc.level.entitiesForRendering()) {
               if (e instanceof PirateFighter) {
                  mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                  CaptureRig.follow(e, dist, yaw, pitch, 0.0);
                  return;
               }
            }
         }
      };
   }

   @Override
   protected int takes(AutoTest s, int t) {
      if (this.stage == YomiCapture.Stage.FUBUKI) {
         return this.fubuki(s, t);
      } else if (this.stage == YomiCapture.Stage.M5) {
         return this.m5(s, t);
      } else if (this.stage == YomiCapture.Stage.KING) {
         return this.king(s, t);
      } else if (this.stage == YomiCapture.Stage.KING_LOOK) {
         return this.kingLook(s, t);
      } else if (this.stage == YomiCapture.Stage.ICE) {
         return this.ice(s, t);
      } else if (this.stage == YomiCapture.Stage.RUN) {
         return this.run(s, t);
      } else if (this.stage == YomiCapture.Stage.WATER) {
         return this.water(s, t);
      } else if (this.stage == YomiCapture.Stage.CANE) {
         t = this.cane(s, t);
         return this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:yomi_yomi/fubuki_giri", 1), rig(4.5, 125.0F, 10.0F, 0.6), 50, c -> {
            for (int k : new int[]{14, 18, 22, 26, 28, 32, 36, 40}) {
               this.shot(s, c + k, "noto_" + k);
            }
         });
      } else if (this.stage == YomiCapture.Stage.POLKA) {
         t = this.take(
            s, t, 8, p -> dummyTo(s, p, -6.5), 2, p -> castAbility(p, "devilfruits:yomi_yomi/polka_remise", 40), rig(4.6, 60.0F, 8.0F, 0.4), 44, c -> {
               for (int k : new int[]{5, 9, 15, 21, 27, 29, 34}) {
                  this.shot(s, c + k, "polka_" + k);
               }
            }
         );
         return this.take(
            s,
            t,
            5,
            p -> dummyTo(s, p, -6.5),
            2,
            p -> castAbility(p, "devilfruits:yomi_yomi/polka_remise", 40),
            rig(4.6, 60.0F, 8.0F, 0.4),
            50,
            c -> s.add(c - 1, mc -> VfxCapture.clip(this.prefix + "clip_polka", 72, 0.6666666666666666))
         );
      } else {
         t = this.cane(s, t);
         t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:yomi_yomi/aubade", 1), rig(5.0, 120.0F, 10.0F, 0.5), 24, c -> {
            for (int k : new int[]{4, 6, 8, 9, 11, 15}) {
               this.shot(s, c + k, "aubade_" + k);
            }
         });
         t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:yomi_yomi/gavotte", 1), rig(8.5, 100.0F, 10.0F, 1.2), 44, c -> {
            for (int k : new int[]{3, 6, 10, 16, 17, 18, 26, 34}) {
               this.shot(s, c + k, "gavotte_" + k);
            }
         });
         t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:yomi_yomi/fubuki_giri", 1), rig(9.0, 125.0F, 16.0F, 0.6), 96, c -> {
            for (int k : new int[]{5, 8, 10, 18, 26, 28, 30, 36, 56, 80}) {
               this.shot(s, c + k, "fubuki_" + k);
            }
         });
         t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:yomi_yomi/fubuki_giri", 1), rig(9.0, 125.0F, 16.0F, 0.6), 50, c -> {
            s.add(c + 9, rig(3.0, -40.0F, 6.0F, 0.1));

            for (int k : new int[]{12, 15, 17, 19, 21, 23, 25, 27, 28, 31, 36, 39}) {
               this.shot(s, c + k, "noto_" + k);
            }
         });
         t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:yomi_yomi/fubuki_giri", 1), mc -> {
         }, 74, c -> {
            s.add(c + 26, onDummy(3.4, 160.0F, 12.0F));
            this.shot(s, c + 34, "frozen_34");
            s.add(c + 36, onDummy(3.2, 215.0F, 8.0F));
            this.shot(s, c + 42, "frozen_42");
            s.add(c + 44, onDummy(3.6, 95.0F, 16.0F));
            this.shot(s, c + 50, "frozen_50");
            s.add(c + 70, mc -> CaptureRig.off());
         });
         t = this.take(
            s, t, 8, p -> dummyTo(s, p, -6.5), 2, p -> castAbility(p, "devilfruits:yomi_yomi/polka_remise", 40), rig(4.6, 60.0F, 8.0F, 0.4), 44, c -> {
               for (int k : new int[]{3, 5, 8, 11, 15, 19, 23, 27, 29, 34}) {
                  this.shot(s, c + k, "polka_" + k);
               }
            }
         );
         t = this.take(s, t, 8, p -> dummyTo(s, p, -6.5), 2, p -> castAbility(p, "devilfruits:yomi_yomi/polka_remise", 40), handsInView(), 40, c -> {
            for (int k : new int[]{6, 9, 12, 16, 28}) {
               this.shot(s, c + k, "polka_fp_" + k);
            }

            s.add(c + 36, guiOff());
         });
         s.add(t, this.lighting(true));
         t = this.take(s, t + 1, 8, none(), 0, p -> castAbility(p, "devilfruits:yomi_yomi/fubuki_giri", 1), rig(9.0, 125.0F, 16.0F, 0.6), 64, c -> {
            for (int k : new int[]{18, 27, 36}) {
               this.shot(s, c + k, "night_fubuki_" + k);
            }
         });
         t = this.take(
            s, t, 8, p -> dummyTo(s, p, -6.5), 2, p -> castAbility(p, "devilfruits:yomi_yomi/polka_remise", 40), rig(4.6, 60.0F, 8.0F, 0.4), 40, c -> {
               for (int k : new int[]{11, 23, 29}) {
                  this.shot(s, c + k, "night_polka_" + k);
               }
            }
         );
         s.add(t, this.lighting(false));
         t = this.take(
            s,
            t + 1,
            5,
            none(),
            0,
            p -> castAbility(p, "devilfruits:yomi_yomi/fubuki_giri", 1),
            rig(9.0, 125.0F, 16.0F, 0.6),
            96,
            c -> s.add(c - 1, mc -> VfxCapture.clip(this.prefix + "clip_fubuki", 140, 0.6666666666666666))
         );
         t = this.take(
            s,
            t,
            5,
            p -> dummyTo(s, p, -6.5),
            2,
            p -> castAbility(p, "devilfruits:yomi_yomi/polka_remise", 40),
            rig(4.6, 60.0F, 8.0F, 0.4),
            50,
            c -> s.add(c - 1, mc -> VfxCapture.clip(this.prefix + "clip_polka", 72, 0.6666666666666666))
         );
         return this.take(s, t, 5, none(), 0, p -> castAbility(p, "devilfruits:yomi_yomi/fubuki_giri", 1), rig(9.0, 125.0F, 16.0F, 0.6), 70, c -> {
            s.add(c + 9, rig(3.0, -40.0F, 6.0F, 0.1));
            s.add(c + 9, mc -> VfxCapture.clip(this.prefix + "clip_noto", 66, 0.6666666666666666));
         });
      }
   }

   private int cane(AutoTest s, int t) {
      return this.take(s, t, 20, p -> arm(p, false), 6, p -> {
      }, rig(2.6, 30.0F, 10.0F, 0.0), 80, c -> {
         this.shot(s, c + 4, "cane_4");
         s.add(c + 6, rig(2.6, 90.0F, 6.0F, 0.0));
         this.shot(s, c + 10, "cane_10");
         s.add(c + 12, mc -> s.server(mc, p -> arm(p, true)));
         s.add(c + 14, rig(2.6, 30.0F, 10.0F, 0.0));
         this.shot(s, c + 20, "cane_20");
         s.add(c + 22, rig(2.6, 90.0F, 6.0F, 0.0));
         this.shot(s, c + 28, "cane_28");
         s.add(c + 29, rig(2.8, 200.0F, 8.0F, 0.0));
         this.shot(s, c + 34, "cane_34");
         s.add(c + 36, handsInView());
         this.shot(s, c + 42, "cane_42");
         s.add(c + 44, mc -> s.server(mc, p -> command(p, "gamemode survival")));
         s.add(c + 47, mc -> mc.setScreen(new InventoryScreen(mc.player)));
         this.shot(s, c + 52, "cane_52");
         s.add(c + 54, mc -> {
            mc.setScreen(null);
            s.server(mc, p -> command(p, "gamemode creative"));
         });
         s.add(c + 56, guiOff());
      });
   }

   private static void guitar(ServerPlayer p) {
      p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)YomiItems.SOUL_GUITAR.get()));
      p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
   }

   private int king(AutoTest s, int t) {
      t = this.take(s, t, 20, p -> guitar(p), 10, p -> {
      }, rig(2.8, 20.0F, 8.0F, 0.1), 10, c -> {
      });
      t = this.take(s, t, 20, p -> guitar(p), 4, p -> {
      }, rig(2.8, 20.0F, 8.0F, 0.1), 44, c -> {
         this.shot(s, c + 4, "guitar_4");
         s.add(c + 6, rig(2.8, 90.0F, 6.0F, 0.1));
         this.shot(s, c + 10, "guitar_10");
         s.add(c + 12, rig(2.8, 210.0F, 10.0F, 0.1));
         this.shot(s, c + 16, "guitar_16");
         s.add(c + 18, handsInView());
         this.shot(s, c + 22, "guitar_22");
         s.add(c + 24, mc -> s.server(mc, p -> command(p, "gamemode survival")));
         s.add(c + 29, mc -> mc.setScreen(new InventoryScreen(mc.player)));
         this.shot(s, c + 34, "guitar_34");
         s.add(c + 36, mc -> {
            mc.setScreen(null);
            s.server(mc, p -> command(p, "gamemode creative"));
         });
         s.add(c + 38, guiOff());
      });
      t = this.take(s, t, 8, p -> {
         guitar(p);
         dummyTo(s, p, -15.5);
      }, 2, p -> castAbility(p, "devilfruits:yomi_yomi/soul_king", 1), rig(8.0, 20.0F, 4.0F, 2.4), 96, c -> {
         for (int k : new int[]{4, 10, 16, 22, 28, 31, 36, 40, 42, 46, 52, 56, 58, 62, 70, 84}) {
            this.shot(s, c + k, "summon_" + k);
         }
      });
      t = this.take(s, t, 8, p -> dummyTo(s, p, -15.5), 2, p -> {
      }, rig(9.0, 95.0F, 4.0F, 2.6), 30, c -> {
         this.shot(s, c + 6, "idle_6");
         s.add(c + 8, rig(5.0, 180.0F, 12.0F, 1.2));
         this.shot(s, c + 14, "idle_14");
         s.add(c + 16, rig(4.0, 180.0F, -12.0F, 1.0));
         this.shot(s, c + 22, "idle_22");
      });
      t = this.take(s, t, 8, p -> dummyTo(s, p, -5.5), 2, p -> castAbility(p, "devilfruits:yomi_yomi/king_strike", 1), rig(8.5, 75.0F, 8.0F, 2.2), 64, c -> {
         s.add(c + 18, mc -> s.server(mc, p -> castAbility(p, "devilfruits:yomi_yomi/king_strike", 1)));
         s.add(c + 36, mc -> s.server(mc, p -> castAbility(p, "devilfruits:yomi_yomi/king_strike", 1)));

         for (int k : new int[]{5, 8, 10, 12, 25, 27, 28, 30, 44, 47, 49, 51, 58}) {
            this.shot(s, c + k, "strike_" + k);
         }
      });
      t = this.take(
         s, t, 8, p -> dummyTo(s, p, -8.5), 2, p -> castAbility(p, "devilfruits:yomi_yomi/earth_crushing", 1), rig(11.0, 70.0F, 14.0F, 2.0), 70, c -> {
            for (int k : new int[]{8, 16, 22, 25, 26, 28, 30, 33, 40, 56, 66}) {
               this.shot(s, c + k, "crush_" + k);
            }
         }
      );
      t = this.take(
         s, t, 8, p -> dummyTo(s, p, -6.5), 2, p -> castAbility(p, "devilfruits:yomi_yomi/soul_king_roar", 1), rig(9.0, 40.0F, 4.0F, 2.8), 50, c -> {
            for (int k : new int[]{6, 14, 22, 25, 26, 28, 31, 36, 44}) {
               this.shot(s, c + k, "roar_" + k);
            }
         }
      );
      t = this.take(s, t, 20, none(), 0, p -> {
      }, handsInView(), 80, c -> {
         s.add(c + 1, mc -> SoulKingClient.previewStun(100));

         for (int k : new int[]{4, 30, 60, 90}) {
            this.shot(s, c + k, "stunned_" + k);
         }

         s.add(c + 96, guiOff());
      });
      s.add(t, this.lighting(true));
      t = this.take(s, t + 1, 8, p -> {
         guitar(p);
         dummyTo(s, p, -15.5);
         if (SoulKing.active(p)) {
            castAbility(p, "devilfruits:yomi_yomi/soul_king", 1);
         }
      }, 36, p -> {
         DFAttachments.data(p).clearCooldowns();
         castAbility(p, "devilfruits:yomi_yomi/soul_king", 1);
      }, rig(8.0, 20.0F, 4.0F, 2.4), 90, c -> {
         for (int k : new int[]{22, 42, 56, 70}) {
            this.shot(s, c + k, "night_summon_" + k);
         }
      });
      t = this.take(
         s, t, 8, p -> dummyTo(s, p, -6.5), 2, p -> castAbility(p, "devilfruits:yomi_yomi/soul_king_roar", 1), rig(9.0, 40.0F, 4.0F, 2.8), 40, c -> {
            for (int k : new int[]{14, 26, 31}) {
               this.shot(s, c + k, "night_roar_" + k);
            }
         }
      );
      s.add(t, this.lighting(false));
      t = this.take(
         s, t + 1, 8, p -> dummyTo(s, p, -15.5), 2, p -> castAbility(p, "devilfruits:yomi_yomi/soul_king", 1), rig(8.0, 20.0F, 4.0F, 2.4), 40, c -> {
            for (int k : new int[]{2, 8, 14, 20, 26, 32}) {
               this.shot(s, c + k, "vanish_" + k);
            }
         }
      );
      t = this.take(
         s,
         t,
         5,
         p -> {
            guitar(p);
            dummyTo(s, p, -15.5);
            DFAttachments.data(p).clearCooldowns();
         },
         4,
         p -> castAbility(p, "devilfruits:yomi_yomi/soul_king", 1),
         rig(8.0, 20.0F, 4.0F, 2.4),
         96,
         c -> s.add(c - 1, mc -> VfxCapture.clip(this.prefix + "clip_summon", 150, 0.6666666666666666))
      );
      return this.take(
         s, t, 5, p -> dummyTo(s, p, -5.5), 2, p -> castAbility(p, "devilfruits:yomi_yomi/king_strike", 1), rig(10.0, 70.0F, 10.0F, 2.2), 110, c -> {
            s.add(c - 1, mc -> VfxCapture.clip(this.prefix + "clip_fight", 160, 0.6666666666666666));
            s.add(c + 18, mc -> s.server(mc, p -> castAbility(p, "devilfruits:yomi_yomi/king_strike", 1)));
            s.add(c + 36, mc -> s.server(mc, p -> castAbility(p, "devilfruits:yomi_yomi/king_strike", 1)));
            s.add(c + 62, mc -> s.server(mc, p -> {
                  dummyTo(s, p, -8.5);
                  castAbility(p, "devilfruits:yomi_yomi/earth_crushing", 1);
               }));
         }
      );
   }

   private int ice(AutoTest s, int t) {
      t = this.take(s, t, 20, p -> arm(p, true), 10, p -> {
      }, rig(2.8, 20.0F, 8.0F, 0.1), 10, c -> {
      });
      t = this.take(s, t, 8, p -> {
         p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
         p.getAbilities().flying = false;
         p.onUpdateAbilities();
         p.teleportTo(p.serverLevel(), 0.5, (double)s.origin().getY(), 1.5, 0.0F, 10.0F);
      }, 4, p -> {
      }, rig(4.0, 90.0F, 6.0F, 0.4), 44, c -> {
         s.add(c + 1, mc -> {
            mc.options.keyUp.setDown(true);
            mc.options.keySprint.setDown(true);
         });

         for (int k = 2; k < 40; k++) {
            s.add(c + k, mc -> {
               if (mc.player != null) {
                  mc.player.setSprinting(true);
               }
            });
         }

         for (int k : new int[]{10, 11, 12, 13, 14, 15}) {
            this.shot(s, c + k, "run_" + k);
         }

         s.add(c + 17, rig(4.0, 160.0F, 6.0F, 0.4));

         for (int k : new int[]{22, 23, 24, 25}) {
            this.shot(s, c + k, "run_" + k);
         }

         s.add(c + 27, rig(4.0, 0.0F, 10.0F, 0.4));

         for (int k : new int[]{32, 33}) {
            this.shot(s, c + k, "run_" + k);
         }

         s.add(c + 40, mc -> {
            mc.options.keyUp.setDown(false);
            mc.options.keySprint.setDown(false);
         });
      });
      t = this.take(s, t, 8, p -> arm(p, true), 4, p -> castAbility(p, "devilfruits:yomi_yomi/fubuki_giri", 1), rig(6.5, 110.0F, 14.0F, 0.6), 100, c -> {
         for (int k : new int[]{10, 28, 30, 33, 40, 56, 76, 80, 84, 88}) {
            this.shot(s, c + k, "fubuki_" + k);
         }
      });
      t = this.take(s, t, 8, p -> dummyTo(s, p, -6.5), 2, p -> castAbility(p, "devilfruits:yomi_yomi/polka_remise", 90), rig(4.6, 60.0F, 8.0F, 0.4), 96, c -> {
         for (int k : new int[]{10, 40, 70, 80, 86}) {
            this.shot(s, c + k, "polka_" + k);
         }
      });
      return this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:yomi_yomi/soul_projection", 1), rig(6.5, 60.0F, 14.0F, 0.4), 80, c -> {
         s.add(c + 20, mc -> s.server(mc, p -> castAbility(p, "devilfruits:yomi_yomi/cold_soul", 1)));

         for (int k : new int[]{26, 33, 41, 49, 60, 72}) {
            this.shot(s, c + k, "cold_" + k);
         }

         s.add(c + 76, mc -> s.server(mc, p -> castAbility(p, "devilfruits:yomi_yomi/soul_projection", 1)));
      });
   }

   private int run(AutoTest s, int t) {
      t = this.take(s, t, 20, p -> arm(p, false), 10, p -> {
      }, rig(2.8, 20.0F, 8.0F, 0.1), 10, c -> {
      });
      t = this.take(s, t, 5, p -> {
         p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
         p.getAbilities().flying = false;
         p.onUpdateAbilities();
         p.teleportTo(p.serverLevel(), 0.5, (double)s.origin().getY(), 1.5, 0.0F, 10.0F);
      }, 4, p -> {
      }, rig(3.6, 75.0F, 6.0F, 0.5), 70, c -> {
         s.add(c + 1, mc -> {
            mc.options.keyUp.setDown(true);
            mc.options.keySprint.setDown(true);
         });

         for (int k = 2; k < 66; k++) {
            s.add(c + k, mc -> {
               if (mc.player != null) {
                  mc.player.setSprinting(true);
               }
            });
         }

         s.add(c + 8, mc -> VfxCapture.clip(this.prefix + "clip_run", 90, 0.6666666666666666));
         s.add(c + 66, mc -> {
            mc.options.keyUp.setDown(false);
            mc.options.keySprint.setDown(false);
         });
      });
      return this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:yomi_yomi/soul_projection", 1), rig(6.5, 60.0F, 14.0F, 0.4), 80, c -> {
         s.add(c + 20, mc -> s.server(mc, p -> castAbility(p, "devilfruits:yomi_yomi/cold_soul", 1)));

         for (int k : new int[]{41, 60}) {
            this.shot(s, c + k, "cold_" + k);
         }

         s.add(c + 76, mc -> s.server(mc, p -> castAbility(p, "devilfruits:yomi_yomi/soul_projection", 1)));
      });
   }

   private int water(AutoTest s, int t) {
      t = this.take(s, t, 20, p -> arm(p, false), 10, p -> {
      }, rig(2.8, 20.0F, 8.0F, 0.1), 10, c -> {
      });
      return this.take(
         s,
         t,
         20,
         p -> {
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            command(p, "gamemode survival");
            p.getFoodData().setFoodLevel(20);
            p.teleportTo(p.serverLevel(), 0.5, (double)s.origin().getY(), 1.5, 0.0F, 10.0F);
         },
         4,
         p -> {
         },
         rig(4.5, 75.0F, 8.0F, 0.5),
         110,
         c -> {
            s.add(c + 1, mc -> {
               mc.options.keyUp.setDown(true);
               mc.options.keySprint.setDown(true);
            });

            for (int k = 10; k <= 100; k += 10) {
               int at = k;
               s.add(
                  c + k,
                  mc -> {
                     LocalPlayer lp = mc.player;
                     if (lp != null) {
                        double top = WaterRun.surface(lp.level(), lp.getX(), lp.getY(), lp.getZ());
                        DevilFruits.LOGGER
                           .info(
                              "[devilfruits autotest] water {} client sprint={} running={} y={} top={} inWater={} energy={}",
                              new Object[]{
                                 at,
                                 lp.isSprinting(),
                                 WaterRunClient.running(lp),
                                 String.format("%.3f", lp.getY()),
                                 String.format("%.3f", top),
                                 lp.isInWater(),
                                 DFAttachments.data(lp).energy()
                              }
                           );
                        s.server(
                           mc,
                           p -> DevilFruits.LOGGER
                                 .info(
                                    "[devilfruits autotest] water {} server sprint={} onWater={} submerged={} suppressed={} weakness={} y={} food={}",
                                    new Object[]{
                                       at,
                                       p.isSprinting(),
                                       WaterRun.onWater(p),
                                       Weakness.isSubmerged(p),
                                       Weakness.isSuppressed(p),
                                       p.hasEffect(MobEffects.WEAKNESS),
                                       String.format("%.3f", p.getY()),
                                       p.getFoodData().getFoodLevel()
                                    }
                                 )
                        );
                     }
                  }
               );
            }

            for (int k : new int[]{20, 50, 80}) {
               this.shot(s, c + k, "water_" + k);
            }

            s.add(c + 104, mc -> {
               mc.options.keyUp.setDown(false);
               mc.options.keySprint.setDown(false);
               s.server(mc, p -> command(p, "gamemode creative"));
            });
         }
      );
   }

   private int kingLook(AutoTest s, int t) {
      t = this.take(s, t, 20, p -> guitar(p), 10, p -> {
      }, rig(2.8, 20.0F, 8.0F, 0.1), 10, c -> {
      });
      t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:yomi_yomi/soul_projection", 1), rig(3.2, 60.0F, 8.0F, 0.9), 70, c -> {
         for (int k : new int[]{14, 20}) {
            this.shot(s, c + k, "soul_" + k);
         }

         s.add(c + 22, mc -> s.server(mc, p -> castAbility(p, "devilfruits:yomi_yomi/cold_soul", 1)));

         for (int k : new int[]{28, 36, 44, 60}) {
            this.shot(s, c + k, "soul_" + k);
         }

         s.add(c + 64, mc -> s.server(mc, p -> castAbility(p, "devilfruits:yomi_yomi/soul_projection", 1)));
      });
      t = this.take(s, t, 8, p -> {
         guitar(p);
         dummyTo(s, p, -15.5);
      }, 2, p -> castAbility(p, "devilfruits:yomi_yomi/soul_king", 1), rig(9.0, 20.0F, 4.0F, 2.4), 90, c -> {
         for (int k : new int[]{16, 28, 36, 42, 46, 56, 70, 84}) {
            this.shot(s, c + k, "summon_" + k);
         }
      });
      t = this.take(s, t, 8, p -> dummyTo(s, p, -15.5), 2, p -> {
      }, rig(9.0, 95.0F, 4.0F, 2.6), 40, c -> {
         this.shot(s, c + 6, "idle_6");
         s.add(c + 8, rig(4.0, 180.0F, 10.0F, 1.4));
         this.shot(s, c + 14, "idle_14");
         s.add(c + 16, rig(7.0, 25.0F, 12.0F, 3.0));
         this.shot(s, c + 22, "idle_22");
         s.add(c + 24, rig(6.0, 140.0F, 18.0F, 3.4));
         this.shot(s, c + 30, "idle_30");
      });
      t = this.take(
         s, t, 8, p -> dummyTo(s, p, -8.5), 2, p -> castAbility(p, "devilfruits:yomi_yomi/earth_crushing", 1), rig(12.0, 70.0F, 16.0F, 2.0), 64, c -> {
            for (int k : new int[]{16, 24, 26, 27, 29, 31, 34, 40, 52}) {
               this.shot(s, c + k, "crush_" + k);
            }
         }
      );
      s.add(t, this.lighting(true));
      t = this.take(s, t + 1, 8, p -> dummyTo(s, p, -15.5), 2, p -> {
      }, rig(9.0, 60.0F, 6.0F, 2.6), 16, c -> {
         this.shot(s, c + 6, "night_6");
         s.add(c + 7, rig(4.0, 180.0F, 10.0F, 1.4));
         this.shot(s, c + 13, "night_13");
      });
      s.add(t, this.lighting(false));
      return t + 1;
   }

   private static void wall(AutoTest s, ServerPlayer p) {
      int y = s.origin().getY();

      for (int x = -3; x <= 3; x++) {
         for (int h = 0; h < 4; h++) {
            p.serverLevel().setBlockAndUpdate(new BlockPos(x, y + h, -6), Blocks.STONE_BRICKS.defaultBlockState());
         }
      }
   }

   private static void flyNorth(AutoTest s, int at, int ticks, double speed) {
      for (int i = 0; i < ticks; i++) {
         s.add(at + i, mc -> s.server(mc, p -> Targeting.moveSelf(p, new Vec3(0.0, 0.01, -speed))));
      }
   }

   private int m5(AutoTest s, int t) {
      t = this.take(s, t, 8, p -> wall(s, p), 2, p -> castAbility(p, "devilfruits:yomi_yomi/soul_projection", 1), rig(4.0, 30.0F, 10.0F, 0.3), 90, c -> {
         for (int k : new int[]{2, 6, 12, 20}) {
            this.shot(s, c + k, "project_" + k);
         }

         flyNorth(s, c + 22, 26, 0.36);
         s.add(c + 26, rig(7.0, 95.0F, 10.0F, 0.6));

         for (int k : new int[]{30, 34, 38, 44, 50}) {
            this.shot(s, c + k, "project_" + k);
         }

         s.add(c + 52, mc -> s.server(mc, p -> castAbility(p, "devilfruits:yomi_yomi/soul_projection", 1)));

         for (int k : new int[]{54, 57, 60, 62, 64, 70, 80}) {
            this.shot(s, c + k, "project_" + k);
         }
      });
      t = this.take(s, t, 8, p -> wall(s, p), 2, p -> castAbility(p, "devilfruits:yomi_yomi/soul_projection", 1), rig(9.0, 100.0F, 10.0F, 0.8), 60, c -> {
         s.add(c + 22, mc -> s.server(mc, p -> {
               p.setXRot(-3.0F);
               castAbility(p, "devilfruits:yomi_yomi/soul_dash", 1);
            }));

         for (int k : new int[]{26, 29, 30, 31, 33, 38}) {
            this.shot(s, c + k, "dash_" + k);
         }

         s.add(c + 50, mc -> s.server(mc, p -> castAbility(p, "devilfruits:yomi_yomi/soul_projection", 1)));
      });
      t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:yomi_yomi/soul_projection", 1), rig(6.5, 60.0F, 14.0F, 0.4), 70, c -> {
         s.add(c + 20, mc -> s.server(mc, p -> {
               p.teleportTo(0.5, (double)s.origin().getY() + 0.5, -7.5);
               castAbility(p, "devilfruits:yomi_yomi/cold_soul", 1);
            }));

         for (int k : new int[]{24, 31, 33, 39, 41, 47, 49, 55}) {
            this.shot(s, c + k, "cold_" + k);
         }

         s.add(c + 62, mc -> s.server(mc, p -> castAbility(p, "devilfruits:yomi_yomi/soul_projection", 1)));
      });
      t = this.take(s, t, 8, p -> {
         dummyTo(s, p, -5.0);
         command(p, "gamemode survival");
      }, 4, p -> {
         p.setHealth(2.0F);
         p.hurt(p.damageSources().generic(), 100.0F);
      }, rig(5.5, 150.0F, 12.0F, 0.4), 30, c -> {
         for (int k : new int[]{1, 2, 4, 6, 9, 14, 20}) {
            this.shot(s, c + k, "second_" + k);
         }

         s.add(c + 26, mc -> s.server(mc, p -> command(p, "gamemode creative")));
      });
      return this.take(s, t, 5, p -> wall(s, p), 2, p -> castAbility(p, "devilfruits:yomi_yomi/soul_projection", 1), rig(5.0, 70.0F, 10.0F, 0.4), 90, c -> {
         s.add(c - 1, mc -> VfxCapture.clip(this.prefix + "clip_projection", 130, 0.6666666666666666));
         flyNorth(s, c + 22, 26, 0.36);
         s.add(c + 52, mc -> s.server(mc, p -> castAbility(p, "devilfruits:yomi_yomi/soul_projection", 1)));
      });
   }

   private int fubuki(AutoTest s, int t) {
      t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:yomi_yomi/fubuki_giri", 1), rig(9.0, 125.0F, 16.0F, 0.6), 70, c -> {
         for (int k : new int[]{26, 29, 32, 36, 46, 60}) {
            this.shot(s, c + k, "fubuki_" + k);
         }
      });
      t = this.take(s, t, 8, none(), 0, p -> castAbility(p, "devilfruits:yomi_yomi/fubuki_giri", 1), rig(7.0, 165.0F, 22.0F, 0.4), 70, c -> {
         for (int k : new int[]{30, 36, 46}) {
            this.shot(s, c + k, "behind_" + k);
         }
      });
      return this.take(
         s,
         t,
         5,
         none(),
         0,
         p -> castAbility(p, "devilfruits:yomi_yomi/fubuki_giri", 1),
         rig(9.0, 125.0F, 16.0F, 0.6),
         96,
         c -> s.add(c - 1, mc -> VfxCapture.clip(this.prefix + "clip_fubuki", 140, 0.6666666666666666))
      );
   }

   public static enum Stage {
      M4,
      FUBUKI,
      M5,
      POLKA,
      KING,
      KING_LOOK,
      CANE,
      ICE,
      RUN,
      WATER;
   }
}
