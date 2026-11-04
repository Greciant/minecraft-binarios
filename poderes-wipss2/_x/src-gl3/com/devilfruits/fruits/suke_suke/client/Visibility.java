package com.devilfruits.fruits.suke_suke.client;

import com.devilfruits.fruits.suke_suke.SukeSuke;
import com.devilfruits.fruits.suke_suke.clear.Clear;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class Visibility {
   private static Visibility.Viewer forced = Visibility.Viewer.REAL;

   public static void viewer(Visibility.Viewer viewer) {
      forced = viewer;
   }

   public static Visibility.Viewer viewer() {
      return forced;
   }

   public static Visibility.Mode of(Entity target) {
      Minecraft mc = Minecraft.getInstance();
      Entity viewer = (Entity)(mc.getCameraEntity() instanceof Player p ? p : mc.player);
      return of(viewer, target);
   }

   public static Visibility.Mode of(@Nullable Entity viewer, Entity target) {
      return Clear.invisible(target) ? relation(viewer, target) : Visibility.Mode.NORMAL;
   }

   public static Visibility.Mode relation(Entity target) {
      Minecraft mc = Minecraft.getInstance();
      return relation((Entity)(mc.getCameraEntity() instanceof Player p ? p : mc.player), target);
   }

   public static Visibility.Mode relation(@Nullable Entity viewer, Entity target) {
      switch (forced) {
         case SELF:
            return viewer == target ? Visibility.Mode.SELF : Visibility.Mode.ALLY;
         case ALLY:
            return Visibility.Mode.ALLY;
         case STRANGER:
            return Visibility.Mode.HIDDEN;
         default:
            if (viewer == target) {
               return Visibility.Mode.SELF;
            } else {
               if (viewer instanceof Player p && p.isSpectator() && SukeSuke.flag("spectatorsSee")) {
                  return Visibility.Mode.SPECTATOR;
               }

               return Clear.sees(viewer, target, false) ? Visibility.Mode.ALLY : Visibility.Mode.HIDDEN;
            }
      }
   }

   public static boolean hidden(Entity target) {
      return Clear.invisible(target) && relation(target) == Visibility.Mode.HIDDEN && ClearTransitions.gone(target);
   }

   public static boolean itemsHidden(Entity target) {
      return !Clear.hiddenItems(target)
         ? false
         : forced == Visibility.Viewer.STRANGER
            || forced == Visibility.Viewer.ALLY
            || target != Minecraft.getInstance().getCameraEntity() && target != Minecraft.getInstance().player;
   }

   private Visibility() {
   }

   public static enum Mode {
      NORMAL,
      SELF,
      ALLY,
      SPECTATOR,
      HIDDEN;

      public boolean glass() {
         return this == SELF || this == ALLY;
      }
   }

   public static enum Viewer {
      REAL,
      SELF,
      ALLY,
      STRANGER;
   }
}
