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

import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.engine.math.threadLocalRandom
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.logging.KLogging
import java.io.FilePermission
import java.lang.reflect.ReflectPermission
import java.net.SocketPermission
import java.net.URLClassLoader
import java.security.*
import java.util.*

object Sandbox : KLogging() {
    private val whitelist = packageAccess("java.**", "kotlin.**",
            "kotlinx.**", "org.tobi29.scapes.**", "scapes.plugin.**",
            "org.slf4j.**")
    private val permission = RuntimePermission("scapes.restrictedPkg")
    private var sandboxed = false

    fun sandbox(accessibleDirectories: Iterable<FilePath>) {
        if (sandboxed) {
            return
        }
        sandboxed = true
        preload()
        val defaultPermissions = ArrayList<Permission>()

        // Using a proper application sandbox seems better for this
        logger.debug { "Game permission: File /- read,readlink" }
        defaultPermissions.add(FilePermission("/-", "read,readlink"))
        for (path in accessibleDirectories) {
            val pathStr = path.toAbsolutePath().toString()
            logger.debug { "Game permission: File $pathStr read,write,delete,readlink" }
            defaultPermissions.add(
                    FilePermission(pathStr, "read,write,delete,readlink"))
            // TODO: Handle - in path
            val wildcard = "${pathStr.removeSuffix("/")}/-"
            logger.debug { "Game permission: File $wildcard read,write,delete,readlink" }
            defaultPermissions.add(
                    FilePermission(wildcard, "read,write,delete,readlink"))
        }

        // Allow using all packages
        defaultPermissions.add(permission)

        // Allow getting metadata from files
        logger.debug { "Game permission: accessUserInformation" }
        defaultPermissions.add(RuntimePermission("accessUserInformation"))

        // Allow using plugin jars for assets
        logger.debug { "Game permission: getClassLoader" }
        defaultPermissions.add(RuntimePermission("getClassLoader"))

        // Allow using threads
        logger.debug { "Game permission: modifyThread" }
        defaultPermissions.add(RuntimePermission("modifyThread"))

        // Log4j2
        reflectPermission("javax.management.MBeanServerPermission",
                "createMBeanServer")?.let {
            logger.debug { "Game permission: MBeanServer createMBeanServer" }
            defaultPermissions.add(it)
        }
        reflectPermission("javax.management.MBeanPermission",
                "-#-[-]", "queryNames")?.let {
            logger.debug { "Game permission: MBean -#-[-] queryNames" }
            defaultPermissions.add(it)
        }
        reflectPermission("javax.management.MBeanPermission",
                "org.apache.logging.log4j.core.jmx.*",
                "queryNames,unregisterMBean")?.let {
            logger.debug { "Game permission: MBean org.apache.logging.log4j.core.jmx.* queryNames,unregisterMBean" }
            defaultPermissions.add(it)
        }

        // LWJGL
        logger.debug { "Game permission: accessClassInPackage.sun.misc" }
        defaultPermissions.add(
                RuntimePermission("accessClassInPackage.sun.misc"))
        defaultPermissions.add(
                RuntimePermission("accessDeclaredMembers"))
        defaultPermissions.add(
                ReflectPermission("suppressAccessChecks"))

        // Native libraries
        val loadLibraries = mutableListOf(
                "lwjgl",
                "lwjgl_opengl",
                "lwjgl_opengles",
                "lwjgl_stb",
                "lwjgl_tinyfd")
        System.getProperty("java.library.path")?.let {
            // TODO: Verify property
            loadLibraries.add("$it/libsqlitejdbc.so")
            logger.debug { "Game permission: File read $it/*" }
            defaultPermissions.add(FilePermission("$it/*", "read"))
        }
        for (loadLibrary in loadLibraries.map { "loadLibrary.$it" }) {
            logger.debug { "Game permission: $loadLibrary" }
            defaultPermissions.add(RuntimePermission(loadLibrary))
        }

        // Temp
        System.getProperty("java.io.tmpdir")?.let {
            logger.debug { "Game permission: File read,write,delete,readlink $it/-" }
            defaultPermissions.add(
                    FilePermission("$it/-", "read,write,delete,readlink"))
        }

        // Sandbox
        logger.debug { "Game permission: getProtectionDomain" }
        defaultPermissions.add(RuntimePermission("getProtectionDomain"))

        // Scapes
        logger.debug { "Game permission: scapes.*" }
        defaultPermissions.add(RuntimePermission("scapes.*"))
        logger.debug { "Game permission: createClassLoader" }
        defaultPermissions.add(RuntimePermission("createClassLoader"))
        logger.debug { "Game permission: closeClassLoader" }
        defaultPermissions.add(RuntimePermission("closeClassLoader"))
        logger.debug { "Game permission: Socket * resolve,connect,listen,accept" }
        defaultPermissions.add(
                SocketPermission("*", "resolve,connect,listen,accept"))
        logger.debug { "Game permission: accessClassInPackage.sun.security.pkcs12" }
        defaultPermissions.add(
                RuntimePermission("accessClassInPackage.sun.security.pkcs12"))
        logger.debug { "Game permission: accessClassInPackage.sun.security.jca" }
        defaultPermissions.add(
                RuntimePermission("accessClassInPackage.sun.security.jca"))
        logger.debug { "Game permission: accessClassInPackage.sun.security.util" }
        defaultPermissions.add(
                RuntimePermission("accessClassInPackage.sun.security.util"))
        logger.debug { "Game permission: accessClassInPackage.sun.security.ssl" }
        defaultPermissions.add(
                RuntimePermission("accessClassInPackage.sun.security.ssl"))
        logger.debug { "Game permission: accessClassInPackage.sun.security.rsa" }
        defaultPermissions.add(
                RuntimePermission("accessClassInPackage.sun.security.rsa"))
        logger.debug { "Game permission: accessClassInPackage.sun.security.internal.interfaces" }
        defaultPermissions.add(
                RuntimePermission(
                        "accessClassInPackage.sun.security.internal.interfaces"))
        logger.debug { "Game permission: accessClassInPackage.sun.security.internal.spec" }
        defaultPermissions.add(
                RuntimePermission(
                        "accessClassInPackage.sun.security.internal.spec"))
        logger.debug { "Game permission: Security putProviderProperty.SunJCE" }
        defaultPermissions.add(
                SecurityPermission("putProviderProperty.SunJCE"))
        logger.debug { "Game permission: Security putProviderProperty.SunJSSE" }
        defaultPermissions.add(
                SecurityPermission("putProviderProperty.SunJSSE"))

        // Allow reading various required properties
        val readProperties = listOf(
                "os.arch",
                "os.name",
                "os.version",
                "java.version",
                "java.vm.name",
                "java.vm.version",
                "java.vm.vendor",
                "java.library.path",
                "user.name",
                "java.io.tmpdir",
                "kotlin.test.is.pre.release",
                "kotlinx.coroutines.DefaultExecutor.keepAlive",
                "log4j2.*",
                "tika.config",
                "org.apache.tika.*",
                "org.sqlite.*",
                "org.lwjgl.*")
        for (property in readProperties) {
            logger.debug { "Game permission: Property read $property" }
            defaultPermissions.add(PropertyPermission(property, "read"))
        }

        // Allow reading various required environment variables
        val environmentVariables = listOf(
                "TURN_OFF_LR_LOOP_ENTRY_BRANCH_OPT",
                "TIKA_CONFIG")
        for (variable in environmentVariables) {
            logger.debug { "Game permission: getenv.$variable" }
            defaultPermissions.add(RuntimePermission("getenv.$variable"))
        }

        (ScapesClient::class.java.classLoader as? URLClassLoader)?.let { rootClassLoader ->
            rootClassLoader.urLs.asSequence().mapNotNull { it.file }.map {
                logger.debug { "Game permission: File read ($it)" }
                FilePermission(it, "read")
            }.forEach { defaultPermissions.add(it) }
        }
        Policy.setPolicy(object : Policy() {
            override fun getPermissions(codesource: CodeSource?): Permissions {
                val permissions = Permissions()
                //permissions.add(AllPermission())

                for (permission in defaultPermissions) {
                    permissions.add(permission)
                }

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
        threadLocalRandom()
        logger.debug { "Load logging" }
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

private inline fun <reified P1> reflectPermission(
        className: String,
        parameter1: P1
): Permission? = reflectPermissionImpl(className,
        arrayOf(P1::class.java),
        arrayOf(parameter1))

private inline fun <reified P1, reified P2> reflectPermission(
        className: String,
        parameter1: P1,
        parameter2: P2
): Permission? = reflectPermissionImpl(className,
        arrayOf(P1::class.java, P2::class.java),
        arrayOf(parameter1, parameter2))

private fun reflectPermissionImpl(
        className: String,
        types: Array<Class<*>>,
        parameters: Array<Any?>
): Permission? = try {
    val clazz = Class.forName(className)
    val constructor = clazz.getConstructor(*types)
    constructor.newInstance(*parameters) as Permission
} catch (e: Exception) {
    null
}
