package com.zootdungeon.sprites;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.zootdungeon.Assets;
import com.zootdungeon.actors.hero.HeroClass;
import com.zootdungeon.tiles.DungeonTileSheet;
import com.zootdungeon.utils.EventBus;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.gltextures.Atlas;
import com.watabou.utils.RectF;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Unified sprite registry:
 * - Static (compile-time) items by ID via ItemSpriteSheet
 * - Dynamic mobs by key with explicit fallback
 *
 * Rules:
 * - Any dynamic fetch MUST provide a fallback static id (items) or texture (mobs)
 * - If key not found, fallback is used
 */
public final class SpriteRegistry {

    private SpriteRegistry() {}

    // ---------------------------
    // Overlay stack (material priority)
    // ---------------------------

    /**
     * Overlays are keyed packs of material-id -> texture-handle mappings.
     * When resolving a material, the last overlay in the stack has highest priority.
     *
     * Overlay only manages *file/texture priority*, not layout/frames.
     */
    private static final ArrayList<String> overlayStack = new ArrayList<>();
    private static final Map<String, Map<String, Object>> overlays = new HashMap<>();

    /**
     * Adds an overlay to the stack, or moves it to the top (highest priority).
     */
    public static void useOverlay(String overlayKey) {
        if (overlayKey == null) return;
        overlayStack.remove(overlayKey);
        overlayStack.add(overlayKey);
    }

    /**
     * Returns the current overlay stack (bottom -> top). Intended for debugging.
     */
    public static List<String> overlays() {
        return overlayStack;
    }

    /**
     * Sets a mapping inside an overlay: materialId -> textureHandle (usually a String path).
     * This is the core API that a future TexturePackManager/JSON loader should call into.
     */
    public static void overlayMap(String overlayKey, String materialId, Object textureHandle) {
        if (overlayKey == null || materialId == null || textureHandle == null) return;
        overlays.computeIfAbsent(overlayKey, k -> new HashMap<>()).put(materialId, textureHandle);
    }

    /**
     * Resolves a material id to the highest-priority texture handle from overlays, or returns fallback.
     */
    public static Object resolveMaterial(String materialId, Object fallbackTextureHandle) {
        if (materialId != null) {
            for (int i = overlayStack.size() - 1; i >= 0; i--) {
                Map<String, Object> map = overlays.get(overlayStack.get(i));
                if (map == null) continue;
                Object handle = map.get(materialId);
                if (handle != null) return handle;
            }
        }
        return fallbackTextureHandle;
    }

    // ---------------------------
    // Segment base (Atlas-backed)
    // ---------------------------

    /**
     * Segment is a unit of sprite sheet management. Every segment owns an Atlas
     * and lazily loads its SmartTexture via TextureCache, with overlay-based
     * material resolution.
     *
     * Subclasses define frame/layout behavior (grid, named regions, etc).
     */
    public abstract static class Segment<S extends Segment<S>> {
        protected final Object baseTextureHandle;
        protected Object resolvedTextureHandle;
        protected String materialId;

        protected SmartTexture cache;
        protected Atlas atlas;

        protected Segment(Object baseTextureHandle) {
            this.baseTextureHandle = baseTextureHandle;
            // default material id is the base handle string form; callers can override via as()
            this.materialId = baseTextureHandle == null ? null : String.valueOf(baseTextureHandle);
        }

        protected abstract S self();

        /**
         * Assigns a stable material id for overlay resolution.
         * Example: registerItemTexture("sprites/items_mod.png").as("items.main")
         */
        public S as(String materialId) {
            this.materialId = materialId;
            return self();
        }

        /**
         * Ensures the underlying texture is loaded and up-to-date with the current overlay stack.
         */
        protected final void ensureLoaded() {
            Object handle = SpriteRegistry.resolveMaterial(materialId, baseTextureHandle);
            if (cache == null || cache.bitmap == null || !Objects.equals(resolvedTextureHandle, handle)) {
                cache = TextureCache.get(handle);
                atlas = new Atlas(cache);
                resolvedTextureHandle = handle;
                afterLoad();
            }
        }

        /**
         * Hook for subclasses to apply atlas grid/layout after texture load/reload.
         */
        protected void afterLoad() {}
    }

    // ---------------------------
    // Item dynamic atlas
    // ---------------------------

    // dynamic item segments and ids
    private static final ArrayList<ItemSegment> itemSegments = new ArrayList<>();
    public static final HashMap<String, Integer> ITEM_TEXTURE_ID_MAP = new HashMap<>();
    private static int latestItemLocation = 120000;

    public static ItemSegment getItemSegment(int id) {
        for (ItemSegment s : itemSegments) {
            if (s.id_start <= id && id <= s.id_start + s.id_size) {
                s.ensureLoaded();
                return s;
            }
        }
        return null;
    }

    public static int itemByName(String name) {
        Integer res = ITEM_TEXTURE_ID_MAP.get(name);
        if (res == null) {
            System.out.println("Invalid texture name: " + name);
            return ItemSpriteSheet.SOMETHING;
        }
        return res;
    }

    public static ItemSegment registerItemTexture(String texture, int size) {
        ItemSegment s = new ItemSegment(texture, latestItemLocation, size);
        itemSegments.add(s);
        latestItemLocation += 1000;
        return s;
    }

    public static ItemSegment registerItemTexture(String texture) {
        return registerItemTexture(texture, 16);
    }

    public static ImageMapping getItemImageMapping(int id) {
        ItemSegment s = getItemSegment(id);
        if (s == null) {
            return null;
        }
        return s.get(id);
    }

    public static ImageMapping getItemImageMapping(String label) {
        Integer id = ITEM_TEXTURE_ID_MAP.get(label);
        if (id == null) return null;
        ItemSegment s = getItemSegment(id);
        return s != null ? s.get(label) : null;
    }

    public static class ItemSegment extends Segment<ItemSegment> {
        int id_start;
        int id_size;
        int size;
        int cols;

        // localIndex -> optional label for rebuilding named frames after reload
        private final ArrayList<String> labels = new ArrayList<>();

        public ItemSegment(String texture, int id_start, int size) {
            super(texture);
            this.id_start = id_start;
            this.id_size = 0;
            this.size = size;
            ensureLoaded();
        }

        @Override
        protected ItemSegment self() {
            return this;
        }

        @Override
        protected void afterLoad() {
            // Setup grid
            this.atlas.grid(this.size, this.size);
            this.cols = (int) (cache.width / size);

            // Rebuild label mappings (overlay switch may recreate the Atlas)
            for (int i = 0; i < labels.size(); i++) {
                String label = labels.get(i);
                if (label == null) continue;
                int x = i % cols;
                int y = i / cols;
                atlas.add(label, x * size, y * size, (x + 1) * size, (y + 1) * size);
            }
        }

        private ItemSegment settle(int id) {
            ensureLoaded();
            int x = id % cols;
            int y = id / cols;
            // Add named frame to Atlas
            atlas.add(id, x * size, y * size, (x + 1) * size, (y + 1) * size);
            return this;
        }

        public ImageMapping get(int id) {
            ensureLoaded();
            int where = id >= id_start ? id - id_start : id;
            
            // Get from Atlas
            RectF rect = atlas.get(where);
            if (rect == null) {
                settle(where);
                rect = atlas.get(where);
            }
            
            return new ImageMapping(cache, rect, atlas.height(rect), size);
        }

        public ItemSegment label(String label) {
            ensureLoaded();
            ITEM_TEXTURE_ID_MAP.put(label, id_start + id_size);
            settle(id_size);
            // Also add the label directly to Atlas for named access
            int x = id_size % cols;
            int y = id_size / cols;
            atlas.add(label, x * size, y * size, (x + 1) * size, (y + 1) * size);

            // persist label mapping so it survives overlay-driven reload
            while (labels.size() <= id_size) labels.add(null);
            labels.set(id_size, label);

            id_size++;
            return this;
        }

        /**
         * Reserve slots in current segment.
         * Use with care; intended for development utilities.
         */
        public ItemSegment span(int size) {
            // keep label list aligned with reserved indices
            for (int i = 0; i < size; i++) labels.add(null);
            this.id_size += size;
            return this;
        }

        public ImageMapping get(String label) {
            ensureLoaded();
            // Direct Atlas lookup by name
            RectF rect = atlas.get(label);
            if (rect != null) {
                return new ImageMapping(cache, rect, atlas.height(rect), size);
            }
            
            // Fallback to ID-based lookup
            Integer id = ITEM_TEXTURE_ID_MAP.get(label);
            return id == null ? null : get(id);
        }

        public static int getByName(String name) {
            return SpriteRegistry.itemByName(name);
        }
    }

    /**
     * Dynamic image mapping container.
     */
    public static class ImageMapping {

        public SmartTexture texture;
        public RectF rect;
        public float height;
        public int size;

        public ImageMapping(SmartTexture texture, RectF rect, float height) {
            this.rect = rect;
            this.height = height;
            this.texture = texture;
            this.size = 16;
        }

        public ImageMapping(SmartTexture texture, RectF rect, float height, int size) {
            this.rect = rect;
            this.height = height;
            this.texture = texture;
            this.size = size;
        }
    }

    /**
     * Map an integer image id to a dynamic ImageMapping if it's in dynamic range.
     * Falls back to special hardcoded examples for some ids (legacy behavior).
     */
    public static ImageMapping mapItemImage(int image) {
        if (image < 114514) {
            return null;
        } else {
            ItemSegment seg = getItemSegment(image);
            if (seg != null) {
                return seg.get(image);
            } else {
                System.out.println("Invalid image segment for " + image);
            }
            if (image == 114515) {
                return new ImageMapping(TextureCache.get("minecraft/bread.png"), new RectF(0f, 0, 1f, 1), 16);
            } else if (image == 114516) {
                int size = 16;
                TextureBuilder builder = new TextureBuilder(size, size);

                java.util.Random rand = new java.util.Random();

                int r = 220 + rand.nextInt(35);
                int g = 180 + rand.nextInt(40);
                int b = 10 + rand.nextInt(50);
                int goldColor = 0xFF000000 | (r << 16) | (g << 8) | b;

                r = Math.max(150, r - 40 - rand.nextInt(20));
                g = Math.max(120, g - 40 - rand.nextInt(20));
                b = Math.max(5, b - 5 - rand.nextInt(10));
                int darkGoldColor = 0xFF000000 | (r << 16) | (g << 8) | b;

                r = Math.min(255, r + 20 + rand.nextInt(20));
                g = Math.min(255, g + 20 + rand.nextInt(20));
                b = Math.min(255, b + 40 + rand.nextInt(60));
                int highlightColor = 0xFF000000 | (r << 16) | (g << 8) | b;

                int leafR = 20 + rand.nextInt(60);
                int leafG = 150 + rand.nextInt(105);
                int leafB = 20 + rand.nextInt(60);
                int leafColor = 0xFF000000 | (leafR << 16) | (leafG << 8) | leafB;

                int stemR = 90 + rand.nextInt(80);
                int stemG = 40 + rand.nextInt(80);
                int stemB = 5 + rand.nextInt(40);
                int stemColor = 0xFF000000 | (stemR << 16) | (stemG << 8) | stemB;

                int appleSize = 5 + rand.nextInt(3);
                int offsetX = rand.nextInt(3) - 1;
                int offsetY = rand.nextInt(3) - 1;

                builder.setColor(goldColor);
                builder.fillCircle(size / 2 + offsetX, size / 2 + 1 + offsetY, appleSize);

                builder.setColor(darkGoldColor);
                int shadowX = size / 2 - 2 + rand.nextInt(3) - 1;
                int shadowY = size / 2 + 2 + rand.nextInt(3) - 1;
                builder.fillCircle(shadowX, shadowY, 2 + rand.nextInt(2));

                builder.setColor(highlightColor);
                int highlightX = size / 2 + 2 + rand.nextInt(3) - 1;
                int highlightY = size / 2 - 1 + rand.nextInt(3) - 1;
                builder.fillCircle(highlightX, highlightY, 1 + rand.nextInt(2));

                int stemWidth = 1 + rand.nextInt(2);
                int stemHeight = 2 + rand.nextInt(2);
                int stemPosX = size / 2 - stemWidth / 2 + offsetX;
                int stemPosY = size / 2 - 5 + rand.nextInt(2) - 1;

                builder.setColor(stemColor);
                builder.fillRect(stemPosX, stemPosY, stemWidth, stemHeight);

                int leafSize = 1 + rand.nextInt(2);
                int leafX = stemPosX + stemWidth + rand.nextInt(2);
                int leafY = stemPosY + rand.nextInt(2);

                builder.setColor(leafColor);
                builder.fillCircle(leafX, leafY, leafSize);

                int numSparkles = 1 + rand.nextInt(3);
                for (int i = 0; i < numSparkles; i++) {
                    int sparkleX = rand.nextInt(size);
                    int sparkleY = rand.nextInt(size);

                    double dist = Math.sqrt(Math.pow(sparkleX - (size / 2 + offsetX), 2)
                            + Math.pow(sparkleY - (size / 2 + offsetY), 2));
                    if (dist <= appleSize) {
                        int sparkleR = 240 + rand.nextInt(15);
                        int sparkleG = 240 + rand.nextInt(15);
                        int sparkleB = 190 + rand.nextInt(65);
                        int sparkleColor = 0xBF000000 | (sparkleR << 16) | (sparkleG << 8) | sparkleB;

                        builder.setColor(sparkleColor);
                        builder.fillCircle(sparkleX, sparkleY, 1);
                    }
                }

                return builder.build();
            } else {
                return new ImageMapping(TextureCache.get("minecraft/golden_apple.png"), new RectF(0.25f, 0, 0.75f, 1),
                        16);
            }
        }
    }

    /**
     * 初始化动态生成的贴图（示例）
     */
    public static void initDynamicTextures() {
        DynamicSpriteExample.createGoldenApple("golden_apple");
    }

    /**
     * Mob segment for Atlas-based mob sprite management
     */
    public static class MobSegment extends Segment<MobSegment> {
        private final String key;

        private Integer gridW;
        private Integer gridH;

        private static final class SpriteDef {
            final String name;
            final Integer x, y, w, h; // pixel definition
            final RectF rect;         // uv definition

            SpriteDef(String name, int x, int y, int w, int h) {
                this.name = name;
                this.x = x; this.y = y; this.w = w; this.h = h;
                this.rect = null;
            }

            SpriteDef(String name, RectF rect) {
                this.name = name;
                this.x = null; this.y = null; this.w = null; this.h = null;
                this.rect = new RectF(rect);
            }
        }

        private static final class AnimDef {
            final String baseName;
            final int startFrame;
            final int frameCount;

            AnimDef(String baseName, int startFrame, int frameCount) {
                this.baseName = baseName;
                this.startFrame = startFrame;
                this.frameCount = frameCount;
            }
        }

        private final ArrayList<SpriteDef> spriteDefs = new ArrayList<>();
        private final ArrayList<AnimDef> animDefs = new ArrayList<>();

        public MobSegment(String key, Object baseTextureHandle) {
            super(baseTextureHandle);
            this.key = key;
            ensureLoaded();
        }

        @Override
        protected MobSegment self() {
            return this;
        }

        @Override
        protected void afterLoad() {
            // restore grid (if any) and named regions (overlay switch recreates Atlas)
            if (gridW != null && gridH != null) {
                atlas.grid(gridW, gridH);
            }

            for (SpriteDef d : spriteDefs) {
                if (d.rect != null) {
                    atlas.add(d.name, new RectF(d.rect));
                } else {
                    atlas.add(d.name, d.x, d.y, d.x + d.w, d.y + d.h);
                }
            }

            for (AnimDef a : animDefs) {
                for (int i = 0; i < a.frameCount; i++) {
                    String frameName = a.baseName + "_" + i;
                    atlas.add(frameName, atlas.get(a.startFrame + i));
                }
            }
        }

        /**
         * Add a named sprite region to this mob atlas.
         */
        public MobSegment addSprite(String name, int x, int y, int width, int height) {
            ensureLoaded();
            atlas.add(name, x, y, x + width, y + height);
            spriteDefs.add(new SpriteDef(name, x, y, width, height));
            return this;
        }

        /**
         * Add a sprite using RectF coordinates (UV coords).
         */
        public MobSegment addSprite(String name, RectF rect) {
            ensureLoaded();
            atlas.add(name, rect);
            spriteDefs.add(new SpriteDef(name, rect));
            return this;
        }

        /**
         * Setup a regular grid for mob animations.
         */
        public MobSegment setupGrid(int frameWidth, int frameHeight) {
            ensureLoaded();
            this.gridW = frameWidth;
            this.gridH = frameHeight;
            atlas.grid(frameWidth, frameHeight);
            return this;
        }

        /**
         * Add animation frames with names.
         */
        public MobSegment addAnimation(String baseName, int startFrame, int frameCount) {
            ensureLoaded();
            animDefs.add(new AnimDef(baseName, startFrame, frameCount));
            for (int i = 0; i < frameCount; i++) {
                String frameName = baseName + "_" + i;
                atlas.add(frameName, atlas.get(startFrame + i));
            }
            return this;
        }

        /**
         * Get sprite mapping by name.
         */
        public ImageMapping getSprite(String name) {
            ensureLoaded();
            RectF rect = atlas.get(name);
            if (rect != null) {
                return new ImageMapping(cache, rect, atlas.height(rect), 16);
            }
            return null;
        }

        /**
         * Get sprite mapping by frame index (grid-based).
         */
        public ImageMapping getSprite(int frameIndex) {
            ensureLoaded();
            RectF rect = atlas.get(frameIndex);
            if (rect != null) {
                return new ImageMapping(cache, rect, atlas.height(rect), 16);
            }
            return null;
        }
    }

    /**
     * Icon segment (Atlas-backed, grid-based). Intended for buff icons, small UI icons, etc.
     */
    public static class IconSegment extends Segment<IconSegment> {
        private final int frameW;
        private final int frameH;

        public IconSegment(Object baseTextureHandle, int frameW, int frameH) {
            super(baseTextureHandle);
            this.frameW = frameW;
            this.frameH = frameH;
            ensureLoaded();
        }

        @Override
        protected IconSegment self() {
            return this;
        }

        @Override
        protected void afterLoad() {
            atlas.grid(frameW, frameH);
        }

        public ImageMapping get(int index, int size) {
            ensureLoaded();
            RectF rect = atlas.get(index);
            return new ImageMapping(cache, rect, atlas.height(rect), size);
        }
    }

    /**
     * UI segment (Atlas-backed, grid-based). Intended for chrome/toolbars/menus.
     */
    public static class UiSegment extends Segment<UiSegment> {
        private final int frameW;
        private final int frameH;

        public UiSegment(Object baseTextureHandle, int frameW, int frameH) {
            super(baseTextureHandle);
            this.frameW = frameW;
            this.frameH = frameH;
            ensureLoaded();
        }

        @Override
        protected UiSegment self() {
            return this;
        }

        @Override
        protected void afterLoad() {
            atlas.grid(frameW, frameH);
        }
    }

    /**
     * Tile segment (Atlas-backed, grid-based). Intended for tiles/water/terrain sheets.
     */
    public static class TileSegment extends Segment<TileSegment> {
        private final int tileSize;

        public TileSegment(Object baseTextureHandle, int tileSize) {
            super(baseTextureHandle);
            this.tileSize = tileSize;
            ensureLoaded();
        }

        @Override
        protected TileSegment self() {
            return this;
        }

        @Override
        protected void afterLoad() {
            atlas.grid(tileSize, tileSize);
        }
    }

    // ---------------------------
    // Tilemap & Tileset definitions (must be before static block)
    // ---------------------------

    /**
     * Tilemap texture definition for legacy APIs that only care about textures.
     * New code should prefer Tileset, which carries sheet information as well.
     */
    public static final class TilemapDef {
        public final String tilesTexture;   // path to tiles texture (e.g., "environment/tiles_custom.png")
        public final String waterTexture;   // path to water texture (e.g., "environment/water_custom.png")
        public final Map<String, String> meta = new HashMap<>();

        public TilemapDef(String tilesTexture, String waterTexture){
            this.tilesTexture = tilesTexture;
            this.waterTexture = waterTexture;
        }

        public TilemapDef(String tilesTexture){
            this(tilesTexture, null);
        }
    }

    /**
     * Tileset definition backed by a JSON sheet describing tile names and coordinates.
     * This is the recommended way to define map graphics for content packs.
     */
    public static final class Tileset {
        public final String key;
        public final String texture;
        public final int tileSize;
        public final String author;
        public final String version;

        // tile id (DungeonTileSheet constant) -> tile coordinate (x,y in tile grid, 1-based)
        public final Map<Integer, TileCoord> tiles = new HashMap<>();

        public Tileset(String key, String texture, int tileSize, String author, String version) {
            this.key = key;
            this.texture = texture;
            this.tileSize = tileSize;
            this.author = author;
            this.version = version;
        }
    }

    public static final class TileCoord {
        public final int x;
        public final int y;

        public TileCoord(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final Map<String, TilemapDef> tilemapDefs = new HashMap<>();
    private static final Map<String, Tileset> tilesets = new HashMap<>();

    // ---------------------------
    // Buff & UI icon (atlas) side
    // ---------------------------

    /**
     * Buff/UI atlases are intentionally simple for now: we allow a material pack to
     * override the small/large buff sheets and the generic UI icons sheet,
     * while the existing code (BuffIcon, various UI classes) continues to index
     * into them by integer.
     *
     * This keeps compatibility while routing all underlying textures through
     * SpriteRegistry so that packs can swap them out.
     */
    private static Object buffSmallTexture = Assets.Interfaces.BUFFS_SMALL;
    private static Object buffLargeTexture = Assets.Interfaces.BUFFS_LARGE;
    private static Object uiIconsTexture   = Assets.Interfaces.ICONS;

    // Material ids for overlay mapping (file priority only)
    public static final String MAT_BUFFS_SMALL = "ui.buffs.small";
    public static final String MAT_BUFFS_LARGE = "ui.buffs.large";
    public static final String MAT_UI_ICONS    = "ui.icons";

    // hero class -> overridden spritesheet handle
    private static final Map<HeroClass, Object> heroTextures = new HashMap<>();

    public static void setBuffTextures(Object small, Object large) {
        if (small != null) buffSmallTexture = small;
        if (large != null) buffLargeTexture = large;
    }

    public static void setUiIconsTexture(Object uiIcons) {
        if (uiIcons != null) uiIconsTexture = uiIcons;
    }

    public static Object resolveBuffTexture(boolean large) {
        return resolveMaterial(
                large ? MAT_BUFFS_LARGE : MAT_BUFFS_SMALL,
                large ? buffLargeTexture : buffSmallTexture
        );
    }

    public static Object resolveUiIconsTexture() {
        return resolveMaterial(MAT_UI_ICONS, uiIconsTexture);
    }

    public static void registerHeroTexture(HeroClass cls, Object texture) {
        if (cls != null && texture != null) {
            heroTextures.put(cls, texture);
        }
    }

    public static Object heroTextureOr(HeroClass cls, Object fallbackTexture) {
        if (cls != null) {
            Object tx = heroTextures.get(cls);
            if (tx != null) {
                return tx;
            }
        }
        return fallbackTexture;
    }

    static {
        registerItemTexture("minecraft/misc.png", 16)
                .span(144).label("skel");
        registerItemTexture("minecraft/bread.png", 16)
                .label("bread");
        registerItemTexture("effects/misc.png", 16)
                .label("square")
                .label("cross")
                .label("slash");
        registerItemTexture("sprites/gun.png", 16)
                .label("gun");
        registerItemTexture("effects/gunfire.png", 16)
                .label("gunfire");
        registerItemTexture("cola/mask16.png", 16)
                .label("mask16");
        registerItemTexture("cola/mask32.png", 32)
                .label("mask32");
        registerItemTexture("cola/mask64.png", 64)
                .label("mask64");
        registerItemTexture("cola/arksupply.png", 64).label("arksupply");

        // Register tilemap textures (legacy, texture-only)
        registerTilemap(
                "cola:chel_sewer",
                new TilemapDef("cola/tiles_chel.png", Assets.Environment.WATER_SEWERS)
        );

        // Register tileset with JSON sheet for cola:chel_sewer
        // This allows the tiles_chel.png image to define its own layout via JSON,
        // instead of relying on the built-in DungeonTileSheet layout.
        registerTilesetFromJson(
                "cola:chel_sewer",
                "cola/tiles_chel.png",
                "cola/tiles_chel.json"
        );
    }

    // ---------------------------
    // Item side
    // ---------------------------

    /**
     * Returns a dynamic mapping by key with enforced fallback to a static item id.
     * If key is missing, returns mapping built from ItemSpriteSheet for elseId.
     */
    public static ImageMapping itemMapping(String key, int elseId){
        Objects.requireNonNull(key, "key");
        ImageMapping m = getItemImageMapping(key);
        if (m != null) return m;
        // fallback to static sheet id
        return staticItemMapping(elseId);
    }

    /**
     * Returns a static mapping built from ItemSpriteSheet film for a given id.
     */
    public static ImageMapping staticItemMapping(int id){
        RectF rect = ItemSpriteSheet.film.get(id);
        if (rect == null){
            // If invalid id, fallback to SOMETHING
            rect = ItemSpriteSheet.film.get(ItemSpriteSheet.SOMETHING);
        }
        SmartTexture tx = TextureCache.get(Assets.Sprites.ITEMS);
        float h = ItemSpriteSheet.film.height(id != 0 ? id : ItemSpriteSheet.SOMETHING);
        return new ImageMapping(tx, rect, h, ItemSpriteSheet.SIZE);
    }

    /**
     * Returns a resolved image id for backward compatibility:
     * - if dynamic key exists, returns its assigned dynamic id
     * - else returns fallback static id
     *
     * This is useful when code path expects an integer image id.
     */
    public static int itemImageId(String key, int elseId){
        Integer dyn = ITEM_TEXTURE_ID_MAP.get(key);
        return dyn != null ? dyn : elseId;
    }

    // ---------------------------
    // Mob side
    // ---------------------------

    public static final class MobDef {
        public final Object texture;  // path or Assets.Sprites.* handle
        public final int frameWidth;
        public final int frameHeight;
        public final Map<String, String> meta = new HashMap<>();

        public MobDef(Object texture, int frameWidth, int frameHeight){
            this.texture = texture;
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
        }
    }

    private static final HashMap<String, MobSegment> mobSegments = new HashMap<>();
    private static final Map<String, MobDef> mobDefs = new HashMap<>();

    /**
     * Registers a dynamic mob sprite definition.
     * Callers MUST ensure they also provide a fallback when fetching.
     */
    public static void registerMob(String key, MobDef def){
        if (key == null || def == null) return;
        mobDefs.put(key, def);
    }

    /**
     * Resolves mob texture handle by key, or returns fallbackTexture if key missing.
     * This only returns the texture object; framing remains the responsibility of caller.
     */
    /**
     * Register a mob texture with Atlas support for flexible sprite layouts
     */
    public static MobSegment registerMobAtlas(String key, Object texture) {
        MobSegment segment = new MobSegment(key, texture);
        mobSegments.put(key, segment);
        return segment;
    }
    
    /**
     * Get mob segment for advanced sprite management
     */
    public static MobSegment getMobSegment(String key) {
        return mobSegments.get(key);
    }

    /**
     * Get mob sprite mapping with Atlas support
     */
    public static ImageMapping getMobSprite(String key, String spriteName, Object fallbackTexture) {
        MobSegment segment = mobSegments.get(key);
        if (segment != null) {
            ImageMapping mapping = segment.getSprite(spriteName);
            if (mapping != null) {
                return mapping;
            }
        }
        
        // Fallback to legacy texture
        Object texture = mobTextureOr(fallbackTexture, key);
        SmartTexture smartTex = TextureCache.get(texture);
        return new ImageMapping(smartTex, new RectF(0, 0, 1, 1), smartTex.height, 16);
    }

    public static Object mobTextureOr(Object fallbackTexture, String key){
        if (key != null){
            MobDef def = mobDefs.get(key);
            if (def != null && def.texture != null){
                return def.texture;
            }
        }
        return fallbackTexture;
    }

    /**
     * @deprecated Use getMobSprite() with Atlas-based system instead
     * Returns a TextureFilm for a mob using a dynamic key if present; otherwise builds from fallback texture+size.
     */
    @Deprecated
    public static com.watabou.noosa.TextureFilm mobFilm(String key, Object fallbackTexture, int fallbackFrameW, int fallbackFrameH){
        if (key != null){
            MobDef def = mobDefs.get(key);
            if (def != null && def.texture != null){
                SmartTexture tx = TextureCache.get(def.texture);
                return new com.watabou.noosa.TextureFilm(tx, def.frameWidth, def.frameHeight);
            }
        }
        SmartTexture tx = TextureCache.get(fallbackTexture);
        return new com.watabou.noosa.TextureFilm(tx, fallbackFrameW, fallbackFrameH);
    }

    /**
     * Registers a dynamic tilemap texture set.
     * Key format suggestion: "mod:region_name" (e.g., "mymod:shadow_forest")
     *
     * @param key Unique identifier for this tilemap
     * @param def TilemapDef containing tiles and optional water texture paths
     */
    public static void registerTilemap(String key, TilemapDef def){
        if (key == null || def == null) return;
        tilemapDefs.put(key, def);
    }

    /**
     * Resolves tilemap tiles texture by key, or returns fallback if key missing.
     *
     * @param key Dynamic tilemap key (can be null)
     * @param fallbackTexture Default texture path (e.g., Assets.Environment.TILES_SEWERS)
     * @return Resolved texture path
     */
    public static String tilemapTilesTextureOr(String fallbackTexture, String key){
        if (key != null){
            TilemapDef def = tilemapDefs.get(key);
            if (def != null && def.tilesTexture != null){
                return def.tilesTexture;
            }
        }
        return fallbackTexture;
    }

    /**
     * Resolves tilemap water texture by key, or returns fallback if key missing.
     *
     * @param key Dynamic tilemap key (can be null)
     * @param fallbackTexture Default texture path (e.g., Assets.Environment.WATER_SEWERS)
     * @return Resolved texture path
     */
    public static String tilemapWaterTextureOr(String fallbackTexture, String key){
        if (key != null){
            TilemapDef def = tilemapDefs.get(key);
            if (def != null && def.waterTexture != null){
                return def.waterTexture;
            }
        }
        return fallbackTexture;
    }

    /**
     * Gets registered tilemap definition by key.
     *
     * @param key Dynamic tilemap key
     * @return TilemapDef or null if not found
     */
    public static TilemapDef getTilemapDef(String key){
        return tilemapDefs.get(key);
    }

    /**
     * Checks if a tilemap key is registered.
     *
     * @param key Dynamic tilemap key
     * @return true if registered, false otherwise
     */
    public static boolean hasTilemap(String key){
        return key != null && tilemapDefs.containsKey(key);
    }

    // ---------------------------
    // Tileset side
    // ---------------------------

    /**
     * Returns a Tileset by key, or null if not found.
     */
    public static Tileset getTileset(String key) {
        if (key == null) return null;
        return tilesets.get(key);
    }

    /**
     * Registers a tileset from a JSON sheet:
     * - texturePath is the tiles image (e.g. "cola/tiles_chel.png")
     * - jsonPath is the sheet (e.g. "cola/tiles_chel.json")
     *
     * JSON format example:
     * {
     *   "author": "Cola Dungeon Team",
     *   "version": "1.0.0",
     *   "pixelsize": 16,
     *   "FLOOR": [1, 1],
     *   "GRASS": [3, 1]
     * }
     *
     * All keys other than author/version/pixelsize are interpreted as
     * tile names that correspond to static int fields in DungeonTileSheet,
     * e.g. "FLOOR" -> DungeonTileSheet.FLOOR.
     */
    public static Tileset registerTilesetFromJson(String key, String texturePath, String jsonPath) {
        try {
            JsonValue root = new JsonReader().parse(Gdx.files.internal(jsonPath));

            String author = root.getString("author", null);
            String version = root.getString("version", null);
            int pixelSize = root.getInt("pixelsize", 16);

            Tileset tileset = new Tileset(key, texturePath, pixelSize, author, version);

            for (JsonValue child : root) {
                String name = child.name;
                if ("author".equals(name) || "version".equals(name) || "pixelsize".equals(name)) {
                    continue;
                }

                // Expect [x, y] array
                if (!child.isArray() || child.size < 2) {
                    System.out.println("Invalid tileset entry for " + key + ": " + name);
                    continue;
                }

                int x = child.getInt(0);
                int y = child.getInt(1);

                try {
                    Field field = DungeonTileSheet.class.getField(name);
                    int tileId = field.getInt(null);
                    tileset.tiles.put(tileId, new TileCoord(x, y));
                } catch (NoSuchFieldException nsfe) {
                    System.out.println("Tileset " + key + " refers to unknown tile name: " + name);
                } catch (IllegalAccessException iae) {
                    System.out.println("Cannot access DungeonTileSheet field for tile name: " + name);
                }
            }

            tilesets.put(key, tileset);
            System.out.println("Registered tileset: " + key + " (texture=" + texturePath + ")");
            return tileset;
        } catch (Exception e) {
            System.out.println("Failed to load tileset JSON for key " + key + " from " + jsonPath);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Applies a tileset's sheet layout to a given TextureFilm by remapping all known
     * DungeonTileSheet tile ids to coordinates defined in the tileset JSON.
     *
     * Any tile id that is not explicitly present in the tileset will fall back to:
     * - the coordinate defined for DungeonTileSheet.FLOOR if present, or
     * - the first entry in the tileset's map.
     *
     * This is primarily intended for content packs and debugging, so that a tileset
     * can override the default Pixel Dungeon tile layout without touching core logic.
     */
    public static void applyTilesetToFilm(com.watabou.noosa.TextureFilm film, String tilesetKey, int tileSize) {
        if (tilesetKey == null || film == null) return;

        Tileset tilesetDef = tilesets.get(tilesetKey);
        if (tilesetDef == null) return;

        // Pick a fallback coordinate: FLOOR if defined, otherwise first entry.
        TileCoord fallback = tilesetDef.tiles.get(DungeonTileSheet.FLOOR);
        if (fallback == null && !tilesetDef.tiles.isEmpty()) {
            fallback = tilesetDef.tiles.values().iterator().next();
        }
        if (fallback == null) return;

        // Remap every public static int constant in DungeonTileSheet.
        for (Field field : DungeonTileSheet.class.getFields()) {
            try {
                if (!Modifier.isStatic(field.getModifiers())) continue;
                if (field.getType() != int.class) continue;

                int tileId = field.getInt(null);

                TileCoord coord = tilesetDef.tiles.get(tileId);
                if (coord == null) {
                    coord = fallback;
                }
                if (coord == null) continue;

                int left   = (coord.x - 1) * tileSize;
                int top    = (coord.y - 1) * tileSize;
                int right  = left + tileSize;
                int bottom = top + tileSize;

                film.add(tileId, left, top, right, bottom);

            } catch (IllegalAccessException e) {
                // Should not happen for public static fields, but log just in case.
                System.out.println("Failed to remap tile id from DungeonTileSheet field: " + field.getName());
            }
        }

        System.out.println("Applied tileset '" + tilesetKey + "' to Tilemap film (" + tilesetDef.tiles.size() + " explicit mappings, with fallback).");
    }
}

