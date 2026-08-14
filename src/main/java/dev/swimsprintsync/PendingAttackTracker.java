package dev.swimsprintsync;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks sprint-swimming attacks only for the short synchronous window in which
 * Paper may apply attack knockback and interrupt the attacker's sprint.
 */
final class PendingAttackTracker {
    private final Map<UUID, PendingAttack> attacksByPlayer = new HashMap<>();
    private long nextToken;

    long remember(final UUID attackerId, final UUID targetId) {
        final long token = ++this.nextToken;
        this.attacksByPlayer.put(attackerId, new PendingAttack(targetId, token));
        return token;
    }

    boolean consumeIfMatches(final UUID attackerId, final UUID targetId) {
        final PendingAttack pendingAttack = this.attacksByPlayer.get(attackerId);
        if (pendingAttack == null || !pendingAttack.targetId().equals(targetId)) {
            return false;
        }

        this.attacksByPlayer.remove(attackerId);
        return true;
    }

    void forgetIfCurrent(final UUID attackerId, final long token) {
        final PendingAttack pendingAttack = this.attacksByPlayer.get(attackerId);
        if (pendingAttack != null && pendingAttack.token() == token) {
            this.attacksByPlayer.remove(attackerId);
        }
    }

    void forget(final UUID attackerId) {
        this.attacksByPlayer.remove(attackerId);
    }

    void clear() {
        this.attacksByPlayer.clear();
    }

    int size() {
        return this.attacksByPlayer.size();
    }

    private record PendingAttack(UUID targetId, long token) {
    }
}
