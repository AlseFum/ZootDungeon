# 地形贴图与 Terrain 对应说明

本文档描述 ColaDungeon（Shattered Pixel Dungeon 系）中 **主地形图集**（`Dungeon.level.tilesTex()`）与 **`terrain_features`**（`Assets.Environment.TERRAIN_FEATURES`）在代码里的用法。  
贴图在运行时被切成等大的格子（`TextureFilm`）；逻辑格世界尺寸见 `DungeonTilemap.SIZE`；若使用 `TEXTURE_TILE != SIZE`，仅改变单格像素尺寸，**帧下标与下述一致**。

**源码依据**：`com.zootdungeon.tiles.DungeonTileSheet`、`DungeonTerrainTilemap`、`DungeonWallsTilemap`、`RaisedTerrainTilemap`、`TerrainFeaturesTilemap`、`SpriteRegistry.applyTilesetToFilm`（自定义 tileset 可覆盖 UV，不改变逻辑下标）。

---

## 1. 坐标与帧下标

`DungeonTileSheet.xy(x, y)` 使用 **1-based** 的图集格坐标，图集 **宽 16 列**（`WIDTH = 16`）：

```text
帧下标 index = (x - 1) + 16 × (y - 1)
```

- **x**：从左到右 1…16  
- **y**：从上到下 1…  

同一帧可能对应多种 **Terrain**（拼接/变体），或 **Terrain** 经 `getVisualWithAlts` 在相邻帧间切换。

---

## 2. 主地形图集 `tilesTex()`

由 `Dungeon.level.tilesTex()` 指定；`DungeonTerrainTilemap`、`DungeonWallsTilemap`、`RaisedTerrainTilemap` 共用此纹理（各自 `getTileVisual` 不同）。

### 2.1 渲染分层（概念）

| 层 / Tilemap | 作用 |
|----------------|------|
| `DungeonTerrainTilemap` | 地面 direct、水/裂隙拼接；门与墙的 **抬高透视下层**；雕像/路障/草/矿等 **raised**；`flat` 模式下 `directFlatVisuals` |
| `DungeonWallsTilemap` | **墙体内角**、**挑檐**、侧向门、出口 underhang 等 |
| `RaisedTerrainTilemap` | 仅 **高草 / 犁沟草** 的 **underhang** 装饰层 |

### 2.2 图集区域总览（按 `DungeonTileSheet` 常量）

以下 **(x,y)** 均为 `xy()` 的 1-based 起点；**槽位**为连续下标数量或注释中的块大小。

| 起点 (x,y) | 常量锚点 | 用途摘要 |
|------------|-----------|----------|
| (1,1) | `GROUND` | 地板、草、余烬、特殊地板、变体；入口/出口/井/空井/基座；`ENTRANCE_SP` 等 |
| (1,17)–(8,17) | `THEME_FLOOR_1`…`8` | 自定义主题地板格（`THEME_TILE_*`） |
| (9,2) | `CHASM` | 裂隙及与上方地形拼接（8 槽） |
| (1,3) | `WATER` | 水 + 与陆地四邻拼接（16 槽） |
| (1,4) | `FLAT_WALLS` | 俯视墙、书架、门、出口（16 槽） |
| (1,5) | `FLAT_OTHER` | 俯视炼金锅、路障、草、雕像、矿晶/矿石等 |
| (1,6) | `RAISED_WALLS` | 透视下层墙/书架墙/门后墙 + 左右开口 + alt（32 槽） |
| (1,8) | `RAISED_DOORS` | 透视门、竖洞侧向地板（8 槽） |
| (9,8) | `RAISED_OTHER` | 透视下层物体（炼金、路障、草、雕像、矿等，24 槽） |
| (1,10) | `WALLS_INTERNAL` | 墙体内角/木书架角等（48 槽，邻格掩码） |
| (1,13) | `WALLS_OVERHANG` | 墙/门挑檐（32 槽，邻格掩码） |
| (1,15) | `DOOR_OVERHANG` | 门挑檐、侧向门、出口 underhang |
| (9,15) | `OTHER_OVERHANG` | 物体挑檐 + 高草/犁沟 **underhang** / **overhang** 等 |

### 2.3 地面块 `GROUND`（起点 (1,1)）

与 **地板类 Terrain** 直接对应（再经 `getVisualWithAlts` 可能切到 alt 槽）：

| 常量 | 相对 `GROUND` | 典型 Terrain / 说明 |
|------|----------------|---------------------|
| `FLOOR` | +0 | `EMPTY` 及多种陷阱/装饰共用视觉 |
| `FLOOR_DECO` | +1 | `EMPTY_DECO` |
| `GRASS` | +2 | `GRASS` |
| `EMBERS` | +3 | `EMBERS` |
| `FLOOR_SP` | +4 | `EMPTY_SP` |
| `FLOOR_ALT_1` | +6 | `FLOOR` 常见变体 |
| `FLOOR_DECO_ALT` | +7 | |
| `GRASS_ALT` | +8 | |
| `EMBERS_ALT` | +9 | |
| `FLOOR_SP_ALT` | +10 | |
| `FLOOR_ALT_2` | +12 | `FLOOR` 稀有变体（5%） |
| `ENTRANCE` | +16 | `ENTRANCE` |
| `EXIT` | +17 | `EXIT` |
| `WELL` | +18 | `WELL` |
| `EMPTY_WELL` | +19 | `EMPTY_WELL` |
| `PEDESTAL` | +20 | `PEDESTAL` |
| `ENTRANCE_SP` | +22 | `ENTRANCE_SP` |

**主题地板**（显式槽位，无回落）：

| 常量 | 图集格 (x,y) | Terrain |
|------|----------------|---------|
| `THEME_FLOOR_1` | (1,17) | `THEME_TILE_1` |
| `THEME_FLOOR_2` | (2,17) | （代码中 `THEME_TILE_2` 也指向 `THEME_FLOOR_1`，与 1 同槽） |
| `THEME_FLOOR_3` | (3,17) | `THEME_TILE_3` |
| `THEME_FLOOR_4` | (4,17) | `THEME_TILE_4` |
| `THEME_FLOOR_5` | (5,17) | `THEME_TILE_5` |
| `THEME_FLOOR_6` | (6,17) | `THEME_TILE_6` |
| `THEME_FLOOR_7` | (7,17) | `THEME_TILE_7` |
| `THEME_FLOOR_8` | (8,17) | `THEME_TILE_8` |

### 2.4 裂隙 `CHASM`（起点 (9,2)，8 槽）

- 基础：`CHASM`  
- `CHASM+1`…：与 **上方** 地形类型对应的拼接（`chasmStitcheable` 映射），如 `CHASM_FLOOR`、`CHASM_WALL`、`CHASM_WATER` 等。

### 2.5 水 `WATER`（起点 (1,3)，16 槽）

- 基础：`WATER`  
- 下标偏移为四邻 **可接陆地** 的位掩码（+1 上、+2 右、+4 下、+8 左），见 `stitchWaterTile`；可接集合为 `waterStitcheable`。

### 2.6 俯视墙/门 `FLAT_WALLS`（起点 (1,4)）

| 常量 | 偏移 | Terrain（`directFlatVisuals`） |
|------|------|--------------------------------|
| `FLAT_WALL` | +0 | `WALL`、`SECRET_DOOR` |
| `FLAT_WALL_DECO` | +1 | `WALL_DECO` |
| `FLAT_BOOKSHELF` | +2 | `BOOKSHELF` |
| `FLAT_WALL_ALT` | +4 | 墙变体 |
| `FLAT_WALL_DECO_ALT` | +5 | |
| `FLAT_BOOKSHELF_ALT` | +6 | |
| `FLAT_DOOR` | +8 | `DOOR` |
| `FLAT_DOOR_OPEN` | +9 | `OPEN_DOOR` |
| `FLAT_DOOR_LOCKED` | +10 | `LOCKED_DOOR` |
| `FLAT_DOOR_CRYSTAL` | +11 | `CRYSTAL_DOOR` |
| `UNLOCKED_EXIT` | +12 | `UNLOCKED_EXIT` |
| `LOCKED_EXIT` | +13 | `LOCKED_EXIT` |

### 2.7 俯视其它 `FLAT_OTHER`（起点 (1,5)）

| 常量 | Terrain |
|------|---------|
| `FLAT_ALCHEMY_POT` | `ALCHEMY` |
| `FLAT_BARRICADE` | `BARRICADE` |
| `FLAT_HIGH_GRASS` | `HIGH_GRASS` |
| `FLAT_FURROWED_GRASS` | `FURROWED_GRASS` |
| `FLAT_STATUE` / `FLAT_STATUE_SP` | `STATUE` / `STATUE_SP` |
| `FLAT_MINE_CRYSTAL` 等 | `MINE_CRYSTAL`（与 `MINE_BOULDER` 共用部分下标，见源码） |

**主题俯视**（`THEME_FLAT_*`）：与 `THEME_FLOOR_8` 起算的一组槽绑定，`THEME_TILE_3`…`8` 在 `directFlatVisuals` 中映射（`THEME_TILE_1/2` 未单独列在 `directFlatVisuals`，与主题地板逻辑一致见 `DungeonTileSheet`）。

### 2.8 透视下层墙 `RAISED_WALLS`（起点 (1,6)）

- 基底：`RAISED_WALL`、`RAISED_WALL_DECO`、`RAISED_WALL_DOOR`、`RAISED_WALL_BOOKSHELF` 及 **+16** 起的 alt。  
- 最终下标再按 **左/右非墙** 各 **+1 / +2**（`getRaisedWallTile`）。  
- 适用 Terrain：`WALL`、`WALL_DECO`、`SECRET_DOOR`、`BOOKSHELF`（及门下特殊情况）。

### 2.9 透视门 `RAISED_DOORS`（起点 (1,8)）

| 常量 | Terrain |
|------|---------|
| `RAISED_DOOR` | `DOOR` |
| `RAISED_DOOR_OPEN` | `OPEN_DOOR` |
| `RAISED_DOOR_LOCKED` | `LOCKED_DOOR` |
| `RAISED_DOOR_CRYSTAL` | `CRYSTAL_DOOR` |
| `RAISED_DOOR_SIDEWAYS` | 竖向门洞与下方为墙时的特殊格（`getRaisedDoorTile`） |

### 2.10 透视下层其它 `RAISED_OTHER`（起点 (9,8)）

含 `RAISED_ALCHEMY_POT`、`RAISED_BARRICADE`、`RAISED_HIGH_GRASS`、`RAISED_FURROWED_GRASS`、`RAISED_STATUE`、`RAISED_STATUE_SP`、`RAISED_MINE_*` 及 alt；由 `DungeonTerrainTilemap.getTileVisual` 在 **非 flat** 分支选用。

### 2.11 墙体内角 `WALLS_INTERNAL`（起点 (1,10)）

`stitchInternalWallTile`：按 **右 / 右下 / 下 / 左下 / 左** 是否为 `wallStitcheable` 加 **1、2、4、8**（具体见源码）；书架、矿层 `WALL_DECO` 等会选不同基底（`WALL_INTERNAL`、`WALL_INTERNAL_DECO`、`WALL_INTERNAL_WOODEN`）。

### 2.12 挑檐 `WALLS_OVERHANG`（起点 (1,13)）

`stitchWallOverhangTile`：门类型与下方 `BOOKSHELF`、`WALL_DECO`（矿层）等影响基底，再按 **右下/左下** 掩码 **+1/+2**。

### 2.13 门与出口挑檐 `DOOR_OVERHANG`（起点 (1,15)）

含 `DOOR_OVERHANG`、`DOOR_OVERHANG_OPEN`、`DOOR_OVERHANG_CRYSTAL`、`DOOR_SIDEWAYS*`、`EXIT_UNDERHANG` 等（`DungeonWallsTilemap`）。

### 2.14 其它挑檐 / underhang `OTHER_OVERHANG`（起点 (9,15)）

炼金锅、路障、草、雕像、矿的 **overhang**；以及 `HIGH_GRASS_UNDERHANG`、`FURROWED_UNDERHANG` 及 alt（供 `RaisedTerrainTilemap`）。

### 2.15 `directVisuals` 一览（无拼接的地面）

| Terrain | 视觉常量 |
|---------|-----------|
| `EMPTY` | `FLOOR` |
| `GRASS` | `GRASS` |
| `EMPTY_WELL` | `EMPTY_WELL` |
| `ENTRANCE` | `ENTRANCE` |
| `EXIT` | `EXIT` |
| `EMBERS` | `EMBERS` |
| `PEDESTAL` | `PEDESTAL` |
| `EMPTY_SP` | `FLOOR_SP` |
| `ENTRANCE_SP` | `ENTRANCE_SP` |
| `SECRET_TRAP`、`TRAP`、`INACTIVE_TRAP`、`CUSTOM_DECO`、`CUSTOM_DECO_EMPTY` | 与 `EMPTY` 同视觉（`FLOOR`） |
| `THEME_TILE_1`…`8` | `THEME_FLOOR_1`…`8`（1 与 2 同指向见上） |
| `EMPTY_DECO` | `FLOOR_DECO` |
| `LOCKED_EXIT` | `LOCKED_EXIT` |
| `UNLOCKED_EXIT` | `UNLOCKED_EXIT` |
| `WELL` | `WELL` |

`WATER`、`CHASM` 不使用 `directVisuals`。

---

## 3. `terrain_features`（`Assets.Environment.TERRAIN_FEATURES`）

由 `TerrainFeaturesTilemap` 使用；与 `tilesTex()` **不是同一张图**。  
帧下标同样按 **16 列** 网格理解：`index = column0Based + 16 * row0Based`（与 `Trap`/`Plant` 公式一致）。

### 3.1 陷阱

```text
帧下标 = trap.color + trap.shape × 16
```

- **`trap.color`**：`Trap.RED`(0) … `Trap.GREY`(7)；未激活等可用 `Trap.BLACK`(8)。  
- **`trap.shape`**：`DOTS`(0)、`WAVES`(1)、`GRILL`(2)、`STARS`(3)、`DIAMOND`(4)、`CROSSHAIR`(5)、`LARGE_DOT`(6)。  

即：**同一列（shape）占连续 9 行颜色**（0–8 行偏移），或理解为 **每行 16 格，shape 换行块**。

### 3.2 植物

```text
帧下标 = plant.image + 7 × 16
```

即从 **第 8 列（1-based x=8）** 起的植物条带；`plant.image` 为各类植物在子类中赋的值，例如：

| `image` | 植物类（示例） |
|---------|----------------|
| 0 | `Rotberry` |
| 1 | `Firebloom` |
| 2 | `Swiftthistle` |
| 3 | `Sungrass` |
| 4 | `Icecap` |
| 5 | `Stormvine` |
| 6 | `Sorrowmoss` |
| 7 | `Mageroyal` |
| 8 | `Earthroot` |
| 9 | `Starflower` |
| 10 | `Fadeleaf` |
| 11 | `Blindweed` |
| 12 | `BlandfruitBush` |

（以源码为准。）

### 3.3 草地 / 余烬（无陷阱无植物时，按 Terrain）

`stage` 由深度推算（`LastShopLevel` 等有特殊减段），且 `stage = min(stage, 4)`：

```text
stage = (Dungeon.depth - 1) / 5   // 再按关卡类型微调
```

| Terrain | 帧下标公式 |
|-----------|------------|
| `HIGH_GRASS` | `9 + 16×stage + (tileVariance[pos] ≥ 50 ? 1 : 0)` |
| `FURROWED_GRASS` | `11 + 16×stage + (同上)` |
| `GRASS` | `13 + 16×stage + (同上)` |
| `EMBERS` | `9 + 16×5 + (同上)`（固定第 6 段行块，约等于 stage 5） |

即：**列 9–14（0-based）附近**随 **区域 stage** 换行，**±1 列** 为方差变体。

---

## 4. 自定义 Tileset（`SpriteRegistry`）

若关卡提供 `tilemapKey()`，可对 **`tilesTex()` 对应 TextureFilm** 做 UV 重映射（`applyTilesetToFilm`）。  
逻辑帧下标仍为上文常量；**像素在大图中的位置** 以 tileset JSON 为准。

---

## 5. 相关文件

| 文件 | 说明 |
|------|------|
| `core/.../tiles/DungeonTileSheet.java` | 全部 `xy`、拼接与 `directVisuals` / `directFlatVisuals` / alt |
| `core/.../tiles/DungeonTerrainTilemap.java` | 主地形格视觉 |
| `core/.../tiles/DungeonWallsTilemap.java` | 墙与挑檐 |
| `core/.../tiles/RaisedTerrainTilemap.java` | 草 underhang |
| `core/.../tiles/TerrainFeaturesTilemap.java` | `terrain_features` |
| `core/.../sprites/SpriteRegistry.java` | 自定义 tileset |
| `core/.../levels/traps/Trap.java` | 陷阱 color/shape 常量 |
| `core/.../plants/*.java` | 各植物 `image` |
