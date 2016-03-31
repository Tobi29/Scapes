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
