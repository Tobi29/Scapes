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

package org.tobi29.scapes.server.ssl.pem

import org.tobi29.scapes.engine.server.keyManagers
import org.tobi29.scapes.engine.server.readCertificateChain
import org.tobi29.scapes.engine.server.readPrivateKeys
import org.tobi29.scapes.engine.utils.io.ByteStreamInputStream
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.read
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.server.ssl.spi.KeyManagerProvider
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.cert.CertificateException
import javax.net.ssl.KeyManager

class PEMKeyManagerProvider : KeyManagerProvider {

    override fun available(): Boolean {
        return true
    }

    override fun configID(): String {
        return "PEM"
    }

    override fun get(path: FilePath,
                     configMap: TagMap): Array<KeyManager> {
        try {
            val certificates = read(
                    path.resolve(configMap["Certificate"].toString())) {
                readCertificateChain(
                        BufferedReader(
                                InputStreamReader(ByteStreamInputStream(it))))
            }
            val keys = read(path.resolve(
                    configMap["PrivateKey"].toString())) {
                readPrivateKeys(
                        BufferedReader(
                                InputStreamReader(ByteStreamInputStream(it))))
            }
            val keyStore = KeyStore.getInstance("JKS")
            keyStore.load(null, EMPTY_CHAR)
            val certificateArray = certificates.toTypedArray()
            for (i in keys.indices) {
                keyStore.setKeyEntry("key" + i, keys[i], EMPTY_CHAR,
                        certificateArray)
            }
            return keyManagers(keyStore, "")
        } catch (e: KeyStoreException) {
            throw IOException(e)
        } catch (e: CertificateException) {
            throw IOException(e)
        }
    }

    companion object {
        private val EMPTY_CHAR = charArrayOf()
    }
}
