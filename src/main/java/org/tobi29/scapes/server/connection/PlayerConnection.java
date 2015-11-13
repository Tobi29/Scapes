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
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.entity.skin.ServerSkin;
import org.tobi29.scapes.packets.*;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.plugins.Plugins;
import org.tobi29.scapes.server.MessageLevel;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.extension.event.PlayerAuthenticateEvent;
import org.tobi29.scapes.server.format.PlayerData;
import org.tobi29.scapes.server.format.PlayerStatistics;
import org.tobi29.scapes.server.format.WorldFormat;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
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
        loginStep0();
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

    private void loginStep0() throws IOException {
        WritableByteStream output = channel.getOutputStream();
        KeyPair keyPair = server.keyPair();
        byte[] array = keyPair.getPublic().getEncoded();
        output.putInt(array.length);
        output.put(array);
        output.putInt(AES_MAX_KEY_LENGTH);
        channel.queueBundle();
    }

    private void loginStep1(ReadableByteStream input) throws IOException {
        int keyLength = input.getInt();
        keyLength = FastMath.min(keyLength, AES_MAX_KEY_LENGTH);
        if (keyLength < AES_MIN_KEY_LENGTH) {
            throw new IOException("Key length too short: " + keyLength);
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
        state = State.LOGIN_STEP_2;
    }

    private void loginStep2(ReadableByteStream input) throws IOException {
        byte[] array = new byte[550];
        input.get(array);
        id = ChecksumUtil.checksum(array, ChecksumUtil.Algorithm.SHA1)
                .toString();
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
        channel.queueBundle();
        state = State.LOGIN_STEP_3;
    }

    private void loginStep3(ReadableByteStream input) throws IOException {
        Plugins plugins = server.server().worldFormat().plugins();
        byte[] challenge = new byte[this.challenge.length];
        input.get(challenge);
        nickname = input.getString(1 << 10);
        int length = input.getInt();
        List<Integer> requests = new ArrayList<>(length);
        while (length-- > 0) {
            requests.add(input.getInt());
        }
        WritableByteStream output = channel.getOutputStream();
        Optional<String> response =
                generateResponse(Arrays.equals(challenge, this.challenge));
        if (response.isPresent()) {
            output.putBoolean(true);
            output.putString(response.get());
            channel.queueBundle();
            throw new ConnectionCloseException(response.get());
        }
        output.putBoolean(false);
        channel.queueBundle();
        for (int request : requests) {
            sendPlugin(plugins.file(request).file(), output);
        }
        state = State.LOGIN_STEP_4;
    }

    private void loginStep4(ReadableByteStream input) throws IOException {
        loadingRadius = FastMath.clamp(input.getInt(), 10,
                server.server().maxLoadingRadius());
        ByteBuffer buffer = ByteBuffer.allocate(64 * 64 * 4);
        input.get(buffer);
        buffer.flip();
        skin = new ServerSkin(new Image(64, 64, buffer));
        WritableByteStream output = channel.getOutputStream();
        setWorld();
        Optional<String> response = server.addPlayer(this);
        if (response.isPresent()) {
            output.putBoolean(true);
            output.putString(response.get());
            channel.queueBundle();
            throw new ConnectionCloseException(response.get());
        }
        output.putBoolean(false);
        output.putInt(loadingRadius);
        TagStructureBinary
                .write(server.server().worldFormat().idStorage().save(),
                        output);
        channel.queueBundle();
        long currentTime = System.currentTimeMillis();
        pingWait = currentTime + 1000;
        pingTimeout = currentTime + 10000;
        server.message("Player connected: " + id + " (" + nickname + ") on " +
                channel, MessageLevel.SERVER_INFO);
        state = State.OPEN;
    }

    public synchronized void setWorld() {
        setWorld(null);
    }

    public synchronized void setWorld(WorldServer world) {
        setWorld(world, null);
    }

    public synchronized void setWorld(WorldServer world, Vector3 pos) {
        if (entity != null) {
            entity.world().deleteEntity(entity);
            entity.world().removePlayer(entity);
            save();
        }
        WorldFormat worldFormat = server.server().worldFormat();
        PlayerData.Player player = worldFormat.playerData().player(id);
        statistics = player.statistics(registry);
        permissionLevel = player.permissions();
        entity = player.createEntity(this, Optional.ofNullable(world));
        if (pos != null) {
            entity.setPos(pos);
        }
        entity.world().addEntity(entity);
        entity.world().addPlayer(entity);
        sendQueueSize.incrementAndGet();
        sendQueue.add(new PacketSetWorld(entity.world(), entity));
    }

    private void save() {
        server.server().worldFormat().playerData()
                .save(id, entity, permissionLevel, statistics);
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
        PlayerAuthenticateEvent event = new PlayerAuthenticateEvent(this);
        server.server().extensions().fireEvent(event);
        if (!event.success()) {
            return Optional.of(event.reason());
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
        byte[] checksum = plugin.checksum().array();
        output.putInt(checksum.length);
        output.put(checksum);
    }

    private void sendPlugin(Path path, WritableByteStream output)
            throws IOException {
        FileUtil.read(path, stream -> ProcessStream.process(stream, buffer -> {
            output.putBoolean(false);
            output.put(buffer);
            channel.queueBundle();
        }, 1 << 10 << 10));
        output.putBoolean(true);
        channel.queueBundle();
    }

    private void sendPacket(Packet packet) throws IOException {
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
            WritableByteStream output = channel.getOutputStream();
            output.putShort(packet.id(
                    server.server().worldFormat().plugins().registry()));
            ((PacketClient) packet).sendClient(this, output);
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
                    if (channel.bundleSize() > 0) {
                        channel.queueBundle();
                    }
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
                default:
                    bundle = channel.fetch();
                    if (bundle.isPresent()) {
                        switch (state) {
                            case LOGIN_STEP_1:
                                loginStep1(bundle.get());
                                break;
                            case LOGIN_STEP_2:
                                loginStep2(bundle.get());
                                break;
                            case LOGIN_STEP_3:
                                loginStep3(bundle.get());
                                break;
                            case LOGIN_STEP_4:
                                loginStep4(bundle.get());
                                break;
                        }
                    }
                    return channel.process();
            }
        } catch (ConnectionCloseException | InvalidPacketDataException e) {
            server.message("Disconnecting player: " + nickname,
                    MessageLevel.SERVER_INFO);
            state = State.CLOSING;
        } catch (IOException e) {
            server.message(
                    "Player disconnected: " + nickname + " (" + e.toString() +
                            ')', MessageLevel.SERVER_INFO);
            state = State.CLOSED;
        }
        return false;
    }

    @Override
    public boolean isClosed() {
        return state == State.CLOSED;
    }

    @Override
    public synchronized void close() throws IOException {
        state = State.CLOSED;
        channel.close();
        server.removePlayer(this);
        if (entity != null) {
            entity.world().deleteEntity(entity);
            entity.world().removePlayer(entity);
            save();
        }
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
    public boolean message(String message, MessageLevel level) {
        if (level.level() < MessageLevel.CHAT.level()) {
            return false;
        }
        send(new PacketChat(message));
        return true;
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
        LOGIN_STEP_4,
        OPEN,
        CLOSING,
        CLOSED
    }
}
