/*
 * Copyright 2012-2017 Tobi29
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

package org.tobi29.scapes.plugins

import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.engine.utils.toArray
import java.io.FilePermission
import java.net.URL
import java.net.URLClassLoader
import java.security.CodeSource
import java.security.PermissionCollection
import java.security.Permissions
import java.util.*

class PluginClassLoader(
        path: List<FilePath>
) : URLClassLoader(urls(path)) {
    private val permissions = Permissions()

    init {
        if (System.getSecurityManager() == null) {
            logger.warn { "No security manager installed!" }
        }

        // Grant required permissions for plugins
        // We want to keep this as generic and simple as possible and use
        // privileged actions instead for e.g. reading save data

        // Allow using plugin jars for assets
        logger.debug { "Plugin permission: Get classloader" }
        permissions.add(RuntimePermission("getClassLoader"))

        // Allow kotlin reflection (Not actual reflection, just reading,
        // e.g. class names)
        logger.debug { "Plugin permission: Property read kotlin.test.is.pre.release" }
        permissions.add(
                PropertyPermission("kotlin.test.is.pre.release", "read"))

        // Allow reading assets from jars
        // TODO: We may want to make a whitelist to only allow useful jars
        (PluginClassLoader::class.java.classLoader as? URLClassLoader)?.let { rootClassLoader ->
            rootClassLoader.urLs.asSequence().mapNotNull { it.file }.map {
                logger.debug { "Plugin permission: File read ($it)" }
                FilePermission(it, "read")
            }.forEach { permissions.add(it) }
        }
        path.asSequence().map { it.toAbsolutePath() }.map {
            logger.debug { "Plugin permission: File read ($it)" }
            FilePermission(it.toString(), "read")
        }.forEach { permissions.add(it) }

        // Lock permissions from modifications
        permissions.setReadOnly()
    }

    override fun getPermissions(codesource: CodeSource): PermissionCollection {
        return permissions
    }

    companion object : KLogging()
}

private fun urls(paths: List<FilePath>): Array<URL> {
    return paths.asSequence().map { it.toUri().java.toURL() }.toArray()
}
