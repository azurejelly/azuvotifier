package com.vexsoftware.votifier.fabric.utils;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

public final class FabricUtils {

    private FabricUtils() {
        throw new UnsupportedOperationException();
    }

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
