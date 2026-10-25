# ADR 0009: Send money as JSON strings and do frontend arithmetic in cents

**Status:** Accepted

## Context

The backend uses `BigDecimal` for exact money. JSON numbers are parsed by JavaScript into binary floating-point `number`s, which cannot represent most cent values exactly (`0.1 + 0.2 === 0.30000000000000004`). The budget grid adds amounts in the browser to show live totals.

## Decision

- Every `BigDecimal` is serialized as a JSON string using its plain form (`"1234.50"`, never `1234.5` or `"1.2345E+3"`). This is configured once, globally, with a Jackson module (`MoneyJsonConfiguration`).
- Requests may send amounts as strings or numbers; they are bound to `BigDecimal` and validated (`@Digits(integer = 13, fraction = 2)`).
- The frontend keeps amounts as strings and converts to integer cents for arithmetic (`lib/money.ts`), which is exact for any realistic amount.

## Alternatives considered

- **JSON numbers + rounding in the UI.** Common, and usually invisible, but totals can be off by a cent and comparisons can fail.
- **Integer cents in the API** (`123450`). Exact, but every client must remember to divide by 100, and the API stops being human-readable.
- **A decimal library in the frontend** (e.g. decimal.js). Works, but adds a dependency for what is only addition and formatting.

## Consequences

- No precision is lost between database, API and UI.
- Clients must not treat amounts as numbers without parsing them; the TypeScript types make this explicit (`amount: string`).
