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
