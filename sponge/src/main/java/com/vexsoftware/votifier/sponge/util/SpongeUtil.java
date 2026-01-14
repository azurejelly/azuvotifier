package com.vexsoftware.votifier.sponge.util;

import lombok.experimental.UtilityClass;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;

@UtilityClass
public class SpongeUtil {

    public static String getMinecraftVersion() {
        return Sponge.platform().minecraftVersion().name();
    }

    public static String getPluginVersion(String id) {
        return Sponge.pluginManager()
                .plugin(id)
                .map((container) -> container.metadata().version().toString())
                .orElse("<unknown>");
    }

    public static String getPlatformComponentName(Platform.Component component) {
        return Sponge.platform()
                .container(component)
                .metadata()
                .name()
                .orElse("<unknown>");
    }

    public static String getPlatformComponentVersion(Platform.Component component) {
        return Sponge.platform()
                .container(component)
                .metadata()
                .version()
                .toString();
    }
}
