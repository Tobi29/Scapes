package org.tobi29.scapes.entity.particle;

import org.tobi29.scapes.block.AABBElement;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.util.Iterator;

public class ParticlePhysics {
    protected static Iterator<AABB> collisions(Pool<AABBElement> aabbs) {
        return Streams.of(aabbs).filter(AABBElement::isSolid)
                .map(AABBElement::aabb).iterator();
    }

    public static boolean update(double delta, ParticleInstance instance,
            TerrainClient terrain, AABB aabb, float gravitation,
            float gravitationMultiplier, float airFriction,
            float groundFriction, float waterFriction) {
        double goX =
                FastMath.clamp(instance.speed.doubleX() * delta, -10.0, 10.0);
        double goY =
                FastMath.clamp(instance.speed.doubleY() * delta, -10.0, 10.0);
        double goZ =
                FastMath.clamp(instance.speed.doubleZ() * delta, -10.0, 10.0);
        Pool<AABBElement> aabbs = terrain.collisions(
                FastMath.floor(aabb.minX + FastMath.min(goX, 0.0)),
                FastMath.floor(aabb.minY + FastMath.min(goY, 0.0)),
                FastMath.floor(aabb.minZ + FastMath.min(goZ, 0.0)),
                FastMath.floor(aabb.maxX + FastMath.max(goX, 0.0)),
                FastMath.floor(aabb.maxY + FastMath.max(goY, 0.0)),
                FastMath.floor(aabb.maxZ + FastMath.max(goZ, 0.0)));
        double lastGoZ = aabb.moveOutZ(collisions(aabbs), goZ);
        instance.pos.plusZ(lastGoZ);
        aabb.add(0.0, 0.0, lastGoZ);
        boolean ground = lastGoZ - goZ > 0.0;
        double lastGoX = aabb.moveOutX(collisions(aabbs), goX);
        instance.pos.plusX(lastGoX);
        aabb.add(lastGoX, 0.0, 0.0);
        if (lastGoX != goX) {
            instance.speed.setX(0.0);
        }
        double lastGoY = aabb.moveOutY(collisions(aabbs), goY);
        instance.pos.plusY(lastGoY);
        aabb.add(0.0, lastGoY, 0.0);
        if (lastGoY != goY) {
            instance.speed.setY(0.0);
        }
        boolean inWater = false;
        for (AABBElement element : aabbs) {
            if (aabb.overlay(element.aabb)) {
                element.collision.inside(instance, delta);
                if (element.collision.isLiquid()) {
                    inWater = true;
                }
            }
        }
        instance.speed.plusZ(-gravitationMultiplier * delta *
                gravitation);
        instance.speed.div(1.0 + airFriction * delta);
        if (inWater) {
            instance.speed.div(1.0 + waterFriction * delta);
        } else if (ground) {
            instance.speed.setZ(0.0);
            instance.speed.div(1.0 + groundFriction * delta * gravitation);
        }
        return ground;
    }
}
