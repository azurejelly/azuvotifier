package com.vexsoftware.votifier.bukkit.sponge.configuration.loader;

import com.vexsoftware.votifier.bukkit.sponge.NuVotifierSponge;
import com.vexsoftware.votifier.bukkit.sponge.configuration.SpongeConfig;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;

public class ConfigLoader {

    private static SpongeConfig spongeConfig;

    public static void loadConfig(NuVotifierSponge plugin) {
        if (!plugin.getConfigDir().toFile().exists()) {
            if (!plugin.getConfigDir().toFile().mkdirs()) {
                throw new RuntimeException("Unable to create the plugin data folder " + plugin.getConfigDir());
            }
        }

        try {
            File config = new File(plugin.getConfigDir().toFile(), "config.yml");
            if (!config.exists() && !config.createNewFile()) {
                throw new IOException("Unable to create the config file at " + config);
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .file(config)
                    .nodeStyle(NodeStyle.BLOCK)
                    .build();

            ConfigurationNode node = loader.load(ConfigurationOptions.defaults().shouldCopyDefaults(true));
            spongeConfig = node.get(TypeToken.get(SpongeConfig.class), new SpongeConfig());
            loader.save(node);
        } catch (Exception e) {
            plugin.getLogger().error("Could not load config.", e);
        }
    }

    public static SpongeConfig getSpongeConfig() {
        return spongeConfig;
    }
}
