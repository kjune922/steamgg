# DESIGN.md

## 1. Visual Theme & Atmosphere

This product should feel like a game discovery platform with strong visual browsing.
Use a Pinterest-inspired image-first layout, but adapted for comparison-heavy search.
The mood is dark, clean, modern, and slightly premium.
Users should feel that browsing games is fast, visual, and low-friction.

Keywords:
- image-first
- dark surfaces
- structured grid
- strong visual hierarchy
- fast scanning
- clean interaction
- minimal clutter

Do not make it look like a social feed.
Do not use chaotic masonry layout for the main search result list.
Use a consistent card grid so users can compare games quickly.

---

## 2. Color Palette & Roles

### Core Colors
- Background / App Shell: #0F1115
- Surface / Card: #181C22
- Surface Hover: #20252D
- Border / Divider: #2A313C
- Primary Text: #F5F7FA
- Secondary Text: #B6BEC9
- Muted Text: #8A93A0

### Accent Colors
- Primary Accent: #E60023
- Accent Hover: #C4001E
- Accent Soft Background: #2A1116

### Semantic Colors
- Success: #22C55E
- Warning: #F59E0B
- Danger: #EF4444
- Info: #3B82F6

### Usage Rules
- Use the red accent sparingly for search actions, active filters, important CTA, and selected states.
- Do not flood the whole UI with red.
- Cards and layout should remain dark and neutral so game thumbnails stand out.
- Let game imagery be the loudest visual element.

---

## 3. Typography Rules

### Font Style
Use a modern sans-serif style.
The UI should feel crisp, readable, and slightly dense.

### Type Scale
- Page Title: 32px, 700
- Section Title: 24px, 700
- Card Title: 18px, 600
- Body: 14px to 16px, 400
- Meta Labels: 12px to 13px, 500
- Button Text: 14px, 600

### Typography Principles
- Titles should be strong and concise.
- Metadata should be compact and easy to scan.
- Avoid long decorative headings.
- Keep paragraphs short.
- DETAIL page descriptions should be readable but not overly editorial.

---

## 4. Layout Principles

### Global Layout
- Use a centered content container.
- Max content width should feel wide enough for media-heavy browsing.
- Keep generous horizontal padding on desktop.
- Keep tighter but still breathable spacing on mobile.

### Grid Rules
- ROOT popular section: fixed card grid
- LIST result section: fixed grid, equal card heights
- DETAIL similar games section: horizontal rail or fixed grid

### Spacing
- Tight internal card spacing
- Clear separation between sections
- Bigger spacing between page blocks than inside components

### Important Rule
Do not use masonry layout on LIST.
LIST must optimize comparison, not endless inspiration browsing.

---

## 5. Component Styling

## 5.1 Navigation
- Dark top bar
- Logo on the left
- Login button on the right
- Minimal chrome
- Search should visually dominate the hero area, not the nav

## 5.2 Search Bar
- Large, obvious, centered on ROOT
- Rounded corners
- High contrast input
- Strong focus ring using soft red accent
- Search button should be clearly visible

## 5.3 Game Cards
- Image-first card design
- Thumbnail or capsule image at top
- Title directly under image
- Small metadata row below title
- Show only the most useful information:
  - genre
  - player range
  - current players or popularity
- Hover should slightly lift or brighten card surface
- Keep height consistent in LIST

## 5.4 Filter UI
- Compact and structured
- Chips, dropdowns, or grouped controls are allowed
- Active filter state should be clearly highlighted
- Filters must feel practical, not decorative

## 5.5 Buttons
- Primary button uses red accent
- Secondary button uses dark surface with border
- Hover states should be visible but restrained

## 5.6 Detail Header
- Large game header image
- Strong title
- Clear purchase CTA
- Metadata should be grouped and easy to scan

## 5.7 Similar Games Section
- Visually similar to LIST cards
- Smaller section title
- Cards should feel like continuation of the browsing journey

---

## 6. Page-Specific Guidance

## 6.1 ROOT
Purpose:
- Start search fast
- Expose popular games immediately

Structure:
- Hero search area
- Popular games section
- Minimal top navigation

Rules:
- Search bar must be the strongest element
- Popular games should appear as a polished visual gallery
- Keep this page simple and decisive

## 6.2 LIST
Purpose:
- Let users narrow results and compare quickly

Structure:
- Search input at top
- Filters above grid or in a side panel
- Sort control near result header
- Fixed grid of cards

Rules:
- Comparison is more important than visual novelty
- Card heights should be consistent
- Metadata should be scannable in 2 seconds

## 6.3 DETAIL
Purpose:
- Help users judge one game and continue to related games

Structure:
- Header image
- Title and short description
- Purchase link
- Game metadata
- Similar games section

Rules:
- Purchase CTA should be easy to find
- Metadata should be grouped, not scattered
- Similar games should encourage the next click

---

## 7. Depth & Elevation

- Use subtle shadows only
- Prefer contrast and border over heavy elevation
- Cards can lift slightly on hover
- Avoid glassmorphism
- Avoid bright glowing effects except very subtle accent focus states

---

## 8. Do's and Don'ts

## Do
- Make game images the visual anchor
- Keep card grids clean and consistent
- Use red accent for action and selection
- Prioritize fast scanning
- Preserve strong contrast and readability

## Don't
- Do not use a cluttered social-feed layout
- Do not use masonry grid for LIST
- Do not overload cards with too much text
- Do not make filters feel hidden or secondary
- Do not use too many accent colors

---

## 9. Responsive Behavior

### Mobile
- Single-column or two-column card layout depending on width
- Filters may collapse into a sheet or horizontal scroll chip row
- Search remains prominent
- Detail metadata should stack clearly

### Tablet
- Two to three card columns
- Filters can remain visible if space allows

### Desktop
- Wide grid
- Search and filters should feel efficient
- Detail page can use a two-column hero area if needed

### Minimum Checks
- 390px mobile
- 768px tablet
- 1280px desktop

---

## 10. Agent Prompt Guide

When building UI for this project:
- Follow this DESIGN.md before creating new components
- Prefer image-first browsing patterns
- Use dark neutral surfaces and restrained red accents
- ROOT should feel decisive and visual
- LIST should feel structured and comparison-friendly
- DETAIL should feel informative and continuation-oriented
- Never use masonry layout for the main LIST page
- Reuse card patterns across ROOT, LIST, and similar-games sections