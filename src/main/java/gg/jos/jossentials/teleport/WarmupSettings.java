package gg.jos.jossentials.teleport;

public interface WarmupSettings {
    boolean warmupEnabled();
    int warmupSeconds();
    boolean cancelOnMove();
    boolean cancelOnDamage();
    double movementThreshold();
    String bypassPermission();
}
