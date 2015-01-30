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

package org.tobi29.scapes.engine.utils.tests;

import org.junit.Assert;
import org.junit.Test;
import org.tobi29.scapes.engine.utils.CompressionUtil;
import org.tobi29.scapes.engine.utils.tests.util.RandomInput;

import java.io.IOException;

public class CompressionUtilTest {
    @Test
    public void testCompressArray() throws IOException {
        for (byte[] array : RandomInput.createRandomArrays(32, 8)) {
            byte[] compressed = CompressionUtil.compress(array);
            byte[] decompressed = CompressionUtil.decompress(compressed);
            Assert.assertArrayEquals("Data not equal after decompression",
                    array, decompressed);
            byte[] recompressed = CompressionUtil.compress(decompressed);
            Assert.assertArrayEquals("Data not equal after recompression",
                    compressed, recompressed);
        }
    }
}
