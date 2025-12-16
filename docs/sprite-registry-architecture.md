# SpriteRegistry Architecture Guide

## Overview

`SpriteRegistry` is the **unified texture & sprite management system** for Cola Dungeon. It:

1. Manages all sprite sheets and texture atlases across the project
2. Provides overlay-based material substitution (texture pack support)
3. Uses an abstract `Segment` class hierarchy for different texture types
4. Ensures all textures pass through a single, centralized resolution point

---

## Texture Usage Map

### 1. **Environment/Tilemap Textures**
Used for dungeon level rendering, water effects, and terrain features.

| Type | Files | Used In |
|------|-------|---------|
| **Base Tiles** | `tiles_sewers.png`, `tiles_prison.png`, `tiles_caves.png`, `tiles_city.png`, `tiles_halls.png` | `DungeonTilemap`, `Level` classes |
| **Water** | `water0.png` - `water4.png` (5 variants for different regions) | Water rendering in dungeons |
| **Terrain Features** | `terrain_features.png` | Trees, rocks, obstacles |
| **Custom Tiles** | `weak_floor.png`, `sewer_boss.png`, `prison_quest.png`, etc. | Boss levels, quest areas |
| **Grid & Debug** | `visual_grid.png`, `wall_blocking.png` | Debug visualization |

**Material IDs** (for overlay mapping):
- `"environment.tiles.sewers"` → `tiles_sewers.png`
- `"environment.tiles.prison"` → `tiles_prison.png`
- `"environment.water.sewers"` → `water0.png`

---

### 2. **Sprite Textures (Characters & Mobs)**
Used for rendering hero characters, enemies, NPCs, and interactive objects.

#### Heroes (6 base classes)
```
sprites/warrior.png      → Warrior class sprite
sprites/mage.png         → Mage class sprite
sprites/rogue.png        → Rogue class sprite
sprites/huntress.png     → Huntress class sprite
sprites/duelist.png      → Duelist class sprite
sprites/cleric.png       → Cleric class sprite
sprites/avatars.png      → Character portraits/avatars
```

**Material IDs**:
- `"sprites.hero.warrior"` → `sprites/warrior.png`
- `"sprites.hero.mage"` → `sprites/mage.png`
- etc.

#### Enemies & Mobs (30+ types)
```
rat.png, brute.png, spinner.png, dm300.png, wraith.png, skeleton.png, thief.png, 
tengu.png, bat.png, elemental.png, monk.png, warlock.png, golem.png, succubus.png, 
crab.png, bee.png, crystal_wisp.png, ghost.png, etc.
```

Used via `SpriteRegistry.registerMobAtlas(key, texturePath)` and `SpriteRegistry.getMobSprite()`.

#### Special Sprites
```
pet.png           → Pet/companion sprite
amulet.png        → Amulet of Yendor
```

---

### 3. **Item Textures**
Used for inventory items, equipment, consumables, and pickup loot.

| Texture | Size | Used In |
|---------|------|---------|
| `sprites/items.png` | 16×16 grid | Main item sheet (static, ItemSpriteSheet) |
| `sprites/item_icons.png` | 32×32 | Item icons in UI |
| Custom item sheets | Dynamic | Modded/DLC items via `SpriteRegistry.registerItemTexture()` |

**Overlay support** via `SpriteRegistry.useOverlay()` and `SpriteRegistry.overlayMap()`.

---

### 4. **UI/Interface Textures**
Used for all user interface elements, menus, status displays, and HUD elements.

| Texture | Purpose | Used In |
|---------|---------|---------|
| `chrome.png` | Window frames, borders | All `Wnd*` classes, `Chrome` class |
| `icons.png` | Generic UI icons | Various UI components |
| `status_pane.png` | Hero status bar | `StatusPane` |
| `menu_pane.png` | Menu backgrounds | `MenuPane` |
| `menu_button.png` | Button styling | `StyledButton` |
| `toolbar.png` | Bottom action bar | `Toolbar` |
| `shadow.png` | Drop shadows | UI components |
| `boss_hp.png` | Boss health indicator | Boss battles |
| `surface.png` | Surface/terrain effects | Visual effects |
| `radial_menu.png` | Radial action menu | `RadialMenu` |

**Material IDs**:
- `"ui.chrome"` → `chrome.png`
- `"ui.icons"` → `icons.png`
- `"ui.buffs.small"` → `buffs.png`
- `"ui.buffs.large"` → `large_buffs.png`
- `"ui.toolbar"` → `toolbar.png`

---

### 5. **Buff & Icon Textures**
Used for status effect icons, ability icons, and UI indicators.

| Texture | Size | Purpose |
|---------|------|---------|
| `buffs.png` | 16×16 | Small buff icons |
| `large_buffs.png` | 32×32 | Large buff icons (when displayed prominently) |
| `talent_icons.png` | Variable | Talent/ability icons |
| `talent_button.png` | Variable | Talent selection buttons |
| `hero_icons.png` | Variable | Hero class icons |
| `badges.png` | Variable | Achievement badges |

**Material IDs**:
- `"ui.buffs.small"` → `buffs.png`
- `"ui.buffs.large"` → `large_buffs.png`

Used via `SpriteRegistry.resolveBuffTexture(large)` and `SpriteRegistry.resolveUiIconsTexture()`.

---

### 6. **Effect Textures**
Used for visual effects, particles, spells, and impact effects.

| Texture | Purpose |
|---------|---------|
| `effects/effects.png` | General game effects |
| `effects/fireball.png` | Fire spell effects |
| `effects/specks.png` | Particle effects, sparkles |
| `effects/spell_icons.png` | Spell ability icons |
| `effects/text_icons.png` | Floating text indicators |

---

### 7. **Font Textures**
Used for text rendering, especially for retro/pixel fonts.

```
fonts/pixel_font.png  → Pixelated font for game text
```

---

### 8. **Splash Screens**
Used for character select, level transitions, and cinematic screens.

| Type | Files |
|------|-------|
| **Hero Splashes** | `warrior.jpg`, `mage.jpg`, `rogue.jpg`, `huntress.jpg`, `duelist.jpg`, `cleric.jpg` |
| **Level Splashes** | `sewers.jpg`, `prison.jpg`, `caves.jpg`, `city.jpg`, `halls.jpg` |

Used in `TitleScene`, `HeroSelectScene`, `AmuletScene`, `InterlevelScene`.

---

## Architecture: Segment + Overlay System

### Segment Hierarchy

The `Segment` base class provides:
- Lazy texture loading via `ensureLoaded()`
- Material ID for overlay resolution (via `.as(materialId)`)
- Atlas management for frame/sprite layout
- Automatic reload when overlay stack changes

```
Segment<S extends Segment<S>>  (abstract base)
  ├── ItemSegment              (item grid with labels)
  ├── MobSegment               (mob sprites, named regions, animations)
  ├── IconSegment              (UI icons, grid-based)
  ├── UiSegment                (UI chrome, grid-based)
  └── TileSegment              (tile sheets, grid-based)
```

### How Segments Work

1. **Registration**: Each segment is created with a base texture handle
   ```java
   ItemSegment seg = SpriteRegistry.registerItemTexture("sprites/items.png", 16);
   seg.as("items.main");  // Set material ID for overlay resolution
   seg.label("sword").label("shield");  // Add named frames
   ```

2. **Material Resolution**: When fetching a sprite, overlays are checked first
   ```java
   // Inside Segment.ensureLoaded():
   Object handle = SpriteRegistry.resolveMaterial(materialId, baseTextureHandle);
   // If overlay "my_pack" has "items.main" → uses that instead of baseTextureHandle
   ```

3. **Overlay Stack** (bottom → top): Higher index = higher priority
   ```
   Stack: ["default_pack", "my_pack", "custom_override"]
                                       ↑ checked first
   ```

---

### Overlay API

#### Push/Prioritize an Overlay
```java
SpriteRegistry.useOverlay("texture_pack_name");
// Adds overlay to top of stack, or moves it to top if already present
```

#### Map Material → Texture in Overlay
```java
SpriteRegistry.overlayMap("my_pack", "items.main", "mods/items_custom.png");
SpriteRegistry.overlayMap("my_pack", "ui.chrome", "mods/ui_custom.png");
```

#### Resolve Material (used internally by Segments)
```java
Object texture = SpriteRegistry.resolveMaterial("items.main", fallbackHandle);
// Returns first overlay mapping, or fallback if not found
```

#### Debug: View Overlay Stack
```java
List<String> stack = SpriteRegistry.overlays();
for (String key : stack) {
    System.out.println(key);  // Print overlay keys in priority order
}
```

---

### ImageMapping: Bridge to noosa.Image

`ImageMapping` is a simple data structure that holds the resolved texture + frame info:

```java
public static class ImageMapping {
    public SmartTexture texture;    // The actual texture loaded
    public RectF rect;              // UV frame within texture
    public float height;            // Pixel height (for scaling)
    public int size;                // Frame size (16, 32, 64, etc.)
}
```

**Usage in Rendering**:
```java
ImageMapping map = SpriteRegistry.mapItemImage(imageId);
if (map != null) {
    // ItemSprite example:
    texture = map.texture;
    frame(map.rect);
    scale.set(16f / map.size);  // Scale if size != 16
}
```

This design allows `noosa.Image` (and subclasses) to work seamlessly:
- `texture` → passed to `image.texture()`
- `rect` → passed to `image.frame()`
- `height` / `size` → used for scaling/perspective

---

## Complete Usage Example

### Step 1: Register a Custom Item Sheet
```java
// In static initializer or TexturePackManager:
ItemSegment customItems = SpriteRegistry.registerItemTexture("mods/items_cola.png", 16)
    .as("items.cola")                    // Set material ID
    .label("cola_potion")
    .label("cola_bomb");
```

### Step 2: Define an Overlay
```java
// Create a texture pack overlay:
SpriteRegistry.useOverlay("summer_pack");

// Map materials to custom textures:
SpriteRegistry.overlayMap("summer_pack", "items.main", "packs/summer/items.png");
SpriteRegistry.overlayMap("summer_pack", "ui.chrome", "packs/summer/chrome.png");
SpriteRegistry.overlayMap("summer_pack", "sprites.hero.warrior", "packs/summer/warrior.png");
```

### Step 3: Fetch & Render
```java
// Item rendering (ItemSprite):
ImageMapping map = SpriteRegistry.getItemImageMapping("cola_potion");
if (map != null) {
    sprite.texture = map.texture;
    sprite.frame(map.rect);
}

// Mob rendering (MobSprite):
ImageMapping mobMap = SpriteRegistry.getMobSprite("orc_warrior", "attack", Assets.Sprites.BRUTE);
if (mobMap != null) {
    sprite.texture = mobMap.texture;
    sprite.frame(mobMap.rect);
}

// UI rendering (Chrome):
Object chromeHandle = SpriteRegistry.resolveUiIconsTexture();
SmartTexture chromeTexture = TextureCache.get(chromeHandle);
```

---

## Material ID Naming Convention

Use dot-separated hierarchical names:

```
<category>.<subcategory>.<specific>

Examples:
  items.main              → Main item sheet
  items.cola              → Cola Dungeon custom items
  ui.chrome               → UI window frames
  ui.buffs.small          → Small buff icons
  ui.buffs.large          → Large buff icons
  ui.icons                → Generic UI icons
  sprites.hero.warrior    → Warrior hero sprite
  sprites.mob.orc         → Orc mob sprite
  environment.tiles.sewers → Sewer tiles
  environment.water.sewers → Sewer water
```

---

## Future: TexturePackManager Integration

A future `TexturePackManager` should:

1. Load JSON texture pack definitions
2. Extract overlay key and material mappings
3. Call `SpriteRegistry.useOverlay(overlayKey)`
4. Call `SpriteRegistry.overlayMap()` for each mapping
5. Optionally reload/reload all Segments to pick up new textures

Example JSON structure:
```json
{
  "pack_name": "summer_pack",
  "author": "Cola Team",
  "version": "1.0",
  "materials": {
    "items.main": "textures/items.png",
    "ui.chrome": "textures/chrome.png",
    "sprites.hero.warrior": "textures/warrior.png"
  }
}
```

---

## Summary

| Component | Purpose |
|-----------|---------|
| **Segment** | Owns an Atlas, manages texture loading + overlay resolution |
| **Overlay Stack** | Prioritizes texture pack materials over defaults |
| **Material ID** | Stable identifier for texture resolution (via `.as()`) |
| **ImageMapping** | Result struct (texture + frame) passed to noosa.Image |
| **SpriteRegistry** | Singleton coordinator of all segments + overlays |

This design enables:
- ✅ Centralized texture management
- ✅ Texture pack support (overlays)
- ✅ Lazy loading + automatic reload
- ✅ Full noosa.Image compatibility
- ✅ Easy to extend with new segment types

