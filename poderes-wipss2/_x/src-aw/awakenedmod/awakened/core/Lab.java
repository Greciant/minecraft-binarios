package com.awakenedmod.awakened.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Lab {
   public static final Lab SERVER = new Lab("server");
   public static final Lab CLIENT = new Lab("client");
   private final String side;
   private final Map<String, Lab.Value> declared = new ConcurrentHashMap<>();
   private final Map<String, Double> live = new ConcurrentHashMap<>();

   private Lab(String side) {
      this.side = side;
   }

   public String side() {
      return this.side;
   }

   public Lab.Value def(String key, double fallback) {
      double span = Math.abs(fallback) < 1.0E-9 ? 1.0 : Math.abs(fallback) * 4.0;
      return this.def(key, fallback, Math.min(0.0, fallback), Math.max(span, fallback), 0.0);
   }

   public Lab.Value def(String key, double fallback, double min, double max, double step) {
      Lab.Value existing = this.declared.get(key);
      if (existing != null) {
         return existing;
      } else {
         Lab.Value value = new Lab.Value(key, fallback, min, max, step);
         Lab.Value raced = this.declared.putIfAbsent(key, value);
         return raced != null ? raced : value;
      }
   }

   public List<Lab.Value> all() {
      List<Lab.Value> out = new ArrayList<>(this.declared.values());
      out.sort((a, b) -> a.key.compareTo(b.key));
      return out;
   }

   public List<String> keys() {
      List<String> out = new ArrayList<>(this.declared.keySet());
      out.sort(String::compareTo);
      return out;
   }

   public Lab.Value value(String key) {
      return this.declared.get(key);
   }

   public boolean set(String key, double raw) {
      Lab.Value value = this.declared.get(key);
      if (value == null) {
         return false;
      } else {
         double v = value.clamp(raw);
         if (Math.abs(v - value.fallback) < 1.0E-9) {
            this.live.remove(key);
         } else {
            this.live.put(key, v);
         }

         return true;
      }
   }

   public boolean reset(String key) {
      if (!this.declared.containsKey(key)) {
         return false;
      } else {
         this.live.remove(key);
         return true;
      }
   }

   public void resetAll() {
      this.live.clear();
   }

   public int changedCount() {
      return this.live.size();
   }

   public List<String> list() {
      List<String> out = new ArrayList<>();

      for (Lab.Value v : this.all()) {
         out.add(v.changed() ? v.key + " = " + num(v.get()) + "   (default " + num(v.fallback) + ")" : v.key + " = " + num(v.get()));
      }

      return out;
   }

   public List<String> changedLines() {
      List<String> out = new ArrayList<>();

      for (Lab.Value v : this.all()) {
         if (v.changed()) {
            out.add(v.key + "=" + num(v.get()));
         }
      }

      return out;
   }

   public static String num(double v) {
      if (v == Math.rint(v) && Math.abs(v) < 1.0E9) {
         return String.valueOf((long)v);
      } else {
         String s = String.format(Locale.ROOT, "%.4f", v);

         while (s.endsWith("0")) {
            s = s.substring(0, s.length() - 1);
         }

         return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
      }
   }

   public final class Value {
      private final String key;
      private final double fallback;
      private final double min;
      private final double max;
      private final double step;

      private Value(String key, double fallback, double min, double max, double step) {
         this.key = key;
         this.fallback = fallback;
         this.min = min;
         this.max = max;
         this.step = step;
      }

      public double get() {
         Double v = Lab.this.live.get(this.key);
         return v == null ? this.fallback : v;
      }

      public float getF() {
         return (float)this.get();
      }

      public int getI() {
         return (int)Math.round(this.get());
      }

      public String key() {
         return this.key;
      }

      public double defaultValue() {
         return this.fallback;
      }

      public double min() {
         return this.min;
      }

      public double max() {
         return this.max;
      }

      public double step() {
         return this.step;
      }

      public boolean changed() {
         return Lab.this.live.containsKey(this.key);
      }

      public double clamp(double raw) {
         double v = Math.max(this.min, Math.min(this.max, raw));
         if (this.step > 0.0) {
            v = (double)Math.round(v / this.step) * this.step;
            v = Math.max(this.min, Math.min(this.max, v));
         }

         return v;
      }
   }
}
