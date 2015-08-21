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
package org.tobi29.scapes.vanilla.basics.entity.client;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.gui.GuiPlayerInventory;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.Frustum;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.client.MobPlayerClient;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.model.MobLivingModelHuman;
import org.tobi29.scapes.entity.model.MobModel;

import java.util.Optional;

public class MobPlayerClientVB extends MobPlayerClient {
    public MobPlayerClientVB(WorldClient world) {
        this(world, Vector3d.ZERO, Vector3d.ZERO, 0.0, 0.0, "");
    }

    public MobPlayerClientVB(WorldClient world, Vector3 pos, Vector3 speed,
            double xRot, double zRot, String nickname) {
        super(world, pos, speed, new AABB(-0.4, -0.4, -1, 0.4, 0.4, 0.9), 100,
                100, new Frustum(90, 1, 0.1, 24), new Frustum(50, 1, 0.1, 2),
                nickname);
        rot.setX(xRot);
        rot.setZ(zRot);
    }

    @Override
    public Vector3 viewOffset() {
        return new Vector3d(0.0, 0.0, 0.63);
    }

    @Override
    public Gui gui(MobPlayerClientMain player) {
        // TODO: Trade or steal UI maybe?
        return new GuiPlayerInventory(player);
    }

    @Override
    public Optional<MobModel> createModel() {
        Texture texture = world.scene().skinStorage().get(skin);
        return Optional.of(new MobLivingModelHuman(this, texture));
    }
}
