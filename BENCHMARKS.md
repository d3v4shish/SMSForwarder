# Benchmarks

## Method

`scripts/benchmark.sh` runs deterministic JVM unit tests that execute a fixed 100,000-route-selection workload across 100 ordered rules: 75 literal contains non-matches followed by one case-insensitive comma/plus message-term match. It measures matcher work only; it does not measure Contacts lookup or carrier delivery, which are external and variable.

## Results

Measured on 2026-09-14 with `scripts/benchmark.sh`:

- Host: AMD Ryzen 7 7800X3D (16 logical CPUs), JDK 21 running Java 17-targeted output, Android SDK Platform 36.
- Fixed workload: 100,000 ordered route selections across 100 routes.
- Result: **280 ms** using the comma/plus message-term matcher.

The historical 124 ms substring-only result, 357 ms regex-only result, and 245 ms mixed contains/regex result are not directly comparable to this message-term contract. No performance improvement is claimed.

This is the initial baseline for the new project, not a claimed optimization. The workload runs locally on the JVM and excludes telephony/carrier delivery.

## Interpretation

The benchmark is a regression signal, not a device battery measurement. Battery efficiency comes chiefly from platform broadcast delivery and the absence of polling or a persistent service.
