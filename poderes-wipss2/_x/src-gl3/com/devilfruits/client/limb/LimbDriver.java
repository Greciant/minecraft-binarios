package com.devilfruits.client.limb;

import com.devilfruits.core.limb.LimbPath;

@FunctionalInterface
public interface LimbDriver {
   boolean path(LimbFrame var1, LimbPath var2);
}
