package dev.swimsprintsync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PendingAttackTrackerTest {
    private final PendingAttackTracker tracker = new PendingAttackTracker();
    private final UUID attackerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @Test
    void consumesMatchingAttackExactlyOnce() {
        this.tracker.remember(this.attackerId, this.targetId);

        assertTrue(this.tracker.consumeIfMatches(this.attackerId, this.targetId));
        assertFalse(this.tracker.consumeIfMatches(this.attackerId, this.targetId));
        assertEquals(0, this.tracker.size());
    }

    @Test
    void targetMismatchDoesNotConsumeAttack() {
        this.tracker.remember(this.attackerId, this.targetId);

        assertFalse(this.tracker.consumeIfMatches(this.attackerId, UUID.randomUUID()));
        assertTrue(this.tracker.consumeIfMatches(this.attackerId, this.targetId));
    }

    @Test
    void staleExpiryCannotRemoveNewerAttack() {
        final long oldToken = this.tracker.remember(this.attackerId, UUID.randomUUID());
        this.tracker.remember(this.attackerId, this.targetId);

        this.tracker.forgetIfCurrent(this.attackerId, oldToken);

        assertTrue(this.tracker.consumeIfMatches(this.attackerId, this.targetId));
    }

    @Test
    void currentExpiryRemovesUnmatchedAttack() {
        final long token = this.tracker.remember(this.attackerId, this.targetId);

        this.tracker.forgetIfCurrent(this.attackerId, token);

        assertEquals(0, this.tracker.size());
    }
}
