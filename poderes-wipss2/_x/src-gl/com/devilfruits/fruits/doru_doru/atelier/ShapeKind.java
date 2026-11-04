package com.devilfruits.fruits.doru_doru.atelier;

import java.util.Locale;

public enum ShapeKind {
   WALL(2, p("height", 3.0, 0.25, 32.0), p("thickness", 1.0, 0.25, 8.0)),
   BOX(2, p("thickness", 1.0, 0.25, 16.0)),
   COLUMN(2, p("radius", 1.0, 0.25, 16.0)),
   CONE(2, p("radius", 2.0, 0.25, 16.0)),
   SPHERE(1, p("radius", 3.0, 0.5, 16.0)),
   DOME(1, p("radius", 4.0, 0.5, 16.0)),
   ARCH(2, p("rise", 4.0, 0.5, 32.0), p("thickness", 1.0, 0.25, 8.0), p("depth", 1.0, 0.25, 8.0)),
   BRIDGE(3, p("thickness", 1.0, 0.25, 8.0)),
   RAMP(2, p("width", 3.0, 0.25, 16.0), p("thickness", 1.0, 0.25, 8.0)),
   STAIRS(2, p("width", 3.0, 0.25, 16.0), p("thickness", 1.0, 0.25, 8.0)),
   SPIRAL(2, p("radius", 4.0, 1.0, 16.0), p("width", 2.0, 0.5, 8.0), p("pitch", 6.0, 1.0, 32.0)),
   RING(1, p("radius", 4.0, 0.5, 16.0), p("thickness", 1.0, 0.25, 8.0)),
   LOFT(2, p("bottom", 3.0, 0.25, 16.0), p("top", 1.5, 0.0, 16.0));

   private final int points;
   private final ShapeKind.Param[] params;

   private ShapeKind(int points, ShapeKind.Param... params) {
      this.points = points;
      this.params = params;
   }

   private static ShapeKind.Param p(String name, double def, double min, double max) {
      return new ShapeKind.Param(name, (float)def, (float)min, (float)max);
   }

   public int points() {
      return this.points;
   }

   public ShapeKind.Param[] params() {
      return this.params;
   }

   public String key() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   public static ShapeKind of(int ordinal) {
      ShapeKind[] all = values();
      return all[Math.floorMod(ordinal, all.length)];
   }

   public static record Param(String name, float def, float min, float max) {
      public float step(float v) {
         return v >= 12.0F ? 1.0F : (v >= 4.0F ? 0.5F : 0.25F);
      }

      public float clamp(float v) {
         return Math.max(this.min, Math.min(this.max, v));
      }
   }
}
