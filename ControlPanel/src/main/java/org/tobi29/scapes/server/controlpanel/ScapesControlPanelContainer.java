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

import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.server.controlpanel.ui.ControlPanelShell;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ScapesControlPanelContainer {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ScapesControlPanelContainer.class);

    public void run() {
        Preferences node =
                Preferences.userRoot().node("Scapes").node("ControlPanel");
        List<String> bookmarks = new ArrayList<>();
        try {
            String[] children = node.keys();
            for (String child : children) {
                bookmarks.add(node.get(child, ""));
            }
        } catch (BackingStoreException e) {
            LOGGER.warn("Failed to load bookmarks", e);
        }
        Display.setAppName("Scapes Control Panel");
        Display.setAppVersion("0.0.0_1");
        Display display = Display.getDefault();
        ControlPanelShell shell = new ControlPanelShell(display, bookmarks);
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
        try {
            node.clear();
            for (int i = 0; i < bookmarks.size(); i++) {
                node.put(String.valueOf(i), bookmarks.get(i));
            }
        } catch (BackingStoreException e) {
            LOGGER.warn("Failed to store bookmarks", e);
        }
    }
}
