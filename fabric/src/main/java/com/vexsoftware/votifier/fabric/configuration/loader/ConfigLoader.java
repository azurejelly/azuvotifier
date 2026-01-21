package com.vexsoftware.votifier.fabric.configuration.loader;

import com.vexsoftware.votifier.fabric.configuration.FabricConfig;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.io.File;
import java.io.IOException;

// FIXME: temporary?
public class ConfigLoader {

    private static FabricConfig cfg;

    public static FabricConfig loadFrom(File configDir) throws IOException {
        if (!configDir.exists()) {
            if (!configDir.mkdirs()) {
                throw new RuntimeException("Unable to create the mod data folder at " + configDir);
            }
        }

        File config = new File(configDir, "settings.conf");
        if (!config.exists() && !config.createNewFile()) {
            throw new IOException("Unable to create the config file at " + config);
        }

        HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .file(config)
                .prettyPrinting(true)
                .emitComments(true)
                .build();

        ConfigurationNode node = loader.load(ConfigurationOptions.defaults().shouldCopyDefaults(true));
        cfg = node.get(TypeToken.get(FabricConfig.class), new FabricConfig());
        loader.save(node);

        return cfg;
    }

    public static FabricConfig get() {
        return cfg;
    }
}
