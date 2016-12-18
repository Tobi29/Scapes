/*
 * Copyright 2012-2016 Tobi29
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

package org.tobi29.scapes.plugins.tests

import org.junit.Assert
import org.junit.Test
import org.tobi29.scapes.plugins.Sandbox

class SandboxTests {
    @Test
    fun testExactMatches() {
        val pkgs = setOf("a.b.c", "a.b.c.d", "a.b.d")
        val pairs = pkgs.map { Pair(it, Sandbox.packageAccess(it)) }
        for (pkg in pkgs) {
            for (check in pairs) {
                val matches = check.second(pkg)
                if (pkg == check.first) {
                    Assert.assertTrue("Equal package does not match", matches)
                } else {
                    Assert.assertFalse("Not equal package matches", matches)
                }
            }
        }
    }

    @Test
    fun testWildcardMatches() {
        val access = Sandbox.packageAccess("a.b.*")
        Assert.assertTrue("Equal package does not match", access("a.b.c"))
        Assert.assertTrue("Equal package does not match", access("a.b.d"))
        Assert.assertFalse("Not equal package matches", access("a.c.d"))
        Assert.assertFalse("Shorter package matches", access("a.b"))
        Assert.assertFalse("Longer package matches", access("a.b.c.d"))
    }

    @Test
    fun testDoubleWildcardMatches() {
        val access = Sandbox.packageAccess("a.b.**")
        Assert.assertTrue("Equal package does not match", access("a.b.c"))
        Assert.assertTrue("Equal package does not match", access("a.b.d"))
        Assert.assertTrue("Shorter package does not matches", access("a.b.c.d"))
        Assert.assertTrue("Longer package does not matches", access("a.b.c.d"))
        Assert.assertFalse("Not equal package matches", access("a.c.d"))
        Assert.assertFalse("Twice shorter package matches", access("a"))
    }
}
