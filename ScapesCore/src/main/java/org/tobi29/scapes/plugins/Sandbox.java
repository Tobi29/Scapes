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
