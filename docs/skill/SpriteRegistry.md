# SpriteRegistry

纹理与区域注册中心，负责管理所有游戏纹理资源的加载、切片和命名。

## 核心概念

| 概念 | 说明 |
|------|------|
| `LazyAtlas` | 懒加载基类，持有纹理句柄并提供 `load()` 和 `afterLoad()` 生命周期。 |
| `TextureHandle` | 指向一张图片的句柄，不切片。用于只需要原始纹理的场景（如 MobSprite 配合 TextureFilm 使用）。 |
| `TextureArea` | 纹理中的一块区域数据：texture + uv rect + size。可直接通过 `toImage()` 生成 Image。 |
| `TextureHandler` | 继承 LazyAtlas，从一张图片切出多个区域（TextureArea），支持命名和按 label 寻址。 |

## 静态字段

```java
// 所有已分配的动态物品图集；用于把 image id 解析到具体 sheet
public static final ArrayList<TextureHandler> itemSheets = new ArrayList<>();

// label -> 全局 image id
public static final HashMap<String, Integer> LABEL_ID_MAP = new HashMap<>();

// 名称/label -> sheet（允许别名）
public static final HashMap<String, TextureHandler> LABEL_SHEETS_MAP = new HashMap<>();

// 名称/label -> handle（用于只注册纹理句柄的场景）
public static final HashMap<String, TextureHandle> LABEL_HANDLES_MAP = new HashMap<>();

// 纹理 path/key -> sheet（按来源去重复用）
public static final HashMap<String, TextureHandler> PATH_SHEETS_MAP = new HashMap<>();

// 最新分配的 item id 位置，用于分配新的 item id
public static int latestItemLocation = 1024;
```

## 静态方法

### texture(name)

```java
// 获取已注册的 sheet
TextureHandler sheet = SpriteRegistry.texture("ui.item_icons");
```

### texture(name, pathToFile)

```java
// 注册一个 sheet
SpriteRegistry.texture("hero.ReservedOp", "cola/guard.png");

// 获取已注册的句柄
TextureHandle h = SpriteRegistry.the("hero.ReservedOp");
Object rawHandle = h.textureHandle();
```

等价旧 API：`SpriteRegistry.texture(name, path)`

### idByLabel(name)

```java
// 获取全局 image id
int id = SpriteRegistry.idByLabel("cell_0");
image = new ItemSprite(id);
```

## TextureHandler 实例方法

### next(label)

按默认格子大小（构造时指定）分配下一个区域并挂上 label。

```java
SpriteRegistry.texture("sheet.items", "items.png")
    .next("item_0")
    .next("item_1")
    .next("item_2");
```

### setArea(label, x, y, width, height)

定义像素坐标区域并挂上 label。

```java
SpriteRegistry.texture("sheet.items", "items.png")
    .setArea("potion", 0, 0, 16, 16)
    .setArea("scroll", 16, 0, 16, 16);
```

### span(count)

预留格子（不分配 label）。

```java
SpriteRegistry.texture("sheet.items", "items.png")
    .next("item_0")
    .span(5)  // 跳过 5 个格子
    .next("item_6");  // 实际分配 item_6
```

### grid(cellW, cellH)

开始网格模式，返回 GridBuilder。

```java
SpriteRegistry.texture("sheet.arksupply", "cola/arksupply.png")
    .grid(64, 64)
    .area("cell_0")
    .area("cell_1")
    .span(2)
    .setXY("cell_4", 0, 1)
    .done();
```

## GridBuilder

网格切片构建器。

### area(label)

为当前 grid 位置分配一个区域并挂上 label，推进指针。

```java
.grid(32, 32)
    .area("frame_0")
    .area("frame_1")
    .area("frame_2");
```

### label(label)

已废弃，同 `area(label)`。

### span(count)

跳过 n 个格子。

```java
.grid(16, 16)
    .area("first")
    .span(10)    // 跳过 10 个格子
    .area("twelfth");
```

### setXY(label, gridX, gridY)

按 grid 坐标定义区域并挂上 label。

```java
.grid(32, 32)
    .setXY("top_left", 0, 0)
    .setXY("bottom_right", 1, 1);
```

### done()

结束 grid 模式，返回 TextureHandler。

## TextureArea

纹理中的一块区域数据。

```java
public record TextureArea(SmartTexture texture, RectF rect, float height, int size) {
    // 转为 Image
    public Image toImage() { ... }
}
```

## 内部机制

### 懒加载

```java
public final void load() {
    // 只有 cache 为空、bitmap 为空或 handle 变化时才重新加载
    if (cache == null || cache.bitmap == null || !Objects.equals(resolvedTextureHandle, handle)) {
        cache = TextureCache.get(handle);
        atlas = new Atlas(cache);
        resolvedTextureHandle = handle;
        afterLoad();  // 钩子方法
    }
}
```

### TextureCache.clear() 后重建

`TextureHandler` 持有 `areaPixels` 列表保存像素坐标 `[x, y, width, height]`。当 `TextureCache.clear()` 后，再次调用 `load()` 时：

```java
@Override
public void afterLoad() {
    // 用新的 cache 重建 areas
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
```

### 全局映射

- `LABEL_ID_MAP`：label → 全局 image id（用于 ItemSprite 寻址）
- `LABEL_SHEETS_MAP`：label → 所属 sheet（用于按 label 找 sheet）
- `LABEL_HANDLES_MAP`：label → TextureHandle（用于句柄查找）
- `itemSheets`：所有 TextureHandler 列表（用于按 id 找 sheet）
- `PATH_SHEETS_MAP`：纹理路径 → sheet（按来源去重）

## 典型使用场景

### 场景 1：按格子注册（最常见）

```java
// 注册阶段（在类 static 块或初始化）
SpriteRegistry.texture("sheet.cola.my_item", "cola/my_item.png")
    .grid(32, 32)
    .area("my_item_image");

// 使用阶段
int id = SpriteRegistry.idByLabel("my_item_image");
ItemSprite sprite = new ItemSprite(id);
```

### 场景 2：只注册句柄（用于 TextureFilm）

```java
// 注册阶段
SpriteRegistry.texture("mod:rat", "cola/rat.png");

// MobSprite 中使用
TextureFilm frames = textureWithFallback("mod:rat", Assets.Sprites.RAT, 16, 15);
```

### 场景 3：指定像素区域

```java
SpriteRegistry.texture("sheet.tools", "tools.png")
    .setArea("hammer", 0, 0, 32, 32)
    .setArea("sword", 32, 0, 32, 16);  // 非正方形
```

## 注意事项

- `grid()` 只设置格子尺寸，**不分配任何 label**，需要配合 `area()` 系列方法使用
- `area()` 同时推进指针，指向下一个未分配的格子
- `TextureHandler` 会自动从 `latestItemLocation` 分配 id，每分配一次增加 256
- 进入新场景时如果调用了 `TextureCache.clear()`，下次访问会自动重建 areas
