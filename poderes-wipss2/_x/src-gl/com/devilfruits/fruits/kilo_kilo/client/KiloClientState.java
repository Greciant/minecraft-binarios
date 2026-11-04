package com.devilfruits.fruits.kilo_kilo.client;

import com.devilfruits.fruits.kilo_kilo.KiloKilo;
import com.devilfruits.fruits.kilo_kilo.weight.BodyWeight;
import com.devilfruits.fruits.kilo_kilo.weight.WeightCurves;
import com.devilfruits.fruits.kilo_kilo.weight.WeightManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class KiloClientState {
   private static final Int2ObjectMap<double[]> WEIGHTS = new Int2ObjectOpenHashMap();

   static void tick(ClientLevel level) {
      double rate = WeightManager.defaultRate();

      for (AbstractClientPlayer p : level.players()) {
         Optional<BodyWeight> w = p.getExistingData(KiloKilo.WEIGHT);
         if (w.isEmpty()) {
            WEIGHTS.remove(p.getId());
         } else {
            BodyWeight bw = w.get();
            bw.advance(rate);
            double[] st = (double[])WEIGHTS.computeIfAbsent(p.getId(), k -> new double[]{bw.kg(), bw.kg()});
            st[0] = st[1];
            st[1] = bw.kg();
         }
      }

      WEIGHTS.keySet().removeIf(id -> level.getEntity(id) == null);
   }

   static void clear() {
      WEIGHTS.clear();
   }

   public static double kg(Player player, float partialTick) {
      double[] st = (double[])WEIGHTS.get(player.getId());
      return st == null ? 70.0 : Math.exp(Mth.lerp((double)partialTick, Math.log(st[0]), Math.log(st[1])));
   }

   public static double scale(Player player, float partialTick) {
      return WeightCurves.scale(kg(player, partialTick));
   }

   @Nullable
   public static BodyWeight weight(Player player) {
      return (BodyWeight)player.getExistingData(KiloKilo.WEIGHT).orElse(null);
   }

   private KiloClientState() {
   }
}
