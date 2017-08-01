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
package org.tobi29.scapes.vanilla.basics.viewer.generator

import org.tobi29.scapes.engine.swt.util.framework.DocumentComposite
import org.tobi29.scapes.engine.swt.util.framework.MultiDocumentApplication
import org.tobi29.scapes.engine.swt.util.widgets.SmartMenuBar
import org.tobi29.scapes.engine.swt.util.widgets.ifPresent

class TerrainViewerAnimatedDocument(colorSupplier: () -> TerrainViewerCanvas.ColorSupplier,
                                    private val progress: () -> Unit) : TerrainViewerDocument(
        colorSupplier) {
    override fun populate(composite: DocumentComposite,
                          menu: SmartMenuBar,
                          application: MultiDocumentApplication) {
        super.populate(composite, menu, application)
        application.loop.addTask({
            canvas.ifPresent {
                if (!it.isRendering) {
                    progress()
                    it.render()
                }
                return@addTask 250
            }
            -1
        }, "Animation-Progress", 0, false)
    }
}
