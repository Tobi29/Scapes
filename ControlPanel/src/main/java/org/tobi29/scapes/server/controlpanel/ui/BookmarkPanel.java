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

package org.tobi29.scapes.server.controlpanel.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.server.controlpanel.ServerInfo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.regex.Pattern;

public class BookmarkPanel extends ScrolledComposite {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(BookmarkPanel.class);
    private static final Pattern SPLIT_PATTERN = Pattern.compile(":");
    private static final byte[] CONNECTION_HEADER =
            {'S', 'c', 'a', 'p', 'e', 's'};
    private final ControlPanelShell shell;
    private final Collection<String> bookmarks;
    private final Composite composite;

    public BookmarkPanel(Composite parent, ControlPanelShell shell,
            Collection<String> bookmarks) {
        super(parent, SWT.V_SCROLL);
        this.shell = shell;
        this.bookmarks = bookmarks;
        setExpandHorizontal(true);
        setExpandVertical(true);
        composite = new Composite(this, SWT.NONE);
        setContent(composite);
        composite.setLayout(new GridLayout(4, false));
        bookmarks.forEach(Bookmark::new);
        setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    private static String getError(Throwable e) {
        String message = e.getMessage();
        if (message != null) {
            return message;
        }
        return e.getClass().getSimpleName();
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void addBookmark(String bookmark) {
        bookmarks.add(bookmark);
        new Bookmark(bookmark);
        setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    @Override
    protected void checkSubclass() {
    }

    public class Bookmark implements Runnable {
        private final ByteBuffer outBuffer, headerBuffer =
                BufferCreator.byteBuffer(4);
        private final Display display;
        private final Label icon, label;
        private final SocketChannel channel;
        private int readState;
        private ByteBuffer buffer;

        private Bookmark(String address) {
            display = getDisplay();
            String[] ipSplit = SPLIT_PATTERN.split(address, 2);
            int port;
            if (ipSplit.length > 1) {
                port = Integer.valueOf(ipSplit[1]);
            } else {
                port = 12345;
            }
            InetSocketAddress socketAddress =
                    new InetSocketAddress(ipSplit[0], port);
            outBuffer = BufferCreator.byteBuffer(CONNECTION_HEADER.length + 1);
            outBuffer.put(CONNECTION_HEADER);
            outBuffer.put((byte) 1);
            outBuffer.rewind();

            icon = new Label(composite, SWT.NONE);
            icon.setLayoutData(
                    new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));

            label = new Label(composite, SWT.NONE);
            label.setLayoutData(
                    new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
            label.setText("Connecting");
            label.setToolTipText(address);

            Button connect = new Button(composite, SWT.NONE);
            connect.setLayoutData(
                    new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
            connect.setText("Connect");
            connect.addListener(SWT.Selection, event -> {
                String password = new PasswordDialog(shell).open();
                if (password != null) {
                    shell.addTab(address, password);
                }
            });

            Button remove = new Button(composite, SWT.NONE);
            remove.setLayoutData(
                    new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
            remove.setText("Remove");
            remove.addListener(SWT.Selection, event -> {
                icon.dispose();
                label.dispose();
                connect.dispose();
                remove.dispose();
                bookmarks.remove(address);
                setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                layout(true, true);
            });

            SocketChannel channel;
            try {
                channel = SocketChannel.open();
                channel.configureBlocking(false);
                if (socketAddress.isUnresolved()) {
                    throw new IOException("Could not resolve address");
                }
                channel.connect(socketAddress);
                display.asyncExec(this);
            } catch (IOException e) {
                LOGGER.info("Failed connecting to server: {}", e.toString());
                label.setText(e.toString());
                channel = null;
            }
            this.channel = channel;
        }

        @Override
        public void run() {
            if (isDisposed() || label.isDisposed() ||
                    icon.isDisposed()) {
                return;
            }
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
                            org.tobi29.scapes.engine.utils.graphics.Image
                                    image = serverInfo.getImage();
                            ByteBuffer imageBuffer = image.getBuffer();
                            int length = image.getWidth() * image.getHeight();
                            byte[] color = new byte[length * 3];
                            byte[] alpha = new byte[length];
                            int c = 0, a = 0;
                            while (a < length) {
                                color[c++] = imageBuffer.get();
                                color[c++] = imageBuffer.get();
                                color[c++] = imageBuffer.get();
                                alpha[a++] = imageBuffer.get();
                            }
                            PaletteData paletteData =
                                    new PaletteData(0xFF0000, 0xFF00, 0xFF);
                            ImageData imageData =
                                    new ImageData(image.getWidth(),
                                            image.getHeight(), 24, paletteData,
                                            image.getWidth() * 3, color);
                            imageData.setAlphas(0, 0, length, alpha, 0);
                            Image rawImage = new Image(display, imageData);
                            Point dpi = display.getDPI();
                            int width =
                                    (int) FastMath.ceil(dpi.x / 96.0 * 32.0);
                            int height =
                                    (int) FastMath.ceil(dpi.y / 96.0 * 32.0);
                            Image scaledImage =
                                    new Image(display, width, height);
                            GC gc = new GC(scaledImage);
                            gc.drawImage(rawImage, 0, 0, image.getWidth(),
                                    image.getHeight(), 0, 0, width, height);
                            gc.dispose();
                            rawImage.dispose();
                            icon.setImage(scaledImage);
                            icon.addListener(SWT.Dispose,
                                    event -> scaledImage.dispose());
                            setMinSize(composite
                                    .computeSize(SWT.DEFAULT, SWT.DEFAULT));
                            layout(true, true);
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
                layout(true, true);
            }
            if (readState == -1) {
                readState = -2;
                try {
                    channel.close();
                } catch (IOException e) {
                    LOGGER.warn("Failed to close server info socket: {}",
                            e.toString());
                }
            } else {
                display.timerExec(20, this);
            }
        }
    }
}
