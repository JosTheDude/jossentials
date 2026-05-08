package gg.jos.jossentials.qol;

import org.bukkit.configuration.file.FileConfiguration;

public class QOLSettings {

    public final boolean enderchestCommandEnabled;
    public final boolean backCommandEnabled;
    public final BackSettings backSettings;

    public QOLSettings(
            boolean enderchestCommandEnabled,
            boolean backCommandEnabled,
            BackSettings backSettings
    ) {
        this.enderchestCommandEnabled = enderchestCommandEnabled;
        this.backCommandEnabled = backCommandEnabled;
        this.backSettings = backSettings;
    }


    public static QOLSettings fromConfig(FileConfiguration config) {
        boolean enderchestCommandEnabled = config.getBoolean("qol.commands.enderchest", true);
        boolean backCommandEnabled = config.getBoolean("qol.commands.back", true);

        return new QOLSettings(
                enderchestCommandEnabled,
                backCommandEnabled,
                BackSettings.fromConfig(config)
        );
    }

}
