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
package org.tobi29.scapes.engine.server.android

import org.tobi29.scapes.engine.server.RemoteAddress
import org.tobi29.scapes.engine.server.SSLHandle
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.stream
import org.tobi29.scapes.engine.utils.toTypedArray
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*

class AndroidSSLHandle(keyManagers: Array<KeyManager>?,
                       feedbackPredicate: ((Array<X509Certificate>) -> Boolean)?) : SSLHandle {
    private val context: SSLContext
    private val feedbackPredicate: Function1<Array<X509Certificate>, Boolean>

    init {
        try {
            context = SSLContext.getInstance("TLSv1.2")
            if (feedbackPredicate != null) {
                context.init(keyManagers, DUMMY_TRUST_MANAGERS,
                        SecureRandom())
                this.feedbackPredicate = feedbackPredicate
            } else {
                context.init(keyManagers, null, SecureRandom())
                this.feedbackPredicate = { certificates -> false }
            }
        } catch (e: NoSuchAlgorithmException) {
            throw IOException(e)
        } catch (e: KeyManagementException) {
            throw IOException(e)
        }

    }

    override fun newEngine(address: RemoteAddress): SSLEngine {
        return context.createSSLEngine(address.address, address.port)
    }

    override fun certificateFeedback(certificates: Array<X509Certificate>): Boolean {
        return feedbackPredicate.invoke(certificates)
    }

    override fun requiresVerification(): Boolean {
        return true
    }

    override fun verifySession(address: RemoteAddress,
                               engine: SSLEngine,
                               certificates: Array<X509Certificate>) {
        val session = engine.session
        try {
            val trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(null as KeyStore?)
            val trustManagers = stream(
                    *trustManagerFactory.trustManagers).filterMap<X509TrustManager>().toTypedArray()
            for (trustManager in trustManagers) {
                val authType = StringBuilder(12)
                var first = true
                for (str in session.cipherSuite.split(
                        "_".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()) {
                    if ("WITH" == str) {
                        break
                    } else if ("TLS" != str) {
                        if (first) {
                            first = false
                        } else {
                            authType.append('_')
                        }
                        authType.append(str)
                    }
                }
                if (engine.useClientMode) {
                    trustManager.checkServerTrusted(certificates,
                            authType.toString())
                } else {
                    trustManager.checkClientTrusted(certificates,
                            authType.toString())
                }
            }
        } catch (e: NoSuchAlgorithmException) {
            throw IOException(e)
        } catch (e: KeyStoreException) {
            throw IOException(e)
        } catch (e: CertificateException) {
            throw IOException(e)
        }

        val verifier = HttpsURLConnection.getDefaultHostnameVerifier()
        if (!verifier.verify(address.address, session)) {
            throw SSLException("Hostname verification failed")
        }
    }

    companion object {
        private val EMPTY_CERTIFICATE = arrayOf<X509Certificate>()
        private val DUMMY_TRUST_MANAGERS = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(
                            x509Certificates: Array<X509Certificate>,
                            s: String) {
                    }

                    override fun checkServerTrusted(
                            x509Certificates: Array<X509Certificate>,
                            s: String) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return EMPTY_CERTIFICATE
                    }
                })
    }
}
