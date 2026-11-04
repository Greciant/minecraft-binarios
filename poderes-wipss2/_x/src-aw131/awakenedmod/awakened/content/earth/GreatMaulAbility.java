package com.awakenedmod.awakened.content.earth;

import com.awakenedmod.awakened.content.vfx.BlockFxEntity;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GreatMaulAbility extends Ability {
   private static final double RANGE = 14.0;
   private static final float CRUSH_BASE = 22.0F;
   private static final float SPLASH_DAMAGE = 10.0F;
   private static final double SPLASH_RADIUS = 3.5;
   private static final float GRIND_DAMAGE = 7.0F;
   private static final double RAISE_HEIGHT = 10.0;
   private static final int RAISE_TICKS = 14;
   private static final int SMASH_TICKS = 9;
   private static final int CONNECT = 23;
   private static final int HOLD_TICKS = 18;
   private static final int END = 41;
   private static final int BOX_LIFE = 56;
   private static final double MAUL_REST = 1.7;
   private static final double HEAD_HALF = 1.6;
   private static final int ROOT_TICKS = 24;
   private static final GreatMaulAbility.MaulBox[] MAUL = new GreatMaulAbility.MaulBox[]{
      new GreatMaulAbility.MaulBox(0.0, -1.35, 0.0, 6.4F, 0.5F, 4.2F, 0.0F, Blocks.POLISHED_DEEPSLATE),
      new GreatMaulAbility.MaulBox(0.0, 0.15, 0.0, 3.2F, 2.5F, 4.0F, 0.0F, Blocks.COBBLED_DEEPSLATE),
      new GreatMaulAbility.MaulBox(2.0, 0.0, 0.0, 1.4F, 2.2F, 3.6F, 0.0F, Blocks.COBBLED_DEEPSLATE),
      new GreatMaulAbility.MaulBox(-2.0, 0.0, 0.0, 1.4F, 2.2F, 3.6F, 0.0F, Blocks.COBBLED_DEEPSLATE),
      new GreatMaulAbility.MaulBox(2.95, 0.0, 0.0, 0.9F, 2.6F, 4.0F, 0.0F, Blocks.CRACKED_DEEPSLATE_BRICKS),
      new GreatMaulAbility.MaulBox(2.8, 1.35, 0.0, 1.4F, 0.5F, 4.2F, 0.0F, Blocks.POLISHED_DEEPSLATE),
      new GreatMaulAbility.MaulBox(-2.95, 0.0, 0.0, 1.2F, 2.0F, 3.0F, 0.0F, Blocks.BLACKSTONE),
      new GreatMaulAbility.MaulBox(-3.65, 0.05, 0.0, 0.9F, 1.4F, 1.8F, 0.0F, Blocks.BLACKSTONE),
      new GreatMaulAbility.MaulBox(-4.2, 0.1, 0.0, 0.9F, 1.0F, 1.0F, 22.0F, Blocks.POLISHED_BLACKSTONE),
      new GreatMaulAbility.MaulBox(0.0, 1.7, 0.0, 2.4F, 0.6F, 2.4F, 0.0F, Blocks.RAW_IRON_BLOCK),
      new GreatMaulAbility.MaulBox(0.0, 3.0, 0.0, 1.6F, 3.2F, 1.6F, 0.0F, Blocks.STONE),
      new GreatMaulAbility.MaulBox(0.0, 6.0, 0.0, 1.2F, 2.8F, 1.2F, 0.0F, Blocks.STONE),
      new GreatMaulAbility.MaulBox(0.0, 2.6, 0.0, 1.8F, 0.45F, 1.8F, 0.0F, Blocks.RAW_IRON_BLOCK),
      new GreatMaulAbility.MaulBox(0.0, 4.6, 0.0, 1.7F, 0.5F, 1.7F, 0.0F, Blocks.RAW_IRON_BLOCK),
      new GreatMaulAbility.MaulBox(0.0, 6.6, 0.0, 1.35F, 0.45F, 1.35F, 0.0F, Blocks.RAW_IRON_BLOCK),
      new GreatMaulAbility.MaulBox(0.0, 7.9, 0.0, 1.9F, 1.0F, 1.9F, 0.0F, Blocks.RAW_IRON_BLOCK),
      new GreatMaulAbility.MaulBox(0.0, 8.7, 0.0, 1.1F, 0.6F, 1.1F, 45.0F, Blocks.IRON_BLOCK),
      new GreatMaulAbility.MaulBox(2.45, 1.05, -1.55, 1.1F, 1.0F, 1.1F, 45.0F, Blocks.DEEPSLATE),
      new GreatMaulAbility.MaulBox(-2.45, 1.05, -1.55, 1.1F, 1.0F, 1.1F, 45.0F, Blocks.DEEPSLATE),
      new GreatMaulAbility.MaulBox(0.6, -0.55, -2.05, 1.7F, 1.1F, 0.7F, 22.0F, null),
      new GreatMaulAbility.MaulBox(-1.3, 1.5, -0.7, 1.4F, 0.9F, 1.3F, 22.0F, null)
   };
   private final Map<UUID, GreatMaulAbility.Maul> mauls = this.playerState();

   public GreatMaulAbility() {
      super("great_maul", 40.0F, 7);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().range(14.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      double range = (double)MasteryScaling.scaled(14.0F, player);
      LivingEntity foe = AimUtil.aimedLivingSoft(player, range, 10.0, e -> e != player && Harm.allowed(player, e));
      if (foe == null) {
         return false;
      } else if (!Harm.controls(player, foe)) {
         return this.fail(player, "awakened.feedback.pvp_control_off", new Object[0]);
      } else {
         EarthMaterial mat = EarthMaterial.under(foe);
         Vec3 fwd = flatLook(player);
         Vec3 right = new Vec3(-fwd.z, 0.0, fwd.x);
         float yaw = (float)Math.toDegrees(Math.atan2(fwd.x, fwd.z));
         Vec3 t = new Vec3(foe.getX(), foe.getY(), foe.getZ());
         Vec3 headCenter = t.add(0.0, 10.0, 0.0);
         BlockFxEntity[] boxes = new BlockFxEntity[MAUL.length];
         Vec3[] worldOff = new Vec3[MAUL.length];

         for (int i = 0; i < MAUL.length; i++) {
            GreatMaulAbility.MaulBox mb = MAUL[i];
            worldOff[i] = right.scale(mb.r()).add(0.0, mb.u(), 0.0).add(fwd.scale(mb.f()));
            Block boxBlock = mb.block() != null ? mb.block() : echoBlock(mat);
            boxes[i] = Vfx.blockTracked(level, headCenter.add(worldOff[i]), boxBlock, (double)mb.sx(), (double)mb.sy(), (double)mb.sz(), 56, yaw + mb.yawOff());
         }

         EarthFx.maulForge(level, t, headCenter, mat);
         refreshRoot(player, foe);
         this.mauls.put(player.getUUID(), new GreatMaulAbility.Maul(boxes, worldOff, t, foe, mat));
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      GreatMaulAbility.Maul c = this.mauls.get(player.getUUID());
      if (c != null) {
         ServerLevel level = player.serverLevel();
         if (c.foe != null && c.foe.isAlive() && !c.foe.isRemoved()) {
            if (c.step < 14) {
               c.t = new Vec3(c.foe.getX(), c.foe.getY(), c.foe.getZ());
               reposition(c, c.t.add(0.0, 10.0, 0.0));
               refreshRoot(player, c.foe);
               EarthFx.maulLoom(level, new Vec3(c.t.x, c.t.y + 10.0 - 1.6, c.t.z), c.mat);
               if (c.step == 13) {
                  EarthFx.maulTell(level, new Vec3(c.t.x, c.t.y + 10.0 - 1.6, c.t.z), c.mat);
               }
            } else if (c.step < 23) {
               if (c.step == 14) {
                  c.t = new Vec3(c.foe.getX(), c.foe.getY(), c.foe.getZ());
               }

               double p = (double)(c.step - 14 + 1) / 9.0;
               double y = c.t.y + 10.0 - 8.3 * Math.pow(p, 1.7);
               reposition(c, new Vec3(c.t.x, y, c.t.z));
               refreshRoot(player, c.foe);
               EarthFx.maulFall(level, new Vec3(c.t.x, y - 1.6, c.t.z), c.mat);
            } else if (c.step == 23) {
               this.snapAndCrush(player, c);
            } else {
               if (c.step >= 41) {
                  this.teardown(c, player, true);
                  return;
               }

               clampFoe(player, c);
               reposition(c, new Vec3(c.t.x, c.t.y + 1.7, c.t.z));
               if (c.step <= 25) {
                  EarthFx.maulSettle(level, c.t, c.mat);
               }

               if (c.step == 32) {
                  if (c.foe.isAlive()) {
                     boolean dealt = Dmg.hurt(
                        c.foe, level, Dmg.by(player, DamageTypes.FALLING_BLOCK), MasteryScaling.scaledDamage(7.0F, player, this.balanceKey())
                     );
                     if (dealt) {
                        MasterySystem.award(player, MasteryReason.HIT, c.foe);
                     }
                  }

                  EarthFx.maulPress(level, c.t, c.mat);
               }
            }

            c.step++;
         } else {
            if (crushed(player, c)) {
               QuestSystem.feat(player, "geo_crush");
            }

            this.teardown(c, player, false);
         }
      }
   }

   private static boolean crushed(ServerPlayer caster, GreatMaulAbility.Maul c) {
      return c.step <= 23 ? false : Foes.defeatedBy(c.foe, caster);
   }

   private void snapAndCrush(ServerPlayer player, GreatMaulAbility.Maul c) {
      ServerLevel level = player.serverLevel();
      EarthFx.maulSlam(level, c.t, c.mat);
      EarthFx.maulMaterial(level, c.t, c.mat);
      if (c.foe.isAlive()) {
         clampFoe(player, c);
         if (!(c.foe instanceof Player)) {
            TickFreeze.freeze(c.foe);
            c.frozen = true;
         }

         boolean dealt = Dmg.hurt(
            c.foe, level, Dmg.by(player, DamageTypes.FALLING_BLOCK), MasteryScaling.scaledDamage(22.0F + c.mat.damage(), player, this.balanceKey())
         );
         if (dealt) {
            MasterySystem.award(player, MasteryReason.HIT, c.foe);
         }

         c.mat.stagger(player, c.foe);
      }

      for (LivingEntity e : level.getEntitiesOfClass(
         LivingEntity.class, new AABB(c.t, c.t).inflate(3.5), ex -> ex != player && ex != c.foe && ex.isAlive() && Harm.allowed(player, ex)
      )) {
         Dmg.hurt(e, level, Dmg.by(player, DamageTypes.FALLING_BLOCK), MasteryScaling.scaledDamage(10.0F, player, this.balanceKey()));
         c.mat.stagger(player, e);
      }
   }

   private static void clampFoe(ServerPlayer player, GreatMaulAbility.Maul c) {
      LivingEntity foe = c.foe;
      foe.setPos(c.t.x, c.t.y, c.t.z);
      foe.setDeltaMovement(Vec3.ZERO);
      foe.hurtMarked = true;
      foe.resetFallDistance();
      foe.setSprinting(false);
      if (foe instanceof Player) {
         Harm.effect(player, foe, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 250, false, false));
         Harm.effect(player, foe, new MobEffectInstance(MobEffects.WEAKNESS, 8, 4, false, false));
         Harm.effect(player, foe, new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 8, 4, false, false));
      }
   }

   private static void refreshRoot(ServerPlayer player, LivingEntity foe) {
      Harm.effect(player, foe, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24, 5));
      Harm.effect(player, foe, new MobEffectInstance(MobEffects.WEAKNESS, 24, 2));
   }

   private static void reposition(GreatMaulAbility.Maul c, Vec3 center) {
      for (int i = 0; i < c.boxes.length; i++) {
         BlockFxEntity box = c.boxes[i];
         if (box != null && !box.isRemoved()) {
            Vec3 p = center.add(c.worldOff[i]);
            box.setPos(p.x, p.y, p.z);
         }
      }
   }

   private void teardown(GreatMaulAbility.Maul c, ServerPlayer player, boolean crumble) {
      this.mauls.remove(player.getUUID());
      releaseAndDiscard(c);
      if (crumble) {
         EarthFx.maulCrumble(player.serverLevel(), c.t.add(0.0, 1.7, 0.0), c.mat);
      }
   }

   private static void releaseAndDiscard(GreatMaulAbility.Maul c) {
      if (c.frozen && !(c.foe instanceof Player)) {
         TickFreeze.release(c.foe);
      }

      for (BlockFxEntity box : c.boxes) {
         if (box != null && !box.isRemoved()) {
            box.discard();
         }
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      GreatMaulAbility.Maul c = this.mauls.remove(player.getUUID());
      if (c != null) {
         releaseAndDiscard(c);
      }

      super.clearPlayer(player);
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   private static Vec3 flatLook(ServerPlayer player) {
      Vec3 look = player.getLookAngle();
      Vec3 fwd = new Vec3(look.x, 0.0, look.z);
      return fwd.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : fwd.normalize();
   }

   private static Block echoBlock(EarthMaterial mat) {
      return mat.accent();
   }

   private static final class Maul {
      private final BlockFxEntity[] boxes;
      private final Vec3[] worldOff;
      private Vec3 t;
      private final LivingEntity foe;
      private final EarthMaterial mat;
      private int step;
      private boolean frozen;

      private Maul(BlockFxEntity[] boxes, Vec3[] worldOff, Vec3 t, LivingEntity foe, EarthMaterial mat) {
         this.boxes = boxes;
         this.worldOff = worldOff;
         this.t = t;
         this.foe = foe;
         this.mat = mat;
      }
   }

   private static record MaulBox(double r, double u, double f, float sx, float sy, float sz, float yawOff, Block block) {
   }
}
