package org.tobi29.scapes.server.extension;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.extension.spi.ServerExtensionProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerExtensions {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ServerExtensions.class);
    private final List<ServerExtension> extensions = new ArrayList<>();
    private final ScapesServer server;
    private final Map<Class<? extends ServerEvent>, List<ServerListener<?>>>
            listeners = new ConcurrentHashMap<>();

    public ServerExtensions(ScapesServer server) {
        this.server = server;
    }

    public void init() {
        extensions.forEach(extension -> extension.init(this::listener));
        extensions.forEach(extension -> extension.initLate(this::listener));
    }

    public void loadExtensions() {
        for (ServerExtensionProvider provider : ServiceLoader
                .load(ServerExtensionProvider.class)) {
            try {
                ServerExtension extension = provider.create(server);
                if (extension != null) {
                    loadExtension(extension);
                }
            } catch (ServiceConfigurationError e) {
                LOGGER.warn("Unable to load input mode provider: {}",
                        e.toString());
            }
        }
    }

    public void loadExtension(ServerExtension extension) {
        LOGGER.info("Loaded extension: {}", extension.getClass().getName());
        extension.initEarly();
        extensions.add(extension);
    }

    private <E extends ServerEvent> void listener(Class<E> clazz,
            ServerListener<E> listener) {
        List<ServerListener<?>> list = listeners.get(clazz);
        if (list == null) {
            list = new ArrayList<>();
            listeners.put(clazz, list);
        }
        list.add(listener);
    }

    @SuppressWarnings("unchecked")
    public <E extends ServerEvent> void fireEvent(E event) {
        List<ServerListener<?>> list = listeners.get(event.getClass());
        if (list != null) {
            list.stream().map(listener -> (ServerListener<E>) listener)
                    .forEach(listener -> listener.event(event));
        }
    }

    public interface Registrar {
        <E extends ServerEvent> void listener(Class<E> clazz,
                ServerListener<E> listener);
    }
}
