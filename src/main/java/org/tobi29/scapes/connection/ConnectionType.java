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

package org.tobi29.scapes.connection;

import java.util.HashMap;

public enum ConnectionType {
    GET_INFO((byte) 1),
    PLAY((byte) 11),
    CONTROL_PANEL((byte) 101);
    private static final HashMap<Byte, ConnectionType> VALUES = new HashMap<>();
    private final byte data;

    ConnectionType(byte data) {
        this.data = data;
    }

    public static ConnectionType get(byte data) {
        return VALUES.get(data);
    }

    public byte getData() {
        return data;
    }

    static {
        for (ConnectionType type : values()) {
            VALUES.put(type.data, type);
        }
    }
}
