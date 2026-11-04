package de.miraculixx.showmyworld.mixin;

import de.miraculixx.showmyworld.ShowMyWorld;
import de.miraculixx.showmyworld.WorldPanoramaManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Port reducido de ShowMyWorld: solo la pantalla de seleccion de mundo (SelectWorldScreen).
 * SelectWorldScreen no tiene su propio renderBackground (usa el de Screen), asi que el
 * mixin va en Screen y solo actua si la instancia es SelectWorldScreen.
 * Sin captura automatica ni cruce de fundido entre dos paneles: si el mundo resaltado tiene
 * panorama\ con sus 6 caras, se dibuja entero (alpha 1); si no, queda el fondo normal.
 */
@Mixin(Screen.class)
public abstract class ScreenPanoramaMixin {

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void showmyworld$renderPanorama(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (ShowMyWorld.config == null || !ShowMyWorld.config.show) return;
        Object self = this;
        if (!(self instanceof SelectWorldScreen)) return;

        WorldSelectionList list = ((SelectWorldScreenAccessor) self).showmyworld$getList();
        if (list == null) return;

        String levelId = list.getSelectedOpt()
                .map(entry -> ((WorldListEntryAccessor) (Object) entry).showmyworld$getSummary().getLevelId())
                .orElse(null);
        if (levelId == null) return;

        PanoramaRenderer renderer = WorldPanoramaManager.get(levelId);
        if (renderer == null) return;

        float partialTick = Minecraft.getInstance().getFrameTime();
        renderer.render(partialTick, 1.0f);
        ci.cancel();
    }
}
