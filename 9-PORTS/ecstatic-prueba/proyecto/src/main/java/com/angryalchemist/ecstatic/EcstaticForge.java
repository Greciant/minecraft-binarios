package com.angryalchemist.ecstatic;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

// Port Forge 1.20.1: NeoForge.EVENT_BUS -> MinecraftForge.EVENT_BUS. El mod solo hace algo en
// cliente con servidor integrado (un jugador); en servidor dedicado no carga nada.
@Mod("ecstatic")
public class EcstaticForge {
    public EcstaticForge() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
            () -> new IExtensionPoint.DisplayTest(() -> IExtensionPoint.DisplayTest.IGNORESERVERONLY, (a, b) -> true));
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        Ecstatic.init();
        MinecraftForge.EVENT_BUS.register(this);
        com.angryalchemist.ecstatic.dev.AutoTest.init();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        Ecstatic.onServerStarted(event.getServer().overworld());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        Ecstatic.onServerStopping(event.getServer());
    }
}
