/*
 * Copyright 2012-2015 Tobi29
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
package org.tobi29.scapes.client.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.chunk.IDStorage;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.client.states.GameStateServerDisconnect;
import org.tobi29.scapes.connection.Account;
import org.tobi29.scapes.connection.ConnectionType;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues;
import org.tobi29.scapes.engine.server.ConnectionCloseException;
import org.tobi29.scapes.engine.server.PlayConnection;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.MutableSingle;
import org.tobi29.scapes.engine.utils.SleepUtil;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.graphics.PNG;
import org.tobi29.scapes.engine.utils.io.LimitedBufferStream;
import org.tobi29.scapes.engine.utils.io.PacketBundleChannel;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.engine.utils.io.filesystem.FileCache;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.task.Joiner;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.packets.PacketClient;
import org.tobi29.scapes.packets.PacketPingClient;
import org.tobi29.scapes.packets.PacketServer;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.plugins.Plugins;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientConnection
        implements TaskExecutor.ASyncTask, PlayConnection<Packet> {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ClientConnection.class);
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
    private final Selector selector;
    private final IDStorage idStorage = new IDStorage();
    private final FileCache cache;
    private final GuiWidgetDebugValues.Element pingDebug, downloadDebug,
            uploadDebug;
    private final Queue<Packet> sendQueue = new ConcurrentLinkedQueue<>();
    private int loadingDistance = -1;
    private LoginData loginData;
    private GameStateGameMP game;
    private MobPlayerClientMain entity;
    private Joiner joiner;
    private State state = State.LOGIN_STEP_1;
    private WorldClient world;
    private Plugins plugins;

    public ClientConnection(ScapesEngine engine, SocketChannel channel,
            Account.Client account, int loadingDistance) throws IOException {
        this.engine = engine;
        loadingDistanceRequest = loadingDistance;
        this.channel = new PacketBundleChannel(channel);
        selector = Selector.open();
        this.channel.register(selector, SelectionKey.OP_READ);
        GuiWidgetDebugValues debugValues = engine.debugValues();
        pingDebug = debugValues.get("Connection-Ping");
        downloadDebug = debugValues.get("Connection-Down");
        uploadDebug = debugValues.get("Connection-Up");
        cache = engine.fileCache();
        loginData = new LoginData(channel, account);
    }

    public boolean login() throws IOException {
        switch (state) {
            case LOGIN_STEP_1:
                loginData.channel.write(loginData.headerBuffer);
                if (!loginData.headerBuffer.hasRemaining()) {
                    state = State.LOGIN_STEP_2;
                }
            case LOGIN_STEP_2:
                Optional<ReadableByteStream> bundle = channel.fetch();
                if (bundle.isPresent()) {
                    ReadableByteStream input = bundle.get();
                    try {
                        byte[] array = new byte[input.getInt()];
                        input.get(array);
                        int keyLength = input.getInt();
                        keyLength = FastMath.min(keyLength, AES_MAX_KEY_LENGTH);
                        if (keyLength < AES_MIN_KEY_LENGTH) {
                            throw new IOException(
                                    "Key length too short: " + keyLength);
                        }
                        byte[] keyServer = new byte[keyLength];
                        byte[] keyClient = new byte[keyLength];
                        Random random = new SecureRandom();
                        random.nextBytes(keyServer);
                        random.nextBytes(keyClient);
                        WritableByteStream output = channel.getOutputStream();
                        output.putInt(keyLength);
                        PublicKey rsaKey = KeyFactory.getInstance("RSA")
                                .generatePublic(new X509EncodedKeySpec(array));
                        Cipher cipher = Cipher.getInstance("RSA");
                        cipher.init(Cipher.ENCRYPT_MODE, rsaKey);
                        output.put(cipher.update(keyServer));
                        output.put(cipher.doFinal(keyClient));
                        channel.queueBundle();
                        channel.setKey(keyClient, keyServer);
                        KeyPair keyPair = loginData.account.keyPair();
                        array = keyPair.getPublic().getEncoded();
                        output.put(array);
                        channel.queueBundle();
                        while (channel.process()) {
                            SleepUtil.sleep(10);
                        }
                    } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidKeySpecException e) {
                        throw new IOException(e);
                    }
                    state = State.LOGIN_STEP_3;
                }
                break;
            case LOGIN_STEP_3:
                bundle = channel.fetch();
                if (bundle.isPresent()) {
                    ReadableByteStream input = bundle.get();
                    byte[] challenge = new byte[512];
                    input.get(challenge);
                    try {
                        Cipher cipher = Cipher.getInstance("RSA");
                        cipher.init(Cipher.DECRYPT_MODE,
                                loginData.account.keyPair().getPrivate());
                        challenge = cipher.doFinal(challenge);
                    } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
                        throw new IOException(e);
                    }
                    int length = input.getInt();
                    for (int i = 0; i < length; i++) {
                        Optional<Path> file = checkPlugin(input);
                        if (file.isPresent()) {
                            loginData.plugins.add(new PluginFile(file.get()));
                        } else {
                            loginData.pluginRequests.add(i);
                            loginData.plugins.add(null);
                        }
                    }
                    readIDStorage(input);
                    WritableByteStream output = channel.getOutputStream();
                    output.put(challenge);
                    output.putString(loginData.account.nickname());
                    output.putInt(loadingDistanceRequest);
                    sendSkin(output);
                    output.putInt(loginData.pluginRequests.size());
                    for (int i : loginData.pluginRequests) {
                        output.putInt(i);
                    }
                    channel.queueBundle();
                    while (channel.process()) {
                        SleepUtil.sleep(10);
                    }
                    state = State.LOGIN_STEP_4;
                }
                break;
            case LOGIN_STEP_4:
                bundle = channel.fetch();
                if (bundle.isPresent()) {
                    ReadableByteStream input = bundle.get();
                    if (input.getBoolean()) {
                        throw new ConnectionCloseException(input.getString());
                    }
                    loadingDistance = input.getInt();
                    for (Integer request : loginData.pluginRequests) {
                        Optional<Path> file = cache.retrieve(cache.store(
                                new LimitedBufferStream(input, input.getInt()),
                                "plugins"));
                        if (!file.isPresent()) {
                            throw new IllegalStateException(
                                    "Concurrent cache modification");
                        }
                        loginData.plugins
                                .set(request, new PluginFile(file.get()));
                    }
                    loginData.pluginRequests.clear();
                    plugins = new Plugins(loginData.plugins, idStorage);
                    state = State.OPEN;
                    loginData = null;
                    return true;
                }
                break;
            default:
                throw new IllegalStateException(
                        "Invalid state for login: " + state);
        }
        return false;
    }

    private void readIDStorage(ReadableByteStream stream) throws IOException {
        TagStructure idsTag = new TagStructure();
        TagStructureBinary.read(idsTag, stream);
        idStorage.load(idsTag);
    }

    private Optional<Path> checkPlugin(ReadableByteStream stream)
            throws IOException {
        byte[] checksum = new byte[stream.getInt()];
        stream.get(checksum);
        FileCache.Location location =
                new FileCache.Location("plugins", checksum);
        return cache.retrieve(location);
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

    @Override
    public void run(Joiner joiner) {
        try {
            while (!joiner.marked()) {
                WritableByteStream output = channel.getOutputStream();
                while (!sendQueue.isEmpty()) {
                    Packet packet = sendQueue.poll();
                    output.putShort(packet.id(plugins.registry()));
                    ((PacketServer) packet).sendServer(this, output);
                }
                channel.queueBundle();
                Optional<ReadableByteStream> bundle = channel.fetch();
                if (bundle.isPresent()) {
                    ReadableByteStream input = bundle.get();
                    while (input.hasRemaining()) {
                        PacketClient packet = (PacketClient) Packet
                                .make(plugins.registry(), input.getShort());
                        packet.parseClient(this, input);
                        packet.runClient(this, world);
                    }
                }
                if (!channel.process() && !joiner.marked()) {
                    try {
                        selector.select(10);
                        selector.selectedKeys().clear();
                    } catch (IOException e) {
                        LOGGER.warn("Error when waiting for events: {}",
                                e.toString());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.info("Lost connection: {}", e.toString());
            engine.setState(new GameStateServerDisconnect(e.getMessage(),
                    channel.getRemoteAddress(), engine));
        }
        try {
            close();
        } catch (IOException e) {
            LOGGER.error("Error closing socket: {}", e.toString());
        }
        LOGGER.info("Closed client connection!");
    }

    private void close() throws IOException {
        channel.close();
        selector.close();
    }

    @Override
    public synchronized void send(Packet packet) {
        if (!(packet instanceof PacketServer)) {
            throw new IllegalArgumentException(
                    "Packet is not a to-server packet!");
        }
        sendQueue.add(packet);
    }

    public int loadingRadius() {
        if (loadingDistance == -1) {
            throw new IllegalStateException("Client not logged in");
        }
        return loadingDistance;
    }

    public void start(GameStateGameMP game) {
        this.game = game;
        joiner = engine.taskExecutor().runTask(this, "Client-Connection");
        engine.taskExecutor().addTask(() -> {
            send(new PacketPingClient(System.currentTimeMillis()));
            downloadDebug.setValue(channel.getInputRate() / 128.0);
            uploadDebug.setValue(channel.getOutputRate() / 128.0);
            return state == State.CLOSED ? -1 : 1000;
        }, "Connection-Rate", 1000, false);
    }

    public void stop() {
        state = State.CLOSED;
        joiner.join();
    }

    public MobPlayerClientMain entity() {
        return entity;
    }

    public WorldClient world() {
        return world;
    }

    public GameStateGameMP game() {
        return game;
    }

    public Plugins plugins() {
        return plugins;
    }

    public void changeWorld(WorldClient world) {
        this.world = world;
        entity = world.player();
        game.setScene(world.scene());
    }

    public void updatePing(long ping) {
        pingDebug.setValue(System.currentTimeMillis() - ping);
    }

    enum State {
        LOGIN_STEP_1,
        LOGIN_STEP_2,
        LOGIN_STEP_3,
        LOGIN_STEP_4,
        OPEN,
        CLOSED
    }

    private static class LoginData {
        private final SocketChannel channel;
        private final List<Integer> pluginRequests = new ArrayList<>();
        private final List<PluginFile> plugins = new ArrayList<>();
        private final Account.Client account;
        private final ByteBuffer headerBuffer = BufferCreator
                .wrap(new byte[]{'S', 'c', 'a', 'p', 'e', 's',
                        ConnectionType.PLAY.data()});

        private LoginData(SocketChannel channel, Account.Client account) {
            this.channel = channel;
            this.account = account;
        }
    }
}
