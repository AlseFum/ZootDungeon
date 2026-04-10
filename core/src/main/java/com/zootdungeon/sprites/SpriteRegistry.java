package com.zootdungeon.sprites;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.zootdungeon.Assets;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.gltextures.Atlas;
import com.watabou.utils.RectF;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public final class SpriteRegistry {

    private SpriteRegistry() {}

    /*懒加载的Atlas，作为基类使用。*/
    public abstract static class LazyAtlas {
        protected final Object baseTextureHandle;
        protected Object resolvedTextureHandle;

        protected SmartTexture cache;
        protected Atlas atlas;

        public LazyAtlas(Object baseTextureHandle) {
            this.baseTextureHandle = baseTextureHandle;
        }

        /** Loads/reloads the texture and atlas if the resolved handle changed. */
        public final void load() {
            Object handle = baseTextureHandle;
            if (handle instanceof String) handle = Assets.getTexture((String) handle);
            if (cache == null || cache.bitmap == null || !Objects.equals(resolvedTextureHandle, handle)) {
                cache = TextureCache.get(handle);
                atlas = new Atlas(cache);
                resolvedTextureHandle = handle;
                afterLoad();
            }
        }

        /** Called after a (re)load to rebuild atlas slicing/mappings. */
        public void afterLoad() {}
    }

    /** 所有已分配的动态物品图集；用于把 image id 解析到具体 sheet。 */
    public static final ArrayList<TextureSheet> itemSheets = new ArrayList<>();
    /** label -> 全局 image id。 */
    public static final HashMap<String, Integer> LABEL_ID_MAP = new HashMap<>();
    /** 名称/label -> sheet（允许别名）。 */
    public static final HashMap<String, TextureSheet> LABEL_SHEETS_MAP = new HashMap<>();
    /** 纹理 path/key -> sheet（按来源去重复用）。 */
    public static final HashMap<String, TextureSheet> PATH_SHEETS_MAP = new HashMap<>();
    /** 最新分配的 item id 位置，用于分配新的 item id。 */
    public static int latestItemLocation = 1024;

    /** 动态纹理序列号，用于区分不同动态纹理。 */
    public static int dynamicTextureSeq = 1;

    /** Item texture sheet with optional grid and named regions. */
    public static final class TextureSheet extends LazyAtlas {
        int id_start;
        int id_size;
        int size;
        int cols;

        // localIndex -> optional label for rebuilding named frames after reload
        private final ArrayList<String> labels = new ArrayList<>();
        // localIndex -> optional custom rect for non-grid sprites (rebuild after reload)
        private final ArrayList<RectF> customRects = new ArrayList<>();
        // localIndex -> custom sprite pixel size for scaling (used when rect is custom)
        private final ArrayList<Integer> customSizes = new ArrayList<>();

        private Integer gridW;
        private Integer gridH;

        TextureSheet(String texture, int id_start, int size) {
            super(texture);
            this.id_start = id_start;
            this.id_size = 0;
            this.size = size;
            load();
        }

        /** Returns the registered texture handle/path (unresolved). */
        public Object textureHandle() {
            return baseTextureHandle;
        }

        @Override
        public void afterLoad() {
            // Setup grid (default square) unless overridden by grid(w,h)
            if (gridW != null && gridH != null) {
                this.atlas.grid(gridW, gridH);
                this.cols = (int) (cache.width / gridW);
            } else {
                this.atlas.grid(this.size, this.size);
                this.cols = (int) (cache.width / size);
            }

            // Rebuild label mappings after reload
            for (int i = 0; i < labels.size(); i++) {
                String label = labels.get(i);
                RectF custom = i < customRects.size() ? customRects.get(i) : null;
                if (custom != null) {
                    atlas.add(i, new RectF(custom));
                    if (label != null) atlas.add(label, new RectF(custom));
                } else if (label != null) {
                    RectF rect = atlas.get(i);
                    atlas.add(label, rect);
                }
            }
        }

        public ImageMapping get(int id) {
            load();
            int where = id >= id_start ? id - id_start : id;

            boolean isCustom = where >= 0 && where < customRects.size() && customRects.get(where) != null;
            RectF rect = isCustom ? customRects.get(where) : atlas.get(where);
            if (atlas.get((Object) where) == null) {
                atlas.add(where, rect);
            }

            int spriteSize = size;
            if (isCustom && where < customSizes.size() && customSizes.get(where) != null) {
                spriteSize = Math.max(1, customSizes.get(where));
            }
            return new ImageMapping(cache, rect, atlas.height(rect), spriteSize);
        }

        public ImageMapping get(String label) {
            load();
            RectF rect = atlas.get(label);
            if (rect != null) {
                return new ImageMapping(cache, rect, atlas.height(rect), size);
            }
            Integer id = LABEL_ID_MAP.get(label);
            return id == null ? null : get(id);
        }

        /* Switch this sheet to a non-square grid. */
        public TextureSheet grid(int cellW, int cellH) {
            load();
            this.gridW = cellW;
            this.gridH = cellH;
            this.atlas.grid(cellW, cellH);
            this.cols = (int) (cache.width / cellW);
            this.size = cellW;
            return this;
        }

        /* Define a named region in pixel coordinates. */
        public TextureSheet setXY(String label, int x, int y, int width, int height) {
            load();
            if (label == null) return this;

            RectF rect = new RectF(
                    x / (float) cache.width,
                    y / (float) cache.height,
                    (x + width) / (float) cache.width,
                    (y + height) / (float) cache.height
            );

            LABEL_ID_MAP.put(label, id_start + id_size);
            LABEL_SHEETS_MAP.put(label, this);
            atlas.add(id_size, rect);
            atlas.add(label, rect);

            while (labels.size() <= id_size) labels.add(null);
            labels.set(id_size, label);
            while (customRects.size() <= id_size) customRects.add(null);
            customRects.set(id_size, rect);
            while (customSizes.size() <= id_size) customSizes.add(null);
            customSizes.set(id_size, Math.max(width, height));

            id_size++;
            return this;
        }

        public TextureSheet setXY(int label, int x, int y, int width, int height) {
            return setXY(String.valueOf(label), x, y, width, height);
        }

        /* Define a named region by grid coordinates. */
        public TextureSheet setXY(String label, int gridX, int gridY) {
            int cw = gridW != null ? gridW : size;
            int ch = gridH != null ? gridH : size;
            return setXY(label, gridX * cw, gridY * ch, cw, ch);
        }

        public TextureSheet setXY(int label, int gridX, int gridY) {
            return setXY(String.valueOf(label), gridX, gridY);
        }

        public RowBuilder row(int x, int y, int width, int height) {
            return new RowBuilder(this, x, y, width, height, false, 0, 0);
        }

        public RowBuilder group(int x, int y, int rows, int cols, int width, int height) {
            return new RowBuilder(this, x, y, width, height, true, rows, cols);
        }

        public static final class RowBuilder {
            private final TextureSheet sheet;
            private final int startX, startY, w, h;
            private final boolean wrap;
            private final int rows, cols;

            RowBuilder(TextureSheet sheet, int x, int y, int w, int h, boolean wrap, int rows, int cols) {
                this.sheet = sheet;
                this.startX = x;
                this.startY = y;
                this.w = w;
                this.h = h;
                this.wrap = wrap;
                this.rows = rows;
                this.cols = cols;
            }

            public TextureSheet label(String... names) {
                if (names == null) return sheet;
                int x = startX;
                int y = startY;
                int col = 0;
                int row = 0;
                for (String n : names) {
                    if (n == null) continue;
                    sheet.setXY(n, x, y, w, h);

                    x += w;
                    col++;
                    if (wrap && cols > 0 && col >= cols) {
                        col = 0;
                        row++;
                        x = startX;
                        y += h;
                        if (rows > 0 && row >= rows) {
                            break;
                        }
                    }
                }
                return sheet;
            }
        }

        public TextureSheet label(String label) {
            load();
            LABEL_ID_MAP.put(label, id_start + id_size);
            LABEL_SHEETS_MAP.put(label, this);

            RectF rect = atlas.get(id_size);
            atlas.add(id_size, rect);
            atlas.add(label, rect);

            while (labels.size() <= id_size) labels.add(null);
            labels.set(id_size, label);

            id_size++;
            return this;
        }

        /* Reserve slots in current sheet. */
        public TextureSheet span(int size) {
            for (int i = 0; i < size; i++) labels.add(null);
            this.id_size += size;
            return this;
        }
    }

    /**
     * Register a texture sheet and return its builder facade.
     */
    public static TextureSheet texture(String textureName, String pathToFile) {
        if (textureName == null) return null;

        if (pathToFile != null) {
            TextureSheet existing = PATH_SHEETS_MAP.get(pathToFile);
            if (existing != null) {
                LABEL_SHEETS_MAP.put(textureName, existing);
                return existing;
            }
        }

        TextureSheet sheet = LABEL_SHEETS_MAP.get(textureName);
        if (sheet == null) {
            // Default grid is 16×16 for backward compatibility; callers can override via grid().
            sheet = allocateSheet(pathToFile, 16);
            LABEL_SHEETS_MAP.put(textureName, sheet);
            if (pathToFile != null) PATH_SHEETS_MAP.put(pathToFile, sheet);
        }
        return sheet;
    }

    /**
     * Get a previously registered texture sheet by name.
     */
    public static TextureSheet texture(String textureName) {
        return LABEL_SHEETS_MAP.get(textureName);
    }

    /**
     * Shorthand for {@link #texture(String)}.
     */
    public static TextureSheet the(String textureName) {
        return texture(textureName);
    }

    /*
    ## dynamic(textureName, baseTextureLabel, pixmapPainter)
    - `baseTextureLabel` must be a sheet label registered via `texture(label, path)`
    - This method only creates/registers the new texture label (no slicing helpers by design)
    */
    public static void dynamic(String textureName, String baseTextureLabel, java.util.function.Consumer<Pixmap> pixmapPainter) {
        if (textureName == null || baseTextureLabel == null) return;

        TextureSheet baseSheet = texture(baseTextureLabel);
        if (baseSheet == null) {
            throw new IllegalArgumentException("Base texture label not registered: " + baseTextureLabel);
        }
        baseSheet.load();

        Object rawHandle = baseSheet.resolvedTextureHandle != null ? baseSheet.resolvedTextureHandle : baseSheet.baseTextureHandle;
        if (!(rawHandle instanceof String)) rawHandle = String.valueOf(rawHandle);
        String resolvedPath = Assets.getTexture((String) rawHandle);

        Pixmap src = null;
        Pixmap dst = null;
        try {
            src = new Pixmap(Gdx.files.internal(resolvedPath));
            dst = new Pixmap(src.getWidth(), src.getHeight(), src.getFormat());
            dst.drawPixmap(src, 0, 0);
            if (pixmapPainter != null) pixmapPainter.accept(dst);

            String key = "dynamic/" + textureName + "/" + (dynamicTextureSeq++);
            com.watabou.gltextures.SmartTexture tx = com.watabou.gltextures.TextureCache.create(key, dst.getWidth(), dst.getHeight());
            tx.bitmap(dst);

            // Register/alias the generated texture under the requested label.
            texture(textureName, key);
        } finally {
            if (dst != null) dst.dispose();
            if (src != null) src.dispose();
        }
    }

    public static int byLabel(String name) {
        Integer res = LABEL_ID_MAP.get(name);
        if (res == null) {
            System.out.println("Invalid texture name: " + name);
            return ItemSpriteSheet.SOMETHING;
        }
        return res;
    }

    static TextureSheet allocateSheet(String texture, int size) {
        TextureSheet s = new TextureSheet(texture, latestItemLocation, size);
        itemSheets.add(s);
        latestItemLocation += 256;
        return s;
    }

    /*
    ## ImageMapping
    Dynamic mapping container: texture + uv rect (+ height/size for rendering).
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
    static {
        // built-in UI texture labels (callers should use `SpriteRegistry.the(label).textureHandle()`)
        texture("ui.icons", Assets.Interfaces.ICONS);
        texture("ui.buffs_small", Assets.Interfaces.BUFFS_SMALL);
        texture("ui.buffs_large", Assets.Interfaces.BUFFS_LARGE);
        texture("ui.item_icons", Assets.Sprites.ITEM_ICONS);
        texture("ui.hero_icons", Assets.Interfaces.HERO_ICONS);

        texture("sheet.minecraft.misc", "minecraft/misc.png").grid(16, 16).span(144).label("skel");
        texture("sheet.sprites.gun", "sprites/gun.png").grid(16, 16).label("gun");
        texture("sheet.effects.gunfire", "effects/gunfire.png").grid(16, 16).label("gunfire");
        texture("sheet.cola.arksupply", "cola/arksupply.png").grid(64, 64).label("arksupply");

    }
}

