package com.devilfruits.fruits.gura_gura.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.fruits.gura_gura.GuraGura;
import com.devilfruits.fruits.suna_suna.SunaAbilities;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import com.devilfruits.registry.DFAttachments;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class QuakeDeflectCheck extends CaptureStage {
   private static final UUID SUNA = UUID.nameUUIDFromBytes("quake_deflect_suna".getBytes());

   public QuakeDeflectCheck() {
      super("vfx_qd_");
   }

   @Override
   protected ResourceLocation fruit() {
      return GuraGura.ID;
   }

   @Override
   protected void setup(ServerPlayer p) {
      DFAttachments.data(p).setMastery(10);
      command(p, "time set 6000");
   }

   private static FakePlayer suna(ServerPlayer p) {
      FakePlayer f = FakePlayerFactory.get(p.serverLevel(), new GameProfile(SUNA, "sand_thrower"));
      if (!SunaSuna.ID.equals(DFAttachments.data(f).fruitId())) {
         FruitRules.grantPower(f, SunaSuna.fruit(), false);
      }

      PlayerFruitData data = DFAttachments.data(f);
      Mastery.setLevel(f, data, 10, false);
      data.clearCooldowns();
      data.setEnergy((float)Mastery.maxEnergy(data));
      f.moveTo(p.getX(), p.getY(), p.getZ() - 6.0, 0.0F, 0.0F);
      f.setYHeadRot(0.0F);
      return f;
   }

   @Override
   protected void onReset(ServerPlayer p) {
      PlayerFruitData data = DFAttachments.data(p);
      data.clearCooldowns();
      data.setEnergy((float)Mastery.maxEnergy(data));
   }

   @Override
   protected int takes(AutoTest s, int t) {
      Consumer<ServerPlayer> away = p -> dummyTo(s, p, -40.0);
      t += 30;
      t = this.take(s, t, 8, away, 0, p -> {
         FakePlayer f = suna(p);
         AbilityManager.press(f, SunaAbilities.SABLES_PESADO);
      }, rig(8.0, 110.0F, 8.0F, 0.4), 40, c -> {
         s.add(c + 12, mc -> s.server(mc, p -> AbilityManager.release(suna(p), SunaAbilities.SABLES_PESADO)));
         s.add(c + 13, mc -> s.server(mc, p -> castAbility(p, "devilfruits:gura_gura/kaishin", 1)));

         for (int k = 12; k <= 30; k++) {
            int kk = k;
            s.add(c + k, mc -> s.shot(mc, String.format("vfx_qd_ball_%02d", kk)));
         }
      });
      t = this.take(s, t, 8, away, 0, p -> AbilityManager.press(suna(p), SunaAbilities.DESERT_LA_SPADA), rig(8.0, 110.0F, 8.0F, 0.4), 50, c -> {
         s.add(c + 7, mc -> s.server(mc, p -> castAbility(p, "devilfruits:gura_gura/kaishin", 1)));

         for (int k = 4; k <= 24; k++) {
            int kk = k;
            s.add(c + k, mc -> s.shot(mc, String.format("vfx_qd_blades_%02d", kk)));
         }
      });
      s.add(t, mc -> s.server(mc, p -> FruitRules.removePower(suna(p))));
      return t + 2;
   }
}
