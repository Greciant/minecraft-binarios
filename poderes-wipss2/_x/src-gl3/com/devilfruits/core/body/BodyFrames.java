package com.devilfruits.core.body;

public final class BodyFrames {
   public final int capacity;
   public int count;
   public final double[] x;
   public final double[] y;
   public final double[] z;
   public final float[] tx;
   public final float[] ty;
   public final float[] tz;
   public final float[] ux;
   public final float[] uy;
   public final float[] uz;
   public final float[] sx;
   public final float[] sy;
   public final float[] sz;
   public final float[] radius;
   public final float[] distance;

   public BodyFrames(int capacity) {
      this.capacity = capacity;
      this.x = new double[capacity];
      this.y = new double[capacity];
      this.z = new double[capacity];
      this.tx = new float[capacity];
      this.ty = new float[capacity];
      this.tz = new float[capacity];
      this.ux = new float[capacity];
      this.uy = new float[capacity];
      this.uz = new float[capacity];
      this.sx = new float[capacity];
      this.sy = new float[capacity];
      this.sz = new float[capacity];
      this.radius = new float[capacity];
      this.distance = new float[capacity];
   }
}
