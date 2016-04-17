package org.tobi29.scapes.vanilla.basics.entity.particle;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.RenderType;
import org.tobi29.scapes.engine.opengl.VAO;
import org.tobi29.scapes.engine.opengl.VAOUtility;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.particle.ParticleEmitter;
import org.tobi29.scapes.entity.particle.ParticleInstance;
import org.tobi29.scapes.entity.particle.ParticleSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ParticleEmitterLightning
        extends ParticleEmitter<ParticleInstanceLightning> {
    private final VAO[] vaos;

    public ParticleEmitterLightning(ParticleSystem system) {
        super(system, new ParticleInstanceLightning[256],
                ParticleInstanceLightning::new);
        vaos = new VAO[16];
        for (int i = 0; i < vaos.length; i++) {
            List<Line> lines = createLighting();
            float[] vertex = new float[lines.size() * 6];
            float[] normal = new float[lines.size() * 6];
            int j = 0;
            for (Line line : lines) {
                vertex[j] = line.start.floatX();
                normal[j++] = 0.0f;
                vertex[j] = line.start.floatY();
                normal[j++] = 0.0f;
                vertex[j] = line.start.floatZ();
                normal[j++] = 1.0f;
                vertex[j] = line.end.floatX();
                normal[j++] = 0.0f;
                vertex[j] = line.end.floatY();
                normal[j++] = 0.0f;
                vertex[j] = line.end.floatZ();
                normal[j++] = 1.0f;
            }
            int[] index = new int[(lines.size() << 1)];
            for (j = 0; j < index.length; j++) {
                index[j] = j;
            }
            vaos[i] = VAOUtility
                    .createVNI(vertex, normal, index, RenderType.LINES);
        }
    }

    private static List<Line> createLighting() {
        Random random = ThreadLocalRandom.current();
        List<Line> lines = new ArrayList<>();
        double x = 0, y = 0;
        Vector3 start = Vector3d.ZERO;
        for (double z = 10.0; z < 100.0; z += random.nextDouble() * 4 + 2) {
            x += random.nextDouble() * 6.0 - 3.0;
            y += random.nextDouble() * 6.0 - 3.0;
            Vector3 end = new Vector3d(x, y, z);
            lines.add(new Line(start, end));
            start = end;
            if (random.nextInt(2) == 0 && z > 40) {
                createLightingArm(lines, x, y, z);
            }
        }
        return lines;
    }

    private static void createLightingArm(List<Line> lines, double x, double y,
            double z) {
        Random random = ThreadLocalRandom.current();
        Vector3 start = new Vector3d(x, y, z);
        double dir = random.nextDouble() * FastMath.TWO_PI;
        double xs = FastMath.cosTable(dir);
        double ys = FastMath.sinTable(dir);
        dir = FastMath.pow(random.nextDouble(), 6.0) * 20.0 + 0.2;
        for (int i = 0; i < random.nextInt(30) + 4; i++) {
            x += xs * random.nextDouble() * dir;
            y += ys * random.nextDouble() * dir;
            Vector3 end = new Vector3d(x, y, z);
            lines.add(new Line(start, end));
            start = end;
            z -= random.nextDouble() * 4.0 + 2.0;
            if (z < 20.0) {
                return;
            }
        }
    }

    public int maxVAO() {
        return vaos.length;
    }

    @Override
    public void update(double delta) {
        if (!hasAlive) {
            return;
        }
        boolean hasAlive = false;
        for (ParticleInstanceLightning instance : instances) {
            if (instance.state != ParticleInstance.State.ALIVE) {
                continue;
            }
            hasAlive = true;
            instance.time -= delta;
            if (instance.time <= 0.0) {
                instance.state = ParticleInstance.State.DEAD;
            }
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
        Shader shader =
                gl.shaders().get("VanillaBasics:shader/ParticleLightning", gl);
        gl.textures().unbind(gl);
        for (ParticleInstanceLightning instance : instances) {
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
                vaos[instance.vao].render(gl, shader);
                matrixStack.pop();
            }
        }
    }

    private static class Line {
        private final Vector3 start, end;

        public Line(Vector3 start, Vector3 end) {
            this.start = start;
            this.end = end;
        }
    }
}
