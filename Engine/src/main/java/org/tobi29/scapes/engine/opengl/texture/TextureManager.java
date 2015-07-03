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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tobi29.scapes.engine.opengl.texture;

import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.utils.BufferCreatorDirect;
import org.tobi29.scapes.engine.utils.io.filesystem.FileSystemContainer;
import org.tobi29.scapes.engine.utils.io.filesystem.Resource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class TextureManager {
    private final ScapesEngine engine;
    private final Map<String, TextureAsset> cache = new ConcurrentHashMap<>();
    private final Texture empty;

    public TextureManager(ScapesEngine engine) {
        this.engine = engine;
        ByteBuffer buffer = BufferCreatorDirect.byteBuffer(4);
        buffer.put((byte) -1);
        buffer.put((byte) -1);
        buffer.put((byte) -1);
        buffer.put((byte) -1);
        buffer.rewind();
        empty = new TextureCustom(1, 1, buffer, 0);
    }

    public void bind(String asset, GraphicsSystem graphics) {
        if (asset == null) {
            unbind(graphics);
        } else if (asset.isEmpty()) {
            unbind(graphics);
        } else {
            bind(getTexture(asset), graphics);
        }
    }

    public void bind(Texture texture, GraphicsSystem graphics) {
        if (texture == null) {
            unbind(graphics);
        } else {
            texture.bind(graphics);
        }
    }

    public Texture getTexture(String asset) {
        if (!cache.containsKey(asset)) {
            loadFromAsset(asset);
        }
        return cache.get(asset);
    }

    private void loadFromAsset(String asset) {
        try {
            Properties properties = new Properties();
            FileSystemContainer files = engine.getFiles();
            Resource imageResource = files.get(asset + ".png");
            Resource propertiesResource = files.get(asset + ".properties");
            if (propertiesResource.exists()) {
                properties.load(propertiesResource.readIO());
            }
            TextureAsset texture =
                    new TextureAsset(imageResource.readIO(), properties);
            cache.put(asset, texture);
        } catch (IOException e) {
            engine.crash(e);
        }
    }

    public void unbind(GraphicsSystem graphics) {
        empty.bind(graphics);
    }

    public void clearCache(GraphicsSystem graphics) {
        for (Texture texture : cache.values()) {
            texture.dispose(graphics);
        }
        cache.clear();
    }
}
