package gg.jos.jossentials.feature;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FeatureManager {
    private final Map<String, Feature> features = new LinkedHashMap<>();

    public void register(Feature feature) {
        features.put(feature.key(), feature);
    }

    public Feature get(String key) {
        return features.get(key);
    }

    public void enableConfigured() {
        for (Feature feature : features.values()) {
            if (feature.isEnabledFromConfig()) {
                feature.enable();
            }
        }
    }

    public void reloadConfigured() {
        for (Feature feature : features.values()) {
            boolean shouldEnable = feature.isEnabledFromConfig();
            if (shouldEnable && !feature.isEnabled()) {
                feature.enable();
            } else if (!shouldEnable && feature.isEnabled()) {
                feature.disable();
            } else if (shouldEnable) {
                feature.reload();
            }
        }
    }

    public void disableAll() {
        for (Feature feature : features.values()) {
            if (feature.isEnabled()) {
                feature.disable();
            }
        }
    }
}
