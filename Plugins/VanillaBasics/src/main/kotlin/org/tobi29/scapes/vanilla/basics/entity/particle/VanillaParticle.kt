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

package org.tobi29.scapes.vanilla.basics.entity.particle

import org.tobi29.scapes.entity.particle.ParticleTransparentAtlas
import org.tobi29.scapes.entity.particle.ParticleTransparentTexture

class VanillaParticle(pa: ParticleTransparentAtlas) {
    val cloud: ParticleTransparentTexture
    val explosion: ParticleTransparentTexture
    val smoke: ParticleTransparentTexture

    init {
        cloud = pa.registerTexture(
                "VanillaBasics:image/entity/particle/Cloud.png")
        explosion = pa.registerTexture(
                "VanillaBasics:image/entity/particle/Explosion.png")
        smoke = pa.registerTexture(
                "VanillaBasics:image/entity/particle/Smoke.png")
    }
}
