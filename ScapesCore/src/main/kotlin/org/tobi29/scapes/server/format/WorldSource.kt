/*
 * Copyright 2012-2018 Tobi29
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

package org.tobi29.scapes.server.format

import org.tobi29.graphics.Bitmap
import org.tobi29.scapes.plugins.spi.PluginReference
import org.tobi29.scapes.server.ScapesServer

interface WorldSource : AutoCloseable {
    fun init(seed: Long, plugins: List<PluginReference>)

    fun panorama(images: Panorama)

    fun panorama(): Panorama?

    fun open(server: ScapesServer): WorldFormat

    override fun close()

    class Panorama(
        image0: Bitmap<*, *>,
        image1: Bitmap<*, *>,
        image2: Bitmap<*, *>,
        image3: Bitmap<*, *>,
        image4: Bitmap<*, *>,
        image5: Bitmap<*, *>
    ) {
        val elements = listOf(image0, image1, image2, image3, image4, image5)
    }
}

inline fun newPanorama(
    initializer: (Int) -> Bitmap<*, *>
): WorldSource.Panorama = WorldSource.Panorama(
    initializer(0), initializer(1), initializer(2),
    initializer(3), initializer(4), initializer(5)
)
