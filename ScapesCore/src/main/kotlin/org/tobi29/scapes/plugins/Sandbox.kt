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

package org.tobi29.scapes.plugins

import java.security.*
import java.util.concurrent.ThreadLocalRandom

object Sandbox {
    private val whitelist = packageAccess("java.**", "kotlin.**",
            "org.tobi29.scapes.**", "scapes.plugin.**", "org.slf4j.**",
            "java8.**")
    private val permission = RuntimePermission("scapes.restrictedPkg")
    private var sandboxed = false

    fun sandbox() {
        if (sandboxed) {
            return
        }
        sandboxed = true
        preload()
        Policy.setPolicy(object : Policy() {
            override fun getPermissions(codesource: CodeSource?): Permissions {
                val permissions = Permissions()
                permissions.add(AllPermission())
                return permissions
            }

            override fun getPermissions(domain: ProtectionDomain?): Permissions {
                if (domain == null || domain.classLoader is PluginClassLoader) {
                    return Permissions()
                }
                val permissions = getPermissions(domain.codeSource)
                val domainPermissions = domain.permissions
                synchronized(domainPermissions) {
                    val domainPermission = domainPermissions.elements()
                    while (domainPermission.hasMoreElements()) {
                        permissions.add(domainPermission.nextElement())
                    }
                }
                return permissions
            }
        })
        System.setSecurityManager(object : SecurityManager() {
            override fun checkPackageAccess(pkg: String) {
                super.checkPackageAccess(pkg)
                if (!whitelist(pkg)) {
                    try {
                        checkPermission(permission)
                    } catch (e: AccessControlException) {
                        throw AccessControlException(
                                "Package access denied on: $pkg", permission)
                    }
                }
            }
        })
    }

    fun preload() {
        // Preload these to avoid permission errors when plugins trip into these
        ThreadLocalRandom.current()
    }

    fun packageAccess(vararg str: String): (String) -> Boolean {
        val check = compose(matchPackages(*str))
        return { check(it.split('.')) }
    }

    private fun compose(functions: List<(List<String>) -> Boolean>): (List<String>) -> Boolean {
        return { pkg -> functions.any { it(pkg) } }
    }

    private fun matchPackages(vararg str: String): List<(List<String>) -> Boolean> {
        return str.asSequence().map { matchPackage(it) }.toList()
    }

    private fun matchPackage(str: String): (List<String>) -> Boolean {
        val split = str.split('.').asSequence()
                .map<String, (String?) -> PkgMatchResult> {
                    when (it) {
                        "*" -> { pkg ->
                            PkgMatchResult.CONTINUE
                        }
                        "**" -> { pkg ->
                            PkgMatchResult.MATCHES
                        }
                        else -> { pkg ->
                            if (it == pkg) {
                                PkgMatchResult.CONTINUE
                            } else {
                                PkgMatchResult.ABORT
                            }
                        }
                    }
                }.toList()

        return matcher@ { pkg ->
            val iterator = pkg.iterator()
            split.forEach {
                if (iterator.hasNext()) {
                    when (it(iterator.next())) {
                        PkgMatchResult.CONTINUE -> {
                        }
                        PkgMatchResult.MATCHES -> {
                            return@matcher true
                        }
                        PkgMatchResult.ABORT -> {
                            return@matcher false
                        }
                    }
                } else {
                    return@matcher when (it(null)) {
                        PkgMatchResult.CONTINUE -> false
                        PkgMatchResult.MATCHES -> true
                        PkgMatchResult.ABORT -> false
                    }
                }
            }
            !iterator.hasNext()
        }
    }

    private enum class PkgMatchResult {
        CONTINUE,
        MATCHES,
        ABORT
    }
}
