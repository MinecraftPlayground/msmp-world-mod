package dev.loat.msmp_world.config;

import dev.loat.config_lib.ConfigManager;
import dev.loat.msmp_world.config.files.MSMPWorldConfigFile;


public class Config {
    
    private Config() {}
    
    private static final String ROOT_DIRECTORY = "msmp/world";
    private static final ConfigManager CONFIG_MANAGER = new ConfigManager(ROOT_DIRECTORY);

    public static void register() {
        
        CONFIG_MANAGER.add("config.yml", MSMPWorldConfigFile.class);
    }

    public static MSMPWorldConfigFile getConfig() {

        return CONFIG_MANAGER.get(MSMPWorldConfigFile.class);
    }
}
