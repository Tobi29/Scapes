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
package org.tobi29.scapes.entity.server;

import org.tobi29.scapes.block.AABBElement;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.connection.PlayConnection;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.MobPositionHandler;
import org.tobi29.scapes.entity.MobileEntity;

import java.util.Iterator;

public abstract class MobServer extends EntityServer implements MobileEntity {
    protected final AABB collision;
    protected final MutableVector3d speed, rot = new MutableVector3d();
    protected final MobPositionHandler positionHandler;
    protected boolean ground, inWater, swimming, headInWater, slidingWall;
    protected int swim;
    protected double gravitationMultiplier = 1.0, airFriction = 0.2,
            groundFriction = 1.6, wallFriction = 2.0, waterFriction = 8.0,
            stepHeight = 1.0;

    protected MobServer(WorldServer world, Vector3 pos, Vector3 speed,
            AABB aabb) {
        super(world, pos);
        this.speed = new MutableVector3d(speed);
        collision = aabb;
        positionHandler = createPositionHandler(world);
    }

    protected static Iterator<AABB> collisions(Pool<AABBElement> aabbs) {
        return Streams.of(aabbs).filter(AABBElement::isSolid)
                .map(AABBElement::aabb).iterator();
    }

    protected void updateVelocity(double gravitation, double delta) {
        speed.div(1.0 + airFriction * delta);
        if (inWater) {
            speed.div(1.0 + waterFriction * delta);
        } else {
            if (ground) {
                speed.div(1.0 + groundFriction * delta * gravitation);
            }
            if (slidingWall) {
                speed.div(1.0 + wallFriction * delta);
            }
        }
        speed.plusZ(-gravitation * gravitationMultiplier * delta);
    }

    protected void move(AABB aabb, Pool<AABBElement> aabbs, double goX,
            double goY, double goZ) {
        boolean ground = false;
        boolean slidingWall = false;
        double lastGoZ = aabb.moveOutZ(collisions(aabbs), goZ);
        pos.plusZ(lastGoZ);
        aabb.add(0, 0, lastGoZ);
        if (lastGoZ - goZ > 0) {
            ground = true;
        }
        // Walk
        boolean walking = true;
        while (walking) {
            walking = false;
            if (goX != 0.0) {
                double lastGoX = aabb.moveOutX(collisions(aabbs), goX);
                if (lastGoX != 0.0) {
                    pos.plusX(lastGoX);
                    aabb.add(lastGoX, 0.0, 0.0);
                    goX -= lastGoX;
                    walking = true;
                }
            }
            if (goY != 0.0) {
                double lastGoY = aabb.moveOutY(collisions(aabbs), goY);
                if (lastGoY != 0.0) {
                    pos.plusY(lastGoY);
                    aabb.add(0.0, lastGoY, 0.0);
                    goY -= lastGoY;
                    walking = true;
                }
            }
        }
        // Check collision
        boolean slidingX = goX != 0.0;
        boolean slidingY = goY != 0.0;
        if (slidingX || slidingY) {
            if (stepHeight > 0.0 && (this.ground || inWater)) {
                // Step
                // Calculate step height
                AABB aabbStep = new AABB(aabb).add(goX, 0.0, 0.0);
                double stepX = aabbStep.moveOutZ(collisions(aabbs), stepHeight);
                aabbStep = new AABB(aabb).add(0.0, goY, 0.0);
                double stepY = aabbStep.moveOutZ(collisions(aabbs), stepHeight);
                double step = FastMath.max(stepX, stepY);
                aabbStep = new AABB(aabb).add(goX, goY, step);
                step += aabbStep.moveOutZ(collisions(aabbs), -step);
                // Check step height
                aabbStep.copy(aabb).add(0.0, 0.0, step);
                step = aabb.moveOutZ(collisions(aabbs), step);
                // Attempt walk at new height
                double lastGoX = aabbStep.moveOutX(collisions(aabbs), goX);
                aabbStep.add(lastGoX, 0.0, 0.0);
                double lastGoY = aabbStep.moveOutY(collisions(aabbs), goY);
                // Check if walk was successful
                if (lastGoX != 0.0 || lastGoY != 0.0) {
                    pos.plusX(lastGoX);
                    pos.plusY(lastGoY);
                    aabb.copy(aabbStep).add(0.0, lastGoY, 0.0);
                    pos.plusZ(step);
                } else {
                    // Collide
                    slidingWall = true;
                    if (slidingX) {
                        speed.setX(0.0);
                    }
                    if (slidingY) {
                        speed.setY(0.0);
                    }
                }
            } else {
                // Collide
                slidingWall = true;
                if (slidingX) {
                    speed.setX(0.0);
                }
                if (slidingY) {
                    speed.setY(0.0);
                }
            }
        }
        this.ground = ground;
        this.slidingWall = slidingWall;
    }

    protected void collide(AABB aabb, Pool<AABBElement> aabbs, double delta) {
        boolean inWater = false;
        boolean swimming;
        for (AABBElement element : aabbs) {
            if (aabb.overlay(element.aabb)) {
                element.collision.inside(this, delta);
                if (element.collision.isLiquid()) {
                    inWater = true;
                }
            }
        }
        aabb.minZ = FastMath.mix(aabb.minZ, aabb.maxZ, 0.6);
        boolean water = false;
        for (AABBElement element : aabbs) {
            if (aabb.overlay(element.aabb)) {
                if (element.collision.isLiquid()) {
                    water = true;
                }
            }
        }
        if (water) {
            swim++;
            swimming = swim > 1;
        } else {
            swimming = false;
            swim = 0;
        }
        this.inWater = inWater;
        this.swimming = swimming;
    }

    public void dropItem(ItemStack item) {
        EntityServer entity = new MobItemServer(world, pos.now(), new Vector3d(
                FastMath.cosTable(rot.doubleZ() * FastMath.DEG_2_RAD) *
                        10.0 *
                        FastMath.cosTable(rot.doubleX() * FastMath.DEG_2_RAD),
                FastMath.sinTable(rot.doubleZ() * FastMath.DEG_2_RAD) *
                        10.0 *
                        FastMath.cosTable(rot.doubleX() * FastMath.DEG_2_RAD),
                FastMath.sinTable(rot.doubleX() * FastMath.DEG_2_RAD) * 0.3 +
                        0.3), item, Double.NaN);
        entity.onSpawn();
        world.addEntity(entity);
    }

    public AABB aabb() {
        AABB aabb = new AABB(collision);
        aabb.add(pos.doubleX(), pos.doubleY(), pos.doubleZ());
        return aabb;
    }

    @Override
    public TagStructure write() {
        TagStructure tagStructure = super.write();
        tagStructure.setMultiTag("Speed", speed);
        tagStructure.setMultiTag("Rot", rot);
        return tagStructure;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        tagStructure.getMultiTag("Speed", speed);
        tagStructure.getMultiTag("Rot", rot);
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

    public void setSpeed(Vector3 speed) {
        assert world.checkThread();
        this.speed.set(speed);
    }

    public Vector3 rot() {
        return rot.now();
    }

    public void setRot(Vector3 rot) {
        this.rot.set(rot);
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

    public void push(double x, double y, double z) {
        assert world.checkThread();
        speed.plusX(x).plusY(y).plusZ(z);
    }

    public void setPos(Vector3 pos) {
        assert world.checkThread();
        synchronized (this.pos) {
            this.pos.set(pos);
        }
    }

    public void updatePosition() {
        positionHandler
                .submitUpdate(entityID, pos.now(), speed.now(), rot.now(),
                        ground, slidingWall, inWater, swimming, true);
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
        if (!world.getTerrain()
                .isBlockTicking(pos.intX(), pos.intY(), pos.intZ())) {
            return;
        }
        updateVelocity(world.gravity(), delta);
        double goX = FastMath.clamp(speed.doubleX() * delta, -1.0, 1.0);
        double goY = FastMath.clamp(speed.doubleY() * delta, -1.0, 1.0);
        double goZ = FastMath.clamp(speed.doubleZ() * delta, -1.0, 1.0);
        AABB aabb = aabb();
        Pool<AABBElement> aabbs = world.getTerrain()
                .collisions(FastMath.floor(aabb.minX + FastMath.min(goX, 0.0)),
                        FastMath.floor(aabb.minY + FastMath.min(goY, 0.0)),
                        FastMath.floor(aabb.minZ + FastMath.min(goZ, 0.0)),
                        FastMath.floor(aabb.maxX + FastMath.max(goX, 0.0)),
                        FastMath.floor(aabb.maxY + FastMath.max(goY, 0.0)),
                        FastMath.floor(
                                aabb.maxZ + FastMath.max(goZ, stepHeight)));
        move(aabb, aabbs, goX, goY, goZ);
        if (ground) {
            speed.setZ(speed.doubleZ() / (1.0 + 4.0 * delta));
        }
        headInWater = world.getTerrain().type(pos.intX(), pos.intY(),
                FastMath.floor(pos.doubleZ() + 0.7)).isLiquid();
        collide(aabb, aabbs, delta);
        positionHandler
                .submitUpdate(entityID, pos.now(), speed.now(), rot.now(),
                        ground, slidingWall, inWater, swimming);
    }
}
