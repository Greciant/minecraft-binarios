package com.devilfruits.client.animation;

import java.util.Locale;
import net.minecraft.util.Mth;

public enum Easing {
   LINEAR {
      @Override
      public float apply(float t) {
         return t;
      }
   },
   CATMULLROM {
      @Override
      public float apply(float t) {
         return t;
      }
   },
   STEP {
      @Override
      public float apply(float t) {
         return t < 1.0F ? 0.0F : 1.0F;
      }
   },
   IN_SINE {
      @Override
      public float apply(float t) {
         return 1.0F - Mth.cos(t * (float) (Math.PI / 2));
      }
   },
   OUT_SINE {
      @Override
      public float apply(float t) {
         return Mth.sin(t * (float) (Math.PI / 2));
      }
   },
   IN_OUT_SINE {
      @Override
      public float apply(float t) {
         return -(Mth.cos((float) Math.PI * t) - 1.0F) / 2.0F;
      }
   },
   IN_QUAD {
      @Override
      public float apply(float t) {
         return t * t;
      }
   },
   OUT_QUAD {
      @Override
      public float apply(float t) {
         return 1.0F - (1.0F - t) * (1.0F - t);
      }
   },
   IN_OUT_QUAD {
      @Override
      public float apply(float t) {
         return t < 0.5F ? 2.0F * t * t : 1.0F - (-2.0F * t + 2.0F) * (-2.0F * t + 2.0F) / 2.0F;
      }
   },
   IN_CUBIC {
      @Override
      public float apply(float t) {
         return t * t * t;
      }
   },
   OUT_CUBIC {
      @Override
      public float apply(float t) {
         float u = 1.0F - t;
         return 1.0F - u * u * u;
      }
   },
   IN_OUT_CUBIC {
      @Override
      public float apply(float t) {
         return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float)Math.pow((double)(-2.0F * t + 2.0F), 3.0) / 2.0F;
      }
   },
   IN_EXPO {
      @Override
      public float apply(float t) {
         return t == 0.0F ? 0.0F : (float)Math.pow(2.0, (double)(10.0F * t - 10.0F));
      }
   },
   OUT_EXPO {
      @Override
      public float apply(float t) {
         return t == 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, (double)(-10.0F * t));
      }
   },
   IN_BACK {
      @Override
      public float apply(float t) {
         return 2.70158F * t * t * t - 1.70158F * t * t;
      }
   },
   OUT_BACK {
      @Override
      public float apply(float t) {
         float u = t - 1.0F;
         return 1.0F + 2.70158F * u * u * u + 1.70158F * u * u;
      }
   },
   IN_OUT_BACK {
      @Override
      public float apply(float t) {
         float c2 = 2.5949094F;
         return t < 0.5F
            ? (float)(Math.pow((double)(2.0F * t), 2.0) * (double)((c2 + 1.0F) * 2.0F * t - c2)) / 2.0F
            : (float)(Math.pow((double)(2.0F * t - 2.0F), 2.0) * (double)((c2 + 1.0F) * (t * 2.0F - 2.0F) + c2) + 2.0) / 2.0F;
      }
   },
   OUT_ELASTIC {
      @Override
      public float apply(float t) {
         return t != 0.0F && t != 1.0F
            ? (float)(Math.pow(2.0, (double)(-10.0F * t)) * Math.sin(((double)(t * 10.0F) - 0.75) * (Math.PI * 2.0 / 3.0)) + 1.0)
            : t;
      }
   },
   OUT_BOUNCE {
      @Override
      public float apply(float t) {
         float n = 7.5625F;
         float d = 2.75F;
         if (t < 1.0F / d) {
            return n * t * t;
         } else if (t < 2.0F / d) {
            t -= 1.5F / d;
            return n * t * t + 0.75F;
         } else if ((double)t < 2.5 / (double)d) {
            t -= 2.25F / d;
            return n * t * t + 0.9375F;
         } else {
            t -= 2.625F / d;
            return n * t * t + 0.984375F;
         }
      }
   };

   public abstract float apply(float var1);

   public static Easing parse(String name) {
      try {
         return valueOf(name.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException var2) {
         return LINEAR;
      }
   }
}
