package org.tobi29.scapes.entity.particle;

import java8.util.function.Consumer;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.EnvironmentClient;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.client.states.scenes.SceneScapesVoxelWorld;
import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.utils.BufferCreatorNative;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.matrix.Matrix4f;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParticleEmitterTransparent
        extends ParticleEmitterInstanced<ParticleInstanceTransparent> {
    private static final float[] EMPTY_FLOAT = {};
    private final ParticleInstanceTransparent[] instancesSorted;
    private final Matrix4f matrix;

    public ParticleEmitterTransparent(ParticleSystem system, Texture texture) {
        super(system, texture, createVBO(), createVBOStream(),
                RenderType.TRIANGLES, new ParticleInstanceTransparent[10240],
                ParticleInstanceTransparent::new);
        matrix = new Matrix4f(BufferCreatorNative::bytes);
        instancesSorted = new ParticleInstanceTransparent[maxInstances];
        System.arraycopy(instances, 0, instancesSorted, 0, maxInstances);
    }

    private static VBO createVBO() {
        List<VBO.VBOAttribute> vboAttributes = new ArrayList<>();
        vboAttributes.add(new VBO.VBOAttribute(OpenGL.VERTEX_ATTRIBUTE, 3,
                new float[]{-1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f,
                        1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, -1.0f, 1.0f, 0.0f,
                        1.0f}, false, 0, VertexType.HALF_FLOAT));
        vboAttributes.add(new VBO.VBOAttribute(OpenGL.TEXTURE_ATTRIBUTE, 2,
                new float[]{0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f, 1.0f}, false, 0, VertexType.FLOAT));
        return new VBO(vboAttributes, 6);
    }

    private static VBO createVBOStream() {
        List<VBO.VBOAttribute> vboAttributes = new ArrayList<>();
        vboAttributes.add(new VBO.VBOAttribute(OpenGL.COLOR_ATTRIBUTE, 4,
                EMPTY_FLOAT, false, 1, VertexType.HALF_FLOAT));
        vboAttributes.add(new VBO.VBOAttribute(4, 2, EMPTY_FLOAT, false, 1,
                VertexType.FLOAT));
        vboAttributes.add(new VBO.VBOAttribute(5, 4, EMPTY_FLOAT, false, 1,
                VertexType.FLOAT));
        vboAttributes.add(new VBO.VBOAttribute(6, 4, EMPTY_FLOAT, false, 1,
                VertexType.FLOAT));
        vboAttributes.add(new VBO.VBOAttribute(7, 4, EMPTY_FLOAT, false, 1,
                VertexType.FLOAT));
        vboAttributes.add(new VBO.VBOAttribute(8, 4, EMPTY_FLOAT, false, 1,
                VertexType.FLOAT));
        vboAttributes.add(new VBO.VBOAttribute(9, 4, EMPTY_FLOAT, false, 1,
                VertexType.FLOAT));
        return new VBO(vboAttributes, 0);
    }

    @Override
    protected Shader prepareShader(GL gl, Cam cam) {
        WorldClient world = system.world();
        SceneScapesVoxelWorld scene = world.scene();
        MobPlayerClientMain player = world.player();
        EnvironmentClient environment = world.environment();
        float sunLightReduction = environment
                .sunLightReduction(cam.position.doubleX(),
                        cam.position.doubleY()) / 15.0f;
        float playerLight = FastMath.max(
                player.leftWeapon().material().playerLight(player.leftWeapon()),
                player.rightWeapon().material()
                        .playerLight(player.rightWeapon()));
        Shader shader =
                gl.shaders().get("Scapes:shader/ParticleTransparent", gl);
        shader.setUniform3f(4, scene.fogR(), scene.fogG(), scene.fogB());
        shader.setUniform1f(5, scene.fogDistance() * scene.renderDistance());
        shader.setUniform1i(6, 1);
        shader.setUniform1f(7, sunLightReduction);
        shader.setUniform1f(8, playerLight);
        return shader;
    }

    @Override
    protected int prepareBuffer(Cam cam) {
        Vector3 camPos = cam.position.now();
        WorldClient world = system.world();
        TerrainClient terrain = world.terrain();
        for (ParticleInstanceTransparent instance : instancesSorted) {
            if (instance.state != ParticleInstance.State.ALIVE) {
                continue;
            }
            instance.posRender = instance.pos.now();
        }
        Arrays.sort(instancesSorted, (instance1, instance2) -> {
            double distance1 =
                    FastMath.pointDistanceSqr(instance1.posRender, camPos);
            double distance2 =
                    FastMath.pointDistanceSqr(instance2.posRender, camPos);
            return distance1 == distance2 ? 0 : distance1 < distance2 ? 1 : -1;
        });
        int count = 0;
        for (ParticleInstanceTransparent instance : instancesSorted) {
            if (instance.state != ParticleInstance.State.ALIVE) {
                continue;
            }
            int x = instance.posRender.intX(), y = instance.posRender.intY(),
                    z = instance.posRender.intZ();
            BlockType type = terrain.type(x, y, z);
            if (!type.isSolid(terrain, x, y, z) ||
                    type.isTransparent(terrain, x, y, z)) {
                float posRenderX = (float) (instance.posRender.doubleX() -
                        camPos.doubleX());
                float posRenderY = (float) (instance.posRender.doubleY() -
                        camPos.doubleY());
                float posRenderZ = (float) (instance.posRender.doubleZ() -
                        camPos.doubleZ());
                float yaw = FastMath.atan2Fast(-posRenderY, -posRenderX);
                float pitch = FastMath.atan2Fast(posRenderZ,
                        (float) FastMath.length(posRenderX, posRenderY));
                matrix.identity();
                matrix.translate(posRenderX, posRenderY, posRenderZ);
                matrix.rotateRad(yaw + (float) FastMath.HALF_PI, 0, 0, 1);
                matrix.rotateRad(pitch, 1, 0, 0);
                matrix.rotateRad(yaw + instance.dir, 0, 1, 0);
                float progress = instance.time / instance.timeMax;
                float size = FastMath.mix(instance.sizeEnd, instance.sizeStart,
                        progress);
                float r =
                        FastMath.mix(instance.rEnd, instance.rStart, progress);
                float g =
                        FastMath.mix(instance.gEnd, instance.gStart, progress);
                float b =
                        FastMath.mix(instance.bEnd, instance.bStart, progress);
                float a =
                        FastMath.mix(instance.aEnd, instance.aStart, progress);
                matrix.scale(size, size, size);
                buffer.putShort(FastMath.convertFloatToHalf(r));
                buffer.putShort(FastMath.convertFloatToHalf(g));
                buffer.putShort(FastMath.convertFloatToHalf(b));
                buffer.putShort(FastMath.convertFloatToHalf(a));
                buffer.putFloat(terrain.blockLight(x, y, z) / 15.0f);
                buffer.putFloat(terrain.sunLight(x, y, z) / 15.0f);
                buffer.put(matrix.getByteBuffer());
                buffer.putFloat(instance.textureOffset.floatX());
                buffer.putFloat(instance.textureOffset.floatY());
                buffer.putFloat(instance.textureSize.floatX());
                buffer.putFloat(instance.textureSize.floatY());
                count++;
            }
        }
        return count;
    }

    @Override
    protected void initInstance(ParticleInstanceTransparent instance,
            Consumer<ParticleInstanceTransparent> consumer) {
        super.initInstance(instance, consumer);
        instance.timeMax = instance.time;
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
        for (ParticleInstanceTransparent instance : instances) {
            if (instance.state != ParticleInstance.State.ALIVE) {
                continue;
            }
            hasAlive = true;
            instance.time -= delta;
            if (instance.time <= 0.0) {
                instance.state = ParticleInstance.State.DEAD;
                continue;
            }
            if (!instance.physics) {
                continue;
            }
            aabb.minX = instance.pos.doubleX() - instance.sizeStart;
            aabb.minY = instance.pos.doubleY() - instance.sizeStart;
            aabb.minZ = instance.pos.doubleZ() - instance.sizeStart;
            aabb.maxX = instance.pos.doubleX() + instance.sizeStart;
            aabb.maxY = instance.pos.doubleY() + instance.sizeStart;
            aabb.maxZ = instance.pos.doubleZ() + instance.sizeStart;
            ParticlePhysics.update(delta, instance, terrain, aabb, gravitation,
                    instance.gravitationMultiplier, instance.airFriction,
                    instance.groundFriction, instance.waterFriction);
        }
        this.hasAlive = hasAlive;
    }
}
