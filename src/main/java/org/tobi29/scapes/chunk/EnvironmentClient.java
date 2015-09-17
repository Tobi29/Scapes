package org.tobi29.scapes.chunk;

import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;

public interface EnvironmentClient {
    void load(TagStructure tagStructure);

    void tick(double delta);

    float sunLightReduction(double x, double y);

    Vector3 sunLightNormal(double x, double y);

    WorldSkybox createSkybox(WorldClient world);
}
