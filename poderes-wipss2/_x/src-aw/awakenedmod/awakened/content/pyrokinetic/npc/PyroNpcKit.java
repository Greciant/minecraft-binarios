package com.awakenedmod.awakened.content.pyrokinetic.npc;

import com.awakenedmod.awakened.content.pyrokinetic.FlameTone;
import com.awakenedmod.awakened.content.pyrokinetic.PyroBurningGround;
import com.awakenedmod.awakened.content.pyrokinetic.PyroFlames;
import com.awakenedmod.awakened.content.pyrokinetic.PyroHeatState;
import com.awakenedmod.awakened.content.pyrokinetic.PyrokineticPower;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.npc.MoveTactic;
import com.awakenedmod.awakened.core.npc.NpcBrain;
import com.awakenedmod.awakened.core.npc.NpcCaster;
import com.awakenedmod.awakened.core.npc.NpcContext;
import com.awakenedmod.awakened.core.npc.NpcKit;
import com.awakenedmod.awakened.core.npc.NpcMove;
import com.awakenedmod.awakened.core.npc.NpcTactic;
import com.awakenedmod.awakened.core.npc.PoweredNpc;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class PyroNpcKit extends NpcKit {
   public static final String ID = "pyrokinetic_npc";
   static final String FIREBALL_ID = "fireball";
   static final String BEAM_ID = "flame_beam";
   static final String JETS_ID = "fire_jets";
   static final String BARRAGE_ID = "barrage";
   static final String BACKDRAFT_ID = "backdraft";
   public static final ResourceLocation FIREBALL_R = AwId.of("fireball_throw_r");
   public static final ResourceLocation FIREBALL_L = AwId.of("fireball_throw_l");
   public static final ResourceLocation FLAME_BEAM = AwId.of("flame_beam");
   public static final ResourceLocation FIRE_JETS = AwId.of("fire_jets");
   public static final ResourceLocation FIRE_JETS_TAP = AwId.of("fire_jets_tap");
   public static final ResourceLocation FIRE_JETS_LAND = AwId.of("fire_jets_land");
   public static final ResourceLocation BARRAGE = AwId.of("pyro_barrage");
   public static final ResourceLocation BACKDRAFT = AwId.of("backdraft");
   public static final ResourceLocation NOTICE = AwId.of("pyro_npc_notice");
   public static final ResourceLocation FLICKER_R = AwId.of("pyro_npc_flicker_r");
   public static final ResourceLocation FLICKER_L = AwId.of("pyro_npc_flicker_l");
   private static final List<ResourceLocation> CLIPS = List.of(
      FIREBALL_R, FIREBALL_L, FLAME_BEAM, FIRE_JETS, FIRE_JETS_TAP, FIRE_JETS_LAND, BARRAGE, BACKDRAFT, NOTICE, FLICKER_R, FLICKER_L
   );
   private static final ResourceLocation SKIN = AwId.of("textures/entity/npc/pyrokinetic.png");
   private static final int PUSH_MASTERY = 12;
   private static final float PUSH_HEALTH = 0.4F;
   private static final float PUSH_MARGIN = 0.3F;
   private static final double HAZARD_MARGIN = 0.4;
   private static final double FLICKER_SCALE = 0.25;
   private static final float NOTICE_SNAP = 60.0F;
   private static final int NOTICE_SNAP_TICKS = 2;
   private static final int NOTICE_FLARE_AT = 2;
   private static final double NOTICE_ARM_PITCH = 15.0;
   private static final int FLICKER_LIGHT_AT = 6;
   private static final int FLICKER_TICKS = 16;
   private static final double FLICKER_ARM_PITCH = 52.0;
   private static final int INTEREST_RADIUS = 6;
   private static final float LOW_MASTERY_MARGIN = 0.02F;

   public PyroNpcKit() {
      super("pyrokinetic_npc", "pyrokinetic");
   }

   @Override
   public ResourceLocation skin() {
      return SKIN;
   }

   @Override
   public List<NpcMove> createMoves() {
      return List.of(new FireballMove(), new FlameBeamMove(), new FireJetsMove(), new BarrageMove(), new BackdraftMove());
   }

   @Override
   public List<NpcTactic> createTactics() {
      return List.of(
         new PressureTactic(),
         new PunishTactic(),
         new KiteTactic(),
         new EvadeTactic(),
         new BurnTactic(),
         new FlankTactic(),
         new MoveTactic("traverse", "fire_jets", 5, 1.0F, ""),
         new ZoneTactic(),
         new CloserTactic()
      );
   }

   static boolean throwFits(NpcCaster caster) {
      return PyroHeatState.heat(caster) + 0.08F <= PyroHeatState.cap(caster.masteryLevel());
   }

   static int throwsLeft(NpcContext ctx) {
      float room = (1.0F - ctx.strain()) * ventHeat(ctx.npc().mastery());
      return Math.max(0, (int)Math.floor((double)(room / 0.08F) + 0.001));
   }

   @Override
   public List<ResourceLocation> clips() {
      return CLIPS;
   }

   @Override
   public void passiveTick(NpcCaster caster) {
      PyroHeatState.tick(caster);
      PyrokineticPower.keepFireImmunity(caster.body(), caster.masteryLevel());
      idleClips(caster);
   }

   private static void idleClips(NpcCaster caster) {
      PoweredNpc npc = caster.npc();
      ResourceLocation clip = npc.activeClip();
      if (clip != null) {
         if (clip.equals(NOTICE) && npc.npcBrain().state() != NpcBrain.State.NOTICE) {
            npc.clearClip();
         } else if (clip.equals(FLICKER_R) || clip.equals(FLICKER_L)) {
            int age = (int)npc.clipAge(0.0F);
            if (age == 6) {
               boolean offHand = caster.rightArm(false) != clip.equals(FLICKER_R);
               Vec3 hand = caster.hand(offHand, 52.0, 0.34, 0.46, 0.6);
               FlameTone tone = FlameTone.of(caster);
               PyroFlames.knot(caster.level(), hand, 0.25, tone);
               PyroFlames.sound(caster, hand, SoundEvents.FIRE_AMBIENT, 0.4F, 1.4F, tone);
            } else if (age >= 16) {
               npc.clearClip();
            }
         }
      }
   }

   @Override
   public void onNotice(NpcContext ctx) {
      ctx.npc().showClip(NOTICE);
   }

   @Override
   public void noticeTick(NpcContext ctx, int age) {
      LivingEntity target = ctx.target();
      if (age < 2 && target != null) {
         ctx.npc().aimAt(target.getEyePosition(), 60.0F);
      }

      if (age == 2) {
         NpcCaster caster = ctx.caster();
         Vec3 hand = caster.hand(false, 15.0, 0.34, 0.46, 0.6);
         FlameTone tone = FlameTone.of(caster);
         PyroFlames.impactFlare(caster.level(), hand, tone);
         PyroFlames.sound(caster, hand, SoundEvents.FIRECHARGE_USE, 0.5F, 1.3F, tone);
      }
   }

   @Override
   public void onRemoved(NpcCaster caster) {
      PyroHeatState.forget(caster);
      PyroBurningGround.clear(caster.body());
   }

   @Override
   public float strain(NpcCaster caster) {
      int mastery = caster.masteryLevel();
      float vent = ventHeat(mastery);
      if (mastery >= 12 && caster.body().getHealth() < caster.body().getMaxHealth() * 0.4F) {
         vent += 0.3F;
      }

      return PyroHeatState.heat(caster) / vent;
   }

   @Override
   public void onVent(NpcContext ctx) {
      NpcCaster caster = ctx.caster();
      Vec3 chest = caster.body().position().add(0.0, 1.2, 0.0);
      PyroFlames.sound(caster, chest, SoundEvents.FIRE_EXTINGUISH, 0.7F, 1.4F, FlameTone.of(caster));
      Fx.burst(caster.level(), ParticleTypes.LARGE_SMOKE, chest, 8, 0.35, 0.02);
   }

   @Nullable
   @Override
   public Vec3 hazardNear(NpcCaster caster) {
      ServerLevel level = caster.level();
      BlockPos feet = caster.body().blockPosition();
      return !level.getBlockState(feet).is(BlockTags.FIRE) && !level.getFluidState(feet).is(FluidTags.LAVA)
         ? PyroBurningGround.burningAt(level, caster.body().position(), 0.4)
         : Vec3.atBottomCenterOf(feet);
   }

   @Override
   public void idleFlourish(NpcCaster caster) {
      PoweredNpc npc = caster.npc();
      if (!npc.isCasting()) {
         boolean offHand = caster.body().getRandom().nextBoolean();
         npc.showClip(caster.rightArm(offHand) ? FLICKER_R : FLICKER_L);
      }
   }

   @Nullable
   @Override
   public Vec3 pointOfInterest(NpcCaster caster) {
      ServerLevel level = caster.level();
      BlockPos base = caster.body().blockPosition();
      Vec3 best = null;
      double bestDist = Double.MAX_VALUE;

      for (BlockPos pos : BlockPos.betweenClosed(base.offset(-6, -2, -6), base.offset(6, 2, 6))) {
         BlockState state = level.getBlockState(pos);
         if (state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES) || level.getFluidState(pos).is(FluidTags.LAVA)) {
            double d = pos.distSqr(base);
            if (d < bestDist) {
               bestDist = d;
               best = Vec3.atCenterOf(pos);
            }
         }
      }

      return best;
   }

   @Override
   public boolean avoidsWater() {
      return true;
   }

   public static float ventHeat(int mastery) {
      float margin = mastery >= 12 ? 0.1F : (mastery >= 7 ? -0.08F : -0.099999994F);
      return Mth.clamp(PyroHeatState.cap(mastery) + margin, 0.2F, 1.0F);
   }
}
