package org.tobi29.scapes;

public final class Debug {
    private static boolean enabled;
    private static boolean socketSingleplayer;

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

    public static void socketSingleplayer(boolean value) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new RuntimePermission("scapes.debug"));
        }
        socketSingleplayer = value;
    }

    public static boolean socketSingleplayer() {
        return socketSingleplayer;
    }
}
