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
package org.tobi29.scapes.server.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.Scapes;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.connection.PlayConnection;
import org.tobi29.scapes.engine.server.*;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.io.*;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.entity.skin.ServerSkin;
import org.tobi29.scapes.packets.*;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.plugins.Plugins;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.format.PlayerStatistics;
import org.tobi29.scapes.server.format.WorldFormat;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerConnection
        implements Connection, PlayConnection, Command.Executor {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PlayerConnection.class);
    private static final int AES_MIN_KEY_LENGTH, AES_MAX_KEY_LENGTH;

    static {
        int length = 16;
        try {
            length = Cipher.getMaxAllowedKeyLength("AES") >> 3;
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("Failed to detect maximum key length", e);
        }
        length = FastMath.min(length, 32);
        AES_MAX_KEY_LENGTH = length;
        AES_MIN_KEY_LENGTH = FastMath.min(16, length);
    }

    private final ServerConnection server;
    private final PacketBundleChannel channel;
    private final GameRegistry registry;
    private final Queue<Packet> sendQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger sendQueueSize = new AtomicInteger();
    private State state = State.LOGIN_STEP_1;
    private byte[] challenge;
    private MobPlayerServer entity;
    private ServerSkin skin;
    private PublicKey key;
    private String id, nickname = "_Error_";
    private int loadingRadius, permissionLevel;
    private PlayerStatistics statistics;
    private long ping, pingTimeout, pingWait;

    public PlayerConnection(PacketBundleChannel channel,
            ServerConnection server) throws IOException {
        this.channel = channel;
        this.server = server;
        registry = server.server().worldFormat().plugins().registry();
        loginStep1();
    }

    public MobPlayerServer mob() {
        return entity;
    }

    public ServerSkin skin() {
        return skin;
    }

    public PublicKey key() {
        return key;
    }

    public String id() {
        return id;
    }

    public Optional<InetAddress> address() {
        Optional<InetSocketAddress> address = channel.getRemoteAddress();
        if (address.isPresent()) {
            return Optional.of(address.get().getAddress());
        }
        return Optional.empty();
    }

    public String nickname() {
        return nickname;
    }

    public ServerConnection server() {
        return server;
    }

    public PlayerStatistics statistics() {
        return statistics;
    }

    public long ping() {
        return ping;
    }

    private void loginStep1() throws IOException {
        WritableByteStream output = channel.getOutputStream();
        KeyPair keyPair = server.keyPair();
        byte[] array = keyPair.getPublic().getEncoded();
        output.putInt(array.length);
        output.put(array);
        output.putInt(AES_MAX_KEY_LENGTH);
        channel.queueBundle();
    }

    private void loginStep2(ReadableByteStream input) throws IOException {
        int keyLength = input.getInt();
        keyLength = FastMath.min(keyLength, AES_MAX_KEY_LENGTH);
        if (keyLength < AES_MIN_KEY_LENGTH) {
            throw new IOException(
                    "Key length too short: " + keyLength);
        }
        byte[] keyServer = new byte[keyLength];
        byte[] keyClient = new byte[keyLength];
        try {
            KeyPair keyPair = server.keyPair();
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] array = new byte[cipher.getOutputSize(keyLength << 1)];
            input.get(array);
            array = cipher.doFinal(array);
            System.arraycopy(array, 0, keyServer, 0, keyLength);
            System.arraycopy(array, keyLength, keyClient, 0, keyLength);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            throw new IOException(e);
        }
        channel.setKey(keyServer, keyClient);
    }

    private void loginStep3(ReadableByteStream input) throws IOException {
        byte[] array = new byte[550];
        input.get(array);
        id = ChecksumUtil.getChecksum(array, ChecksumUtil.Algorithm.SHA1);
        challenge = new byte[501];
        new SecureRandom().nextBytes(challenge);
        WritableByteStream output = channel.getOutputStream();
        try {
            key = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(array));
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            output.put(cipher.doFinal(challenge));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidKeySpecException e) {
            throw new IOException(e);
        }
        Plugins plugins = server.server().worldFormat().plugins();
        output.putInt(plugins.fileCount());
        Iterator<PluginFile> pluginIterator = plugins.files().iterator();
        while (pluginIterator.hasNext()) {
            sendPluginChecksum(pluginIterator.next(), output);
        }
        TagStructureBinary
                .write(server.server().worldFormat().idStorage().save(),
                        output);
        channel.queueBundle();
    }

    private void loginStep4(ReadableByteStream input) throws IOException {
        Plugins plugins = server.server().worldFormat().plugins();
        byte[] challenge = new byte[this.challenge.length];
        input.get(challenge);
        nickname = input.getString(1 << 10);
        loadingRadius = FastMath.clamp(input.getInt(), 10,
                server.server().maxLoadingRadius());
        ByteBuffer buffer = ByteBuffer.allocate(64 * 64 * 4);
        input.get(buffer);
        buffer.flip();
        skin = new ServerSkin(new Image(64, 64, buffer));
        int length = input.getInt();
        List<Integer> requests = new ArrayList<>(length);
        while (length-- > 0) {
            requests.add(input.getInt());
        }
        WritableByteStream output = channel.getOutputStream();
        Optional<String> response =
                generateResponse(Arrays.equals(challenge, this.challenge));
        if (!response.isPresent()) {
            response = start();
        }
        if (response.isPresent()) {
            output.putBoolean(true);
            output.putString(response.get());
            channel.queueBundle();
            throw new ConnectionCloseException(response.get());
        } else {
            output.putBoolean(false);
            output.putInt(loadingRadius);
            for (int request : requests) {
                sendPlugin(plugins.file(request).file(), output);
            }
            channel.queueBundle();
            long currentTime = System.currentTimeMillis();
            pingWait = currentTime + 1000;
            pingTimeout = currentTime + 10000;
            LOGGER.info("Client accepted: {} ({}) on {}", id, nickname,
                    channel.toString());
        }
    }

    private Optional<String> start() {
        WorldFormat worldFormat = server.server().worldFormat();
        Optional<TagStructure> tag = worldFormat.playerData().load(id);
        WorldServer world;
        statistics = new PlayerStatistics();
        if (tag.isPresent()) {
            TagStructure tagStructure = tag.get();
            world = worldFormat.world(tagStructure.getString("World"));
            entity = server().server().worldFormat().plugins().worldType()
                    .newPlayer(world, Vector3d.ZERO, Vector3d.ZERO, 0.0, 0.0,
                            nickname, skin.checksum(), this);
            entity.read(tagStructure.getStructure("Entity"));
            statistics
                    .load(world.registry(), tagStructure.getList("Statistics"));
            permissionLevel = tagStructure.getInteger("Permissions");
        } else {
            world = worldFormat.defaultWorld();
            entity = server().server().worldFormat().plugins().worldType()
                    .newPlayer(world,
                            new Vector3d(0.5, 0.5, 1.0).plus(world.spawn()),
                            Vector3d.ZERO, 0.0, 0.0, nickname, skin.checksum(),
                            this);
            entity.onSpawn();
        }
        world.addEntity(entity);
        world.addPlayer(entity);
        sendQueueSize.incrementAndGet();
        sendQueue.add(new PacketSetWorld(world, entity));
        state = State.OPEN;
        return server.addPlayer(this);
    }

    private Optional<String> generateResponse(boolean challengeMatch) {
        if (!server.doesAllowJoin()) {
            return Optional.of("Server not public!");
        }
        if (!challengeMatch) {
            return Optional.of("Invalid private key!");
        }
        if (!server.server().worldFormat().playerData().playerExists(id)) {
            if (!server.doesAllowCreation()) {
                return Optional
                        .of("This server does not allow account creation!");
            }
        }
        Optional<String> nicknameCheck = Account.isNameValid(nickname);
        if (nicknameCheck.isPresent()) {
            return nicknameCheck;
        }
        Optional<String> banCheck =
                server.server().worldFormat().playerBans().matches(this);
        if (banCheck.isPresent()) {
            return banCheck;
        }
        return Optional.empty();
    }

    @Override
    public void send(Packet packet) {
        if (sendQueueSize.get() > 128) {
            if (!packet.isVital()) {
                return;
            }
        }
        sendQueueSize.incrementAndGet();
        sendQueue.add(packet);
    }

    public int loadingRadius() {
        return loadingRadius;
    }

    private void sendPluginChecksum(PluginFile plugin,
            WritableByteStream output) throws IOException {
        byte[] checksum = plugin.checksum();
        output.putInt(checksum.length);
        output.put(checksum);
    }

    private void sendPlugin(Path path, WritableByteStream output)
            throws IOException {
        output.putInt((int) Files.size(path));
        FileUtil.read(path,
                stream -> ProcessStream.process(stream, output::put));
    }

    private void sendPacket(Packet packet) {
        if (!packet.isVital() && sendQueueSize.get() > 256) {
            return;
        }
        Vector3 pos3d = packet.pos();
        boolean flag = true;
        if (pos3d != null) {
            WorldServer world = entity.world();
            if (world != null && !world.getTerrain()
                    .isBlockSendable(entity, pos3d.intX(), pos3d.intY(),
                            pos3d.intZ(), packet.isChunkContent())) {
                flag = false;
            }
            if (flag) {
                double range = packet.range();
                if (range > 0.0) {
                    if (FastMath.pointDistanceSqr(pos3d, entity.pos()) >
                            range * range) {
                        flag = false;
                    }
                }
            }
        }
        if (flag) {
            try {
                WritableByteStream output = channel.getOutputStream();
                output.putShort(packet.id(
                        server.server().worldFormat().plugins().registry()));
                ((PacketClient) packet).sendClient(this, output);
            } catch (SocketException e) {
            } catch (IOException e) {
                LOGGER.error("Error in connection: {}", e.toString());
            }
        }
    }

    @Override
    public void register(Selector selector, int opt) throws IOException {
        channel.register(selector, opt);
    }

    @Override
    public boolean tick(AbstractServerConnection.NetWorkerThread worker) {
        try {
            switch (state) {
                case LOGIN_STEP_1: {
                    Optional<ReadableByteStream> bundle = channel.fetch();
                    if (bundle.isPresent()) {
                        loginStep2(bundle.get());
                        state = State.LOGIN_STEP_2;
                    }
                    return channel.process();
                }
                case LOGIN_STEP_2: {
                    Optional<ReadableByteStream> bundle = channel.fetch();
                    if (bundle.isPresent()) {
                        loginStep3(bundle.get());
                        state = State.LOGIN_STEP_3;
                    }
                    return channel.process();
                }
                case LOGIN_STEP_3: {
                    Optional<ReadableByteStream> bundle = channel.fetch();
                    if (bundle.isPresent()) {
                        loginStep4(bundle.get());
                    }
                    return channel.process();
                }
                case OPEN:
                    long currentTime = System.currentTimeMillis();
                    if (pingTimeout < currentTime) {
                        throw new ConnectionCloseException(
                                "Connection timeout");
                    }
                    if (pingWait < currentTime) {
                        pingWait = System.currentTimeMillis() + 1000;
                        sendPacket(new PacketPingServer(currentTime));
                    }
                    while (!sendQueue.isEmpty()) {
                        Packet packet = sendQueue.poll();
                        sendPacket(packet);
                        sendQueueSize.decrementAndGet();
                        if (channel.bundleSize() > 1 << 10 << 4) {
                            break;
                        }
                    }
                    channel.queueBundle();
                    Optional<ReadableByteStream> bundle = channel.fetch();
                    if (bundle.isPresent()) {
                        ReadableByteStream stream = bundle.get();
                        while (stream.hasRemaining()) {
                            PacketServer packet = (PacketServer) Packet
                                    .make(registry, stream.getShort());
                            packet.parseServer(this, stream);
                            packet.runServer(this, entity.world());
                        }
                    }
                    return channel.process();
                case CLOSING:
                    if (channel.process()) {
                        return true;
                    } else {
                        state = State.CLOSED;
                    }
                    break;
            }
        } catch (ConnectionCloseException | InvalidPacketDataException e) {
            LOGGER.info("Disconnecting player: {}", e.toString());
            state = State.CLOSING;
        } catch (IOException e) {
            LOGGER.info("Player disconnected: {}", e.toString());
            state = State.CLOSED;
        }
        return false;
    }

    @Override
    public boolean isClosed() {
        return state == State.CLOSED;
    }

    @Override
    public void close() throws IOException {
        state = State.CLOSED;
        channel.close();
        if (entity != null) {
            server.removePlayer(this);
            WorldServer world = entity.world();
            if (world != null) {
                world.deleteEntity(entity);
                world.removePlayer(entity);
                TagStructure tagStructure = new TagStructure();
                tagStructure.setStructure("Entity", entity.write(false));
                tagStructure.setString("World", world.name());
                tagStructure.setList("Statistics", statistics.save());
                tagStructure.setInteger("Permissions", permissionLevel);
                server.server().worldFormat().playerData()
                        .save(tagStructure, id);
            }
        }
        LOGGER.info("Client disconnected: {} ({})", id, nickname);
    }

    public void updatePing(long ping) {
        this.ping = System.currentTimeMillis() - ping;
        pingTimeout = ping + 10000;
    }

    @Override
    public Optional<String> playerName() {
        return Optional.of(nickname);
    }

    @Override
    public String name() {
        return nickname;
    }

    @Override
    public void tell(String message) {
        LOGGER.info("Chat ({}): {}", nickname, message);
        send(new PacketChat(message));
    }

    @Override
    public int permissionLevel() {
        if (Scapes.debug) {
            return 10;
        }
        return permissionLevel;
    }

    public void setPermissionLevel(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    enum State {
        LOGIN_STEP_1,
        LOGIN_STEP_2,
        LOGIN_STEP_3,
        OPEN,
        CLOSING,
        CLOSED
    }
}
