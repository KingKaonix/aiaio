---
name: aiaio
description: Dark, muted, high-tech command UI for managing AI coding tools — luxurious calm with nerdy precision.
status: final
updated: 2026-11-13
sources:
  - https://www.bluesminds.com/ (visual/feel reference)
  - docs/PLAN.md
colors:
  bg-base: '#010203'
  bg-canvas: '#06080C'
  surface: 'rgba(10, 14, 20, 0.80)'
  surface-raised: 'rgba(15, 20, 28, 0.82)'
  surface-hero: 'rgba(11, 18, 26, 0.80)'
  border-subtle: '#131923'
  border-default: '#151C27'
  border-strong: '#182231'
  border-accent: '#2A5F78'
  text-primary: '#E8EEF7'
  text-strong: '#D9E1EC'
  text-secondary: '#A9B4C2'
  text-muted: '#7F8EA3'
  text-faint: '#748090'
  accent: '#5F879E'
  accent-bright: '#7FA6BC'
  accent-rail: '#4FD6FF'
  accent-rail-secondary: '#38C4EF'
  accent-rail-soft: 'rgba(79, 214, 255, 0.22)'
  grid-line: 'rgba(95, 135, 158, 0.035)'
  success: '#5E9C86'
  success-soft: 'rgba(94, 156, 134, 0.10)'
  success-border: 'rgba(94, 156, 134, 0.22)'
  success-text: '#7FB39D'
  warning: '#B8A06A'
  danger: '#B06A74'
  focus-ring: '#4FD6FF'
  button-primary-bg: '#1B4F66'
  button-primary-border: '#2F7F9E'
  button-primary-text: '#EAF8FF'
  button-primary-icon: '#7FE6FF'
  button-secondary-bg: '#164659'
  button-secondary-border: '#286F8A'
  button-secondary-text: '#E6F6FF'
  button-secondary-icon: '#74DCFF'
  wolf-tint: '#5F879E'
  wolf-tint-alt: '#4FD6FF'
  wolf-tint-soft: 'rgba(95, 135, 158, 0.6)'
typography:
  display:
    fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif'
    fontSize: 28px
    fontWeight: 650
    lineHeight: 1.12
    letterSpacing: '-0.03em'
  headline:
    fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif'
    fontSize: 22px
    fontWeight: 650
    lineHeight: 1.2
    letterSpacing: '-0.02em'
  title:
    fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif'
    fontSize: 16px
    fontWeight: 650
    lineHeight: 1.3
    letterSpacing: '-0.01em'
  body:
    fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif'
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.55
    letterSpacing: '0'
  label:
    fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif'
    fontSize: 12px
    fontWeight: 600
    lineHeight: 1.4
    letterSpacing: '0'
  mono:
    fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace'
    fontSize: 12px
    fontWeight: 500
    lineHeight: 1.45
    letterSpacing: '0'
  mono-kicker:
    fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace'
    fontSize: 10px
    fontWeight: 600
    lineHeight: 1.3
    letterSpacing: '0.08em'
rounded:
  sm: 6px
  md: 8px
  lg: 12px
  xl: 14px
  pill: 999px
spacing:
  '1': 4px
  '2': 6px
  '3': 8px
  '4': 10px
  '5': 12px
  '6': 14px
  '7': 16px
  '8': 18px
  '9': 20px
  '10': 24px
  '11': 28px
components:
  button-primary:
    background: '{colors.button-primary-bg}'
    border: '{colors.button-primary-border}'
    text: '{colors.button-primary-text}'
    icon: '{colors.button-primary-icon}'
    radius: '{rounded.md}'
    rail: '{colors.accent-rail}'
  button-secondary:
    background: '{colors.button-secondary-bg}'
    border: '{colors.button-secondary-border}'
    text: '{colors.button-secondary-text}'
    icon: '{colors.button-secondary-icon}'
    radius: '{rounded.md}'
    rail: '{colors.accent-rail-secondary}'
  card:
    background: '{colors.surface}'
    border: '{colors.border-default}'
    radius: '{rounded.md}'
  hero-card:
    background: '{colors.surface-raised}'
    border: '{colors.border-strong}'
    radius: '{rounded.lg}'
  status-pill:
    background: '{colors.success-soft}'
    border: '{colors.success-border}'
    text: '{colors.success-text}'
    radius: '{rounded.pill}'
  ambient-wolf-layer:
    tints: ['{colors.wolf-tint}', '{colors.wolf-tint-alt}']
    opacity-peak: '0.03–0.06'
    blend: 'normal (very low alpha on dark canvas)'
    panel-translucency: 'surfaces ~0.78–0.84 alpha over ambient layer'
---

## Brand & Style

aiaio’s UI should feel like a **luxurious command console**: calm, premium, and in-control — with a **nerdy, high-tech** undercurrent expressed through precision, telemetry, and system awareness rather than noise. The mood takes inspiration from the dark, confident, mission-critical elegance of [bluesminds.com](https://www.bluesminds.com/) (premium dark canvas, luminous intent, refined hierarchy) while staying purpose-built for an AI tooling control app.

The visual language is **dark-first and muted** (Stealth Slate): deep near-black surfaces, quiet steel accents, restrained glow, and crisp structure. Luxury comes from spacing, hierarchy, depth, and polish; nerdiness comes from mono readouts, status language, subtle grid/technical texture, and precise metrics. Color is used with discipline — accents signal state and action, never decoration for its own sake.

A signature ambient layer adds **AI-inspired digital wolf imagery** to screen backgrounds: geometric/wireframe/constellation wolf motifs rendered in muted tints at extremely low opacity. They act as a subtle, living technical watermark — felt more than seen — reinforcing the app’s identity without competing with content. See **Components → Ambient wolf background layer** and `EXPERIENCE.md` (Ambient Background Motif behavior).

**Anti-patterns (avoid):** loud/neon palettes, cluttered dashboards, gimmicky effects, skeuomorphism, heavy ornament, playful gamified chrome, and any background imagery that distracts from content or reduces legibility.

## Colors

Palette is intentionally muted and neutral-cool so the interface feels dark, focused, and premium while remaining technical.

- **Base / Canvas (`{colors.bg-base}`, `{colors.bg-canvas}`):** near-black foundations for the app shell and screen backgrounds. Keep surfaces dark and quiet.
- **Surfaces (`{colors.surface}`, `{colors.surface-raised}`, `{colors.surface-hero}`):** layered dark panels for cards and hero regions with slight translucency so ambient background motifs remain faintly visible; separation is achieved by subtle tone and border shifts, not bright fills.
- **Borders (`{colors.border-subtle}`, `{colors.border-default}`, `{colors.border-strong}`, `{colors.border-accent}`):** hairline-to-structural edges that define modules and technical framing. Use `{colors.border-accent}` sparingly for focus/featured framing.
- **Text (`{colors.text-primary}`, `{colors.text-strong}`, `{colors.text-secondary}`, `{colors.text-muted}`, `{colors.text-faint}`):** high-legibility cool neutrals. Reserve `{colors.text-primary}` for key values/headlines; use `{colors.text-muted}`/`{colors.text-faint}` for supporting telemetry and labels.
- **Accent (`{colors.accent}`, `{colors.accent-bright}`):** muted steel-blue for emphasis, links-to-meaning, and restrained highlights. Keep low saturation.
- **Action rails (`{colors.accent-rail}`, `{colors.accent-rail-secondary}`):** bright cyan signals used **specifically** for button branding (B1 icon + B3 left rail) to give actions clear, technical identity without making the whole UI loud.
- **Semantic (`{colors.success}`, `{colors.warning}`, `{colors.danger}` + soft variants):** status only (health, live, caution, error). `{colors.success}` indicates nominal/connected/live states.
- **Grid (`{colors.grid-line}`):** optional subtle technical texture on key surfaces; never dominant.
- **Ambient wolf tints (`{colors.wolf-tint}`, `{colors.wolf-tint-alt}`):** muted steel-blue / restrained cyan used for background wolf motifs at extremely low alpha only; never used as foreground or high-contrast decoration.

Dark-first, single-mode palette for this pass. **[ASSUMPTION]** Light mode is out of scope for pass 1.

## Typography

Sleek sans for product clarity and a premium feel; monospace for nerdy precision and telemetry.

- **Sans (`{typography.display.fontFamily}` / body family):** Inter/system sans for headlines, labels, and UI copy. Confident weights and tight tracking for hero/display moments (`{typography.display}`, `{typography.headline}`).
- **Mono (`{typography.mono.fontFamily}`):** used for metrics, routes, IDs, endpoints, paths, status codes, timestamps, and technical readouts (e.g. `openai/gpt-5 · 42ms`, `127.0.0.1:8787`, `runtime:3/3`).
- **Kickers (`{typography.mono-kicker}`):** small uppercase mono labels for system sections (e.g. `ROUTING STATUS`, `REQ_24H`, `STEP 2 / 5`).

Hierarchy is calm and deliberate: large hero values/titles, compact meta, and crisp mono telemetry. Avoid decorative type; legibility and precision come first.

## Layout & Spacing

Medium density: information-rich but breathable, with clear grouping and generous touch targets.

- **Scale (`{spacing}`):** 4–28px rhythm. Use tighter gaps inside stat tiles/lists and larger gaps between major sections (hero → stats → actions → sessions).
- **Structure:** card-and-panel composition with a strong hero region (D1 Command Hero). Group related telemetry into modules; keep actions visually distinct from readouts.
- **Phone/tablet:** phone-first single-column flow; tablet evolves into a richer command-center layout (more panels/side-by-side grouping) while preserving the same visual language. **[ASSUMPTION]** Tablet-specific split/command-center arrangement to be detailed in implementation.
- **Layering:** background canvas → **ambient wolf layer** (behind UI, above base canvas, below grid/content) → subtle grid texture → content surfaces/cards. The ambient layer must never reduce text/UI contrast or readability.
- **Texture:** subtle grid may be applied at low opacity (`{colors.grid-line}`) over the canvas/ambient layer for technical character.

## Elevation & Depth

Depth is restrained and premium — quiet separation, not dramatic shadows.

- Prefer **tonal layering** (base → canvas → ambient motif → surface → raised/hero) with `{colors.border-default}`/`{colors.border-strong}` edges over heavy drop shadows. Panels/cards use slight translucency so the ambient wolf layer can breathe through subtly while preserving legibility.
- Hero/featured cards may use a soft gradient and minimal ambient emphasis (very restrained glow tied to `{colors.accent}`/status) to convey “in control” focus.
- Avoid glassmorphism excess, strong bloom, or stacked shadows that create clutter.

## Shapes

A slightly tightened, technical geometry (less rounding than early explorations) balanced with premium smoothness.

- **Controls & inner modules:** `{rounded.md}` (8px) for buttons, stat tiles, list containers.
- **Panels/cards:** `{rounded.lg}` (12px) for primary cards and hero modules.
- **Device/frame accents:** `{rounded.xl}` (14px) where framed.
- **Status/chips:** `{rounded.pill}` for live/status pills and chips.
- Accent **rails** on buttons are straight, crisp (2–3px), reinforcing a precise, instrument-like feel.
- **Wolf motifs:** angular/geometric/wireframe/constellation forms (digital, technical) — not illustrative/cartoon; silhouettes and facets read as engineered.

## Components

Visual specs (behavior in EXPERIENCE.md). Key components reflect D1 dashboard and O1 onboarding.

- **Primary button (`button-primary`, V1 B1+B3):** filled `{colors.button-primary-bg}` with `{colors.button-primary-border}`, text `{colors.button-primary-text}`. **Clean leading icon (B1)** in `{colors.button-primary-icon}` and a **left accent rail (B3)** in `{colors.accent-rail}`. Bright, legible, branded action.
- **Secondary button (`button-secondary`, V1 B1+B3):** filled `{colors.button-secondary-bg}` with `{colors.button-secondary-border}`, text `{colors.button-secondary-text}`. Leading icon `{colors.button-secondary-icon}` and left rail `{colors.accent-rail-secondary}`. Also bright/vivid but distinct in the cyan family.
- **Status pill / badge:** compact pill using success-soft styling for live/nominal states (e.g. `PROXY:LIVE`), mono uppercase text.
- **Hero status card (D1):** large command centerpiece on `{colors.surface-hero}`/`{colors.border-strong}` showing primary system state (“Proxy Active / Systems nominal”), active route/model + latency/failover in mono, status ring/indicator, and telemetry tags.
- **Stat tile:** compact raised module with mono kicker label, prominent mono value, optional progress bar (quota/usage) using muted accent fill.
- **Telemetry tag/chip:** small mono label in bordered dark chip for runtime/health facts (`shizuku:connected`, `runtime:3/3`, `peer:verified`).
- **Session/list row:** title + context with right-aligned mono state/time (`ok · 2m`, `idle · 18m`); hairline separators.
- **Progress stepper (onboarding):** segmented bar with active segment in accent; step dots/states for current/done/todo.
- **Onboarding hero mark (O1):** restrained luminous mark/badge on dark canvas, premium and cinematic but muted.
- **Ambient wolf background layer (`ambient-wolf-layer`):** a non-interactive, behind-content layer rendering AI-inspired digital wolf motifs (wireframe/low-poly/constellation/circuit-trace styles). Tinted with `{colors.wolf-tint}` / `{colors.wolf-tint-alt}` at **very low peak opacity (~0.03–0.06)**, large/partial placement, no hard edges or fills that compete with UI. Foreground panels are slightly translucent to let this layer show through subtly. Selection/animation behavior defined in `EXPERIENCE.md`.

## Do's and Don'ts

| Do | Don't |
|---|---|
| Keep the canvas dark, muted, and calm (Stealth Slate neutrals) | Use loud neon palettes or saturate the whole UI |
| Use bright color purposefully on actions (V1 cyan B1+B3 rails/icons) and semantic status | Scatter bright accents as decoration |
| Pair sleek sans UI copy with mono telemetry/readouts for nerdy precision | Use monospace everywhere or decorative/display gimmicks |
| Show clear system state (“in control”): hero status, precise metrics, runtime health | Hide critical state behind vague labels or clutter |
| Use subtle grid, crisp borders, and tonal depth for a technical/lux feel | Add heavy glow, glassmorphism excess, or skeuomorphic chrome |
| Render wolf motifs as **super subtle**, low-opacity, behind-content digital geometry | Let background imagery compete with content, reduce contrast, or become a focal point |
| Keep medium density with strong hierarchy and breathable grouping | Overcrowd screens or create dense, unscannable walls of data |
| Make optional/degraded states (e.g. Shizuku optional) explicit and graceful | Block core understanding or shame degraded/offline states |
| Treat ambient motion as calm atmosphere; honor reduced-motion preferences | Add flashy, distracting, or continuous high-motion effects |
