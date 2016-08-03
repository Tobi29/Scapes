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
