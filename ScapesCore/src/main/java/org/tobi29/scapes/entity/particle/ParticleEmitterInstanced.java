package org.tobi29.scapes.entity.particle;

import java8.util.function.Supplier;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.RenderType;
import org.tobi29.scapes.engine.opengl.VAOHybrid;
import org.tobi29.scapes.engine.opengl.VBO;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.utils.graphics.Cam;

import java.nio.ByteBuffer;

public abstract class ParticleEmitterInstanced<P extends ParticleInstance>
        extends ParticleEmitter<P> {
    protected final VAOHybrid vao;
    protected final VBO vboStream;
    protected final Texture texture;
    protected final ByteBuffer buffer;

    protected ParticleEmitterInstanced(ParticleSystem system, Texture texture,
            VBO vbo, VBO vboStream, RenderType renderType, P[] instances,
            Supplier<P> instanceSupplier) {
        super(system, instances, instanceSupplier);
        this.texture = texture;
        this.vboStream = vboStream;
        vao = new VAOHybrid(vbo, vboStream, renderType);
        buffer = system.world().game().engine()
                .allocate(vboStream.stride() * maxInstances);
    }

    @Override
    public void render(GL gl, Cam cam) {
        if (!hasAlive) {
            return;
        }
        texture.bind(gl);
        Shader shader = prepareShader(gl, cam);
        buffer.clear();
        vao.ensureStored(gl);
        int count = prepareBuffer(cam);
        if (count > 0) {
            buffer.flip();
            vboStream.replaceBuffer(gl, buffer);
            vao.renderInstanced(gl, shader, 6, count);
        }
    }

    protected abstract Shader prepareShader(GL gl, Cam cam);

    protected abstract int prepareBuffer(Cam cam);
}
