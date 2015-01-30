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

package org.tobi29.scapes.server.shell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.tobi29.scapes.Scapes;
import org.tobi29.scapes.engine.swt.util.Dialogs;
import org.tobi29.scapes.engine.utils.io.filesystem.Directory;
import org.tobi29.scapes.engine.utils.io.filesystem.FileSystem;
import org.tobi29.scapes.engine.utils.io.filesystem.FileSystemContainer;
import org.tobi29.scapes.server.ControlPanel;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.controlpanel.ui.MessageDialog;
import org.tobi29.scapes.server.controlpanel.ui.OperatorDialog;
import org.tobi29.scapes.server.controlpanel.ui.ServerPanel;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class ScapesServerShell extends ScapesStandaloneServer
        implements ControlPanel {
    public final Shell shell;
    public final Display display;
    private final ServerPanel serverPanel;

    public ScapesServerShell() throws IOException {
        super(FileSystemContainer.newFileSystem(Scapes.directory, ""));
        display = Display.getDefault();
        shell = new Shell(display);
        shell.setText("Scapes Server");
        shell.setMinimumSize(640, 480);
        shell.setLayout(new FillLayout());
        serverPanel = new ServerPanel(shell);
        serverPanel.input.addListener(SWT.DefaultSelection, event -> {
            String line = serverPanel.input.getText();
            ScapesServer server = this.server;
            if (server != null) {
                server.getCommandRegistry().get(line, this).execute()
                        .forEach(output -> appendLog(output.toString()));
            }
            serverPanel.input.setText("");
        });
        shell.addDisposeListener(
                event -> server.stop(ScapesServer.ShutdownReason.STOP));
        shell.open();
        display.timerExec(100, new Runnable() {
            @Override
            public void run() {
                if (!serverPanel.isDisposed()) {
                    serverPanel.ram.canvas.redraw();
                    serverPanel.worlds.values()
                            .forEach(profiler -> profiler.canvas.redraw());
                    display.timerExec(100, this);
                }
            }
        });
        serverPanel.playerKick.addListener(SWT.Selection, event -> {
            ScapesServer server = this.server;
            if (server != null) {
                for (String player : serverPanel.players.getSelection()) {
                    server.getCommandRegistry().get("kick -p " + player, this)
                            .execute();
                }
            }
        });
        serverPanel.playerOP.addListener(SWT.Selection, event -> {
            ScapesServer server = this.server;
            if (server != null) {
                for (String player : serverPanel.players.getSelection()) {
                    int level =
                            new OperatorDialog(serverPanel.getShell()).open();
                    if (level >= 0) {
                        server.getCommandRegistry()
                                .get("op -p " + player + " -l " +
                                        level, this).execute();
                    }
                }
            }
        });
        serverPanel.playerMessage.addListener(SWT.Selection, event -> {
            ScapesServer server = this.server;
            if (server != null) {
                for (String player : serverPanel.players.getSelection()) {
                    String message =
                            new MessageDialog(serverPanel.getShell()).open();
                    if (message != null) {
                        server.getCommandRegistry()
                                .get("tell -p " + player + ' ' + message, this)
                                .execute();
                    }
                }
            }
        });
    }

    public void run() throws IOException {
        Directory directory = files.get("data");
        if (!directory.exists()) {
            DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN);
            String path = dialog.open();
            if (path == null) {
                Dialogs.openMessage(shell, "This Server has no Save!",
                        SWT.ICON_ERROR | SWT.OK);
                throw new IOException("No save found");
            }
            FileSystem importDirectory =
                    FileSystemContainer.newFileSystem(path, "");
            importDirectory.copy(files.get("data"));
        }
        while (true) {
            start(Collections.singletonList(this));
            while (!shell.isDisposed() && !server.hasStopped()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            if (stop() != ScapesServer.ShutdownReason.RELOAD) {
                break;
            }
        }
    }

    @Override
    public String getID() {
        return "LocalUI";
    }

    @Override
    public void updatePlayers(String[] players) {
        display.asyncExec(() -> serverPanel.updatePlayers(players));
    }

    @Override
    public void updateWorlds(String[] worlds) {
        display.asyncExec(() -> serverPanel.updateWorlds(worlds));
    }

    @Override
    public void appendLog(String line) {
        display.asyncExec(() -> serverPanel.appendLine(line));
    }

    @Override
    public void sendProfilerResults(long ram, Map<String, Double> tps) {
        display.asyncExec(() -> serverPanel.appendProfiler(ram, tps));
    }

    @Override
    public void replaced() {
    }

    @Override
    public boolean isClosed() {
        return false;
    }
}
