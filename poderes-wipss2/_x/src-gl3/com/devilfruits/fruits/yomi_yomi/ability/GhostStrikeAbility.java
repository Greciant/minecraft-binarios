package com.devilfruits.fruits.yomi_yomi.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.combat.Stun;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.yomi_yomi.YomiFx;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class GhostStrikeAbility extends KingAbility {
   private static final double[] HALF_ANGLE = new double[]{85.0, 55.0, 42.0};

   public GhostStrikeAbility() {
      super(
         "king_strike",
         CastType.COMBO,
         AbilityStats.builder()
            .energy(0)
            .cooldown(16)
            .mastery(9)
            .damage(9.0)
            .range(3.0)
            .knockback(1.1)
            .extra("pinTicks", 16.0, 0.0, 100.0, "Ticks the second blow pins what it slams to the ground.")
            .extra("pushSpeed", 0.75, 0.0, 3.0, "Speed the push throws what it strikes away with (about 5-6 blocks).")
            .build()
      );
   }

   @Override
   public int maxComboSteps() {
      return 3;
   }

   @Override
   public int comboWindowTicks() {
      return 34;
   }

   @Override
   public void comboStep(AbilityContext ctx, int step) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      int hitAt = YomiTiming.KING_STRIKE_HIT[step];
      AbilityManager.setBusy(player, hitAt + 4);
      ServerFx.send(player, YomiFx.KING_STRIKE, player.position(), ctx.horizontalLook(), ctx.range(), ctx.seed(), step);
      ServerScheduler.schedule(
         level,
         0,
         age -> {
            if (!present(player, level)) {
               return true;
            } else {
               if (age == hitAt - 3) {
                  level.playSound(
                     null, player.getX(), player.getY() + 2.5, player.getZ(), YomiSounds.KING_SWING.get(), SoundSource.PLAYERS, 1.0F, step == 2 ? 0.8F : 1.0F
                  );
               } else if (age == hitAt) {
                  strike(ctx, step);
               }

               return age >= hitAt;
            }
         }
      );
   }

   public static List<Entity> strike(AbilityContext ctx, int step) {
      ServerPlayer player = ctx.player();
      Vec3 dir = ctx.horizontalLook();
      Vec3 right = new Vec3(-dir.z, 0.0, dir.x);
      List<Entity> hit = reach(player, dir, (double)ctx.range(), step);
      float damage = step == 2 ? ctx.damage() * 2.0F : ctx.damage();

      for (Entity e : hit) {
         e.invulnerableTime = 0;
         breakShield(e);
         if (ctx.hurt(e, damage)) {
            Vec3 out = away(player.position(), e, dir);

            Vec3 fling = switch (step) {
               case 0 -> right.scale(0.85).add(out.scale(0.45)).normalize().scale((double)ctx.knockback()).add(0.0, 0.35, 0.0);
               case 1 -> new Vec3(0.0, -1.2, 0.0);
               default -> out.scale(ctx.extra("pushSpeed")).add(0.0, 0.42, 0.0);
            };
            Targeting.launch(e, fling);
            if (step == 1 && e instanceof LivingEntity living) {
               Stun.apply(living, (int)ctx.extra("pinTicks"), false);
            }

            ServerFx.send(e, YomiFx.KING_HIT, e.getBoundingBox().getCenter(), fling, damage, ctx.seed() + (long)e.getId(), step);
         }
      }

      ServerLevel level = ctx.level();
      if (!hit.isEmpty()) {
         level.playSound(
            null, player.getX(), player.getY() + 1.0, player.getZ(), YomiSounds.KING_HIT.get(), SoundSource.PLAYERS, 1.1F, step == 2 ? 0.75F : 0.95F
         );
      }

      return hit;
   }

   public static List<Entity> reach(ServerPlayer player, Vec3 dir, double range, int step) {
      Vec3 feet = player.position();
      double cos = Math.cos(Math.toRadians(HALF_ANGLE[step]));
      List<Entity> out = new ArrayList<>();

      for (Entity e : Targeting.inRadius(player.serverLevel(), feet.add(0.0, 1.0, 0.0), range + 2.5, x -> x != player && Targeting.canHurt(player, x))) {
         Vec3 c = e.getBoundingBox().getCenter();
         Vec3 flat = new Vec3(c.x - feet.x, 0.0, c.z - feet.z);
         double d = flat.length() - (double)e.getBbWidth() * 0.5;
         if (!(d > range) && !(c.y < feet.y - 1.5) && !(c.y > feet.y + 4.5) && (d < 1.0 || flat.normalize().dot(dir) >= cos)) {
            out.add(e);
         }
      }

      return out;
   }
}
