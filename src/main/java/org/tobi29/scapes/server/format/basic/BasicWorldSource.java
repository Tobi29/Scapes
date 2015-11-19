package org.tobi29.scapes.server.format.basic;

import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.graphics.PNG;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.format.WorldFormat;
import org.tobi29.scapes.server.format.WorldSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class BasicWorldSource implements WorldSource {
    private final Path path;
    private final TagStructure tagStructure;

    public BasicWorldSource(Path path) throws IOException {
        this.path = path;
        Path data = path.resolve("Data.stag");
        if (Files.exists(data)) {
            tagStructure = FileUtil.readReturn(data, TagStructureBinary::read);
        } else {
            tagStructure = new TagStructure();
        }
    }

    @Override
    public void init(long seed, List<Path> plugins) throws IOException {
        Files.createDirectories(path);
        tagStructure.setLong("Seed", seed);
        Path pluginsDir = path.resolve("plugins");
        Files.createDirectories(pluginsDir);
        for (Path plugin : plugins) {
            Files.copy(plugin, pluginsDir.resolve(plugin.getFileName()));
        }
    }

    @Override
    public void panorama(Image[] images) throws IOException {
        if (images.length != 6) {
            throw new IllegalArgumentException("6 panorama images required");
        }
        for (int i = 0; i < 6; i++) {
            int j = i;
            FileUtil.write(path.resolve("Panorama" + i + ".png"),
                    streamOut -> PNG.encode(images[j], streamOut, 9, false));
        }
    }

    @Override
    public Optional<Image[]> panorama() throws IOException {
        Image[] array = new Image[6];
        for (int i = 0; i < 6; i++) {
            Path background = path.resolve("Panorama" + i + ".png");
            if (Files.exists(background)) {
                array[i] = FileUtil.readReturn(background,
                        stream -> PNG.decode(stream, BufferCreator::bytes));
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(array);
    }

    @Override
    public WorldFormat open(ScapesServer server) throws IOException {
        return new BasicWorldFormat(path, tagStructure);
    }

    @Override
    public void close() throws IOException {
        FileUtil.write(path.resolve("Data.stag"),
                streamOut -> TagStructureBinary.write(tagStructure, streamOut));
    }
}
