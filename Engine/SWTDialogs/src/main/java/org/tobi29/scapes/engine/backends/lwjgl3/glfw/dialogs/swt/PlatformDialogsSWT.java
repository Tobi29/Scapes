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

package org.tobi29.scapes.engine.backends.lwjgl3.glfw.dialogs.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.tobi29.scapes.engine.backends.lwjgl3.glfw.PlatformDialogs;
import org.tobi29.scapes.engine.opengl.Container;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.io.filesystem.Directory;
import org.tobi29.scapes.engine.utils.io.filesystem.File;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class PlatformDialogsSWT implements PlatformDialogs {
    private static final URI[] EMPTY_URI = new URI[0];
    private static final boolean IS_COCOA = "cocoa".equals(SWT.getPlatform());
    private final Shell shell;

    public PlatformDialogsSWT(String id) {
        this(id, new Shell());
    }

    public PlatformDialogsSWT(String id, Shell shell) {
        this.shell = shell;
        Display.setAppName(id);
    }

    @Override
    public URI[] openFileDialog(Pair<String, String>[] extensions, String title,
            boolean multiple) {
        String[] filterExtensions = new String[extensions.length];
        String[] filterNames = new String[extensions.length];
        for (int i = 0; i < extensions.length; i++) {
            Pair<String, String> extension = extensions[i];
            filterExtensions[i] = extension.a;
            filterNames[i] = extension.b;
        }
        int style = SWT.OPEN | SWT.APPLICATION_MODAL;
        if (multiple) {
            style |= SWT.MULTI;
        }
        FileDialog fileDialog = new FileDialog(shell, style);
        fileDialog.setText(title);
        fileDialog.setFilterExtensions(filterExtensions);
        fileDialog.setFilterNames(filterNames);
        boolean successful = fileDialog.open() != null;
        if (!successful) {
            return EMPTY_URI;
        }
        String filterPath = fileDialog.getFilterPath();
        String[] fileNames = fileDialog.getFileNames();
        URI[] files = new URI[fileNames.length];
        for (int i = 0; i < fileNames.length; i++) {
            files[i] = Paths.get(filterPath, fileNames[i]).toAbsolutePath()
                    .toUri();
        }
        return files;
    }

    @Override
    public Optional<URI> saveFileDialog(Pair<String, String>[] extensions,
            String title) {
        String[] filterExtensions = new String[extensions.length];
        String[] filterNames = new String[extensions.length];
        for (int i = 0; i < extensions.length; i++) {
            Pair<String, String> extension = extensions[i];
            filterExtensions[i] = extension.a;
            filterNames[i] = extension.b;
        }
        FileDialog fileDialog =
                new FileDialog(shell, SWT.SAVE | SWT.APPLICATION_MODAL);
        fileDialog.setText(title);
        fileDialog.setFilterExtensions(filterExtensions);
        fileDialog.setFilterNames(filterNames);
        boolean successful = fileDialog.open() != null;
        if (!successful) {
            return Optional.empty();
        }
        String fileName = fileDialog.getFileName();
        return Optional.of(Paths.get(fileDialog.getFilterPath(), fileName)
                .toAbsolutePath().toUri());
    }

    @Override
    public boolean exportToUser(File file, Pair<String, String>[] extensions,
            String title) throws IOException {
        Optional<URI> export = saveFileDialog(extensions, title);
        if (export.isPresent()) {
            Files.copy(Paths.get(file.getURI()), Paths.get(export.get()));
            return true;
        }
        return false;
    }

    @Override
    public boolean importFromUser(File file, Pair<String, String>[] extensions,
            String title) throws IOException {
        URI[] exports = openFileDialog(extensions, title, false);
        for (URI export : exports) {
            Files.copy(Paths.get(export), Paths.get(file.getURI()));
        }
        return exports.length > 0;
    }

    @Override
    public boolean importFromUser(Directory directory,
            Pair<String, String>[] extensions, String title, boolean multiple)
            throws IOException {
        URI[] exports = openFileDialog(extensions, title, multiple);
        for (URI export : exports) {
            Path path = Paths.get(export);
            Files.copy(path,
                    Paths.get(directory.getURI()).resolve(path.getFileName()));
        }
        return exports.length > 0;
    }

    @Override
    public void message(Container.MessageType messageType, String title,
            String message) {
        int style = SWT.APPLICATION_MODAL;
        switch (messageType) {
            case ERROR:
                style |= SWT.ICON_ERROR;
                break;
            case INFORMATION:
                style |= SWT.ICON_INFORMATION;
                break;
            case WARNING:
                style |= SWT.ICON_WARNING;
                break;
            case QUESTION:
                style |= SWT.ICON_QUESTION;
                break;
        }
        MessageBox messageBox = new MessageBox(shell, style);
        messageBox.setText(title);
        messageBox.setMessage(message);
        messageBox.open();
    }

    @Override
    public void openFile(URI file) {
        Program.launch(file.toString());
    }

    @Override
    public void renderTick() {
        if (!IS_COCOA) { // Avoid jvm crash on osx
            shell.getDisplay().readAndDispatch();
        }
    }

    @Override
    public void dispose() {
        shell.dispose();
    }
}