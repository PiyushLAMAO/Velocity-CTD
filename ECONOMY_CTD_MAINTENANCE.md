# Economy Velocity-CTD maintenance guide

This file is the handoff for the localhost-only Insane Economy Velocity fork. Read D:\Insane_Economy\source\AGENTS.md, D:\Insane_Economy\source\CONTEXT.md, and D:\Insane_Economy\README.md before changing the fork. The README is the verified-versus-unverified ledger. Older docs/survival-reference material is reference history, not an Economy requirement. Never use production credentials or player data, and do not deploy this fork to the live Insane SMP network.

## Repository and patch lineage

- Fork: https://github.com/PiyushLAMAO/Velocity-CTD
- Working branch: economy/26.3-seamless
- origin: PiyushLAMAO/Velocity-CTD; upstream: GemstoneGG/Velocity-CTD
- At the time of this guide, upstream/HEAD resolved to upstream/dev/4.0.0. Resolve it again during every update; do not assume that branch name stays fixed.
- The fast-transition foundation was merged from CTD pull request #1004 as research, then hardened for the Economy lab. It is not an upstream-supported feature.
- Economy commit sequence after upstream build-394 (779f8adb): cd0a74ca merged the fast-transition proposal; 172111fc hardened compatibility/fallback and added the update script; a1b16d6f added guarded screen-free Spawn↔Overworld transfers; 9ffb88fa enabled protocols 775 and 776. Use git log and git diff upstream/HEAD...HEAD to inspect the current patch rather than assuming these are still the latest commits.
- The current patch touches the API, proxy configuration and connection state machine, protocol packet codecs, two test classes, ECONOMY.md, and scripts/update-economy.ps1. Keep unrelated CTD changes and other agents' work intact.

## Behavior added to CTD

1. An opt-in advanced.fast-server-switch setting (default false) permits a compatible 1.20.2+ backend transfer to remain in PLAY rather than showing the client configuration phase. The new PlayerBackendConfigurationEvent gives plugins a hook during this process. The main implementation is proxy/src/main/java/com/velocityctd/proxy/connection/fasttransition/FastBackendConfigSessionHandler.java.
2. The proxy captures what the client actually received during configuration, then independently captures the destination backend's registry sync, tags, enabled features, and known-pack exchange. ConfigStateSnapshot.java fingerprints these semantically: NBT compound key and registry/tag map ordering do not cause a mismatch, while registry IDs, NBT values, features, and known packs must match. Incomplete state or a mismatch uses normal client reconfiguration. An unclassified configuration packet also falls back. An unknown raw packet cannot be replayed safely, so the target connection is refused and the player remains on the previous server. Keep this conservative behavior.
3. On the narrower Spawn↔Overworld route, ClientPlaySessionHandler.java may withhold destination JoinGame/Respawn entirely, leaving the client's current dimension/world rendered during the switch. This is allowed only after the compatible PLAY transition, with advanced.fast-server-switch enabled, a client protocol in the table below, non-legacy-Forge, matching current/destination dimension keys, and server names spawn and overworld in either direction. Other server pairs and dimensions retain normal or existing fast-transfer behavior.
4. That screen-free route clears old entities, tab entries, titles, boss bars, scoreboard objectives/teams, header/footer, effects, attributes, and selected player metadata; it tracks/remaps the destination backend entity ID to the retained client entity ID. It sends the backend a player-loaded acknowledgement because the client never receives a fresh JoinGame. It suppresses chunk-wait loading events for up to 20 seconds. The relevant code is ClientPlaySessionHandler.java, BackendPlaySessionHandler.java, MinecraftSessionHandler.java and the packet classes registered in StateRegistry.java. Do not remove the cleanup or the dimension/protocol guard to make a loading screen disappear.
5. The entity/game-event codecs include adapted Conduit GPL-3.0 code from https://github.com/tame-gg/conduit at d5176e5; preserve attribution and review licensing when moving or replacing it. ObjectivePacket and TeamPacket support scoreboard cleanup. KnownPacksPacket and TagsUpdatePacket expose data for configuration comparison. StateRegistry.java has versioned packet-ID registrations. The 26.3 IDs were counted from the local Mojang-mapped protocol; earlier IDs follow the Conduit registry and must be checked against a current codec when versions change.
6. Fast-switch configuration is documented in proxy/src/main/resources/default-velocity.toml and migrated through CtdConfigMigrations.java. Do not assume an existing runtime config has the opt-in enabled.

Supported screen-free Spawn↔Overworld client protocols at this snapshot:

| Client version | Wire protocol | Local protocol round trip | Real-client screen/movement |
| --- | ---: | --- | --- |
| 1.21.11 | 774 | Passed | Both directions: no loading screen; movement at Spawn confirmed |
| 26.1, 26.1.1, 26.1.2 | 775 (shared) | Passed | Not yet checked with these client builds |
| 26.2 | 776 | Passed | Both directions: no loading screen; movement reported working |
| 26.3 | 777 | Passed | Screen-free Spawn round trip not yet visually checked with a real 26.3 client |

All four protocol-bot round trips on the isolated 26.3 lab showed one initial JoinGame, zero extra Respawn, and zero StartConfiguration, with both screen-free directions logged. Protocol bots cannot prove rendering, movement, collision, inventory, entity state or interactions. The real-client reports above are limited to what the operator actually observed. The 26.3 bot also exercised a separate Spawn→Overworld→Nether→End→Spawn compatible-PLAY route before the narrower screen-free path; cross-dimension travel is not claimed to be screen-free.

## Runtime scope and rollback

The special build was installed only on the isolated test proxy at 127.0.0.1:25695. The regular proxy at 127.0.0.1:25690 was not replaced. Local backends are Spawn 25691, Overworld tile 5 at 25692, shard_4 tile 4 at 25696, Nether 25693, and End 25694; these endpoints are loopback only. Runtime files and isolated data live under D:\Insane_Economy\runtime, outside this source fork. Do not commit runtime configs, secrets, logs or player data.

The last tested isolated JAR (before future rebuilds) had SHA-256 021078E2477B424B54E7C521069502D7DF2BE091CEA5108FBE40F0AFC159610F. Recompute the hash for every new build; it is a historical verification marker, not a permanent expected hash. Set advanced.fast-server-switch back to false on the isolated proxy and restart that proxy if the experimental route regresses. That restores ordinary Velocity transfers, including normal loading/reconfiguration. Do not use the regular proxy as a rollback target.

## Updating from CTD upstream

Use a clean working tree on the Economy branch. The script fetches the current upstream default branch, merges it into this branch, and runs proxy checks and a shadow JAR build. It does not copy or start a runtime proxy. A merge conflict, failed test, or failed build is a stop point for review; never deploy the resulting JAR merely because the script reached a build step.

    cd D:\Insane_Economy\source\third-party-src\Velocity-CTD
    git switch economy/26.3-seamless
    git status --short
    .\scripts\update-economy.ps1

If the script reports a conflict, inspect git status and git diff, resolve only the conflicting CTD files, and finish the merge. Review all changes touching configuration packets, connection/session handlers, packet registries, queues, and player state. Upstream may have implemented or replaced PR #1004; do not blindly keep two fast-switch implementations. Check git diff upstream/HEAD...HEAD after resolving. Run the build explicitly again:

    .\gradlew.bat :velocity-proxy:check :velocity-proxy:shadowJar --no-daemon --max-workers=2 --console=plain

The last successful pre-guide build passed 251 proxy tests and those two tasks. Test counts can change with upstream; investigate failures rather than hardcoding 251 as an invariant. Locate the new proxy/build/libs/velocity-proxy-*-all.jar, calculate its SHA-256, and record the exact revision/hash in the README ledger.

Before replacing even the isolated test proxy, recheck every wire protocol's packet IDs and codecs in StateRegistry.java and SeamlessPacketRegistryTest.java. A new Minecraft client version needs an explicit protocol gate, mappings for every cleanup/game-event packet, tests, and live protocol/real-client acceptance; never expand the gate solely by version range. Rerun ConfigStateSnapshotTest for changed registry/tag/feature/known-pack behavior. Keep fallback on incomplete or different state and on unexpected configuration packets.

Use only the loopback CTD test proxy and disposable accounts/data for a 774, 775, 776 and 777 Spawn→Overworld→Spawn protocol round trip against 26.3 backends. Check the proxy logs for both “Fast transition ... switching in PLAY” and “Screen-free switch ...” directions, backend acceptance, one initial JoinGame, zero extra Respawn and zero StartConfiguration. A bot's command output alone is insufficient: wait until the first transfer finishes before sending the return command, then verify both log entries. Test an incompatible live backend so fallback actually reconfigures; that case remains unverified in the current ledger. Check cross-dimension Nether/End routes separately.

Then test with real clients for all advertised versions. In both directions verify visible screens, movement, block/entity interaction, inventory sync, tab/bossbar/scoreboard cleanup, effects/attributes, death/respawn, and a backend restart. Record passed and failed observations separately in D:\Insane_Economy\README.md and this guide. The README has some legacy mixed-encoding bytes: edit narrowly and preserve unrelated bytes. Do not turn a protocol-bot result into a real-client claim.

Only after the build, protocol checks, real-client checks, and ledger update should the validated branch be pushed:

    git push origin economy/26.3-seamless

Updating source and pushing the branch does not authorize production deployment. Do not replace the regular proxy without a separate decision and acceptance run.

## Known gaps and adjacent work

- No backend-restart limbo, reconnect hold, or numbered reconnect queue is implemented or enabled by this Economy CTD patch. CTD can redirect to another live backend and has its own queue machinery, but that is not a virtual holding world when every backend is unavailable.
- The retained LimboReconnect/LimboAPI source is separate from this fork and has not passed a 26.3 port/startup/behavior test. The recommended design for backend restart holding is a Velocity-side plugin with a stationary limbo and a real per-backend queue, not an additional CTD core patch. It must be tested before the README marks it enabled. A backend restart can preserve the proxy's connected-player count if clients remain held; restarting the proxy itself disconnects them.
- The screen-free route is only Spawn↔Overworld as named above. shard_4 handoffs, Nether/End dimension changes, portals, incompatible backends, and general teleport/loading behavior are separate cases.
- Real 26.1.x and 26.3 visual Spawn-round-trip acceptance, full gameplay state cleanup, and deliberately incompatible-backend fallback are still open. The root README is authoritative if later tests supersede this snapshot.