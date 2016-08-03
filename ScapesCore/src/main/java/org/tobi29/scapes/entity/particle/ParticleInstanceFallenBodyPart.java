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

package org.tobi29.scapes.entity.particle;

import org.tobi29.scapes.engine.graphics.Texture;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3f;
import org.tobi29.scapes.entity.model.Box;

public class ParticleInstanceFallenBodyPart extends ParticleInstance {
    public final MutableVector3 rotation = new MutableVector3f(),
            rotationSpeed = new MutableVector3f();
    public Box box;
    public Texture texture;
}
