package org.tobi29.scapes.entity.particle;

import org.tobi29.scapes.block.TextureAtlas;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.utils.graphics.Image;

public class ParticleTransparentAtlas
        extends TextureAtlas<ParticleTransparentTexture> {
    public ParticleTransparentAtlas(ScapesEngine engine) {
        super(engine, 4);
    }

    public ParticleTransparentTexture registerTexture(String path) {
        ParticleTransparentTexture texture = textures.get(path);
        if (texture != null) {
            return texture;
        }
        Image image = load(path);
        texture = new ParticleTransparentTexture(image.buffer(), image.width(),
                this);
        textures.put(path, texture);
        return texture;
    }
}
