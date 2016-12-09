/*
 * Copyright 2012-2016 Tobi29
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

package org.tobi29.scapes.server.format.sqlite

import java8.util.stream.Stream
import org.tobi29.scapes.client.SaveStorage
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.engine.utils.stream
import org.tobi29.scapes.server.format.WorldSource
import java.io.IOException

class SQLiteSaveStorage(private val path: FilePath) : SaveStorage {

    @Throws(IOException::class)
    override fun list(): Stream<String> {
        if (!exists(path)) {
            return stream()
        }
        return list(path, ::isDirectory,
                ::isNotHidden).stream().map { it.fileName }.map { it.toString() }
    }

    @Throws(IOException::class)
    override fun exists(name: String): Boolean {
        return exists(path.resolve(name))
    }

    @Throws(IOException::class)
    override fun get(name: String): WorldSource {
        return SQLiteWorldSource(path.resolve(name))
    }

    @Throws(IOException::class)
    override fun delete(name: String): Boolean {
        deleteDir(path.resolve(name))
        return true
    }
}
