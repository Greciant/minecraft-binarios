package com.devilfruits;

import com.devilfruits.core.fruit.FruitModuleContext;
import com.devilfruits.core.fruit.FruitModules;
import com.devilfruits.core.gametest.fixture.DebugLogia;
import com.devilfruits.core.gametest.fixture.DebugZoan;
import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.gomu_gomu.GomuGomu;
import com.devilfruits.fruits.gura_gura.GuraGura;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kilo_kilo.KiloKilo;
import com.devilfruits.fruits.suke_suke.SukeSuke;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import com.devilfruits.fruits.supa_supa.SupaSupa;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import net.neoforged.fml.loading.FMLEnvironment;

final class BuiltinFruits {
   static void register(FruitModuleContext ctx) {
      FruitModules.add(new GuraGura(), ctx);
      FruitModules.add(new UoUoSeiryu(), ctx);
      FruitModules.add(new SunaSuna(), ctx);
      FruitModules.add(new KiloKilo(), ctx);
      FruitModules.add(new BomuBomu(), ctx);
      FruitModules.add(new DoruDoru(), ctx);
      FruitModules.add(new SupaSupa(), ctx);
      FruitModules.add(new KageKage(), ctx);
      FruitModules.add(new YomiYomi(), ctx);
      FruitModules.add(new HoroHoro(), ctx);
      FruitModules.add(new SukeSuke(), ctx);
      FruitModules.add(new GomuGomu(), ctx);
      if (!FMLEnvironment.production) {
         FruitModules.add(new DebugZoan(), ctx);
         FruitModules.add(new DebugLogia(), ctx);
      }
   }

   private BuiltinFruits() {
   }
}
