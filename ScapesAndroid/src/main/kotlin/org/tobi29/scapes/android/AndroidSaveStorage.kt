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

package org.tobi29.scapes.android

import android.content.Context
import org.tobi29.scapes.client.SaveStorage
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.deleteDir
import org.tobi29.scapes.engine.utils.io.filesystem.isDirectory
import org.tobi29.scapes.engine.utils.io.filesystem.isNotHidden
import org.tobi29.scapes.server.format.WorldSource


class AndroidSaveStorage(private val context: Context,
                         private val path: FilePath) : SaveStorage {
    override fun list(): Sequence<String> {
        if (!org.tobi29.scapes.engine.utils.io.filesystem.exists(path)) {
            return emptySequence()
        }
        return org.tobi29.scapes.engine.utils.io.filesystem.list(path, {
            isDirectory(path) && isNotHidden(path)
        }).asSequence().map { it.fileName.toString() }
    }

    override fun exists(name: String): Boolean {
        return org.tobi29.scapes.engine.utils.io.filesystem.exists(
                path.resolve(name))
    }

    override fun get(name: String): WorldSource {
        return AndroidWorldSource(context, path.resolve(name))
    }

    override fun delete(name: String): Boolean {
        deleteDir(path.resolve(name))
        return true
    }
}