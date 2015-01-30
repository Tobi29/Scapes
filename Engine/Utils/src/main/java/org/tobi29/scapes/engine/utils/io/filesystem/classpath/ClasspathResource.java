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

import org.tobi29.scapes.engine.utils.io.filesystem.Resource;
import org.tobi29.scapes.engine.utils.io.filesystem.ResourceAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class ClasspathResource implements Resource {
    final ClassLoader classLoader;
    final String path, id;

    public ClasspathResource(ClassLoader classLoader, String path, String id) {
        this.classLoader = classLoader;
        this.path = path;
        this.id = id;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public boolean exists() {
        return getURL() != null;
    }

    @Override
    public ResourceAttributes getAttributes() {
        return new ClasspathAttributes(this);
    }

    @Override
    public URL getURL() {
        return AccessController.doPrivileged(
                (PrivilegedAction<URL>) () -> classLoader.getResource(path));
    }

    @Override
    public InputStream read() throws IOException {
        return AccessController.doPrivileged(
                (PrivilegedAction<InputStream>) () -> classLoader
                        .getResourceAsStream(path));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Resource && ((Resource) obj).getID().equals(id);
    }
}
