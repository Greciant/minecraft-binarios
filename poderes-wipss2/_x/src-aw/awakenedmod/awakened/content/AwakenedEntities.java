package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.content.aero.AeroCrescentEntity;
import com.awakenedmod.awakened.content.cryokinetic.GlacierEntity;
import com.awakenedmod.awakened.content.earth.HoverRockEntity;
import com.awakenedmod.awakened.content.earth.StoneConstructEntity;
import com.awakenedmod.awakened.content.earth.StoneMechEntity;
import com.awakenedmod.awakened.content.entity.ArbiterEntity;
import com.awakenedmod.awakened.content.entity.ArbiterSealEntity;
import com.awakenedmod.awakened.content.entity.ArbiterWhipEntity;
import com.awakenedmod.awakened.content.entity.EnforcerEntity;
import com.awakenedmod.awakened.content.entity.LeviathanEntity;
import com.awakenedmod.awakened.content.entity.OverseerEntity;
import com.awakenedmod.awakened.content.entity.StalkerEntity;
import com.awakenedmod.awakened.content.entity.SuppressorEntity;
import com.awakenedmod.awakened.content.entity.VerdictHookEntity;
import com.awakenedmod.awakened.content.entity.archevoker.ArchEvokerCloneEntity;
import com.awakenedmod.awakened.content.entity.archevoker.ArchEvokerEntity;
import com.awakenedmod.awakened.content.entity.archevoker.EvogatorEntity;
import com.awakenedmod.awakened.content.entity.archevoker.VexplodeEntity;
import com.awakenedmod.awakened.content.entity.duelist.VarkosEntity;
import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.awakenedmod.awakened.content.entity.sentinel.SentinelEntity;
import com.awakenedmod.awakened.content.exosavant.WarhammerThrowEntity;
import com.awakenedmod.awakened.content.graviton.EventHorizonEntity;
import com.awakenedmod.awakened.content.hydro.HydroLance;
import com.awakenedmod.awakened.content.limitless.blue.BlueOrbEntity;
import com.awakenedmod.awakened.content.limitless.domain.VoidAnchorEntity;
import com.awakenedmod.awakened.content.limitless.purple.PurpleSphereEntity;
import com.awakenedmod.awakened.content.limitless.red.RedBlastEntity;
import com.awakenedmod.awakened.content.reaper.SoulBoltEntity;
import com.awakenedmod.awakened.content.scaler.GripAnchorEntity;
import com.awakenedmod.awakened.content.shadow.ShadowBolt;
import com.awakenedmod.awakened.content.shadow.ShadowShapeEntity;
import com.awakenedmod.awakened.content.summoning.SoulOrbEntity;
import com.awakenedmod.awakened.content.sun.SunOrbEntity;
import com.awakenedmod.awakened.content.teleporter.AfterimageEntity;
import com.awakenedmod.awakened.content.velocity.SpeedEchoEntity;
import com.awakenedmod.awakened.content.vfx.BlockFxEntity;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.AwakenedThreat;
import com.awakenedmod.awakened.core.npc.NpcKits;
import com.awakenedmod.awakened.core.voidview.VoidOccluderEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AwakenedEntities {
   private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, "awakened");
   public static final Supplier<EntityType<KineticProjectile>> KINETIC_CHARGE = ENTITIES.register(
      "kinetic_charge",
      () -> Builder.of(KineticProjectile::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("kinetic_charge")
   );
   public static final Supplier<EntityType<KineticProjectile>> ICE_SHARD = ENTITIES.register(
      "ice_shard", () -> Builder.of(KineticProjectile::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("ice_shard")
   );
   public static final Supplier<EntityType<KineticProjectile>> EXO_MISSILE = ENTITIES.register(
      "exo_missile",
      () -> Builder.of(KineticProjectile::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("exo_missile")
   );
   public static final Supplier<EntityType<KineticProjectile>> WEB_SHOT = ENTITIES.register(
      "web_shot", () -> Builder.of(KineticProjectile::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("web_shot")
   );
   public static final Supplier<EntityType<KineticProjectile>> PYRO_FIREBALL = ENTITIES.register(
      "pyro_fireball",
      () -> Builder.of(KineticProjectile::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(2).build("pyro_fireball")
   );
   public static final Supplier<EntityType<HydroLance>> HYDRO_LANCE = ENTITIES.register(
      "hydro_lance",
      () -> Builder.of(HydroLance::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(6)
            .updateInterval(2)
            .setShouldReceiveVelocityUpdates(false)
            .build("hydro_lance")
   );
   public static final Supplier<EntityType<WarhammerThrowEntity>> EXO_WARHAMMER = ENTITIES.register(
      "exo_warhammer",
      () -> Builder.of(WarhammerThrowEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(2).build("exo_warhammer")
   );
   public static final Supplier<EntityType<ShadowBolt>> SHADOW_BOLT = ENTITIES.register(
      "shadow_bolt", () -> Builder.of(ShadowBolt::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(6).updateInterval(10).build("shadow_bolt")
   );
   public static final Supplier<EntityType<ShadowShapeEntity>> SHADOW_SHAPE = ENTITIES.register(
      "shadow_shape",
      () -> Builder.of(ShadowShapeEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1).build("shadow_shape")
   );
   public static final Supplier<EntityType<SuppressorEntity>> SUPPRESSOR = ENTITIES.register(
      "suppressor", () -> Builder.of(SuppressorEntity::new, MobCategory.MONSTER).sized(0.9F, 1.2F).clientTrackingRange(8).build("suppressor")
   );
   public static final Supplier<EntityType<StalkerEntity>> STALKER = ENTITIES.register(
      "stalker", () -> Builder.of(StalkerEntity::new, MobCategory.MONSTER).sized(1.0F, 0.9F).clientTrackingRange(8).build("stalker")
   );
   public static final Supplier<EntityType<EnforcerEntity>> ENFORCER = ENTITIES.register(
      "enforcer", () -> Builder.of(EnforcerEntity::new, MobCategory.MONSTER).sized(0.9F, 2.0F).clientTrackingRange(9).build("enforcer")
   );
   public static final Supplier<EntityType<OverseerEntity>> OVERSEER = ENTITIES.register(
      "overseer", () -> Builder.of(OverseerEntity::new, MobCategory.MONSTER).sized(1.6F, 3.7F).clientTrackingRange(10).build("overseer")
   );
   public static final Supplier<EntityType<ArbiterEntity>> ARBITER = ENTITIES.register(
      "arbiter", () -> Builder.of(ArbiterEntity::new, MobCategory.MONSTER).sized(1.8F, 4.4F).clientTrackingRange(12).build("arbiter")
   );
   public static final Supplier<EntityType<ArbiterSealEntity>> ARBITER_SEAL = ENTITIES.register(
      "arbiter_seal",
      () -> Builder.of(ArbiterSealEntity::new, MobCategory.MISC).sized(0.7F, 0.7F).clientTrackingRange(12).updateInterval(2).fireImmune().build("arbiter_seal")
   );
   public static final Supplier<EntityType<ArbiterWhipEntity>> ARBITER_WHIP = ENTITIES.register(
      "arbiter_whip",
      () -> Builder.of(ArbiterWhipEntity::new, MobCategory.MISC).sized(0.4F, 0.4F).clientTrackingRange(12).updateInterval(1).fireImmune().build("arbiter_whip")
   );
   public static final Supplier<EntityType<VerdictHookEntity>> VERDICT_HOOK = ENTITIES.register(
      "verdict_hook",
      () -> Builder.of(VerdictHookEntity::new, MobCategory.MISC).sized(0.4F, 0.4F).clientTrackingRange(12).updateInterval(1).fireImmune().build("verdict_hook")
   );
   public static final Supplier<EntityType<LeviathanEntity>> LEVIATHAN = ENTITIES.register(
      "leviathan", () -> Builder.of(LeviathanEntity::new, MobCategory.MONSTER).sized(4.8F, 9.0F).clientTrackingRange(16).fireImmune().build("leviathan")
   );
   public static final Supplier<EntityType<SentinelEntity>> SENTINEL = ENTITIES.register(
      "sentinel", () -> Builder.of(SentinelEntity::new, MobCategory.MONSTER).sized(2.4F, 7.2F).clientTrackingRange(16).fireImmune().build("sentinel")
   );
   public static final Supplier<EntityType<VarkosEntity>> VARKOS = ENTITIES.register(
      "varkos", () -> Builder.of(VarkosEntity::new, MobCategory.MONSTER).sized(1.08F, 3.31F).clientTrackingRange(10).fireImmune().build("varkos")
   );
   public static final Supplier<EntityType<ArchEvokerEntity>> ARCHEVOKER = ENTITIES.register(
      "archevoker", () -> Builder.of(ArchEvokerEntity::new, MobCategory.MONSTER).sized(0.9F, 2.5F).clientTrackingRange(12).build("archevoker")
   );
   public static final Supplier<EntityType<ArchEvokerCloneEntity>> ARCHEVOKER_CLONE = ENTITIES.register(
      "archevoker_clone", () -> Builder.of(ArchEvokerCloneEntity::new, MobCategory.MONSTER).sized(0.9F, 2.5F).clientTrackingRange(12).build("archevoker_clone")
   );
   public static final Supplier<EntityType<EvogatorEntity>> EVOGATOR = ENTITIES.register(
      "evogator", () -> Builder.of(EvogatorEntity::new, MobCategory.MONSTER).sized(1.2F, 0.85F).clientTrackingRange(10).build("evogator")
   );
   public static final Supplier<EntityType<VexplodeEntity>> VEXPLODE = ENTITIES.register(
      "vexplode", () -> Builder.of(VexplodeEntity::new, MobCategory.MISC).sized(0.6F, 0.6F).clientTrackingRange(10).updateInterval(2).build("vexplode")
   );
   public static final Supplier<EntityType<SoulOrbEntity>> SOUL_ORB = ENTITIES.register(
      "soul_orb", () -> Builder.of(SoulOrbEntity::new, MobCategory.MISC).sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(4).build("soul_orb")
   );
   public static final Supplier<EntityType<SoulBoltEntity>> SOUL_BOLT = ENTITIES.register(
      "soul_bolt", () -> Builder.of(SoulBoltEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(6).updateInterval(2).build("soul_bolt")
   );
   public static final Supplier<EntityType<AeroCrescentEntity>> AERO_CRESCENT = ENTITIES.register(
      "aero_crescent",
      () -> Builder.of(AeroCrescentEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(6).updateInterval(2).build("aero_crescent")
   );
   public static final Supplier<EntityType<VfxEntity>> VFX = ENTITIES.register(
      "vfx", () -> Builder.of(VfxEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(Integer.MAX_VALUE).build("vfx")
   );
   public static final Supplier<EntityType<VoidOccluderEntity>> VOID_OCCLUDER = ENTITIES.register(
      "void_occluder",
      () -> Builder.of(VoidOccluderEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .noSummon()
            .noSave()
            .clientTrackingRange(0)
            .updateInterval(Integer.MAX_VALUE)
            .build("void_occluder")
   );
   public static final Supplier<EntityType<VoidAnchorEntity>> LIMITLESS_VOID_ANCHOR = ENTITIES.register(
      "limitless_void_anchor",
      () -> Builder.of(VoidAnchorEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .noSummon()
            .noSave()
            .clientTrackingRange(10)
            .updateInterval(20)
            .build("limitless_void_anchor")
   );
   public static final Supplier<EntityType<EventHorizonEntity>> EVENT_HORIZON = ENTITIES.register(
      "event_horizon",
      () -> Builder.of(EventHorizonEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(4).build("event_horizon")
   );
   public static final Supplier<EntityType<BlueOrbEntity>> LIMITLESS_BLUE_ORB = ENTITIES.register(
      "limitless_blue_orb",
      () -> Builder.of(BlueOrbEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).noSummon().clientTrackingRange(8).updateInterval(20).build("limitless_blue_orb")
   );
   public static final Supplier<EntityType<RedBlastEntity>> LIMITLESS_RED_BLAST = ENTITIES.register(
      "limitless_red_blast",
      () -> Builder.of(RedBlastEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .noSummon()
            .clientTrackingRange(8)
            .updateInterval(20)
            .build("limitless_red_blast")
   );
   public static final Supplier<EntityType<PurpleSphereEntity>> LIMITLESS_PURPLE_SPHERE = ENTITIES.register(
      "limitless_purple_sphere",
      () -> Builder.of(PurpleSphereEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .noSummon()
            .clientTrackingRange(10)
            .updateInterval(10)
            .build("limitless_purple_sphere")
   );
   public static final Supplier<EntityType<BlockFxEntity>> BLOCK_FX = ENTITIES.register(
      "block_fx", () -> Builder.of(BlockFxEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1).build("block_fx")
   );
   public static final Supplier<EntityType<StoneConstructEntity>> STONE_CONSTRUCT = ENTITIES.register(
      "stone_construct",
      () -> Builder.of(StoneConstructEntity::new, MobCategory.MISC)
            .sized(1.0F, 1.0F)
            .clientTrackingRange(10)
            .updateInterval(Integer.MAX_VALUE)
            .build("stone_construct")
   );
   public static final Supplier<EntityType<GlacierEntity>> GLACIER = ENTITIES.register(
      "glacier",
      () -> Builder.of(GlacierEntity::new, MobCategory.MISC).sized(1.0F, 1.0F).clientTrackingRange(10).updateInterval(Integer.MAX_VALUE).build("glacier")
   );
   public static final Supplier<EntityType<HoverRockEntity>> HOVER_ROCK = ENTITIES.register(
      "hover_rock", () -> Builder.of(HoverRockEntity::new, MobCategory.MISC).sized(1.0F, 1.0F).clientTrackingRange(12).updateInterval(1).build("hover_rock")
   );
   public static final Supplier<EntityType<StoneMechEntity>> STONE_MECH = ENTITIES.register(
      "stone_mech", () -> Builder.of(StoneMechEntity::new, MobCategory.MONSTER).sized(1.8F, 3.2F).clientTrackingRange(12).build("stone_mech")
   );
   public static final Supplier<EntityType<AfterimageEntity>> AFTERIMAGE = ENTITIES.register(
      "afterimage",
      () -> Builder.of(AfterimageEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(24).updateInterval(Integer.MAX_VALUE).build("afterimage")
   );
   public static final Supplier<EntityType<SpeedEchoEntity>> SPEED_ECHO = ENTITIES.register(
      "speed_echo",
      () -> Builder.of(SpeedEchoEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(6).updateInterval(Integer.MAX_VALUE).build("speed_echo")
   );
   public static final Supplier<EntityType<SunOrbEntity>> SUN_ORB = ENTITIES.register(
      "sun_orb", () -> Builder.of(SunOrbEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(20).updateInterval(1).build("sun_orb")
   );
   public static final Supplier<EntityType<GripAnchorEntity>> GRIP_ANCHOR = ENTITIES.register(
      "grip_anchor", () -> Builder.of(GripAnchorEntity::new, MobCategory.MISC).sized(0.2F, 0.2F).clientTrackingRange(10).updateInterval(1).build("grip_anchor")
   );
   private static final int BOSS_CAP_RADIUS = 128;

   private AwakenedEntities() {
   }

   public static void register(IEventBus modEventBus) {
      ENTITIES.register(modEventBus);
      AwakenedNpcs.registerAll();
      NpcKits.register(modEventBus);
      modEventBus.addListener(AwakenedEntities::registerAttributes);
      modEventBus.addListener(AwakenedEntities::registerSpawnPlacements);
   }

   private static void registerAttributes(EntityAttributeCreationEvent event) {
      event.put(SUPPRESSOR.get(), SuppressorEntity.createAttributes().build());
      event.put(STALKER.get(), StalkerEntity.createAttributes().build());
      event.put(ENFORCER.get(), EnforcerEntity.createAttributes().build());
      event.put(OVERSEER.get(), OverseerEntity.createAttributes().build());
      event.put(ARBITER.get(), ArbiterEntity.createAttributes().build());
      event.put(ARBITER_SEAL.get(), ArbiterSealEntity.createAttributes().build());
      event.put(LEVIATHAN.get(), LeviathanEntity.createAttributes().build());
      event.put(SENTINEL.get(), SentinelEntity.createAttributes().build());
      event.put(VARKOS.get(), VarkosEntity.createAttributes().build());
      event.put(ARCHEVOKER.get(), ArchEvokerEntity.createAttributes().build());
      event.put(ARCHEVOKER_CLONE.get(), ArchEvokerCloneEntity.createAttributes().build());
      event.put(EVOGATOR.get(), EvogatorEntity.createAttributes().build());
      event.put(VEXPLODE.get(), VexplodeEntity.createAttributes().build());
      event.put(STONE_MECH.get(), StoneMechEntity.createAttributes().build());
   }

   private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
      event.register(SUPPRESSOR.get(), SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, AwakenedEntities::surfaceMonster, Operation.REPLACE);
      event.register(STALKER.get(), SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, AwakenedEntities::surfaceMonster, Operation.REPLACE);
      event.register(ENFORCER.get(), SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, AwakenedEntities::surfaceMonster, Operation.REPLACE);
      event.register(OVERSEER.get(), SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, AwakenedEntities::bossSurfaceMonster, Operation.REPLACE);
   }

   private static <T extends Monster> boolean bossSurfaceMonster(
      EntityType<T> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random
   ) {
      if (!surfaceMonster(type, level, reason, pos, random)) {
         return false;
      } else {
         AABB area = new AABB(
            (double)(pos.getX() - 128),
            (double)(pos.getY() - 64),
            (double)(pos.getZ() - 128),
            (double)(pos.getX() + 128),
            (double)(pos.getY() + 64),
            (double)(pos.getZ() + 128)
         );
         return level.getEntitiesOfClass(Mob.class, area, mob -> {
            if (mob instanceof AwakenedThreat threat && threat.isBoss()) {
               return true;
            }

            return false;
         }).isEmpty();
      }
   }

   private static <T extends Monster> boolean surfaceMonster(
      EntityType<T> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random
   ) {
      if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
         return false;
      } else {
         int surface = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
         return pos.getY() >= surface - 1;
      }
   }
}
