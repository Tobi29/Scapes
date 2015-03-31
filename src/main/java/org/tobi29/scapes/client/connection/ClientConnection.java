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
import org.tobi29.scapes.connection.ConnectionCloseException;
import org.tobi29.scapes.connection.ConnectionType;
import org.tobi29.scapes.connection.PlayConnection;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.SleepUtil;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.graphics.PNG;
import org.tobi29.scapes.engine.utils.io.FileCache;
import org.tobi29.scapes.engine.utils.io.LimitedInputStream;
import org.tobi29.scapes.engine.utils.io.PacketBundleChannel;
import org.tobi29.scapes.engine.utils.io.filesystem.File;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.task.Joiner;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.packets.PacketClient;
import org.tobi29.scapes.packets.PacketServer;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.plugins.Plugins;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientConnection
        implements TaskExecutor.ASyncTask, PlayConnection {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ClientConnection.class);
    private static final int AES_KEY_LENGTH;
    private final ScapesEngine engine;
    private final PacketBundleChannel channel;
    private final IDStorage idStorage = new IDStorage();
    private final int loadingDistance;
    private final FileCache cache;
    private final GuiWidgetDebugValues.Element pingDebug, downloadDebug,
            uploadDebug;
    private final Queue<Packet> sendQueue = new ConcurrentLinkedQueue<>();
    private LoginData loginData;
    private GameStateGameMP game;
    private MobPlayerClientMain entity;
    private Joiner joiner;
    private State state = State.LOGIN_STEP_1;
    private WorldClient world;
    private Plugins plugins;

    static {
        int length = 16;
        try {
            length = Cipher.getMaxAllowedKeyLength("AES") >> 3;
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("Failed to detect maximum key length", e);
        }
        AES_KEY_LENGTH = length;
    }

    public ClientConnection(ScapesEngine engine, SocketChannel channel,
            Account.Client account, int loadingDistance) {
        this.engine = engine;
        this.loadingDistance = loadingDistance;
        this.channel = new PacketBundleChannel(channel);
        GuiWidgetDebugValues debugValues = engine.getDebugValues();
        pingDebug = debugValues.get("Connection-Ping");
        downloadDebug = debugValues.get("Connection-Down");
        uploadDebug = debugValues.get("Connection-Up");
        cache = engine.getFileCache();
        loginData = new LoginData(channel, account);
    }

    public boolean login() throws IOException, ConnectionCloseException {
        switch (state) {
            case LOGIN_STEP_1:
                loginData.channel.write(loginData.headerBuffer);
                if (!loginData.headerBuffer.hasRemaining()) {
                    state = State.LOGIN_STEP_2;
                }
            case LOGIN_STEP_2:
                Optional<DataInputStream> bundle = channel.fetch();
                if (bundle.isPresent()) {
                    DataInputStream streamIn = bundle.get();
                    try {
                        byte[] array = new byte[streamIn.readInt()];
                        streamIn.readFully(array);
                        int keyLength = streamIn.readInt();
                        keyLength = FastMath.min(keyLength, AES_KEY_LENGTH);
                        byte[] keyServer = new byte[keyLength];
                        byte[] keyClient = new byte[keyLength];
                        Random random = new SecureRandom();
                        random.nextBytes(keyServer);
                        random.nextBytes(keyClient);
                        DataOutputStream streamOut = channel.getOutputStream();
                        streamOut.writeInt(keyLength);
                        PublicKey rsaKey = KeyFactory.getInstance("RSA")
                                .generatePublic(new X509EncodedKeySpec(array));
                        Cipher cipher = Cipher.getInstance("RSA");
                        cipher.init(Cipher.ENCRYPT_MODE, rsaKey);
                        streamOut.write(cipher.update(keyServer));
                        streamOut.write(cipher.doFinal(keyClient));
                        channel.queueBundle();
                        channel.setKey(keyClient, keyServer);
                        KeyPair keyPair = loginData.account.getKeyPair();
                        array = keyPair.getPublic().getEncoded();
                        streamOut.write(array);
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
                    DataInputStream streamIn = bundle.get();
                    byte[] challenge = new byte[512];
                    streamIn.readFully(challenge);
                    try {
                        Cipher cipher = Cipher.getInstance("RSA");
                        cipher.init(Cipher.DECRYPT_MODE,
                                loginData.account.getKeyPair().getPrivate());
                        challenge = cipher.doFinal(challenge);
                    } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
                        throw new IOException(e);
                    }
                    int length = streamIn.readInt();
                    for (int i = 0; i < length; i++) {
                        Optional<File> file = checkPlugin(streamIn);
                        if (file.isPresent()) {
                            loginData.plugins.add(new PluginFile(file.get()));
                        } else {
                            loginData.pluginRequests.add(i);
                            loginData.plugins.add(null);
                        }
                    }
                    readIDStorage(streamIn);
                    DataOutputStream streamOut = channel.getOutputStream();
                    streamOut.write(challenge);
                    streamOut.writeUTF(loginData.account.getNickname());
                    streamOut.writeInt(loadingDistance);
                    sendSkin(streamOut);
                    streamOut.writeInt(loginData.pluginRequests.size());
                    for (Integer request : loginData.pluginRequests) {
                        streamOut.writeInt(request);
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
                    DataInputStream streamIn = bundle.get();
                    if (streamIn.readBoolean()) {
                        throw new ConnectionCloseException(streamIn.readUTF());
                    }
                    for (Integer request : loginData.pluginRequests) {
                        Optional<File> file = cache.retrieve(cache.store(
                                new LimitedInputStream(streamIn,
                                        streamIn.readInt()), "plugins"));
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

    private void readIDStorage(DataInputStream streamIn) throws IOException {
        TagStructure idsTag = new TagStructure();
        TagStructureBinary.read(idsTag, streamIn);
        idStorage.load(idsTag);
    }

    private Optional<File> checkPlugin(DataInputStream streamIn)
            throws IOException {
        byte[] checksum = new byte[streamIn.readInt()];
        streamIn.readFully(checksum);
        FileCache.Location location =
                new FileCache.Location("plugins", checksum);
        return cache.retrieve(location);
    }

    private void sendSkin(DataOutputStream streamOut)
            throws IOException, ConnectionCloseException {
        Image image;
        File file = engine.getFiles().getFile("File:Skin.png");
        if (file.exists()) {
            try (InputStream streamIn = file.read()) {
                image = PNG.decode(streamIn, BufferCreator::byteBuffer);
                if (image.getWidth() != 64 || image.getHeight() != 64) {
                    throw new ConnectionCloseException("Invalid skin!");
                }
            }
        } else {
            try (InputStream streamIn = engine.getFiles()
                    .getResource("Scapes:image/entity/mob/Player.png").read()) {
                image = PNG.decode(streamIn, BufferCreator::byteBuffer);
                if (image.getWidth() != 64 || image.getHeight() != 64) {
                    throw new ConnectionCloseException("Invalid skin!");
                }
            }
        }
        byte[] skin = new byte[64 * 64 * 4];
        image.getBuffer().get(skin);
        streamOut.write(skin);
    }

    @Override
    public void run(Joiner joiner) {
        try {
            while (!joiner.marked()) {
                DataOutputStream streamOut = channel.getOutputStream();
                while (!sendQueue.isEmpty()) {
                    Packet packet = sendQueue.poll();
                    streamOut.writeShort(packet.getID(plugins.getRegistry()));
                    ((PacketServer) packet).sendServer(this, streamOut);
                }
                channel.queueBundle();
                Optional<DataInputStream> bundle = channel.fetch();
                if (bundle.isPresent()) {
                    DataInputStream streamIn = bundle.get();
                    while (streamIn.available() > 0) {
                        PacketClient packet = (PacketClient) Packet
                                .makePacket(plugins.getRegistry(),
                                        streamIn.readShort());
                        packet.parseClient(this, streamIn);
                        packet.runClient(this, world);
                    }
                } else {
                    if (!channel.process()) {
                        SleepUtil.sleep(10);
                    }
                }
            }
        } catch (ConnectionCloseException | IOException e) {
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
    }

    @Override
    public synchronized void send(Packet packet) {
        if (!(packet instanceof PacketServer)) {
            throw new IllegalArgumentException(
                    "Packet is not a to-server packet!");
        }
        sendQueue.add(packet);
    }

    @Override
    public int getLoadingRadius() {
        return loadingDistance;
    }

    public void start(GameStateGameMP game) {
        this.game = game;
        joiner = engine.getTaskExecutor().runTask(this, "Client-Connection");
        engine.getTaskExecutor().addTask(() -> {
            downloadDebug.setValue(channel.getInputRate() / 128.0);
            uploadDebug.setValue(channel.getOutputRate() / 128.0);
            return state == State.CLOSED ? -1 : 1000;
        }, "Connection-Rate", 1000, false);
    }

    public void stop() {
        state = State.CLOSED;
        joiner.join();
    }

    public MobPlayerClientMain getEntity() {
        return entity;
    }

    public WorldClient getWorld() {
        return world;
    }

    public GameStateGameMP getGame() {
        return game;
    }

    public Plugins getPlugins() {
        return plugins;
    }

    public void changeWorld(WorldClient world, int playerID,
            TagStructure tagStructure) {
        this.world = world;
        entity = world.getPlayer();
        entity.read(tagStructure);
        world.addEntity(entity, playerID);
        game.setScene(world.getScene());
        LOGGER.info("Received player entity: {} with id: {}", entity, playerID);
    }

    public void updatePing(long ping) {
        pingDebug.setValue(ping);
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
                        ConnectionType.PLAY.getData()});

        private LoginData(SocketChannel channel, Account.Client account) {
            this.channel = channel;
            this.account = account;
        }
    }
}
