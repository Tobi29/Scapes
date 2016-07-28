package org.tobi29.scapes.client.connection;

import java8.util.Optional;
import org.tobi29.scapes.chunk.IDStorage;
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.connection.ConnectionType;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.server.Account;
import org.tobi29.scapes.engine.server.ConnectionCloseException;
import org.tobi29.scapes.engine.server.PacketBundleChannel;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.VersionUtil;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.graphics.PNG;
import org.tobi29.scapes.engine.utils.io.*;
import org.tobi29.scapes.engine.utils.io.filesystem.FileCache;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.binary.TagStructureBinary;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.plugins.Plugins;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class NewConnection {
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
    private IOFunction<RandomReadableByteStream, Optional<String>> state =
            this::loginStep1;
    private Optional<String> status = Optional.of("Logging in...");

    public NewConnection(ScapesEngine engine, PacketBundleChannel channel,
            Account account, int loadingDistance) throws IOException {
        this.engine = engine;
        this.channel = channel;
        this.loadingDistance = loadingDistance;
        loadingDistanceRequest = loadingDistance;
        cache = engine.fileCache();
        this.account = account;
        loginStep0();
    }

    private Optional<String> loginStep0() throws IOException {
        WritableByteStream output = channel.getOutputStream();
        output.put(new byte[]{'S', 'c', 'a', 'p', 'e', 's',
                ConnectionType.PLAY.data()});
        channel.queueBundle();
        KeyPair keyPair = account.keyPair();
        byte[] array = keyPair.getPublic().getEncoded();
        output.put(array);
        channel.queueBundle();
        state = this::loginStep1;
        return Optional.of("Logging in...");
    }

    private Optional<String> loginStep1(RandomReadableByteStream input)
            throws IOException {
        byte[] challenge = new byte[512];
        input.get(challenge);
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, account.keyPair().getPrivate());
            challenge = cipher.doFinal(challenge);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new IOException(e);
        }
        int length = input.getInt();
        for (int i = 0; i < length; i++) {
            String id = input.getString();
            VersionUtil.Version version, scapesVersion;
            try {
                version = VersionUtil.get(input.getString());
                scapesVersion = VersionUtil.get(input.getString());
            } catch (VersionUtil.VersionException e) {
                throw new IOException(e);
            }
            byte[] checksum = input.getByteArray();
            Optional<PluginFile> embedded = Streams.of(Plugins.embedded())
                    .filter(plugin -> plugin.id().equals(id))
                    .filter(plugin -> VersionUtil
                            .compare(plugin.version(), version)
                            .in(VersionUtil.Comparison.LOWER_BUILD,
                                    VersionUtil.Comparison.HIGHER_MINOR))
                    .findAny();
            if (embedded.isPresent()) {
                plugins.add(embedded.get());
            } else {
                FileCache.Location location =
                        new FileCache.Location("plugins", checksum);
                Optional<FilePath> file = cache.retrieve(location);
                if (file.isPresent()) {
                    plugins.add(new PluginFile(file.get()));
                } else {
                    pluginRequests.add(i);
                    plugins.add(null);
                }
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
        state = this::loginStep2;
        return Optional.of("Logging in...");
    }

    private Optional<String> loginStep2(RandomReadableByteStream input)
            throws IOException {
        if (input.getBoolean()) {
            throw new ConnectionCloseException(input.getString());
        }
        if (pluginRequests.isEmpty()) {
            return loginStep4();
        }
        state = this::loginStep3;
        return Optional.of("Downloading plugins...");
    }

    private Optional<String> loginStep3(RandomReadableByteStream input)
            throws IOException {
        int request = pluginRequests.get(0);
        if (input.getBoolean()) {
            pluginStream.buffer().flip();
            Optional<FilePath> file =
                    cache.retrieve(cache.store(pluginStream, "plugins"));
            pluginStream.buffer().clear();
            if (!file.isPresent()) {
                throw new IllegalStateException(
                        "Concurrent cache modification");
            }
            plugins.set(request, new PluginFile(file.get()));
            pluginRequests.remove(0);
            if (pluginRequests.isEmpty()) {
                return loginStep4();
            }
        } else {
            ProcessStream.process(input, pluginStream::put);
        }
        return Optional.of("Downloading plugins...");
    }

    private Optional<String> loginStep4() throws IOException {
        WritableByteStream output = channel.getOutputStream();
        output.putInt(loadingDistanceRequest);
        sendSkin(output);
        channel.queueBundle();
        state = this::loginStep5;
        return Optional.of("Receiving server info...");
    }

    private Optional<String> loginStep5(ReadableByteStream input)
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
        FilePath path = engine.home().resolve("Skin.png");
        Image image;
        if (FileUtil.exists(path)) {
            image = FileUtil.readReturn(path,
                    stream -> PNG.decode(stream, BufferCreator::bytes));
        } else {
            AtomicReference<Image> reference = new AtomicReference<>();
            engine.files().get("Scapes:image/entity/mob/Player.png")
                    .read(stream -> {
                        Image defaultImage =
                                PNG.decode(stream, BufferCreator::bytes);
                        reference.set(defaultImage);
                    });
            image = reference.get();
        }
        if (image.width() != 64 || image.height() != 64) {
            throw new ConnectionCloseException("Invalid skin!");
        }
        byte[] skin = new byte[64 * 64 * 4];
        image.buffer().get(skin);
        output.put(skin);
    }

    public Optional<String> login() throws IOException {
        Optional<RandomReadableByteStream> bundle = channel.fetch();
        if (bundle.isPresent()) {
            status = state.apply(bundle.get());
        }
        if (channel.process()) {
            throw new IOException("Connection closed before login");
        }
        return status;
    }

    public IOFunction<GameStateGameMP, RemoteClientConnection> finish()
            throws IOException {
        Plugins plugins = new Plugins(this.plugins, idStorage);
        return state -> new RemoteClientConnection(state, channel, plugins,
                loadingDistance);
    }
}
