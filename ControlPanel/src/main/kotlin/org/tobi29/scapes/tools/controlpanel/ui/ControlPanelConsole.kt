/*
 * Copyright 2012-2017 Tobi29
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

package org.tobi29.scapes.tools.controlpanel.ui

import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Text
import org.tobi29.application.swt.Fonts
import org.tobi29.application.swt.HTMLLineStyler

class ControlPanelConsole(parent: Composite) : Composite(parent, SWT.NONE) {
    val console: StyledText
    val input: Text

    init {
        layout = GridLayout(1, false)
        console = StyledText(this,
                SWT.BORDER or SWT.READ_ONLY or SWT.WRAP or SWT.V_SCROLL or
                        SWT.MULTI)
        val font = Font(display, Fonts.monospace)
        addDisposeListener { e -> font.dispose() }
        console.font = font
        console.layoutData = GridData(SWT.FILL, SWT.FILL, true, true, 1, 1)
        HTMLLineStyler(console)
        input = Text(this, SWT.BORDER)
        input.layoutData = GridData(SWT.FILL, SWT.FILL, true, false, 1, 1)
    }

    override fun checkSubclass() {
    }
}
