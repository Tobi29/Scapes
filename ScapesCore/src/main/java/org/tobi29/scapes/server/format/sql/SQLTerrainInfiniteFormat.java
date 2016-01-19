/*
 * Copyright 2012-2015 Tobi29
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tobi29.scapes.server.format.sql;

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.sql.SQLDatabase;
import org.tobi29.scapes.engine.sql.SQLQuery;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.io.ByteBufferStream;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.binary.TagStructureBinary;
import org.tobi29.scapes.engine.utils.math.vector.Vector2i;
import org.tobi29.scapes.server.format.TerrainInfiniteFormat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SQLTerrainInfiniteFormat implements TerrainInfiniteFormat {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SQLTerrainInfiniteFormat.class);
    private final ByteBufferStream stream = new ByteBufferStream();
    private final SQLDatabase database;
    private final String table;
    private final SQLQuery getChunk;

    public SQLTerrainInfiniteFormat(SQLDatabase database, String table) {
        this.database = database;
        this.table = table;
        getChunk = database.compileQuery(table, new String[]{"Data"}, "Pos");
    }

    @Override
    public synchronized List<Optional<TagStructure>> chunkTags(
            List<Vector2i> chunks) {
        List<Optional<TagStructure>> tagStructures =
                new ArrayList<>(chunks.size());
        for (Vector2i chunk : chunks) {
            try {
                tagStructures.add(chunkTag(chunk.intX(), chunk.intY()));
            } catch (IOException e) {
                LOGGER.error("Failed to load chunk: {}", e.toString());
                tagStructures.add(Optional.empty());
            }
        }
        return tagStructures;
    }

    @Override
    public synchronized void putChunkTags(
            List<Pair<Vector2i, TagStructure>> chunks) throws IOException {
        List<Object[]> values = new ArrayList<>(chunks.size());
        for (Pair<Vector2i, TagStructure> chunk : chunks) {
            long pos = pos(chunk.a.intX(), chunk.a.intY());
            TagStructureBinary.write(chunk.b, stream, (byte) 1);
            stream.buffer().flip();
            byte[] array = new byte[stream.buffer().remaining()];
            stream.buffer().get(array);
            stream.buffer().clear();
            values.add(new Object[]{pos, array});
            if (values.size() >= 64) {
                database.replace(table, new String[]{"Pos", "Data"}, values);
                values.clear();
            }
        }
        if (!values.isEmpty()) {
            database.replace(table, new String[]{"Pos", "Data"}, values);
        }
    }

    @Override
    public void dispose() {
    }

    private Optional<TagStructure> chunkTag(int x, int y) throws IOException {
        long pos = pos(x, y);
        List<Object[]> rows = getChunk.run(pos);
        if (!rows.isEmpty()) {
            Object[] row = rows.get(0);
            if (row[0] instanceof byte[]) {
                byte[] array = (byte[]) row[0];
                TagStructure tagStructure = TagStructureBinary
                        .read(new ByteBufferStream(ByteBuffer.wrap(array)));
                return Optional.of(tagStructure);
            }
        }
        return Optional.empty();
    }

    private long pos(int x, int y) {
        long xx = x;
        if (xx < 0) {
            xx += 0x100000000L;
        }
        long yy = y;
        if (yy < 0) {
            yy += 0x100000000L;
        }
        return yy << 32 | xx;
    }
}
