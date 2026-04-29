# AniStream Stitch UI Schemas

## Stitch Project

- Project: `projects/8604884940665693635`
- Design system asset: `assets/3410471198065136663`
- Theme: Material 3 / Dark / Vibrant
- Primary color seed: `#3B82F6`
- Secondary accent: `#22D3EE`
- Tertiary accent: `#F59E0B`

## Dashboard Screen Schema

- Screen ID: `projects/8604884940665693635/screens/e3b83b9a8c504bb7a30f91a2cf01bd6d`
- Title: `AniStream Home Dashboard`
- Device: `MOBILE`

### Component Contract

1. `HeroCarousel`
   - Featured poster artwork
   - Title, short synopsis, rating pill, CTA
   - Dominant-poster gradient background

2. `ResumeWatchingCard`
   - Episode thumbnail
   - Current progress bar
   - Remaining time label
   - Continue CTA

3. `FilterChipRail`
   - Genre chip
   - Season chip
   - Year chip
   - Status chip

4. `HorizontalEpisodeRail`
   - Latest episode cards
   - Publish time badge
   - Play CTA

5. `RecommendationPosterRail`
   - Poster card
   - Score badge
   - Compact metadata

6. `NewsAndTrailersRail`
   - News card
   - Trailer card with play affordance

7. `BottomNavigation`
   - Home
   - Calendar
   - Downloads
   - Library
   - Settings

## Player / Details Screen Schema

- Screen ID: `projects/8604884940665693635/screens/44241b9180944f7ba02e9ff0d73d8e19`
- Title: `Anime Details & Player`
- Device: `MOBILE`

### Component Contract

1. `CinematicPlayerSurface`
   - Full-width player container
   - Poster-led background wash
   - Collapse-ready header

2. `PrimaryPlaybackActions`
   - Play / pause
   - Skip intro
   - Next episode
   - Download offline
   - PiP
   - Cast

3. `FailoverSourceSelector`
   - Server chips
   - Active server state
   - Retry / recover surface

4. `SubtitleControlsSheet`
   - Font
   - Size
   - Color
   - Shadow

5. `DataSaverQualitySelector`
   - Auto
   - 360p
   - 480p
   - Higher quality options if available

6. `EpisodeQueueRail`
   - Previous / next episode cards
   - Quick jump CTA

7. `GestureHintOverlay`
   - Brightness hint
   - Volume hint
   - Double-tap seek hint

## Design Notes

- All surfaces are modular and poster-color-aware.
- Home and player are designed for RTL-friendly spacing and Arabic typography.
- Reusable units map cleanly to Compose: hero, rails, chips, media controls, overlays, and sheets.
