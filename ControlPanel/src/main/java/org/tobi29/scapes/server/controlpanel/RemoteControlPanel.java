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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TabItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.swt.util.Dialogs;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.SleepUtil;
import org.tobi29.scapes.server.controlpanel.ui.MessageDialog;
import org.tobi29.scapes.server.controlpanel.ui.OperatorDialog;
import org.tobi29.scapes.server.controlpanel.ui.ServerPanel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class RemoteControlPanel implements Runnable {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RemoteControlPanel.class);
    private static final Pattern SPLIT_PATTERN = Pattern.compile(":");
    private static final byte[] SALT =
            "Scapes-Control-Panel".getBytes(StandardCharsets.UTF_8);
    private final String address, name, tip;
    private final TabItem tabItem;
    private final ServerPanel serverPanel;
    private final ControlPanelProtocol connection;

    public RemoteControlPanel(String address, String password, TabItem tabItem,
            ServerPanel serverPanel) throws IOException {
        this.address = address;
        this.tabItem = tabItem;
        this.serverPanel = serverPanel;
        String[] addressSplit = SPLIT_PATTERN.split(address, 2);
        String ip = addressSplit[0];
        int port;
        if (addressSplit.length > 1) {
            port = Integer.valueOf(addressSplit[1]);
        } else {
            port = 12345;
        }
        InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
        if (socketAddress.isUnresolved()) {
            throw new IOException("Could not resolve address");
        }
        SocketChannel channel = SocketChannel.open(socketAddress);
        channel.configureBlocking(false);
        while (!channel.finishConnect()) {
            SleepUtil.sleep(10);
        }
        name = socketAddress.getHostName();
        tip = socketAddress.getHostName() + '/' + socketAddress.getAddress();
        LOGGER.info("Opening connection: {}", name);
        ByteBuffer buffer = BufferCreator
                .wrap(new byte[]{'S', 'c', 'a', 'p', 'e', 's', 101});
        while (buffer.hasRemaining()) {
            int write = channel.write(buffer);
            if (write == -1) {
                throw new IOException("Connection closed");
            }
        }
        connection = new ControlPanelProtocol(channel, password, SALT);
        connection.addCommand("updateplayers", serverPanel::updatePlayers);
        connection.addCommand("updateworlds", serverPanel::updateWorlds);
        connection.addCommand("appendlog", serverPanel::appendLine);
        connection.addCommand("appendprofiler", command -> {
            Map<String, Double> tps =
                    new ConcurrentHashMap<>(command.length - 1);
            for (int i = 1; i < command.length; i += 2) {
                tps.put(command[i], Double.valueOf(command[i + 1]));
            }
            serverPanel.appendProfiler(Long.valueOf(command[0]), tps);
        });
        connection.addCommand("ping", serverPanel::appendLine);
        serverPanel.input.addListener(SWT.DefaultSelection, event -> {
            connection.send("scapescmd", serverPanel.input.getText());
            serverPanel.input.setText("");
        });
        serverPanel.playerKick.addListener(SWT.Selection, event -> {
            for (String player : serverPanel.players.getSelection()) {
                connection.send("scapescmd", "kick -p " + player);
            }
        });
        serverPanel.playerOP.addListener(SWT.Selection, event -> {
            for (String player : serverPanel.players.getSelection()) {
                int level = new OperatorDialog(serverPanel.getShell()).open();
                if (level >= 0) {
                    connection.send("scapescmd",
                            "op -p " + player + " -l " + level);
                }
            }
        });
        serverPanel.playerMessage.addListener(SWT.Selection, event -> {
            for (String player : serverPanel.players.getSelection()) {
                String message =
                        new MessageDialog(serverPanel.getShell()).open();
                if (message != null) {
                    connection.send("scapescmd",
                            "tell -t " + player + ' ' + message);
                }
            }
        });
        serverPanel.getDisplay().timerExec(100, this);
    }

    @Override
    public void run() {
        if (!serverPanel.isDisposed()) {
            try {
                if (!connection.process()) {
                    SleepUtil.sleep(10);
                }
            } catch (IOException e) {
                LOGGER.warn("Lost connection to server", e);
                Dialogs.openMessage(serverPanel.getShell(),
                        "Lost connection to server:\n" + e.getMessage(),
                        SWT.ICON_WARNING | SWT.OK);
                close();
            }
        }
        if (!serverPanel.isDisposed()) {
            serverPanel.getDisplay().timerExec(100, this);
        }
    }

    public void close() {
        LOGGER.info("Closing connection: {}", name);
        try {
            connection.close();
        } catch (IOException e) {
        }
        tabItem.dispose();
        serverPanel.dispose();
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public String getTip() {
        return tip;
    }
}
