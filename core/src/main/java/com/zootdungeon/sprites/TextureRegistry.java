package com.zootdungeon.sprites;

import com.zootdungeon.Assets;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.gltextures.Atlas;
import com.watabou.utils.RectF;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * @see docs/skill/ TextureRegistry.md
 * @see TextureHandler
 * @see TextureArea
 * @see TextureRegistry.TextureHandle
 */
public final class TextureRegistry {

    private TextureRegistry() {}

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
    public static final ArrayList<TextureHandler> itemSheets = new ArrayList<>();
    /** label -> 全局 image id。 */
    public static final HashMap<String, Integer> LABEL_ID_MAP = new HashMap<>();
    /** 名称/label -> sheet（允许别名）。 */
    public static final HashMap<String, TextureHandler> LABEL_SHEETS_MAP = new HashMap<>();
    /** 名称/label -> handle（用于只注册纹理句柄的场景）。 */
    public static final HashMap<String, TextureHandle> LABEL_HANDLES_MAP = new HashMap<>();
    /** 纹理 path/key -> sheet（按来源去重复用）。 */
    public static final HashMap<String, TextureHandler> PATH_SHEETS_MAP = new HashMap<>();
    /** 最新分配的 item id 位置，用于分配新的 item id。 */
    public static int latestItemLocation = 1024;

    // ==============================================================
    // TextureHandle: 指向一张图片的句柄，不切片
    // ==============================================================

    /** 只持有纹理路径，用于只需要原始纹理的场景（如 MobSprite）。 */
    public record TextureHandle(String name, Object baseHandle) {}

    // ==============================================================
    // TextureArea: 从纹理中划分出来的一块区域（单格数据容器）
    // ==============================================================

    /** 纹理中的一块区域数据：texture + uv rect + size。 */
    public record TextureArea(SmartTexture texture, RectF rect, float height, int size) {
        public TextureArea(SmartTexture texture, RectF rect, float height) {
            this(texture, rect, height, 16);
        }

        public com.watabou.noosa.Image toImage() {
            com.watabou.noosa.Image img = new com.watabou.noosa.Image(texture);
            img.frame(rect);
            return img;
        }
    }

    // ==============================================================
    // TextureHandler: 加载一张图片并按 grid/row 切出多个区域
    // ==============================================================

    public static class TextureHandler extends LazyAtlas {
        public final int id_start;
        int id_size;
        public int size;

        // 直接存储 TextureArea 列表，不再使用 atlas
        private final ArrayList<TextureArea> areas = new ArrayList<>();
        // 保存像素坐标，用于 TextureCache.clear() 后重建 areas
        private final ArrayList<int[]> areaPixels = new ArrayList<>(); // [x, y, width, height]

        TextureHandler(String texture, int id_start, int size) {
            super(texture);
            this.id_start = id_start;
            this.id_size = 0;
            this.size = size;
            load();
        }

        @Override
        public void afterLoad() {
            // TextureCache.clear() 后，用新的 cache 重建 areas
            areas.clear();
            for (int[] px : areaPixels) {
                int x = px[0], y = px[1], w = px[2], h = px[3];
                RectF rect = new RectF(
                        x / (float) cache.width,
                        y / (float) cache.height,
                        (x + w) / (float) cache.width,
                        (y + h) / (float) cache.height
                );
                areas.add(new TextureArea(cache, rect, h, Math.max(w, h)));
            }
        }

        /** Returns the registered texture handle/path (unresolved). */
        public Object textureHandle() {
            return baseTextureHandle;
        }

        public TextureArea getArea(int id) {
            load();
            int where = id >= id_start ? id - id_start : id;
            if (where >= 0 && where < areas.size()) {
                return areas.get(where);
            }
            return null;
        }



        /**
         * 为当前指针位置分配一个区域并挂上 label。
         */
        public TextureHandler next(String label) {
            load();

            int col = id_size % (int)(cache.width / size);
            int row = id_size / (int)(cache.width / size);
            int x = col * size;
            int y = row * size;
            nameArea(label, x, y, size, size);
            return this;
        }

        /** 定义像素坐标区域并挂上 label。 */
        public TextureHandler setArea(String label, int x, int y, int width, int height) {
            load();
            nameArea(label, x, y, width, height);
            return this;
        }

        /** Reserve slots in current sheet. */
        public TextureHandler span(int size) {
            id_size += size;
            return this;
        }

        /** 添加一个区域到当前指针位置并挂上 label。 */
        private TextureArea nameArea(String label, int x, int y, int width, int height) {
            // 保存像素坐标用于 afterLoad 重建
            areaPixels.add(new int[]{x, y, width, height});

            RectF rect = new RectF(
                    x / (float) cache.width,
                    y / (float) cache.height,
                    (x + width) / (float) cache.width,
                    (y + height) / (float) cache.height
            );

            TextureArea area = new TextureArea(cache, rect, height, Math.max(width, height));
            while (areas.size() <= id_size) areas.add(null);
            areas.set(id_size, area);

            if (label != null) {
                LABEL_ID_MAP.put(label, id_start + id_size);
                LABEL_SHEETS_MAP.put(label, this);
            }

            id_size++;
            return area;
        }
        // row builder
        public RowBuilder row(int x, int y, int width, int height) {
            return new RowBuilder(this, x, y, width, height, false, 0, 0);
        }

        public static final class RowBuilder {
            private final TextureHandler sheet;
            private final int startX, startY, w, h;
            private final boolean wrap;
            private final int rows, cols;

            RowBuilder(TextureHandler sheet, int x, int y, int w, int h, boolean wrap, int rows, int cols) {
                this.sheet = sheet;
                this.startX = x;
                this.startY = y;
                this.w = w;
                this.h = h;
                this.wrap = wrap;
                this.rows = rows;
                this.cols = cols;
            }
        }

        /**
         * 开始一个 grid 模式，返回 GridBuilder。
         */
        public GridBuilder grid(int cellW, int cellH) {
            return new GridBuilder(this, cellW, cellH);
        }

        /**
         * 网格切片构建器。
         */
        public class GridBuilder {
            private final TextureHandler handler;
            private final int cellW, cellH;
            private int index = 0;

            GridBuilder(TextureHandler handler, int cellW, int cellH) {
                this.handler = handler;
                this.cellW = cellW;
                this.cellH = cellH;
            }

            /** 为当前 grid 位置分配一个区域并挂上 label。 */
            public GridBuilder area(String label) {
                int col = index % (int)(cache.width / cellW);
                int row = index / (int)(cache.width / cellW);
                int x = col * cellW;
                int y = row * cellH;
                handler.nameArea(label, x, y, cellW, cellH);
                index++;
                return this;
            }

            /** @deprecated Use {@link #area(String)} */
            @Deprecated
            public GridBuilder label(String label) {
                return area(label);
            }

            /** 跳过 n 个格子。 */
            public GridBuilder span(int count) {
                index += count;
                return this;
            }

            /** 按 grid 坐标定义区域并挂上 label。 */
            public GridBuilder setXY(String label, int gridX, int gridY) {
                handler.nameArea(label, gridX * cellW, gridY * cellH, cellW, cellH);
                return this;
            }

            /** 结束 grid 模式，返回 TextureHandler。 */
            public TextureHandler done() {
                return handler;
            }
        }
    }

    // ==============================================================
    // 静态方法
    // ==============================================================
    /**
     * Get a previously registered texture sheet by name.
     */
    public static TextureHandler texture(String textureName) {
        return LABEL_SHEETS_MAP.get(textureName);
    }

    /**
     * Shorthand for {@link #texture(String)}.
     */
    public static TextureHandler the(String textureName) {
        return texture(textureName);
    }

    /**
     * A quick function to register single item texture
     */
    public static int once(String areaName, String texturePath, int x, int y, int w, int h) {
        // If area already registered, return existing id
        Integer existing = LABEL_ID_MAP.get(areaName);
        if (existing != null) {
            return existing;
        }

        // Create new sheet with the texture and define the area
        TextureHandler sheet = texture(areaName, texturePath);
        if (sheet != null) {
            sheet.setArea(areaName, x, y, w, h);
        }
        return idByLabel(areaName);
    }

    public static TextureHandler texture(String textureName, String pathToFile) {
        if (textureName == null) return null;

        // 已存在 sheet，直接 alias 并返回
        TextureHandler existing = LABEL_SHEETS_MAP.get(textureName);
        if (existing != null) {
            if (pathToFile != null) {
                TextureHandler byPath = PATH_SHEETS_MAP.get(pathToFile);
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
            TextureHandler byPath = PATH_SHEETS_MAP.get(pathToFile);
            if (byPath != null) {
                LABEL_SHEETS_MAP.put(textureName, byPath);
                return byPath;
            }
        }

        // 创建新 sheet
        TextureHandler sheet = allocateSheet(pathToFile, 16);
        LABEL_SHEETS_MAP.put(textureName, sheet);
        if (pathToFile != null) PATH_SHEETS_MAP.put(pathToFile, sheet);
        // 同时注册 handle
        LABEL_HANDLES_MAP.put(textureName, new TextureHandle(textureName, sheet.baseTextureHandle));
        return sheet;
    }

    /**
     * Get image id by label. Returns SOMETHING if not found.
     */
    public static int idByLabel(String name) {
        Integer res = LABEL_ID_MAP.get(name);
        if (res == null) {
            System.out.println("Invalid texture name: " + name);
            return ItemSpriteSheet.SOMETHING;
        }
        return res;
    }

    static TextureHandler allocateSheet(String texture, int size) {
        TextureHandler s = new TextureHandler(texture, latestItemLocation, size);
        itemSheets.add(s);
        latestItemLocation += 256;
        return s;
    }
}
