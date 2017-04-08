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
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite

class ControlPanelGraphs(parent: Composite) : Composite(parent, SWT.NONE) {
    val cpu: ControlPanelGraph
    val memory: ControlPanelGraph

    init {
        layout = FillLayout(SWT.VERTICAL)
        cpu = ControlPanelGraph(this, 1.0,
                1.0) { "CPU: ${(it * 100.0).toInt()}%" }
        memory = ControlPanelGraph(this, 4096.0,
                1.0) { "Memory: ${it.toInt()}MB" }
    }

    override fun checkSubclass() {
    }
}
