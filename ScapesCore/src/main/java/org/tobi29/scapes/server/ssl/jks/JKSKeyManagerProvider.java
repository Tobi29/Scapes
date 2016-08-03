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

package org.tobi29.scapes.server.ssl.jks;

import org.tobi29.scapes.engine.utils.SSLUtil;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.server.ssl.spi.KeyManagerProvider;

import javax.net.ssl.KeyManager;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

public class JKSKeyManagerProvider implements KeyManagerProvider {
    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String configID() {
        return "JKS";
    }

    @Override
    public KeyManager[] get(FilePath path, TagStructure config)
            throws IOException {
        try {
            KeyStore keyStore = FileUtil.readReturn(
                    path.resolve(config.getString("KeyStore")),
                    stream -> SSLUtil.keyStore(stream,
                            config.getString("StorePassword")));
            return SSLUtil
                    .keyManagers(keyStore, config.getString("KeyPassword"));
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new IOException(e);
        }
    }
}
