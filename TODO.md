# TODO

- [x] Implement ordered local SMS forwarding routes.
  - Contract: a route has at least one sender/content substring and a required destination; the first matching route forwards the incoming body.
  - Validation: pure JVM tests cover route matching and order; static project validation passes.

- [x] Implement the event-driven SMS receiver and permissions UI.
  - Contract: receiving an SMS does not start a persistent service and forwarding work never executes on the main thread.
  - Validation: manifest/static checks pass; Android unit-test build is pending a host with JDK 17 and Android SDK 35.

- [x] Provide deterministic developer workflow and accurate project documents.
  - Contract: clean-checkout commands exist for build, run, test, and benchmark.
  - Validation: shell syntax and static project validation pass.

- [x] Apply the technical-paper visual system to route management.
  - Contract: the app uses off-white surfaces, sharp bordered controls, technical typography for phone data, and blue/green/amber/red only for their documented semantic roles.
  - Validation: XML/static validation passes; visual device/emulator review remains part of the configured-host validation below.

- [x] Add an adaptive launcher icon.
  - Contract: Android 8+ uses a transparent SMS-forwarding foreground mark over the app's off-white background; Android 6–7 receive a bitmap fallback.
  - Validation: density-specific foreground/fallback images and adaptive XML are present; the Android debug build will validate the manifest reference.

- [x] Run Android build, unit tests, and fixed-workload benchmark on a configured build host.
  - Contract: `scripts/build.sh`, `scripts/test.sh`, and `scripts/benchmark.sh` complete with JDK 17+ and Android SDK Platform 36 installed (the app continues to target SDK 35).
  - Validation: debug APK built successfully; all matcher tests passed; `scripts/benchmark.sh` measured 124 ms for its fixed workload and is recorded in `BENCHMARKS.md`.
