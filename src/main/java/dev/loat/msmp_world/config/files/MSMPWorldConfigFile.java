package dev.loat.msmp_world.config.files;

import dev.loat.config_lib.annotation.Annotation;


@Annotation.Comment("""
    Main configuration file for MSMP World.
""")
public class MSMPWorldConfigFile {
    private MSMPWorldConfigFile() {}

    @Annotation.Comment("Example foo")
    public String foo = "foo";
}
