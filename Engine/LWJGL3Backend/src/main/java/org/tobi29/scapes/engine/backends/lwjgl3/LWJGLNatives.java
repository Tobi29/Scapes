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

package org.tobi29.scapes.engine.backends.lwjgl3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.ScapesEngineException;
import org.tobi29.scapes.engine.utils.io.ProcessStream;
import org.tobi29.scapes.engine.utils.io.filesystem.*;
import org.tobi29.scapes.engine.utils.platform.Platform;

import java.io.IOException;

public final class LWJGLNatives {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(LWJGLNatives.class);

    public static String extract(FileSystemContainer files) {
        try {
            Directory directory = files.getDirectory("Temp:natives");
            directory.make();
            Platform platform = Platform.getPlatform();
            Path path = files.get("Class:native");
            switch (platform.getID()) {
                case "Linux": {
                    if (platform.is64Bit()) {
                        extract("liblwjgl.so", path, directory);
                        extract("libopenal.so", path, directory);
                    } else {
                        extract("liblwjgl32.so", path, directory);
                        extract("libopenal32.so", path, directory);
                    }
                    break;
                }
                case "MacOSX": {
                    extract("liblwjgl.dylib", path, directory);
                    extract("libopenal.dylib", path, directory);
                    break;
                }
                case "Windows":
                    if (platform.is64Bit()) {
                        extract("lwjgl.dll", path, directory);
                        extract("OpenAL.dll", path, directory);
                    } else {
                        extract("lwjgl32.dll", path, directory);
                        extract("OpenAL32.dll", path, directory);
                    }
                    break;
                default:
                    throw new ScapesEngineException(
                            "Unsupported platform:" + platform.getID());
            }
            return directory.getFile().getAbsolutePath();
        } catch (IOException e) {
            LOGGER.error("Failed to create temporary directory:", e);
        }
        return System.getProperty("user.dir");
    }

    private static void extract(String name, Path path, Directory directory)
            throws IOException {
        Resource resource = path.getResource(name);
        if (resource.exists()) {
            File file = directory.getResource(name);
            ProcessStream.processSourceAndDestination(resource, file);
        } else {
            LOGGER.warn("Native library not found in classpath: {}", name);
        }
    }
}
