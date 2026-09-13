# Hotspots

- **Wakeups / battery:** Incoming `SMS_RECEIVED` broadcasts are the only trigger. There is no polling, alarm, service, socket, or periodic worker.
- **Main thread:** PDU decoding, JSON loading, rule matching, and SMS submission run on the receiver's single background executor.
- **Storage / matching:** Routes use one small `SharedPreferences` JSON payload. Route loading and selection are linear in configured routes and occur only per received SMS. Default literal contains matching has no compilation work; valid advanced regular expressions are cached in a bounded 256-entry map and limited to 256 characters.
- **Network / carrier:** `SmsManager` submission and eventual delivery depend on the telephony stack and carrier; the app cannot make this deterministic.
- **Contention:** A single executor serializes forwarding within the process, avoiding concurrent preference reads and repeated thread creation during burst delivery.
