package de.miraculixx.showmyworld.mixin;

import de.miraculixx.showmyworld.ShowMyWorld;
import de.miraculixx.showmyworld.WorldPanoramaManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin {

    /**
     * En 1.20.1 SelectWorldScreen no llama a renderBackground: la lista de mundos pinta su
     * propio fondo de tierra (cuerpo y franjas de arriba/abajo) y taparia el panorama. Si el
     * mundo resaltado tiene panorama, se dibuja aqui antes que la lista y se apaga su fondo.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void showmyworld$renderPanorama(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        WorldSelectionList list = ((SelectWorldScreenAccessor) this).showmyworld$getList();
        if (list == null) return;

        PanoramaRenderer renderer = null;
        if (ShowMyWorld.config == null || ShowMyWorld.config.show) {
            String levelId = list.getSelectedOpt()
                    .map(entry -> ((WorldListEntryAccessor) (Object) entry).showmyworld$getSummary().getLevelId())
                    .orElse(null);
            if (levelId != null) renderer = WorldPanoramaManager.get(levelId);
        }

        boolean panorama = renderer != null;
        list.setRenderBackground(!panorama);
        list.setRenderTopAndBottom(!panorama);
        if (panorama) {
            renderer.render(Minecraft.getInstance().getFrameTime(), 1.0f);
        }
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void showmyworld$onRemoved(CallbackInfo ci) {
        WorldPanoramaManager.releaseAll();
    }
}
