package org.tobi29.scapes.server.format.mariadb;

import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.server.format.WorldSource;
import org.tobi29.scapes.server.format.spi.WorldSourceProvider;

import java.io.IOException;

public class MariaDBWorldSourceProvider implements WorldSourceProvider {
    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String configID() {
        return "MariaDB";
    }

    @Override
    public WorldSource get(FilePath path, TagStructure config)
            throws IOException {
        String address = config.getString("Address");
        int port = config.getInteger("Port");
        String name = config.getString("Name");
        String user = config.getString("User");
        String password = config.getString("Password");
        return new MariaDBWorldSource(path, address, port, name, user,
                password);
    }
}
