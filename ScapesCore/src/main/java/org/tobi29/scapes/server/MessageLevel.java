package org.tobi29.scapes.server;

public enum MessageLevel {
    SERVER_INFO(0),
    SERVER_ERROR(10),
    CHAT(100),
    FEEDBACK_INFO(110),
    FEEDBACK_ERROR(120);
    private final int level;

    MessageLevel(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }
}
