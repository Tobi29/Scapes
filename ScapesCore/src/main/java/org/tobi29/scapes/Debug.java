package org.tobi29.scapes;

public class Debug {
    private static boolean enabled;

    private Debug() {
    }

    public static void enable() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new RuntimePermission("scapes.debug"));
        }
        enabled = true;
    }

    public static boolean enabled() {
        return enabled;
    }
}
