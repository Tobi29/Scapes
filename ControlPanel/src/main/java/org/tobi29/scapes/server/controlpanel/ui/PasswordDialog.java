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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class PasswordDialog extends Dialog {
    private final Shell shell;
    private String output;

    public PasswordDialog(Shell parent) {
        super(parent);
        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
        shell.setText("Connect");
        shell.setSize(450, 120);
        shell.setLayout(new GridLayout(1, false));

        Label label = new Label(shell, SWT.NONE);
        label.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        label.setText("Password");

        Text text = new Text(shell, SWT.BORDER | SWT.PASSWORD);
        text.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Button connect = new Button(shell, SWT.NONE);
        connect.setLayoutData(
                new GridData(SWT.CENTER, SWT.CENTER, false, true, 1, 1));
        connect.setText("Connect");
        connect.addListener(SWT.Selection, event -> {
            output = text.getText();
            shell.dispose();
        });
    }

    public String open() {
        shell.open();
        Display display = getParent().getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return output;
    }

    @Override
    protected void checkSubclass() {
    }
}
