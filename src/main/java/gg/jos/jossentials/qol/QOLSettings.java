package gg.jos.jossentials.qol;

import org.bukkit.configuration.file.FileConfiguration;

public class QOLSettings {

    public final boolean enderchestCommandEnabled;

    public QOLSettings(
            boolean enderchestCommandEnabled
    ) {
        this.enderchestCommandEnabled = enderchestCommandEnabled;
    }


    public static QOLSettings fromConfig(FileConfiguration config) {
        boolean enderchestCommandEnabled = config.getBoolean("qol.commands.enderchest", true);

        return new QOLSettings(
                enderchestCommandEnabled
        );
    }

}
