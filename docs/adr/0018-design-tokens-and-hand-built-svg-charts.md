# ADR 0018: CSS design tokens and hand-built SVG charts

**Status:** Accepted

## Context

The first version of the frontend was functional but plain: unstyled tables and text, no charts. Finance users read budget-vs-actual reports in seconds by looking for shape (which months missed, how far the year will land from budget) before reading numbers. The redesign needed charts, a consistent look across four pages, light and dark themes, and had to keep every accessible name the tests rely on. Everything must stay free.

## Decision

- **Plain CSS with design tokens** in `index.css`: colours, radii, shadows and spacing are CSS custom properties on `:root`, redefined for dark mode (under `prefers-color-scheme`, or `data-theme` when the user picks a theme). Components use classes, never hard-coded colours.
- **One accent colour and reserved status colours.** Blue marks data and interaction. Green and red are kept for favorable/unfavorable, always with an icon and a text label, so meaning never depends on colour alone. The chart palette was checked for colour-vision deficiency and contrast in both themes.
- **Charts are hand-built SVG components** (`src/charts/`): a monthly chart (actual columns, budget ticks) and a cumulative outlook chart (actual, dashed forecast, Best–Worst band, budget). Small helpers (`linearScale`, `niceTicks`, `formatAxisMoney`) are unit tested. Each chart has hover and keyboard tooltips, a legend, one y-axis starting at zero, and a table view nearby.
- **Inter (self-hosted through Fontsource) and Lucide icons,** both open source and bundled at build time, so there are no runtime calls to font or icon CDNs.
- **A collapsible sidebar** turns into an icon rail, giving the 16-column forecast table room on a 1440px screen.
- **Refetches keep the previous content on screen, dimmed,** rather than flashing a loader; skeleton placeholders appear only on first load.

## Alternatives considered

- **A component library (MUI, Ant Design, Chakra).** Faster to start, but it adds a large dependency and a look that is hard to make feel specific, and replacing its table styles for dense financial data takes about as much CSS as writing them.
- **Tailwind CSS.** Productive, but it would mean rewriting every component's markup, and tokens in plain CSS already give one source of truth for theming.
- **A chart library (Recharts, Chart.js, ECharts).** Good for many chart types. We need two charts with precise control (budget as a tick rather than a second bar, a forecast band from the last closed month, exact money formatting in tooltips). Hand-built SVG is about 300 lines with no dependency, renders in jsdom for tests, and is easy to explain line by line.

## Consequences

- No new runtime services and only two small dependencies (font files and icons).
- New chart types would need more hand-written code; if the app grew many charts, a library would become worth it.
- Chart geometry uses floating point for pixel positions only; every figure shown in a tooltip or label is formatted from the exact amount string.
