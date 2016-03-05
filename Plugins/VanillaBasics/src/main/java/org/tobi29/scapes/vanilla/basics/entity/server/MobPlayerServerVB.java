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
package org.tobi29.scapes.vanilla.basics.entity.server;

import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.Checksum;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.Frustum;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.WieldMode;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MobPlayerServerVB extends MobPlayerServer {
    public MobPlayerServerVB(WorldServer world, Vector3 pos, Vector3 speed,
            double xRot, double zRot, String nickname, Checksum skin,
            PlayerConnection connection) {
        super(world, pos, speed, new AABB(-0.4, -0.4, -1, 0.4, 0.4, 0.9), 100,
                100, new Frustum(90, 1, 0.1, 24), new Frustum(50, 1, 0.1, 2),
                nickname, skin, connection);
        rot.setX(xRot);
        rot.setZ(zRot);
    }

    @Override
    public WieldMode wieldMode() {
        return inventorySelectLeft == inventorySelectRight ? WieldMode.RIGHT :
                WieldMode.DUAL;
    }

    @Override
    public void update(double delta) {
        double lookX = FastMath.cosTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * 6;
        double lookY = FastMath.sinTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * 6;
        double lookZ = FastMath.sinTable(rot.doubleX() * FastMath.PI / 180) * 6;
        Vector3 viewOffset = viewOffset();
        viewField.setView(pos.doubleX() + viewOffset.doubleX(),
                pos.doubleY() + viewOffset.doubleY(),
                pos.doubleZ() + viewOffset.doubleZ(), pos.doubleX() + lookX,
                pos.doubleY() + lookY, pos.doubleZ() + lookZ, 0, 0, 1);
        world.entities(viewField).forEach(entity -> {
            Vector3 mobPos = entity.pos();
            if (!world.checkBlocked(pos.intX(), pos.intY(), pos.intZ(),
                    mobPos.intX(), mobPos.intY(), mobPos.intZ())) {
                onNotice(entity);
            }
        });
        if (pos.doubleZ() < -100.0) {
            damage(-pos.doubleZ() - 100.0);
        }
        if (health < 10.0) {
            Random random = ThreadLocalRandom.current();
            if (random.nextInt(40) == 0) {
                push(random.nextDouble() * 2.0 - 1.0, 0.0, 0.0);
            }
            if (random.nextInt(40) == 0) {
                push(0.0, random.nextDouble() * 2.0 - 1.0, 0.0);
            }
            if (random.nextInt(20) == 0) {
                setRot(new Vector3d(
                        rot.doubleX() + random.nextDouble() * 60.0 - 30.0,
                        rot.doubleY(), rot.doubleZ()));
            }
            if (random.nextInt(20) == 0) {
                setRot(new Vector3d(rot.doubleX(), rot.doubleY(),
                        rot.doubleZ() + random.nextDouble() * 60.0 - 30.0));
            }
        }
    }

    @Override
    public Vector3 viewOffset() {
        return new Vector3d(0.0, 0.0, 0.63);
    }

    @Override
    public boolean isActive() {
        TagStructure conditionTag =
                metaData("Vanilla").getStructure("Condition");
        return !conditionTag.getBoolean("Sleeping");
    }
}
