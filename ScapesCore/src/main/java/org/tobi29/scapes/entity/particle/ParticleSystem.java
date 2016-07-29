package org.tobi29.scapes.entity.particle;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.Sync;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.task.Joiner;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ParticleSystem {
    private final WorldClient world;
    @SuppressWarnings("rawtypes")
    private final Map<Class<? extends ParticleEmitter>, ParticleEmitter<?>>
            emitters = new ConcurrentHashMap<>();
    private final Joiner joiner;

    public ParticleSystem(WorldClient world, double tps) {
        this.world = world;
        joiner = world.game().engine().taskExecutor().runTask(joiner -> {
            Sync sync = new Sync(tps, 0, false, "Particles");
            sync.init();
            while (!joiner.marked()) {
                update(sync.delta());
                sync.cap(joiner);
            }
        }, "Particles", TaskExecutor.Priority.MEDIUM);
    }

    public <P extends ParticleEmitter<?>> void register(P emitter) {
        emitters.put(emitter.getClass(), emitter);
    }

    @SuppressWarnings("unchecked")
    public <P extends ParticleEmitter<?>> P emitter(Class<P> clazz) {
        ParticleEmitter<?> emitter = emitters.get(clazz);
        if (emitter == null) {
            throw new IllegalStateException("Particle emitter not registered");
        }
        return (P) emitter;
    }

    public void update(double delta) {
        Streams.forEach(emitters.values(), emitter -> {
            emitter.poll();
            emitter.update(delta);
        });
    }

    public void render(GL gl, Cam cam) {
        Streams.forEach(emitters.values(), emitter -> {
            emitter.pollRender();
            emitter.render(gl, cam);
        });
    }

    public WorldClient world() {
        return world;
    }

    public void dispose() {
        joiner.join();
    }
}
