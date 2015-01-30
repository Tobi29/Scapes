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

package org.tobi29.scapes.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.utils.io.filesystem.Directory;

import java.io.FilePermission;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.List;

public class PluginClassLoader extends URLClassLoader {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PluginClassLoader.class);
    private final Permissions permissions = new Permissions();

    public PluginClassLoader(List<PluginFile> files, Directory directory)
            throws IOException {
        super(getURLs(files));
        if (System.getSecurityManager() == null) {
            LOGGER.warn("No security manager installed!");
        }
        if (directory != null) {
            permissions.add(new FilePermission(
                    directory.getFile().getAbsolutePath() + "/-",
                    "read,write,delete"));
        }
        permissions.add(new RuntimePermission("getClassLoader"));
        permissions.setReadOnly();
    }

    private static URL[] getURLs(List<PluginFile> files) throws IOException {
        URL[] urls = new URL[files.size()];
        int i = 0;
        for (PluginFile file : files) {
            urls[i] = file.getFile().getURL();
            i++;
        }
        return urls;
    }

    @Override
    protected PermissionCollection getPermissions(CodeSource codesource) {
        return permissions;
    }
}
