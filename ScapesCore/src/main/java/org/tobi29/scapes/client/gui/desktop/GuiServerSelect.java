/*
 * Copyright 2012-2016 Tobi29
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

package org.tobi29.scapes.client.gui.desktop;

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.client.states.GameStateLoadMP;
import org.tobi29.scapes.connection.ConnectionType;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.graphics.Texture;
import org.tobi29.scapes.engine.graphics.TextureFilter;
import org.tobi29.scapes.engine.graphics.TextureWrap;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.server.*;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.io.RandomReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class GuiServerSelect extends GuiMenu {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiServerSelect.class);
    private final List<TagStructure> servers = new ArrayList<>();
    private final List<Element> elements = new ArrayList<>();
    private final GuiComponentScrollPaneViewport scrollPane;

    public GuiServerSelect(GameState state, Gui previous, GuiStyle style) {
        super(state, "Multiplayer", previous, style);
        TagStructure scapesTag =
                state.engine().tagStructure().getStructure("Scapes");
        if (scapesTag.has("Servers")) {
            servers.addAll(scapesTag.getList("Servers"));
        }
        scrollPane = pane.addVert(16, 5, 368, 340,
                p -> new GuiComponentScrollPane(p, 70)).viewport();
        GuiComponentTextButton add =
                pane.addVert(112, 5, 176, 30, p -> button(p, "Add"));
        updateServers();

        selection(-1, add);

        add.on(GuiEvent.CLICK_LEFT, event -> state.engine().guiStack()
                .add("10-Menu", new GuiAddServer(state, this, style)));
        on(GuiAction.BACK, this::disposeServers);
    }

    private static String error(Throwable e) {
        String message = e.getMessage();
        if (message != null) {
            return message;
        }
        return e.getClass().getSimpleName();
    }

    private void disposeServers() {
        for (Element element : elements) {
            if (element.channel != null) {
                try {
                    element.channel.close();
                } catch (IOException e) {
                    LOGGER.warn("Failed to close server info socket: {}",
                            e.toString());
                }
            }
            scrollPane.remove(element);
        }
        elements.clear();
    }

    public void updateServers() {
        disposeServers();
        for (TagStructure tagStructure : servers) {
            Element element = scrollPane
                    .addVert(0, 0, -1, 70, p -> new Element(p, tagStructure));
            elements.add(element);
        }
    }

    @Override
    public void updateComponent(ScapesEngine engine, double delta) {
        Streams.forEach(elements, Element::checkConnection);
    }

    public void addServer(TagStructure server) {
        servers.add(server);
        TagStructure scapesTag =
                state.engine().tagStructure().getStructure("Scapes");
        scapesTag.setList("Servers", servers);
    }

    private class Element extends GuiComponentGroupSlab {
        private final GuiComponentIcon icon;
        private final GuiComponentTextButton label;
        private final RemoteAddress address;
        private SocketChannel channel;
        private PacketBundleChannel bundleChannel;
        private int readState;

        public Element(GuiLayoutData parent, TagStructure tagStructure) {
            super(parent);
            icon = addHori(15, 15, 40, -1, GuiComponentIcon::new);
            label = addHori(5, 20, -1, -1, p -> button(p, "Pinging..."));
            GuiComponentTextButton delete =
                    addHori(5, 20, 80, -1, p -> button(p, "Delete"));

            selection(label, delete);

            address = new RemoteAddress(tagStructure);
            label.on(GuiEvent.CLICK_LEFT, event -> state.engine().setState(
                    new GameStateLoadMP(address, state.engine(),
                            state.scene())));
            delete.on(GuiEvent.CLICK_LEFT, event -> {
                servers.remove(tagStructure);
                TagStructure scapesTag =
                        state.engine().tagStructure().getStructure("Scapes");
                scapesTag.setList("Servers", servers);
                elements.remove(this);
                scrollPane.remove(this);
            });
        }

        private void checkConnection() {
            try {
                switch (readState) {
                    case 0:
                        Optional<InetSocketAddress> socketAddress =
                                AddressResolver.resolve(address,
                                        state.engine().taskExecutor());
                        if (socketAddress.isPresent()) {
                            channel = SocketChannel.open();
                            channel.configureBlocking(false);
                            channel.connect(socketAddress.get());
                            readState++;
                        }
                        break;
                    case 1:
                        if (channel.finishConnect()) {
                            // Ignore invalid certificates because worst case
                            // server name and icon get faked
                            SSLHandle ssl =
                                    SSLProvider.sslHandle(certificates -> true);
                            bundleChannel =
                                    new PacketBundleChannel(address, channel,
                                            state.engine().taskExecutor(), ssl,
                                            true);
                            WritableByteStream output =
                                    bundleChannel.getOutputStream();
                            output.put(new byte[]{'S', 'c', 'a', 'p', 'e', 's',
                                    ConnectionType.GET_INFO.data()});
                            bundleChannel.queueBundle();
                            readState++;
                        }
                        break;
                    case 2:
                        if (bundleChannel.process()) {
                            throw new IOException("Disconnected");
                        }
                        Optional<RandomReadableByteStream> bundle =
                                bundleChannel.fetch();
                        if (bundle.isPresent()) {
                            RandomReadableByteStream input = bundle.get();
                            ByteBuffer infoBuffer =
                                    BufferCreator.bytes(input.remaining());
                            input.get(infoBuffer);
                            infoBuffer.flip();
                            ServerInfo serverInfo = new ServerInfo(infoBuffer);
                            label.setText(serverInfo.getName());
                            Image image = serverInfo.getImage();
                            Texture texture = state.engine().graphics()
                                    .createTexture(image, 0,
                                            TextureFilter.NEAREST,
                                            TextureFilter.NEAREST,
                                            TextureWrap.CLAMP,
                                            TextureWrap.CLAMP);
                            icon.setIcon(texture);
                            bundleChannel.requestClose();
                            readState++;
                        }
                        break;
                    case 3:
                        if (bundleChannel.process()) {
                            readState = -1;
                        }
                }
            } catch (IOException e) {
                LOGGER.info("Failed to fetch server info: {}", e.toString());
                readState = -1;
                label.setText(error(e));
            }
            if (readState == -1) {
                readState = -2;
                try {
                    channel.close();
                } catch (IOException e) {
                    LOGGER.warn("Failed to close server info socket: {}",
                            e.toString());
                }
            }
        }
    }
}
