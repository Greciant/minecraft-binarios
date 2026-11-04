package com.devilfruits.fruits.gura_gura.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.fruits.gura_gura.GuraAbilities;
import com.devilfruits.fruits.gura_gura.GuraFx;
import com.devilfruits.fruits.gura_gura.GuraGura;
import com.devilfruits.fruits.gura_gura.GuraItems;
import com.devilfruits.fruits.gura_gura.entity.QuakeSlashEntity;
import com.devilfruits.registry.DFAttachments;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;

public class QuakeInfusionAbility extends Ability {
   public QuakeInfusionAbility() {
      super(
         GuraGura.id("quake_infusion"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(12)
            .cooldown(24)
            .mastery(3)
            .damage(9.0)
            .range(18.0)
            .knockback(1.0)
            .extra("slashSpeed", 1.3, 0.2, 5.0, "Travel speed of the crescent in blocks per tick.")
            .extra("slashWidth", 3.2, 0.5, 16.0, "Width of the crescent in blocks.")
            .extra("weaponDamageFactor", 0.8, 0.0, 10.0, "Fraction of the held weapon's attack damage added to the slash.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.DIRECTIONAL).force(0.8).blockDamage(1.0).manual().build();
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return player.getMainHandItem().is(GuraGura.QUAKE_WEAPONS) ? null : Component.translatable("message.devilfruits.gura_gura.needs_weapon");
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      Vec3 look = ctx.look();
      float weaponDamage = (float)Math.max(0.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE) - 1.0);
      float damage = ctx.damage() + weaponDamage * (float)ctx.extra("weaponDamageFactor");
      float mul = ctx.range() / (float)ctx.config().range();
      float roll = (float)(player.getRandom().nextBoolean() ? 1 : -1) * (0.25F + player.getRandom().nextFloat() * 0.45F);
      Vec3 start = ctx.eye().add(0.0, -0.35, 0.0).add(look.scale(1.1));
      ServerFx.animate(player, ctx.animation(GuraGura.anim("quake_slash")));
      player.swing(InteractionHand.MAIN_HAND, true);
      QuakeSlashEntity slash = new QuakeSlashEntity(
            ctx.level(), player, start, look, (float)ctx.extra("slashSpeed"), (float)ctx.extra("slashWidth") * mul, roll, damage, ctx.knockback(), ctx.range()
         )
         .withDestruction(GuraGura.destruction(ctx.mastery()));
      slash.setDeltaMovement(slash.getDeltaMovement().add(PhysicsWorld.carrierVelocity(player)));
      ctx.level().addFreshEntity(slash);
      ServerFx.send(player, GuraFx.SLASH_CAST, start, look, roll, ctx.seed(), 0);
   }

   public static void onRightClickItem(ServerPlayer player, RightClickItem event) {
      if (event.getHand() == InteractionHand.MAIN_HAND) {
         ItemStack stack = event.getItemStack();
         if (stack.is((Item)GuraItems.BISENTO.get()) || stack.is(ItemTags.SWORDS)) {
            PlayerFruitData data = DFAttachments.data(player);
            QuakeInfusionAbility ability = GuraAbilities.QUAKE_INFUSION;
            if (GuraGura.isUser(player)
               && !Weakness.isSuppressed(player)
               && data.mastery() >= ability.config().requiredMastery()
               && !data.isOnCooldown(ability.id(), player.level().getGameTime())
               && ability.config().enabled()) {
               if (AbilityManager.press(player, ability).succeeded()) {
                  event.setCanceled(true);
                  event.setCancellationResult(InteractionResult.SUCCESS);
               }
            }
         }
      }
   }
}
