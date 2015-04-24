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

package org.tobi29.scapes.server.controlpanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.utils.io.PacketBundleChannel;
import org.tobi29.scapes.engine.utils.math.FastMath;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ControlPanelProtocol {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ControlPanelProtocol.class);
    private static final int AES_KEY_LENGTH;
    private final PacketBundleChannel channel;
    private final Queue<String[]> queue = new ConcurrentLinkedQueue<>();
    private final Map<String, PacketListener> commands =
            new ConcurrentHashMap<>();
    private final String password;
    private final Optional<KeyPair> keyPair;
    private State state;

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

    public ControlPanelProtocol(SocketChannel channel, String password,
            Optional<KeyPair> keyPair) throws IOException {
        this.channel = new PacketBundleChannel(channel, null, null);
        this.password = password;
        this.keyPair = keyPair;
        if (keyPair.isPresent()) {
            state = State.SERVER_LOGIN_STEP_1;
            DataOutputStream streamOut = this.channel.getOutputStream();
            byte[] array = keyPair.get().getPublic().getEncoded();
            streamOut.writeInt(array.length);
            streamOut.write(array);
            streamOut.writeInt(AES_KEY_LENGTH);
            this.channel.queueBundle();
        } else {
            state = State.CLIENT_LOGIN_STEP_1;
        }
    }

    public void send(String... command) {
        if ("connection".equals(command[0])) {
            if (command.length > 1) {
                switch (command[1]) {
                    case "end":
                        queue.add(new String[]{"connection", "end"});
                        break;
                    case "ping":
                        queue.add(new String[]{"connection", "ping",
                                String.valueOf(System.currentTimeMillis())});
                        break;
                }
            }
        } else {
            queue.add(command);
        }
    }

    public boolean process() throws IOException {
        boolean processing = false;
        Optional<DataInputStream> bundle = channel.fetch();
        if (bundle.isPresent()) {
            DataInputStream streamIn = bundle.get();
            switch (state) {
                case CLIENT_LOGIN_STEP_1:
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
                        streamOut.writeUTF(password);
                        channel.queueBundle();
                    } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidKeySpecException e) {
                        throw new IOException(e);
                    }
                    state = State.OPEN;
                    break;
                case SERVER_LOGIN_STEP_1:
                    int keyLength = streamIn.readInt();
                    keyLength = FastMath.min(keyLength, AES_KEY_LENGTH);
                    byte[] keyServer = new byte[keyLength];
                    byte[] keyClient = new byte[keyLength];
                    try {
                        assert keyPair.isPresent();
                        Cipher cipher = Cipher.getInstance("RSA");
                        cipher.init(Cipher.DECRYPT_MODE,
                                keyPair.get().getPrivate());
                        byte[] array =
                                new byte[cipher.getOutputSize(keyLength << 1)];
                        streamIn.readFully(array);
                        array = cipher.doFinal(array);
                        System.arraycopy(array, 0, keyServer, 0, keyLength);
                        System.arraycopy(array, keyLength, keyClient, 0,
                                keyLength);
                    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                        throw new IOException(e);
                    }
                    channel.setKey(keyServer, keyClient);
                    state = State.SERVER_LOGIN_STEP_2;
                    break;
                case SERVER_LOGIN_STEP_2:
                    String password = streamIn.readUTF();
                    if (!this.password.equals(password)) {
                        throw new IOException("Failed password authentication");
                    }
                    state = State.OPEN;
                    break;
                case OPEN:
                    int length = streamIn.readInt();
                    String[] command = new String[length];
                    for (int i = 0; i < length; i++) {
                        command[i] = streamIn.readUTF();
                    }
                    processCommand(command);
                    processing = true;
                    break;
            }
        }
        switch (state) {
            case OPEN:
                while (!queue.isEmpty()) {
                    String[] command = queue.poll();
                    DataOutputStream streamOut = channel.getOutputStream();
                    streamOut.writeInt(command.length);
                    for (String str : command) {
                        streamOut.writeUTF(str);
                    }
                    channel.queueBundle();
                    processing = true;
                }
                break;
        }
        return channel.process() || processing;
    }

    private void processCommand(String[] command) throws IOException {
        if ("connection".equals(command[0])) {
            if (command.length > 1) {
                switch (command[1]) {
                    case "end":
                        throw new IOException("Connection end");
                    case "ping":
                        if (command.length > 2) {
                            queue.add(new String[]{"connection", "pong",
                                    command[2]});
                        }
                        break;
                    case "pong":
                        if (command.length > 2) {
                            try {
                                PacketListener packetListener =
                                        commands.get("ping");
                                packetListener.receive(String.valueOf(
                                        System.currentTimeMillis() -
                                                Long.valueOf(command[2])));
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Failed decoding ping command: {}",
                                        e.toString());
                            }
                        }
                        break;
                }
            }
        } else {
            PacketListener packetListener = commands.get(command[0]);
            if (packetListener == null) {
                LOGGER.warn("Unknown command: {}", command[0]);
            } else {
                String[] args = new String[command.length - 1];
                System.arraycopy(command, 1, args, 0, args.length);
                packetListener.receive(args);
            }
        }
    }

    public void addCommand(String command, PacketListener packetListener) {
        commands.put(command, packetListener);
    }

    public void register(Selector selector, int opt) throws IOException {
        channel.register(selector, opt);
    }

    public void close() throws IOException {
        channel.close();
    }

    @FunctionalInterface
    public interface PacketListener {
        void receive(String... command);
    }

    enum State {
        CLIENT_LOGIN_STEP_1,
        SERVER_LOGIN_STEP_1,
        SERVER_LOGIN_STEP_2,
        OPEN,
        CLOSED
    }
}
