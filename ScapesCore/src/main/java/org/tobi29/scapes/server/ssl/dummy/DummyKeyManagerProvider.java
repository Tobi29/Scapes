package org.tobi29.scapes.server.ssl.dummy;

import org.tobi29.scapes.engine.utils.SSLUtil;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.server.ssl.spi.KeyManagerProvider;

import javax.net.ssl.KeyManager;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

public class DummyKeyManagerProvider implements KeyManagerProvider {
    public static KeyManager[] get() throws IOException {
        try {
            KeyStore keyStore = SSLUtil.keyStore("default.jks", "storepass",
                    DummyKeyManagerProvider.class.getClassLoader());
            return SSLUtil.keyManagers(keyStore, "keypass");
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String configID() {
        return "Dummy";
    }

    @Override
    public KeyManager[] get(FilePath path, TagStructure config)
            throws IOException {
        return get();
    }
}
