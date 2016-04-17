package org.tobi29.scapes.entity.particle;

import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector2;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector2f;

public class ParticleInstanceBlock extends ParticleInstance {
    public final MutableVector2 textureOffset = new MutableVector2f(),
            textureSize = new MutableVector2f();
    public float dir;
    public byte r, g, b, a;

    public void setColor(float r, float g, float b, float a) {
        this.r = (byte) FastMath.round(r * 255.0f);
        this.g = (byte) FastMath.round(g * 255.0f);
        this.b = (byte) FastMath.round(b * 255.0f);
        this.a = (byte) FastMath.round(a * 255.0f);
    }
}
