package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.content.quest.pools.AeroQuests;
import com.awakenedmod.awakened.content.quest.pools.BrawlerQuests;
import com.awakenedmod.awakened.content.quest.pools.ChronokineticQuests;
import com.awakenedmod.awakened.content.quest.pools.CommandQuests;
import com.awakenedmod.awakened.content.quest.pools.CryokineticQuests;
import com.awakenedmod.awakened.content.quest.pools.EarthQuests;
import com.awakenedmod.awakened.content.quest.pools.ExosavantQuests;
import com.awakenedmod.awakened.content.quest.pools.GammaQuests;
import com.awakenedmod.awakened.content.quest.pools.GravitonQuests;
import com.awakenedmod.awakened.content.quest.pools.HydrokineticQuests;
import com.awakenedmod.awakened.content.quest.pools.KineticQuests;
import com.awakenedmod.awakened.content.quest.pools.LimitlessQuests;
import com.awakenedmod.awakened.content.quest.pools.NuclearQuests;
import com.awakenedmod.awakened.content.quest.pools.PhytokineticQuests;
import com.awakenedmod.awakened.content.quest.pools.PyrokineticQuests;
import com.awakenedmod.awakened.content.quest.pools.ReaperQuests;
import com.awakenedmod.awakened.content.quest.pools.RegeneratorQuests;
import com.awakenedmod.awakened.content.quest.pools.RiftQuests;
import com.awakenedmod.awakened.content.quest.pools.RuinQuests;
import com.awakenedmod.awakened.content.quest.pools.ScalerQuests;
import com.awakenedmod.awakened.content.quest.pools.ShadowQuests;
import com.awakenedmod.awakened.content.quest.pools.SpiderQuests;
import com.awakenedmod.awakened.content.quest.pools.StormCallerQuests;
import com.awakenedmod.awakened.content.quest.pools.SummoningQuests;
import com.awakenedmod.awakened.content.quest.pools.SunQuests;
import com.awakenedmod.awakened.content.quest.pools.TeleporterQuests;
import com.awakenedmod.awakened.content.quest.pools.UniversalQuests;
import com.awakenedmod.awakened.content.quest.pools.VelocityQuests;
import com.awakenedmod.awakened.core.quest.QuestRegistry;

public final class AwakenedQuests {
   private AwakenedQuests() {
   }

   public static void registerAll() {
      QuestRegistry.beginRegistration();
      UniversalQuests.register();
      RegeneratorQuests.register();
      PyrokineticQuests.register();
      CryokineticQuests.register();
      StormCallerQuests.register();
      TeleporterQuests.register();
      VelocityQuests.register();
      KineticQuests.register();
      GravitonQuests.register();
      ChronokineticQuests.register();
      ShadowQuests.register();
      SunQuests.register();
      SummoningQuests.register();
      HydrokineticQuests.register();
      RiftQuests.register();
      NuclearQuests.register();
      ScalerQuests.register();
      SpiderQuests.register();
      ReaperQuests.register();
      EarthQuests.register();
      ExosavantQuests.register();
      GammaQuests.register();
      PhytokineticQuests.register();
      AeroQuests.register();
      BrawlerQuests.register();
      CommandQuests.register();
      RuinQuests.register();
      LimitlessQuests.register();
   }
}
