package com.vexsoftware.votifier.fabric.util;

import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

@UtilityClass
public class FabricUtil {

    public static String getMinecraftVersion() {
        return SharedConstants.getGameVersion().name();
    }

    public static String getModVersion(String id) {
        return FabricLoader.getInstance()
                .getModContainer(id)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("<unknown>");
    }
}
