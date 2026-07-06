package dev.loat.msmp_world.config.files;

import dev.loat.config_lib.annotation.Annotation;
import dev.loat.msmp_world.config.files.path_find.PathFindConfig;


@Annotation.Comment("""
    Main configuration file for MSMP World.
""")
public class MSMPWorldConfigFile {
    private MSMPWorldConfigFile() {}

    @Annotation.Comment("Configuration for path finding settings.")
    public PathFindConfig pathFind = new PathFindConfig();
}
