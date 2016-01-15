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

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.server.AbstractServerConnection;
import org.tobi29.scapes.engine.server.ConnectionCloseException;
import org.tobi29.scapes.engine.server.InvalidPacketDataException;
import org.tobi29.scapes.engine.server.PacketBundleChannel;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.io.*;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.skin.ServerSkin;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.packets.PacketClient;
import org.tobi29.scapes.packets.PacketPingServer;
import org.tobi29.scapes.packets.PacketServer;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.plugins.Plugins;
import org.tobi29.scapes.server.MessageLevel;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class RemotePlayerConnection extends PlayerConnection {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RemotePlayerConnection.class);
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

    private final PacketBundleChannel channel;
    private final Queue<IORunnable> sendQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger sendQueueSize = new AtomicInteger();
    private State state = State.LOGIN_STEP_1;
    private byte[] challenge;
    private long pingTimeout, pingWait;

    public RemotePlayerConnection(PacketBundleChannel channel,
            ServerConnection server) throws IOException {
        super(server);
        this.channel = channel;
        loginStep0();
    }

    @Override
    public void send(Packet packet) {
        if (sendQueueSize.get() > 128) {
            if (!packet.isVital()) {
                return;
            }
        }
        task(() -> sendPacket(packet));
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
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
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
            PublicKey key = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(array));
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            output.put(cipher.doFinal(challenge));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidKeySpecException e) {
            throw new IOException(e);
        }
        Plugins plugins = server.plugins();
        output.putInt(plugins.fileCount());
        Iterator<PluginFile> iterator = plugins.files().iterator();
        while (iterator.hasNext()) {
            sendPluginMetaData(iterator.next(), output);
        }
        channel.queueBundle();
        state = State.LOGIN_STEP_3;
    }

    private void loginStep3(ReadableByteStream input) throws IOException {
        Plugins plugins = server.plugins();
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
        Optional<String> response = server.addPlayer(this);
        if (response.isPresent()) {
            output.putBoolean(true);
            output.putString(response.get());
            channel.queueBundle();
            throw new ConnectionCloseException(response.get());
        }
        added = true;
        setWorld();
        output.putBoolean(false);
        output.putInt(loadingRadius);
        TagStructureBinary
                .write(server.server().plugins().registry().idStorage().save(),
                        output);
        channel.queueBundle();
        long currentTime = System.currentTimeMillis();
        pingWait = currentTime + 1000;
        pingTimeout = currentTime + 10000;
        server.message("Player connected: " + id + " (" + nickname + ") on " +
                channel, MessageLevel.SERVER_INFO);
        state = State.OPEN;
    }

    @Override
    protected void task(IORunnable runnable) {
        sendQueueSize.incrementAndGet();
        sendQueue.add(runnable);
    }

    @Override
    protected void transmit(Packet packet) throws IOException {
        WritableByteStream output = channel.getOutputStream();
        output.putShort(packet.id(server.plugins().registry()));
        ((PacketClient) packet).sendClient(this, output);
    }

    @Override
    public synchronized void close() throws IOException {
        super.close();
        channel.close();
        state = State.CLOSED;
    }

    public void updatePing(long ping) {
        pingTimeout = ping + 10000;
    }

    private void sendPluginMetaData(PluginFile plugin,
            WritableByteStream output) throws IOException {
        byte[] checksum = plugin.checksum().array();
        output.putString(plugin.name());
        output.putString(plugin.version().toString());
        output.putString(plugin.scapesVersion().toString());
        output.putByteArray(checksum);
    }

    private void sendPlugin(FilePath path, WritableByteStream output)
            throws IOException {
        FileUtil.read(path, stream -> ProcessStream.process(stream, buffer -> {
            output.putBoolean(false);
            output.put(buffer);
            channel.queueBundle();
        }, 1 << 10 << 10));
        output.putBoolean(true);
        channel.queueBundle();
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
                        sendQueue.poll().run();
                        sendQueueSize.decrementAndGet();
                        if (channel.bundleSize() > 1 << 10 << 4) {
                            break;
                        }
                    }
                    if (channel.bundleSize() > 0) {
                        channel.queueBundle();
                    }
                    Optional<RandomReadableByteStream> bundle = channel.fetch();
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
            server.message("Player disconnected: " + nickname + " (" + e +
                    ')', MessageLevel.SERVER_INFO);
            state = State.CLOSED;
        }
        return false;
    }

    @Override
    public boolean isClosed() {
        return state == State.CLOSED;
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
