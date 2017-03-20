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

package org.tobi29.scapes.server.format.sqljet

import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.task.TaskExecutor
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.scapes.server.format.spi.WorldSourceProvider
import java.io.IOException

class SQLJetWorldSourceProvider : WorldSourceProvider {
    override fun available(): Boolean {
        return true
    }

    override fun configID(): String {
        return "SQLJet"
    }

    @Throws(IOException::class)
    override fun get(path: FilePath,
                     configMap: TagMap,
                     taskExecutor: TaskExecutor): WorldSource {
        return SQLJetWorldSource(path)
    }
}