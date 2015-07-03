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
import org.tobi29.scapes.connection.*;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.io.*;
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
    private static final int AES_KEY_LENGTH;
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

    static {
        int length = 16;
        try {
            length = Cipher.getMaxAllowedKeyLength("AES") >> 3;
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("Failed to detect maximum key length", e);
        }
        length = FastMath.min(length, 32);
        AES_KEY_LENGTH = length;
    }

    public PlayerConnection(PacketBundleChannel channel,
            ServerConnection server) throws IOException {
        this.channel = channel;
        this.server = server;
        registry =
                server.getServer().getWorldFormat().getPlugins().getRegistry();
        loginStep1();
    }

    public MobPlayerServer getMob() {
        return entity;
    }

    public ServerSkin getSkin() {
        return skin;
    }

    public PublicKey getKey() {
        return key;
    }

    public String getID() {
        return id;
    }

    public Optional<InetAddress> getAddress() {
        Optional<InetSocketAddress> address = channel.getRemoteAddress();
        if (address.isPresent()) {
            return Optional.of(address.get().getAddress());
        }
        return Optional.empty();
    }

    public String getNickname() {
        return nickname;
    }

    public ServerConnection getServer() {
        return server;
    }

    public PlayerStatistics getStatistics() {
        return statistics;
    }

    public long getPing() {
        return ping;
    }

    private void loginStep1() throws IOException {
        WritableByteStream output = channel.getOutputStream();
        KeyPair keyPair = server.getKeyPair();
        byte[] array = keyPair.getPublic().getEncoded();
        output.putInt(array.length);
        output.put(array);
        output.putInt(AES_KEY_LENGTH);
        channel.queueBundle();
    }

    private void loginStep2(ReadableByteStream input) throws IOException {
        int keyLength = input.getInt();
        keyLength = FastMath.min(keyLength, AES_KEY_LENGTH);
        byte[] keyServer = new byte[keyLength];
        byte[] keyClient = new byte[keyLength];
        try {
            KeyPair keyPair = server.getKeyPair();
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
        id = ChecksumUtil
                .getChecksum(array, ChecksumUtil.ChecksumAlgorithm.SHA1);
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
        Plugins plugins = server.getServer().getWorldFormat().getPlugins();
        output.putInt(plugins.getFileCount());
        Iterator<PluginFile> pluginIterator = plugins.getFiles().iterator();
        while (pluginIterator.hasNext()) {
            sendPluginChecksum(pluginIterator.next(), output);
        }
        TagStructureBinary
                .write(server.getServer().getWorldFormat().getIDStorage()
                        .save(), output);
        channel.queueBundle();
    }

    private void loginStep4(ReadableByteStream input) throws IOException {
        Plugins plugins = server.getServer().getWorldFormat().getPlugins();
        byte[] challenge = new byte[this.challenge.length];
        input.get(challenge);
        nickname = input.getString();
        loadingRadius = FastMath.clamp(input.getInt(), 10,
                server.getServer().getMaxLoadingRadius());
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
                sendPlugin(plugins.getFile(request).getFile(), output);
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
        WorldFormat worldFormat = server.getServer().getWorldFormat();
        Optional<TagStructure> tag = worldFormat.getPlayerData().load(id);
        WorldServer world;
        statistics = new PlayerStatistics();
        if (tag.isPresent()) {
            TagStructure tagStructure = tag.get();
            world = worldFormat.getWorld(tagStructure.getString("World"));
            entity = new MobPlayerServer(world, Vector3d.ZERO, Vector3d.ZERO,
                    0.0, 0.0, nickname, skin.checksum(), this);
            entity.read(tagStructure.getStructure("Entity"));
            statistics.load(world.getRegistry(),
                    tagStructure.getList("Statistics"));
            permissionLevel = tagStructure.getInteger("Permissions");
        } else {
            world = worldFormat.getDefaultWorld();
            entity = new MobPlayerServer(world,
                    new Vector3d(0.5, 0.5, 1.0).plus(world.getSpawn()),
                    Vector3d.ZERO, 0.0, 0.0, nickname, skin.checksum(), this);
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
        if (!server.getAllowsJoin()) {
            return Optional.of("Server not public!");
        }
        if (!challengeMatch) {
            return Optional.of("Invalid private key!");
        }
        if (!server.getServer().getWorldFormat().getPlayerData()
                .playerExists(id)) {
            if (!server.getAllowsCreation()) {
                return Optional
                        .of("This server does not allow account creation!");
            }
        }
        Optional<String> nicknameCheck = Account.isNameValid(nickname);
        if (nicknameCheck.isPresent()) {
            return nicknameCheck;
        }
        Optional<String> banCheck =
                server.getServer().getWorldFormat().getPlayerBans()
                        .matches(this);
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

    @Override
    public int getLoadingRadius() {
        return loadingRadius;
    }

    private void sendPluginChecksum(PluginFile plugin,
            WritableByteStream output) throws IOException {
        byte[] checksum = plugin.getChecksum();
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
        Vector3 pos3d = packet.getPosition();
        boolean flag = true;
        if (pos3d != null) {
            WorldServer world = entity.getWorld();
            if (world != null && !world.getTerrain()
                    .isBlockSendable(entity, pos3d.intX(), pos3d.intY(),
                            pos3d.intZ(), packet.isChunkContent())) {
                flag = false;
            }
            if (flag) {
                double range = packet.getRange();
                if (range > 0.0d) {
                    if (FastMath.pointDistanceSqr(pos3d, entity.getPos()) >
                            range * range) {
                        flag = false;
                    }
                }
            }
        }
        if (flag) {
            try {
                WritableByteStream output = channel.getOutputStream();
                output.putShort(packet.getID(
                        server.getServer().getWorldFormat().getPlugins()
                                .getRegistry()));
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
    public boolean tick(ServerConnection.NetWorkerThread worker) {
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
                        if (channel.bundleSize() > 102400) {
                            break;
                        }
                    }
                    channel.queueBundle();
                    Optional<ReadableByteStream> bundle = channel.fetch();
                    if (bundle.isPresent()) {
                        ReadableByteStream stream = bundle.get();
                        while (stream.hasRemaining()) {
                            PacketServer packet = (PacketServer) Packet
                                    .makePacket(registry, stream.getShort());
                            packet.parseServer(this, stream);
                            packet.runServer(this, entity.getWorld());
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

    public void updatePing(long ping) {
        this.ping = System.currentTimeMillis() - ping;
        pingTimeout = ping + 10000;
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
            WorldServer world = entity.getWorld();
            if (world != null) {
                world.deleteEntity(entity);
                world.removePlayer(entity);
                TagStructure tagStructure = new TagStructure();
                tagStructure.setStructure("Entity", entity.write(false));
                tagStructure.setString("World", world.getName());
                tagStructure.setList("Statistics", statistics.save());
                tagStructure.setInteger("Permissions", permissionLevel);
                server.getServer().getWorldFormat().getPlayerData()
                        .save(tagStructure, id);
            }
        }
        LOGGER.info("Client disconnected: {} ({})", id, nickname);
    }

    @Override
    public Optional<String> getPlayerName() {
        return Optional.of(nickname);
    }

    @Override
    public String getName() {
        return nickname;
    }

    @Override
    public void tell(String message) {
        LOGGER.info("Chat ({}): {}", nickname, message);
        send(new PacketChat(message));
    }

    @Override
    public int getPermissionLevel() {
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
