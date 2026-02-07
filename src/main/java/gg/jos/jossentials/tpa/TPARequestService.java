package gg.jos.jossentials.tpa;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.tpa.teleport.TPATeleportService;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TPARequestService implements Listener {
    private final Jossentials plugin;
    private final MessageDispatcher messageDispatcher;
    private final TPATeleportService teleportService;
    private final Map<UUID, Map<UUID, PendingRequest>> pendingByTarget = new ConcurrentHashMap<>();
    private final Map<UUID, PendingRequest> pendingByRequester = new ConcurrentHashMap<>();
    private volatile TPASettings settings;

    public TPARequestService(Jossentials plugin, MessageDispatcher messageDispatcher, TPATeleportService teleportService, TPASettings settings) {
        this.plugin = plugin;
        this.messageDispatcher = messageDispatcher;
        this.teleportService = teleportService;
        this.settings = settings;
    }

    public void updateSettings(TPASettings settings) {
        this.settings = settings;
    }

    public void request(Player requester, Player target) {
        request(requester, target, RequestType.TO_TARGET);
    }

    public void requestHere(Player requester, Player target) {
        request(requester, target, RequestType.HERE);
    }

    private void request(Player requester, Player target, RequestType type) {
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            String message = plugin.getConfig().getString("messages.tpa-self", "<red>You cannot send a teleport request to yourself.");
            messageDispatcher.sendWithKey(requester, "messages.tpa-self", message);
            return;
        }
        if (pendingByRequester.containsKey(requester.getUniqueId())) {
            String message = plugin.getConfig().getString("messages.tpa-already-pending", "<red>You already have a pending teleport request.");
            messageDispatcher.sendWithKey(requester, "messages.tpa-already-pending", message);
            return;
        }

        Map<UUID, PendingRequest> incoming = pendingByTarget.computeIfAbsent(target.getUniqueId(), key -> new ConcurrentHashMap<>());
        if (incoming.containsKey(requester.getUniqueId())) {
            String message = plugin.getConfig().getString("messages.tpa-already-pending", "<red>You already have a pending teleport request.");
            messageDispatcher.sendWithKey(requester, "messages.tpa-already-pending", message);
            return;
        }

        PendingRequest request = new PendingRequest(requester.getUniqueId(), requester.getName(), target.getUniqueId(), target.getName(), type);
        pendingByRequester.put(request.requesterId, request);
        incoming.put(request.requesterId, request);

        String sentKey = type == RequestType.HERE ? "messages.tpa-here-request-sent" : "messages.tpa-request-sent";
        String sent = plugin.getConfig().getString(sentKey, "<green>Teleport request sent to <gold>%target%</gold>.");
        sent = applyPlaceholders(sent, request);
        messageDispatcher.sendWithKey(requester, sentKey, sent);

        String receivedKey = type == RequestType.HERE ? "messages.tpa-here-request-received" : "messages.tpa-request-received";
        String received = plugin.getConfig().getString(receivedKey, "<gold>%requester%</gold> wants to teleport to you. Type <yellow>/tpaccept %requester%</yellow> to accept.");
        received = applyPlaceholders(received, request).replace("%seconds%", String.valueOf(settings.requestExpirySeconds));
        messageDispatcher.sendWithKey(target, receivedKey, received);

        int expirySeconds = Math.max(0, settings.requestExpirySeconds);
        if (expirySeconds > 0) {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> expireRequest(request), expirySeconds * 20L);
            request.task = task;
        }
    }

    public void accept(Player target, Player requester) {
        PendingRequest request = findRequest(target, requester);
        if (request == null) {
            return;
        }
        acceptRequest(target, request);
    }

    public void acceptSingle(Player target) {
        PendingRequest request = findSingleRequest(target);
        if (request == null) {
            return;
        }
        acceptRequest(target, request);
    }

    public void deny(Player target, Player requester) {
        PendingRequest request = findRequest(target, requester);
        if (request == null) {
            return;
        }
        denyRequest(target, request);
    }

    public void denySingle(Player target) {
        PendingRequest request = findSingleRequest(target);
        if (request == null) {
            return;
        }
        denyRequest(target, request);
    }

    public void cancel(Player requester) {
        PendingRequest request = pendingByRequester.get(requester.getUniqueId());
        if (request == null) {
            String message = plugin.getConfig().getString("messages.tpa-no-pending", "<red>You have no pending teleport requests.");
            messageDispatcher.sendWithKey(requester, "messages.tpa-no-pending", message);
            return;
        }
        if (!removeRequest(request)) {
            return;
        }

        String cancelledRequester = plugin.getConfig().getString("messages.tpa-request-cancelled", "<red>Teleport request cancelled.");
        messageDispatcher.sendWithKey(requester, "messages.tpa-request-cancelled", applyPlaceholders(cancelledRequester, request));

        Player target = Bukkit.getPlayer(request.targetId);
        if (target != null && target.isOnline()) {
            String cancelledTarget = plugin.getConfig().getString("messages.tpa-request-cancelled-target", "<red>Teleport request from <gold>%requester%</gold> was cancelled.");
            messageDispatcher.sendWithKey(target, "messages.tpa-request-cancelled-target", applyPlaceholders(cancelledTarget, request));
        }
    }

    private PendingRequest findRequest(Player target, Player requester) {
        if (requester == null) {
            String message = plugin.getConfig().getString("messages.tpa-no-pending", "<red>You have no pending teleport requests.");
            messageDispatcher.sendWithKey(target, "messages.tpa-no-pending", message);
            return null;
        }
        Map<UUID, PendingRequest> incoming = pendingByTarget.get(target.getUniqueId());
        if (incoming == null || incoming.isEmpty()) {
            String message = plugin.getConfig().getString("messages.tpa-no-pending", "<red>You have no pending teleport requests.");
            messageDispatcher.sendWithKey(target, "messages.tpa-no-pending", message);
            return null;
        }
        PendingRequest request = incoming.get(requester.getUniqueId());
        if (request == null) {
            String message = plugin.getConfig().getString("messages.tpa-no-pending", "<red>You have no pending teleport requests.");
            messageDispatcher.sendWithKey(target, "messages.tpa-no-pending", message);
            return null;
        }
        return request;
    }

    private PendingRequest findSingleRequest(Player target) {
        Map<UUID, PendingRequest> incoming = pendingByTarget.get(target.getUniqueId());
        if (incoming == null || incoming.isEmpty()) {
            String message = plugin.getConfig().getString("messages.tpa-no-pending", "<red>You have no pending teleport requests.");
            messageDispatcher.sendWithKey(target, "messages.tpa-no-pending", message);
            return null;
        }
        if (incoming.size() > 1) {
            String message = plugin.getConfig().getString("messages.tpa-multiple-pending", "<yellow>Multiple requests pending. Use <gold>/tpaccept <player></gold>.");
            messageDispatcher.sendWithKey(target, "messages.tpa-multiple-pending", message);
            return null;
        }
        return incoming.values().iterator().next();
    }

    private void acceptRequest(Player target, PendingRequest request) {
        if (!removeRequest(request)) {
            return;
        }

        Player requesterPlayer = Bukkit.getPlayer(request.requesterId);
        if (requesterPlayer == null || !requesterPlayer.isOnline()) {
            String message = plugin.getConfig().getString("messages.tpa-request-expired-target", "<red>That teleport request has expired.");
            messageDispatcher.sendWithKey(target, "messages.tpa-request-expired-target", applyPlaceholders(message, request));
            return;
        }

        String acceptedRequester = plugin.getConfig().getString("messages.tpa-request-accepted", "<green>Your teleport request was accepted.");
        messageDispatcher.sendWithKey(requesterPlayer, "messages.tpa-request-accepted", applyPlaceholders(acceptedRequester, request));

        String acceptedTarget = plugin.getConfig().getString("messages.tpa-request-accepted-target", "<green>Accepted teleport request from <gold>%requester%</gold>.");
        messageDispatcher.sendWithKey(target, "messages.tpa-request-accepted-target", applyPlaceholders(acceptedTarget, request));

        if (request.type == RequestType.HERE) {
            teleportService.teleport(target, requesterPlayer);
        } else {
            teleportService.teleport(requesterPlayer, target);
        }
    }

    private void denyRequest(Player target, PendingRequest request) {
        if (!removeRequest(request)) {
            return;
        }

        Player requesterPlayer = Bukkit.getPlayer(request.requesterId);
        if (requesterPlayer != null && requesterPlayer.isOnline()) {
            String message = plugin.getConfig().getString("messages.tpa-request-denied", "<red>Your teleport request was denied.");
            messageDispatcher.sendWithKey(requesterPlayer, "messages.tpa-request-denied", applyPlaceholders(message, request));
        }

        String deniedTarget = plugin.getConfig().getString("messages.tpa-request-denied-target", "<red>Denied teleport request from <gold>%requester%</gold>.");
        messageDispatcher.sendWithKey(target, "messages.tpa-request-denied-target", applyPlaceholders(deniedTarget, request));
    }

    private boolean removeRequest(PendingRequest request) {
        Map<UUID, PendingRequest> incoming = pendingByTarget.get(request.targetId);
        if (incoming == null) {
            return false;
        }
        PendingRequest current = incoming.remove(request.requesterId);
        if (current == null) {
            return false;
        }
        if (incoming.isEmpty()) {
            pendingByTarget.remove(request.targetId);
        }
        pendingByRequester.remove(request.requesterId);
        if (current.task != null) {
            current.task.cancel();
        }
        return true;
    }

    private void expireRequest(PendingRequest request) {
        if (!removeRequest(request)) {
            return;
        }
        Player requester = Bukkit.getPlayer(request.requesterId);
        if (requester != null && requester.isOnline()) {
            String message = plugin.getConfig().getString("messages.tpa-request-expired", "<red>Your teleport request expired.");
            messageDispatcher.sendWithKey(requester, "messages.tpa-request-expired", applyPlaceholders(message, request));
        }

        Player target = Bukkit.getPlayer(request.targetId);
        if (target != null && target.isOnline()) {
            String message = plugin.getConfig().getString("messages.tpa-request-expired-target", "<red>Teleport request from <gold>%requester%</gold> expired.");
            messageDispatcher.sendWithKey(target, "messages.tpa-request-expired-target", applyPlaceholders(message, request));
        }
    }

    public void shutdown() {
        for (PendingRequest request : pendingByRequester.values()) {
            if (request.task != null) {
                request.task.cancel();
            }
        }
        pendingByRequester.clear();
        pendingByTarget.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        PendingRequest outgoing = pendingByRequester.get(playerId);
        if (outgoing != null && removeRequest(outgoing)) {
            Player target = Bukkit.getPlayer(outgoing.targetId);
            if (target != null && target.isOnline()) {
                String cancelledTarget = plugin.getConfig().getString("messages.tpa-request-cancelled-target", "<red>Teleport request from <gold>%requester%</gold> was cancelled.");
                messageDispatcher.sendWithKey(target, "messages.tpa-request-cancelled-target", applyPlaceholders(cancelledTarget, outgoing));
            }
        }

        Map<UUID, PendingRequest> incoming = pendingByTarget.remove(playerId);
        if (incoming == null || incoming.isEmpty()) {
            return;
        }
        for (PendingRequest request : incoming.values()) {
            pendingByRequester.remove(request.requesterId);
            if (request.task != null) {
                request.task.cancel();
            }
            Player requester = Bukkit.getPlayer(request.requesterId);
            if (requester != null && requester.isOnline()) {
                String cancelledRequester = plugin.getConfig().getString("messages.tpa-request-cancelled", "<red>Teleport request cancelled.");
                messageDispatcher.sendWithKey(requester, "messages.tpa-request-cancelled", applyPlaceholders(cancelledRequester, request));
            }
        }
    }

    private String applyPlaceholders(String message, PendingRequest request) {
        return message
            .replace("%requester%", request.requesterName)
            .replace("%target%", request.targetName);
    }

    private static final class PendingRequest {
        private final UUID requesterId;
        private final String requesterName;
        private final UUID targetId;
        private final String targetName;
        private final RequestType type;
        private BukkitTask task;

        private PendingRequest(UUID requesterId, String requesterName, UUID targetId, String targetName, RequestType type) {
            this.requesterId = requesterId;
            this.requesterName = requesterName;
            this.targetId = targetId;
            this.targetName = targetName;
            this.type = type;
        }
    }

    private enum RequestType {
        TO_TARGET,
        HERE
    }
}
