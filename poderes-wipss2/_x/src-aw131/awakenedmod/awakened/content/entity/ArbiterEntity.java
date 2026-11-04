package com.awakenedmod.awakened.content.entity;

import com.awakenedmod.awakened.Config;
import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.content.entity.boss.AwakenedBossEntity;
import com.awakenedmod.awakened.content.entity.boss.BossDefenses;
import com.awakenedmod.awakened.content.summoning.PossessMelee;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.net.ArbiterRootWarnPayload;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.relic.RelicDrops;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ArbiterEntity extends AwakenedBossEntity implements PossessMelee {
   private static final BossDefenses.Profile DEFENSE_PROFILE = new BossDefenses.Profile(48.0, 45.0F, 5.0, 100);
   public static final byte PHASE_NONE = 0;
   public static final byte PHASE_SHOCKWAVE = 2;
   public static final byte PHASE_BLINK = 3;
   public static final byte PHASE_ERUPT = 4;
   public static final byte PHASE_SENTENCE = 5;
   public static final byte PHASE_WHIP = 6;
   public static final byte PHASE_PENDULUM = 7;
   public static final byte PHASE_REEL = 8;
   private static final double COURT_FLOOR_RADIUS = 17.5;
   private static final int WHIP_TELL_TICKS = 22;
   private static final double WHIP_RANGE = 26.0;
   private static final double WHIP_LINE_BACK = 1.5;
   private static final double WHIP_LINE_AHEAD = 4.0;
   private static final double WHIP_STRIP_HALF_WIDTH = 1.3;
   private static final float WHIP_DAMAGE = 16.5F;
   private static final double WHIP_SHOVE = 0.75;
   private static final double WHIP_LIFT = 0.28;
   private static final int PENDULUM_TELL_TICKS = 28;
   private static final int PENDULUM_TELL_LAND = 22;
   private static final double PENDULUM_TELL_LIFT = 1.5;
   private static final double PENDULUM_LENGTH = 17.5;
   private static final double PENDULUM_INNER = 1.0;
   private static final double PENDULUM_HALF_WIDTH = 1.0;
   private static final double PENDULUM_HEIGHT = 0.55;
   private static final double PENDULUM_TURNS = 3.0;
   private static final float PENDULUM_DEG_PER_TICK_START = 4.5F;
   private static final float PENDULUM_DEG_PER_TICK_END = 7.0F;
   private static final float PENDULUM_TIER_SPEEDUP = 1.2F;
   private static final double PENDULUM_CARRY = 0.78;
   private static final double PENDULUM_CARRY_OUT = 0.22;
   private static final double PENDULUM_CARRY_UP = 0.36;
   private static final double PENDULUM_REBITE_DEG = 150.0;
   private static final float PENDULUM_BODY_TURN = 0.35F;
   private static final int PENDULUM_SCRAPE_DEG = 45;
   private static final int SWEEP_VOLLEY_CAP = 3;
   private static final double SWEEP_PASS_DEG = 180.0;
   private static final float PENDULUM_DAMAGE = 10.0F;
   private static final int ROOT_LINE_FIRST = 24;
   private static final double ROOT_RANGE = 24.0;
   private static final int ROOT_POINTS = 3;
   private static final double ROOT_HIT_RADIUS = 2.2;
   private static final double ROOT_HIT_RISE = 2.0;
   private static final double ROOT_WARN_MARGIN = 0.3;
   private static final int ROOT_WARN_RESEND = 10;
   private static final int ROOT_WAVE_COUNT = 5;
   private static final int ROOT_WAVE_STEP = 5;
   private static final double ROOT_WAVE_PITCH = 2.6;
   private static final int ROOT_MARKS_CAP = 12;
   private static final double ROOT_LINE_STILL = 0.05;
   private static final double ROOT_WAVE_SPACING = 1.0;
   private static final double ROOT_SHAKE_RADIUS = 8.8;
   private static final int ROOT_FOLLOW_TICKS = 6;
   private static final int ROOT_SPALL = 7;
   private static final int ROOT_SCORCH_TICKS = 60;
   private static final float ROOT_DAMAGE = 11.5F;
   private static final double ROOT_KNOCKUP = 0.67;
   private static final int REEL_TELL_TICKS = 30;
   private static final int REEL_INTERVAL = 240;
   private static final double REEL_FAR_BAND = 18.0;
   private static final double REEL_NEAR_BAND = 14.0;
   private static final int REEL_PATIENCE_TICKS = 70;
   private static final double REEL_HOOK_SPEED = 1.6;
   private static final int REEL_FLY_MIN = 8;
   private static final int REEL_FLY_CAP = 40;
   private static final int REEL_DRAG_TICKS = 26;
   private static final int REEL_DRAG_MAX = 60;
   private static final double REEL_HAUL_PER_TICK = 0.33;
   private static final double REEL_RELEASE = 8.0;
   private static final double REEL_ATTACH_RADIUS = 1.8;
   private static final double REEL_PULL = 0.3;
   private static final int REEL_TRACER_CADENCE = 5;
   private static final int REEL_TRACER_LIFE = 6;
   private static final double REEL_HAUL_DOWN = -0.12;
   private static final double REPEL_RADIUS = 5.0;
   private static final int REPEL_INTERVAL = 160;
   private static final double REPEL_SWEEP_LENGTH = 5.0;
   private static final double REPEL_SWEEP_HEIGHT = 1.55;
   private static final double REPEL_BAND_LOW = 0.9;
   private static final double REPEL_BAND_HIGH = 2.0;
   private static final double REPEL_INNER = 0.0;
   private static final double REPEL_HALF_WIDTH = 1.0;
   private static final int REPEL_TELL_TICKS = 18;
   private static final int REPEL_TELL_LAND = 12;
   private static final double REPEL_SWEEP_TURNS = 2.5;
   private static final float REPEL_DEG_START = 7.0F;
   private static final float REPEL_DEG_END = 11.0F;
   private static final double REPEL_REBITE_DEG = 150.0;
   private static final int REPEL_PATIENCE_TICKS = 35;
   private static final int REPEL_PATIENCE_DECAY = 2;
   private static final float REPEL_DAMAGE = 5.0F;
   private static final double REPEL_CARRY = 0.5;
   private static final double REPEL_CARRY_OUT = 1.0;
   private static final double REPEL_CARRY_UP = 0.35;
   private static final ArbiterEntity.Sweep SWEEP_PENDULUM = new ArbiterEntity.Sweep(
      17.5, 1.0, 1.0, 0.55, 0.35000000000000003, 0.55, 28, 22, 1.5, 3.0, 4.5F, 7.0F, 150.0, 10.0F, 0.78, 0.22, 0.36, (byte)2
   );
   private static final ArbiterEntity.Sweep SWEEP_REPEL = new ArbiterEntity.Sweep(
      5.0, 0.0, 1.0, 1.55, 0.9, 2.0, 18, 12, 0.0, 2.5, 7.0F, 11.0F, 150.0, 5.0F, 0.5, 1.0, 0.35, (byte)4
   );
   private static final int FV_TELL_TICKS = 70;
   private static final double FV_RADIUS = 12.0;
   private static final int FV_SAFE_CELLS = 3;
   private static final double FV_CELL_RADIUS = 2.2;
   private static final double FV_CELL_RING = 6.0;
   private static final float FV_DAMAGE = 20.0F;
   private static final int FV_CRACK_EVERY = 4;
   private static final double FV_CRACK_RADIUS = 2.6;
   private static final int FV_CRACK_FADE = 60;
   private static final int FV_RIM_LOCKS = 6;
   private static final double FV_RIM_LOCK_HEIGHT = 5.0;
   private static final double FV_RIM_WALL_HEIGHT = 3.2;
   private static final double FV_RIM_LOCK_WIDTH = 0.5;
   private static final double FV_RIM_WALL_WIDTH = 0.3;
   private static final DustParticleOptions JUDGE_CYAN = Fx.dust(1630463, 1.3F);
   private static final DustParticleOptions JUDGE_VIOLET = Fx.dust(11544319, 1.4F);
   private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(ArbiterEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Byte> DATA_GUARD_CUE = SynchedEntityData.defineId(ArbiterEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Boolean> DATA_KNEELING = SynchedEntityData.defineId(ArbiterEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Float> DATA_PINNED_YAW = SynchedEntityData.defineId(ArbiterEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Byte> DATA_COURT_STAGE = SynchedEntityData.defineId(ArbiterEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Byte> DATA_BAR = SynchedEntityData.defineId(ArbiterEntity.class, EntityDataSerializers.BYTE);
   public static final int BAR_LINE_TIER_SHIFT = 6;
   public static final int BAR_LINE_WRAP = 64;
   public static final int KNEEL_TICKS = 180;
   private static final double ARENA_INTERIOR_RADIUS = 30.0;
   private static final double ARENA_INTERIOR_RISE = 24.0;
   private static final double ARENA_INTERIOR_DROP = 8.0;
   private static final double PROVOKE_RADIUS = 18.0;
   private static final int CEREMONY_TICKS = 70;
   private static final int CEREMONY_FORGE_START = 10;
   private static final int CEREMONY_FORGE_STAGGER = 9;
   public static final int GAVEL_HOLD_TICKS = 20;
   public static final int KNEEL_STIR_TICKS = 40;
   private static final int COURT_EMPTY_TICKS = 100;
   private static final float COURT_REGEN = 0.35F;
   private static final int GUARD_CUE_COOLDOWN = 4;
   private static final UUID NO_ATTACKER = new UUID(0L, 0L);
   private static final double POST_ADOPT = 24.0;
   private int pendingGavel;
   public static final int SEAL_COUNT = 6;
   private final List<ArbiterSealEntity> seals = new ArrayList<>();
   private static final byte COURT_RECESS = 0;
   private static final byte COURT_OPENING = 1;
   private static final byte COURT_SESSION = 2;
   private byte courtStage = 0;
   private int ceremonyTicks;
   private int ceremonyForged;
   private int gavelHold;
   private long kneelEndsAt;
   private float pinnedYaw = Float.NaN;
   private int emptyCourtTicks;
   @Nullable
   private Vec3 courtCenter;
   private final Map<UUID, Integer> guardCueAt = new HashMap<>();
   private int whipTell;
   private int whipLandIn;
   private Vec3 whipStart = Vec3.ZERO;
   private Vec3 whipEnd = Vec3.ZERO;
   private final Map<UUID, Integer> whippedAt = new HashMap<>();
   @Nullable
   private ArbiterEntity.Sweep sweep;
   private int sweepTell;
   private boolean sweeping;
   private double sweepSwept;
   private float sweepAngle;
   private float sweepFacing;
   private ArbiterWhipEntity sweepHeadA;
   private ArbiterWhipEntity sweepHeadB;
   private int sweepVolleys;
   private final Map<UUID, Double> sweepBites = new HashMap<>();
   private final List<ArbiterEntity.RootMark> rootMarks = new ArrayList<>();
   private final Set<UUID> rootWarned = new HashSet<>();
   private Set<UUID> rootStruck = new HashSet<>();
   private boolean rootIsBar;
   private int rootFollowThrough;
   private final List<ArbiterWhipEntity> rootHeads = new ArrayList<>();
   private int rootStandTicks = 12;
   private boolean rootSolid;
   private boolean rootCaged;
   private int reelCooldown = 100;
   private final Map<UUID, Integer> reelPatience = new HashMap<>();
   private int reelTell;
   private int reelFlyIn;
   private int reelFlyTotal = 8;
   private int reelDragTicks;
   private Vec3 reelSnapshot = Vec3.ZERO;
   private Vec3 reelMark = Vec3.ZERO;
   private ArbiterWhipEntity reelHead;
   private Player reelVictim;
   private int repelCooldown;
   private int repelCloseTicks;
   private boolean finalVerdictSeen;
   private boolean finalVerdictArmed;
   private int finalVerdictTell;
   private Vec3 finalVerdictCenter = Vec3.ZERO;
   private final List<Vec3> finalVerdictSafeCells = new ArrayList<>();
   private final ArbiterVerses.Runner verses = new ArbiterVerses.Runner();
   private boolean repelCutsSweep;
   private final List<ArbiterEntity.ReadyBeat> readyBeats = new ArrayList<>();
   private static final float GATE_EPSILON = 0.01F;
   private static final float[] TIER_DAMAGE = new float[]{1.1F, 1.6F, 2.1F};
   public static final int DEATH_TICKS = 90;

   public ArbiterEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level, Component.translatable("entity.awakened.arbiter"), BossBarColor.BLUE, BossBarOverlay.NOTCHED_6);
      this.xpReward = 80;
      this.setPersistenceRequired();
   }

   @Override
   protected BossDefenses.Profile defenseProfile() {
      return DEFENSE_PROFILE;
   }

   @Override
   protected boolean suppressEscalationJuice() {
      return true;
   }

   public void checkDespawn() {
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 520.0)
         .add(Attributes.ATTACK_DAMAGE, 22.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.ARMOR, 8.0)
         .add(Attributes.FOLLOW_RANGE, 48.0);
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_PHASE, (byte)0);
      builder.define(DATA_KNEELING, false);
      builder.define(DATA_GUARD_CUE, (byte)0);
      builder.define(DATA_PINNED_YAW, 0.0F);
      builder.define(DATA_COURT_STAGE, (byte)0);
      builder.define(DATA_BAR, (byte)0);
   }

   public int barLine() {
      return (Byte)this.entityData.get(DATA_BAR) & 0xFF;
   }

   public static int barLineTier(int barLine) {
      return barLine >>> 6;
   }

   public boolean inRecess() {
      return (Byte)this.entityData.get(DATA_COURT_STAGE) == 0;
   }

   public boolean inCeremony() {
      return (Byte)this.entityData.get(DATA_COURT_STAGE) == 1;
   }

   public boolean isKneeling() {
      return (Boolean)this.entityData.get(DATA_KNEELING);
   }

   public byte guardCue() {
      return (Byte)this.entityData.get(DATA_GUARD_CUE);
   }

   public float pinnedYaw() {
      return (Float)this.entityData.get(DATA_PINNED_YAW);
   }

   private void setKneeling(boolean kneeling) {
      this.entityData.set(DATA_KNEELING, kneeling);
      if (!this.level().isClientSide()) {
         if (kneeling) {
            this.pinnedYaw = this.yBodyRot;
            this.entityData.set(DATA_PINNED_YAW, this.pinnedYaw);
            this.holdPinnedFacing();
            this.goalSelector.disableControlFlag(Flag.MOVE);
            this.goalSelector.disableControlFlag(Flag.LOOK);
         } else {
            this.pinnedYaw = Float.NaN;
            this.goalSelector.enableControlFlag(Flag.MOVE);
            this.goalSelector.enableControlFlag(Flag.LOOK);
         }
      }
   }

   protected void updateControlFlags() {
      super.updateControlFlags();
      if (this.isKneeling()) {
         this.goalSelector.disableControlFlag(Flag.MOVE);
         this.goalSelector.disableControlFlag(Flag.LOOK);
      }
   }

   private void holdPinnedFacing() {
      if (Float.isNaN(this.pinnedYaw)) {
         this.pinnedYaw = this.yBodyRot;
      }

      this.setYRot(this.pinnedYaw);
      this.yRotO = this.pinnedYaw;
      this.setYBodyRot(this.pinnedYaw);
      this.yBodyRotO = this.pinnedYaw;
      this.setYHeadRot(this.pinnedYaw);
      this.yHeadRotO = this.pinnedYaw;
      this.setXRot(0.0F);
      this.xRotO = 0.0F;
   }

   public void tick() {
      if (!this.level().isClientSide()) {
         this.holdPost();
      }

      super.tick();
      if (!this.level().isClientSide()) {
         this.holdPost();
         this.entityData.set(DATA_COURT_STAGE, this.courtStage);
         if (this.deathTime > 0 && !this.isKneeling()) {
            this.setKneeling(true);
         }

         if (this.isKneeling()) {
            this.holdPinnedFacing();
         }
      }
   }

   private void holdPost() {
      Vec3 post = this.courtCenter;
      if (post == null || this.position().distanceToSqr(post) > 576.0) {
         this.courtCenter = post = this.position();
      }

      this.setDeltaMovement(Vec3.ZERO);
      this.resetFallDistance();
      if (this.getX() != post.x || this.getY() != post.y || this.getZ() != post.z) {
         this.setPos(post.x, post.y, post.z);
         this.setOldPosAndRot();
      }
   }

   public boolean isNoGravity() {
      return true;
   }

   public boolean isPushable() {
      return false;
   }

   protected void pushEntities() {
   }

   public byte getPhase() {
      return (Byte)this.entityData.get(DATA_PHASE);
   }

   private void setPhase(byte phase) {
      this.entityData.set(DATA_PHASE, phase);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 26.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
   }

   public void setTarget(LivingEntity target) {
      LivingEntity prev = this.getTarget();
      super.setTarget(target);
      if (target != null && prev == null && this.level() instanceof ServerLevel server) {
         Fx.scanLock(server, this.position().add(0.0, (double)this.getBbHeight() * 0.85, 0.0), SoundEvents.BEACON_POWER_SELECT, 1.7F, 0.9F);
         Fx.burst(server, JUDGE_CYAN, this.position().add(0.0, (double)this.getBbHeight() * 0.85, 0.0), 16, 0.3, 0.02);
      }
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      ServerLevel level = (ServerLevel)this.level();
      if (this.isKneeling()) {
         this.tickKneelStir(level);
         this.tickRootTell(level);
         if (level.getGameTime() >= this.kneelEndsAt) {
            this.rise(level);
         }
      } else if (this.courtStage == 0) {
         this.tickRecess(level);
      } else if (this.courtStage == 1) {
         this.tickCeremony(level);
      } else {
         if (this.gavelHold > 0 && --this.gavelHold == 0) {
            this.setPhase((byte)0);
         }

         this.maintainSeals(level);
         this.acquireDefendant(level);
         this.tickCourtRune(level);
         this.tickWhipResolution(level);
         this.tickRootBeat(level);
         this.tickSweep(level);
         this.tickReelDrag(level);
         this.tickFinalVerdict(level);
         LivingEntity target = this.getTarget();
         if (target == null) {
            this.dropIdleTells();
            this.tickEmptyCourt();
         } else {
            this.emptyCourtTicks = 0;
            this.readyBeats.clear();
            this.tickWhipTell(level);
            this.tickReelAim(level);
            this.tickRepel(level);
            this.callTheNextBeat();
            this.tickVerse(level);
         }
      }
   }

   private void offerBeat(ArbiterEntity.Beat beat, Runnable start) {
      this.readyBeats.add(new ArbiterEntity.ReadyBeat(beat, start));
   }

   private void callTheNextBeat() {
      if (!this.readyBeats.isEmpty()) {
         ArbiterEntity.ReadyBeat chosen = this.readyBeats.get(0);
         this.readyBeats.clear();
         this.verses.interject();
         chosen.start().run();
      }
   }

   private void tickVerse(ServerLevel level) {
      ArbiterVerses.Bar bar = this.verses.tick(this.escalationPhase(), this.getRandom(), this.beatBusy());
      if (bar != null) {
         this.markBarLine(level);
         this.playBar(level, bar);
      }
   }

   private void markBarLine(ServerLevel level) {
      int count = this.barLine() + 1 & 63;
      int tier = Math.max(1, Math.min(3, this.escalationPhase()));
      this.entityData.set(DATA_BAR, (byte)(tier << 6 | count));
      Vfx.ring(level, this.position().add(0.0, 0.12, 0.0), 3.9, 11544319, 6);
      Fx.sound(level, this.position(), (SoundEvent)SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.HOSTILE, 0.35F, 0.6F);
   }

   private void playBar(ServerLevel level, ArbiterVerses.Bar bar) {
      switch (bar) {
         case WHIP:
            Player defendant = this.nextWhipDefendant(level);
            if (defendant != null) {
               this.beginWhip(level, defendant);
            }
            break;
         case ROOT:
            this.beginRootEruption(level, this.getTarget());
            break;
         case SWEEP:
            this.beginPendulum(level);
            break;
         case VERDICT:
            this.beginFinalVerdict(level);
         case REST:
      }
   }

   private void openVerse(@Nullable ArbiterVerses.Verse opener) {
      if (this.finalVerdictArmed) {
         this.finalVerdictArmed = false;
         opener = ArbiterVerses.V0;
      }

      this.verses.seed(this.escalationPhase(), this.getRandom(), opener, false);
   }

   private boolean beatBusy() {
      return this.gavelHold > 0
         || this.whipTell > 0
         || this.whipLandIn > 0
         || this.sweepTell > 0
         || this.sweeping
         || this.rootIsBar
         || this.rootFollowThrough > 0
         || this.reelTell > 0
         || this.reelFlyIn > 0
         || this.reelDragTicks > 0
         || this.finalVerdictTell > 0;
   }

   private void dropIdleTells() {
      this.whipTell = 0;
      this.reelTell = 0;
      this.reelPatience.clear();
      this.repelCloseTicks = 0;
      if (this.getPhase() != 0 && this.finalVerdictTell == 0 && !this.sweeping && this.sweepTell == 0 && this.gavelHold == 0) {
         this.setPhase((byte)0);
      }
   }

   private void tickRecess(ServerLevel level) {
      for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(18.0), this::isDefendant)) {
         if (this.distanceToSqr(player) <= 324.0) {
            this.callToOrder(level, player);
            return;
         }
      }
   }

   private void callToOrder(ServerLevel level, @Nullable Player challenger) {
      this.courtStage = 1;
      this.ceremonyTicks = 0;
      this.ceremonyForged = 0;
      if (challenger != null) {
         this.setTarget(challenger);
      }

      for (ServerPlayer viewer : level.players()) {
         if (viewer.distanceToSqr(this) <= 9216.0) {
            this.bossEvent.addPlayer(viewer);
         }
      }

      this.setPhase((byte)4);
      Fx.sound(level, this.position(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.HOSTILE, 1.8F, 0.5F);
   }

   private void tickCeremony(ServerLevel level) {
      this.tickCourtRune(level);
      int forgeAt = this.ceremonyTicks - 10;
      if (forgeAt >= 0 && forgeAt % 9 == 0) {
         int index = forgeAt / 9;
         if (index < 6) {
            this.forgeSeal(level, index);
            this.ceremonyForged = index + 1;
            Fx.sound(level, this.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.5F, 0.6F + (float)index * 0.09F);
            Fx.sound(level, this.position(), SoundEvents.ANVIL_USE, SoundSource.HOSTILE, 0.9F, 0.7F + (float)index * 0.09F);
         }
      }

      if (++this.ceremonyTicks >= 70) {
         this.openSession(level);
      }
   }

   private void openSession(ServerLevel level) {
      this.courtStage = 2;
      this.ceremonyTicks = 0;
      this.setPhase((byte)0);
      this.openVerse(ArbiterVerses.V3);

      while (this.ceremonyForged < 6) {
         this.forgeSeal(level, this.ceremonyForged++);
      }
   }

   private void cancelCeremonyIntoFight(ServerLevel level) {
      if (this.courtStage == 0) {
         this.callToOrder(level, null);
      }

      if (this.courtStage == 1) {
         Fx.sound(level, this.position(), SoundEvents.ANVIL_USE, SoundSource.HOSTILE, 1.6F, 0.45F);
         this.openSession(level);
      }
   }

   private void tickKneelStir(ServerLevel level) {
      long left = this.kneelEndsAt - level.getGameTime();
      if (left > 0L && left <= 40L) {
         if (left == 40L) {
            this.beginCourtVolley(level, 1 + this.getRandom().nextInt(2));
         }

         int period = left > 30L ? 8 : (left > 20L ? 6 : (left > 10L ? 4 : 2));
         if (left % (long)period == 0L) {
            float rising = 1.0F - (float)left / 40.0F;
            Fx.sound(level, this.position(), SoundEvents.CHAIN_STEP, SoundSource.HOSTILE, 0.9F + rising * 0.5F, 0.5F + rising * 0.35F);
            Fx.burst(level, JUDGE_VIOLET, this.position().add(0.0, 0.3, 0.0), 3, 1.6, 0.01);
         }
      }
   }

   private void acquireDefendant(ServerLevel level) {
      LivingEntity current = this.getTarget();
      if (!this.isDefendant(current) || !this.inCourt(current)) {
         Player best = null;
         double bestSq = Double.MAX_VALUE;

         for (Player player : this.courtPlayers(level)) {
            double dsq = this.distanceToSqr(player);
            if (dsq < bestSq) {
               bestSq = dsq;
               best = player;
            }
         }

         if (best != current) {
            this.setTarget(best);
         }
      }
   }

   private List<Player> courtPlayers(ServerLevel level) {
      return level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(30.0), p -> this.isDefendant(p) && this.inCourt(p));
   }

   private boolean isDefendant(@Nullable LivingEntity entity) {
      if (entity instanceof Player player && player.isAlive() && !player.isCreative() && !player.isSpectator()) {
         return true;
      }

      return false;
   }

   private boolean inCourt(LivingEntity entity) {
      return this.inCourt(entity.position());
   }

   private boolean inCourt(Vec3 p) {
      double dy = p.y - this.courtPost().y;
      if (!(dy > 24.0) && !(dy < -8.0)) {
         double flat = this.courtDistanceFlat(p);
         return flat <= 30.0;
      } else {
         return false;
      }
   }

   private Vec3 courtPost() {
      return this.courtCenter != null ? this.courtCenter : this.position();
   }

   private double courtDistanceFlat(Vec3 p) {
      Vec3 post = this.courtPost();
      double dx = p.x - post.x;
      double dz = p.z - post.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private void tickEmptyCourt() {
      if (++this.emptyCourtTicks >= 100) {
         this.heal(0.35F);
      }
   }

   private void tickCourtRune(ServerLevel level) {
      if (this.tickCount % 2 == 0) {
         Vec3 feet = this.position().add(0.0, 0.08, 0.0);
         Vec3 u = new Vec3(1.0, 0.0, 0.0);
         Vec3 v = new Vec3(0.0, 0.0, 1.0);
         double phase = (double)this.tickCount * 0.045;
         Fx.ring(level, JUDGE_VIOLET, feet, u, v, 2.7, 14, phase);
         Fx.ring(level, JUDGE_CYAN, feet, u, v, 3.3, 10, -phase * 0.7);
      }
   }

   private void tickWhipTell(ServerLevel level) {
      if (this.whipTell > 0) {
         if (--this.whipTell == 0) {
            this.fireWhip(level);
         } else {
            if (this.escalationPhase() >= 3 && this.whipTell == 18) {
               this.layerRootVolley(level);
            }

            if (this.whipTell % 2 == 0) {
               this.drawWhipTell(level);
            }
         }
      }
   }

   private void beginWhip(ServerLevel level, @Nullable Player target) {
      if (target != null) {
         this.noteTick(this.whippedAt, target.getUUID(), 1200);
      }

      Vec3 mark = target != null ? target.position() : this.position();
      Vec3 flat = new Vec3(mark.x - this.getX(), 0.0, mark.z - this.getZ());
      Vec3 dir = flat.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.getYRot()) : flat.normalize();
      double y = groundUnder(level, mark).y;
      this.whipStart = new Vec3(mark.x - dir.x * 1.5, y, mark.z - dir.z * 1.5);
      this.whipEnd = new Vec3(mark.x + dir.x * 4.0, y, mark.z + dir.z * 4.0);
      this.whipTell = 22;
      this.setPhase((byte)6);
      Fx.sound(level, this.position(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 1.5F, 0.5F);
      Vec3 side = this.whipSide();
      Vfx.beam(level, this.whipStart.add(side).add(0.0, 0.22, 0.0), this.whipEnd.add(side).add(0.0, 0.22, 0.0), 0.12, 11544319, 22);
      Vfx.beam(level, this.whipStart.subtract(side).add(0.0, 0.22, 0.0), this.whipEnd.subtract(side).add(0.0, 0.22, 0.0), 0.12, 11544319, 22);
      Vfx.beam(level, this.whipStart.add(0.0, 0.22, 0.0), this.whipEnd.add(0.0, 0.22, 0.0), 0.24, 11544319, 22);
      Vfx.pillar(level, this.whipStart, 0.22, 1.1, 11544319, 22);
      Vfx.pillar(level, this.whipEnd, 0.22, 1.1, 11544319, 22);
      this.drawWhipTell(level);
   }

   @Nullable
   private Player nextWhipDefendant(ServerLevel level) {
      Player best = null;
      int bestAt = Integer.MAX_VALUE;
      double bestSq = Double.MAX_VALUE;

      for (Player player : this.courtPlayers(level)) {
         double dsq = this.distanceToSqr(player);
         if (!(dsq > 676.0)) {
            int at = this.whippedAt.getOrDefault(player.getUUID(), Integer.MIN_VALUE);
            if (at < bestAt || at == bestAt && dsq < bestSq) {
               bestAt = at;
               bestSq = dsq;
               best = player;
            }
         }
      }

      return best;
   }

   private Vec3 whipSide() {
      Vec3 span = this.whipEnd.subtract(this.whipStart);
      Vec3 perp = new Vec3(-span.z, 0.0, span.x);
      return perp.lengthSqr() < 1.0E-6 ? Vec3.ZERO : perp.normalize().scale(1.3);
   }

   private void drawWhipTell(ServerLevel level) {
      Vec3 span = this.whipEnd.subtract(this.whipStart);
      int points = Math.max(4, (int)(span.length() / 0.7));

      for (int i = 0; i <= points; i++) {
         Vec3 p = this.whipStart.add(span.scale((double)i / (double)points));
         level.sendParticles(JUDGE_VIOLET, p.x, p.y + 0.12, p.z, 2, 0.65, 0.0, 0.65, 0.0);
      }

      Fx.burst(level, JUDGE_CYAN, this.whipStart.add(0.0, 0.15, 0.0), 2, 0.1, 0.01);
      Fx.burst(level, JUDGE_CYAN, this.whipEnd.add(0.0, 0.15, 0.0), 2, 0.1, 0.01);
   }

   private void fireWhip(ServerLevel level) {
      this.setPhase((byte)0);
      this.swing(InteractionHand.MAIN_HAND);
      Vec3 yoke = this.position().add(0.0, (double)this.getBbHeight() * 0.76, 0.0);
      ArbiterWhipEntity head = new ArbiterWhipEntity(AwakenedEntities.ARBITER_WHIP.get(), level);
      head.launch(this, yoke, this.whipEnd);
      level.addFreshEntity(head);
      this.whipLandIn = 6;
      Fx.sound(level, yoke, SoundEvents.CHAIN_FALL, SoundSource.HOSTILE, 1.8F, 0.5F);
   }

   private void tickWhipResolution(ServerLevel level) {
      if (this.whipLandIn > 0) {
         if (this.whipLandIn == 3) {
            Vec3 mid = this.whipStart.add(this.whipEnd).scale(0.5);
            Telegraphs.commitCue(level, mid, SoundEvents.CHAIN_BREAK, 1.7F);
            Telegraphs.dangerCloseLine(level, this.whipStart, this.whipEnd, 1.8);
         }

         if (--this.whipLandIn == 0) {
            this.landWhip(level);
         }
      }
   }

   private void landWhip(ServerLevel level) {
      Vec3 mid = this.whipStart.add(this.whipEnd).scale(0.5);
      Vfx.ring(level, mid.add(0.0, 0.25, 0.0), 2.6, 11544319, 8);
      Vfx.pillar(level, mid, 0.8, 2.0, 15244287, 6);
      Vfx.beam(level, this.whipStart.add(0.0, 0.12, 0.0), this.whipEnd.add(0.0, 0.12, 0.0), 0.85, 11544319, 16);
      Vec3 spanDir = this.whipEnd.subtract(this.whipStart);
      Vec3 side = new Vec3(-spanDir.z, 0.0, spanDir.x).normalize();

      for (int i = 0; i < 4; i++) {
         Vec3 at = this.whipStart.add(spanDir.scale(0.2 + 0.2 * (double)i)).add(0.0, 0.1, 0.0);
         Vec3 out = side.scale((double)(i % 2 == 0 ? 1 : -1) * (1.4 + 0.5 * (double)(i % 3)));
         Vfx.arc(level, at, at.add(out).add(spanDir.normalize().scale(0.6)), 0.09, 14257151, 10);
      }

      Fx.sound(level, mid, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5F, 0.5F);
      Fx.sound(level, mid, SoundEvents.CHAIN_HIT, SoundSource.HOSTILE, 1.4F, 0.45F);
      Fx.shake(level, mid, 16.0, 0.2F, 0, 8);
      Fx.groundImpact(level, mid, 2.5, true);
      Vec3 span = this.whipEnd.subtract(this.whipStart);
      int points = Math.max(4, (int)(span.length() / 0.5));

      for (int i = 0; i <= points; i++) {
         Vec3 p = this.whipStart.add(span.scale((double)i / (double)points));
         Fx.burst(level, JUDGE_VIOLET, p.add(0.0, 0.3, 0.0), 2, 0.2, 0.05);
      }

      AABB area = new AABB(this.whipStart, this.whipEnd).inflate(1.8, 2.0, 1.8);
      Vec3 along = spanDir.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(spanDir.x, 0.0, spanDir.z).normalize();

      for (Player player : level.getEntitiesOfClass(Player.class, area, p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
         if (!(distToSegment2D(player.position(), this.whipStart, this.whipEnd) > 1.3)) {
            Dmg.hurt(player, level, this.damageSources().mobAttack(this), this.scaleDamage(16.5F));
            player.push(along.x * 0.75, 0.28, along.z * 0.75);
            player.hurtMarked = true;
            this.struckByChain(player, 0.3F, 8);
         }
      }
   }

   private void struckByChain(Player victim, float shake, int fade) {
      victim.resetFallDistance();
      if (victim instanceof ServerPlayer served) {
         Vec3 at = victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0);
         Fx.soundOnlyFor(served, at, SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.HOSTILE, 0.9F, 1.2F);
         Fx.shakeFor(served, at, 12.0, shake, 0, fade);
      }
   }

   private void beginPendulum(ServerLevel level) {
      this.beginSweep(level, SWEEP_PENDULUM);
   }

   private boolean sweepRunning(ArbiterEntity.Sweep spec) {
      return this.sweep == spec && (this.sweepTell > 0 || this.sweeping);
   }

   private void beginSweep(ServerLevel level, ArbiterEntity.Sweep spec) {
      this.sweep = spec;
      this.sweepTell = spec.tellTicks();
      this.sweeping = false;
      this.sweepSwept = 0.0;
      this.sweepVolleys = 0;
      this.sweepAngle = this.getYRot();
      this.sweepFacing = this.sweepAngle;
      this.sweepBites.clear();
      this.sweepHeadA = this.spawnSweepHead(level, 0.0F);
      this.sweepHeadB = this.spawnSweepHead(level, 180.0F);
      this.setPhase((byte)7);
      float pitch = spec == SWEEP_PENDULUM ? 0.0F : 0.25F;
      Fx.sound(level, this.position(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.HOSTILE, 2.4F, 0.4F + pitch);
      Fx.sound(level, this.position(), SoundEvents.NETHERITE_BLOCK_FALL, SoundSource.HOSTILE, 1.8F, 0.5F + pitch);
   }

   private ArbiterWhipEntity spawnSweepHead(ServerLevel level, float offsetDeg) {
      ArbiterWhipEntity head = new ArbiterWhipEntity(AwakenedEntities.ARBITER_WHIP.get(), level);
      head.bindDriven(this, this.sweepHeadPos(offsetDeg), this.sweep.chainMode());
      head.setScriptTicks(this.sweep.tellTicks());
      level.addFreshEntity(head);
      return head;
   }

   private int sweepTellAge() {
      return this.sweep.tellTicks() - this.sweepTell;
   }

   private double sweepReach() {
      if (this.sweepTell <= 0) {
         return this.sweep.length();
      } else {
         double pay = Mth.clamp((double)this.sweepTellAge() / ((double)this.sweep.tellLand() * 0.85), 0.0, 1.0);
         double eased = 1.0 - (1.0 - pay) * (1.0 - pay) * (1.0 - pay);
         double from = Math.max(this.sweep.inner(), 0.5);
         return from + eased * (this.sweep.length() - from);
      }
   }

   private double sweepLift() {
      if (this.sweepTell > 0 && !(this.sweep.tellLift() <= 0.0)) {
         double fall = Mth.clamp(((double)this.sweepTellAge() - (double)this.sweep.tellLand() * 0.45) / ((double)this.sweep.tellLand() * 0.55), 0.0, 1.0);
         return this.sweep.tellLift() * (1.0 - fall * fall);
      } else {
         return 0.0;
      }
   }

   private Vec3 sweepHeadPos(float offsetDeg) {
      double a = Math.toRadians((double)(this.sweepAngle + offsetDeg));
      double radius = this.sweepReach();
      return new Vec3(this.getX() + Math.cos(a) * radius, this.getY() + this.sweep.height() + this.sweepLift(), this.getZ() + Math.sin(a) * radius);
   }

   private void moveSweepHeads() {
      if (this.sweepHeadA != null && this.sweepHeadA.isAlive()) {
         this.sweepHeadA.setPos(this.sweepHeadPos(0.0F));
      }

      if (this.sweepHeadB != null && this.sweepHeadB.isAlive()) {
         this.sweepHeadB.setPos(this.sweepHeadPos(180.0F));
      }
   }

   private double sweepTurns() {
      return this.sweep.turns() + (this.sweep == SWEEP_PENDULUM && this.escalationPhase() >= 3 ? 1.0 : 0.0);
   }

   private float sweepSpeed() {
      float ramp = (float)Mth.clamp(this.sweepSwept / (this.sweepTurns() * 360.0), 0.0, 1.0);
      float tier = (float)Math.pow(1.2F, (double)(this.escalationPhase() - 1));
      return Mth.lerp(ramp, this.sweep.degStart(), this.sweep.degEnd()) * tier;
   }

   private void tickSweep(ServerLevel level) {
      if (this.sweep != null) {
         if (this.sweepTell > 0) {
            this.tickSweepTell(level);
         } else if (this.sweeping) {
            float step = this.sweepSpeed();
            this.sweepAngle += step;
            this.sweepSwept += (double)step;
            this.sweepFacing += step * 0.35F;
            double fa = Math.toRadians((double)this.sweepFacing);
            this.getLookControl().setLookAt(this.getX() + Math.cos(fa) * 8.0, this.getEyeY(), this.getZ() + Math.sin(fa) * 8.0);
            this.moveSweepHeads();
            if (crossed(this.sweepSwept, (double)step, 45.0)) {
               if (this.sweepOnFloor()) {
                  this.eachSweepHead(level, SoundEvents.NETHERITE_BLOCK_STEP, 1.5F, 0.35F);
               } else {
                  this.eachSweepHead(level, (SoundEvent)SoundEvents.WIND_CHARGE_BURST.value(), 1.2F, 0.75F);
               }
            }

            if (this.sweep == SWEEP_PENDULUM
               && this.sweepVolleys < (this.layerTier() >= 3 ? 3 : 1)
               && crossed(this.sweepSwept, (double)step, 180.0)
               && this.layerRootVolley(level)) {
               this.sweepVolleys++;
            }

            if (this.repelCutsSweep && this.sweep == SWEEP_PENDULUM && crossed(this.sweepSwept, (double)step, 180.0)) {
               Fx.sound(level, this.position(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.HOSTILE, 1.8F, 0.45F);
               this.endSweep();
            } else {
               this.clipSweep(level);
               if (this.sweepSwept >= this.sweepTurns() * 360.0) {
                  Fx.sound(level, this.position(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.HOSTILE, 1.8F, 0.45F);
                  this.endSweep();
               }
            }
         }
      }
   }

   private static boolean crossed(double swept, double step, double every) {
      return (int)(swept / every) != (int)((swept - step) / every);
   }

   private void tickSweepTell(ServerLevel level) {
      this.moveSweepHeads();
      int age = this.sweepTellAge();
      int land = this.sweep.tellLand();
      if (age > 0 && age < land && age % 5 == 0) {
         this.eachSweepHead(level, SoundEvents.NETHERITE_BLOCK_STEP, 1.7F, 0.32F + 0.2F * (float)age / (float)land);
      }

      if (age == land) {
         if (this.sweepOnFloor()) {
            this.eachSweepHead(level, SoundEvents.NETHERITE_BLOCK_FALL, 1.9F, 0.35F);
            Fx.shake(level, this.position(), 22.0, 0.16F, 0, 6);
         } else {
            this.eachSweepHead(level, SoundEvents.NETHERITE_BLOCK_HIT, 1.4F, 0.55F);
         }
      }

      if (--this.sweepTell <= 0) {
         this.sweepTell = 0;
         this.sweeping = true;
         this.moveSweepHeads();
         Telegraphs.commitCue(level, this.sweepHeadPos(0.0F), SoundEvents.NETHERITE_BLOCK_PLACE, 0.5F);
         Telegraphs.commitCue(level, this.sweepHeadPos(180.0F), SoundEvents.NETHERITE_BLOCK_PLACE, 0.5F);
         Fx.sound(level, this.position(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.HOSTILE, 1.9F, 0.75F);
      }
   }

   private void eachSweepHead(ServerLevel level, SoundEvent sound, float volume, float pitch) {
      Fx.sound(level, this.sweepHeadPos(0.0F), sound, SoundSource.HOSTILE, volume, pitch);
      Fx.sound(level, this.sweepHeadPos(180.0F), sound, SoundSource.HOSTILE, volume, pitch);
   }

   private boolean sweepOnFloor() {
      return this.sweep.tellLift() > 0.0;
   }

   private void clipSweep(ServerLevel level) {
      Vec3 root = this.position();
      double bandLow = this.getY() + this.sweep.bandLow();
      double bandHigh = this.getY() + this.sweep.bandHigh();
      Vec3 headA = this.sweepHeadPos(0.0F);
      Vec3 headB = this.sweepHeadPos(180.0F);
      AABB area = this.getBoundingBox().inflate(this.sweep.length() + 1.0, 2.5, this.sweep.length() + 1.0);

      for (Player player : level.getEntitiesOfClass(Player.class, area, p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
         if (!(player.getY() > bandHigh) && !(player.getY() + (double)player.getBbHeight() < bandLow)) {
            Vec3 flat = player.position().subtract(root);
            if (!(Math.sqrt(flat.x * flat.x + flat.z * flat.z) < this.sweep.inner()) && !this.biteSweep(level, player, root, headA)) {
               this.biteSweep(level, player, root, headB);
            }
         }
      }
   }

   private boolean biteSweep(ServerLevel level, Player player, Vec3 root, Vec3 head) {
      if (distToSegment2D(player.position(), root, head) > this.sweep.halfWidth()) {
         return false;
      } else {
         Double last = this.sweepBites.get(player.getUUID());
         if (last != null && this.sweepSwept - last < this.sweep.rebiteDeg()) {
            return false;
         } else {
            this.sweepBites.put(player.getUUID(), this.sweepSwept);
            Dmg.hurt(player, level, this.damageSources().mobAttack(this), this.scaleDamage(this.sweep.damage()));
            Vec3 out = player.position().subtract(root);
            Vec3 radial = new Vec3(out.x, 0.0, out.z);
            radial = radial.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : radial.normalize();
            Vec3 along = new Vec3(-radial.z, 0.0, radial.x);
            player.push(
               along.x * this.sweep.carry() + radial.x * this.sweep.carryOut(),
               this.sweep.carryUp(),
               along.z * this.sweep.carry() + radial.z * this.sweep.carryOut()
            );
            player.hurtMarked = true;
            Fx.sound(level, player.position(), SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.HOSTILE, 1.5F, 0.55F);
            Fx.burst(level, JUDGE_VIOLET, player.position().add(0.0, 0.4, 0.0), 8, 0.35, 0.05);
            this.struckByChain(player, 0.26F, 7);
            return true;
         }
      }
   }

   private void endSweep() {
      if (this.sweep == SWEEP_REPEL) {
         this.repelCooldown = 160;
      }

      if (this.sweepHeadA != null) {
         this.sweepHeadA.discard();
         this.sweepHeadA = null;
      }

      if (this.sweepHeadB != null) {
         this.sweepHeadB.discard();
         this.sweepHeadB = null;
      }

      this.sweepTell = 0;
      this.sweeping = false;
      this.sweepSwept = 0.0;
      this.sweep = null;
      this.sweepBites.clear();
      if (this.getPhase() == 7) {
         this.setPhase((byte)0);
      }
   }

   private void tickRootBeat(ServerLevel level) {
      if (this.rootFollowThrough > 0 && --this.rootFollowThrough == 0 && this.getPhase() == 4) {
         this.setPhase((byte)0);
      }

      this.tickRootTell(level);
   }

   private void tickRootTell(ServerLevel level) {
      if (this.rootMarks.isEmpty()) {
         this.tickRootWarning(level, List.of());
      } else {
         ArbiterEntity.RootMark soonest = null;
         List<Vec3> dangerNow = null;
         List<ArbiterEntity.RootMark> live = new ArrayList<>(this.rootMarks.size());
         Iterator<ArbiterEntity.RootMark> it = this.rootMarks.iterator();

         while (it.hasNext()) {
            ArbiterEntity.RootMark mark = it.next();
            if (--mark.tell <= 0) {
               this.burstMark(level, mark);
               it.remove();
            } else {
               live.add(mark);
               Telegraphs.zoneMarker(level, mark.at, 2.2, 11544319, mark.tell, mark.total, false);
               if (mark.tell == 3) {
                  Telegraphs.commitCue(level, mark.at, SoundEvents.POINTED_DRIPSTONE_LAND, 0.8F);
                  this.spawnRootHead(level, mark.at, mark.standTicks, mark.solid);
               }

               if (mark.tell == 8) {
                  if (dangerNow == null) {
                     dangerNow = new ArrayList<>();
                  }

                  dangerNow.add(mark.at);
               }

               if (soonest == null || mark.tell < soonest.tell) {
                  soonest = mark;
               }
            }
         }

         if (soonest != null) {
            Telegraphs.countdownClick(level, soonest.at, soonest.tell, soonest.total);
         }

         if (dangerNow != null) {
            Telegraphs.dangerCloseAny(level, dangerNow, 2.2);
         }

         this.tickRootWarning(level, live);
      }
   }

   private void tickRootWarning(ServerLevel level, List<ArbiterEntity.RootMark> live) {
      Set<UUID> inside = null;
      if (!live.isEmpty()) {
         double reach = 2.5;
         boolean resend = level.getGameTime() % 10L == 0L;

         for (ServerPlayer player : level.getPlayers(p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            int soonest = Integer.MAX_VALUE;

            for (ArbiterEntity.RootMark mark : live) {
               if (player.getBoundingBox().intersects(new AABB(mark.at, mark.at).inflate(reach, 2.0, reach))) {
                  double dx = player.getX() - mark.at.x;
                  double dz = player.getZ() - mark.at.z;
                  if (dx * dx + dz * dz <= reach * reach) {
                     soonest = Math.min(soonest, mark.tell);
                  }
               }
            }

            if (soonest != Integer.MAX_VALUE) {
               if (inside == null) {
                  inside = new HashSet<>();
               }

               inside.add(player.getUUID());
               if (this.rootWarned.add(player.getUUID()) || resend) {
                  PacketDistributor.sendToPlayer(player, new ArbiterRootWarnPayload(true, soonest), new CustomPacketPayload[0]);
               }
            }
         }
      }

      if (!this.rootWarned.isEmpty()) {
         Iterator<UUID> it = this.rootWarned.iterator();

         while (it.hasNext()) {
            UUID id = it.next();
            if (inside == null || !inside.contains(id)) {
               it.remove();
               if (level.getPlayerByUUID(id) instanceof ServerPlayer left) {
                  PacketDistributor.sendToPlayer(left, new ArbiterRootWarnPayload(false, 0), new CustomPacketPayload[0]);
               }
            }
         }
      }
   }

   private int layLine(ServerLevel level, Vec3 from, Vec3 heading) {
      int laid = 0;

      for (int i = 0; i < 5 && this.rootMarks.size() < 12; i++) {
         if (this.addRootMark(level, from.add(heading.scale((double)i * 2.6)), 1.0, 24 + i * 5, 1.0F, true)) {
            laid++;
         }
      }

      return laid;
   }

   private Vec3 lineHeading(Entity who) {
      Vec3 step = new Vec3(who.getX() - who.xo, 0.0, who.getZ() - who.zo);
      return step.lengthSqr() >= 0.0025000000000000005 ? step.normalize() : this.awayFromBench(who.position());
   }

   private Vec3 awayFromBench(Vec3 at) {
      Vec3 out = new Vec3(at.x - this.getX(), 0.0, at.z - this.getZ());
      return out.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.getYRot()) : out.normalize();
   }

   private void lineCrack(ServerLevel level, Vec3 at) {
      Fx.sound(level, at, SoundEvents.DEEPSLATE_BRICKS_BREAK, SoundSource.HOSTILE, 2.0F, 0.62F);
   }

   private boolean beginRootEruption(ServerLevel level, @Nullable LivingEntity target) {
      return this.castRoots(level, target, true);
   }

   private boolean castRoots(ServerLevel level, @Nullable LivingEntity target, boolean bar) {
      if (!this.rootMarks.isEmpty()) {
         return false;
      } else {
         this.applyRootMode(level);
         if (!bar) {
            this.rootCaged = false;
         }

         List<Player> chased = new ArrayList<>();

         for (Player player : this.courtPlayers(level)) {
            if (chased.size() >= 3) {
               break;
            }

            if (this.distanceToSqr(player) <= 576.0) {
               chased.add(player);
            }
         }

         this.rootStruck = new HashSet<>();
         List<Vec3> opened = new ArrayList<>();
         if (chased.isEmpty()) {
            if (target == null || this.distanceToSqr(target) > 576.0) {
               return false;
            }

            Vec3 fixed = target.position();
            if (this.layLine(level, fixed, this.awayFromBench(fixed)) > 0) {
               opened.add(fixed);
            }
         } else {
            for (Player player : chased) {
               if (this.layLine(level, player.position(), this.lineHeading(player)) > 0) {
                  opened.add(player.position());
               }
            }
         }

         if (opened.isEmpty()) {
            return false;
         } else {
            this.armReport(level, opened.get(0));

            for (Vec3 at : opened) {
               this.lineCrack(level, at);
            }

            if (this.rootCaged) {
               for (Vec3 at : opened) {
                  this.cageAround(level, at);
               }
            }

            this.addCourtRoots(
               level, ArbiterRootVariants.courtRoots(bar ? this.escalationPhase() : this.layerTier(), Math.max(1, chased.size()), this.rootCaged)
            );
            if (bar) {
               this.rootIsBar = true;
               this.setPhase((byte)4);
            }

            return true;
         }
      }
   }

   private boolean layerRootVolley(ServerLevel level) {
      return this.layerTier() >= 2 && this.castRoots(level, this.getTarget(), false);
   }

   private int layerTier() {
      return this.escalationPhase();
   }

   private void armReport(ServerLevel level, Vec3 at) {
      Fx.sound(level, at, (SoundEvent)SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.HOSTILE, 1.3F, 0.5F);
      Fx.sound(level, at, SoundEvents.ANCIENT_DEBRIS_BREAK, SoundSource.HOSTILE, 0.9F, 0.6F);
   }

   private boolean addRootMark(ServerLevel level, Vec3 about, double minSpacing, int tell, float volume, boolean wave) {
      Optional<Vec3> floor = AimUtil.groundedPositionIfFound(level, about);
      if (floor.isEmpty()) {
         return false;
      } else {
         Vec3 p = floor.get();

         for (ArbiterEntity.RootMark taken : this.rootMarks) {
            if (!wave || !taken.wave) {
               double dx = p.x - taken.at.x;
               double dz = p.z - taken.at.z;
               if (dx * dx + dz * dz < minSpacing * minSpacing) {
                  return false;
               }
            }
         }

         this.rootMarks.add(new ArbiterEntity.RootMark(p, tell, volume, this.rootStandTicks, this.rootSolid, wave, this.rootStruck));
         return true;
      }
   }

   private void applyRootMode(ServerLevel level) {
      ArbiterRootVariants.Mode mode = ArbiterRootVariants.mode(level.getServer());
      boolean forest = ArbiterRootVariants.forest(level.getServer()) && this.escalationPhase() >= 2;
      this.rootCaged = mode.cage();
      this.rootSolid = mode.stand();
      this.rootStandTicks = forest ? 12000 : (mode.stand() ? 120 : 12);
   }

   private void addCourtRoots(ServerLevel level, int count) {
      count = Math.min(count, 12 - this.rootMarks.size());
      if (count > 0) {
         int before = this.rootMarks.size();
         double inner = 4.0;
         double outer = 16.0;
         Vec3 post = this.courtPost();
         int laid = 0;

         for (int tries = 0; laid < count && tries < count * 8; tries++) {
            double a = this.getRandom().nextDouble() * Math.PI * 2.0;
            double r = inner + this.getRandom().nextDouble() * (outer - inner);
            Vec3 about = post.add(Math.cos(a) * r, 0.0, Math.sin(a) * r);
            if (!this.insideSafeCell(about) && this.addRootMark(level, about, 3.0, 24, 0.7F, false)) {
               laid++;
            }
         }

         List<ArbiterEntity.RootMark> court = new ArrayList<>(this.rootMarks.subList(before, this.rootMarks.size()));
         court.sort(Comparator.comparingDouble(mark -> mark.at.distanceToSqr(this.position())));

         for (int i = 0; i < court.size(); i++) {
            court.get(i).fuse(24 + 5 * i);
         }
      }
   }

   private boolean beginCourtVolley(ServerLevel level, int count) {
      if (this.escalationPhase() < 2) {
         return false;
      } else {
         this.applyRootMode(level);
         this.rootCaged = false;
         this.rootStruck = new HashSet<>();
         int before = this.rootMarks.size();
         this.addCourtRoots(level, count);
         if (this.rootMarks.size() == before) {
            return false;
         } else {
            this.armReport(level, this.rootMarks.get(this.rootMarks.size() - 1).at);
            return true;
         }
      }
   }

   private void cageAround(ServerLevel level, Vec3 centre) {
      Vec3 out = centre.subtract(this.position());
      double door = out.x * out.x + out.z * out.z < 1.0E-4 ? Math.toRadians((double)(this.getYRot() + 90.0F)) : Math.atan2(out.z, out.x);
      int slots = 8;
      int emptyPairs = (slots - 6) / 2;
      double step = (Math.PI * 2) / (double)slots;

      for (int i = 0; i < slots; i++) {
         if (Math.min(i, slots - 1 - i) >= emptyPairs) {
            double a = door + ((double)i + 0.5) * step;
            this.addRootMark(level, centre.add(Math.cos(a) * 3.0, 0.0, Math.sin(a) * 3.0), 1.5, 24, 0.7F, false);
         }
      }
   }

   private void spawnRootHead(ServerLevel level, Vec3 p, int standTicks, boolean solid) {
      ArbiterWhipEntity rootHead = new ArbiterWhipEntity(AwakenedEntities.ARBITER_WHIP.get(), level);
      rootHead.launchRoot(this, p, standTicks, solid);
      level.addFreshEntity(rootHead);
      this.rootHeads.removeIf(h -> !h.isAlive());
      this.rootHeads.add(rootHead);
      this.capStandingRoots();
   }

   private void capStandingRoots() {
      List<ArbiterWhipEntity> standing = new ArrayList<>();

      for (ArbiterWhipEntity head : this.rootHeads) {
         if (head.isAlive() && head.isStandingRoot()) {
            standing.add(head);
         }
      }

      for (int i = 0; i < standing.size() - 9; i++) {
         ArbiterWhipEntity oldest = standing.get(i);
         if (oldest.tickCount == 0) {
            break;
         }

         oldest.sinkRoot();
      }
   }

   private void sinkRoots() {
      for (ArbiterWhipEntity head : this.rootHeads) {
         if (head.isAlive()) {
            head.sinkRoot();
         }
      }

      this.rootHeads.clear();
   }

   private void rootBurst(ServerLevel level, Vec3 p, float volume, int standTicks) {
      Fx.Floor floor = Fx.floorState(level, p.x, p.y, p.z);
      double top = floor == null ? p.y : floor.topY();
      Vec3 hole = new Vec3(p.x, top, p.z);
      if (floor != null) {
         Fx.debrisFan(level, new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), hole.add(0.0, 0.1, 0.0), 18, 50.0, 85.0, 0.35, 0.8);
      }

      Fx.groundImpact(level, p, 2.2, false);
      Fx.ring(
         level,
         ParticleTypes.LARGE_SMOKE,
         hole.add(0.0, 0.25, 0.0),
         new Vec3(1.0, 0.0, 0.0),
         new Vec3(0.0, 0.0, 1.0),
         1.2100000000000002,
         10,
         this.getRandom().nextDouble() * Math.PI
      );
      Fx.ring(
         level,
         ParticleTypes.CAMPFIRE_COSY_SMOKE,
         hole.add(0.0, 0.15, 0.0),
         new Vec3(1.0, 0.0, 0.0),
         new Vec3(0.0, 0.0, 1.0),
         2.09,
         12,
         this.getRandom().nextDouble() * Math.PI
      );
      Vfx.pillar(level, hole, 2.2, 1.4, 15244287, 5);
      Vfx.ring(level, hole.add(0.0, 0.05, 0.0), 2.2, 11544319, 7);
      Fx.burst(level, JUDGE_VIOLET, p.add(0.0, 0.6, 0.0), 10, 0.4, 0.08);
      int standing = Math.max(6, 4 + standTicks + 6 - 3 + 2);
      if (floor != null) {
         for (int i = 0; i < 7; i++) {
            double a = (Math.PI * 2) * (double)i / 7.0 + this.getRandom().nextDouble() * 0.7;
            double size = 0.3 + this.getRandom().nextDouble() * 0.2;
            double out = 0.14 + this.getRandom().nextDouble() * 0.13;
            double up = 0.3 + this.getRandom().nextDouble() * 0.2;
            Vec3 start = hole.add(Math.cos(a) * 0.25, 0.2 + size * 0.5, Math.sin(a) * 0.25);
            Vec3 vel = new Vec3(Math.cos(a) * out, up, Math.sin(a) * out);
            int life = i < 2 ? standing + 60 : 28 + this.getRandom().nextInt(16);
            Vfx.blockThrown(
               level,
               start,
               floor.state(),
               size,
               size * 0.72,
               size,
               life,
               this.getRandom().nextFloat() * 360.0F,
               vel,
               top + size * 0.36,
               (this.getRandom().nextFloat() - 0.5F) * 44.0F
            );
         }
      }

      Vfx.pit(level, hole.add(0.0, 0.012, 0.0), 0.66, 3807829, standing);
      Vfx.scorch(level, hole.add(0.0, 0.02, 0.0), 1.9800000000000002, 6171526, standing + 60);
      Fx.sound(level, p, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.2F * volume, 0.5F);
   }

   private void burstMark(ServerLevel level, ArbiterEntity.RootMark mark) {
      if (this.rootIsBar) {
         this.rootIsBar = false;
         this.rootFollowThrough = this.getPhase() == 4 ? 6 : 0;
      }

      Vec3 p = mark.at;
      this.rootBurst(level, p, mark.volume, mark.standTicks);
      Fx.shake(level, p, 8.8, 0.22F, 0, 8);

      for (Player player : level.getEntitiesOfClass(
         Player.class, new AABB(p, p).inflate(2.2, 2.0, 2.2), pl -> pl.isAlive() && !pl.isCreative() && !pl.isSpectator()
      )) {
         double dx = player.getX() - p.x;
         double dz = player.getZ() - p.z;
         if (!(dx * dx + dz * dz > 4.840000000000001) && mark.struck.add(player.getUUID())) {
            if (!Dmg.hurt(player, level, this.damageSources().mobAttack(this), this.scaleDamage(11.5F))) {
               mark.struck.remove(player.getUUID());
            } else {
               player.push(0.0, 0.67, 0.0);
               player.hurtMarked = true;
               this.struckByChain(player, 0.28F, 8);
            }
         }
      }
   }

   private void tickReelAim(ServerLevel level) {
      if (this.reelTell > 0) {
         if (!this.withdrawReel(level)) {
            this.aimReel();
            this.drawReelTell(level);
            if (--this.reelTell == 0) {
               this.fireReel(level);
            }
         }
      } else {
         if (this.reelCooldown > 0) {
            this.reelCooldown--;
         }

         Player worst = null;
         int worstTicks = 0;
         List<Player> court = this.courtPlayers(level);

         for (Player player : court) {
            double flat = this.courtDistanceFlat(player.position());
            int ticks = this.reelPatience.getOrDefault(player.getUUID(), 0);
            if (flat > 18.0) {
               ticks++;
            } else if (flat < 14.0) {
               ticks = 0;
            }

            this.reelPatience.put(player.getUUID(), ticks);
            if (ticks > worstTicks) {
               worstTicks = ticks;
               worst = player;
            }
         }

         if (this.reelPatience.size() > court.size()) {
            Set<UUID> present = new HashSet<>();

            for (Player player : court) {
               present.add(player.getUUID());
            }

            this.reelPatience.keySet().retainAll(present);
         }

         if (this.reelCooldown <= 0 && worst != null && worstTicks >= 70 && !this.beatBusy()) {
            Player camper = worst;
            this.offerBeat(ArbiterEntity.Beat.REEL, () -> this.beginReel(level, camper));
         }
      }
   }

   private void beginReel(ServerLevel level, @Nullable Player victim) {
      this.reelTell = 30;
      this.reelVictim = victim;
      this.reelMark = victim != null ? victim.position() : this.position();
      this.reelSnapshot = this.reelAimPoint();
      this.setPhase((byte)8);
      Fx.sound(level, this.position(), (SoundEvent)SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.HOSTILE, 3.0F, 0.6F);
      if (this.reelVictim != null && victim instanceof ServerPlayer marked) {
         Fx.soundOnlyFor(
            marked, marked.position().add(0.0, (double)marked.getBbHeight() * 0.5, 0.0), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.2F, 1.9F
         );
      }

      this.drawReelTell(level);
   }

   private void drawReelTell(ServerLevel level) {
      Telegraphs.trackingZoneMarker(level, this.reelMark, 1.8, 11544319, this.reelTell, 30);
      if ((30 - this.reelTell) % 5 == 0) {
         Vfx.beam(level, this.reelHand(), this.reelAimPoint(), 0.14, 15244287, Math.min(6, Math.max(2, this.reelTell + 1)));
      }

      if (this.reelTell == 3) {
         Telegraphs.commitCue(level, this.reelMark, (SoundEvent)SoundEvents.TRIDENT_RIPTIDE_1.value(), 1.3F);
         Telegraphs.dangerClose(level, this.reelMark, 1.8);
      }
   }

   private boolean withdrawReel(ServerLevel level) {
      Player victim = this.reelVictim;
      if (this.reelBodyHere(victim) && !(this.courtDistanceFlat(victim.position()) > 18.0)) {
         this.reelTell = 0;
         this.setPhase((byte)0);
         this.reelPatience.put(victim.getUUID(), 0);
         Fx.sound(level, this.position(), SoundEvents.TRIDENT_RETURN, SoundSource.HOSTILE, 1.1F, 0.75F);
         Fx.burst(level, JUDGE_CYAN, this.reelHand(), 6, 0.25, 0.02);
         return true;
      } else {
         return false;
      }
   }

   private void aimReel() {
      Player victim = this.reelVictim;
      if (this.reelBodyHere(victim)) {
         this.reelMark = victim.position();
      }
   }

   private boolean reelBodyHere(@Nullable Player victim) {
      return victim != null && !victim.isRemoved() && this.isDefendant(victim) && victim.level() == this.level();
   }

   private Vec3 reelAimPoint() {
      Player victim = this.reelVictim;
      return this.reelMark.add(0.0, (double)(victim != null ? victim.getBbHeight() : 1.8F) * 0.5, 0.0);
   }

   private void fireReel(ServerLevel level) {
      this.setPhase((byte)0);
      this.aimReel();
      this.reelSnapshot = this.reelAimPoint();
      Vec3 hand = this.reelHand();
      this.reelHead = new ArbiterWhipEntity(AwakenedEntities.ARBITER_WHIP.get(), level);
      this.reelHead.bindDriven(this, hand, (byte)3);
      this.reelFlyTotal = reelFlyTicks(hand.distanceTo(this.reelSnapshot));
      this.reelHead.setScriptTicks(this.reelFlyTotal);
      level.addFreshEntity(this.reelHead);
      this.reelFlyIn = 40;
      if (this.reelVictim != null) {
         this.reelPatience.put(this.reelVictim.getUUID(), 0);
      }

      this.reelCooldown = 240;
      Fx.sound(level, this.position(), (SoundEvent)SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 3.0F, 0.7F);
   }

   private Vec3 reelHand() {
      return this.position().add(0.0, (double)this.getBbHeight() * 0.76, 0.0);
   }

   private static int reelFlyTicks(double distance) {
      return Mth.clamp((int)Math.ceil(distance / 1.6), 8, 40);
   }

   private static int reelDragTicksFor(double distance) {
      int needed = (int)Math.ceil((distance - 8.0) / 0.33);
      return Mth.clamp(needed, 26, 60);
   }

   private void tickReelDrag(ServerLevel level) {
      if (this.reelFlyIn > 0) {
         this.tickReelFlight(level);
      } else if (this.reelDragTicks > 0) {
         this.reelDragTicks--;
         Player victim = this.reelVictim;
         if (this.reelBodyHere(victim)
            && this.reelHead != null
            && this.reelHead.isAlive()
            && !(this.courtPost().distanceToSqr(victim.position()) < 64.0)
            && this.reelDragTicks != 0) {
            this.reelHead.setPos(victim.position().add(0.0, (double)victim.getBbHeight() * 0.55, 0.0));
            if (this.reelDragTicks % 6 == 0) {
               Fx.sound(level, this.reelHead.position(), SoundEvents.TRIDENT_RETURN, SoundSource.HOSTILE, 0.9F, 0.55F);
            }

            if (this.reelDragTicks % 2 == 0) {
               Vec3 pull = this.courtPost().subtract(victim.position());
               Vec3 flat = new Vec3(pull.x, 0.0, pull.z);
               if (flat.lengthSqr() > 1.0E-4) {
                  Vec3 v = flat.normalize().scale(0.3);
                  victim.push(v.x, pull.y < -1.0 ? -0.12 : 0.04, v.z);
                  victim.hurtMarked = true;
               }
            }
         } else {
            this.resetReel();
         }
      }
   }

   private void tickReelFlight(ServerLevel level) {
      this.reelFlyIn--;
      if (this.reelHead != null && this.reelHead.isAlive()) {
         Player victim = this.reelVictim;
         boolean chasing = this.reelBodyHere(victim) && this.inCourt(victim);
         Vec3 aim = chasing ? victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0) : this.reelSnapshot;
         Vec3 at = this.reelHead.position();
         Vec3 span = aim.subtract(at);
         double gap = span.length();
         Vec3 next = gap <= 1.6 ? aim : at.add(span.scale(1.6 / gap));
         this.reelHead.setPos(next);
         if (chasing && next.distanceToSqr(aim) <= 3.24) {
            this.biteReel(level, victim, next);
         } else if (!chasing && gap <= 1.6) {
            this.missReel(level, next);
         } else {
            if (this.reelFlyIn <= 0) {
               this.missReel(level, next);
            }
         }
      } else {
         this.resetReel();
      }
   }

   private void biteReel(ServerLevel level, Player victim, Vec3 at) {
      this.reelDragTicks = reelDragTicksFor(this.courtPost().distanceTo(victim.position()));
      this.reelHead.rideOn(victim);
      Fx.sound(level, at, SoundEvents.TRIDENT_HIT, SoundSource.HOSTILE, 1.6F, 0.6F);
      Vfx.sphere(level, at, 0.8, 15244287, 5);
      this.struckByChain(victim, 0.2F, 8);
   }

   private void missReel(ServerLevel level, Vec3 at) {
      Fx.burst(level, JUDGE_VIOLET, at, 8, 0.3, 0.03);
      Fx.sound(level, at, SoundEvents.TRIDENT_RETURN, SoundSource.HOSTILE, 1.2F, 0.9F);
      this.resetReel();
   }

   private void resetReel() {
      if (this.reelHead != null) {
         this.reelHead.discard();
         this.reelHead = null;
      }

      this.reelVictim = null;
      this.reelFlyIn = 0;
      this.reelDragTicks = 0;
   }

   private void tickRepel(ServerLevel level) {
      if (this.repelCooldown > 0 && !this.sweepRunning(SWEEP_REPEL)) {
         this.repelCooldown--;
      }

      boolean hugged = !level.getEntitiesOfClass(
            Player.class, this.getBoundingBox().inflate(5.0), p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && this.flatDistance(p.position()) < 5.0
         )
         .isEmpty();
      this.repelCloseTicks = hugged ? this.repelCloseTicks + 1 : Math.max(0, this.repelCloseTicks - 2);
      if (this.repelCooldown > 0 || this.repelCloseTicks < 35) {
         this.repelCutsSweep = false;
      } else if (this.beatBusy()) {
         this.repelCutsSweep = this.sweepRunning(SWEEP_PENDULUM);
      } else {
         this.repelCutsSweep = false;
         this.offerBeat(ArbiterEntity.Beat.REPEL, () -> this.beginRepel(level));
      }
   }

   private void beginRepel(ServerLevel level) {
      this.repelCloseTicks = 0;
      this.repelCutsSweep = false;
      this.beginSweep(level, SWEEP_REPEL);
   }

   private void tickFinalVerdict(ServerLevel level) {
      if (this.finalVerdictTell > 0) {
         this.drawFinalVerdictTell(level);
         if (this.finalVerdictTell == 3) {
            Telegraphs.commitCue(level, this.finalVerdictCenter, SoundEvents.WARDEN_SONIC_BOOM, 0.6F);
         }

         if (--this.finalVerdictTell == 0) {
            this.fireFinalVerdict(level);
         }
      }
   }

   private void beginFinalVerdict(ServerLevel level) {
      this.finalVerdictArmed = false;
      this.finalVerdictTell = 70;
      this.finalVerdictCenter = this.position();
      this.placeVerdictCells(level);
      this.raiseVerdictRim(level);
      this.setPhase((byte)6);
      Fx.sound(level, this.position(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0F, 0.7F);
      Fx.sound(level, this.position(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.8F, 0.4F);
      Fx.sound(level, this.position(), SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.HOSTILE, 2.4F, 0.35F);
   }

   private void raiseVerdictRim(ServerLevel level) {
      double step = Math.PI / 3;

      for (int i = 0; i < 12; i++) {
         boolean lock = i % 2 == 0;
         double a = (double)i * step * 0.5;
         Vec3 at = this.finalVerdictCenter.add(Math.cos(a) * 12.0, 0.0, Math.sin(a) * 12.0);
         Optional<Vec3> ground = AimUtil.groundedPositionIfFound(level, at);
         if (!ground.isEmpty()) {
            Vfx.pillar(level, ground.get(), lock ? 0.5 : 0.3, lock ? 5.0 : 3.2, lock ? 15244287 : 11544319, 80);
         }
      }
   }

   private void placeVerdictCells(ServerLevel level) {
      this.finalVerdictSafeCells.clear();
      double step = Math.PI * 2.0 / 3.0;
      List<Player> court = this.courtPlayers(level);
      double rotation = Math.toRadians((double)this.getYRot());
      double bestWorst = Double.MAX_VALUE;

      for (Player candidate : court) {
         double bearing = Math.atan2(candidate.getZ() - this.finalVerdictCenter.z, candidate.getX() - this.finalVerdictCenter.x);
         double worst = 0.0;

         for (Player player : court) {
            worst = Math.max(worst, this.runToNearestCell(player, bearing, step));
         }

         if (worst < bestWorst) {
            bestWorst = worst;
            rotation = bearing;
         }
      }

      for (int i = 0; i < 3; i++) {
         double a = rotation + (double)i * step;
         this.finalVerdictSafeCells.add(this.finalVerdictCenter.add(Math.cos(a) * 6.0, 0.0, Math.sin(a) * 6.0));
      }
   }

   private double runToNearestCell(Player player, double rot, double step) {
      double best = Double.MAX_VALUE;

      for (int i = 0; i < 3; i++) {
         double a = rot + (double)i * step;
         double dx = player.getX() - (this.finalVerdictCenter.x + Math.cos(a) * 6.0);
         double dz = player.getZ() - (this.finalVerdictCenter.z + Math.sin(a) * 6.0);
         best = Math.min(best, Math.sqrt(dx * dx + dz * dz));
      }

      return best;
   }

   private void drawFinalVerdictTell(ServerLevel level) {
      double edge = Math.max(0.6, 12.0 * (1.0 - (double)this.finalVerdictTell / 70.0));
      if (this.finalVerdictTell % 4 == 0) {
         this.layVerdictCracks(level, edge);
      }

      if (this.finalVerdictTell % 2 == 0) {
         Vec3 floor = this.finalVerdictCenter.add(0.0, 0.25, 0.0);
         Vfx.ring(level, floor, edge, 15244287, 4);
         Vfx.ring(level, floor, edge * 0.86, 15244287, 4);
         Vfx.ring(level, floor, edge * 0.72, 11544319, 4);
         Vfx.ring(level, floor, edge * 0.4, 11544319, 4);
         level.sendParticles(JUDGE_VIOLET, floor.x, floor.y, floor.z, 10, edge * 0.5, 0.02, edge * 0.5, 0.0);
         if (!this.finalVerdictSafeCells.isEmpty()) {
            Telegraphs.countdownClick(level, this.finalVerdictSafeCells.get(0), this.finalVerdictTell, 70);
         }

         for (Vec3 cell : this.finalVerdictSafeCells) {
            Telegraphs.zoneMarker(level, cell, 2.2, 1630463, this.finalVerdictTell, 70, false);
            Vfx.pillar(level, cell, 0.45, 3.2, 10220543, 4);
         }
      }
   }

   private void layVerdictCracks(ServerLevel level, double edge) {
      for (int i = 0; i < 2; i++) {
         double a = this.getRandom().nextDouble() * Math.PI * 2.0;
         double r = Math.max(0.4, edge - this.getRandom().nextDouble() * 2.6);
         Vec3 at = this.finalVerdictCenter.add(Math.cos(a) * r, 0.0, Math.sin(a) * r);
         if (!this.insideSafeCell(at)) {
            Optional<Vec3> ground = AimUtil.groundedPositionIfFound(level, at);
            if (!ground.isEmpty()) {
               Vec3 on = ground.get().add(0.0, 0.02, 0.0);
               Vfx.scorch(level, on, 2.6 * (0.7 + this.getRandom().nextDouble() * 0.5), 3805264, this.finalVerdictTell + 60);
               level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, on.x, on.y + 0.1, on.z, 3, 0.5, 0.05, 0.5, 0.02);
               level.sendParticles(JUDGE_VIOLET, on.x, on.y + 0.1, on.z, 6, 0.7, 0.1, 0.7, 0.01);
            }
         }
      }
   }

   private void fireFinalVerdict(ServerLevel level) {
      this.setPhase((byte)0);
      Vec3 floor = groundUnder(level, this.finalVerdictCenter).add(0.0, 0.04, 0.0);
      Vfx.splat(level, floor, 12.0, 15244287, 8, 0.0F, 0.0F);
      Vfx.scorch(level, floor.add(0.0, -0.02, 0.0), 11.76, 3805264, 60);
      Vfx.dome(level, floor.add(0.0, 0.06, 0.0), 12.0, 11544319, 14);
      Vfx.ring(level, this.finalVerdictCenter.add(0.0, 0.4, 0.0), 12.0, 15244287, 12);
      Vfx.sphere(level, this.finalVerdictCenter.add(0.0, 1.0, 0.0), 4.0, 11544319, 8);
      Fx.ring(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, floor, new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), 12.0, 48, 0.06);
      Fx.disk(level, JUDGE_VIOLET, floor, 9.600000000000001);
      Fx.shake(level, this.finalVerdictCenter, 24.0, 0.45F, 0, 18);
      Fx.sound(level, this.finalVerdictCenter, (SoundEvent)SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.HOSTILE, 2.0F, 0.5F);
      Fx.sound(level, this.finalVerdictCenter, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.6F, 0.5F);

      for (Player player : level.getEntitiesOfClass(
         Player.class,
         new AABB(this.finalVerdictCenter, this.finalVerdictCenter).inflate(12.0, 3.0, 12.0),
         p -> p.isAlive() && !p.isCreative() && !p.isSpectator()
      )) {
         double dx = player.getX() - this.finalVerdictCenter.x;
         double dz = player.getZ() - this.finalVerdictCenter.z;
         if (!(dx * dx + dz * dz > 144.0) && !this.insideSafeCell(player.position())) {
            Dmg.hurt(player, level, this.damageSources().mobAttack(this), this.scaleDamage(20.0F));
            player.push(0.0, 1.0, 0.0);
            player.hurtMarked = true;
            this.struckByChain(player, 0.45F, 14);
         }
      }

      this.finalVerdictSafeCells.clear();
   }

   private static Vec3 groundUnder(ServerLevel level, Entity entity) {
      return groundUnder(level, entity.position());
   }

   private static Vec3 groundUnder(ServerLevel level, Vec3 at) {
      return AimUtil.groundedPositionIfFound(level, at).orElse(at);
   }

   private boolean insideSafeCell(Vec3 p) {
      for (Vec3 cell : this.finalVerdictSafeCells) {
         double dx = p.x - cell.x;
         double dz = p.z - cell.z;
         if (dx * dx + dz * dz <= 4.840000000000001) {
            return true;
         }
      }

      return false;
   }

   private double flatDistance(Vec3 p) {
      double dx = p.x - this.getX();
      double dz = p.z - this.getZ();
      return Math.sqrt(dx * dx + dz * dz);
   }

   private void noteTick(Map<UUID, Integer> clocks, UUID key, int staleAfter) {
      if (clocks.size() > 16) {
         clocks.entrySet().removeIf(e -> this.tickCount - e.getValue() > staleAfter);
      }

      clocks.put(key, this.tickCount);
   }

   private static double distToSegment2D(Vec3 p, Vec3 a, Vec3 b) {
      double abx = b.x - a.x;
      double abz = b.z - a.z;
      double lenSq = abx * abx + abz * abz;
      double t = lenSq < 1.0E-8 ? 0.0 : Mth.clamp(((p.x - a.x) * abx + (p.z - a.z) * abz) / lenSq, 0.0, 1.0);
      double dx = p.x - (a.x + abx * t);
      double dz = p.z - (a.z + abz * t);
      return Math.sqrt(dx * dx + dz * dz);
   }

   private void maintainSeals(ServerLevel level) {
      if (!this.isKneeling() && !this.hasLivingSeals()) {
         this.forgeSeals(level);
      }
   }

   private void forgeSeal(ServerLevel level, int index) {
      ArbiterSealEntity seal = new ArbiterSealEntity(AwakenedEntities.ARBITER_SEAL.get(), level);
      seal.bind(this, index);
      level.addFreshEntity(seal);
      this.seals.add(seal);
   }

   private void clearSeals() {
      for (ArbiterSealEntity stale : List.copyOf(this.seals)) {
         stale.discard();
      }

      this.seals.clear();
   }

   private void forgeSeals(ServerLevel level) {
      this.clearSeals();

      for (int i = 0; i < 6; i++) {
         this.forgeSeal(level, i);
      }

      Fx.sound(level, this.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.6F, 0.7F);
      Fx.sound(level, this.position(), SoundEvents.ANVIL_USE, SoundSource.HOSTILE, 1.2F, 0.6F);
   }

   void onSealShattered(ArbiterSealEntity seal) {
      this.seals.remove(seal);
      int left = this.seals.size();
      boolean last = left == 0;
      Fx.sound(this.level(), this.position(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, last ? 2.0F : 1.2F, last ? 0.5F : 0.75F + (float)left * 0.09F);
      if (last && this.courtStage == 2 && this.level() instanceof ServerLevel server && this.isAlive() && !this.isKneeling()) {
         this.beginKneel(server);
      }
   }

   void onSealDiscarded(ArbiterSealEntity seal) {
      this.seals.remove(seal);
   }

   void onSealStruck(LivingEntity attacker) {
      if (this.getTarget() == null && attacker instanceof Player player && !player.isCreative() && !player.isSpectator() && this.inCourt(player)) {
         this.setTarget(player);
      }
   }

   private void sealGuardCue(ServerLevel level, @Nullable Entity attacker) {
      UUID key = attacker != null ? attacker.getUUID() : NO_ATTACKER;
      Integer last = this.guardCueAt.get(key);
      if (last == null || this.tickCount - last >= 4) {
         this.noteTick(this.guardCueAt, key, 100);
         Vec3 yoke = this.position().add(0.0, (double)this.getBbHeight() * 0.76, 0.0);
         Fx.sound(level, yoke, SoundEvents.IRON_DOOR_CLOSE, SoundSource.HOSTILE, 1.5F, 0.55F);
         Fx.sound(level, yoke, SoundEvents.STONE_HIT, SoundSource.HOSTILE, 1.0F, 0.5F);
         this.entityData.set(DATA_GUARD_CUE, (byte)(this.guardCue() + 1));

         for (ArbiterSealEntity seal : this.seals) {
            Fx.burst(level, JUDGE_VIOLET, seal.position().add(0.0, (double)seal.getBbHeight() * 0.5, 0.0), 3, 0.25, 0.02);
         }
      }
   }

   private boolean punishWindowOpen(ServerLevel level) {
      return this.isDeadOrDying() || this.isKneeling() && level.getGameTime() < this.kneelEndsAt;
   }

   private void cancelBeats(boolean rearmVerdict) {
      this.setPhase((byte)0);
      this.verses.reset();
      this.repelCutsSweep = false;
      this.gavelHold = 0;
      this.whipTell = 0;
      this.whipLandIn = 0;
      this.endSweep();
      this.rootMarks.clear();
      this.rootFollowThrough = 0;
      this.rootIsBar = false;
      this.sinkRoots();
      this.reelTell = 0;
      this.resetReel();
      if (this.finalVerdictTell > 0) {
         this.finalVerdictTell = 0;
         this.finalVerdictArmed = rearmVerdict;
      }
   }

   private void beginKneel(ServerLevel level) {
      this.kneelEndsAt = level.getGameTime() + 180L;
      this.setKneeling(true);
      this.cancelBeats(true);
      Vec3 center = this.position().add(0.0, (double)this.getBbHeight() * 0.5, 0.0);
      Fx.sound(level, center, (SoundEvent)AwakenedSounds.ARBITER_COURT_COLLAPSE.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
      Fx.shake(level, this.position(), 24.0, 0.28F, 0, 12);
      Fx.burst(level, JUDGE_VIOLET, center, 40, 1.2, 0.06);
      Fx.disk(level, JUDGE_VIOLET, this.position().add(0.0, 0.1, 0.0), 2.2);
      Vfx.sphere(level, center, 2.2, 11544319, 10);
      Vfx.ring(level, this.position().add(0.0, 0.25, 0.0), 3.6, 11544319, 22);
   }

   private void rise(ServerLevel level) {
      this.setKneeling(false);
      this.kneelEndsAt = 0L;
      if (this.pendingGavel > 0) {
         this.playGavel(level);
      } else {
         this.forgeSeals(level);
         this.openVerse(null);
         Vec3 center = this.position().add(0.0, (double)this.getBbHeight() * 0.5, 0.0);
         Fx.sound(level, center, (SoundEvent)AwakenedSounds.ARBITER_RISE_TOLL.get(), SoundSource.HOSTILE, 1.6F, 1.0F);
         Fx.burst(level, JUDGE_CYAN, center, 18, 0.8, 0.04);
         Vfx.ring(level, this.position().add(0.0, 0.22, 0.0), 3.0, 1630463, 16);
      }
   }

   public boolean hasLivingSeals() {
      this.seals.removeIf(seal -> seal == null || !seal.isAlive() || seal.isRemoved() || seal.level() != this.level());
      return !this.seals.isEmpty();
   }

   @Override
   protected boolean bossHurt(ServerLevel level, DamageSource source, float amount) {
      if (this.courtStage != 2) {
         if (source.getEntity() instanceof Player challenger && this.getTarget() == null) {
            this.setTarget(challenger);
         }

         this.cancelCeremonyIntoFight(level);
      }

      if (!this.punishWindowOpen(level) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         this.sealGuardCue(level, source.getEntity());
         return false;
      } else {
         float gate = this.nextGateHealth();
         if (gate > 0.0F
            && this.isKneeling()
            && !this.isDeadOrDying()
            && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
            && this.getHealth() - amount <= gate) {
            float landAt = gate - 0.01F;
            if (!super.bossHurt(level, source, Math.max(0.0F, this.getHealth() - landAt))) {
               return false;
            } else {
               if (this.getHealth() > landAt) {
                  this.setHealth(landAt);
               }

               this.updateEscalation(level);
               this.rise(level);
               return true;
            }
         } else {
            return super.bossHurt(level, source, amount);
         }
      }
   }

   @Override
   public float scaleDamage(float base) {
      float tier = TIER_DAMAGE[Mth.clamp(this.escalationPhase() - 1, 0, TIER_DAMAGE.length - 1)];
      return base * tier * ((Double)Config.ARBITER_DAMAGE_SCALE.get()).floatValue();
   }

   @Override
   protected void onEscalate(ServerLevel level, int newTier) {
      this.pendingGavel = newTier;
      if (!this.isKneeling()) {
         this.playGavel(level);
      }
   }

   private void playGavel(ServerLevel level) {
      int tier = this.pendingGavel;
      this.pendingGavel = 0;
      if (tier > 0) {
         this.setPhase((byte)5);
         this.gavelHold = 20;
         Vec3 mid = this.position().add(0.0, (double)this.getBbHeight() * 0.6, 0.0);
         Fx.sound(level, this.position(), (SoundEvent)AwakenedSounds.ARBITER_GAVEL_CRACK.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
         Fx.burst(level, JUDGE_VIOLET, mid, 50, 1.3, 0.15);
         Fx.burst(level, ParticleTypes.SONIC_BOOM, mid, 1, 0.0, 0.0);
         Fx.shake(level, this.position(), 26.0, 0.35F, 0, 12);
         Vfx.ring(level, this.position().add(0.0, 0.2, 0.0), 6.6, 11544319, 20);
         Vfx.ring(level, this.position().add(0.0, 0.22, 0.0), 3.0, 1630463, 20);
         Vfx.pillar(level, this.position(), 1.2, (double)this.getBbHeight() + 1.0, 1630463, 14);
         this.beginCourtVolley(level, 2 + this.getRandom().nextInt(2));
         this.forgeSeals(level);
         this.bossEvent.setColor(BossBarColor.PURPLE);
         this.bossEvent.setName(Component.translatable("entity.awakened.arbiter.enraged"));
         if (tier >= 3 && !this.finalVerdictSeen) {
            this.finalVerdictSeen = true;
            this.openVerse(ArbiterVerses.V0);
         } else if (tier == 2) {
            this.openVerse(ArbiterVerses.V4);
         } else {
            this.openVerse(null);
         }
      }
   }

   public boolean doHurtTarget(Entity target) {
      ServerLevel level = (ServerLevel)this.level();
      boolean hit = super.doHurtTarget(target);
      if (hit) {
         Fx.burst(level, JUDGE_VIOLET, target.position().add(0.0, 0.5, 0.0), 5, 0.3, 0.05);
         Fx.sound(level, this.position(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.0F, 0.7F);
         if (target instanceof Player struck) {
            this.struckByChain(struck, 0.18F, 6);
         }
      }

      return hit;
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.BEACON_AMBIENT;
   }

   public int getAmbientSoundInterval() {
      return 220;
   }

   protected SoundEvent getHurtSound(DamageSource damageSource) {
      return SoundEvents.IRON_GOLEM_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.IRON_GOLEM_DEATH;
   }

   @Override
   public void onCreditedKill(ServerPlayer player) {
      AwakenedAttachments.progress(player).bossDefeated();
      PowerService.syncProgress(player);
      QuestSystem.onBossDefeated(player);
   }

   @Override
   public void startSeenByPlayer(ServerPlayer player) {
      super.startSeenByPlayer(player);
      if (this.courtStage == 0) {
         this.bossEvent.removePlayer(player);
      }
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("VerdictSeen", this.finalVerdictSeen);
      tag.putBoolean("VerdictOwed", this.finalVerdictArmed);
      tag.putBoolean("InSession", this.courtStage == 2);
      if (this.courtCenter != null) {
         tag.putDouble("CourtX", this.courtCenter.x);
         tag.putDouble("CourtY", this.courtCenter.y);
         tag.putDouble("CourtZ", this.courtCenter.z);
      }
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.finalVerdictSeen = tag.getBoolean("VerdictSeen");
      this.finalVerdictArmed = tag.getBoolean("VerdictOwed");
      this.courtStage = (byte)(tag.getBoolean("InSession") ? 2 : 0);
      this.courtCenter = tag.contains("CourtX") ? new Vec3(tag.getDouble("CourtX"), tag.getDouble("CourtY"), tag.getDouble("CourtZ")) : null;
   }

   @Override
   protected int deathDuration() {
      return 90;
   }

   @Override
   protected void onDeathSequenceStart() {
      this.setKneeling(true);
      this.cancelBeats(false);
      this.finalVerdictArmed = false;
   }

   @Override
   protected void deathBeat(ServerLevel level, int deathTime) {
      Vec3 mid = this.position().add(0.0, (double)this.getBbHeight() * 0.5, 0.0);
      if (deathTime == 1) {
         Fx.sound(level, mid, (SoundEvent)AwakenedSounds.ARBITER_COURT_COLLAPSE.get(), SoundSource.HOSTILE, 2.0F, 0.8F);
         Fx.burst(level, JUDGE_VIOLET, mid, 40, 1.2, 0.08);
      }

      if (deathTime == 30) {
         Fx.sound(level, mid, SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.6F, 0.5F);
         Vfx.ring(level, this.position().add(0.0, 0.2, 0.0), 3.6, 11544319, 26);
      }

      if (deathTime == 70) {
         Fx.burst(level, ParticleTypes.SCULK_SOUL, mid, 50, 1.1, 0.15);
         Fx.burst(level, JUDGE_VIOLET, mid, 50, 1.3, 0.2);
         Fx.shake(level, this.position(), 18.0, 0.2F, 0, 10);
      }
   }

   @Override
   protected void dropCinematicLoot(ServerLevel level) {
      bless(RelicDrops.dropSignature(level, this.position().add(0.3, 1.0, 0.0), "scales_of_judgment"));
      bless(RelicDrops.dropAt(level, this.position().add(-0.3, 1.0, 0.0), this.getRandom(), Rarity.RARE, Rarity.EPIC, true));
      bless(this.spawnAtFeet(level, new ItemStack((ItemLike)AwakenedItems.VERDICT_CHAIN.get())));
      bless(this.spawnAtFeet(level, new ItemStack(Items.ECHO_SHARD, 6 + this.getRandom().nextInt(5))));
      bless(this.spawnAtFeet(level, new ItemStack(Items.DIAMOND, 2 + this.getRandom().nextInt(3))));
      bless(this.spawnAtFeet(level, new ItemStack(Items.IRON_INGOT, 4 + this.getRandom().nextInt(5))));
   }

   @Override
   public double bonusMastery() {
      return 200.0;
   }

   @Override
   public double bonusXp() {
      return 750.0;
   }

   private static enum Beat {
      REEL,
      REPEL;
   }

   private static record ReadyBeat(ArbiterEntity.Beat beat, Runnable start) {
   }

   private static final class RootMark {
      final Vec3 at;
      int total;
      final float volume;
      final int standTicks;
      final boolean solid;
      final boolean wave;
      final Set<UUID> struck;
      int tell;

      RootMark(Vec3 at, int tell, float volume, int standTicks, boolean solid, boolean wave, Set<UUID> struck) {
         this.at = at;
         this.total = tell;
         this.tell = tell;
         this.volume = volume;
         this.standTicks = standTicks;
         this.solid = solid;
         this.wave = wave;
         this.struck = struck;
      }

      void fuse(int ticks) {
         this.total = ticks;
         this.tell = ticks;
      }
   }

   private static record Sweep(
      double length,
      double inner,
      double halfWidth,
      double height,
      double bandLow,
      double bandHigh,
      int tellTicks,
      int tellLand,
      double tellLift,
      double turns,
      float degStart,
      float degEnd,
      double rebiteDeg,
      float damage,
      double carry,
      double carryOut,
      double carryUp,
      byte chainMode
   ) {
   }
}
