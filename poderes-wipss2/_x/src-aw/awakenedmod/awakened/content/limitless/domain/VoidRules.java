package com.awakenedmod.awakened.content.limitless.domain;

import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.voidview.VoidZones;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;

@EventBusSubscriber(
   modid = "awakened"
)
public final class VoidRules {
   private static final List<ResourceKey<DamageType>> WORLD_HARM = List.of(
      DamageTypes.IN_WALL,
      DamageTypes.IN_FIRE,
      DamageTypes.ON_FIRE,
      DamageTypes.LAVA,
      DamageTypes.HOT_FLOOR,
      DamageTypes.CACTUS,
      DamageTypes.SWEET_BERRY_BUSH,
      DamageTypes.FREEZE,
      DamageTypes.DROWN,
      DamageTypes.LIGHTNING_BOLT,
      DamageTypes.FALL,
      DamageTypes.FLY_INTO_WALL,
      DamageTypes.STALAGMITE,
      DamageTypes.FALLING_BLOCK,
      DamageTypes.FALLING_ANVIL,
      DamageTypes.FALLING_STALACTITE,
      DamageTypes.CRAMMING
   );

   private VoidRules() {
   }

   private static boolean blockRefused(Player body, BlockPos pos) {
      Level level = body.level();
      return VoidBody.of(body).held() || VoidZones.any(level) && (VoidZones.inside(body) || VoidZones.inside(level, pos));
   }

   private static <E extends ICancellableEvent> void refuse(E event, boolean refused) {
      if (refused) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onLeftClickBlock(LeftClickBlock event) {
      refuse(event, blockRefused(event.getEntity(), event.getPos()));
   }

   @SubscribeEvent
   public static void onRightClickBlock(RightClickBlock event) {
      refuse(event, blockRefused(event.getEntity(), event.getPos()));
   }

   @SubscribeEvent
   public static void onBreak(BreakEvent event) {
      refuse(event, blockRefused(event.getPlayer(), event.getPos()));
   }

   @SubscribeEvent
   public static void onPlace(EntityPlaceEvent event) {
      if (event.getEntity() instanceof Player player) {
         refuse(event, blockRefused(player, event.getPos()));
      } else if (event.getLevel() instanceof Level level) {
         refuse(event, VoidZones.inside(level, event.getPos()));
      }
   }

   @SubscribeEvent
   public static void onBreakSpeed(BreakSpeed event) {
      if (event.getPosition().isPresent()) {
         refuse(event, blockRefused(event.getEntity(), (BlockPos)event.getPosition().get()));
      }
   }

   @SubscribeEvent
   public static void onRightClickItem(RightClickItem event) {
      refuse(event, VoidBody.of(event.getEntity()).held());
   }

   @SubscribeEvent
   public static void onEntityInteract(EntityInteract event) {
      refuse(event, VoidBody.of(event.getEntity()).held());
   }

   @SubscribeEvent
   public static void onEntityInteractSpecific(EntityInteractSpecific event) {
      refuse(event, VoidBody.of(event.getEntity()).held());
   }

   @SubscribeEvent
   public static void onAttack(AttackEntityEvent event) {
      refuse(event, VoidBody.of(event.getEntity()).held());
   }

   @SubscribeEvent
   public static void onProjectileImpact(ProjectileImpactEvent event) {
      HitResult hit = event.getRayTraceResult();
      if (hit instanceof BlockHitResult block && hit.getType() == Type.BLOCK) {
         Entity shot = event.getProjectile();
         refuse(event, VoidZones.inside(shot.level(), block.getBlockPos()));
      }
   }

   @SubscribeEvent
   public static void onBreathe(LivingBreatheEvent event) {
      if (VoidZones.inside(event.getEntity())) {
         event.setCanBreathe(true);
      }
   }

   @SubscribeEvent
   public static void onLightning(EntityStruckByLightningEvent event) {
      refuse(event, VoidZones.inside(event.getEntity()));
   }

   @SubscribeEvent
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      LivingEntity victim = event.getEntity();
      if (VoidZones.any(victim.level()) && VoidZones.inside(victim)) {
         DamageSource source = event.getSource();

         for (ResourceKey<DamageType> type : WORLD_HARM) {
            if (source.is(type)) {
               event.setCanceled(true);
               return;
            }
         }
      }
   }

   @SubscribeEvent
   public static void onDamaged(Post event) {
      LivingEntity victim = event.getEntity();
      if (!(event.getNewDamage() <= 0.0F) && victim.level() instanceof ServerLevel level && VoidZones.any(level) && VoidZones.inside(victim)) {
         VoidFx.hitSparks(level, victim.position().add(0.0, (double)victim.getBbHeight() * 0.6, 0.0));
         if (event.getSource().getEntity() instanceof ServerPlayer caster && VoidControl.caughtBy(caster, victim)) {
            MasterySystem.award(caster, MasteryReason.HIT, victim);
         }
      }
   }
}
