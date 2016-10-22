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

package org.tobi29.scapes.connection

import mu.KLogging
import org.tobi29.scapes.engine.utils.UnsupportedJVMException
import org.tobi29.scapes.engine.utils.extractPublic
import org.tobi29.scapes.engine.utils.io.ByteStreamInputStream
import org.tobi29.scapes.engine.utils.io.ByteStreamOutputStream
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.exists
import org.tobi29.scapes.engine.utils.io.filesystem.read
import org.tobi29.scapes.engine.utils.io.filesystem.write
import org.tobi29.scapes.engine.utils.readPrivate
import org.tobi29.scapes.engine.utils.writePrivate
import java.io.IOException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.util.*

class Account(private val keyPair: KeyPair, private val nickname: String) {

    @Throws(IOException::class)
    fun write(path: FilePath) {
        val properties = Properties()
        properties.setProperty("Key", key(keyPair))
        properties.setProperty("Nickname", nickname)
        write(path) {
            properties.store(ByteStreamOutputStream(it), "")
        }
    }

    fun keyPair(): KeyPair {
        return keyPair
    }

    fun nickname(): String {
        return nickname
    }

    fun valid(): Boolean {
        return valid(nickname)
    }

    companion object : KLogging() {

        @Throws(IOException::class)
        operator fun get(path: FilePath): Account {
            val account = read(path) ?: throw IOException(
                    "No valid account in: " + path)
            return account
        }

        @Throws(IOException::class)
        fun read(path: FilePath): Account? {
            var key: String? = null
            var nickname = ""
            if (exists(path)) {
                val properties = Properties()
                read(path, {
                    properties.load(ByteStreamInputStream(it))
                })
                key = properties.getProperty("Key")
                nickname = properties.getProperty("Nickname", "")
            }
            val keyPair = key(key)
            if (keyPair != null) {
                return Account(keyPair, nickname)
            }
            return null
        }

        @Throws(IOException::class)
        fun generate(path: FilePath): Account {
            val account = Account(genKey(), "")
            account.write(path)
            return account
        }

        fun key(str: String?): KeyPair? {
            if (str == null) {
                return null
            }
            try {
                val privateKey = readPrivate(str)
                val publicKey = extractPublic(privateKey)
                return KeyPair(publicKey, privateKey)
            } catch (e: InvalidKeySpecException) {
                logger.warn { "Failed to parse key: $e" }
                return null
            } catch (e: IllegalArgumentException) {
                logger.warn { "Failed to parse key: $e" }
                return null
            }

        }

        fun genKey(): KeyPair {
            logger.info { "Generating key-pair..." }
            try {
                val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
                keyPairGenerator.initialize(4096)
                return keyPairGenerator.genKeyPair()
            } catch (e: NoSuchAlgorithmException) {
                throw UnsupportedJVMException(e)
            }

        }

        fun key(keyPair: KeyPair?): String {
            if (keyPair == null) {
                return ""
            }
            try {
                return writePrivate(keyPair.private)
            } catch (e: InvalidKeySpecException) {
                return ""
            }

        }

        fun valid(nickname: String): Boolean {
            return isNameValid(nickname) == null
        }

        fun isNameValid(nickname: String): String? {
            if (nickname.length < 6) {
                return "Name must be at least 6 characters long!"
            }
            if (nickname.length > 20) {
                return "Name may not be longer than 20 characters!"
            }
            if (nickname.contains(" ")) {
                return "Name may not contain spaces!"
            }
            for (i in 0..nickname.length - 1) {
                if (!Character.isLetterOrDigit(nickname[i])) {
                    return "Name may only contain letters and digits!"
                }
            }
            return null
        }
    }
}
