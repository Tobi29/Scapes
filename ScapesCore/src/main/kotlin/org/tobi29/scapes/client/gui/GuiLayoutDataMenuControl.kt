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

package org.tobi29.scapes.client.gui

import org.tobi29.scapes.engine.gui.GuiComponent
import org.tobi29.scapes.engine.gui.GuiLayoutDataFlow
import org.tobi29.scapes.engine.utils.math.vector.Vector2d

class GuiLayoutDataMenuControl(parent: GuiComponent,
                               marginStart: Vector2d,
                               marginEnd: Vector2d,
                               size: Vector2d,
                               val marginHorizontalStart: Vector2d,
                               val marginHorizontalEnd: Vector2d,
                               val sizeHorizontal: Vector2d,
                               priority: Long,
                               blocksEvents: Boolean = false) : GuiLayoutDataFlow(
        parent, marginStart, marginEnd, size, priority, blocksEvents)