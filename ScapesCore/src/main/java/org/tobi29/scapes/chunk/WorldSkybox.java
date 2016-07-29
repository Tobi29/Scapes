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

package org.tobi29.scapes.chunk;

import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.utils.graphics.Cam;

public interface WorldSkybox {
    void update(double delta);

    void init(GL gl);

    void renderUpdate(Cam cam, double delta);

    void render(GL gl, Cam cam);

    void dispose(GL gl);

    float exposure();

    float fogR();

    float fogG();

    float fogB();

    float fogDistance();
}
