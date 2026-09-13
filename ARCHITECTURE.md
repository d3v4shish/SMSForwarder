# Architecture

## Components

- `MainActivity` requests SMS permissions and lets the user create, edit, delete, and order routes. Its local presentation helpers apply the shared technical-paper color, border, spacing, and typography tokens without a UI-library dependency.
- `RouteStore` persists routes as JSON in app-private `SharedPreferences`.
- `RouteMatcher` is a pure Kotlin function that picks the first matching route.
- `SmsReceiver` receives Android's `SMS_RECEIVED` broadcast, extracts message parts, and schedules forwarding on one process-local executor.
- `SmsForwarder` uses `SmsManager` to send the matched message to its destination.

## Data flow

`SMS_RECEIVED` → `SmsReceiver` → PDU decoding → `RouteStore` → `RouteMatcher` → `SmsForwarder` → cellular network.

## Interfaces and boundaries

`RouteMatcher` has no Android dependencies and is unit-tested on the JVM. Android permissions, broadcast delivery, `SharedPreferences`, and `SmsManager` are the platform/storage/network boundaries. The app holds no server connection or database.

## Concurrency

Android invokes the receiver on the main thread. It calls `goAsync()` and uses a single background executor for decoding, matching, and submission to `SmsManager`; the pending result is always finished. There is no service, polling loop, alarm, or periodic worker.

## Important decisions

Routes are evaluated in stored order; the first match wins. Every route needs at least one predicate, and non-empty sender/body predicates are combined with AND. This prevents a mistaken catch-all from forwarding every SMS while making overlapping routes predictable.

## Presentation system

The View-based UI uses app-local resource tokens: warm off-white canvas (`#F3F0E8`), near-black primary text, sharp 3dp bordered surfaces, and small elevation. Blue denotes interaction, green healthy access, amber action required, red destructive actions, purple intelligent/derived output, and grey inactive state. Phone numbers use platform monospace; normal UI uses Android's Roboto-family system sans. The application icon is an adaptive off-white background plus a density-aware, transparent blue SMS-forwarding foreground. These helpers stay in `MainActivity` and `res/mipmap-*` until a second app needs a shared design-system module.
