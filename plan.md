## 全贴图 SpriteRegistry & 材质包改造规划

### 1. 总体目标

- **统一入口**：游戏中所有贴图访问（物品、怪物 Sprite、Buff 图标、UI 图标、地图 Tilemap 等）都通过 `SpriteRegistry` 或其子模块，而不是直接散落使用 `Assets.*` 字符串路径或自行构造 `TextureFilm`。
- **支持材质包/皮肤**：可以在配置或设置界面选择“材质包”（texture pack），不同材质包可以：
  - 覆盖物品图标大图、角色/怪物大图、Buff 图标大图等；
  - 覆盖地图 tileset（包括 `tiles_*.png` 与 JSON 布局）；
  - 为特定 key 注册额外的动态 sprite。
- **兼容旧逻辑**：不一次性大爆改，优先在关键通路（地图、Buff、主要怪物与界面）接入，再逐步替换剩余调用。

---

### 2. 现状梳理（基于当前代码与文档）

- **SpriteRegistry 已有能力**
  - 动态物品：
    - `registerItemTexture(path, size).label(key)`：注册动态物品贴图段；
    - `itemImageId(key, elseId)`：按 key 取整数 ID，失败回退静态 ID；
    - `itemMapping(key, elseId)` / `staticItemMapping(id)`：返回 `ImageMapping`（`SmartTexture + RectF`）。
  - 动态怪物：
    - `registerMob(key, new MobDef(texture, w, h))`；
    - `mobFilm(key, fallbackTex, w, h)`：按 key/回退构造 `TextureFilm`。
  - 地图/Tilemap：
    - `registerTilemap(key, TilemapDef)`，`tilemapTilesTextureOr(...)`，`tilemapWaterTextureOr(...)`；
    - `registerTilesetFromJson(key, texturePath, jsonPath)` + `applyTilesetToFilm(film, key, tileSize)`；文档与 `DungeonTilemap.applyTilesetOverridesForCurrentLevel()` 已就绪。
  - 动态生成：
    - `TextureBuilder` + `SpriteRegistry.ImageMapping`，用于示例和运行期生成贴图。
- **Assets 静态路径**
  - `Assets.Sprites`：所有职业、怪物、物品大图路径；
  - `Assets.Interfaces`：UI、Buff 图标大图：`BUFFS_SMALL` / `BUFFS_LARGE`、`ICONS` 等；
  - `Assets.Environment`：`TILES_*`、`WATER_*` 等地图图像。
- **文档**
  - `docs/sprite-guide.md`：已经描述了 item/mob/tilemap 如何通过 `SpriteRegistry` 使用；
  - `docs/sprite-system-todo.md`：当前主要关注 items & mobs 与 tilemap 的统一；Buff / UI 与材质包切换尚未系统化。

---

### 3. 目标架构（高层设计）

- **SpriteRegistry 扩展为多域注册中心**：
  - Item 域：现有实现继续使用；
  - Mob 域：继续基于 `MobDef`/`mobFilm`，补充更多调用点；
  - Tilemap/Tileset 域：复用现有 `TilemapDef` / `Tileset`；
  - **Buff/Icon 域（新增）**：
    - 按“类别 + key”统一管理（如 `buff:poison`, `ui:close`, `ui:inventory`）；
    - 支持静态 atlas（`interfaces/buffs.png`、`interfaces/icons.png`）与动态独立图混用；
    - 与 `StatusPane` / `Buff` 相关 UI 通过 key 或枚举访问。
- **材质包抽象**：
  - 定义 `TexturePack` 概念：一个材质包提供若干“覆盖声明”，包括：
    - 覆盖 `Assets.Sprites.*` / `Assets.Interfaces.*` / `Assets.Environment.*` 的路径；
    - 覆盖某些 tilemap/tile set 的 key → 新纹理与 JSON；
    - 额外注册动态 items/mobs/buffs 的 key。
  - 在 `SpriteRegistry` 中维护：
    - 当前激活材质包 `currentPackId`；
    - `resolvePath(canonicalPath)`：将“规范路径”（如 `sprites/rat.png`）映射到材质包实际路径（如 `minecraft/rat.png` 或自定义 pack 的路径）。
  - UI 层（如设置窗口）只需设置当前材质包 id，底层 Asset 加载与 SpriteRegistry 映射自动生效。

---

### 4. 阶段实施计划

#### Phase 1：打通 tilemap & tileset 覆盖（地图）

- [ ] 在 `DungeonTilemap` 的构造或初始化阶段调用 `applyTilesetOverridesForCurrentLevel()`，确保：
  - 若当前 `Level` 提供 `tilemapKey()`，则根据该 key 从 `SpriteRegistry` 获取 tileset，重写 Tilemap 的 `TextureFilm`；
  - 旧关卡无 key 时行为完全不变。
- [ ] 在各 `Level` 子类中：
  - 为试点关卡（如 `SewerLevel`/某个自定义关卡）添加 `tilemapKey` 字段与存取方法；
  - 使用 `SpriteRegistry.tilemapTilesTextureOr(...)` / `tilemapWaterTextureOr(...)` 替换直接返回的 `Assets.Environment.TILES_*` / `WATER_*`。
- [ ] 为至少一个自定义 tileset（例如文档中的 `cola:chel_sewer`）跑通 end-to-end 流程：
  - JSON + PNG + `registerTilesetFromJson`；
  - Level → tilemapKey → 实际渲染。

#### Phase 2：统一 Mob Sprite 获取路径，并为材质包预留挂点

- [ ] 检查所有 `MobSprite` 子类对 `TextureFilm`/`SmartTexture` 的构造：
  - 找出直接使用 `new TextureFilm(Assets.Sprites.X, ...)` 或 `TextureCache.get(Assets.Sprites.X)` 的位置；
  - 在基类或公共辅助方法中引入 `SpriteRegistry.mobFilm(key, fallbackTex, w, h)` 封装（部分代码已经存在 `textureWithFallback`，需全面使用）。
- [ ] 为几个试点怪物（如 `RatSprite`, `BatSprite`, `GnollSprite`, `GhoulSprite` 等）：
  - 添加动态 key（如 `"pack:rat"`, `"pack:bat"` 等）；
  - 默认不注册 key 时走 `Assets.Sprites.RAT` 等静态贴图；
  - 材质包激活时注册对应 key → 新的纹理路径。
- [ ] 在 `SpriteRegistry` 或材质包管理模块中：
  - 设计 Mob 贴图的注册表结构：`registerMobForPack(packId, mobKey, new MobDef(...))`；
  - 切换材质包时重建/刷新相关 mob 定义。

#### Phase 3：Buff & UI 图标接入 SpriteRegistry

- [ ] 梳理 Buff/UI 图标使用点：
  - `StatusPane`、`BuffIndicator`（或等价类）、若干 window/scene 中使用的图标；
  - 涉及 `Assets.Interfaces.BUFFS_SMALL` / `BUFFS_LARGE` / `ICONS` 等。
- [ ] 在 `SpriteRegistry` 中新增 Buff/Icon 模块：
  - 提供 `registerBuffAtlas(path, size)` / `label("buff:poison")` 等接口；
  - 提供 `buffIcon(String key, int fallbackIndex)` → 返回 `ImageMapping` 或整数 index；
  - 可选：对 UI 图标提供 `uiIcon(String key, int fallbackIndex)`。
- [ ] 改造 Buff 显示逻辑：
  - Buff 自身提供一个“逻辑 key”（如 `buff:poison`, `buff:haste`），而不是硬编码 index；
  - StatusPane/BuffIndicator 从 Buff 取 key，然后通过 `SpriteRegistry.buffIcon(key, fallbackIndex)` 取得贴图；
  - 旧逻辑仍可使用 index 作为 fallback。
- [ ] 为材质包提供 Buff/Icon 定义：
  - 默认材质包注册一套与当前 `interfaces/buffs.png` 一致的映射；
  - 替换材质包时重新注册映射到新的大图或独立图。

#### Phase 4：物品 & 角色 Sprite 与材质包联动

- [ ] 基于现有 `SpriteRegistry` 的 Item 动态注册机制，定义“材质包友好”的物品 key 命名约定（如 `item:shortsword`, `item:greatsword` 等）。
- [ ] 为主角/职业/宠物等角色 Sprite 设计 key：
  - 如 `hero:warrior`, `hero:mage`, `pet:default`，由 `SpriteRegistry` 或专门模块管理其大图路径与动画 `TextureFilm`。
- [ ] 在角色 Sprite 渲染中：
  - 引入 `SpriteRegistry` 的封装方法获取 `TextureFilm`（类似 Mob）；
  - 材质包可以覆盖角色/宠物的大图路径。
- [ ] 为至少一个“主题材质包”演示：
  - 地图 + 主要怪物 + 若干物品 + Buff/Icon 一起替换，验证路径解析与回退逻辑。

#### Phase 5：材质包系统（选择与切换）

- [ ] 定义材质包描述文件格式（建议 JSON），例如：
  - 路径：`assets/cola/packs/<packId>.json`；
  - 内容包含：
    - 元信息（名称、作者、版本）；
    - 覆盖的纹理路径映射（如 `sprites/rat.png` → `minecraft/rat.png`）；
    - tileset 注册（key → tiles PNG + JSON）；
    - item/mob/buff/ui 的动态 key → 纹理注册指令。
- [ ] 在启动流程中加载“当前材质包”：
  - 通过配置文件或存档中的设置字段（后续可接入设置界面）；
  - 解析 JSON，调用 `SpriteRegistry` 的各种 `register*` 接口完成注册；
  - 若加载失败则回退到默认材质包。
- [ ] 在设置界面（如 `WndSettings`）中增加材质包选择 UI：
  - 枚举可用 `packId`（扫描目录或通过一个注册表）；
  - 选择后在下次启动（或立即）生效，考虑是否支持“热切换”（高级目标）。

---

### 5. 兼容性与风险控制

- **渐进迁移**：优先在新增或可选路径上使用 `SpriteRegistry` 新接口，不一次性替换所有 `Assets.*` 调用。
- **强制 fallback**：所有动态查询 API 始终要求 fallback 参数，避免材质包缺贴图导致崩溃；
  - 对 Buff/Icon 也遵循“key + fallbackIndex”的模式。
- **诊断与错误贴图**：
  - 在 `SpriteRegistry` 中增加简单的日志与“错误贴图占位”（例如红框问号）：
    - 当 key 未命中时记录一次性警告；
    - 当 fallback 也失效时使用统一占位图，避免 NPE。
- **性能与内存**：
  - 保持与当前系统类似的纹理缓存策略（依然用 `TextureCache`）；
  - 在材质包切换或注册时注意不要反复加载/泄漏纹理。

---

### 6. 接下来要做的事（与本次改造直接相关）

1. **确认统一使用方式示例**（包含物品、怪物、Buff、地图四类的代码示例），与需求对齐后再开始真正代码改造。
2. 实施 Phase 1 & Phase 2 的核心改动（地图 tileset 覆盖 + MobSprite 接入 SpriteRegistry），并保证默认材质下行为不变。
3. 设计并实现 Buff/Icon 子模块与最小材质包 JSON 格式，完成一个“示例材质包”的从配置到渲染闭环。
4. 在此基础上再扩展到更多怪物/物品/职业，并逐步削减直接硬编码 `Assets.*` 路径的使用。


