/*
 * Copyright 2012-2016 Tobi29
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

package org.tobi29.scapes.vanilla.basics.viewer.generator

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.tobi29.scapes.engine.swt.util.framework.Document
import org.tobi29.scapes.engine.swt.util.framework.DocumentComposite
import org.tobi29.scapes.engine.swt.util.framework.MultiDocumentApplication
import org.tobi29.scapes.engine.swt.util.widgets.SmartMenuBar

open class TerrainViewerDocument(
        protected val colorSupplier: () -> TerrainViewerCanvas.ColorSupplier) : Document {
    override val title = "Terrain Viewer"
    protected var canvas: TerrainViewerCanvas? = null

    override fun forceClose() {
    }

    override fun destroy() {
        canvas = null
    }

    override fun populate(composite: DocumentComposite,
                          menu: SmartMenuBar,
                          application: MultiDocumentApplication) {
        composite.layout = FillLayout()
        canvas = TerrainViewerCanvas(composite, SWT.DOUBLE_BUFFERED,
                application, colorSupplier, 1)
    }
}
