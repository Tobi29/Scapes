/*
 * Copyright 2012-2017 Tobi29
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

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.tobi29.assertions.shouldEqual
import org.tobi29.scapes.plugins.Sandbox

object SandboxTests : Spek({
    describe("package matching") {
        given("a matcher for a.b.c, a.b.c.d, a.b.d") {
            val pkgs = setOf("a.b.c", "a.b.c.d", "a.b.d")
            val pairs = pkgs.map { Pair(it, Sandbox.packageAccess(it)) }
            on("checking if all those can match themselves") {
                for (pkg in pkgs) {
                    for (check in pairs) {
                        val matches = check.second(pkg)
                        if (pkg == check.first) {
                            it("should match") {
                                matches shouldEqual true
                            }
                        } else {
                            it("should not match") {
                                matches shouldEqual false
                            }
                        }
                    }
                }
            }
        }
        given("a matcher for a.b.*") {
            val access = Sandbox.packageAccess("a.b.*")
            val pkgs = listOf(
                    Pair("a.b.c", true),
                    Pair("a.b.d", true),
                    Pair("a.c.d", false),
                    Pair("a.b", false),
                    Pair("a.b.c.d", false)
            )
            for ((pkg, expected) in pkgs) {
                on("checking $pkg") {
                    it("should match") {
                        access(pkg) shouldEqual expected
                    }
                }
            }
        }
        given("a matcher for a.b.**") {
            val access = Sandbox.packageAccess("a.b.**")
            val pkgs = listOf(
                    Pair("a.b", true),
                    Pair("a.b.c", true),
                    Pair("a.b.d", true),
                    Pair("a.b.c.d", true),
                    Pair("a.c.d", false),
                    Pair("a", false)
            )
            for ((pkg, expected) in pkgs) {
                on("checking $pkg") {
                    it("should match") {
                        access(pkg) shouldEqual expected
                    }
                }
            }
        }
    }
})
