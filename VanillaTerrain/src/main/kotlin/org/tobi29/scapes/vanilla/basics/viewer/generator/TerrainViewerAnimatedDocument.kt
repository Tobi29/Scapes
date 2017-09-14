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

import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.tobi29.scapes.engine.swt.util.framework.Application
import org.tobi29.scapes.engine.swt.util.widgets.ifPresent
import org.tobi29.scapes.engine.utils.task.Timer
import org.tobi29.scapes.engine.utils.task.loop
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class TerrainViewerAnimatedDocument(
        application: Application,
        colorSupplier: () -> TerrainViewerCanvas.ColorSupplier,
        private val progress: () -> Unit
) : TerrainViewerDocument(colorSupplier) {
    val job = AtomicBoolean(false).let { stop ->
        launch(application + CoroutineName("Animation-Progress")) {
            Timer().apply { init() }.loop(Timer.toDiff(4.0),
                    { delay(it, TimeUnit.NANOSECONDS) }) {
                if (stop.get()) return@loop false

                canvas.ifPresent {
                    if (!it.isRendering) {
                        progress()
                        it.render()
                    }
                }

                true
            }
        } to stop
    }

    override fun destroy() {
        super.destroy()
        job.let { (_, stop) -> stop.set(true) }
    }
}
