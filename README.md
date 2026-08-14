# SwimSprintSync

SwimSprintSync is a small, dependency-free Paper plugin that works around
[MC-220390](https://bugs.mojang.com/browse/MC-220390): after attacking while
sprint-swimming, a client can remain in the swimming pose while the server has
already interrupted sprinting. In tight spaces this pose desynchronization can
leave the player jittering and repeatedly rolled back.

This repository targets **Paper 26.1.2 build 66** and Java 25.

## Requirements

- Paper 26.1.2 build 66
- Java 25 or newer
- `misc.disable-sprint-interruption-on-attack: false` in the effective Paper
  world configuration
- A full server restart after installing or removing the plugin

There are no runtime dependencies. PacketEvents, ProtocolLib, CommandAPI, and
OldCombatMechanics are not required by this workaround.

Do not use this plugin together with
`misc.disable-sprint-interruption-on-attack: true`. That setting prevents the
server-side sprint interruption that this plugin is designed to mirror on the
client.

## Install

1. Obtain `SwimSprintSync-1.0.0.jar` from a successful GitHub Actions run, or
   build it locally as described below.
2. Put the JAR in the server's `plugins/` directory.
3. Fully restart the server. Avoid `/reload` and plugin hot-reloaders.
4. Confirm that the console logs `MC-220390 workaround enabled`.

To remove the workaround, stop the server, remove the JAR, and start the server
again.

## How it works

The plugin deliberately follows Paper's attack path instead of reacting to
every damage event:

1. `PrePlayerAttackEntityEvent` records an attack only when the server sees the
   attacker as both sprinting and swimming.
2. A matching `EntityPushedByEntityAttackEvent` confirms that the same attack
   reached the knockback path where Paper will interrupt the attacker's sprint.
3. Paper's `Player#sendPotionEffectChange` API sends the attacker a hidden,
   client-only Blindness pulse.
4. Two ticks later the plugin removes the fake effect, or re-sends any real
   Blindness effect that was applied during that short window.

Blindness is used because receiving it makes the vanilla client leave the stale
sprint-swimming state. The pulse has no particles or HUD icon and is never added
to the server-side potion-effect list. Consequently, the plugin does not change
damage, attack cooldown, critical hits, sweep attacks, knockback strength,
hunger, movement attributes, or the server's sprint rules. No packets are
intercepted and no NMS code is used.

There can be a very brief Blindness fog transition on the attacking client. The
two-tick pulse is intentionally as short as the known datapack workaround while
avoiding its two major problems: applying a real server effect to the player and
clearing legitimate Blindness.

## PvP verification checklist

Test on a staging copy of the server before production deployment:

- Reproduce MC-220390 by sprint-swimming through the affected geometry and
  landing a fully charged hit. Verify with a second player that the attacker's
  pose resynchronizes and rollback jitter stops.
- Repeat with a Knockback-enchanted weapon and with target knockback cancelled
  by any protection or combat plugin in use.
- Check normal land sprint hits, partially charged attacks, critical hits,
  shields, sword sweeps, and rapid clicking. Their server-side behavior should
  be unchanged.
- Test a legitimate Blindness effect before and during a swimming attack. It
  must remain present for its original duration.
- Confirm there are no new console errors and that the plugin does not appear in
  timings as a meaningful cost.

## Build locally

The checked-in Gradle wrapper downloads Gradle 9.4.1 and verifies its SHA-256
checksum. Install a Java 25 JDK, then run:

```bash
./gradlew clean build
```

The deployable file is written to:

```text
build/libs/SwimSprintSync-1.0.0.jar
```

Unit tests run as part of `build` and of the GitHub Actions workflow.

## Versioning and GitHub Actions

`pluginVersion` in `gradle.properties` is the single version source. It must be
a valid [Semantic Version](https://semver.org/), such as `1.0.1`, `1.1.0`, or
`2.0.0-rc.1`; Gradle rejects invalid values during configuration.

On every push to `main`, `.github/workflows/build.yml`:

1. installs Java 25;
2. validates the version;
3. runs the tests and builds the plugin; and
4. uploads `SwimSprintSync-<version>.jar` as a workflow artifact for 90 days.

Before merging a change to `main`, update `pluginVersion` according to normal
SemVer rules. GitHub rejects duplicate artifact names only within the same
workflow run, so repeated builds of a version remain independent artifacts.

## Maintenance scope

This is a narrow workaround for MC-220390, not a general movement or combat
plugin. It intentionally has no commands and no configuration file. Re-test it
when updating Paper, and remove it once the upstream client bug is fixed and the
fix is present in every client version allowed onto the server.
