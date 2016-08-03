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

package org.tobi29.scapes.plugins;

import java8.util.Spliterators;
import java8.util.concurrent.ThreadLocalRandom;

import java.security.*;
import java.util.Enumeration;
import java.util.concurrent.ForkJoinPool;

public class Sandbox {
    private static final String[] PACKAGE_WHITELIST =
            {"java", "org.tobi29.scapes", "org.slf4j", "java8"};
    private static boolean sandboxed;

    @SuppressWarnings("CustomSecurityManager")
    public static void sandbox() {
        if (sandboxed) {
            return;
        }
        sandboxed = true;
        preload();
        Policy.setPolicy(new Policy() {
            @Override
            public Permissions getPermissions(CodeSource codesource) {
                Permissions permissions = new Permissions();
                permissions.add(new AllPermission());
                return permissions;
            }

            @Override
            public Permissions getPermissions(ProtectionDomain domain) {
                if (domain.getClassLoader() instanceof PluginClassLoader) {
                    return new Permissions();
                }
                Permissions permissions =
                        getPermissions(domain.getCodeSource());
                PermissionCollection domainPermissions =
                        domain.getPermissions();
                synchronized (domainPermissions) {
                    Enumeration<Permission> domainPermission =
                            domainPermissions.elements();
                    while (domainPermission.hasMoreElements()) {
                        permissions.add(domainPermission.nextElement());
                    }
                }
                return permissions;
            }
        });
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPackageAccess(String pkg) {
                super.checkPackageAccess(pkg);
                boolean whitelisted = false;
                for (String whitelist : PACKAGE_WHITELIST) {
                    if (pkg.startsWith(whitelist)) {
                        whitelisted = true;
                        break;
                    }
                }
                if (!whitelisted) {
                    checkPermission(
                            new RuntimePermission("scapes.restrictedPkg"));
                }
            }
        });
    }

    public static void preload() {
        // Preload these to avoid permission errors when plugins trip into these
        Spliterators.emptySpliterator();
        ForkJoinPool.commonPool();
        ThreadLocalRandom.current();
    }
}
