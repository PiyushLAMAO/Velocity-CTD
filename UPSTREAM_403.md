# Upstream build 403 integration

Merge base: upstream build 394. Updated upstream: build 403, commit
45b09b22c3cfb91a2497b06ec541df3c12628fea (6 October 2026).

This branch retains the deployed compatible fast transitions, conservative registry
snapshot fallback, explicit screen-free routes/protocols, entity/world cleanup,
proxy-route admission, forwarding address restoration, entity sounds and 26.3
boat passenger translation. It includes upstream's login bounds, connection
timeouts/backpressure, configuration buffering, shared player-entry safeguards
and updated dependencies.

Four configuration/transition conflicts required integration. Early PLAY frames
are retained and replayed through the actual PLAY handler so custom entity/chunk
tracking still applies. Backend read timeout changes now occur at upstream's
login-completion point. Every configuration activation resets its snapshot and
acknowledgement state. Known-pack responses preserve the fast-transition late
response guard and upstream's bound on responses awaiting an asynchronous event;
completed requests can be answered again during the same configuration phase.

The old dirty local fork is preserved. Main and Economy continue using their
existing namespace bootstrap; Survival's stock bootstrap remains separately
qualified. Configuration, native admission plugins, authentication, Geyser,
voice settings and player data are outside this engine source update.

Run `:velocity-proxy:check :velocity-proxy:shadowJar` and isolated current-plugin
compatibility/startup checks before runtime replacement. Retain exact previous
engines. Updating source alone does not establish live deployment or graphical
Java/Bedrock acceptance; the dated network deployment receipt records those.
