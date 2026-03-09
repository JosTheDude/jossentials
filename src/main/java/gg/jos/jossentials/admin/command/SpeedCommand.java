package gg.jos.jossentials.admin.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import gg.jos.jossentials.admin.AdminFeature;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.entity.Player;

@CommandAlias("%{admin-speed-aliases}")
@CommandPermission("jossentials.speed")
public final class SpeedCommand extends BaseCommand {
    private static final float DEFAULT_WALK_SPEED = 0.2F;
    private static final float DEFAULT_FLY_SPEED = 0.1F;

    private final AdminFeature feature;
    private final MessageDispatcher messageDispatcher;

    public SpeedCommand(AdminFeature feature, MessageDispatcher messageDispatcher) {
        this.feature = feature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    @CommandCompletion("@adminspeedtypes @adminspeedvalues")
    public void onSpeed(Player player, String type, @Optional Integer speed) {
        if (!feature.isEnabled()) {
            messageDispatcher.send(player, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }

        String normalizedType = type.toLowerCase();
        if (!normalizedType.equals("walk") && !normalizedType.equals("fly")) {
            String message = feature.plugin().configs().messages().getString(
                "messages.admin-speed-invalid-type",
                "<red>Speed type must be <gold>walk</gold> or <gold>fly</gold>."
            );
            messageDispatcher.sendWithKey(player, "messages.admin-speed-invalid-type", message);
            return;
        }

        if (speed == null) {
            resetSpeed(player, normalizedType);
            return;
        }

        if (speed < 0 || speed > feature.settings().maxSpeed()) {
            String message = feature.plugin().configs().messages().getString(
                "messages.admin-speed-invalid-range",
                "<red>Speed must be between <gold>0</gold> and <gold>%max%</gold>."
            );
            messageDispatcher.sendWithKey(
                player,
                "messages.admin-speed-invalid-range",
                message.replace("%max%", String.valueOf(feature.settings().maxSpeed()))
            );
            return;
        }

        float applied = speed == 0
            ? (normalizedType.equals("walk") ? DEFAULT_WALK_SPEED : DEFAULT_FLY_SPEED)
            : Math.min(1.0F, speed / 10.0F);

        if (normalizedType.equals("walk")) {
            player.setWalkSpeed(applied);
            String message = feature.plugin().configs().messages().getString(
                "messages.admin-speed-walk",
                "<green>Walk speed set to <gold>%speed%</gold>."
            );
            messageDispatcher.sendWithKey(player, "messages.admin-speed-walk", message.replace("%speed%", String.valueOf(speed)));
            return;
        }

        player.setFlySpeed(applied);
        String message = feature.plugin().configs().messages().getString(
            "messages.admin-speed-fly",
            "<green>Fly speed set to <gold>%speed%</gold>."
        );
        messageDispatcher.sendWithKey(player, "messages.admin-speed-fly", message.replace("%speed%", String.valueOf(speed)));
    }

    private void resetSpeed(Player player, String type) {
        if (type.equals("walk")) {
            player.setWalkSpeed(DEFAULT_WALK_SPEED);
        } else {
            player.setFlySpeed(DEFAULT_FLY_SPEED);
        }

        String message = feature.plugin().configs().messages().getString(
            "messages.admin-speed-reset",
            "<green>%type% speed reset to normal."
        );
        messageDispatcher.sendWithKey(
            player,
            "messages.admin-speed-reset",
            message.replace("%type%", type.equals("walk") ? "Walk" : "Fly")
        );
    }
}
