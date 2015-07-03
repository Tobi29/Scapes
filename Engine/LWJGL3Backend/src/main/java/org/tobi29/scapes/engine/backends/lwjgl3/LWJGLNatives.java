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

import org.lwjgl.LWJGLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.ScapesEngineException;
import org.tobi29.scapes.engine.utils.io.FileUtil;
import org.tobi29.scapes.engine.utils.io.ProcessStream;
import org.tobi29.scapes.engine.utils.io.filesystem.FileSystemContainer;
import org.tobi29.scapes.engine.utils.io.filesystem.Resource;

import java.io.IOException;
import java.nio.file.Path;

public final class LWJGLNatives {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(LWJGLNatives.class);

    public static void extract(FileSystemContainer files, Path path)
            throws IOException {
        String classpath = "Class:";
        boolean is32Bit = !System.getProperty("os.arch").contains("64");
        switch (LWJGLUtil.getPlatform()) {
            case LINUX: {
                if (is32Bit) {
                    extract(files, "liblwjgl32.so", classpath, path);
                    extract(files, "libopenal32.so", classpath, path);
                } else {
                    extract(files, "liblwjgl.so", classpath, path);
                    extract(files, "libopenal.so", classpath, path);
                }
                break;
            }
            case MACOSX: {
                extract(files, "liblwjgl.dylib", classpath, path);
                extract(files, "libopenal.dylib", classpath, path);
                break;
            }
            case WINDOWS:
                if (is32Bit) {
                    extract(files, "lwjgl32.dll", classpath, path);
                    extract(files, "OpenAL32.dll", classpath, path);
                } else {
                    extract(files, "lwjgl.dll", classpath, path);
                    extract(files, "OpenAL.dll", classpath, path);
                }
                break;
            default:
                throw new ScapesEngineException(
                        "Unsupported platform:" + LWJGLUtil.getPlatform());
        }
    }

    private static void extract(FileSystemContainer files, String name,
            String classpath, Path path) throws IOException {
        Resource resource = files.get(classpath + name);
        if (resource.exists()) {
            Path file = path.resolve(name);
            FileUtil.write(file, stream -> ProcessStream
                    .processSource(resource, stream::put));
        } else {
            LOGGER.warn("Native library not found in classpath: {}", name);
        }
    }
}
