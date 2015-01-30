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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.swt.util.Dialogs;
import org.tobi29.scapes.server.controlpanel.RemoteControlPanel;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ControlPanelShell extends Shell {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ControlPanelShell.class);
    private final TabFolder tabFolder;
    private final BookmarkPanel bookmarkPanel;
    private final Map<TabItem, RemoteControlPanel> controlPanels =
            new ConcurrentHashMap<>();

    public ControlPanelShell(Display display, Collection<String> bookmarks) {
        super(display, SWT.SHELL_TRIM);
        setText("Scapes Control Panel");
        setMinimumSize(640, 480);
        setLayout(new FillLayout(SWT.HORIZONTAL));
        tabFolder = new TabFolder(this, SWT.NONE);

        TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
        bookmarkPanel = new BookmarkPanel(tabFolder, this, bookmarks);
        tabItem.setText("Bookmarks");
        tabItem.setToolTipText("View Bookmarks");
        tabItem.setControl(bookmarkPanel);

        Menu menu = new Menu(this, SWT.BAR);
        setMenuBar(menu);

        MenuItem connectionMenuItem = new MenuItem(menu, SWT.CASCADE);
        connectionMenuItem.setText("Connection");

        Menu connectionMenu = new Menu(connectionMenuItem);
        connectionMenuItem.setMenu(connectionMenu);

        MenuItem connectConnectTo = new MenuItem(connectionMenu, SWT.NONE);
        connectConnectTo.setText("Connect to...");
        connectConnectTo.addListener(SWT.Selection, event -> {
            ConnectDialog dialog = new ConnectDialog(this);
            ConnectDialog.Output login = dialog.open();
            if (login != null) {
                addTab(login.address, login.password);
            }
        });

        MenuItem connectDisconnect = new MenuItem(connectionMenu, SWT.NONE);
        connectDisconnect.setText("Disconnect");
        connectDisconnect.addListener(SWT.Selection, event -> removeTab());

        MenuItem connectBookmark = new MenuItem(connectionMenu, SWT.NONE);
        connectBookmark.setText("Bookmark");
        connectBookmark.addListener(SWT.Selection, event -> bookmarkTab());
    }

    public void addTab(String address, String password) {
        TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
        ServerPanel serverPanel = new ServerPanel(tabFolder);
        try {
            RemoteControlPanel controlPanel =
                    new RemoteControlPanel(address, password, tabItem,
                            serverPanel);
            tabItem.setText(controlPanel.getName());
            tabItem.setToolTipText(controlPanel.getTip());
            controlPanels.put(tabItem, controlPanel);
        } catch (IOException e) {
            LOGGER.warn("Failed to connect to server", e);
            Dialogs.openMessage(this,
                    "Failed to connect to server:\n" + e.getMessage(),
                    SWT.ICON_WARNING | SWT.OK);
            tabItem.dispose();
            serverPanel.dispose();
            return;
        }
        tabItem.setControl(serverPanel);
        tabFolder.setSelection(tabItem);
        layout();
    }

    public void removeTab() {
        for (TabItem tabItem : tabFolder.getSelection()) {
            RemoteControlPanel tab = controlPanels.get(tabItem);
            if (tab != null) {
                tab.close();
            }
        }
    }

    public void bookmarkTab() {
        for (TabItem tabItem : tabFolder.getSelection()) {
            RemoteControlPanel tab = controlPanels.get(tabItem);
            if (tab != null) {
                bookmarkPanel.addBookmark(tab.getAddress());
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        controlPanels.values().forEach(RemoteControlPanel::close);
    }

    @Override
    protected void checkSubclass() {
    }
}
