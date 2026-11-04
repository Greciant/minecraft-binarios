package com.devilfruits.fruits.doru_doru;

import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.fruits.doru_doru.ability.CandleServiceAbility;
import com.devilfruits.fruits.doru_doru.ability.CandleWallAbility;
import com.devilfruits.fruits.doru_doru.ability.ChampionForm;
import com.devilfruits.fruits.doru_doru.ability.WaxToolsAbility;
import com.devilfruits.fruits.doru_doru.atelier.BuildEngine;
import com.devilfruits.fruits.doru_doru.atelier.UndoHistory;
import com.devilfruits.fruits.doru_doru.coating.WaxCoating;
import com.devilfruits.fruits.doru_doru.item.WaxTools;
import com.devilfruits.fruits.doru_doru.wax.WaxHeatService;
import com.devilfruits.fruits.doru_doru.wax.WaxIntegrityHooks;
import com.devilfruits.registry.DFAttachments;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;

public final class DoruEvents {
   private static final Map<UUID, Long> WARNED = new HashMap<>();

   static void register(IEventBus bus) {
      bus.addListener(DoruEvents::onLevelTick);
      bus.addListener(DoruEvents::onServerTick);
      bus.addListener(DoruEvents::onNeighborNotify);
      bus.addListener(DoruEvents::onEntityJoin);
      bus.addListener(DoruEvents::onProjectileImpact);
      bus.addListener(DoruEvents::onRightClickItem);
      bus.addListener(DoruEvents::onEntityTick);
      bus.addListener(DoruEvents::onPlayerTick);
      bus.addListener(DoruEvents::onChangedDimension);
      bus.addListener(DoruEvents::onLevelUnload);
      bus.addListener(DoruEvents::onLivingDamage);
      bus.addListener(DoruEvents::onContainerOpen);
      bus.addListener(DoruEvents::onDeath);
      bus.addListener(DoruEvents::onToolAttributes);
      bus.addListener(ChampionForm::onFall);
      bus.addListener(e -> {
         WARNED.remove(e.getEntity().getUUID());
         UndoHistory.forget(e.getEntity().getUUID());
         DoruAbilities.forget(e.getEntity().getUUID());
         WaxToolsAbility.forget(e.getEntity().getUUID());
         CandleWallAbility.forget(e.getEntity().getUUID());
         ChampionForm.forget(e.getEntity().getUUID());
         WaxCoating.forget(e.getEntity().getUUID());
         if (e.getEntity() instanceof ServerPlayer p) {
            CandleServiceAbility.forget(p);
            BuildEngine.cancel(p, false);
            WaxTools.dissolveAll(p, false);
         }
      });
      bus.addListener(e -> {
         BuildEngine.clear();
         CandleServiceAbility.clear();
      });
   }

   private static void onLevelTick(Post event) {
      if (event.getLevel() instanceof ServerLevel level) {
         WaxHeatService.tick(level);
         WaxIntegrityHooks.tick(level);
         CandleServiceAbility.tick(level);
      }
   }

   private static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
      BuildEngine.tick(event.getServer());
   }

   private static void onNeighborNotify(NeighborNotifyEvent event) {
      if (event.getLevel() instanceof ServerLevel level && WaxHeatService.sourceWeight(event.getState()) > 0.0F) {
         WaxHeatService.pokeAround(level, event.getPos(), 4);
      }
   }

   private static void onEntityJoin(EntityJoinLevelEvent event) {
      if (event.getEntity() instanceof LightningBolt bolt && event.getLevel() instanceof ServerLevel level) {
         WaxHeatService.heatBurst(level, bolt.position(), 2.5F, 40);
      }
   }

   private static void onProjectileImpact(ProjectileImpactEvent event) {
      Projectile p = event.getProjectile();
      if (p.level() instanceof ServerLevel level && event.getRayTraceResult() instanceof BlockHitResult hit && hit.getType() != Type.MISS) {
         float weight = (!(p instanceof AbstractHurtingProjectile hurting) || !hurting.isOnFire()) && !(p instanceof Fireball)
            ? (p instanceof AbstractArrow && p.isOnFire() ? 0.5F : 0.0F)
            : 1.2F;
         if (weight > 0.0F) {
            WaxHeatService.heatBurst(level, hit.getLocation(), weight, 30);
         }

         return;
      }
   }

   private static void onRightClickItem(RightClickItem event) {
      if (event.getEntity() instanceof ServerPlayer player && DoruDoru.canWax(player)) {
         ItemStack stack = event.getItemStack();
         PlayerFruitData data = DFAttachments.data(player);
         double gain;
         if (stack.is(Items.HONEYCOMB)) {
            gain = DoruDoru.setting("honeycombEnergy");
         } else if (stack.is(ItemTags.CANDLES)) {
            gain = DoruDoru.setting("candleEnergy");
         } else {
            if (!stack.is((Item)DoruItems.WAX_LUMP.get())) {
               return;
            }

            gain = DoruDoru.setting("lumpRefundShare") * DoruDoru.blockCost(data.mastery()) / 4.0;
         }

         float max = (float)Mastery.maxEnergy(data);
         if (!(gain <= 0.0) && !(data.energy() >= max - 0.01F)) {
            data.setEnergy((float)Math.min((double)max, (double)data.energy() + gain));
            if (!player.isCreative()) {
               stack.shrink(1);
            }

            Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(0.4)).add(0.0, -0.25, 0.0);
            player.level()
               .playSound(
                  null,
                  player.getX(),
                  player.getY(),
                  player.getZ(),
                  DoruSounds.ABSORB.get(),
                  SoundSource.PLAYERS,
                  0.8F,
                  0.9F + player.getRandom().nextFloat() * 0.2F
               );
            ServerFx.send(player, DoruFx.ABSORB, at, Vec3.ZERO, (float)gain, player.getRandom().nextLong(), 0);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
         }

         return;
      }
   }

   private static void onEntityTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
      if (event.getEntity() instanceof LivingEntity living && living.hasData(WaxCoating.TYPE) && !living.level().isClientSide()) {
         WaxCoating.tick(living);
      }

      if (event.getEntity() instanceof ItemEntity item
         && item.level() instanceof ServerLevel level
         && item.getAge() > 60
         && item.getAge() % 10 == 0
         && isWax(item.getItem())
         && WaxHeatService.tooHot(level)) {
         level.sendParticles(
            new ItemParticleOption(ParticleTypes.ITEM, item.getItem().copyWithCount(1)), item.getX(), item.getY() + 0.1, item.getZ(), 6, 0.1, 0.05, 0.1, 0.02
         );
         level.sendParticles(ParticleTypes.WHITE_SMOKE, item.getX(), item.getY() + 0.2, item.getZ(), 3, 0.1, 0.05, 0.1, 0.01);
         level.playSound(null, item.getX(), item.getY(), item.getZ(), DoruSounds.MELT.get(), SoundSource.BLOCKS, 0.5F, 1.2F);
         item.discard();
      }
   }

   public static boolean isWax(ItemStack stack) {
      return stack.is((Item)DoruItems.WAX_LUMP.get());
   }

   private static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
      if (event.getEntity() instanceof ServerPlayer p) {
         ChampionForm.tick(p);
      }

      if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0 && WaxHeatService.tooHot(player.level())) {
         if (DoruDoru.isUser(player) && !Weakness.isSuppressed(player)) {
            long now = player.level().getGameTime();
            Long last = WARNED.get(player.getUUID());
            if (last == null || now - last > 600L) {
               warn(player);
            }
         }

         Inventory inv = player.getInventory();

         for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isWax(stack)) {
               stack.shrink(Math.max(1, stack.getCount() / 2));
               player.level().playSound(null, player.getX(), player.getY(), player.getZ(), DoruSounds.MELT.get(), SoundSource.PLAYERS, 0.4F, 1.3F);
            }
         }

         return;
      }
   }

   private static void onLivingDamage(LivingIncomingDamageEvent event) {
      LivingEntity victim = event.getEntity();
      if (!victim.level().isClientSide()) {
         WaxCoating.onDamage(victim, event.getSource());
         ChampionForm.onDamage(event);
         if (event.getSource().getEntity() instanceof LivingEntity attacker && WaxCoating.get(attacker).encased(attacker.level().getGameTime())) {
            event.setCanceled(true);
            if (attacker instanceof ServerPlayer p) {
               WaxCoating.struggle(p);
            }
         }
      }
   }

   private static void onContainerOpen(Open event) {
      if (event.getEntity() instanceof ServerPlayer p) {
         WaxTools.dissolveAll(p, false);
      }
   }

   private static void onDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer p) {
         WaxTools.dissolveAll(p, false);
      }
   }

   private static void onToolAttributes(ItemAttributeModifierEvent event) {
      if (WaxTools.isWaxTool(event.getItemStack())
         && !event.getItemStack().is((Item)DoruItems.WAX_LANCE.get())
         && !event.getItemStack().is((Item)DoruItems.WAX_KEY.get())
         && DoruDoru.fruit() != null) {
         double bonus = DoruAbilities.TOOLS.config().extra("toolDamageBonus");
         if (bonus != 0.0) {
            event.addModifier(
               Attributes.ATTACK_DAMAGE, new AttributeModifier(DoruDoru.id("tool_bonus"), bonus, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND
            );
         }
      }
   }

   private static void onChangedDimension(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && DoruDoru.isUser(player) && WaxHeatService.tooHot(player.level())) {
         warn(player);
      }
   }

   private static void warn(ServerPlayer player) {
      WARNED.put(player.getUUID(), player.level().getGameTime());
      player.displayClientMessage(Component.translatable("message.devilfruits.doru_doru.too_hot").withStyle(ChatFormatting.GOLD), true);
      ServerFx.send(player, DoruFx.HEAT_WARNING, player.position(), Vec3.ZERO, 1.0F, 0L, 0);
   }

   private static void onLevelUnload(Unload event) {
      if (event.getLevel() instanceof ServerLevel level) {
         WaxHeatService.clear(level);
         WaxIntegrityHooks.clear(level);
      }
   }

   private DoruEvents() {
   }
}
