package com.awakenedmod.awakened.content.summoning;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import com.awakenedmod.awakened.core.team.EntityOwners;
import com.awakenedmod.awakened.core.team.TeamService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MinionSupport {
   private static final float DRAIN_PER_SECOND_PER_COST = 0.05F;
   private static final int UPKEEP_SYNC_TICKS = 5;
   private static final int UPKEEP_RECALL_TICKS = 20;
   private static final int HELD_TICKS = 600;
   private static final int HELD_CHECK_TICKS = 20;
   private static final String HELD_TAG = "awakened_minion_held";
   private static final double QUERY_RADIUS = 96.0;
   private static final Map<UUID, Vec3> HOLDING = new ConcurrentHashMap<>();
   static final float MIN_BONUS = 1.0E-4F;
   private static final ResourceLocation MINION_HEALTH_ID = ResourceLocation.fromNamespaceAndPath("awakened", "minion_power_health");
   private static final int MAX_CONJURED = 24;
   private static final double ADOPT_RANGE_SQR = 256.0;

   private MinionSupport() {
   }

   public static LivingEntity owner(Mob mob) {
      MinionData data = SummonAttachments.minion(mob);
      if (!data.isMinion()) {
         return null;
      } else {
         Player owner = mob.level().getPlayerByUUID(data.owner());
         return owner != null && owner.isAlive() ? owner : null;
      }
   }

   public static UUID allegianceOwner(Mob mob) {
      MinionData self = SummonAttachments.minion(mob);
      if (self.isMinion()) {
         return self.owner();
      } else {
         if (mob instanceof TraceableEntity traceable && traceable.getOwner() instanceof Mob summoner) {
            MinionData parent = SummonAttachments.minion(summoner);
            if (parent.isMinion()) {
               return parent.owner();
            }
         }

         return null;
      }
   }

   public static boolean alliedSummonForbidden(LivingEntity attacker, LivingEntity target) {
      if (target != null && attacker instanceof Mob mob) {
         UUID ownerId = allegianceOwner(mob);
         return ownerId != null && alliedSummonForbidden(ownerId, target);
      } else {
         return false;
      }
   }

   public static boolean alliedSummonForbidden(UUID ownerId, LivingEntity target) {
      if (target != null && ownerId != null) {
         if (target instanceof Player player && player.getUUID().equals(ownerId)) {
            return true;
         }

         if (SummonAttachments.minion(target).isOwnedBy(ownerId)) {
            return true;
         } else {
            UUID targetOwner = EntityOwners.ownerOf(target);
            return targetOwner != null
               && target.level() instanceof ServerLevel level
               && TeamService.friendlyFireBlocked(level.getServer(), ownerId, targetOwner);
         }
      } else {
         return false;
      }
   }

   public static boolean autonomousTargetForbidden(LivingEntity attacker, LivingEntity target) {
      if (target != null && attacker instanceof Mob mob && allegianceOwner(mob) != null) {
         return alliedSummonForbidden(attacker, target) || !(target instanceof Enemy);
      }

      return false;
   }

   public static boolean isHolding(Mob mob) {
      return HOLDING.containsKey(mob.getUUID());
   }

   public static Vec3 holdSpot(Mob mob) {
      return HOLDING.get(mob.getUUID());
   }

   public static void setHolding(Mob mob, Vec3 spot) {
      HOLDING.put(mob.getUUID(), spot);
   }

   public static void setHolding(Mob mob, boolean holding) {
      if (holding) {
         HOLDING.put(mob.getUUID(), mob.position());
      } else {
         HOLDING.remove(mob.getUUID());
      }
   }

   public static List<Mob> minionsOf(ServerPlayer player) {
      UUID id = player.getUUID();
      AABB box = player.getBoundingBox().inflate(96.0);
      return player.serverLevel().getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && SummonAttachments.minion(m).isOwnedBy(id));
   }

   public static List<? extends Mob> minionsIn(ServerLevel level, ServerPlayer owner) {
      UUID id = owner.getUUID();
      return level.getEntities(EntityTypeTest.forClass(Mob.class), m -> m.isAlive() && SummonAttachments.minion(m).isOwnedBy(id));
   }

   public static int count(ServerPlayer player) {
      int n = 0;

      for (Mob m : minionsOf(player)) {
         if (!SummonAttachments.minion(m).conjured()) {
            n++;
         }
      }

      return n;
   }

   public static int conjuredCount(ServerPlayer player) {
      int n = 0;

      for (Mob m : minionsOf(player)) {
         if (SummonAttachments.minion(m).conjured()) {
            n++;
         }
      }

      return n;
   }

   public static Mob aimedMinion(ServerPlayer player, double range) {
      return AimUtil.aimedLivingCone(player, range, 10.0, e -> {
         if (e instanceof Mob mx && e != player.getVehicle() && SummonAttachments.minion(mx).isOwnedBy(player.getUUID())) {
            return true;
         }

         return false;
      }) instanceof Mob m ? m : null;
   }

   public static Mob nearestMinion(ServerPlayer player) {
      Mob best = null;
      double bestSqr = Double.MAX_VALUE;
      boolean bestRaised = false;

      for (Mob m : minionsOf(player)) {
         if (m != player.getVehicle()) {
            boolean raised = !SummonAttachments.minion(m).conjured();
            if (!bestRaised || raised) {
               double d = m.distanceToSqr(player);
               if (raised && !bestRaised || d < bestSqr) {
                  bestSqr = d;
                  best = m;
                  bestRaised = raised;
               }
            }
         }
      }

      return best;
   }

   public static Vec3 groundSpot(ServerPlayer player, double range) {
      Vec3 aim = AimUtil.aimedPosition(player, range);
      ServerLevel level = player.serverLevel();
      MutableBlockPos pos = new MutableBlockPos(Mth.floor(aim.x), Mth.floor(aim.y), Mth.floor(aim.z));
      int steps = 0;

      while (steps++ < 6 && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.below())) {
         pos.move(0, -1, 0);
      }

      return new Vec3(aim.x, (double)pos.getY(), aim.z);
   }

   public static Mob spawnRising(ServerPlayer owner, String soulType, Vec3 spot) {
      EntityType<?> type = SummonSouls.type(soulType);
      if (type == null) {
         return null;
      } else {
         Vec3 safe = safeSpawnSpot(Dmg.serverLevel(owner), spot, type);
         if (safe == null) {
            return null;
         } else {
            Mob mob = create(owner, soulType, safe);
            if (mob == null) {
               return null;
            } else {
               SummonFx.rise(mob, safe);
               owner.level().addFreshEntity(mob);
               SummonOrders.apply(owner, mob);
               if (SummonSouls.isApex(soulType)) {
                  QuestSystem.feat(owner, "summon_apex_raise");
               }

               return mob;
            }
         }
      }
   }

   public static Vec3 safeSpawnSpot(ServerLevel level, Vec3 desired, EntityType<?> type) {
      int height = Math.max(1, Mth.ceil(type.getHeight()));
      int bx = Mth.floor(desired.x);
      int bz = Mth.floor(desired.z);
      MutableBlockPos pos = new MutableBlockPos();

      for (int dy = 3; dy >= -6; dy--) {
         pos.set(bx, Mth.floor(desired.y) + dy, bz);
         if (standable(level, pos, height)) {
            return new Vec3(desired.x, (double)pos.getY(), desired.z);
         }
      }

      return null;
   }

   private static boolean standable(ServerLevel level, BlockPos pos, int height) {
      BlockPos below = pos.below();
      BlockState floor = level.getBlockState(below);
      if (!floor.isFaceSturdy(level, below, Direction.UP) && floor.getCollisionShape(level, below).isEmpty()) {
         return false;
      } else {
         for (int h = 0; h < height; h++) {
            BlockPos p = pos.above(h);
            BlockState s = level.getBlockState(p);
            if (s.isSuffocating(level, p) || !s.getCollisionShape(level, p).isEmpty()) {
               return false;
            }
         }

         return true;
      }
   }

   private static Mob create(ServerPlayer owner, String soulType, Vec3 pos) {
      EntityType<?> type = SummonSouls.type(soulType);
      if (type == null) {
         return null;
      } else {
         Entity entity = type.create(owner.level());
         if (entity instanceof Mob mob) {
            mob.moveTo(pos.x, pos.y, pos.z, owner.getYRot(), 0.0F);
            markMinion(mob, owner, soulType);
            return mob;
         } else {
            if (entity != null) {
               entity.discard();
            }

            return null;
         }
      }
   }

   public static void markMinion(Mob mob, ServerPlayer owner, String soulType) {
      markMinion(mob, owner, soulType, false);
   }

   public static void markMinion(Mob mob, ServerPlayer owner, String soulType, boolean conjured) {
      float bonus = ownerPowerBonus(owner);
      mob.setData(SummonAttachments.MINION, new MinionData(owner.getUUID(), soulType, bonus, conjured));
      if (!conjured) {
         mob.setPersistenceRequired();
      }

      equipSignatureWeapon(mob);
      applyOwnerScaling(mob, bonus);
      SummonMounts.prepare(mob, owner);
      ensureAllyGoals(mob);
   }

   public static float ownerPowerBonus(ServerPlayer owner) {
      int level = AwakenedAttachments.get(owner).masteryLevel();
      return MasteryScaling.massBonus(level) + MasteryScaling.abilityPower(owner);
   }

   private static void applyOwnerScaling(Mob mob, float bonus) {
      if (!(bonus <= 1.0E-4F)) {
         scaleAttribute(mob, Attributes.MAX_HEALTH, MINION_HEALTH_ID, (double)bonus);
         mob.setHealth(mob.getMaxHealth());
      }
   }

   public static void applyStoredScaling(Mob mob) {
      applyOwnerScaling(mob, SummonAttachments.minion(mob).powerBonus());
   }

   static void scaleAttribute(Mob mob, Holder<Attribute> attr, ResourceLocation id, double bonus) {
      AttributeInstance inst = mob.getAttribute(attr);
      if (inst != null) {
         inst.removeModifier(id);
         if (!(bonus <= 0.0)) {
            inst.addPermanentModifier(new AttributeModifier(id, bonus, Operation.ADD_MULTIPLIED_BASE));
         }
      }
   }

   private static void equipSignatureWeapon(Mob mob) {
      if (mob.getMainHandItem().isEmpty()) {
         EntityType<?> type = mob.getType();
         Item weapon = null;
         if (type == EntityType.SKELETON || type == EntityType.STRAY || type == EntityType.BOGGED || type == EntityType.ILLUSIONER) {
            weapon = Items.BOW;
         } else if (type == EntityType.PILLAGER || type == EntityType.PIGLIN) {
            weapon = Items.CROSSBOW;
         } else if (type == EntityType.WITHER_SKELETON) {
            weapon = Items.STONE_SWORD;
         } else if (type == EntityType.VEX) {
            weapon = Items.IRON_SWORD;
         } else if (type == EntityType.ZOMBIFIED_PIGLIN) {
            weapon = Items.GOLDEN_SWORD;
         } else if (type == EntityType.PIGLIN_BRUTE) {
            weapon = Items.GOLDEN_AXE;
         } else if (type == EntityType.VINDICATOR) {
            weapon = Items.IRON_AXE;
         }

         if (weapon != null) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
            mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
         }
      }
   }

   public static void inheritAllegiance(Mob parent, Mob child) {
      adopt(parent, child);
   }

   public static boolean adopt(Mob parent, Mob child) {
      if (parent != child && !child.level().isClientSide() && child.level() == parent.level()) {
         MinionData parentData = SummonAttachments.minion(parent);
         if (!parentData.isMinion() || parentData.conjured()) {
            return false;
         } else if (SummonAttachments.minion(child).isMinion()) {
            return false;
         } else if (child.distanceToSqr(parent) > 256.0) {
            return false;
         } else {
            if (owner(parent) instanceof ServerPlayer ownerPlayer && conjuredCount(ownerPlayer) < 24) {
               markMinion(child, ownerPlayer, SummonSouls.typeId(child), true);
               SummonOrders.apply(ownerPlayer, child);
               return true;
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public static void ensureAllyGoals(Mob mob) {
      keepWardenFromBurrowing(mob);
      if (mob.isNoAi() && !PossessControl.isPiloted(mob) && !SummonFx.isRising(mob)) {
         mob.setNoAi(false);
      }

      boolean brain = isBrainControlled(mob);
      Class<?> marker = brain ? WardenMinionGoal.class : MinionFollowGoal.class;

      for (WrappedGoal g : mob.goalSelector.getAvailableGoals()) {
         if (marker.isInstance(g.getGoal())) {
            return;
         }
      }

      mob.targetSelector.removeAllGoals(gx -> true);
      mob.setTarget(null);
      if (brain) {
         mob.goalSelector.addGoal(1, new WardenMinionGoal((Warden)mob, 1.0));
      } else {
         mob.goalSelector.addGoal(2, new MinionFollowGoal(mob, 1.0));
         mob.targetSelector.addGoal(1, new MinionAssistGoal(mob));
      }
   }

   private static boolean isBrainControlled(Mob mob) {
      return mob instanceof Warden;
   }

   public static LivingEntity assistTarget(Mob mob, LivingEntity owner) {
      LivingEntity c = validAssistTarget(mob, owner, owner.getLastHurtMob());
      if (c != null) {
         return c;
      } else {
         c = validAssistTarget(mob, owner, owner.getLastHurtByMob());
         return c != null ? c : validAssistTarget(mob, owner, mob.getLastHurtByMob());
      }
   }

   public static LivingEntity commandedOrAssistTarget(Mob mob, LivingEntity owner) {
      if (!canPressAttack(mob)) {
         return null;
      } else {
         LivingEntity foe = mob.level() instanceof ServerLevel level ? SummonOrders.attackTarget(level, owner.getUUID()) : null;
         if (foe == null) {
            foe = assistTarget(mob, owner);
         }

         return foe != null && !alliedSummonForbidden(mob, foe) && mob.canAttack(foe) ? foe : null;
      }
   }

   public static boolean canPressAttack(Mob mob) {
      if (mob instanceof Bee bee && bee.hasStung()) {
         return false;
      }

      return true;
   }

   private static LivingEntity validAssistTarget(Mob mob, LivingEntity owner, LivingEntity candidate) {
      if (candidate != null && candidate.isAlive() && candidate != owner && candidate != mob) {
         if (alliedSummonForbidden(owner.getUUID(), candidate)) {
            return null;
         } else {
            return mob.canAttack(candidate) ? candidate : null;
         }
      } else {
         return null;
      }
   }

   private static void keepWardenFromBurrowing(Mob mob) {
      if (mob instanceof Warden) {
         mob.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
         mob.setPersistenceRequired();
      }
   }

   public static void recall(ServerPlayer owner, Mob mob) {
      MinionData data = SummonAttachments.minion(mob);
      if (!data.conjured()) {
         SoulData souls = SummonAttachments.souls(owner);
         souls.add(data.soulType(), 1, 100);
         owner.setData(SummonAttachments.SOULS, souls);
      }

      setHolding(mob, false);
      PossessControl.stopPiloting(mob);
      SummonFx.cancel(mob);
      if (mob.level() instanceof ServerLevel level) {
         dissolveFx(level, mob);
      }

      mob.discard();
      if (minionsOf(owner).isEmpty()) {
         SummonOrders.clear(owner);
      }
   }

   public static int recallAllIn(ServerLevel level, ServerPlayer owner) {
      List<? extends Mob> out = minionsIn(level, owner);

      for (Mob mob : out) {
         recall(owner, mob);
      }

      return out.size();
   }

   public static void dissolveFx(ServerLevel level, Mob mob) {
      Vec3 c = mob.position().add(0.0, (double)mob.getBbHeight() * 0.5, 0.0);
      Fx.burst(level, ParticleTypes.SCULK_SOUL, c, 18, 0.3, 0.02);
      Fx.burst(level, ParticleTypes.SMOKE, mob.position().add(0.0, 0.1, 0.0), 10, 0.25, 0.01);
      Fx.sound(level, mob.position(), (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.5F, 0.5F);
   }

   public static void tick(ServerPlayer player, PowerData data) {
      List<Mob> minions = minionsOf(player);
      if (!minions.isEmpty()) {
         ServerLevel farthest = player.serverLevel();
         if (farthest instanceof ServerLevel) {
            ServerLevel level = farthest;
            if (player.tickCount % 4 == 0) {
               for (Mob m : minions) {
                  level.sendParticles(ParticleTypes.SCULK_SOUL, m.getX(), m.getY() + 0.08, m.getZ(), 1, 0.18, 0.02, 0.18, 0.0);
               }
            }
         }

         if (player.tickCount % 20 == 0) {
            for (Mob m : minions) {
               if (m.tickCount >= 600 && !SummonAttachments.minion(m).conjured() && m.addTag("awakened_minion_held")) {
                  QuestSystem.feat(player, "summon_minion_held");
               }
            }
         }

         if (!data.infiniteStamina()) {
            float drain = armyDrainPerSecond(minions) / 20.0F;
            if (!(drain <= 0.0F)) {
               StaminaSystem.drainSustained(data, player.serverLevel().getGameTime(), StaminaSystem.effectiveCost(drain, SummonAbility.class.getSimpleName()));
               if (data.stamina() <= 0.0F && player.tickCount % 20 == 0) {
                  Mob farthestx = farthestRecallable(player, minions);
                  if (farthestx != null) {
                     recall(player, farthestx);
                     player.displayClientMessage(Component.translatable("awakened.summon.upkeep_recall"), true);
                  }
               }

               if (player.tickCount % 5 == 0) {
                  PowerService.sync(player);
               }
            }
         }
      }
   }

   private static float armyDrainPerSecond(List<Mob> minions) {
      float cost = 0.0F;

      for (Mob m : minions) {
         if (!SummonAttachments.minion(m).conjured()) {
            cost += (float)SummonSouls.summonCost(m.getType());
         }
      }

      return cost * 0.05F;
   }

   private static Mob farthestRecallable(ServerPlayer player, List<Mob> minions) {
      Mob best = null;
      double bestSqr = -1.0;

      for (Mob m : minions) {
         if (!PossessControl.isPiloted(m) && !SummonAttachments.minion(m).conjured()) {
            double d = m.distanceToSqr(player);
            if (d > bestSqr) {
               bestSqr = d;
               best = m;
            }
         }
      }

      return best;
   }
}
