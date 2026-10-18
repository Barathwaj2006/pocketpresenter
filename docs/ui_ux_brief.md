# PocketPresenter — UI/UX Design Brief

| | |
|---|---|
| **Version** | 1.1 |
| **Design intent** | Precise, quiet, premium and presentation-first |
| **Source references** | [prd.md](prd.md) · [app_flow.md](app_flow.md) · [architecture.md](architecture.md) |

## Table of contents

1. [Product personality](#1-product-personality)
2. [Visual direction](#2-visual-direction)
3. [Typography](#3-typography)
4. [Layout and components](#4-layout-and-components)
5. [Touchpad design](#5-touchpad-design)
6. [The three Windows cursor effects](#6-the-three-windows-cursor-effects)
7. [Android screens](#7-android-screens)
8. [Motion and haptics](#8-motion-and-haptics)
9. [Accessibility and usability](#9-accessibility-and-usability)
10. [Design anti-patterns to avoid](#10-design-anti-patterns-to-avoid)
11. [Design acceptance checks](#11-design-acceptance-checks)
12. [Revision history](#12-revision-history)

## 1. Product personality

PocketPresenter should feel like a purpose-built instrument for presenters—not a generic remote app or gaming interface. The visual signature is a dark, tactile surface with restrained cyan/teal feedback and finely drawn geometry.

Design principles:
- Functional clarity over decoration.
- Low visual noise and obvious connection state.
- Minimal but perceptible 3D depth.
- Animations respond to deliberate input and settle quickly.
- Cursor movement always feels independent and immediate.
- No fake feature cards, fabricated testimonials or unnecessary onboarding pages.

## 2. Visual direction

### Color tokens

Use semantic tokens so colors can be adjusted without rewriting components.

| Token | Initial target | Usage |
|---|---|---|
| `background` | `#101416` | Main application background |
| `surface` | `#181E21` | Main panels and touchpad surroundings |
| `surfaceElevated` | `#222B2F` | Menus and focused controls |
| `borderSubtle` | `#344247` | Dividers and restrained outlines |
| `textPrimary` | `#F2F5F5` | Main labels |
| `textSecondary` | `#A8B5B9` | Supporting text |
| `accent` | `#58D6CE` | Active selection and interaction feedback |
| `accentMuted` | `#2D807C` | Low-intensity accent |
| `warning` | `#E7B86C` | Recoverable warnings |
| `error` | `#E47878` | Errors and disconnected states |
| `success` | `#82C8A6` | Confirmed connection/readiness |

These are initial tokens; test contrast on real displays before finalizing. Do not use a purple gradient as the primary visual identity.

#### Measured contrast (WCAG 2.x relative luminance, computed 2026-10-18)

| Token | on `background` | on `surface` | on `surfaceElevated` | Use for |
|---|---|---|---|---|
| `textPrimary` | 16.90 | 15.37 | 13.17 | Body text — passes AAA |
| `textSecondary` | 8.80 | 8.00 | 6.86 | Supporting text — passes AAA |
| `accent` | 10.52 | 9.57 | 8.20 | Text and icons — passes AAA |
| `warning` | 10.12 | 9.20 | 7.89 | Text and icons — passes AAA |
| `error` | 6.40 | 5.82 | 4.99 | Text and icons — passes AA (≥ 4.5) |
| `success` | 9.49 | 8.63 | 7.40 | Text and icons — passes AAA |
| `accentMuted` | 3.96 | 3.60 | 3.09 | **Not for text.** Non-text graphics only (≥ 3:1) |
| `borderSubtle` | 1.78 | 1.62 | 1.39 | **Decorative dividers only.** Fails 3:1 |

Consequences:
- Controls must **not** rely on `borderSubtle` alone to be perceivable; give interactive controls a fill, label or an `accent`/`textSecondary` outline.
- `accentMuted` is acceptable for low-emphasis graphics on `background`/`surface`, never for labels.
- These are numeric checks of the tokens only. Real-display, outdoor-light and projector testing is still required (§11).

#### Token implementation (Android)

```kotlin
object PpColors {
    val Background = Color(0xFF101416)
    val Surface = Color(0xFF181E21)
    val SurfaceElevated = Color(0xFF222B2F)
    val BorderSubtle = Color(0xFF344247)
    val TextPrimary = Color(0xFFF2F5F5)
    val TextSecondary = Color(0xFFA8B5B9)
    val Accent = Color(0xFF58D6CE)
    val AccentMuted = Color(0xFF2D807C)
    val Warning = Color(0xFFE7B86C)
    val Error = Color(0xFFE47878)
    val Success = Color(0xFF82C8A6)
}
```

The Windows renderer uses the same `accent`/`accentMuted` values as its source palette so phone and cursor effect look related.

## 3. Typography

- Use the Android system sans-serif or a single licensed, legible sans-serif family.
- Prefer short labels and clear status text over decorative display typography.
- Use a consistent scale, approximately 12–14 sp for secondary labels, 14–16 sp for controls/body text, and 20–24 sp for screen titles, adjusted for accessibility and screen size.
- Respect system font scaling; do not clip labels when users increase text size.
- Use sentence case for labels: `Start session`, `Touchpad`, `Honeycomb`, `Pocket mode`.

## 4. Layout and components

- Respect system insets and gesture navigation.
- Use an 8 dp spacing grid, with 4 dp increments where tighter alignment is needed.
- Main touchpad should occupy the largest available interactive area; controls must not overlap it.
- Primary actions must have a clear visual hierarchy without oversized pill-shaped controls everywhere.
- Use conventional iconography for back, settings, connection, click and scrolling; always include labels when the meaning is not unambiguous.
- Keep paired/connected/presenting/error statuses separate and textual, not represented by color alone. Labels are defined in [app_flow.md §3](app_flow.md#3-state-driven-controls).
- Touch targets should generally be at least 48 dp where practical.
- Include a visible off state for the effect selector.

### Controller layout sketch (portrait, Touchpad mode)

```text
┌──────────────────────────────┐
│ ● Presenting      LAPTOP  ⚙  │  header: state text + icon, PC name, settings
├──────────────────────────────┤
│  [ Touchpad ]  [ Slides ]    │  mode selector
├──────────────────────────────┤
│                              │
│                              │
│        TOUCHPAD SURFACE      │  largest area; faint hex grid
│                              │
│                              │
├──────────────────────────────┤
│  [ Left click ] [ Right ]    │  fallback buttons (≥ 48 dp)
├──────────────────────────────┤
│ Honeycomb Diamond Ripple Off │  effect strip, selected = fill + check
└──────────────────────────────┘
```

Landscape: the touchpad takes the full height; header and effect strip move to a side rail. Left-handed layout mirrors the side rail.

## 5. Touchpad design

### Resting state

A near-black graphite surface. The hexagonal/diamond grid should be very faint at rest—enough to communicate material and depth without competing with controls.

### Active state

- A small localized highlight follows the finger.
- Use slight elevation/shading rather than dramatic perspective.
- No full-screen strobing, flashing, persistent particle trails or strong bloom.
- The animation must never alter pointer coordinates or create a sensation that the input is snapping to a grid.
- Feedback should settle rapidly after finger release (starting target: ≤ 300 ms).
- Reduce animation when the user enables reduced motion or the device is under load.

## 6. The three Windows cursor effects

### Geometry reference (reconciling architecture and design)

| Term | Value | Meaning |
|---|---|---|
| Effect radius | ~60–100 px on a 1080p reference display | Visible extent of the effect from the cursor center (≈ 120–200 px diameter). |
| Render canvas | ~180–240 px square | Off-screen bitmap including transparent margin for fades ([architecture.md §11](architecture.md#11-cursor-effect-renderer)). |

The two numbers describe different things and are consistent: the canvas is slightly larger than the visible effect. Both are prototype starting ranges, tuned against projected visibility; neither is a final invariant. Scale with display DPI and the user's `scale` setting (0.5–2.0).

### Honeycomb Pulse

- Connected hexagonal cells with a few nearest cells gently lifting or shading in response to cursor movement.
- Thin cyan/teal edge highlight, localized to a compact region.
- One brief pulse on click; no continuous large-area glow.

### Diamond Facets

- Small diamond/rhombus lattice with restrained angled shading.
- Minimal depth cues and no aggressive rotation.
- Maintain the same cursor anchor, overall footprint and intensity behavior as Honeycomb Pulse.

### Geometric Ripple

- Two or three thin rings or angular contours around the pointer.
- Expand briefly on click and fade.
- Lowest visual density, suitable for text-heavy or technical slides.

### Shared rules

- Default effect: Honeycomb Pulse at low/subtle intensity (default intensity 0.35).
- Effect changes are immediate and preserve cursor position.
- Offer `Off`, intensity, and reduced-motion settings.
- Avoid obscuring slide text or charts. Test on light, dark and saturated slide content.
- **Dual-tone edge (proposed):** draw cyan edges over a faint dark under-stroke so the effect stays visible on light slides as well as dark ones. Verify in Wave 3 projector tests.
- Reduced motion: no looping or easing; show a static, low-intensity highlight and fade immediately on stop.
- The stable center marker is always drawn at the true cursor position; geometry never shifts it.

### Starting parameters (prototype values — tune in Wave 3)

| Parameter | Honeycomb Pulse | Diamond Facets | Geometric Ripple |
|---|---|---|---|
| Elements | ~7–19 hexagons within the radius | ~9–25 rhombi within the radius | 2–3 rings/contours |
| Motion response | Nearest cells lift/shade with speed | Directional shading with movement | Rings expand slightly with speed |
| Click response | One brief pulse (~250 ms) | One brief facet flash (~250 ms) | Rings expand and fade (~400 ms) |
| Settle after stop | ≤ 300 ms | ≤ 300 ms | ≤ 400 ms |
| Idle cost | Zero redraw once settled | Zero redraw once settled | Zero redraw once settled |

## 7. Android screens

### Home / Connection

- State headline and connected PC name, if available.
- One clear primary action: `Start session` or `End session`.
- A compact instruction for connection from the PowerPoint ribbon.
- Avoid dashboards, statistics, accounts or decorative charts.

### Controller

- Header: connection state, device name and settings.
- A clear Touchpad/Slides mode control.
- Large touchpad in Touchpad mode.
- Large Previous/Next actions in Slides mode.
- Effect picker reachable with one tap.
- Settings secondary, not dominant.

### Pocket Mode

- Explicit activation state and short warning that touchscreen use stops when display is off.
- Clear indication whether screen-off volume control is available on the validated device (**validated / not validated / unsupported**).
- No misleading universal guarantee.

### Permission and error states

- Explain why a permission is needed before requesting it.
- Show one recommended next action per error (catalog: [app_flow.md §4](app_flow.md#4-error-and-recovery-catalog)).
- Never show raw stack traces or generic `Something went wrong` when the recovery action is known.

## 8. Motion and haptics

- Pointer motion is direct and must not be eased by animation code.
- Only the decorative effect may interpolate/settle.
- Transitions between phone screens should be short and understated; avoid parallax during critical controls.
- Optional haptic feedback may confirm discrete actions such as connection success or effect selection, but must be configurable and should not fire for every pointer sample.
- Respect reduced-motion preference and remove non-essential looping animation when idle.

## 9. Accessibility and usability

- Provide semantic labels for all icons and controls.
- Do not use color as the only indicator for selected effect or connection.
- Support system text scaling and contrast checks (token contrast measured in §2).
- Provide a non-animated `Off` effect and a reduced-motion option.
- Make the slide controls and on-screen fallback accessible even if the honeycomb surface is not usable.
- Prevent accidental drag by separating tap and drag states; provide an explicit drag lock if gesture disambiguation is unreliable.
- Screen-reader announcements for state changes (`Connected`, `Presenting`, `Connection lost`).
- The Windows cursor remains visible and usable when the effect is off or when Windows high-contrast mode is active; the effect must never hide the real pointer.

## 10. Design anti-patterns to avoid

- Purple/blue full-screen gradients used as decoration.
- Excessive rounded pill buttons on every control.
- Permanent high-intensity glow across every tile.
- Particle bursts or animations that cover slide content.
- Fake device previews presented as working capabilities.
- Hidden connection state or unclear permission errors.
- Animation-driven mouse lag.
- Controls that change position when the effect changes.
- Visual promises of screen-off volume control on devices that have not passed testing.
- Borders as the only affordance for a control (see measured contrast, §2).

## 11. Design acceptance checks

- First-time user can find Start session, connection state and effect selection without searching.
- A user can switch effects while presenting with one action.
- Every state has text/icon cues as well as color.
- Cursor/effect alignment remains stable when moving between monitors.
- Effect is visible on a projected slide but does not materially obstruct text.
- Reduced-motion and Off modes work.
- Touch targets remain usable on small Android displays and in landscape orientation.
- Text tokens meet the measured contrast table on the real device at outdoor-like brightness.

## 12. Revision history

| Version | Change |
|---|---|
| 1.0 | Initial brief. |
| 1.1 | Added measured token contrast and consequences, Compose token object, layout sketch, geometry reconciliation (radius vs canvas), per-effect starting parameters, dual-tone edge proposal, reduced-motion definition, additional accessibility rules. |
