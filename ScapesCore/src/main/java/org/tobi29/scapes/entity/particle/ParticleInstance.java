package org.tobi29.scapes.entity.particle;

import org.tobi29.scapes.engine.utils.math.vector.MutableVector3;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3f;

public class ParticleInstance {
    public final MutableVector3 pos = new MutableVector3d(), speed =
            new MutableVector3f();
    public float time;
    public State state = State.DEAD;

    public enum State {
        DEAD,
        NEW,
        ALIVE
    }
}
