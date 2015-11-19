package org.tobi29.scapes.client.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.chunk.IDStorage;
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.server.Account;
import org.tobi29.scapes.engine.server.ConnectionCloseException;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.MutableSingle;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.graphics.PNG;
import org.tobi29.scapes.engine.utils.io.*;
import org.tobi29.scapes.engine.utils.io.filesystem.FileCache;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.plugins.Plugins;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class NewConnection {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(NewConnection.class);
    private static final int AES_MIN_KEY_LENGTH, AES_MAX_KEY_LENGTH;

    static {
        int length = 16;
        try {
            length = Cipher.getMaxAllowedKeyLength("AES") >> 3;
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("Failed to detect maximum key length", e);
        }
        AES_MAX_KEY_LENGTH = length;
        AES_MIN_KEY_LENGTH = FastMath.min(16, length);
    }

    private final ScapesEngine engine;
    private final int loadingDistanceRequest;
    private final PacketBundleChannel channel;
    private final FileCache cache;
    private final List<Integer> pluginRequests = new ArrayList<>();
    private final List<PluginFile> plugins = new ArrayList<>();
    private final ByteBufferStream pluginStream = new ByteBufferStream();
    private final Account account;
    private int loadingDistance;
    private IDStorage idStorage;
    private IOFunction<ReadableByteStream, Optional<String>> state =
            this::loginStep1;
    private Optional<String> status = Optional.of("Logging in...");

    public NewConnection(ScapesEngine engine, SocketChannel channel,
            Account account, int loadingDistance) {
        this.engine = engine;
        this.loadingDistance = loadingDistance;
        loadingDistanceRequest = loadingDistance;
        this.channel = new PacketBundleChannel(channel);
        cache = engine.fileCache();
        this.account = account;
    }

    private Optional<String> loginStep1(ReadableByteStream input)
            throws IOException {
        byte[] array = new byte[input.getInt()];
        input.get(array);
        int keyLength = input.getInt();
        keyLength = FastMath.min(keyLength, AES_MAX_KEY_LENGTH);
        if (keyLength < AES_MIN_KEY_LENGTH) {
            throw new IOException("Key length too short: " + keyLength);
        }
        byte[] keyServer = new byte[keyLength];
        byte[] keyClient = new byte[keyLength];
        Random random = new SecureRandom();
        random.nextBytes(keyServer);
        random.nextBytes(keyClient);
        WritableByteStream output = channel.getOutputStream();
        output.putInt(keyLength);
        try {
            PublicKey rsaKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(array));
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, rsaKey);
            output.put(cipher.update(keyServer));
            output.put(cipher.doFinal(keyClient));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidKeySpecException e) {
            throw new IOException(e);
        }
        channel.queueBundle();
        channel.setKey(keyClient, keyServer);
        KeyPair keyPair = account.keyPair();
        array = keyPair.getPublic().getEncoded();
        output.put(array);
        channel.queueBundle();
        state = this::loginStep2;
        return Optional.of("Logging in...");
    }

    private Optional<String> loginStep2(ReadableByteStream input)
            throws IOException {
        byte[] challenge = new byte[512];
        input.get(challenge);
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, account.keyPair().getPrivate());
            challenge = cipher.doFinal(challenge);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new IOException(e);
        }
        int length = input.getInt();
        for (int i = 0; i < length; i++) {
            byte[] checksum = new byte[input.getInt()];
            input.get(checksum);
            FileCache.Location location =
                    new FileCache.Location("plugins", checksum);
            Optional<Path> file = cache.retrieve(location);
            if (file.isPresent()) {
                plugins.add(new PluginFile(file.get()));
            } else {
                pluginRequests.add(i);
                plugins.add(null);
            }
        }
        WritableByteStream output = channel.getOutputStream();
        output.put(challenge);
        output.putString(account.nickname());
        output.putInt(pluginRequests.size());
        for (int i : pluginRequests) {
            output.putInt(i);
        }
        channel.queueBundle();
        state = this::loginStep3;
        return Optional.of("Logging in...");
    }

    private Optional<String> loginStep3(ReadableByteStream input)
            throws IOException {
        if (input.getBoolean()) {
            throw new ConnectionCloseException(input.getString());
        }
        if (pluginRequests.isEmpty()) {
            return loginStep5();
        }
        state = this::loginStep4;
        return Optional.of("Downloading plugins...");
    }

    private Optional<String> loginStep4(ReadableByteStream input)
            throws IOException {
        int request = pluginRequests.get(0);
        if (input.getBoolean()) {
            pluginStream.buffer().flip();
            Optional<Path> file =
                    cache.retrieve(cache.store(pluginStream, "plugins"));
            pluginStream.buffer().clear();
            if (!file.isPresent()) {
                throw new IllegalStateException(
                        "Concurrent cache modification");
            }
            plugins.set(request, new PluginFile(file.get()));
            pluginRequests.remove(0);
            if (pluginRequests.isEmpty()) {
                return loginStep5();
            }
        } else {
            ProcessStream.process(input, pluginStream::put);
        }
        return Optional.of("Downloading plugins...");
    }

    private Optional<String> loginStep5() throws IOException {
        WritableByteStream output = channel.getOutputStream();
        output.putInt(loadingDistanceRequest);
        sendSkin(output);
        channel.queueBundle();
        state = this::loginStep6;
        return Optional.of("Receiving server info...");
    }

    private Optional<String> loginStep6(ReadableByteStream input)
            throws IOException {
        if (input.getBoolean()) {
            throw new ConnectionCloseException(input.getString());
        }
        loadingDistance = input.getInt();
        TagStructure idsTag = new TagStructure();
        TagStructureBinary.read(idsTag, input);
        idStorage = new IDStorage(idsTag);
        return Optional.empty();
    }

    private void sendSkin(WritableByteStream output) throws IOException {
        MutableSingle<Image> image = new MutableSingle<>();
        Path path = engine.home().resolve("Skin.png");
        if (Files.exists(path)) {
            image.a = FileUtil.readReturn(path,
                    stream -> PNG.decode(stream, BufferCreator::bytes));
            if (image.a.width() != 64 || image.a.height() != 64) {
                throw new ConnectionCloseException("Invalid skin!");
            }
        } else {
            engine.files().get("Scapes:image/entity/mob/Player.png")
                    .read(stream -> {
                        image.a = PNG.decode(stream, BufferCreator::bytes);
                        if (image.a.width() != 64 || image.a.height() != 64) {
                            throw new ConnectionCloseException("Invalid skin!");
                        }
                    });
        }
        byte[] skin = new byte[64 * 64 * 4];
        image.a.buffer().get(skin);
        output.put(skin);
    }

    public Optional<String> login() throws IOException {
        Optional<ReadableByteStream> bundle = channel.fetch();
        if (bundle.isPresent()) {
            status = state.apply(bundle.get());
        }
        channel.process();
        return status;
    }

    public IOFunction<GameStateGameMP, ClientConnection> finish()
            throws IOException {
        Plugins plugins = new Plugins(this.plugins, idStorage);
        return game -> new ClientConnection(game, channel, plugins,
                loadingDistance);
    }
}
