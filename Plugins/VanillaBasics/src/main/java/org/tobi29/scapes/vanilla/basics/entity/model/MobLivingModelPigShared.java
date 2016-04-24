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

public class MobLivingModelPigShared {
    public final Box body, head, legFrontLeft, legFrontRight, legBackLeft,
            legBackRight;

    public MobLivingModelPigShared(ScapesEngine engine) {
        body = new Box(engine, 0.015625f, -5, -6, -5, 5, 6, 5, 0, 0);
        head = new Box(engine, 0.015625f, -4, 0, -5, 4, 8, 4, 0, 22);
        legFrontLeft = new Box(engine, 0.015625f, -2, -2, -6, 2, 2, 0, 44, 0);
        legFrontRight = new Box(engine, 0.015625f, -2, -2, -6, 2, 2, 0, 44, 10);
        legBackLeft = new Box(engine, 0.015625f, -2, -2, -6, 2, 2, 0, 44, 20);
        legBackRight = new Box(engine, 0.015625f, -2, -2, -6, 2, 2, 0, 44, 30);
    }
}
