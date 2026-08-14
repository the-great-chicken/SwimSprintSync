# SwimSprintSync

SwimSprintSync is a small, dependency-free Paper plugin that works around
[MC-220390](https://bugs.mojang.com/browse/MC-220390): after attacking while
sprint-swimming, a client can remain in the swimming pose while the server has
already interrupted sprinting. In tight spaces this pose desynchronization can
leave the player jittering and repeatedly rolled back.

## Requirements

- Paper 26.1.2 build 66 (probably works on newer versions)
- Java 25 or newer
- `misc.disable-sprint-interruption-on-attack: false` in the effective Paper
  world configuration

## Install

1. Obtain `SwimSprintSync-x.x.x.jar` from a successful GitHub Actions run, or
   build it locally as described below.
2. Put the JAR in the server's `plugins/` directory.
3. Fully restart the server. Avoid `/reload` and plugin hot-reloaders.
4. Confirm that the console logs `MC-220390 workaround enabled`.

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
sprint-swimming state. The pulse has no particles as it is never added
to the server-side potion-effect list. Consequently, the plugin does not change
damage, attack cooldown, critical hits, sweep attacks, knockback strength,
hunger, movement attributes, or the server's sprint rules. There can be a very brief Blindness fog transition on the attacking client though.

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
