package com.devilfruits.core.physics;

import com.devilfruits.core.haki.HakiCombat;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.TerrainDestruction;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

@EventBusSubscriber(
   modid = "devilfruits"
)
public final class PhysicsEvents {
   private static final List<ToDoubleFunction<ServerPlayer>> EMPOWERMENTS = new CopyOnWriteArrayList<>();

   public static void registerEmpowerment(ToDoubleFunction<ServerPlayer> factor) {
      EMPOWERMENTS.add(factor);
   }

   @SubscribeEvent
   public static void onServerTick(Post event) {
      if (PhysicsWorld.bridge().active()) {
         for (ServerLevel level : event.getServer().getAllLevels()) {
            BlockImpactService.flush(level);
         }

         long time = event.getServer().overworld().getGameTime();
         if (time % 20L == 0L) {
            StructureForces.cleanup(time);
         }
      }
   }

   @SubscribeEvent
   public static void onStopped(ServerStoppedEvent event) {
      BlockImpactService.clear();
      StructureForces.clear();
   }

   @SubscribeEvent
   public static void onJump(LivingJumpEvent event) {
      LivingEntity e = event.getEntity();
      if (!e.level().isClientSide() && PhysicsWorld.active() && (Boolean)PhysicsConfig.PLAIN_JUMPS.get()) {
         Vec3 v = e.getDeltaMovement();
         StructureForces.reaction(e, new Vec3(v.x * 0.3, Math.max(0.0, v.y), v.z * 0.3), 0.6);
      }
   }

   @SubscribeEvent
   public static void onFall(LivingFallEvent event) {
      LivingEntity e = event.getEntity();
      if (!e.level().isClientSide() && PhysicsWorld.active()) {
         StructureForces.landing(e, (double)event.getDistance());
      }
   }

   @SubscribeEvent
   public static void onPunchBlock(LeftClickBlock event) {
      if (event.getEntity() instanceof ServerPlayer player
         && event.getAction() == Action.START
         && PhysicsWorld.active()
         && (Boolean)PhysicsConfig.EMPOWERED_MELEE.get()) {
         BlockPos pos = event.getPos();
         PhysicsStructure s = PhysicsWorld.structureAt(player.level(), pos);
         if (s != null && BlockImpactService.allowed(player, s)) {
            boolean coated = HakiCombat.coatedHand(player);
            double factor = coated ? 1.0 : 0.0;

            for (ToDoubleFunction<ServerPlayer> f : EMPOWERMENTS) {
               factor = Math.max(factor, f.applyAsDouble(player));
            }

            if (!(factor <= 0.0) && PhysicsConfig.enabledFor(null)) {
               Vec3 point = s.toWorld(Vec3.atCenterOf(pos));
               Vec3 look = player.getLookAngle();
               double strength = 40.0 * (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5 + 2.4000000000000004) * factor;
               StructureBlock hit = new StructureBlock(s, pos, player.level().getBlockState(pos), point, 1.0);
               StructureForces.strike(player, s, List.of(hit), ImpactShape.sphere(point, 1.0), look, PhysicsProfile.Impact.DIRECTIONAL, strength);
               if (coated
                  && TerrainDestruction.destructionAllowed(player.serverLevel())
                  && (Boolean)PhysicsConfig.BLOCK_DAMAGE.get()
                  && player.getRandom().nextDouble() < (Double)PhysicsConfig.ARMAMENT_BLOCK_BONUS.get() * (Double)PhysicsConfig.BLOCK_DAMAGE_MULTIPLIER.get()) {
                  BlockImpactService.queueBreak(player.serverLevel(), player, s, pos, look.scale(0.3), false, new CastBudget(1, 1));
               }

               if (coated) {
                  BlockImpactService.decal(player.serverLevel(), s, pos, s.dirToLocal(look.scale(-1.0)), BlockImpactService.Decal.HAKI, 0.9F);
               }

               return;
            }

            return;
         }

         return;
      }
   }

   private PhysicsEvents() {
   }
}
