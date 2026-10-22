---
name: "apple-design"
description: >-
  Cross-platform UI/UX design reviewer and craft guide grounded in Apple's Human Interface Guidelines (HIG).
  Focuses on 8 core design principles (Purpose, Agency, Responsibility, Familiarity, Flexibility, Simplicity,
  Craft, Delight) and 5 audit lenses (Accessibility, Platform Conventions, Visual Design & Craft, Interaction, Writing).
---

# Apple Design Skill

## Core Principles (HIG 2026)
1. **Purpose**: Every screen and component has a singular, clear intention. Avoid visual noise.
2. **Agency**: The user always feels in control. All state transitions, edits, additions, and deletions are immediate, visible, and reversible.
3. **Responsibility**: Absolute privacy, local-first integrity, explicit confirmation for destructive actions.
4. **Familiarity**: Intuitive hierarchy, standard navigation, recognizable iconography, consistent mental models.
5. **Flexibility**: Fluid layouts, comfortable spacing on compact and expanded viewports.
6. **Simplicity**: Progressive disclosure—reveal complexity only when the user requests it. Clean typography over heavy decoration.
7. **Craft**: Pixel-perfect alignment, subtle translucent layered surfaces (glassmorphism/materials), 8pt rhythm, exquisite typography contrast.
8. **Delight**: Responsive spring physics, micro-celebrations upon completion, fluid progress animations, satisfying tactile feedback.

## The 5 Audit Lenses
1. **Accessibility**:
   - Touch targets strictly ≥ 48dp (`minimumInteractiveComponentSize`).
   - High-contrast text ratios (≥ 4.5:1 for body text, ≥ 3.0:1 for large display headers).
   - Meaningful `contentDescription` on all icons and interactive controls.
2. **Platform Conventions**:
   - Edge-to-edge layout with full support for safe area insets (`WindowInsets.safeDrawing`).
   - Standard back navigation handling.
   - Distinctive active vs inactive states.
3. **Visual Design & Craft**:
   - Strict 8dp grid spacing (8.dp, 16.dp, 24.dp).
   - Refined typography hierarchy: Large Title (bold display), Headline, Body, Caption.
   - Layered surface depth: Frosted surface containers, subtle 1dp border strokes (`Color.White.copy(alpha = 0.08f)`), soft elevation shadows.
4. **Interaction**:
   - Direct manipulation: 1-tap milestone completion toggles with fluid spring animation.
   - Ripple and press-scale feedback on touch (`animateFloatAsState`).
   - Destructive action guardrails (swipe or confirmation dialog).
5. **Writing & Voice**:
   - Clear, concise, confident, human copy.
   - Empathetic empty states that guide the user on what to do next.
