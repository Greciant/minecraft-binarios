package com.angryalchemist.ecstatic.dev;

import com.angryalchemist.ecstatic.Constants;
import com.angryalchemist.ecstatic.render.LodRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

// Solo para probar el port: con -Decstatic.autotest=true crea un mundo normal nuevo (semilla fija),
// sube al jugador (espectador) a y=220, espera a que haya LOD, saca capturas mirando al horizonte
// en 4 direcciones, mide FPS con y sin LOD y cierra el juego.
public final class AutoTest {
    private static final long SEED = 20261005L;
    private static final int MAX_WAIT_TICKS = 20 * 60 * 6; // 6 min como maximo esperando LOD
    private static boolean worldStarted = false;
    private static int ticks = 0;
    private static int phase = 0;
    private static int phaseStart = 0;
    private static int lastMeshCount = -1;
    private static int stableChecks = 0;
    private static long fpsSum = 0;
    private static int fpsSamples = 0;
    private static long nanosAtStart;
    private static long callsAtStart;
    private static double fpsOn = -1;

    private AutoTest() {
    }

    public static void init() {
        if (!Boolean.getBoolean("ecstatic.autotest")) return;
        MinecraftForge.EVENT_BUS.addListener(AutoTest::onScreenInit);
        MinecraftForge.EVENT_BUS.addListener(AutoTest::onTick);
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        if (worldStarted || !(event.getScreen() instanceof TitleScreen)) return;
        worldStarted = true;
        Minecraft mc = Minecraft.getInstance();
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        LevelSettings settings = new LevelSettings("ectest" + System.currentTimeMillis(), GameType.CREATIVE, false,
                Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(SEED, false, false),
                WorldPresets::createNormalWorldDimensions);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "ectest-" + name + ".png", mc.getMainRenderTarget(),
                c -> Constants.LOG.info("[autotest] captura {}", name));
    }

    private static void next(int newPhase) {
        phase = newPhase;
        phaseStart = ticks;
    }

    private static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ticks++;
        var p = mc.player;
        String name = p.getGameProfile().getName();
        int t = ticks - phaseStart;
        switch (phase) {
            case 0 -> {
                if (ticks == 100) {
                    p.connection.sendCommand("time set noon");
                    p.connection.sendCommand("gamemode spectator " + name);
                    p.connection.sendCommand("tp " + name + " " + (int) p.getX() + " 220 " + (int) p.getZ() + " 0 8");
                    Constants.LOG.info("[autotest] jugador a y=220 en {} {} (renderDistance={})", (int) p.getX(), (int) p.getZ(),
                            mc.options.getEffectiveRenderDistance());
                    next(1);
                }
            }
            case 1 -> {
                // esperar a que el numero de mallas LOD deje de crecer (o tope de tiempo)
                if (t > 0 && t % 200 == 0) {
                    int count = LodRenderer.devMeshCount();
                    Constants.LOG.info("[autotest] t={}s fps={} y={} {}", t / 20, mc.getFps(), (int) p.getY(), LodRenderer.devStats());
                    if (t == 1200) shot(mc, "0-cargando");
                    if (count > 0 && count == lastMeshCount) stableChecks++;
                    else stableChecks = 0;
                    lastMeshCount = count;
                    if ((stableChecks >= 3 && t >= 1200) || t >= MAX_WAIT_TICKS) {
                        Constants.LOG.info("[autotest] LOD listo tras {}s (estable={})", t / 20, stableChecks >= 3);
                        next(2);
                    }
                }
            }
            case 2 -> {
                // 4 direcciones mirando al horizonte
                float[] yaws = {0f, 90f, 180f, 270f};
                for (int i = 0; i < 4; i++) {
                    if (t == i * 60 + 1) { p.setYRot(yaws[i]); p.setXRot(8f); }
                    if (t == i * 60 + 50) shot(mc, "1-horizonte-yaw" + (int) yaws[i]);
                }
                if (t == 260) {
                    p.setYRot(0f); p.setXRot(8f);
                    next(3);
                }
            }
            case 3 -> {
                // FPS con LOD (10 s), tras 3 s de asentarse
                if (t == 60) { fpsSum = 0; fpsSamples = 0; nanosAtStart = LodRenderer.devRenderNanos; callsAtStart = LodRenderer.devRenderCalls; }
                if (t > 60 && t % 20 == 0) { fpsSum += mc.getFps(); fpsSamples++; }
                if (t == 260) {
                    fpsOn = fpsSamples == 0 ? 0 : (double) fpsSum / fpsSamples;
                    long calls = LodRenderer.devRenderCalls - callsAtStart;
                    double msPerFrame = calls == 0 ? 0 : (LodRenderer.devRenderNanos - nanosAtStart) / 1e6 / calls;
                    Constants.LOG.info("[autotest] FPS con LOD: media {} ({} muestras); LodRenderer.render = {} ms/frame en CPU ({} frames); {}",
                            String.format("%.1f", fpsOn), fpsSamples, String.format("%.2f", msPerFrame), calls, LodRenderer.devStats());
                    LodRenderer.devDisabled = true;
                    next(4);
                }
            }
            case 4 -> {
                if (t == 50) shot(mc, "2-sin-lod-yaw0");
                if (t == 60) { fpsSum = 0; fpsSamples = 0; }
                if (t > 60 && t % 20 == 0) { fpsSum += mc.getFps(); fpsSamples++; }
                if (t == 260) {
                    double fpsOff = fpsSamples == 0 ? 0 : (double) fpsSum / fpsSamples;
                    Constants.LOG.info("[autotest] FPS sin LOD: media {} ({} muestras); con LOD {} -> diferencia {}%",
                            String.format("%.1f", fpsOff), fpsSamples, String.format("%.1f", fpsOn),
                            fpsOff <= 0 ? "?" : String.format("%.0f", (fpsOn - fpsOff) / fpsOff * 100.0));
                    LodRenderer.devDisabled = false;
                    next(5);
                }
            }
            case 5 -> {
                // bajar cerca del suelo mirando lejos (vista normal de juego)
                if (t == 1) {
                    int ground = mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) p.getX(), (int) p.getZ());
                    p.connection.sendCommand("tp " + name + " " + (int) p.getX() + " " + Math.max(ground + 12, 80) + " " + (int) p.getZ() + " 90 2");
                }
                if (t == 160) shot(mc, "3-cerca-del-suelo");
                if (t == 200) {
                    Constants.LOG.info("[autotest] fin; {}", LodRenderer.devStats());
                    next(6);
                }
            }
            case 6 -> {
                if (t == 20) mc.stop();
            }
            default -> {
            }
        }
    }
}
