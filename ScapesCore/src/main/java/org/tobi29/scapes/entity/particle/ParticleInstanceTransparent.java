package org.tobi29.scapes.entity.particle;

import org.tobi29.scapes.engine.utils.math.vector.MutableVector2;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector2f;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;

public class ParticleInstanceTransparent extends ParticleInstance {
    public final MutableVector2 textureOffset = new MutableVector2f(),
            textureSize = new MutableVector2f();
    public float sizeStart, sizeEnd;
    public float gravitationMultiplier, airFriction, groundFriction,
            waterFriction;
    public float dir, rStart, gStart, bStart, aStart, rEnd, gEnd, bEnd, aEnd;
    public boolean physics;
    protected float timeMax;
    protected Vector3 posRender = Vector3d.ZERO;

    public void disablePhysics() {
        physics = false;
    }

    public void setPhysics() {
        setPhysics(1.0f);
    }

    public void setPhysics(float gravitationMultiplier) {
        setPhysics(gravitationMultiplier, 0.2f);
    }

    public void setPhysics(float gravitationMultiplier, float airFriction) {
        setPhysics(gravitationMultiplier, airFriction, 0.4f, 8.0f);
    }

    public void setPhysics(float gravitationMultiplier, float airFriction,
            float groundFriction, float waterFriction) {
        physics = true;
        this.gravitationMultiplier = gravitationMultiplier;
        this.airFriction = airFriction;
        this.groundFriction = groundFriction;
        this.waterFriction = waterFriction;
    }

    public void setTexture(ParticleTransparentTexture texture) {
        textureOffset.set(texture.x(), texture.y());
        textureSize.set(texture.size(), texture.size());
    }
}
