package org.tobi29.scapes.entity.particle;

import java8.util.function.Consumer;
import java8.util.function.Supplier;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.utils.ArrayUtil;
import org.tobi29.scapes.engine.utils.graphics.Cam;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class ParticleEmitter<P extends ParticleInstance> {
    protected final int maxInstances;
    protected final ParticleSystem system;
    protected final P[] instances;
    private final Queue<Consumer<P>> queue = new ConcurrentLinkedQueue<>();
    private final Queue<P> activateQueue = new ConcurrentLinkedQueue<>();
    protected boolean hasAlive;
    private int lastFree;

    protected ParticleEmitter(ParticleSystem system, P[] instances,
            Supplier<P> instanceSupplier) {
        this.system = system;
        this.instances = instances;
        maxInstances = instances.length;
        ArrayUtil.fill(instances, instanceSupplier);
    }

    public void add(Consumer<P> consumer) {
        queue.add(consumer);
    }

    public void poll() {
        while (!queue.isEmpty()) {
            Consumer<P> consumer = queue.poll();
            if (!findInstance(consumer)) {
                // Drain queue and exit because no particles left
                queue.clear();
                return;
            }
        }
    }

    public void pollRender() {
        while (!activateQueue.isEmpty()) {
            P instance = activateQueue.poll();
            assert instance.state == ParticleInstance.State.NEW;
            instance.state = ParticleInstance.State.ALIVE;
            hasAlive = true;
        }
    }

    private boolean findInstance(Consumer<P> consumer) {
        for (int i = 0; i < instances.length; i++) {
            int j = (i + lastFree) % instances.length;
            P instance = instances[j];
            if (instance.state == ParticleInstance.State.DEAD) {
                lastFree = j;
                initInstance(instance, consumer);
                instance.state = ParticleInstance.State.NEW;
                activateQueue.add(instance);
                return true;
            }
        }
        return false;
    }

    protected void initInstance(P instance, Consumer<P> consumer) {
        consumer.accept(instance);
    }

    public abstract void update(double delta);

    public abstract void render(GL gl, Cam cam);
}
