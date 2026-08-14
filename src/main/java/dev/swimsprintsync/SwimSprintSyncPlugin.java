package dev.swimsprintsync;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper workaround for MC-220390: the client can remain sprint-swimming after
 * the server interrupts sprint during an attack, leaving the two poses desynced.
 */
public final class SwimSprintSyncPlugin extends JavaPlugin {
    private final PendingAttackTracker pendingAttacks = new PendingAttackTracker();
    private ClientBlindnessPulse blindnessPulse;

    @Override
    public void onEnable() {
        this.blindnessPulse = new ClientBlindnessPulse(this);
        this.getServer().getPluginManager().registerEvents(
            new SwimAttackListener(this, this.pendingAttacks, this.blindnessPulse),
            this
        );

        this.getLogger().info(
            "MC-220390 workaround enabled; keep misc.disable-sprint-interruption-on-attack set to false."
        );
    }

    @Override
    public void onDisable() {
        this.pendingAttacks.clear();
        if (this.blindnessPulse != null) {
            this.blindnessPulse.close();
            this.blindnessPulse = null;
        }
    }
}
