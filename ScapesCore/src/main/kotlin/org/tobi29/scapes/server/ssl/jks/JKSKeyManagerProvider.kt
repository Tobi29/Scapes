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

package org.tobi29.scapes.server.ssl.jks

import org.tobi29.scapes.engine.server.keyManagers
import org.tobi29.scapes.engine.server.keyStore
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.read
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.server.ssl.spi.KeyManagerProvider
import java.security.KeyStoreException
import javax.net.ssl.KeyManager

class JKSKeyManagerProvider : KeyManagerProvider {
    override fun available(): Boolean {
        return true
    }

    override fun configID(): String {
        return "JKS"
    }

    override fun get(path: FilePath,
                     configMap: TagMap): Array<KeyManager> {
        try {
            val keyStore = read(
                    path.resolve(configMap["KeyStore"].toString())) {
                keyStore(it, configMap["StorePassword"].toString())
            }
            return keyManagers(keyStore,
                    configMap["KeyPassword"].toString())
        } catch (e: KeyStoreException) {
            throw IOException(e)
        }
    }
}
