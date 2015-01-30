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

import org.apache.tika.Tika;
import org.tobi29.scapes.engine.utils.io.filesystem.ResourceAttributes;

import java.io.IOException;
import java.net.URL;

public class ClasspathAttributes implements ResourceAttributes {
    final ClasspathResource resource;

    public ClasspathAttributes(ClasspathResource resource) {
        this.resource = resource;
    }

    @Override
    public String getMIMEType() throws IOException {
        return new Tika().detect(resource.read(), resource.path);
    }

    @Override
    public long getSize() throws IOException {
        URL url = resource.getURL();
        if (url == null) {
            return -1;
        }
        return url.openConnection().getContentLength();
    }
}
