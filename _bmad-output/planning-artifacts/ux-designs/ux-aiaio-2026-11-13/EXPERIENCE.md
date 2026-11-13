---
name: aiaio
status: final
updated: 2026-11-13
sources:
  - https://www.bluesminds.com/ (visual/feel reference)
  - docs/PLAN.md
design_ref: ./DESIGN.md
---

# aiaio — Experience Spine

> Dark, muted, high-tech command experience for aiaio: luxurious calm, nerdy precision. Visual identity lives in `DESIGN.md` (Stealth Slate A v5, V1 Cyan Duo B1+B3 buttons, ambient digital wolf layer). This spine defines how it works. **Spines win on conflict with any mock/wireframe.**

## Foundation

- **Product:** aiaio — native Android app (Jetpack Compose, Material 3 target implementation) for managing AI coding tools: providers, MCP servers, skills, prompts, sessions, usage, quotas, local routing proxy, sync, and (privileged) runtime/terminal. No root; Shizuku is the optional privileged path.
- **Form factor:** phone-first, tablet-supported. **[ASSUMPTION]** Phone is primary; tablet presents a richer command-center layout (more panels/side-by-side grouping) using the same visual language.
- **UI system:** Jetpack Compose + Material 3 (per plan). `DESIGN.md` defines the brand/visual layer (tokens, component visuals) applied on top of platform conventions.
- **Mode:** dark-first, muted (pass 1). **[ASSUMPTION]** Light mode out of scope for this pass.
- **Stakes:** hobby/personal power-tool context; clarity and trust over marketing flourish.
- **Visual identity:** see `DESIGN.md` (`{colors.*}`, `{typography.*}`, `{rounded.*}`, `{spacing.*}`, `{components.*}`) including `ambient-wolf-layer`.

→ Visual reference (working): `.working/key-dashboard-d1-v1.html`, `.working/key-onboarding-o1-v1.html`, `.working/wolf-bg-demo.html` (all include translucent panels over ambient wolves).

## Information Architecture

**[ASSUMPTION]** IA reflects plan + this UX pass (dashboard/onboarding set whole-app language). Adjust during implementation as screens evolve.

| Surface | Purpose | Reached from |
|---|---|---|
| Onboarding | First-run guided setup to “ready” (with graceful degradation) | First launch / reset |
| Dashboard (Home) | Mission control: system/routing status, health, key stats, quick actions, recent sessions | App open (post-onboarding) / primary nav |
| Providers | Manage providers/models/endpoints, switch active route | Nav / dashboard action |
| MCP | Manage MCP servers and per-app toggles/connectivity | Nav |
| Skills | Installed/discover/repos/updates and toggles | Nav |
| Prompts / Profiles | Manage prompts, templates, profiles; apply/clear | Nav (More) |
| Sessions | Per-tool session list, search, detail | Nav / dashboard recent |
| Usage & Quotas | Heatmaps/logs/cost/FX; quotas/balance/plans | Nav (More) / dashboard stats |
| Proxy / Routing | Local proxy status, failover/mapping/controls | Dashboard / More |
| Sync | Devices, transports (WebDAV/S3/LAN), Sync Doctor, conflicts | Nav (More) |
| Runtime & Terminal | Shizuku/runtime/bottle status, native terminal, logs | Dashboard action / Nav (More) |
| Settings | App/proxy/logging/theme/about, deep links/import-export | Nav (More) |

- **Primary navigation:** bottom bar focus on core areas (e.g. Dashboard, Providers, MCP, Sessions, More) per plan; exact tab set finalized in implementation.
- **[ASSUMPTION]** Dashboard and Onboarding establish the visual/interaction language for all surfaces.
- **Ambient background:** each surface may display a randomly selected ambient wolf motif variant (see Ambient Background Motif); selection is per screen instance/context, not a fixed global background.

## Voice and Tone

Microcopy tone: **confident, calm, precise, technical** — luxurious restraint, no hype. Nerdy where it aids clarity (mono/status labels), human where it aids trust.

| Do | Don't |
|---|---|
| "Proxy active" / "Systems nominal" | "Everything is AWESOME!" |
| "Shizuku not detected · optional" | "Error: you must install Shizuku" |
| "runtime: 3/3 ready" / "peer: verified" (mono/status contexts) | Vague "OK" with no signal |
| "Skip for now — core features remain available" | Shame or block for degraded/optional paths |
| Short, factual status and next-step copy | Marketing fluff, exclamation marks, emoji noise |

Brand posture and visual voice: see `DESIGN.md` (Brand & Style).

## Component Patterns

Behavioral patterns (visual specs/tokens in `DESIGN.md.Components`). Components use `{colors.*}`, `{typography.*}`, `{rounded.*}`.

| Component | Use | Behavior |
|---|---|---|
| **Command Hero (D1)** | Dashboard top | Primary “in control” surface: shows routing/proxy state, active provider/model, latency/failover, and health tags (Shizuku/runtime/peer/MCP). Dominant at-a-glance; updates reflect live status. May include a status ring/indicator for nominal state. |
| **Status pill** | Hero, lists, runtime | Compact mono label for state (e.g. `PROXY:LIVE`, `connected`). Semantic color by state (`{colors.success-*}` nominal; warning/danger variants for degraded/error). |
| **Stat tile** | Dashboard metrics | Mono kicker + prominent mono value (+ optional bar for quota/usage). Tappable tiles may deep-link to detail (e.g. Usage). **[ASSUMPTION]** |
| **Telemetry tag/chip** | Hero, detail cards | Small mono fact labels for precise system attributes; non-interactive by default. |
| **Primary/Secondary button (V1 B1+B3)** | Key actions (dashboard/onboarding) | Prominent, bright cyan-family actions with clean leading icon + left accent rail for branded/technical identity. Primary = main forward action; secondary = alternate/skip/navigation. Min tap target respected. |
| **Session row** | Dashboard recent, Sessions list | Shows tool/name + context; right side mono state/time. Tap opens session/detail. **[ASSUMPTION]** |
| **Progress stepper** | Onboarding | Segmented/step progress indicating current position in guided flow. |
| **Focused step card (O1 flow)** | Onboarding steps | Presents one step’s purpose, status readout, and actions; cinematic welcome leads into guided steps. |
| **Runtime/telemetry strip** | Dashboard/footer area | Compact mono line for endpoint/sync/uptime or key runtime facts. |
| **Ambient wolf background layer** | All app screens (behind content) | Renders 0–3 (configurable, default 1–2) AI-inspired digital wolf motifs per screen. **Random per screen:** motif(s), placement, scale, and rotation vary on screen entry/creation. **Super subtle:** very low peak opacity (target ~0.03–0.06), tinted per `DESIGN.md` (`{colors.wolf-tint}`/`{colors.wolf-tint-alt}`), never obscuring content. Non-interactive, no hit targets, behind all UI/grid/content. Foreground panels/cards are slightly translucent so motifs are faintly visible through them while readability is preserved. **[ASSUMPTION]** exact asset set, count, bounds, and panel alpha tuned in implementation. |

## State Patterns

States must be explicit, calm, and informative — especially for optional privileged runtime.

| State | Surface(s) | Treatment |
|---|---|---|
| **Nominal / live** | Dashboard, proxy/runtime | Hero shows active/healthy (e.g. “Proxy Active”, `PROXY:LIVE`), success indicators, precise metrics. |
| **Proxy off / idle** | Dashboard | Hero reflects stopped/idle state clearly; primary actions offer start/switch/configure. **[ASSUMPTION]** |
| **Shizuku unavailable / optional** | Onboarding, runtime, dashboard strip | Clearly labeled `optional`/not detected with graceful path: explain what it unlocks, allow skip/continue; core features remain usable. No blocking error tone. |
| **Runtime/bottles partial or installing** | Onboarding, runtime | Progress/status readouts (e.g. installed 0/3), in-progress indicators, verified/ready when complete. **[ASSUMPTION]** |
| **Quota/cost warning** | Dashboard stats, usage | Warning semantic styling on relevant stat/tile with clear value/context; non-alarmist. **[ASSUMPTION]** |
| **Empty** | Sessions, providers, etc. | Calm empty message with a clear next action (e.g. add provider, start session). **[ASSUMPTION]** |
| **Error / disconnected** | Proxy, sync, runtime | Factual error state with cause/action where known; avoid blame; preserve navigation. **[ASSUMPTION]** |
| **Loading** | Data surfaces | Subtle skeleton/placeholder consistent with dark muted surfaces; no flashy spinners. **[ASSUMPTION]** |
| **Focus/pressed/disabled** | Interactive controls | Visible focus ring (`{colors.focus-ring}`), pressed feedback subtle, disabled clearly reduced but legible. |
| **Ambient motif (reduced motion)** | All screens with ambient layer | If user prefers reduced motion, disable continuous fading: render a single static, very faint motif (or none) with no animation. |

## Interaction Primitives

- **Tap-first (mobile):** primary interactions via tap on buttons, rows, tiles, nav. Minimum 48dp touch targets.
- **Quick actions:** dashboard exposes high-frequency actions prominently (e.g. Switch provider, Open terminal) — **[ASSUMPTION]** exact set confirmed by product needs.
- **Navigation:** bottom navigation for primary areas; drill-in to detail; back/predictive back supported per platform.
- **Progressive disclosure:** dense/nerdy detail (telemetry tags, mono readouts) visible where useful; deeper logs/config behind detail surfaces.
- **Keyboard/hardware (where applicable):** terminal and text inputs support hardware keyboard; not primary for dashboard/onboarding.
- **Ambient background motion (continuous, subtle):**
  - Motifs **fade in and out at random intervals/durations continuously** while the screen is visible (not only on page/screen load). Each motif cycles independently with randomized delays, fade durations, visible/hidden dwell times, and peak opacity within the subtle cap.
  - Randomization is **per screen instance** (different selection/placement on entry); animation timing remains organic and non-synchronized.
  - Motion is atmospheric and must never draw attention, pulse in sync with content, or affect layout/performance of foreground UI.
  - **Respect reduced motion:** when reduced-motion is enabled, suppress continuous fades/animation and use static or no ambient motif (see State Patterns).
- **[ASSUMPTION]** No drag-heavy or gesture-hidden critical actions; keep mission-control actions explicit.

## Accessibility Floor

Behavioral floor (visual contrast guided by `DESIGN.md` muted palette — verify ratios in implementation).

- **Contrast:** text and interactive elements meet WCAG AA against dark surfaces; bright V1 action buttons maintain clear text/icon contrast. Ambient wolf layer must remain behind content at very low opacity and panels remain sufficiently opaque so legibility is never reduced.
- **Touch targets:** >= 48dp for interactive controls (buttons, rows, nav items).
- **Focus:** visible focus indication for keyboard/switch/assistive navigation using `{colors.focus-ring}`.
- **Labels:** icons paired with text labels on primary actions (B1); status conveyed by text + semantic color (not color alone).
- **Screen readers:** status pills, hero state, metrics, and step progress announced meaningfully. Ambient wolf imagery is purely decorative: mark as non-accessible/decorative (ignored by assistive tech).
- **Motion:** respect reduced-motion preferences; keep transitions subtle. **Continuous ambient wolf fades are disabled or reduced to static/minimal when reduced motion is requested.**
- **Text scaling:** layouts accommodate larger system font sizes without clipping critical status/actions. **[ASSUMPTION]**

## Inspiration & Anti-patterns

- **Lift from Bluesminds:** premium dark confidence, luminous-but-restrained intent, strong hero/status moments, refined hierarchy and calm “mission-critical” polish.
- **Lift (product-fit, nerdy/lux):** instrument-panel clarity — mono telemetry, precise metrics, status chips/tags, command-center “in control” hero (D1); AI-inspired digital/wireframe wolf motifs as a subtle technical signature.
- **Reject:** loud neon/gamer palettes, cluttered dense-noise layouts, gimmicky animations, skeuomorphism, decorative color without meaning.
- **Reject:** blocking or shaming flows for optional/degraded states (Shizuku optional must degrade gracefully).
- **Reject:** prominent, high-contrast, or distracting background art; synchronized/flashy ambient animation.
- **Keep:** medium density, muted sophistication, clear action hierarchy, technical honesty, atmosphere-over-attention.

## Responsive & Platform

- **Phone (primary):** single-column, vertically stacked modules (hero → stats → actions → lists/strips). Comfortable thumb reach for primary actions.
- **Tablet:** richer command-center composition — **[ASSUMPTION]** multi-panel/split arrangements (e.g. hero+stats alongside sessions/runtime detail), increased information at a glance while preserving hierarchy and the same component language.
- **Orientation:** **[ASSUMPTION]** portrait primary; landscape supported where layout permits (especially tablet/terminal contexts).
- **Platform conventions:** follow Android/Material 3 navigation, back behavior, system bars/edge-to-edge, notifications/FGS surfaces per app requirements; `DESIGN.md` supplies visual skin/tokens.
- **Performance:** ambient motif rendering/animation must be lightweight (e.g. simple vector/raster assets, bounded count, efficient compositing) and must not degrade scroll/UI responsiveness.

## Key Flows

Named protagonists illustrate intent; adapt to real users.

### Flow 1 — Morning mission control (Kaos, phone, checking the rig)

1. **Open app (post-setup):** Dashboard loads showing D1 **Command Hero** with current routing/proxy state. A random, super-subtle digital wolf motif may be present in the background, breathing in/out quietly.
2. **Assess at a glance:** Kaos reads “Proxy Active / Systems nominal”, active provider/model, latency, and health tags (Shizuku, runtime, peer, MCP).
3. **Scan metrics:** stat tiles show requests, quota, cost (and/or key usage) with clear values.
4. **Act:** taps **Switch provider** (primary, V1 B1+B3) or **Open terminal** (secondary) depending on need.
5. **Review recents:** glances at recent sessions (status/time) and taps through if needed.  
**Climax:** within seconds Kaos knows the system is healthy/in control and can take a precise action without hunting — atmosphere supports focus without distraction.  
**Degraded variant:** if proxy off or Shizuku unavailable, hero/strip states it plainly with a direct recovery/continue path.

### Flow 2 — Cinematic onboarding to ready (Kaos, first run, phone)

1. **Welcome (O1):** cinematic dark hero/mark, confident value line (“AI command center, on device”), calm premium pacing. A different random ambient wolf motif may softly fade in/out in the background.
2. **Guided steps:** progress indicates path to ready (e.g. Shizuku optional → runtime/bottles → add first provider → proxy ready).
3. **Shizuku (optional, step 2):** focused card explains benefit (runtime, PTY/native terminal, bottle installs) with clear `recommended · optional` status and factual detection readout.
4. **Choice:** **Set up Shizuku** (primary) or **Skip for now** (secondary) — skip keeps core features available; app degrades gracefully.
5. **Continue setup:** install runtime tools / add provider as applicable, each with transparent status.
6. **Ready:** reaches dashboard in a usable state (full or core-only depending on choices).  
**Climax:** Kaos feels guided and in control — premium welcome, technically honest steps, signature ambient identity, no dead-end if privileged runtime is skipped.

### Flow 3 — Quick recovery/glance (Kaos, noticing an issue)

1. **Notice:** dashboard hero/stat or pill signals warning/error (e.g. quota high, proxy off, runtime partial).
2. **Understand:** mono/label text states what’s affected and current value.
3. **Resolve:** clear action (start proxy, connect runtime, view usage) from dashboard or linked surface.  
**Climax:** problem is legible and actionable without alarm. Ambient motion never competes with alerts. **[ASSUMPTION]**

## Working Artifacts (reference)

- `.working/color-themes-3.html`, `.working/color-themes-4.html`, `.working/color-themes-5.html` — palette evolution (muted Stealth Slate A, button/corner refinements)
- `.working/directions-dashboard-2.html`, `.working/directions-onboarding-2.html` — D1–D3, O1–O3 in applied palette
- `.working/buttons-v5-icons.html`, `.working/buttons-v5-b1b3-bright.html`, `.working/buttons-v5-b1b3-bright-orange.html` — button branding explorations
- `.working/key-dashboard-d1-v1.html` — D1 dashboard key screen (V1 B1+B3)
- `.working/key-onboarding-o1-v1.html` — O1 cinematic onboarding key screen (V1 B1+B3)
- `.working/wolf-bg-demo.html` — ambient AI-inspired digital wolf background concept (random per screen, super subtle continuous random fade in/out, reduced-motion aware)

These are explorations; **DESIGN.md and EXPERIENCE.md are the contracts**.
