# ADR 0014: Scenarios as separate sets of driver values

**Status:** Accepted

## Context

Planners compare outcomes under different assumptions: Base (most likely), Best and Worst. The scenarios share the same structure (which lines are driver-based and how) but differ in the driver values.

## Decision

- Three fixed scenarios (`BASE`, `BEST`, `WORST`) are reference data inserted by the V2 migration.
- **The method and labels belong to the line** (`forecast_line`); **values belong to the line and scenario** (`forecast_driver_value`, unique per line and scenario). A line can't be units × rate in one scenario and budget in another.
- Driver-based lines must have values for **every** scenario. Validation rejects a save that leaves one out, with a field path such as `lines[0].values.BEST.rate`.
- Saving stores only the values the method uses (quantity and rate, or percent), so a line never carries stale numbers from a previous method. Switching a line to `BUDGET` deletes its values (orphan removal).
- The UI edits all scenarios side by side, and `GET /api/forecast/compare` returns every scenario's outlook at once.

## Alternatives considered

- **Best/Worst as percentage adjustments to Base** (e.g. Best = Base drivers +5%). Less input, but real scenarios move different drivers differently (Worst: fewer contracts *and* higher fuel cost per van), and a single multiplier can't express that.
- **Copy the whole forecast per scenario**, with its own methods. Maximum flexibility, but scenarios become structurally incomparable and the data model duplicates configuration.
- **User-defined scenarios.** Easy to add later (the table already supports it), but three fixed scenarios match the requirement and keep the UI simple.

## Consequences

- Scenarios are always comparable line by line.
- Adding a scenario later means inserting a row and supplying values; the engine and reports already loop over scenarios.
