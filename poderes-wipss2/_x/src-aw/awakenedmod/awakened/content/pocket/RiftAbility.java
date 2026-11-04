package com.awakenedmod.awakened.content.pocket;

import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RiftAbility extends Ability {
   private static final float ENTER_COST = 12.0F;
   private static final float RETURN_COST = 8.0F;
   private static final float ENTITY_COST_FRACTION = 0.5F;
   private static final int UNLOCK = 0;
   private static final double ENTITY_RANGE = 22.0;
   private static final double SEND_RANGE = 5.5;
   private static final int PORTAL_TICKS = 120;
   private static final int FOLLOW_TICKS = 40;
   private static final double PORTAL_FORWARD = 2.0;
   private static final double PORTAL_HALF_WIDTH = 1.0;
   private static final double PORTAL_HEIGHT = 3.0;
   private static final double PORTAL_RADIUS = 1.1;
   private final Map<UUID, RiftAbility.RiftPortal> portals = this.playerState();
   private final Map<UUID, RiftAbility.PendingSwallow> swallows = this.playerState();

   public RiftAbility() {
      super("rift", 12.0F, 0);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      if (player.isShiftKeyDown()) {
         return 0.0F;
      } else if (RiftSpace.isInRift(player)) {
         return 8.0F;
      } else {
         return swallowTarget(player, 5.5) != null ? StaminaSystem.baseMax() * 0.5F : StaminaSystem.baseMax();
      }
   }

   @Override
   public boolean activate(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      if (player.isShiftKeyDown()) {
         this.openStash(player, level);
         return true;
      } else if (RiftSpace.isInRift(player)) {
         LivingEntity trapped = swallowTarget(player, 22.0);
         if (trapped != null) {
            if (this.swallows.containsKey(player.getUUID())) {
               return this.fail(player, "awakened.fail.busy", new Object[0]);
            } else {
               this.beginSwallow(player, level, trapped, false);
               player.displayClientMessage(Component.translatable("awakened.pocket.evicted", new Object[]{trapped.getDisplayName()}), true);
               return true;
            }
         } else {
            this.open(player, level, true);
            return true;
         }
      } else {
         LivingEntity foe = swallowTarget(player, 5.5);
         if (foe != null) {
            if (this.swallows.containsKey(player.getUUID())) {
               return this.fail(player, "awakened.fail.busy", new Object[0]);
            } else {
               this.beginSwallow(player, level, foe, true);
               return true;
            }
         } else {
            this.open(player, level, false);
            return true;
         }
      }
   }

   private static LivingEntity swallowTarget(ServerPlayer player, double range) {
      return AimUtil.aimedLivingEntity(player, range, e -> Harm.controls(player, e));
   }

   private void beginSwallow(ServerPlayer player, ServerLevel level, LivingEntity foe, boolean intoRift) {
      Vec3 center = RiftFx.swallowOpen(level, foe.position(), (double)foe.getBbWidth(), (double)foe.getBbHeight(), -player.getYRot());
      if (!(foe instanceof ServerPlayer)) {
         TickFreeze.freeze(foe);
      }

      this.swallows.put(player.getUUID(), new RiftAbility.PendingSwallow(level, foe, center, (double)foe.getBbWidth(), (double)foe.getBbHeight(), intoRift, 4));
      player.swing(InteractionHand.MAIN_HAND);
   }

   private void tickSwallow(ServerPlayer player) {
      RiftAbility.PendingSwallow s = this.swallows.get(player.getUUID());
      if (s != null) {
         LivingEntity foe = s.foe;
         if (!foe.isAlive() || foe.isRemoved() || foe.level() != s.level) {
            TickFreeze.release(foe);
            this.swallows.remove(player.getUUID());
         } else if (--s.ticksLeft <= 0) {
            this.swallows.remove(player.getUUID());
            TickFreeze.release(foe);
            boolean ok = s.intoRift ? RiftSpace.sendEntity(foe, player.getUUID(), s.level.getServer(), true) : RiftSpace.evict(foe, player, true);
            if (ok) {
               RiftFx.swallowVanish(s.level, s.center, s.width, s.height);
               MasterySystem.award(player, MasteryReason.HIT, foe);
            } else {
               player.displayClientMessage(Component.translatable("awakened.pocket.rift_refused"), true);
            }
         }
      }
   }

   private void openStash(ServerPlayer player, ServerLevel level) {
      RiftStashData stash = RiftAttachments.stash(player);
      player.openMenu(new SimpleMenuProvider((id, inv, p) -> ChestMenu.threeRows(id, inv, stash), Component.translatable("container.awakened.pocket_stash")));
      Fx.burst(level, ParticleTypes.PORTAL, player.getEyePosition(), 20, 0.5, 0.1);
      Fx.sound(level, player.position(), SoundEvents.ENDER_CHEST_OPEN, SoundSource.PLAYERS, 0.7F, 1.1F);
   }

   private void open(ServerPlayer player, ServerLevel level, boolean returnPortal) {
      Vec3 forward = AimUtil.horizontalDir(player.position(), player.position().add(player.getLookAngle()));
      Vec3 base = player.position().add(forward.scale(2.0));
      Vec3 gateCenter = base.add(0.0, 1.4850000000000003, 0.0);
      VfxEntity gate = RiftFx.gate(level, gateCenter, 1.1, 120, -player.getYRot());
      this.portals.put(player.getUUID(), new RiftAbility.RiftPortal(level, base, 120, returnPortal, gate));
      player.swing(InteractionHand.MAIN_HAND);
      Fx.sound(level, base, SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.35F, 0.7F);
      Fx.sound(level, base, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.4F, 0.9F);
      player.displayClientMessage(Component.translatable(returnPortal ? "awakened.pocket.rift_return" : "awakened.pocket.rift_open"), true);
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.tickSwallow(player);
      RiftAbility.RiftPortal portal = this.portals.get(player.getUUID());
      if (portal != null) {
         ServerLevel level = portal.level;
         AABB region = new AABB(portal.base.add(0.0, 1.5, 0.0), portal.base.add(0.0, 1.5, 0.0)).inflate(1.3, 1.5, 0.7);

         for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, region, LivingEntity::isAlive)) {
            if (e == player) {
               if (portal.returnPortal) {
                  RiftSpace.returnHome(player);
               } else {
                  RiftSpace.enter(player);
               }

               if (portal.ticksLeft > 40) {
                  portal.ticksLeft = 40;
               }
            } else if (portal.returnPortal) {
               RiftSpace.evict(e, player, false);
            } else {
               RiftSpace.sendEntity(e, player.getUUID(), level.getServer(), false);
            }
         }

         if (--portal.ticksLeft <= 0) {
            this.portals.remove(player.getUUID());
            portal.closeGate();
            Fx.burst(level, ParticleTypes.REVERSE_PORTAL, portal.base.add(0.0, 1.5, 0.0), 30, 0.6, 0.1);
         }
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      RiftAbility.RiftPortal portal = this.portals.remove(player.getUUID());
      if (portal != null) {
         portal.closeGate();
      }

      RiftAbility.PendingSwallow swallow = this.swallows.remove(player.getUUID());
      if (swallow != null) {
         TickFreeze.release(swallow.foe);
      }

      super.clearPlayer(player);
   }

   private static final class PendingSwallow {
      private final ServerLevel level;
      private final LivingEntity foe;
      private final Vec3 center;
      private final double width;
      private final double height;
      private final boolean intoRift;
      private int ticksLeft;

      private PendingSwallow(ServerLevel level, LivingEntity foe, Vec3 center, double width, double height, boolean intoRift, int ticksLeft) {
         this.level = level;
         this.foe = foe;
         this.center = center;
         this.width = width;
         this.height = height;
         this.intoRift = intoRift;
         this.ticksLeft = ticksLeft;
      }
   }

   private static final class RiftPortal {
      private final ServerLevel level;
      private final Vec3 base;
      private final boolean returnPortal;
      private final VfxEntity gate;
      private int ticksLeft;

      private RiftPortal(ServerLevel level, Vec3 base, int ticksLeft, boolean returnPortal, VfxEntity gate) {
         this.level = level;
         this.base = base;
         this.ticksLeft = ticksLeft;
         this.returnPortal = returnPortal;
         this.gate = gate;
      }

      private void closeGate() {
         if (this.gate != null && this.gate.isAlive()) {
            this.gate.discard();
         }
      }
   }
}
