package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.hydro.HydroPalette;

final class HydroLook {
   private HydroLook() {
   }

   static WaterMesh.Paint paint(boolean wet, float fade, int light) {
      return WaterMesh.Paint.of(HydroPalette.rgb(wet), 0.8F * HydroPalette.opacity(wet) * fade, light);
   }

   static WaterMesh.Paint linePaint(boolean wet, int light) {
      return WaterMesh.Paint.of(HydroPalette.rgb(wet), 0.8F * HydroPalette.lineOpacity(wet), light);
   }

   static WaterWave.Look body(boolean wet) {
      return new WaterWave.Look(HydroPalette.bodyRgb(wet), 0.8F * HydroPalette.bodyOpacity(wet), HydroPalette.bodyDeepRgb(wet), HydroPalette.bodyCrestRgb(wet));
   }

   static WaterWave.Look body(VfxRenderState st) {
      boolean wet = wet(st);
      return new WaterWave.Look(st.color, 0.8F * st.intensity, HydroPalette.bodyDeepRgb(wet), HydroPalette.bodyCrestRgb(wet));
   }

   static boolean wet(VfxRenderState st) {
      return (st.color & 16777215) == (HydroPalette.bodyRgb(true) & 16777215);
   }
}
