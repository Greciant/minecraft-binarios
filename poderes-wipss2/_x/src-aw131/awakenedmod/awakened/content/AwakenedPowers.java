package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.content.aero.AeroPower;
import com.awakenedmod.awakened.content.brawler.BrawlerPower;
import com.awakenedmod.awakened.content.chronokinetic.ChronokineticPower;
import com.awakenedmod.awakened.content.command.CommandPower;
import com.awakenedmod.awakened.content.cryokinetic.CryokineticPower;
import com.awakenedmod.awakened.content.earth.EarthPower;
import com.awakenedmod.awakened.content.exosavant.ExosavantPower;
import com.awakenedmod.awakened.content.gamma.GammaPower;
import com.awakenedmod.awakened.content.graviton.GravitonPower;
import com.awakenedmod.awakened.content.hydro.HydrokineticPower;
import com.awakenedmod.awakened.content.kineticcharger.KineticChargerPower;
import com.awakenedmod.awakened.content.limitless.LimitlessPower;
import com.awakenedmod.awakened.content.nuclear.NuclearPower;
import com.awakenedmod.awakened.content.phytokinetic.PhytokineticPower;
import com.awakenedmod.awakened.content.pocket.RiftPower;
import com.awakenedmod.awakened.content.pyrokinetic.PyrokineticPower;
import com.awakenedmod.awakened.content.reaper.ReaperPower;
import com.awakenedmod.awakened.content.regenerator.RegeneratorPower;
import com.awakenedmod.awakened.content.ruin.RuinPower;
import com.awakenedmod.awakened.content.scaler.ScalerPower;
import com.awakenedmod.awakened.content.shadow.ShadowPower;
import com.awakenedmod.awakened.content.spider.SpiderPower;
import com.awakenedmod.awakened.content.stormcaller.StormCallerPower;
import com.awakenedmod.awakened.content.summoning.SummoningPower;
import com.awakenedmod.awakened.content.sun.SunPower;
import com.awakenedmod.awakened.content.teleporter.TeleporterPower;
import com.awakenedmod.awakened.content.velocity.VelocityPower;
import com.awakenedmod.awakened.core.power.PowerMigrations;
import com.awakenedmod.awakened.core.power.PowerRegistry;

public final class AwakenedPowers {
   private AwakenedPowers() {
   }

   public static void registerAll() {
      PowerRegistry.register(new TeleporterPower());
      PowerRegistry.register(new StormCallerPower());
      PowerRegistry.register(new PyrokineticPower());
      PowerRegistry.register(new RegeneratorPower());
      PowerRegistry.register(new CryokineticPower());
      PowerRegistry.register(new GravitonPower());
      PowerRegistry.register(new ChronokineticPower());
      PowerRegistry.register(new HydrokineticPower());
      PowerRegistry.register(new RiftPower());
      PowerRegistry.register(new NuclearPower());
      PowerRegistry.register(new ShadowPower());
      PowerRegistry.register(new SummoningPower());
      PowerRegistry.register(new SunPower());
      PowerRegistry.register(new ScalerPower());
      PowerRegistry.register(new SpiderPower());
      PowerRegistry.register(new ReaperPower());
      PowerRegistry.register(new EarthPower());
      PowerRegistry.register(new ExosavantPower());
      PowerRegistry.register(new GammaPower());
      PowerRegistry.register(new PhytokineticPower());
      PowerRegistry.register(new AeroPower());
      PowerRegistry.register(new BrawlerPower());
      PowerRegistry.register(new VelocityPower());
      PowerRegistry.register(new CommandPower());
      PowerRegistry.register(new KineticChargerPower());
      PowerRegistry.register(new RuinPower());
      PowerRegistry.register(new LimitlessPower());
      PowerMigrations.rename("gojo", "limitless");
      PowerMigrations.renameToggle("gojo_infinity", LimitlessPower.INFINITY);
      PowerMigrations.renameToggle("gojo_six_eyes", LimitlessPower.SIX_EYES);
   }
}
