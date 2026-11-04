package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.FallGuard;
import com.awakenedmod.awakened.core.GravityOverride;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.PulledBody;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public final class GravityWellAbility extends Ability {
   public static final String ID = "gravity_well";
   public static final int UNLOCK = 3;
   public static final float COST = 7.0F;
   private static final float DRAIN_PER_SECOND = 4.0F;
   private static final float DRAIN_PER_TICK = 0.2F;
   private static final int SYNC_TICKS = 5;
   public static final int ACTION_CLOSE = 110;
   public static final String CHAIN_FEAT = "graviton_chain";
   public static final double CHAIN_FEAT_BLOCKS = 80.0;
   public static final String ANIM_LAND = "gravity_well_land";
   private static final ResourceLocation FALL_GUARD_ID = AwId.of("graviton_well_fall_guard");
   private static final int GUARD_GRACE_TICKS = 10;
   private static final int GUARD_MAX_TICKS = 400;
   private static final double SANITY_SPEED = 6.0;
   private static final double ARRIVE_SLACK = 0.6;
   private final Map<UUID, GravityWellAbility.Chain> chains = this.activeState();
   private final FallGuard guard = new FallGuard(FALL_GUARD_ID, 10, 400);

   public GravityWellAbility() {
      super("gravity_well", 7.0F, 3);
      GravityWellMotion.RANGE.get();
   }

   @Override
   public boolean clientDriven() {
      return true;
   }

   @Override
   public String costLabel() {
      return Math.round(this.billed(7.0F)) + " + " + Math.round(this.billed(4.0F)) + "/s";
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (PulledBody.carried(player)) {
         return this.fail(player, "awakened.graviton.well_blocked", new Object[0]);
      } else {
         Optional<GravityWellMotion.Target> target = GravityWellMotion.target(player);
         if (target.isEmpty()) {
            return this.fail(player, "awakened.graviton.well_no_room", new Object[0]);
         } else {
            this.openAt(player, target.get());
            return true;
         }
      }
   }

   public void openAt(ServerPlayer player, GravityWellMotion.Target target) {
      this.openAt(player, target, GravityWellPlanet.roll(player.getRandom(), GravityWellVisual.of(player).planet()));
   }

   public void openAt(ServerPlayer player, GravityWellMotion.Target target, int planet) {
      ServerLevel level = player.serverLevel();
      GravityWellAbility.Chain chain = this.chains.get(player.getUUID());
      if (chain == null) {
         chain = new GravityWellAbility.Chain(player.position());
         this.chains.put(player.getUUID(), chain);
      } else {
         GravityWellFx.swap(level, player, chain.well);
         chain.wells++;
      }

      chain.well = new GravityWellFx.Well(
         target,
         chain.wells,
         (int)level.getGameTime(),
         planet,
         GravityWellPlanet.center(level, target.point(), target.face(), target.surface(), GravityWellPlanet.kind(planet).radius(1.5))
      );
      chain.age = 0;
      GravityWellVisual.set(player, GravityWellVisual.of(player).opened(target, chain.wells, player.position(), planet));
      GravityOverride.set(player, GravityWellMotion.GRAVITY_ID, GravityWellMotion.GRAVITY.get());
      player.resetFallDistance();
      this.guard.arm(player);
      GravityWellFx.open(level, player, chain.well);
   }

   @Override
   public void onSecondaryAction(ServerPlayer player, int action) {
      if (action == 110) {
         this.close(player);
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.guard.tick(player, this.chains.containsKey(player.getUUID()));
      GravityWellAbility.Chain chain = this.chains.get(player.getUUID());
      if (chain != null) {
         chain.age++;
         Vec3 pos = player.position();
         double moved = pos.distanceTo(chain.lastPos);
         chain.lastPos = pos;
         GravityWellFx.Well well = chain.well;
         boolean near = pos.distanceTo(well.target().point()) < GravityWellMotion.ARRIVE.get() + 0.6;
         boolean landed = chain.age > 3 && player.onGround() && near;
         if (chain.age > GravityWellMotion.LIFE.getI() || player.isShiftKeyDown() || landed || moved > 6.0 || PulledBody.mustLetGo(player)) {
            this.close(player);
         } else if (this.drain(player)) {
            if (!near) {
               chain.travelled += moved;
               if (!chain.feat && chain.travelled >= 80.0) {
                  chain.feat = true;
                  QuestSystem.feat(player, "graviton_chain");
               }
            }

            GravityWellVisual.set(player, GravityWellVisual.of(player).arrived(near));
            GravityOverride.set(player, GravityWellMotion.GRAVITY_ID, GravityWellMotion.GRAVITY.get());
            player.resetFallDistance();
            this.guard.arm(player);
            GravityWellFx.tick(player.serverLevel(), player, well, chain.age, moved);
         }
      }
   }

   private boolean drain(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      float drain = this.billed(0.2F);
      if (!StaminaSystem.canAfford(data, drain)) {
         PowerService.feedback(player, 0);
         this.close(player);
         return false;
      } else {
         StaminaSystem.drainSustained(data, player.serverLevel().getGameTime(), drain);
         if (player.tickCount % 5 == 0) {
            PowerService.sync(player);
         }

         return true;
      }
   }

   private void close(ServerPlayer player) {
      GravityWellAbility.Chain chain = this.chains.remove(player.getUUID());
      GravityOverride.clear(player, GravityWellMotion.GRAVITY_ID);
      if (chain != null) {
         Vec3 point = chain.well.target().point();
         boolean arrived = player.position().distanceTo(point) < GravityWellMotion.ARRIVE.get() + 0.6;
         GravityWellVisual.set(player, GravityWellVisual.of(player).ended(arrived ? 0 : 1));
         GravityWellFx.close(player.serverLevel(), player, chain.well, arrived);
         player.resetFallDistance();
         this.guard.arm(player);
         if (arrived && player.onGround()) {
            PowerService.playAnimation(player, "gravity_well_land");
         }
      }
   }

   public static boolean shapesFall(LivingEntity entity) {
      return hasModifier(entity, Attributes.GRAVITY, GravityWellMotion.GRAVITY_ID) || hasModifier(entity, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_GUARD_ID);
   }

   private static boolean hasModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id) {
      AttributeInstance inst = entity.getAttribute(attribute);
      return inst != null && inst.hasModifier(id);
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      GravityWellAbility.Chain chain = this.chains.get(player.getUUID());
      if (chain != null) {
         GravityWellFx.drop(player.serverLevel(), player, chain.well);
         GravityWellVisual.set(player, GravityWellVisual.of(player).ended(1));
      }

      GravityOverride.clear(player, GravityWellMotion.GRAVITY_ID);
      this.guard.clear(player);
      super.clearPlayer(player);
   }

   private static final class Chain {
      GravityWellFx.Well well;
      int wells;
      int age;
      Vec3 lastPos;
      double travelled;
      boolean feat;

      Chain(Vec3 pos) {
         this.lastPos = pos;
      }
   }
}
