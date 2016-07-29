package org.tobi29.scapes.entity.particle;

import java8.util.function.Supplier;
import org.tobi29.scapes.engine.graphics.*;
import org.tobi29.scapes.engine.utils.graphics.Cam;

import java.nio.ByteBuffer;
import java.util.List;

public abstract class ParticleEmitterInstanced<P extends ParticleInstance>
        extends ParticleEmitter<P> {
    protected final ModelHybrid vao;
    protected final Texture texture;
    protected final ByteBuffer buffer;

    protected ParticleEmitterInstanced(ParticleSystem system, Texture texture,
            List<ModelAttribute> attributes, int length,
            List<ModelAttribute> attributesStream, RenderType renderType,
            P[] instances, Supplier<P> instanceSupplier) {
        super(system, instances, instanceSupplier);
        this.texture = texture;
        vao = system.world().game().engine().graphics()
                .createModelHybrid(attributes, length, attributesStream, 0,
                        renderType);
        buffer = system.world().game().engine()
                .allocate(vao.strideStream() * maxInstances);
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
            vao.bufferStream(gl, buffer);
            vao.renderInstanced(gl, shader, 6, count);
        }
    }

    protected abstract Shader prepareShader(GL gl, Cam cam);

    protected abstract int prepareBuffer(Cam cam);
}
