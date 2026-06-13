---
name: Emerald Estate
colors:
  surface: '#031427'
  surface-dim: '#031427'
  surface-bright: '#2a3a4f'
  surface-container-lowest: '#000f21'
  surface-container-low: '#0b1c30'
  surface-container: '#102034'
  surface-container-high: '#1b2b3f'
  surface-container-highest: '#26364a'
  on-surface: '#d3e4fe'
  on-surface-variant: '#bccac0'
  inverse-surface: '#d3e4fe'
  inverse-on-surface: '#213145'
  outline: '#87948b'
  outline-variant: '#3d4a42'
  surface-tint: '#68dba9'
  primary: '#68dba9'
  on-primary: '#003825'
  primary-container: '#25a475'
  on-primary-container: '#00311f'
  inverse-primary: '#006c4a'
  secondary: '#bec6e0'
  on-secondary: '#283044'
  secondary-container: '#3f465c'
  on-secondary-container: '#adb4ce'
  tertiary: '#c4c7c9'
  on-tertiary: '#2d3133'
  tertiary-container: '#8e9193'
  on-tertiary-container: '#272a2c'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#85f8c4'
  primary-fixed-dim: '#68dba9'
  on-primary-fixed: '#002114'
  on-primary-fixed-variant: '#005137'
  secondary-fixed: '#dae2fd'
  secondary-fixed-dim: '#bec6e0'
  on-secondary-fixed: '#131b2e'
  on-secondary-fixed-variant: '#3f465c'
  tertiary-fixed: '#e0e3e5'
  tertiary-fixed-dim: '#c4c7c9'
  on-tertiary-fixed: '#191c1e'
  on-tertiary-fixed-variant: '#444749'
  background: '#031427'
  on-background: '#d3e4fe'
  surface-variant: '#26364a'
typography:
  headline-xl:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  container-max: 1440px
  gutter: 24px
  sidebar-width: 260px
  margin-mobile: 16px
  margin-desktop: 32px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 24px
---

## Brand & Style

This design system transitions the product from a playful, pastel-heavy interface to a sophisticated, enterprise-grade SaaS platform. The brand personality is professional, reliable, and efficient, targeting property owners and managers who require clarity over decoration.

The visual direction follows a **Corporate / Modern** aesthetic with a strong emphasis on **Minimalism**. With the shift to a **Dark Mode** default, the UI focuses on reducing eye strain while maintaining high data density and legibility. The mood is calm and structured, using precision-engineered components and deep tonal layers to evoke a sense of trust and institutional stability.

## Colors

The palette is anchored by **Emerald 600** (#059669) as the primary brand color, providing a refined and modern evolution of the original green theme. This is balanced by **Slate 900** (#0f172a) and deep neutrals to create a sophisticated dark interface.

- **Primary**: Emerald 600 is used for key actions, active states, and brand identifiers, cutting through the dark background with high energy.
- **Secondary**: Slate 900 forms the core of the dark interface, used for secondary surfaces and navigation backgrounds.
- **Neutral**: A range of cool grays (Slate/Gray) used for secondary text, borders, and UI scaffolding.
- **Backgrounds**: The system now utilizes a dark foundation (#0F172A), where the main application background is a deep slate to maximize focus on data and charts.
- **Semantic**: Clear red, amber, and green for status-driven indicators (Occupied, Delinquent, Available), tuned for dark mode visibility.

## Typography

This design system utilizes **Inter** exclusively to ensure a clean, functional, and highly readable interface across all screen sizes. The type scale is strictly hierarchical to guide the user's eye through complex property data, with text colors carefully stepped for dark mode accessibility.

- **Headlines**: Use Semi-Bold to Bold weights with slight negative letter-spacing for a modern, "tight" look.
- **Body**: Standardized at 16px for optimal legibility in management dashboards.
- **Labels**: Used for status chips and small metadata, often employing a slight tracking increase and uppercase transform to distinguish them from body copy.

## Layout & Spacing

The layout philosophy follows a **Fixed Grid** approach for desktop management, centering content within a 1440px max-width container to prevent line lengths from becoming unreadable on ultra-wide monitors.

- **Sidebar**: A fixed left-hand navigation at 260px provides consistent access to core modules.
- **Grid**: A 12-column system for desktop, collapsing to 1 column for mobile. 
- **Margins**: Generous 32px external margins create a spacious feel, which is especially important in dark mode to prevent the UI from feeling claustrophobic.
- **Rhythm**: All spacing (padding, margins, gaps) is built on an 8px base unit to ensure mathematical harmony across the interface.

## Elevation & Depth

Visual hierarchy in dark mode is established through **Tonal Layers** and subtle glows rather than heavy shadows. 

1. **Level 0 (Base)**: Main application background (#0F172A).
2. **Level 1 (Surface)**: Cards and containers use a slightly lighter slate (#1E293B) to appear "closer" to the user, with a subtle 1px border (#334155) for definition.
3. **Level 2 (Raised)**: Active cards or interactive elements during hover, utilizing a subtle brand-tinted outer glow or a lifted tonal background.
4. **Level 3 (Overlay)**: Modals and dropdowns, featuring a distinct tonal lift and a 20% backdrop blur (glassmorphism) on the underlying mask to maintain context.

## Shapes

The shape language is defined as **Rounded**, utilizing a consistent 8px (0.5rem) base for standard components.

- **Standard Cards/Inputs**: 8px corner radius.
- **Large Containers**: 16px corner radius for major dashboard widgets.
- **Status Chips**: Fully rounded (pill-shaped) to distinguish them from interactive buttons.
- **Buttons**: 8px corner radius, maintaining a soft but professional geometry.

## Components

### Buttons
- **Primary**: Solid Emerald 600 with white text. High-contrast, 8px radius.
- **Secondary**: Ghost style with a Slate 700 border and light slate text.
- **Destructive**: Solid Red, reserved for "Supprimer" or "Déconnexion".

### Cards
Cards are the primary data container. In dark mode, they use a slightly lightened slate background relative to the base, a 1px Slate 700 border, and consistent 24px padding.

### Input Fields
Inputs use a dark background with a 1px gray/slate border. On focus, the border transitions to Emerald 600 with a subtle brand-tinted outer glow.

### Navigation (Sidebar)
The sidebar acts as the anchor of the dark theme (Slate 900). Active links are highlighted with an Emerald 600 left-border accent and a subtle background highlight to indicate focus.

### Status Chips
Status indicators (Available, Occupied, etc.) use a low-saturation background of the semantic color with high-saturation text of the same hue to ensure readability against the dark UI background without being overwhelming.