package com.awakenedmod.awakened.client.ruin;

import com.awakenedmod.awakened.content.ruin.RuinAttachments;
import com.awakenedmod.awakened.content.ruin.RuinDecay;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class RuinDecayBodies {
   static final int DEATH_TICKS = 20;
   private static final Int2ObjectOpenHashMap<RuinDecayBodies.Body> BODIES = new Int2ObjectOpenHashMap();
   private static final RandomSource RANDOM = RandomSource.create();
   private static int ticks;

   private RuinDecayBodies() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(RuinDecayBodies::onClientTick);
   }

   private static void onClientTick(Post event) {
      tick(Minecraft.getInstance());
   }

   static void tick(Minecraft mc) {
      if (mc.level != null && !mc.isPaused()) {
         ticks++;
         ObjectIterator now = BODIES.values().iterator();

         while (now.hasNext()) {
            RuinDecayBodies.Body body = (RuinDecayBodies.Body)now.next();
            body.seen = false;
         }

         long nowx = mc.level.getGameTime();
         float flashStep = 1.0F / (float)Math.max(1, RuinLook.FLASH_TICKS.getI());
         int crumbleTicks = Math.max(1, RuinLook.CRUMBLE_TICKS.getI());
         RuinDust.beginTick();

         for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity) {
               LivingEntity living = (LivingEntity)entity;
               RuinDecay decay = living.hasData(RuinAttachments.DECAY) ? (RuinDecay)living.getData(RuinAttachments.DECAY) : RuinDecay.NONE;
               RuinDecayBodies.Body body = (RuinDecayBodies.Body)BODIES.get(living.getId());
               boolean dead = living.deathTime > 0 || living.isDeadOrDying();
               if (body != null && body.crumbling && !dead) {
                  BODIES.remove(body.id);
                  body = null;
               }

               if (body == null) {
                  if (!decay.active() || dead && living.deathTime > 2) {
                     continue;
                  }

                  body = new RuinDecayBodies.Body(living.getId());
                  body.lastSeq = decay.pulseSeq();
                  BODIES.put(body.id, body);
               }

               body.seen = true;
               body.mine = mc.player != null && decay.appliedBy(mc.player);
               body.expiresAt = decay.expiresAt();
               body.prevFlash = body.flash;
               body.prevPop = body.pop;
               body.prevLevel = body.level;
               if (decay.pulseSeq() != body.lastSeq) {
                  body.lastSeq = decay.pulseSeq();
                  if (nowx - decay.pulseTick() <= 40L) {
                     pulse(living, body, decay);
                  }
               }

               body.flash = Math.max(0.0F, body.flash - flashStep);
               body.pop = Math.max(0.0F, body.pop - 0.12F);
               if (dead && !body.crumbling && (body.level > 0.5F || decay.active())) {
                  body.crumbling = true;
                  body.crumbleTicks = 0;
                  body.stacks = Math.max(body.stacks, decay.stacks());
                  body.level = Math.max(body.level, (float)body.stacks);
               }

               if (body.crumbling) {
                  body.crumbleTicks++;
                  RuinDust.crumble(mc, living, Math.min(1.0F, (float)body.crumbleTicks / (float)crumbleTicks));
               } else {
                  body.stacks = decay.stacks();
                  ramp(body, decay.stacks());
                  RuinDust.tick(mc, living, body);
               }

               if (!body.crumbling && body.level <= 0.0F && !decay.active() && body.flash <= 0.0F) {
                  BODIES.remove(body.id);
               }
            }
         }

         BODIES.values().removeIf(b -> !b.seen);
      } else {
         if (mc.level == null) {
            BODIES.clear();
         }
      }
   }

   private static void pulse(LivingEntity living, RuinDecayBodies.Body body, RuinDecay decay) {
      switch (decay.pulseKind()) {
         case 1:
            body.pop = 1.0F;
            break;
         case 2:
            body.flash = 1.0F;
            RuinDust.payout(Minecraft.getInstance(), living, decay.pulseAmount());
         case 3:
         default:
            break;
         case 4:
            body.level = 0.0F;
      }
   }

   private static void ramp(RuinDecayBodies.Body body, int target) {
      if ((float)target > body.level) {
         float gap = (float)target - body.level;
         body.level = Math.min((float)target, body.level + Math.max(0.6F, gap * 0.4F));
      } else if ((float)target < body.level) {
         float gap = body.level - (float)target;
         body.level = Math.max((float)target, body.level - Math.max(0.2F, gap * 0.14F));
      }
   }

   @Nullable
   public static RuinLook.Look of(LivingEntity entity) {
      RuinDecayBodies.Body body = (RuinDecayBodies.Body)BODIES.get(entity.getId());
      if (body == null) {
         return null;
      } else {
         float pt = partialTick();
         if (body.memoTick == ticks && body.memoPt == pt) {
            return body.memo;
         } else {
            body.memo = build(body, pt);
            body.memoTick = ticks;
            body.memoPt = pt;
            return body.memo;
         }
      }
   }

   @Nullable
   private static RuinLook.Look build(RuinDecayBodies.Body body, float pt) {
      float level = Mth.lerp(pt, body.prevLevel, body.level);
      float flash = Mth.lerp(pt, body.prevFlash, body.flash);
      float pop = Mth.lerp(pt, body.prevPop, body.pop);
      float crumble = -1.0F;
      if (body.crumbling) {
         float ticksIn = (float)(body.crumbleTicks - 1) + pt;
         crumble = Mth.clamp(ticksIn / (float)Math.max(1, RuinLook.CRUMBLE_TICKS.getI()), 0.0F, 1.0F);
         level = body.level;
      }

      if (level <= 0.0F && flash <= 0.0F && crumble < 0.0F) {
         return null;
      } else {
         float depth = RuinLook.depthOf(level);
         float grey = RuinLook.greyOf(level, RuinLook.GREY_START.getF(), RuinLook.GREY_FULL.getF()) * RuinLook.GREY_STRENGTH.getF();
         if (crumble >= 0.0F) {
            grey = 1.0F;
         }

         Minecraft mc = Minecraft.getInstance();
         float left = mc.level == null ? Float.MAX_VALUE : (float)body.expiresAt - ((float)mc.level.getGameTime() + pt);
         return new RuinLook.Look(body.id, body.stacks, level, depth, grey, flash, crumble, body.mine, pop, left);
      }
   }

   public static float clock() {
      return (float)ticks + partialTick();
   }

   static RandomSource random() {
      return RANDOM;
   }

   private static float partialTick() {
      Minecraft mc = Minecraft.getInstance();
      return mc.getTimer().getGameTimeDeltaPartialTick(false);
   }

   static final class Body {
      final int id;
      int stacks;
      float level;
      float prevLevel;
      float flash;
      float prevFlash;
      float pop;
      float prevPop;
      int lastSeq = -1;
      boolean crumbling;
      int crumbleTicks;
      boolean mine;
      boolean seen;
      long expiresAt;
      @Nullable
      RuinLook.Look memo;
      int memoTick = -1;
      float memoPt = Float.NaN;

      Body(int id) {
         this.id = id;
      }
   }
}
