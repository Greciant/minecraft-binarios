package com.devilfruits.client.vfx.render;

import com.devilfruits.client.vfx.sprite.VfxSprites;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

public enum MeshTex {
   ATLAS(VfxSprites.ATLAS),
   NOISE(VfxSprites.NOISE),
   FLAME_RIBBON(VfxSprites.FLAME_RIBBON),
   LANCE(VfxSprites.LANCE),
   CRACKLE(VfxSprites.CRACKLE),
   SAND_STREAM(VfxSprites.SAND_STREAM),
   SAND_SHEET(VfxSprites.SAND_SHEET, true),
   SAND_VEIL(VfxSprites.SAND_SHEET),
   WAX_STREAM(VfxSprites.WAX_STREAM, true),
   CUT_RIBBON(VfxSprites.CUT_RIBBON),
   SOUL_TENDRIL(VfxSprites.YOMI_TENDRIL),
   HORO_MIST(VfxSprites.HORO_MIST),
   BLOCKS(InventoryMenu.BLOCK_ATLAS);

   public final ResourceLocation texture;
   public final boolean solid;

   private MeshTex(ResourceLocation texture) {
      this(texture, false);
   }

   private MeshTex(ResourceLocation texture, boolean solid) {
      this.texture = texture;
      this.solid = solid;
   }
}
