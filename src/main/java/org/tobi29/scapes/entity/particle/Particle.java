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

package org.tobi29.scapes.entity.particle;

import org.tobi29.scapes.block.AABBElement;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.OpenGL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;

public abstract class Particle {
    protected final AABB collision;
    protected final ParticleManager particleManager;
    protected final MutableVector3 pos, posRender, speed;
    public boolean ground, inWater, slidingWall;
    protected double gravitationMultiplier = 1.0, airFriction = 0.2,
            groundFriction = 0.4, wallFriction = 2.0, waterFriction = 8.0;
    private int water;

    protected Particle(ParticleManager particleManager, Vector3 pos,
            Vector3 speed, AABB aabb) {
        this.particleManager = particleManager;
        this.pos = new MutableVector3d(pos);
        posRender = new MutableVector3d(pos);
        this.speed = new MutableVector3d(speed);
        collision = aabb;
    }

    public Vector3 getPos() {
        return pos.now();
    }

    public void setPos(Vector3 pos) {
        this.pos.set(pos);
    }

    public Vector3 getPosRender() {
        return posRender.now();
    }

    public Vector3 getSpeed() {
        return speed.now();
    }

    public void setSpeed(Vector3 speed) {
        this.speed.set(speed);
    }

    public void move(double delta) {
        WorldClient world = particleManager.getWorld();
        double gravitation = world.getGravitation();
        speed.plusZ(-gravitationMultiplier * delta * gravitation);
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
        double goX = FastMath.clamp(speed.doubleX() * delta, -1.0, 1.0);
        double goY = FastMath.clamp(speed.doubleY() * delta, -1.0, 1.0);
        double goZ = FastMath.clamp(speed.doubleZ() * delta, -1.0, 1.0);
        boolean ground = false;
        boolean slidingWall = false;
        boolean inWater;
        AABB aabb = new AABB(collision);
        aabb.add(pos.doubleX(), pos.doubleY(), pos.doubleZ());
        Pool<AABBElement> aabbs = world.getTerrain().getCollisions(
                FastMath.floor(aabb.minX + FastMath.min(goX, 0.0)),
                FastMath.floor(aabb.minY + FastMath.min(goY, 0.0)),
                FastMath.floor(aabb.minZ + FastMath.min(goZ, 0.0)),
                FastMath.floor(aabb.maxX + FastMath.max(goX, 0.0)),
                FastMath.floor(aabb.maxY + FastMath.max(goY, 0.0)),
                FastMath.floor(aabb.maxZ + FastMath.max(goZ, 0.0)));
        double lastGoZ = aabb.moveOutZ(
                aabbs.stream().filter(AABBElement::isSolid)
                        .map(AABBElement::getAABB).iterator(), goZ);
        pos.plusZ(lastGoZ);
        aabb.add(0.0, 0.0, lastGoZ);
        if (lastGoZ - goZ > 0.0) {
            ground = true;
            speed.setZ(speed.doubleZ() / (1.0 + 4.0 * delta));
        }
        double lastGoX = aabb.moveOutX(
                aabbs.stream().filter(AABBElement::isSolid)
                        .map(AABBElement::getAABB).iterator(), goX);
        pos.plusX(lastGoX);
        aabb.add(lastGoX, 0.0, 0.0);
        if (lastGoX != goX) {
            slidingWall = true;
            speed.setX(0.0);
        }
        double lastGoY = aabb.moveOutY(
                aabbs.stream().filter(AABBElement::isSolid)
                        .map(AABBElement::getAABB).iterator(), goY);
        pos.plusY(lastGoY);
        aabb.add(0.0, lastGoY, 0.0);
        if (lastGoY != goY) {
            slidingWall = true;
            speed.setY(0.0);
        }
        boolean w = false;
        for (AABBElement element : aabbs) {
            if (aabb.overlay(element.aabb)) {
                element.collision.inside(this, delta);
                if (element.collision.isLiquid()) {
                    w = true;
                }
            }
        }
        if (w) {
            water++;
            inWater = water > 1;
        } else {
            inWater = false;
            water = 0;
        }
        aabbs.reset();
        this.ground = ground;
        this.slidingWall = slidingWall;
        this.inWater = inWater;
        if (pos.doubleZ() < -16.0) {
            particleManager.delete(this);
        }
    }

    public void render(GL gl, Cam cam, Shader shader) {
        WorldClient world = particleManager.getWorld();
        posRender.set(pos.now());
        int x = posRender.intX(), y = posRender.intY(), z = posRender.intZ();
        BlockType type = world.getTerrain().type(x, y, z);
        if (!type.isSolid(world.getTerrain(), x, y, z) ||
                type.isTransparent(world.getTerrain(), x, y, z)) {
            OpenGL openGL = gl.getOpenGL();
            openGL.setAttribute2f(4,
                    world.getTerrain().blockLight(x, y, z) / 15.0f,
                    world.getTerrain().sunLight(x, y, z) / 15.0f);
            float posRenderX =
                    (float) (posRender.doubleX() - cam.position.doubleX());
            float posRenderY =
                    (float) (posRender.doubleY() - cam.position.doubleY());
            float posRenderZ =
                    (float) (posRender.doubleZ() - cam.position.doubleZ());
            renderParticle(posRenderX, posRenderY, posRenderZ, 1.0f, 1.0f, 1.0f,
                    1.0f, gl, shader);
        }
    }

    public abstract void renderParticle(float x, float y, float z, float r,
            float g, float b, float a, GL gl, Shader shader);

    public abstract void update(double delta);
}
