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

package org.tobi29.scapes.engine.opengl.shader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.utils.io.filesystem.FileSystemContainer;
import org.tobi29.scapes.engine.utils.io.filesystem.Resource;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class ShaderManager {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ShaderManager.class);
    private final ScapesEngine engine;
    private final Map<String, Shader> cache = new ConcurrentHashMap<>();
    private final Map<String, ShaderCompileInformation> compileInformation =
            new ConcurrentHashMap<>();

    public ShaderManager(ScapesEngine engine) {
        this.engine = engine;
    }

    public Shader getShader(String asset, GraphicsSystem graphics) {
        if (!cache.containsKey(asset)) {
            loadFromAsset(asset, graphics);
        }
        return cache.get(asset);
    }

    private void loadFromAsset(String asset, GraphicsSystem graphics) {
        try {
            Properties properties = new Properties();
            FileSystemContainer files = engine.getFiles();
            Resource vertexResource = files.getResource(asset + ".vsh");
            Resource fragmentResource = files.getResource(asset + ".fsh");
            Resource propertiesResource =
                    files.getResource(asset + ".properties");
            if (propertiesResource.exists()) {
                properties.load(propertiesResource.read());
            }
            Shader shader =
                    new Shader(vertexResource, fragmentResource, properties,
                            getCompileInformation(asset), graphics);
            cache.put(asset, shader);
        } catch (IOException e) {
            engine.crash(e);
        }
    }

    public ShaderCompileInformation getCompileInformation(String asset) {
        ShaderCompileInformation information = compileInformation.get(asset);
        if (information == null) {
            information = new ShaderCompileInformation();
            compileInformation.put(asset, information);
        }
        return information;
    }

    public void clearCache(GraphicsSystem graphics) {
        for (Shader shader : cache.values()) {
            shader.dispose(graphics);
        }
        cache.clear();
    }
}
