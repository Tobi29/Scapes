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

package org.tobi29.scapes.entity.skin;

import org.tobi29.scapes.engine.utils.io.ChecksumUtil;

public class ServerSkin {
    private final byte[] array;
    private final String checksum;

    public ServerSkin(byte... array) {
        this.array = array;
        checksum = ChecksumUtil.getChecksum(array);
    }

    public byte[] getImage() {
        return array;
    }

    public String getChecksum() {
        return checksum;
    }
}
