package dev.swimsprintsync;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Sends a very short, client-only Blindness effect to make the client stop the
 * stale sprint-swimming state. The effect never enters the server's effect map.
 */
final class ClientBlindnessPulse {
    private static final long REMOVE_AFTER_TICKS = 2L;
    private static final int PACKET_EFFECT_DURATION_TICKS = 20;
    private static final PotionEffect CLIENT_ONLY_BLINDNESS = new PotionEffect(
        PotionEffectType.BLINDNESS,
        PACKET_EFFECT_DURATION_TICKS,
        0,
        false,
        false,
        false
    );

    private final JavaPlugin plugin;
    private final Set<UUID> activePlayers = new HashSet<>();

    ClientBlindnessPulse(final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void send(final Player player) {
        // A real Blindness effect already keeps the client's sprint state in sync.
        if (player.getPotionEffect(PotionEffectType.BLINDNESS) != null) {
            return;
        }

        final UUID playerId = player.getUniqueId();
        if (!this.activePlayers.add(playerId)) {
            return;
        }

        player.sendPotionEffectChange(player, CLIENT_ONLY_BLINDNESS);
        this.plugin.getServer().getScheduler().runTaskLater(
            this.plugin,
            () -> this.restoreServerEffect(playerId),
            REMOVE_AFTER_TICKS
        );
    }

    void forget(final UUID playerId) {
        this.activePlayers.remove(playerId);
    }

    void close() {
        for (final UUID playerId : Set.copyOf(this.activePlayers)) {
            this.restoreServerEffect(playerId);
        }
    }

    private void restoreServerEffect(final UUID playerId) {
        if (!this.activePlayers.remove(playerId)) {
            return;
        }

        final Player player = this.plugin.getServer().getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }

        final PotionEffect realBlindness = player.getPotionEffect(PotionEffectType.BLINDNESS);
        if (realBlindness == null) {
            player.sendPotionEffectChangeRemove(player, PotionEffectType.BLINDNESS);
        } else {
            // Preserve Blindness applied by Minecraft or another plugin during the pulse.
            player.sendPotionEffectChange(player, realBlindness);
        }
    }
}
