package dev.loat.msmp_world.config.files.path_find;

import dev.loat.config_lib.annotation.Annotation;


public class PathFindConfig {

    @Annotation.Comment("Settings for path finding.")
    @Annotation.Key("max-distance")
    public int maxDistance = 256;

    @Annotation.Comment("""
        Configure what type of bounding box the path finder should use.

        SMALL - Requires at least 1 block of horizontal clearance to path find
        NORMAL - Requires at least 2 blocks of horizontal clearance to path find
    """)
    @Annotation.Key("bounding-box-type")
    public BoundingBoyType boundingBoxType = BoundingBoyType.SMALL;

    public static enum BoundingBoyType {
        SMALL,
        NORMAL
    }

    @Annotation.Comment("""
        When debugging is enabled, entities will be spawned to visualize the found path.
    """)
    public boolean debug = false;
}
