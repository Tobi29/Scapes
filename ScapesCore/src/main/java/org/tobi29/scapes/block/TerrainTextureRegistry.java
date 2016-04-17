/*
 * Copyright 2012-2015 Tobi29
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUTerrainTexture WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tobi29.scapes.block;

import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.graphics.Image;

public class TerrainTextureRegistry extends TextureAtlas<TerrainTexture> {
    public TerrainTextureRegistry(ScapesEngine engine) {
        super(engine, 4);
    }

    public TerrainTexture registerTexture(String... paths) {
        return registerTexture(paths, false, ShaderAnimation.NONE);
    }

    public TerrainTexture registerTexture(String path, boolean animated) {
        return registerTexture(path, animated, ShaderAnimation.NONE);
    }

    public TerrainTexture registerTexture(String[] paths, boolean animated) {
        return registerTexture(paths, animated, ShaderAnimation.NONE);
    }

    public TerrainTexture registerTexture(String path,
            ShaderAnimation shaderAnimation) {
        return registerTexture(path, false, shaderAnimation);
    }

    public TerrainTexture registerTexture(String[] paths,
            ShaderAnimation shaderAnimation) {
        return registerTexture(paths, false, shaderAnimation);
    }

    public TerrainTexture registerTexture(String path) {
        return registerTexture(path, false, ShaderAnimation.NONE);
    }

    public TerrainTexture registerTexture(String path, boolean animated,
            ShaderAnimation shaderAnimation) {
        return registerTexture(new String[]{path}, animated, shaderAnimation);
    }

    public TerrainTexture registerTexture(String[] paths, boolean animated,
            ShaderAnimation shaderAnimation) {
        String path = path(paths);
        TerrainTexture texture = textures.get(path);
        if (texture != null) {
            return texture;
        }
        Image image = load(paths);
        if (animated) {
            texture = new AnimatedTerrainTexture(image.buffer(), image.width(),
                    image.height(), shaderAnimation, this);
        } else {
            texture = new TerrainTexture(image.buffer(), image.width(),
                    shaderAnimation, this);
        }
        textures.put(path, texture);
        return texture;
    }

    public void render(GL gl) {
        for (TerrainTexture texture : textures.values()) {
            texture.renderAnim(gl);
        }
    }

    public void update(double delta) {
        Streams.of(textures.values())
                .forEach(texture -> texture.updateAnim(delta));
    }
}
