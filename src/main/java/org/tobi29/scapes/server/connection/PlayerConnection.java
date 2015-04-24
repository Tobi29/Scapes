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
import org.tobi29.scapes.engine.utils.io.ChecksumUtil;
import org.tobi29.scapes.engine.utils.io.PacketBundleChannel;
import org.tobi29.scapes.engine.utils.io.ProcessStream;
import org.tobi29.scapes.engine.utils.io.filesystem.File;
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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.Selector;
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

    private void loginStep1() throws IOException {
        DataOutputStream streamOut = channel.getOutputStream();
        KeyPair keyPair = server.getKeyPair();
        byte[] array = keyPair.getPublic().getEncoded();
        streamOut.writeInt(array.length);
        streamOut.write(array);
        streamOut.writeInt(AES_KEY_LENGTH);
        channel.queueBundle();
    }

    private void loginStep2(DataInputStream streamIn) throws IOException {
        int keyLength = streamIn.readInt();
        keyLength = FastMath.min(keyLength, AES_KEY_LENGTH);
        byte[] keyServer = new byte[keyLength];
        byte[] keyClient = new byte[keyLength];
        try {
            KeyPair keyPair = server.getKeyPair();
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] array = new byte[cipher.getOutputSize(keyLength << 1)];
            streamIn.readFully(array);
            array = cipher.doFinal(array);
            System.arraycopy(array, 0, keyServer, 0, keyLength);
            System.arraycopy(array, keyLength, keyClient, 0, keyLength);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            throw new IOException(e);
        }
        channel.setKey(keyServer, keyClient);
    }

    private void loginStep3(DataInputStream streamIn) throws IOException {
        byte[] array = new byte[550];
        streamIn.readFully(array);
        id = ChecksumUtil
                .getChecksum(array, ChecksumUtil.ChecksumAlgorithm.SHA1);
        challenge = new byte[501];
        new SecureRandom().nextBytes(challenge);
        DataOutputStream streamOut = channel.getOutputStream();
        try {
            key = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(array));
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            streamOut.write(cipher.doFinal(challenge));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidKeySpecException e) {
            throw new IOException(e);
        }
        Plugins plugins = server.getServer().getWorldFormat().getPlugins();
        streamOut.writeInt(plugins.getFileCount());
        plugins.getFiles()
                .forEach(plugin -> sendPluginChecksum(plugin, streamOut));
        TagStructureBinary
                .write(server.getServer().getWorldFormat().getIDStorage()
                        .save(), streamOut);
        channel.queueBundle();
    }

    private void loginStep4(DataInputStream streamIn)
            throws IOException, ConnectionCloseException {
        Plugins plugins = server.getServer().getWorldFormat().getPlugins();
        byte[] challenge = new byte[this.challenge.length];
        streamIn.readFully(challenge);
        nickname = streamIn.readUTF();
        loadingRadius = streamIn.readInt();
        byte[] array = new byte[64 * 64 * 4];
        streamIn.readFully(array);
        skin = new ServerSkin(array);
        int length = streamIn.readInt();
        List<Integer> requests = new ArrayList<>(length);
        while (length-- > 0) {
            requests.add(streamIn.readInt());
        }
        DataOutputStream streamOut = channel.getOutputStream();
        Optional<String> response =
                generateResponse(Arrays.equals(challenge, this.challenge));
        if (!response.isPresent()) {
            response = start();
        }
        if (response.isPresent()) {
            streamOut.writeBoolean(true);
            streamOut.writeUTF(response.get());
            streamOut.flush();
            channel.queueBundle();
            throw new ConnectionCloseException(response.get());
        } else {
            streamOut.writeBoolean(false);
            for (int request : requests) {
                sendPlugin(plugins.getFile(request).getFile(), streamOut);
            }
            channel.queueBundle();
            LOGGER.info("Client accepted: {} ({}) on {}", id, nickname,
                    channel.toString());
        }
    }

    private Optional<String> start() {
        WorldFormat worldFormat = server.getServer().getWorldFormat();
        TagStructure tagStructure = worldFormat.getPlayerData().load(id);
        WorldServer world =
                worldFormat.getWorld(tagStructure.getString("World"));
        if (tagStructure.has("Entity")) {
            entity = new MobPlayerServer(world, Vector3d.ZERO, Vector3d.ZERO,
                    0.0d, 0.0d, nickname, skin.getChecksum(), this);
            entity.read(tagStructure.getStructure("Entity"));
        } else {
            entity = new MobPlayerServer(world,
                    new Vector3d(0.5, 0.5, 1.0d).plus(world.getSpawn()),
                    Vector3d.ZERO, 0.0d, 0.0d, nickname, skin.getChecksum(),
                    this);
            entity.onSpawn();
        }
        statistics = new PlayerStatistics();
        if (tagStructure.has("Statistics")) {
            statistics.load(world.getRegistry(),
                    tagStructure.getList("Statistics"));
        }
        permissionLevel = tagStructure.getInteger("Permissions");
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
            DataOutputStream streamOut) {
        try {
            byte[] checksum = plugin.getChecksum();
            streamOut.writeInt(checksum.length);
            streamOut.write(checksum);
        } catch (IOException e) {
            LOGGER.error("Failed to update plugin:", e);
        }
    }

    private void sendPlugin(File file, DataOutputStream streamOut)
            throws IOException {
        try (InputStream streamIn = file.read()) {
            streamOut.writeInt(streamIn.available());
            ProcessStream.process(streamIn, streamOut::write);
        }
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
                DataOutputStream streamOut = channel.getOutputStream();
                streamOut.writeShort(packet.getID(
                        server.getServer().getWorldFormat().getPlugins()
                                .getRegistry()));
                ((PacketClient) packet).sendClient(this, streamOut);
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
                    Optional<DataInputStream> bundle = channel.fetch();
                    if (bundle.isPresent()) {
                        loginStep2(bundle.get());
                        state = State.LOGIN_STEP_2;
                    }
                    return channel.process();
                }
                case LOGIN_STEP_2: {
                    Optional<DataInputStream> bundle = channel.fetch();
                    if (bundle.isPresent()) {
                        loginStep3(bundle.get());
                        state = State.LOGIN_STEP_3;
                    }
                    return channel.process();
                }
                case LOGIN_STEP_3: {
                    Optional<DataInputStream> bundle = channel.fetch();
                    if (bundle.isPresent()) {
                        loginStep4(bundle.get());
                    }
                    return channel.process();
                }
                case OPEN:
                    while (!sendQueue.isEmpty()) {
                        Packet packet = sendQueue.poll();
                        sendPacket(packet);
                        sendQueueSize.decrementAndGet();
                        if (channel.bundleSize() > 102400) {
                            break;
                        }
                    }
                    channel.queueBundle();
                    Optional<DataInputStream> bundle = channel.fetch();
                    if (bundle.isPresent()) {
                        DataInputStream streamIn = bundle.get();
                        while (streamIn.available() > 0) {
                            PacketServer packet = (PacketServer) Packet
                                    .makePacket(registry, streamIn.readShort());
                            packet.parseServer(this, streamIn);
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
