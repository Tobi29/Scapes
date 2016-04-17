package org.tobi29.scapes.vanilla.basics.entity.particle;

import org.tobi29.scapes.entity.particle.ParticleTransparentAtlas;
import org.tobi29.scapes.entity.particle.ParticleTransparentTexture;

public class VanillaParticle {
    public final ParticleTransparentTexture cloud;
    public final ParticleTransparentTexture explosion;
    public final ParticleTransparentTexture smoke;

    public VanillaParticle(ParticleTransparentAtlas pa) {
        cloud = pa.registerTexture(
                "VanillaBasics:image/entity/particle/Cloud.png");
        explosion = pa.registerTexture(
                "VanillaBasics:image/entity/particle/Explosion.png");
        smoke = pa.registerTexture(
                "VanillaBasics:image/entity/particle/Smoke.png");
    }
}
