package com.devilfruits.fruits.doru_doru.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.doru_doru.DoruAbilities;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.DoruFx;
import com.devilfruits.fruits.doru_doru.DoruItems;
import com.devilfruits.fruits.doru_doru.DoruSounds;
import com.devilfruits.fruits.doru_doru.coating.WaxCoating;
import com.devilfruits.fruits.doru_doru.entity.WaxLanceEntity;
import com.devilfruits.fruits.doru_doru.item.WaxTools;
import java.util.Comparator;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

public class WaxLanceAbility extends Ability {
   public WaxLanceAbility() {
      super(
         DoruDoru.id("wax_lance"),
         CastType.CHARGED,
         AbilityStats.builder()
            .energy(12)
            .cooldown(30)
            .mastery(5)
            .damage(9.0)
            .range(6.0)
            .knockback(1.4)
            .extra("throwChargeTicks", 8.0, 1.0, 100.0, "Hold at least this long (ticks) to throw instead of thrusting.")
            .extra("chargeTicks", 20.0, 2.0, 200.0, "Ticks to a full wind-up.")
            .extra("pierceAtMastery5", 2.0, 1.0, 32.0, "Enemies one thrust runs through at mastery 5.")
            .extra("pierceAtMastery10", 5.0, 1.0, 32.0, "Enemies one thrust runs through at mastery 10.")
            .extra("throwSpeed", 2.6, 0.5, 6.0, "Speed of the thrown lance (blocks per tick).")
            .extra("pegLifetimeSeconds", 60.0, 1.0, 3600.0, "Seconds a lance stuck in a wall stays as a wax peg.")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Override
   public int maxChargeTicks() {
      return (int)this.config().extra("chargeTicks");
   }

   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return DoruDoru.heatBlock(player);
   }

   @Override
   public void onChargeStart(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ItemStack held = player.getMainHandItem();
      if (WaxTools.isWaxTool(held)) {
         WaxTools.dissolve(player, held, player.getInventory().selected, false);
      }

      WaxTools.bind(player, new ItemStack((ItemLike)DoruItems.WAX_LANCE.get()), DoruAbilities.color(player));
      player.level().playSound(null, player.getX(), player.getY(), player.getZ(), DoruSounds.LANCE_FORM.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
      ServerFx.send(player, DoruFx.LANCE_FORM, player.getEyePosition(), ctx.look(), 1.0F, ctx.seed(), DoruAbilities.color(player));
      ServerFx.animate(player, ctx.animation(DoruDoru.id("lance_charge")));
   }

   @Override
   public void onChargeCancel(ServerPlayer player) {
      dropLance(player);
      ServerFx.stopAnimation(player);
   }

   @Override
   public void release(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      int held = Math.round(ctx.charge() * (float)this.maxChargeTicks());
      Vec3 look = ctx.look();
      Vec3 hand = player.getEyePosition().add(look.scale(0.6)).add(0.0, -0.3, 0.0);
      int dye = DoruAbilities.color(player);
      if ((double)held >= ctx.extra("throwChargeTicks")) {
         dropLance(player);
         float power = 0.75F + 0.25F * ctx.charge();
         Vec3 velocity = ctx.aimFrom(hand).scale(ctx.extra("throwSpeed") * (double)power);
         WaxLanceEntity.shoot(player, hand, velocity, ctx.damage() * power, ctx.knockback(), (int)(ctx.extra("pegLifetimeSeconds") * 20.0), dye);
         player.level().playSound(null, player.getX(), player.getY(), player.getZ(), DoruSounds.LANCE_THROW.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
         ServerFx.send(player, DoruFx.LANCE_THROW, hand, velocity, power, ctx.seed(), dye);
         ServerFx.animate(player, ctx.animation(DoruDoru.id("lance_throw")));
      } else {
         double reach = (double)ctx.range();
         Vec3 end = hand.add(ctx.aimFrom(hand).scale(reach));
         List<Entity> hits = Targeting.inCapsule(ctx.level(), player, hand, end, 0.75);
         hits.sort(Comparator.comparingDouble(ex -> ex.distanceToSqr(hand)));
         double t = Math.max(0.0, Math.min(1.0, (double)(ctx.mastery() - 5) / 5.0));
         int pierce = (int)Math.round(ctx.extra("pierceAtMastery5") + (ctx.extra("pierceAtMastery10") - ctx.extra("pierceAtMastery5")) * t);
         int n = 0;

         for (Entity e : hits) {
            if (n >= pierce) {
               break;
            }

            if (Targeting.canHurt(player, e)) {
               ctx.hurt(e, ctx.damage());
               Targeting.launch(e, look.multiply(1.0, 0.0, 1.0).normalize().scale((double)ctx.knockback()).add(0.0, 0.35, 0.0));
               WaxCoating.add(e, 1, player);
               ServerFx.sendNear(
                  ctx.level(),
                  DoruFx.LANCE_HIT,
                  e.getId(),
                  e.position().add(0.0, (double)e.getBbHeight() * 0.6, 0.0),
                  look,
                  1.0F,
                  ctx.seed() + (long)n,
                  dye,
                  64.0
               );
               n++;
            }
         }

         player.level().playSound(null, player.getX(), player.getY(), player.getZ(), DoruSounds.LANCE_THRUST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
         ServerFx.send(player, DoruFx.LANCE_THRUST, hand, end.subtract(hand), (float)n, ctx.seed(), dye);
         ServerFx.animate(player, ctx.animation(DoruDoru.id("lance_thrust")));
         ServerScheduler.later(ctx.level(), 8, () -> dropLance(player));
      }
   }

   private static void dropLance(ServerPlayer player) {
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack s = player.getInventory().getItem(i);
         if (s.is((Item)DoruItems.WAX_LANCE.get())) {
            WaxTools.dissolve(player, s, i, false);
         }
      }
   }
}
