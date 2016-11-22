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

package org.tobi29.scapes.plugins

import java8.util.Spliterators
import java.security.*
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadLocalRandom

object Sandbox {
    private val PACKAGE_WHITELIST = arrayOf("java", "kotlin",
            "org.tobi29.scapes", "org.slf4j", "java8")
    private var sandboxed = false

    @SuppressWarnings("CustomSecurityManager")
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
                if (PACKAGE_WHITELIST.none { pkg.startsWith(it) }) {
                    checkPermission(
                            RuntimePermission("scapes.restrictedPkg"))
                }
            }
        })
    }

    fun preload() {
        // Preload these to avoid permission errors when plugins trip into these
        Spliterators.emptySpliterator<Any>()
        ForkJoinPool.commonPool()
        ThreadLocalRandom.current()
    }
}
