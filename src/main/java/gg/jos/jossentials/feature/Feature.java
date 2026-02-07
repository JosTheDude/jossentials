package gg.jos.jossentials.feature;

public interface Feature {
    String key();

    boolean isEnabledFromConfig();

    boolean isEnabled();

    void enable();

    void disable();

    void reload();
}
