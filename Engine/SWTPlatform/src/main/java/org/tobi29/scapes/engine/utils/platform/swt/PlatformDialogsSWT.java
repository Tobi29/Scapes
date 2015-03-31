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

package org.tobi29.scapes.engine.utils.platform.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.tobi29.scapes.engine.utils.platform.PlatformDialogs;

import java.io.File;
import java.util.Optional;

public class PlatformDialogsSWT implements PlatformDialogs {
    private static final File[] EMPTY_FILE = new File[0];
    private static final boolean IS_COCOA = "cocoa".equals(SWT.getPlatform());
    private final Shell shell;

    public PlatformDialogsSWT() {
        this(new Shell());
    }

    public PlatformDialogsSWT(Shell shell) {
        this.shell = shell;
    }

    @Override
    public File[] openFileDialog(Extension[] extensions, String title,
            boolean multiple) {
        String[] filterExtensions = new String[extensions.length];
        String[] filterNames = new String[extensions.length];
        for (int i = 0; i < extensions.length; i++) {
            Extension extension = extensions[i];
            filterExtensions[i] = extension.pattern;
            filterNames[i] = extension.name;
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
            return EMPTY_FILE;
        }
        String filterPath = fileDialog.getFilterPath();
        String[] fileNames = fileDialog.getFileNames();
        File[] files = new File[fileNames.length];
        for (int i = 0; i < fileNames.length; i++) {
            files[i] = new File(filterPath, fileNames[i]);
        }
        return files;
    }

    @Override
    public Optional<File> saveFileDialog(Extension[] extensions, String title) {
        String[] filterExtensions = new String[extensions.length];
        String[] filterNames = new String[extensions.length];
        for (int i = 0; i < extensions.length; i++) {
            Extension extension = extensions[i];
            filterExtensions[i] = extension.pattern;
            filterNames[i] = extension.name;
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
        return Optional.of(new File(fileDialog.getFilterPath(), fileName));
    }

    @Override
    public void message(MessageType messageType, String title, String message) {
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
    public void openFile(File file) {
        Program.launch(file.getAbsolutePath());
    }

    @Override
    public boolean renderTick(boolean force) {
        return !IS_COCOA &&
                shell.getDisplay().readAndDispatch(); // Avoid jvm crash on osx
    }

    @Override
    public void dispose() {
        shell.dispose();
    }
}
