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

/**
 * @see docs/skill/SpriteRegistry.md
 * @see SpriteRegistry.AtlasSheet
 * @see SpriteRegistry.AtlasArea
 * @see SpriteRegistry.TextureHandle
 */
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
    public static final ArrayList<AtlasSheet> itemSheets = new ArrayList<>();
    /** label -> 全局 image id。 */
    public static final HashMap<String, Integer> LABEL_ID_MAP = new HashMap<>();
    /** 名称/label -> sheet（允许别名）。 */
    public static final HashMap<String, AtlasSheet> LABEL_SHEETS_MAP = new HashMap<>();
    /** 名称/label -> handle（用于只注册纹理句柄的场景）。 */
    public static final HashMap<String, TextureHandle> LABEL_HANDLES_MAP = new HashMap<>();
    /** 纹理 path/key -> sheet（按来源去重复用）。 */
    public static final HashMap<String, AtlasSheet> PATH_SHEETS_MAP = new HashMap<>();
    /** 最新分配的 item id 位置，用于分配新的 item id。 */
    public static int latestItemLocation = 1024;

    /** 动态纹理序列号，用于区分不同动态纹理。 */
    public static int dynamicTextureSeq = 1;

    // ==============================================================
    // TextureHandle: 指向一张图片的句柄，不切片
    // ==============================================================

    /** 只持有纹理路径，用于只需要原始纹理的场景（如 MobSprite）。 */
    public static final class TextureHandle {
        private final String name;
        private final Object baseHandle;

        TextureHandle(String name, Object baseHandle) {
            this.name = name;
            this.baseHandle = baseHandle;
        }

        public Object textureHandle() {
            return baseHandle;
        }

        public String name() {
            return name;
        }
    }

    // ==============================================================
    // AtlasArea: 从纹理中划分出来的一块区域（单格数据容器）
    // ==============================================================

    /** 纹理中的一块区域数据：texture + uv rect + size。 */
    public static class AtlasArea {
        public SmartTexture texture;
        public RectF rect;
        public float height;
        public int size;

        public AtlasArea(SmartTexture texture, RectF rect, float height) {
            this.rect = rect;
            this.height = height;
            this.texture = texture;
            this.size = 16;
        }

        public AtlasArea(SmartTexture texture, RectF rect, float height, int size) {
            this.rect = rect;
            this.height = height;
            this.texture = texture;
            this.size = size;
        }

        public com.watabou.noosa.Image toImage() {
            com.watabou.noosa.Image img = new com.watabou.noosa.Image(texture);
            img.frame(rect);
            return img;
        }
    }

    /** @deprecated Use {@link AtlasArea} */
    @Deprecated
    public static final class ImageMapping extends AtlasArea {
        public ImageMapping(SmartTexture texture, RectF rect, float height) {
            super(texture, rect, height);
        }
        public ImageMapping(SmartTexture texture, RectF rect, float height, int size) {
            super(texture, rect, height, size);
        }
    }

    // ==============================================================
    // AtlasSheet: 加载一张图片并按 grid/row 切出多个区域
    // ==============================================================

    public static class AtlasSheet extends LazyAtlas {
        public final int id_start;
        int id_size;
        public int size;
        public int cols;

        // localIndex -> optional label for rebuilding named frames after reload
        private final ArrayList<String> labels = new ArrayList<>();
        // localIndex -> optional custom rect for non-grid sprites (rebuild after reload)
        private final ArrayList<RectF> customRects = new ArrayList<>();
        // localIndex -> custom sprite pixel size for scaling (used when rect is custom)
        private final ArrayList<Integer> customSizes = new ArrayList<>();

        private Integer gridW;
        private Integer gridH;
        /** 是否已完成隐式 label 自动分配。 */
        private boolean implicitLabelsAssigned = false;
        /** 隐式 label 自动分配到的索引上限。 */
        private int implicitLabelsCount = 0;

        AtlasSheet(String texture, int id_start, int size) {
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
                this.size = gridW;
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

        public AtlasArea getArea(int id) {
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
            return new AtlasArea(cache, rect, atlas.height(rect), spriteSize);
        }

        public AtlasArea getArea(String label) {
            load();
            RectF rect = atlas.get(label);
            if (rect != null) {
                return new AtlasArea(cache, rect, atlas.height(rect), size);
            }
            Integer id = LABEL_ID_MAP.get(label);
            return id == null ? null : getArea(id);
        }

        // ----------------------------------------------------------------
        // 切片 helpers
        // ----------------------------------------------------------------

        /** Switch this sheet to a non-square grid. */
        public AtlasSheet grid(int cellW, int cellH) {
            load();
            this.gridW = cellW;
            this.gridH = cellH;
            this.atlas.grid(cellW, cellH);
            this.cols = (int) (cache.width / cellW);
            this.size = cellW;
            return this;
        }

        /** Define a named region in pixel coordinates. */
        public AtlasSheet setXY(String label, int x, int y, int width, int height) {
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

        public AtlasSheet setXY(int label, int x, int y, int width, int height) {
            return setXY(String.valueOf(label), x, y, width, height);
        }

        /** Define a named region by grid coordinates. */
        public AtlasSheet setXY(String label, int gridX, int gridY) {
            int cw = gridW != null ? gridW : size;
            int ch = gridH != null ? gridH : size;
            return setXY(label, gridX * cw, gridY * ch, cw, ch);
        }

        public AtlasSheet setXY(int label, int gridX, int gridY) {
            return setXY(String.valueOf(label), gridX, gridY);
        }

        public RowBuilder row(int x, int y, int width, int height) {
            return new RowBuilder(this, x, y, width, height, false, 0, 0);
        }

        public RowBuilder group(int x, int y, int rows, int cols, int width, int height) {
            return new RowBuilder(this, x, y, width, height, true, rows, cols);
        }

        public static final class RowBuilder {
            private final AtlasSheet sheet;
            private final int startX, startY, w, h;
            private final boolean wrap;
            private final int rows, cols;

            RowBuilder(AtlasSheet sheet, int x, int y, int w, int h, boolean wrap, int rows, int cols) {
                this.sheet = sheet;
                this.startX = x;
                this.startY = y;
                this.w = w;
                this.h = h;
                this.wrap = wrap;
                this.rows = rows;
                this.cols = cols;
            }

            public AtlasSheet areas(String... names) {
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

        /**
         * 为当前指针位置分配一个区域。
         * 同时推进指针。
         */
        public AtlasSheet area(String label) {
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

        /** @deprecated Use {@link #area(String)} */
        @Deprecated
        public AtlasSheet label(String label) {
            return area(label);
        }

        /**
         * 为当前指针位置分配一个区域，使用隐式数字 label（从 0 开始递增）。
         * 推进指针。
         */
        public AtlasSheet area() {
            return area(String.valueOf(id_size));
        }

        /**
         * 批量为所有现有格子分配隐式数字 label。
         * 如果格子数为 0（尚未切 grid），则先按 size 切出整张图的格子。
         */
        public AtlasSheet areas() {
            if (!implicitLabelsAssigned) {
                assignImplicitLabels();
                implicitLabelsAssigned = true;
            } else {
                // 已分配过，追加新的 label
                for (int i = implicitLabelsCount; i < id_size; i++) {
                    area(String.valueOf(i));
                }
                implicitLabelsCount = id_size;
            }
            return this;
        }

        /** @deprecated Use {@link #areas()} */
        @Deprecated
        public AtlasSheet label() {
            return areas();
        }

        private void assignImplicitLabels() {
            load();
            // 如果尚未切过 grid，先计算整张图的格子数
            int cw = gridW != null ? gridW : size;
            int ch = gridH != null ? gridH : size;
            int totalCols = (int) (cache.width / cw);
            int totalRows = (int) (cache.height / ch);
            int totalCells = totalCols * totalRows;

            for (int i = 0; i < totalCells; i++) {
                area(String.valueOf(i));
            }
            implicitLabelsCount = totalCells;
        }

        /** Reserve slots in current sheet. */
        public AtlasSheet span(int size) {
            for (int i = 0; i < size; i++) labels.add(null);
            this.id_size += size;
            return this;
        }

        /** @deprecated Use {@link #getArea(int)} */
        @Deprecated
        public AtlasArea get(int id) {
            return getArea(id);
        }

        /** @deprecated Use {@link #getArea(String)} */
        @Deprecated
        public AtlasArea get(String label) {
            return getArea(label);
        }

        /** @deprecated Use {@link AtlasSheet} */
        @Deprecated
        public static final class TextureSheet extends AtlasSheet {
            TextureSheet(String texture, int id_start, int size) {
                super(texture, id_start, size);
            }
        }
    }

    // ==============================================================
    // 静态方法
    // ==============================================================

    /**
     * Register a texture handle and return it.
     * Use when you only need the raw texture (e.g., for TextureFilm slicing).
     */
    public static TextureHandle handle(String textureName, String path) {
        if (textureName == null) return null;

        // 如果已有 handle，直接 alias
        TextureHandle existing = LABEL_HANDLES_MAP.get(textureName);
        if (existing != null) {
            return existing;
        }

        // 如果已有 sheet 的 alias，忽略 path
        AtlasSheet sheet = LABEL_SHEETS_MAP.get(textureName);
        if (sheet != null) {
            return new TextureHandle(textureName, sheet.baseTextureHandle);
        }

        TextureHandle h = new TextureHandle(textureName, path);
        LABEL_HANDLES_MAP.put(textureName, h);
        return h;
    }

    /**
     * Get a previously registered texture handle by name.
     */
    public static TextureHandle handle(String textureName) {
        TextureHandle h = LABEL_HANDLES_MAP.get(textureName);
        if (h != null) return h;
        // 尝试从 sheet 获取
        AtlasSheet sheet = LABEL_SHEETS_MAP.get(textureName);
        if (sheet != null) {
            return new TextureHandle(textureName, sheet.baseTextureHandle);
        }
        return null;
    }

    /**
     * Register a texture sheet and return its builder.
     * Also registers a handle alias for the texture name.
     */
    public static AtlasSheet area(String textureName, String pathToFile) {
        if (textureName == null) return null;

        // 已存在 sheet，直接 alias 并返回
        AtlasSheet existing = LABEL_SHEETS_MAP.get(textureName);
        if (existing != null) {
            if (pathToFile != null) {
                AtlasSheet byPath = PATH_SHEETS_MAP.get(pathToFile);
                if (byPath != null && byPath != existing) {
                    LABEL_SHEETS_MAP.put(textureName, byPath);
                    return byPath;
                }
                PATH_SHEETS_MAP.put(pathToFile, existing);
            }
            return existing;
        }

        // 按 path 查找已存在的 sheet
        if (pathToFile != null) {
            AtlasSheet byPath = PATH_SHEETS_MAP.get(pathToFile);
            if (byPath != null) {
                LABEL_SHEETS_MAP.put(textureName, byPath);
                return byPath;
            }
        }

        // 创建新 sheet
        AtlasSheet sheet = allocateSheet(pathToFile, 16);
        LABEL_SHEETS_MAP.put(textureName, sheet);
        if (pathToFile != null) PATH_SHEETS_MAP.put(pathToFile, sheet);
        // 同时注册 handle
        LABEL_HANDLES_MAP.put(textureName, new TextureHandle(textureName, sheet.baseTextureHandle));
        return sheet;
    }

    /**
     * Get a previously registered texture sheet by name.
     */
    public static AtlasSheet area(String textureName) {
        return LABEL_SHEETS_MAP.get(textureName);
    }

    /**
     * Shorthand for {@link #area(String)}.
     */
    public static AtlasSheet the(String textureName) {
        return area(textureName);
    }

    // 兼容旧 API：texture() 等同于 area()
    public static AtlasSheet texture(String textureName, String pathToFile) {
        return area(textureName, pathToFile);
    }
    public static AtlasSheet texture(String textureName) {
        return area(textureName);
    }

    /**
     * Get area by label or integer id.
     * Tries atlas label first, then global LABEL_ID_MAP.
     */
    public static AtlasArea getArea(String label) {
        for (AtlasSheet sheet : itemSheets) {
            AtlasArea a = sheet.getArea(label);
            if (a != null) return a;
        }
        Integer id = LABEL_ID_MAP.get(label);
        if (id != null) {
            for (AtlasSheet sheet : itemSheets) {
                if (id >= sheet.id_start && id < sheet.id_start + sheet.id_size) {
                    AtlasArea a = sheet.getArea(id);
                    if (a != null) return a;
                }
            }
        }
        return null;
    }

    public static AtlasArea getArea(int id) {
        for (AtlasSheet sheet : itemSheets) {
            if (id >= sheet.id_start && id < sheet.id_start + sheet.id_size) {
                return sheet.getArea(id);
            }
        }
        return null;
    }

    // 兼容旧 API：get() 等同于 getArea()
    public static AtlasArea get(String label) { return getArea(label); }
    public static AtlasArea get(int id) { return getArea(id); }

    /*
    ## dynamic(textureName, baseTextureLabel, pixmapPainter)
    - `baseTextureLabel` must be a sheet label registered via `area(label, path)` or `texture(label, path)`
    - This method only creates/registers the new texture label (no slicing helpers by design)
    */
    public static void dynamic(String textureName, String baseTextureLabel, java.util.function.Consumer<Pixmap> pixmapPainter) {
        if (textureName == null || baseTextureLabel == null) return;

        AtlasSheet baseSheet = area(baseTextureLabel);
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
            SmartTexture tx = TextureCache.create(key, dst.getWidth(), dst.getHeight());
            tx.bitmap(dst);

            area(textureName, key);
        } finally {
            if (dst != null) dst.dispose();
            if (src != null) src.dispose();
        }
    }

    /**
     * Get image id by label. Returns SOMETHING if not found.
     * @deprecated Use {@link #getArea(String)} instead for type-safe area access.
     */
    @Deprecated
    public static int byLabel(String name) {
        Integer res = LABEL_ID_MAP.get(name);
        if (res == null) {
            System.out.println("Invalid texture name: " + name);
            return ItemSpriteSheet.SOMETHING;
        }
        return res;
    }

    static AtlasSheet allocateSheet(String texture, int size) {
        AtlasSheet s = new AtlasSheet(texture, latestItemLocation, size);
        itemSheets.add(s);
        latestItemLocation += 256;
        return s;
    }

    // ==============================================================
    // 内置纹理注册
    // ==============================================================
    static {
        // UI 纹理：只注册 handle，不切片
        handle("ui.icons", Assets.Interfaces.ICONS);
        handle("ui.buffs_small", Assets.Interfaces.BUFFS_SMALL);
        handle("ui.buffs_large", Assets.Interfaces.BUFFS_LARGE);
        handle("ui.item_icons", Assets.Sprites.ITEM_ICONS);
        handle("ui.hero_icons", Assets.Interfaces.HERO_ICONS);

        // 示例：minecraft misc sheet，用隐式数字 label
        area("sheet.minecraft.misc", "minecraft/misc.png").grid(16, 16).areas();

        // 示例：单格 sheet，直接用 area() 分配命名区域
        area("sheet.sprites.gun", "sprites/gun.png").grid(16, 16).area("gun");
        area("sheet.effects.gunfire", "effects/gunfire.png").grid(16, 16).area("gunfire");
        area("sheet.cola.arksupply", "cola/arksupply.png").grid(64, 64).area("arksupply");
    }
}
