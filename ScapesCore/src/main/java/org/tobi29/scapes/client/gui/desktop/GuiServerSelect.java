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
package org.tobi29.scapes.client.gui.desktop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.client.gui.GuiAddServer;
import org.tobi29.scapes.client.states.GameStateLoadMP;
import org.tobi29.scapes.client.states.scenes.SceneMenu;
import org.tobi29.scapes.connection.ConnectionType;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.opengl.texture.TextureCustom;
import org.tobi29.scapes.engine.opengl.texture.TextureFilter;
import org.tobi29.scapes.engine.opengl.texture.TextureWrap;
import org.tobi29.scapes.engine.server.ConnectionInfo;
import org.tobi29.scapes.engine.server.ServerInfo;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class GuiServerSelect extends GuiMenu {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiServerSelect.class);
    private static final byte[] CONNECTION_HEADER = ConnectionInfo.header();
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
        updateServers();
        GuiComponentTextButton add =
                pane.addVert(112, 5, 176, 30, p -> button(p, "Add"));

        add.onClickLeft(event -> {
            state.engine().guiStack()
                    .add("10-Menu", new GuiAddServer(state, this, style));
        });
        back.onClickLeft(event -> disposeServers());
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
            try {
                element.channel.close();
            } catch (IOException e) {
                LOGGER.warn("Failed to close server info socket: {}",
                        e.toString());
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
    public void updateComponent(ScapesEngine engine, double delta, Vector2 size) {
        Streams.of(elements).forEach(Element::checkConnection);
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
        private final ByteBuffer outBuffer, headerBuffer =
                BufferCreator.bytes(4);
        private SocketChannel channel;
        private int readState;
        private ByteBuffer buffer;

        public Element(GuiLayoutData parent, TagStructure tagStructure) {
            super(parent);
            icon = addHori(15, 15, 40, -1, GuiComponentIcon::new);
            label = addHori(5, 20, -1, -1, p -> button(p, "Pinging..."));
            GuiComponentTextButton delete =
                    addHori(5, 20, 80, -1, p -> button(p, "Delete"));

            String address = tagStructure.getString("Address");
            int port = tagStructure.getInteger("Port");
            InetSocketAddress socketAddress =
                    new InetSocketAddress(address, port);
            label.onClickLeft(event -> state.engine().setState(
                    new GameStateLoadMP(socketAddress, state.engine(),
                            (SceneMenu) state.scene())));
            delete.onClickLeft(event -> {
                servers.remove(tagStructure);
                TagStructure scapesTag =
                        state.engine().tagStructure().getStructure("Scapes");
                scapesTag.setList("Servers", servers);
                elements.remove(this);
                scrollPane.remove(this);
            });
            try {
                channel = SocketChannel.open();
                channel.configureBlocking(false);
                if (socketAddress.isUnresolved()) {
                    throw new IOException("Could not resolve address");
                }
                channel.connect(socketAddress);
            } catch (IOException e) {
                LOGGER.info("Failed connecting to server: {}", e.toString());
                readState = -1;
                label.setText(error(e));
            }
            outBuffer = BufferCreator.bytes(CONNECTION_HEADER.length + 1);
            outBuffer.put(CONNECTION_HEADER);
            outBuffer.put(ConnectionType.GET_INFO.data());
            outBuffer.rewind();
        }

        private void checkConnection() {
            try {
                switch (readState) {
                    case 0:
                        if (channel.finishConnect()) {
                            readState++;
                        }
                        break;
                    case 1:
                        int write = channel.write(outBuffer);
                        if (!outBuffer.hasRemaining()) {
                            readState++;
                        } else if (write == -1) {
                            readState = -1;
                        }
                        break;
                    case 2: {
                        int read = channel.read(headerBuffer);
                        if (!headerBuffer.hasRemaining()) {
                            buffer = BufferCreator
                                    .bytes(4 + headerBuffer.getInt(0));
                            headerBuffer.rewind();
                            buffer.put(headerBuffer);
                            readState++;
                        } else if (read == -1) {
                            readState = -1;
                        }
                        break;
                    }
                    case 3:
                        int read = channel.read(buffer);
                        if (!buffer.hasRemaining()) {
                            ServerInfo serverInfo = new ServerInfo(buffer);
                            label.setText(serverInfo.getName());
                            Image image = serverInfo.getImage();
                            ByteBuffer imageBuffer = image.buffer();
                            ByteBuffer buffer = BufferCreator
                                    .bytes(imageBuffer.remaining());
                            buffer.put(imageBuffer);
                            buffer.rewind();
                            Texture texture = new TextureCustom(image.width(),
                                    image.height(), buffer, 0,
                                    TextureFilter.NEAREST,
                                    TextureFilter.NEAREST, TextureWrap.CLAMP,
                                    TextureWrap.CLAMP);
                            icon.setIcon(texture);
                            readState = -1;
                        } else if (read == -1) {
                            readState = -1;
                        }
                        break;
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
