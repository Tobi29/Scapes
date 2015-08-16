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

package org.tobi29.scapes.entity.client;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.server.PlayConnection;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.MobPositionHandler;
import org.tobi29.scapes.entity.MobileEntity;
import org.tobi29.scapes.entity.model.MobModel;

import java.util.Optional;

public abstract class MobClient extends EntityClient implements MobileEntity {
    protected final AABB collision;
    protected final MutableVector3d speed, rot = new MutableVector3d();
    protected final MobPositionHandler positionHandler;
    protected boolean ground, inWater, swimming, headInWater, slidingWall;

    protected MobClient(WorldClient world, Vector3 pos, Vector3 speed,
            AABB aabb) {
        super(world, pos);
        this.speed = new MutableVector3d(speed);
        collision = aabb;
        positionHandler = createPositionHandler(world.connection());
    }

    public AABB aabb() {
        AABB aabb = new AABB(collision);
        aabb.add(pos.doubleX(), pos.doubleY(), pos.doubleZ());
        return aabb;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        positionHandler.receiveMoveAbsolute(pos.doubleX(), pos.doubleY(),
                pos.doubleZ());
        tagStructure.getMultiTag("Speed", speed);
        tagStructure.getMultiTag("Rot", rot);
    }

    @Override
    public Optional<? extends MobModel> createModel() {
        return Optional.empty();
    }

    protected MobPositionHandler createPositionHandler(
            PlayConnection connection) {
        return new MobPositionHandler(pos.now(), connection::send, pos::set,
                speed::set, rot::set,
                (ground, slidingWall, inWater, swimming) -> {
                    this.ground = ground;
                    this.slidingWall = slidingWall;
                    this.inWater = inWater;
                    this.swimming = swimming;
                });
    }

    @Override
    public MobPositionHandler positionHandler() {
        return positionHandler;
    }

    public Vector3 speed() {
        return speed.now();
    }

    public Vector3 rot() {
        return rot.now();
    }

    public double speedX() {
        return speed.doubleX();
    }

    public double pitch() {
        return rot.doubleX();
    }

    public double speedY() {
        return speed.doubleY();
    }

    public double tilt() {
        return rot.doubleY();
    }

    public double speedZ() {
        return speed.doubleZ();
    }

    public double yaw() {
        return rot.doubleZ();
    }

    public boolean isHeadInWater() {
        return headInWater;
    }

    public boolean isInWater() {
        return inWater;
    }

    public boolean isSwimming() {
        return swimming;
    }

    public boolean isOnGround() {
        return ground;
    }

    public void move(double delta) {
        headInWater = world.terrain().type(pos.intX(), pos.intY(),
                FastMath.floor(pos.doubleZ() + 0.7)).isLiquid();
    }
}
