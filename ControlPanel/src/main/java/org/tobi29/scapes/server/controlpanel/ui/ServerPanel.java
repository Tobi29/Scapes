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
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.tobi29.scapes.engine.swt.util.ANSILineStyler;
import org.tobi29.scapes.engine.swt.util.Fonts;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerPanel extends Composite {
    public final Text input;
    public final MenuItem playerKick, playerOP, playerMessage;
    public final List players;
    public final ProfilerGraph ram;
    public final Map<String, ProfilerGraph> worlds = new ConcurrentHashMap<>();
    private final StyledText console;
    private final Font font;
    private final Group profilerGroup;

    public ServerPanel(Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(3, false));
        Display display = getDisplay();
        Group playersGroup = new Group(this, SWT.NONE);
        playersGroup.setLayout(new GridLayout(1, false));
        GridData playersGrid =
                new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1);
        playersGrid.widthHint = 96;
        playersGroup.setLayoutData(playersGrid);
        playersGroup.setText("Players");
        players = new List(playersGroup, SWT.BORDER);
        players.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        players.setSize(100, 100);
        Group consoleGroup = new Group(this, SWT.NONE);
        consoleGroup.setLayout(new GridLayout(1, false));
        consoleGroup.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        consoleGroup.setText("Console");
        console = new StyledText(consoleGroup,
                SWT.BORDER | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL |
                        SWT.MULTI);
        font = new Font(display, Fonts.getMonospacedFont());
        console.setFont(font);
        console.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        ANSILineStyler consoleStyler = new ANSILineStyler(console);
        console.addLineStyleListener(consoleStyler);
        console.addVerifyListener(consoleStyler);
        input = new Text(consoleGroup, SWT.BORDER);
        input.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        profilerGroup = new Group(this, SWT.NONE);
        GridLayout profilerLayout = new GridLayout(1, false);
        profilerLayout.marginHeight = 0;
        profilerLayout.marginWidth = 0;
        profilerGroup.setLayout(profilerLayout);
        GridData profilerGrid =
                new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1);
        profilerGrid.widthHint = 128;
        profilerGroup.setLayoutData(profilerGrid);
        profilerGroup.setText("Profiler");
        ram = new ProfilerGraph(profilerGroup, 4096.0d * 1024.0d * 1024.0d,
                1.0d, value -> "RAM: " + (int) (value / 1048576.0d) + "MB");
        ram.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        Menu menu = new Menu(players);
        players.setMenu(menu);
        playerKick = new MenuItem(menu, SWT.NONE);
        playerKick.setText("Kick");
        playerOP = new MenuItem(menu, SWT.NONE);
        playerOP.setText("Operator");
        playerMessage = new MenuItem(menu, SWT.NONE);
        playerMessage.setText("Message");
    }

    public void updatePlayers(String... array) {
        if (isDisposed()) {
            return;
        }
        int index = players.getSelectionIndex();
        players.deselectAll();
        players.setItems(array);
        players.select(index);
    }

    public void updateWorlds(String... array) {
        if (isDisposed()) {
            return;
        }
        worlds.values().forEach(ProfilerGraph::dispose);
        worlds.clear();
        for (String world : array) {
            ProfilerGraph graph = new ProfilerGraph(profilerGroup, 1.0d, 0.5d,
                    value -> world +
                            ' ' + (int) (value * 100.0d) + '%');
            graph.setLayoutData(
                    new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
            worlds.put(world, graph);
        }
        profilerGroup.layout();
    }

    public void appendProfiler(long ram, Map<String, Double> tps) {
        if (isDisposed()) {
            return;
        }
        this.ram.addStamp(ram);
        tps.entrySet().forEach(entry -> {
            String name = entry.getKey();
            if (worlds.containsKey(name)) {
                worlds.get(name).addStamp(entry.getValue());
            }
        });
    }

    public void appendLine(String... line) {
        for (String str : line) {
            appendLine(str);
        }
    }

    public void appendLine(String line) {
        if (isDisposed()) {
            return;
        }
        console.append(line + '\n');
        console.setTopIndex(console.getLineCount() - 1);
    }

    @Override
    protected void checkSubclass() {
    }

    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
    }
}
