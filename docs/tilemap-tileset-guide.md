## 地图贴图与 Tileset 内容包指南

本指南说明当前项目是如何将**tile 图像**和**tile sheet（布局描述）**配合使用的，以及内容包应该如何接入。

---

## 一、核心概念：Tileset

- **Tileset**：一套地图贴图定义，由三部分组成：
  - 一张 **tiles 图像**（例如 `cola/tiles_chel.png`）
  - 一份 **JSON sheet**（例如 `cola/tiles_chel.json`），描述每个逻辑 tile 在图中的位置
  - 一个 **key**（例如 `cola:chel_sewer`），用来在代码和关卡中引用这套贴图
- Tileset 在代码中的表现形式（简化）：

```java
public static final class Tileset {
    public final String key;        // "cola:chel_sewer"
    public final String texture;    // "cola/tiles_chel.png"
    public final int tileSize;      // 通常为 16
    public final String author;
    public final String version;

    // DungeonTileSheet 的逻辑 ID -> (x, y) 格子坐标（1 起始）
    public final Map<Integer, TileCoord> tiles;
}
```

---

## 二、Tileset JSON 格式

Tileset 的布局通过 JSON 文件描述，放在 `core/src/main/assets/` 目录下，例如 `core/src/main/assets/cola/tiles_chel.json`。

### 示例

```json
{
  "author": "Cola Dungeon",
  "version": "1.0.0",
  "pixelsize": 16,

  "FLOOR":      [1, 1],
  "FLOOR_DECO": [2, 1],
  "GRASS":      [3, 1],
  "EMBERS":     [4, 1],
  "FLOOR_SP":   [5, 1],

  "CHASM":        [9, 2],
  "CHASM_FLOOR":  [10, 2],
  "CHASM_WALL":   [12, 2],
  "CHASM_WATER":  [13, 2],

  "WATER": [1, 3],

  "FLAT_WALL":          [1, 4],
  "FLAT_WALL_DECO":     [2, 4],
  "FLAT_BOOKSHELF":     [3, 4],
  "FLAT_DOOR":          [9, 4],
  "FLAT_DOOR_OPEN":     [10, 4],
  "FLAT_DOOR_LOCKED":   [11, 4],
  "FLAT_DOOR_CRYSTAL":  [12, 4]
}
```

### 规则

- 顶层特殊字段：
  - **`author`**：作者名称（可选）
  - **`version`**：版本号（可选）
  - **`pixelsize`**：单个 tile 的像素大小（通常为 16）
- 其它字段：
  - **键名**：逻辑 tile 名，例如 `FLOOR`, `WATER`, `FLAT_WALL` 等
  - **值**：`[x, y]`，表示在 tiles 图像上的格子坐标（从 1 开始）
  - 这些逻辑名必须对应到 `DungeonTileSheet` 中的 `public static final int` 常量，例如 `"FLOOR"` → `DungeonTileSheet.FLOOR`。

---

## 三、SpriteRegistry 中的 Tileset 管理

Tileset 由 `SpriteRegistry` 统一管理。

### 注册 Tileset

在 `SpriteRegistry` 的静态初始化块中调用：

```java
// 注册纹理路径（旧的 tilemap API，仍然保留作兼容）
SpriteRegistry.registerTilemap(
    "cola:chel_sewer",
    new SpriteRegistry.TilemapDef("cola/tiles_chel.png", Assets.Environment.WATER_SEWERS)
);

// 注册 Tileset（推荐，新系统）
SpriteRegistry.registerTilesetFromJson(
    "cola:chel_sewer",
    "cola/tiles_chel.png",
    "cola/tiles_chel.json"
);
```

### 查询 Tileset

```java
SpriteRegistry.Tileset ts = SpriteRegistry.getTileset("cola:chel_sewer");
if (ts != null) {
    String texturePath = ts.texture;
    int size = ts.tileSize;
    SpriteRegistry.TileCoord floor = ts.tiles.get(DungeonTileSheet.FLOOR);
}
```

`registerTilesetFromJson` 会自动：
- 读取 JSON
- 解析 `author` / `version` / `pixelsize`
- 遍历所有 tile 名 → `[x,y]`
- 通过反射查找 `DungeonTileSheet.<NAME>`，将逻辑 ID 映射到坐标

---

## 四、DungeonTerrainTilemap 中如何使用 Tileset

`DungeonTerrainTilemap` 负责根据 `Terrain` 和 `DungeonTileSheet` 生成实际渲染的贴图索引。

构造函数中现在会尝试应用当前关卡的 Tileset：

```java
public DungeonTerrainTilemap() {
    super(Dungeon.level.tilesTex());

    // 如果当前关卡设置了 tilemapKey，并且注册了对应的 Tileset，
    // 则用 Tileset 的 JSON 布局覆盖 TextureFilm 中的帧。
    applyTilesetOverrides();

    map(Dungeon.level.map, Dungeon.level.width());
    instance = this;
}
```

`applyTilesetOverrides()` 的逻辑简要：

1. 读取当前关卡：`Level level = Dungeon.level;`
2. 获取关卡的 tilemapKey：`String key = level.tilemapKey();`
3. 从 `SpriteRegistry` 中取出 Tileset：`Tileset ts = SpriteRegistry.getTileset(key);`
4. 遍历 `ts.tiles`：
   - 每条记录是 `tileId`（`DungeonTileSheet` 的逻辑 ID） → `(x,y)` 坐标
   - 计算像素坐标：
     \[
     left = (x-1) \times SIZE,\ top = (y-1) \times SIZE
     \]
   - 调用 `tileset.add(tileId, left, top, right, bottom)` 覆盖 `TextureFilm` 中对应帧


