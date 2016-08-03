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

package org.tobi29.scapes.server.ssl.pem;

import org.tobi29.scapes.engine.utils.SSLUtil;
import org.tobi29.scapes.engine.utils.io.ByteStreamInputStream;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.server.ssl.spi.KeyManagerProvider;

import javax.net.ssl.KeyManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.util.List;

public class PEMKeyManagerProvider implements KeyManagerProvider {
    private static final char[] EMPTY_CHAR = {};

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String configID() {
        return "PEM";
    }

    @Override
    public KeyManager[] get(FilePath path, TagStructure config)
            throws IOException {
        try {
            List<Certificate> certificates = FileUtil.readReturn(
                    path.resolve(config.getString("Certificate")),
                    stream -> SSLUtil.readCertificateChain(new BufferedReader(
                            new InputStreamReader(
                                    new ByteStreamInputStream(stream)))));
            List<RSAPrivateKey> keys = FileUtil.readReturn(
                    path.resolve(config.getString("PrivateKey")),
                    stream -> SSLUtil.readPrivateKeys(new BufferedReader(
                            new InputStreamReader(
                                    new ByteStreamInputStream(stream)))));
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, EMPTY_CHAR);
            Certificate[] certificateArray =
                    certificates.toArray(new Certificate[certificates.size()]);
            for (int i = 0; i < keys.size(); i++) {
                keyStore.setKeyEntry("key" + i, keys.get(i), EMPTY_CHAR,
                        certificateArray);
            }
            return SSLUtil.keyManagers(keyStore, "");
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException e) {
            throw new IOException(e);
        }
    }
}
