# Benchmarks

## Method

`scripts/benchmark.sh` runs deterministic JVM unit tests that execute a fixed 100,000-route-selection workload. It measures matcher work only; it does not measure carrier delivery, which is external and variable.

## Results

Measured on 2026-09-13 with `scripts/benchmark.sh`:

- Host: AMD Ryzen 7 7800X3D (16 logical CPUs), JDK 21 running Java 17-targeted output, Android SDK Platform 36.
- Fixed workload: 100,000 ordered route selections across 100 routes.
- Result: **124 ms**.

This is the initial baseline for the new project, not a claimed optimization. The workload runs locally on the JVM and excludes telephony/carrier delivery.

## Interpretation

The benchmark is a regression signal, not a device battery measurement. Battery efficiency comes chiefly from platform broadcast delivery and the absence of polling or a persistent service.
