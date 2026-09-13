# SMS Forwarder

An Android app that forwards an incoming SMS to a configured destination when its sender and/or message text match a route.

## Why this exists

Some messages, such as alerts from a service or a particular contact, need to reach another phone without running a permanent background process.

## Demo

Create a route with `Sender contains: BANK`, `Message contains: OTP`, and a destination number. The next SMS matching both populated fields is forwarded to that destination.

## What is interesting technically

The receiver is event-driven: Android wakes the app only for `SMS_RECEIVED`. Route lookup and sending run on a small background executor, so the broadcast receiver does not block the UI or keep a service alive.

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md).

## How it works

Routes are stored locally on-device. Each route must contain a sender or message condition. A populated sender field and a populated message field must both match; an empty one is a wildcard. Routes are evaluated in display order, and the first match is forwarded. Long SMS bodies are split by Android's SMS manager when necessary.

## Performance / Benchmarks

See [BENCHMARKS.md](BENCHMARKS.md) and [HOTSPOTS.md](HOTSPOTS.md).

## Build and run

See [BUILD.md](BUILD.md). On a device, grant SMS permissions from the app, configure at least one route, then send a test SMS. The app does not poll or run a foreground/background service.

## Experiments

Future work can add optional per-route forwarding history and more explicit rule operators while preserving the no-polling design.

## Design decisions

- Route configuration stays on the device in private preferences; it is not sent to a server.
- Matching is case-insensitive substring matching to make rules understandable and deterministic.
- The first matching route wins, which prevents accidental duplicate forwards from overlapping rules.
- The app does not abort the system SMS broadcast or alter the original message.
- The interface uses a technical-paper visual system: neutral off-white chrome, sharp bordered surfaces, and semantic color only for interactive, healthy, pending, and destructive states. The tokens stay app-local until another app needs the same implementation.
- The launcher icon is a blue SMS-bubble/forward-arrow mark on the same off-white surface; Android 8+ uses it as an adaptive icon and older devices use density-specific bitmap fallbacks.

## Limitations

- The user must grant `RECEIVE_SMS` and `SEND_SMS` at runtime.
- The app must be installed on a device that allows the required SMS permissions. Google Play restricts apps that request SMS permissions, so distribution may require default-SMS-app eligibility or another permitted use case.
- Forwarding incurs normal carrier charges and cannot run if the device has no cellular service.
- Routes are local to one device and are not encrypted beyond Android app-private storage.

## Related projects

This project intentionally relies only on Android platform APIs rather than a persistent background-worker framework.

## Article

The implementation follows Android's broadcast-receiver lifecycle guidance: finish quickly, move small work off the main thread, and avoid long-running background work.

## Status / Roadmap

The first version supports local, ordered sender/content routes and direct SMS forwarding. See [TODO.md](TODO.md) for the current acceptance checklist.

## Missing information

Tell us if the app needs multiple destinations per rule, regular expressions, forwarding history, a default-SMS-app role, or enterprise/device-owner deployment; those choices change the routing and distribution design.
