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

package org.tobi29.scapes.client.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.client.states.GameStateLoadMP;
import org.tobi29.scapes.client.states.scenes.SceneMenu;
import org.tobi29.scapes.connection.ConnectionInfo;
import org.tobi29.scapes.connection.ConnectionType;
import org.tobi29.scapes.connection.ServerInfo;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.opengl.texture.TextureCustom;
import org.tobi29.scapes.engine.opengl.texture.TextureFilter;
import org.tobi29.scapes.engine.opengl.texture.TextureWrap;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.BufferCreatorDirect;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class GuiServerSelect extends Gui {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiServerSelect.class);
    private static final Pattern SPLIT_PATTERN = Pattern.compile(":");
    private static final byte[] CONNECTION_HEADER = ConnectionInfo.getHeader();
    private final List<TagStructure> servers = new ArrayList<>();
    private final List<Element> elements = new ArrayList<>();
    private final GuiComponentScrollPaneList scrollPane;
    private final GameState state;

    public GuiServerSelect(GameState state, Gui prev) {
        super(GuiAlignment.CENTER);
        this.state = state;
        TagStructure scapesTag =
                state.getEngine().getTagStructure().getStructure("Scapes");
        if (scapesTag.has("Servers")) {
            servers.addAll(scapesTag.getList("Servers"));
        }
        GuiComponentVisiblePane pane =
                new GuiComponentVisiblePane(200, 0, 400, 512);
        scrollPane = new GuiComponentScrollPaneList(16, 80, 368, 250, 70);
        GuiComponentTextButton create =
                new GuiComponentTextButton(112, 370, 176, 30, 18, "Add");
        create.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiAddServer(state, this));
        });
        GuiComponentTextButton back =
                new GuiComponentTextButton(112, 466, 176, 30, 18, "Back");
        back.addLeftClick(event -> {
            disposeServers();
            state.remove(this);
            state.add(prev);
        });
        pane.add(new GuiComponentText(16, 16, 32, "Multiplayer"));
        pane.add(new GuiComponentSeparator(24, 64, 352, 2));
        pane.add(scrollPane);
        pane.add(create);
        pane.add(new GuiComponentSeparator(24, 448, 352, 2));
        pane.add(back);
        add(pane);
        updateServers();
    }

    private static String getError(Throwable e) {
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
        int i = 0;
        for (TagStructure tagStructure : servers) {
            Element element = new Element(tagStructure.getString("IP"), i);
            elements.add(element);
            scrollPane.add(element);
            i++;
        }
    }

    @Override
    public void update(double mouseX, double mouseY, boolean mouseInside,
            ScapesEngine engine) {
        super.update(mouseX, mouseY, mouseInside, engine);
        elements.forEach(Element::checkConnection);
    }

    public void addServer(TagStructure server) {
        servers.add(server);
        TagStructure scapesTag =
                state.getEngine().getTagStructure().getStructure("Scapes");
        scapesTag.setList("Servers", servers);
    }

    private class Element extends GuiComponentPane {
        private final GuiComponentTextButton label;
        private final ByteBuffer outBuffer, headerBuffer =
                BufferCreator.byteBuffer(4);
        private Texture texture;
        private SocketChannel channel;
        private int readState;
        private ByteBuffer buffer;

        public Element(String address, int i) {
            super(0, 70, 378, 70);
            String[] ipSplit = SPLIT_PATTERN.split(address, 2);
            int port;
            if (ipSplit.length > 1) {
                port = Integer.valueOf(ipSplit[1]);
            } else {
                port = 12345;
            }
            InetSocketAddress socketAddress =
                    new InetSocketAddress(ipSplit[0], port);
            label = new GuiComponentTextButton(70, 20, 200, 30, 18,
                    "Pinging...");
            label.addLeftClick(event -> state.getEngine().setState(
                    new GameStateLoadMP(socketAddress, state.getEngine(),
                            (SceneMenu) state.getScene())));
            GuiComponentTextButton delete =
                    new GuiComponentTextButton(280, 20, 60, 30, 18, "Delete");
            delete.addLeftClick(event -> {
                servers.remove(i);
                TagStructure scapesTag = state.getEngine().getTagStructure()
                        .getStructure("Scapes");
                scapesTag.setList("Servers", servers);
                elements.remove(this);
                scrollPane.remove(this);
            });
            add(label);
            add(delete);
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
                label.setText(getError(e));
            }
            outBuffer = BufferCreator.byteBuffer(CONNECTION_HEADER.length + 1);
            outBuffer.put(CONNECTION_HEADER);
            outBuffer.put(ConnectionType.GET_INFO.getData());
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
                                    .byteBuffer(4 + headerBuffer.getInt(0));
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
                            ByteBuffer imageBuffer = image.getBuffer();
                            ByteBuffer buffer = BufferCreatorDirect
                                    .byteBuffer(imageBuffer.remaining());
                            buffer.put(imageBuffer);
                            buffer.rewind();
                            texture = new TextureCustom(image.getWidth(),
                                    image.getHeight(), buffer, 0,
                                    TextureFilter.NEAREST,
                                    TextureFilter.NEAREST, TextureWrap.CLAMP,
                                    TextureWrap.CLAMP);
                            add(new GuiComponentIcon(15, 15, 40, 40, texture));
                            readState = -1;
                        } else if (read == -1) {
                            readState = -1;
                        }
                        break;
                }
            } catch (IOException e) {
                LOGGER.info("Failed to fetch server info: {}", e.toString());
                readState = -1;
                label.setText(getError(e));
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
