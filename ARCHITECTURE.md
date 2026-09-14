# Architecture

## Components

- `MainActivity` requests SMS permissions and lets the user create, edit, delete, and order routes. Its sender and destination editors can open the system phone-number Contacts picker after an on-demand Contacts permission grant. Local presentation helpers apply the shared technical-paper color, border, spacing, and typography tokens without a UI-library dependency.
- `RouteStore` persists routes as JSON in app-private `SharedPreferences`.
- `RouteMatcher` is a pure Kotlin function that picks the first matching route using literal message terms, advanced regex, and an optional lazy sender-name provider.
- `ContainsMessageTerms` parses and matches comma-OR / plus-AND literal message terms.
- `RouteRegex` validates and performs bounded, case-insensitive Java regular-expression searches for sender and message conditions.
- `ContactNameResolver` does one permission-aware PhoneLookup query when a raw sender did not match a Contains sender rule.
- `SmsReceiver` receives Android's `SMS_RECEIVED` broadcast, extracts message parts, and schedules forwarding on one process-local executor.
- `SmsForwarder` uses `SmsManager` to send the matched message to its destination.

## Data flow

`SMS_RECEIVED` → `SmsReceiver` → PDU decoding → `RouteStore` → `RouteMatcher` → `SmsForwarder` → cellular network. When necessary, `RouteMatcher` lazily calls `ContactNameResolver` through the receiver to compare a sender rule with one Contacts display name.

## Interfaces and boundaries

`RouteMatcher`, `ContainsMessageTerms`, and `RouteRegex` have no Android dependencies and are unit-tested on the JVM. Android permissions, PhoneLookup, the system Contacts picker, broadcast delivery, `SharedPreferences`, and `SmsManager` are the platform/storage/network boundaries. The picker supplies one selected name or number; contact records and the address book are not persisted. The app holds no server connection or database.

## Concurrency

Android invokes the receiver on the main thread. It calls `goAsync()` and uses a single background executor for decoding, matching, and submission to `SmsManager`; the pending result is always finished. There is no service, polling loop, alarm, or periodic worker.

## Important decisions

Routes are evaluated in stored order; the first match wins. Every route needs at least one predicate, and non-empty sender/body predicates are combined with AND. New routes default to case-insensitive literal contains matching: sender rules compare the raw address first, then one contact name if available; message comma terms are OR and plus terms are AND, with AND precedence. Advanced routes use case-insensitive Java regex (`^`/`$` make a full match; `\b` selects words), limited to 256 characters. Invalid stored regex and malformed new message terms never match. Existing persisted routes without a message-syntax key remain literal, while `senderRegex`/`messageRegex` records load as advanced routes.

## Presentation system

The View-based UI uses app-local resource tokens: warm off-white canvas (`#F3F0E8`), near-black primary text, sharp 3dp bordered surfaces, and small elevation. Blue denotes interaction, green healthy access, amber action required, red destructive actions, purple intelligent/derived output, and grey inactive state. Phone numbers use platform monospace; normal UI uses Android's Roboto-family system sans. The application icon is an adaptive off-white background plus a density-aware, transparent blue SMS-forwarding foreground. These helpers stay in `MainActivity` and `res/mipmap-*` until a second app needs a shared design-system module.
