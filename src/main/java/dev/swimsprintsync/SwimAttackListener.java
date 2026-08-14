package dev.swimsprintsync;

import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

final class SwimAttackListener implements Listener {
    private final JavaPlugin plugin;
    private final PendingAttackTracker pendingAttacks;
    private final ClientBlindnessPulse blindnessPulse;

    SwimAttackListener(
        final JavaPlugin plugin,
        final PendingAttackTracker pendingAttacks,
        final ClientBlindnessPulse blindnessPulse
    ) {
        this.plugin = plugin;
        this.pendingAttacks = pendingAttacks;
        this.blindnessPulse = blindnessPulse;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPreAttack(final PrePlayerAttackEntityEvent event) {
        final Player attacker = event.getPlayer();
        if (!event.willAttack() || !attacker.isSwimming() || !attacker.isSprinting()) {
            return;
        }

        final UUID attackerId = attacker.getUniqueId();
        final long token = this.pendingAttacks.remember(attackerId, event.getAttacked().getUniqueId());

        // A matching knockback event is emitted synchronously. Expire unmatched attempts
        // next tick so invulnerable, cancelled, or otherwise failed attacks cannot linger.
        this.plugin.getServer().getScheduler().runTask(
            this.plugin,
            () -> this.pendingAttacks.forgetIfCurrent(attackerId, token)
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAttackPush(final EntityPushedByEntityAttackEvent event) {
        if (!(event.getPushedBy() instanceof Player attacker)) {
            return;
        }

        final boolean matches = this.pendingAttacks.consumeIfMatches(
            attacker.getUniqueId(),
            event.getEntity().getUniqueId()
        );
        if (matches) {
            // Observe cancelled push events too: Paper still interrupts the attacker's
            // sprint after the target's knockback event has been cancelled.
            this.blindnessPulse.send(attacker);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(final PlayerQuitEvent event) {
        final UUID playerId = event.getPlayer().getUniqueId();
        this.pendingAttacks.forget(playerId);
        this.blindnessPulse.forget(playerId);
    }
}
