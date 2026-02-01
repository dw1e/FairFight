package me.dw1e.ff.check.api;

public enum Category {

    AIM("Aim"),
    AUTO_CLICKER("AutoClicker"),
    BAD_PACKET("BadPacket"),
    HITBOX("Hitbox"),
    INTERACT("Interact"),
    INVENTORY("Inventory"),
    KILL_AURA("KillAura"),
    MISC("Misc"),
    POST("Post"),
    SCAFFOLD("Scaffold"),
    FLY("Fly"),
    SPEED("Speed"),
    TIMER("Timer"),
    VELOCITY("Velocity");

    private final String name;

    Category(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }
}
