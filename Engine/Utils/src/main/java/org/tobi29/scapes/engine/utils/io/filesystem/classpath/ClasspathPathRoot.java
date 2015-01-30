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

package org.tobi29.scapes.engine.utils.io.filesystem.classpath;

import org.tobi29.scapes.engine.utils.io.filesystem.FileSystemContainer;
import org.tobi29.scapes.engine.utils.io.filesystem.PathRoot;

public class ClasspathPathRoot extends ClasspathPath implements PathRoot {
    public ClasspathPathRoot(ClassLoader classLoader, String id, String path) {
        super(classLoader, id, path);
    }

    public static FileSystemContainer.PathRootCreator make(
            ClassLoader classLoader) {
        return (id, path) -> new ClasspathPathRoot(classLoader, id, path);
    }
}
