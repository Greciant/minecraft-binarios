package com.devilfruits.client.limb;

import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.limb.LimbUv;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;

public final class Limbs {
   public static final int SWARM_FACTOR = 3;
   private static final Int2ObjectOpenHashMap<List<Limbs.Active>> BY_ENTITY = new Int2ObjectOpenHashMap();
   private static final ArrayDeque<Limbs.Active> ORDER = new ArrayDeque<>();
   private static final ArrayDeque<Limbs.Active> SWARM = new ArrayDeque<>();

   public static Limbs.Active add(Entity entity, LimbSpec spec, LimbDriver driver) {
      Limbs.Active a = new Limbs.Active(entity.getId(), spec, driver);
      ArrayDeque<Limbs.Active> queue = spec.swarm ? SWARM : ORDER;
      int cap = spec.swarm ? cap() * 3 : cap();

      while (queue.size() >= cap) {
         Limbs.Active old = queue.pollFirst();
         if (old != null) {
            old.done = true;
            remove(old);
         }
      }

      ((List)BY_ENTITY.computeIfAbsent(entity.getId(), k -> new ArrayList(2))).add(a);
      queue.addLast(a);
      return a;
   }

   public static List<Limbs.Active> of(Entity entity) {
      List<Limbs.Active> list = BY_ENTITY.isEmpty() ? null : (List)BY_ENTITY.get(entity.getId());
      return list == null ? Collections.emptyList() : list;
   }

   public static boolean any() {
      return !BY_ENTITY.isEmpty();
   }

   public static boolean replaces(Entity entity, LimbUv.Limb limb, LimbUv.Side side) {
      for (Limbs.Active a : of(entity)) {
         if (!a.done && a.spec.replaces && a.spec.limb == limb && a.spec.side == side) {
            return true;
         }
      }

      return false;
   }

   public static boolean replacesArm(Entity entity, HumanoidArm arm) {
      return replaces(entity, LimbUv.Limb.ARM, arm == HumanoidArm.RIGHT ? LimbUv.Side.RIGHT : LimbUv.Side.LEFT);
   }

   public static void hideArmor(Entity entity, HumanoidModel<?> model) {
      if (!BY_ENTITY.isEmpty() && !(Boolean)CoreClientConfig.SHOW_ARMOR_ON_STRETCH.get()) {
         if (replaces(entity, LimbUv.Limb.ARM, LimbUv.Side.RIGHT)) {
            model.rightArm.visible = false;
         }

         if (replaces(entity, LimbUv.Limb.ARM, LimbUv.Side.LEFT)) {
            model.leftArm.visible = false;
         }

         if (replaces(entity, LimbUv.Limb.LEG, LimbUv.Side.RIGHT)) {
            model.rightLeg.visible = false;
         }

         if (replaces(entity, LimbUv.Limb.LEG, LimbUv.Side.LEFT)) {
            model.leftLeg.visible = false;
         }
      }
   }

   public static boolean keepsDrawn(Entity entity, double camX, double camY, double camZ) {
      return !BY_ENTITY.isEmpty() && !of(entity).isEmpty() && entity.distanceToSqr(camX, camY, camZ) < 16384.0;
   }

   public static void clear(Entity entity) {
      List<Limbs.Active> list = (List<Limbs.Active>)BY_ENTITY.remove(entity.getId());
      if (list != null) {
         list.forEach(a -> a.done = true);
         ORDER.removeAll(list);
         SWARM.removeAll(list);
      }
   }

   public static void clearAll() {
      BY_ENTITY.clear();
      ORDER.clear();
      SWARM.clear();
   }

   static void prune() {
      if (!BY_ENTITY.isEmpty()) {
         ClientLevel level = Minecraft.getInstance().level;
         ORDER.removeIf(a -> gone(a, level));
         SWARM.removeIf(a -> gone(a, level));
      }
   }

   private static boolean gone(Limbs.Active a, @Nullable ClientLevel level) {
      boolean gone = a.done || level == null || level.getEntity(a.entity) == null;
      if (gone) {
         a.done = true;
         remove(a);
      }

      return gone;
   }

   private static void remove(Limbs.Active a) {
      List<Limbs.Active> list = (List<Limbs.Active>)BY_ENTITY.get(a.entity);
      if (list != null) {
         list.remove(a);
         if (list.isEmpty()) {
            BY_ENTITY.remove(a.entity);
         }
      }
   }

   private static int cap() {
      int max = (Integer)CoreClientConfig.MAX_LIMBS.get();
      CoreClientConfig.CinematicQuality q = (CoreClientConfig.CinematicQuality)CoreClientConfig.CINEMATIC_QUALITY.get();
      return q != CoreClientConfig.CinematicQuality.ULTRA && q != CoreClientConfig.CinematicQuality.HIGH ? Math.max(2, max / 2) : max;
   }

   private Limbs() {
   }

   public static final class Active {
      public final LimbSpec spec;
      final LimbDriver driver;
      final int entity;
      boolean done;

      private Active(int entity, LimbSpec spec, LimbDriver driver) {
         this.entity = entity;
         this.spec = spec;
         this.driver = driver;
      }

      public void end() {
         this.done = true;
      }

      public boolean done() {
         return this.done;
      }
   }
}
