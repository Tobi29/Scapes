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
import org.tobi29.scapes.engine.utils.UnsupportedJVMException;
import org.tobi29.scapes.engine.utils.io.PacketBundleChannel;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ControlPanelProtocol {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ControlPanelProtocol.class);
    private final PacketBundleChannel channel;
    private final Queue<String[]> queue = new ConcurrentLinkedQueue<>();
    private final Map<String, PacketListener> commands =
            new ConcurrentHashMap<>();

    public ControlPanelProtocol(SocketChannel channel, String password,
            byte[] salt) {
        this(channel, password, salt, () -> {
        });
    }

    public ControlPanelProtocol(SocketChannel channel, String password,
            byte[] salt, PacketBundleChannel.ConnectListener connectListener) {
        SecretKey key;
        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec =
                    new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            key = factory.generateSecret(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new UnsupportedJVMException(e);
        }
        byte[] array = key.getEncoded();
        this.channel =
                new PacketBundleChannel(channel, array, array, connectListener);
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
                    case "gc":
                        queue.add(new String[]{"connection", "gc"});
                        break;
                }
            }
        } else {
            queue.add(command);
        }
    }

    @SuppressWarnings("CallToSystemGC")
    public boolean process() throws IOException {
        boolean processing = false;
        Optional<DataInputStream> bundle = channel.fetch();
        if (bundle.isPresent()) {
            DataInputStream streamIn = bundle.get();
            int length = streamIn.readInt();
            String[] command = new String[length];
            for (int i = 0; i < length; i++) {
                command[i] = streamIn.readUTF();
            }
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
                                    LOGGER.warn(
                                            "Failed decoding ping command: {}",
                                            e.toString());
                                }
                            }
                            break;
                        case "gc":
                            System.gc();
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
            processing = true;
        }
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
        return channel.process() || processing;
    }

    public void addCommand(String command, PacketListener packetListener) {
        commands.put(command, packetListener);
    }

    public void close() throws IOException {
        channel.close();
    }

    @FunctionalInterface
    public interface PacketListener {
        void receive(String... command);
    }
}
