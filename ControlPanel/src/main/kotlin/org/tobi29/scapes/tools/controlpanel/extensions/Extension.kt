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

package org.tobi29.scapes.tools.controlpanel.extensions

import org.eclipse.swt.widgets.Widget
import org.tobi29.server.ControlPanelProtocol
import org.tobi29.utils.ComponentRegisteredHolder
import org.tobi29.scapes.tools.controlpanel.ControlPanelDocument
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConnection

abstract class Extension(
        protected val connection: ControlPanelProtocol
) : ComponentRegisteredHolder<ControlPanelDocument>, ComponentUIControlPanel

interface ComponentUIControlPanel : ComponentUI<ControlPanelConnection>

interface ComponentUI<in C : Widget> {
    val priority: Int get() = 0

    fun populate(composite: C) {}
}
