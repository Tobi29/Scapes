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
package org.tobi29.scapes.vanilla.basics.entity.model;

import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.entity.model.Box;

public class EntityModelBellowsShared {
    public final Box side, middle, pipe;

    public EntityModelBellowsShared(ScapesEngine engine) {
        side = new Box(engine, 0.0625f, -7, -7, -1, 7, 7, 1, 0, 0);
        middle = new Box(engine, 0.0625f, -6, -6, -7, 6, 6, 7, 0, 0);
        pipe = new Box(engine, 0.0625f, -2, -2, -16, 2, 2, 0, 0, 0);
    }
}