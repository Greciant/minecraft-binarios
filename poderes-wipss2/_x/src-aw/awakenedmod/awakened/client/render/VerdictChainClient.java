package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.VerdictHookEntity;
import com.awakenedmod.awakened.content.item.VerdictChainItem;
import com.awakenedmod.awakened.core.AimUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;

public final class VerdictChainClient {
   private static final double WHIRL_RADIUS = 2.5;
   private static final double WHIRL_HEIGHT = 0.55;
   private static final float WHIRL_CHARGE_TICKS = 8.0F;
   private static final float THICKNESS = 1.3F;
   private static final int TRAIL_LEN = 8;
   private static final float TRAIL_HALF_HEIGHT = 0.16F;
   private static final float WHIRL_FLASH_TICKS = 5.0F;
   private static final double WHIRL_BITE_REACH = 0.55;
   private static final double WHIRL_BITE_DROP = 1.2;
   private static final float RELEASE_TICKS = 3.0F;
   private static final float PAYOUT_TICKS = 6.0F;
   private static final float CRACK_TICKS = 2.5F;
   private static final float BITE_FLASH_TICKS = 6.0F;
   private static final float REEL_PULSE_TICKS = 5.0F;
   private static final float GHOST_TICKS = 4.0F;
   private static final int PRUNE_TICKS = 60;
   private static final Int2ObjectMap<VerdictChainClient.Whirl> WHIRLS = new Int2ObjectOpenHashMap();
   private static final Int2ObjectMap<VerdictChainClient.Hook> HOOKS = new Int2ObjectOpenHashMap();
   private static final IntSet HOOK_OWNERS = new IntOpenHashSet();
   private static final List<VerdictChainClient.Ghost> GHOSTS = new ArrayList<>();
   private static final List<ChainLinks.Chain> POOL = new ArrayList<>();
   private static final List<ChainLinks.Chain> FRAME = new ArrayList<>();
   private static final List<ChainLinks.Trail> TRAILS = new ArrayList<>();
   private static Object memoryLevel;

   private VerdictChainClient() {
   }

   public static boolean chainOut(LivingEntity entity) {
      if (entity instanceof Player player) {
         return player.isUsingItem() && player.getUseItem().getItem() instanceof VerdictChainItem ? true : HOOK_OWNERS.contains(player.getId());
      } else {
         return false;
      }
   }

   private static void hideHeldChain(RenderHandEvent event) {
      if (event.getItemStack().getItem() instanceof VerdictChainItem && chainOut(Minecraft.getInstance().player)) {
         event.setCanceled(true);
      }
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(VerdictChainClient::hideHeldChain);
      gameBus.addListener(event -> {
         if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
            draw(event);
         }
      });
   }

   private static void draw(RenderLevelStageEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         memoryLevel = null;
      } else {
         float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
         Vec3 cam = event.getCamera().getPosition();
         long now = mc.level.getGameTime();
         float time = (float)(now % 100000L) + pt;
         if (memoryLevel != mc.level) {
            memoryLevel = mc.level;
            WHIRLS.clear();
            HOOKS.clear();
            GHOSTS.clear();
         }

         FRAME.clear();
         TRAILS.clear();
         HOOK_OWNERS.clear();

         for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof VerdictHookEntity hook) {
               thrown(hook, now, pt, cam);
            } else if (entity instanceof Player player) {
               whirl(player, now, pt, time, cam);
            }
         }

         Iterator<Entry<VerdictChainClient.Hook>> it = HOOKS.int2ObjectEntrySet().iterator();

         while (it.hasNext()) {
            Entry<VerdictChainClient.Hook> e = it.next();
            int id = e.getIntKey();
            VerdictChainClient.Hook h = (VerdictChainClient.Hook)e.getValue();
            if (h.lastSeen != now) {
               it.remove();
               if (now - h.lastSeen <= 1L) {
                  GHOSTS.add(new VerdictChainClient.Ghost(h, now, id));
               }
            }
         }

         ghosts(mc, now, pt, cam);
         releases(mc, now, pt, cam);
         WHIRLS.values().removeIf(w -> now - w.lastSeen > 60L);
         if (!FRAME.isEmpty() || !TRAILS.isEmpty()) {
            PoseStack ps = event.getPoseStack();
            BufferSource buffers = mc.renderBuffers().bufferSource();
            ChainLinks.draw(ps, buffers, cam, FRAME, time);
            trails(ps, buffers, cam);
         }
      }
   }

   private static void whirl(Player player, long now, float pt, float time, Vec3 cam) {
      if (player.isUsingItem() && player.getUseItem().getItem() instanceof VerdictChainItem) {
         float age = VerdictHandAnchor.whirlAge(player, pt);
         float phase = VerdictHandAnchor.whirlPhase(player, pt);
         if (age < 0.0F) {
            age = (float)player.getTicksUsingItem() + pt;
            phase = age / 10.0F;
            phase -= (float)Math.floor((double)phase);
         }

         VerdictChainClient.Whirl w = (VerdictChainClient.Whirl)WHIRLS.computeIfAbsent(player.getId(), id -> new VerdictChainClient.Whirl());
         w.lastSeen = now;
         double turn = ((double)phase - 0.8) * Math.PI * 2.0;
         float bodyYaw = Mth.rotLerp(pt, player.yBodyRotO, player.yBodyRot);
         Vec3 right = AimUtil.horizontalRight((double)bodyYaw);
         Vec3 fwd = AimUtil.direction((double)bodyYaw, 0.0);
         Vec3 centre = player.getPosition(pt).add(0.0, (double)player.getEyeHeight() + 0.55, 0.0);
         Vec3 head = centre.add(right.scale(Math.cos(turn) * 2.5)).add(fwd.scale(-Math.sin(turn) * 2.5));
         Vec3 hand = VerdictHandAnchor.hand(player, pt, cam);
         w.trail.sample(head.x, head.y, head.z, now);
         TRAILS.add(w.trail);
         bites(player, w, head, now);
         float charge = ChainLinks.span(age, 0.0F, 8.0F);
         float ready = charge >= 1.0F ? 0.88F + 0.12F * Mth.sin(time * 0.35F) : charge;
         float bite = 1.0F - ChainLinks.span((float)(now - w.biteTick) + pt, 0.0F, 5.0F);
         ChainLinks.Chain ch = next().from(hand).to(head);
         ch.head = 2;
         ch.thickness = 1.3F;
         ch.seed = player.getId();
         ch.brightness = light(head);
         float span = (float)hand.distanceTo(head);
         ch.sag = span * 0.025F;
         ch.charge = 0.12F + 0.88F * ready;
         ch.ember = 0.4F + 0.6F * charge;
         ch.tremble = 0.012F + 0.014F * charge + 0.03F * bite;
         ch.pulse = phase;
         ch.pulseWidth = 0.14F;
         ch.flash = bite;
         w.hx = head.x;
         w.hy = head.y;
         w.hz = head.z;
         w.charge = ch.charge;
      }
   }

   private static void releases(Minecraft mc, long now, float pt, Vec3 cam) {
      ObjectIterator var5 = WHIRLS.int2ObjectEntrySet().iterator();

      while (var5.hasNext()) {
         Entry<VerdictChainClient.Whirl> e = (Entry<VerdictChainClient.Whirl>)var5.next();
         int id = e.getIntKey();
         VerdictChainClient.Whirl w = (VerdictChainClient.Whirl)e.getValue();
         float age = (float)(now - w.lastSeen) + pt;
         if (w.lastSeen != now && !(age >= 3.0F) && !HOOK_OWNERS.contains(id) && mc.level.getEntity(id) instanceof Player owner) {
            Vec3 hand = VerdictHandAnchor.hand(owner, pt, cam);
            Vec3 end = new Vec3(w.hx, w.hy, w.hz);
            ChainLinks.Chain ch = next().from(hand).to(end);
            ch.head = 2;
            ch.thickness = 1.3F;
            ch.seed = id;
            ch.brightness = light(end);
            float t = age / 3.0F;
            ch.growth = 1.0F - AnimEnvelopes.easeOutCubic(t);
            ch.sag = (float)hand.distanceTo(end) * 0.03F;
            ch.charge = w.charge * (1.0F - t);
            ch.tremble = 0.02F;
         }
      }
   }

   private static void bites(Player player, VerdictChainClient.Whirl w, Vec3 head, long now) {
      if (w.checkedTick != now) {
         w.checkedTick = now;
         AABB near = new AABB(head, head).inflate(1.6);
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null) {
            for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, near, ex -> ex != player && ex.isAlive() && !ex.isSpectator())) {
               AABB bb = e.getBoundingBox();
               double cx = Mth.clamp(head.x, bb.minX, bb.maxX);
               double cy = Mth.clamp(head.y, bb.minY, bb.maxY + 1.2);
               double cz = Mth.clamp(head.z, bb.minZ, bb.maxZ);
               if (head.distanceToSqr(cx, cy, cz) <= 0.30250000000000005) {
                  w.biteTick = now;
                  return;
               }
            }
         }
      }
   }

   private static void thrown(VerdictHookEntity hook, long now, float pt, Vec3 cam) {
      Player owner = hook.owner();
      if (owner != null) {
         VerdictChainClient.Hook h = (VerdictChainClient.Hook)HOOKS.computeIfAbsent(hook.getId(), id -> new VerdictChainClient.Hook());
         byte mode = hook.mode();
         if (h.mode != mode) {
            h.mode = mode;
            h.modeSince = now;
            if ((mode == 1 || mode == 2 || mode == 5) && h.biteTick < 0L) {
               h.biteTick = now;
            }
         }

         h.lastSeen = now;
         h.ownerId = owner.getId();
         HOOK_OWNERS.add(owner.getId());
         Vec3 head = hook.renderPosition(pt);
         Vec3 hand = VerdictHandAnchor.hand(owner, pt, cam);
         float age = (float)hook.tickCount + pt;
         float since = (float)(now - h.modeSince) + pt;
         float bite = 1.0F - ChainLinks.span((float)(now - h.biteTick) + pt, 0.0F, 6.0F);
         ChainLinks.Chain ch = next().from(hand).to(head);
         ch.head = 2;
         ch.thickness = 1.3F;
         ch.seed = hook.getId();
         ch.brightness = light(head);
         float span = (float)hand.distanceTo(head);
         switch (mode) {
            case 0:
               ch.growth = Math.max(0.3F, ChainLinks.span(age, 0.0F, 6.0F));
               ch.charge = 0.45F;
               ch.ember = 0.85F;
               ch.sag = span * Mth.lerp(ChainLinks.span(age, 7.0F, 13.0F), 0.012F, 0.05F);
               ch.tremble = 0.012F;
               ch.flash = 1.0F - ChainLinks.span(age, 0.0F, 2.5F);
               break;
            case 1:
               ch.charge = 1.0F;
               ch.ember = 1.0F;
               ch.sag = span * 0.02F;
               ch.tremble = 0.03F;
               ch.flash = Math.max(bite, 0.6F);
               break;
            case 2:
               ch.charge = 0.95F;
               ch.ember = 1.0F;
               ch.sag = span * 0.012F;
               ch.tremble = 0.035F;
               ch.flash = bite;
               float run = since / 5.0F;
               ch.pulse = run - (float)Math.floor((double)run);
               ch.pulseWidth = 0.14F;
               break;
            case 3:
               ch.sag = span * 0.14F;
               ch.settle = ChainLinks.settle(since);
               ch.charge = Mth.lerp(ChainLinks.span(since, 0.0F, 6.0F), 0.7F, 0.3F);
               ch.ember = 0.5F;
               ch.tremble = 0.03F * (1.0F - ChainLinks.span(since, 0.0F, 6.0F));
               ch.flash = 1.0F - ChainLinks.span(since, 0.0F, 4.0F);
               break;
            case 4:
            default:
               ch.sag = span * 0.06F;
               ch.settle = ChainLinks.settle(since) * 0.6F;
               ch.charge = Mth.lerp(ChainLinks.span(since, 0.0F, 10.0F), 0.4F, 0.15F);
               ch.ember = Mth.lerp(ChainLinks.span(since, 0.0F, 10.0F), 0.5F, 0.2F);
               ch.sway = 0.04F;
               ch.flash = bite * 0.5F;
               break;
            case 5:
               ch.charge = 1.0F;
               ch.ember = 1.0F;
               ch.sag = span * 0.008F;
               ch.tremble = 0.045F + 0.03F * Mth.sin(since * 2.3F);
               ch.flash = bite;
         }

         h.hx = head.x;
         h.hy = head.y;
         h.hz = head.z;
         h.charge = ch.charge;
      }
   }

   private static void ghosts(Minecraft mc, long now, float pt, Vec3 cam) {
      Iterator<VerdictChainClient.Ghost> it = GHOSTS.iterator();

      while (it.hasNext()) {
         VerdictChainClient.Ghost g = it.next();
         float age = (float)(now - g.born) + pt;
         if (!(age >= 4.0F) && mc.level.getEntity(g.last.ownerId) instanceof Player owner) {
            Vec3 var13 = VerdictHandAnchor.hand(owner, pt, cam);
            Vec3 end = new Vec3(g.last.hx, g.last.hy, g.last.hz);
            ChainLinks.Chain ch = next().from(var13).to(end);
            ch.head = 2;
            ch.thickness = 1.3F;
            ch.seed = g.seed;
            ch.brightness = light(var13);
            float t = age / 4.0F;
            ch.growth = 1.0F - AnimEnvelopes.easeOutCubic(t);
            ch.sag = (float)var13.distanceTo(end) * 0.04F;
            ch.charge = g.last.charge * (1.0F - t);
         } else {
            it.remove();
         }
      }
   }

   private static void trails(PoseStack ps, BufferSource buffers, Vec3 cam) {
      if (!TRAILS.isEmpty()) {
         ps.pushPose();
         ps.translate(-cam.x, -cam.y, -cam.z);
         Pose pose = ps.last();
         VertexConsumer body = FxLayers.begin(buffers, FxLayer.PAINT);

         for (ChainLinks.Trail t : TRAILS) {
            t.ribbon(pose, body, 0.16F, 55, 6171526);
         }

         FxLayers.end(buffers, FxLayer.PAINT);
         VertexConsumer glow = FxLayers.begin(buffers, FxLayer.LIGHT);

         for (ChainLinks.Trail t : TRAILS) {
            t.ribbon(pose, glow, 0.16F, 100, 11544319);
         }

         FxLayers.end(buffers, FxLayer.LIGHT);
         ps.popPose();
      }
   }

   private static ChainLinks.Chain next() {
      int i = FRAME.size();
      if (i == POOL.size()) {
         POOL.add(new ChainLinks.Chain());
      }

      ChainLinks.Chain c = POOL.get(i).reset();
      FRAME.add(c);
      return c;
   }

   private static float light(Vec3 at) {
      Minecraft mc = Minecraft.getInstance();
      return mc.level == null ? 1.0F : (float)mc.level.getMaxLocalRawBrightness(BlockPos.containing(at)) / 15.0F;
   }

   private static final class Ghost {
      final VerdictChainClient.Hook last;
      final long born;
      final int seed;

      Ghost(VerdictChainClient.Hook last, long born, int seed) {
         this.last = last;
         this.born = born;
         this.seed = seed;
      }
   }

   private static final class Hook {
      byte mode = -1;
      long modeSince;
      long biteTick = -4611686018427387904L;
      int ownerId;
      double hx;
      double hy;
      double hz;
      float charge;
      long lastSeen;
   }

   private static final class Whirl {
      final ChainLinks.Trail trail = new ChainLinks.Trail(8);
      long biteTick = -4611686018427387904L;
      long lastSeen;
      double hx;
      double hy;
      double hz;
      float charge;
      long checkedTick = Long.MIN_VALUE;
   }
}
