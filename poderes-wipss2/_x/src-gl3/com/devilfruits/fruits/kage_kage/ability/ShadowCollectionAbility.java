package com.devilfruits.fruits.kage_kage.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.fruits.kage_kage.KageAim;
import com.devilfruits.fruits.kage_kage.KageFx;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.KageSounds;
import com.devilfruits.fruits.kage_kage.item.MoriaScissorsItem;
import com.devilfruits.fruits.kage_kage.shadow.Shadows;
import com.devilfruits.fruits.kage_kage.shadow.StolenShadow;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ShadowCollectionAbility extends Ability {
   private static final Map<UUID, Integer> LOCKED = new HashMap<>();
   private static final Set<UUID> SNIPPED = new HashSet<>();

   public ShadowCollectionAbility() {
      super(
         KageKage.id("shadow_collection"),
         CastType.HELD,
         AbilityStats.builder()
            .energy(25)
            .cooldown(100)
            .mastery(3)
            .damage(0.0)
            .range(5.0)
            .extra("castTicks", 40.0, 5.0, 200.0, "Ticks the snip takes (hold the key).")
            .extra("aimConeDegrees", 12.0, 0.0, 45.0, "Aim assist: half-angle of the cone round the crosshair the target may stand in.")
            .build()
      );
   }

   @Override
   public int maxChannelTicks() {
      return (int)this.config().extra("castTicks") + 2;
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      if (!(player.getMainHandItem().getItem() instanceof MoriaScissorsItem)) {
         return Component.translatable("message.devilfruits.kage_kage.needs_scissors");
      } else {
         LivingEntity target = KageAim.aimed(player, this.config().range(), this.config().extra("aimConeDegrees"));
         return (Component)(target == null ? Component.translatable("message.devilfruits.kage_kage.shadow.no_target") : Shadows.refusal(player, target));
      }
   }

   @Override
   public void onChannelStart(AbilityContext ctx) {
      LivingEntity target = KageAim.aimed(ctx.player(), (double)ctx.range(), ctx.extra("aimConeDegrees"));
      if (target != null) {
         LOCKED.put(ctx.player().getUUID(), target.getId());
         SNIPPED.remove(ctx.player().getUUID());
         ServerFx.send(ctx.player(), KageFx.COLLECT, target.position(), Vec3.ZERO, 1.0F, ctx.seed(), target.getId());
         ServerFx.animate(ctx.player(), ctx.animation(KageKage.id("snip_open")));
         ctx.level()
            .playSound(null, ctx.player().getX(), ctx.player().getY(), ctx.player().getZ(), KageSounds.SNIP_CHARGE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
      }
   }

   @Override
   public boolean channelTick(AbilityContext ctx, int ticks) {
      ServerPlayer user = ctx.player();
      Integer id = LOCKED.get(user.getUUID());
      if (!((id == null ? null : ctx.level().getEntity(id)) instanceof LivingEntity target)
         || !target.isAlive()
         || (double)target.distanceTo(user) > (double)ctx.range() + 1.5
         || !(user.getMainHandItem().getItem() instanceof MoriaScissorsItem)) {
         return false;
      }

      if (ticks < (int)ctx.extra("castTicks")) {
         return true;
      } else {
         StolenShadow shadow = Shadows.take(user, target);
         if (shadow == null) {
            Component why = Shadows.refusal(user, target);
            if (why != null) {
               user.displayClientMessage(why, true);
            }

            return false;
         } else {
            ctx.level().playSound(null, target.getX(), target.getY(), target.getZ(), KageSounds.SNIP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            SNIPPED.add(user.getUUID());
            ServerFx.send(user, KageFx.COLLECT_SNIP, target.position(), Vec3.ZERO, 1.0F, ctx.seed(), target.getId());
            ServerFx.animate(user, ctx.animation(KageKage.id("snip_cut")));
            ctx.level().playSound(null, user.getX(), user.getY(), user.getZ(), KageSounds.ABSORB.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            user.displayClientMessage(
               Component.translatable("message.devilfruits.kage_kage.shadow.taken", new Object[]{shadow.name(), Shadows.count(user)}), true
            );
            return false;
         }
      }
   }

   @Override
   public void onChannelEnd(AbilityContext ctx, int ticks, boolean interrupted) {
      Integer id = LOCKED.remove(ctx.player().getUUID());
      if (id != null && !SNIPPED.remove(ctx.player().getUUID())) {
         ServerFx.send(ctx.player(), KageFx.COLLECT_END, ctx.player().position(), Vec3.ZERO, 1.0F, 0L, id);
         ServerFx.stopAnimation(ctx.player());
      }
   }
}
