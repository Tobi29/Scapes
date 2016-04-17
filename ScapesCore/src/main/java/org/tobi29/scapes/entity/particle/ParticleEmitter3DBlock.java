package org.tobi29.scapes.entity.particle;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.AABB;

public class ParticleEmitter3DBlock
        extends ParticleEmitter<ParticleInstance3DBlock> {
    private static final float SIZE = 0.5f;

    public ParticleEmitter3DBlock(ParticleSystem system) {
        super(system, new ParticleInstance3DBlock[256],
                ParticleInstance3DBlock::new);
    }

    @Override
    public void update(double delta) {
        if (!hasAlive) {
            return;
        }
        AABB aabb = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        float gravitation = (float) system.world().gravity();
        TerrainClient terrain = system.world().terrain();
        boolean hasAlive = false;
        for (ParticleInstance3DBlock instance : instances) {
            if (instance.state != ParticleInstance.State.ALIVE) {
                continue;
            }
            hasAlive = true;
            instance.time -= delta;
            if (instance.time <= 0.0) {
                instance.state = ParticleInstance.State.DEAD;
                continue;
            }
            aabb.minX = instance.pos.doubleX() - SIZE;
            aabb.minY = instance.pos.doubleY() - SIZE;
            aabb.minZ = instance.pos.doubleZ() - SIZE;
            aabb.maxX = instance.pos.doubleX() + SIZE;
            aabb.maxY = instance.pos.doubleY() + SIZE;
            aabb.maxZ = instance.pos.doubleZ() + SIZE;
            if (ParticlePhysics
                    .update(delta, instance, terrain, aabb, gravitation, 1.0f,
                            0.2f, 0.4f, 8.0f)) {
                instance.rotationSpeed.div(1.0 + 0.4 * delta * gravitation);
            }
            instance.rotation
                    .plus(instance.rotationSpeed.now().multiply(delta));
        }
        this.hasAlive = hasAlive;
    }

    @Override
    public void render(GL gl, Cam cam) {
        if (!hasAlive) {
            return;
        }
        WorldClient world = system.world();
        TerrainClient terrain = world.terrain();
        Shader shader = gl.shaders().get("Scapes:shader/Entity", gl);
        for (ParticleInstance3DBlock instance : instances) {
            if (instance.state != ParticleInstance.State.ALIVE) {
                continue;
            }
            int x = instance.pos.intX(), y = instance.pos.intY(), z =
                    instance.pos.intZ();
            BlockType type = terrain.type(x, y, z);
            if (!type.isSolid(world.terrain(), x, y, z) ||
                    type.isTransparent(world.terrain(), x, y, z)) {
                float posRenderX = (float) (instance.pos.doubleX() -
                        cam.position.doubleX());
                float posRenderY = (float) (instance.pos.doubleY() -
                        cam.position.doubleY());
                float posRenderZ = (float) (instance.pos.doubleZ() -
                        cam.position.doubleZ());
                MatrixStack matrixStack = gl.matrixStack();
                Matrix matrix = matrixStack.push();
                matrix.translate(posRenderX, posRenderY, posRenderZ);
                matrix.rotate(instance.rotation.floatZ(), 0, 0, 1);
                matrix.rotate(instance.rotation.floatX(), 1, 0, 0);
                gl.setAttribute2f(4,
                        world.terrain().blockLight(x, y, z) / 15.0f,
                        world.terrain().sunLight(x, y, z) / 15.0f);
                instance.item.material()
                        .render(instance.item, gl, shader, 1.0f, 1.0f, 1.0f,
                                1.0f);
                matrixStack.pop();
            }
        }
    }
}
