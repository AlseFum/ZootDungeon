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

---

## 全局骰点系统改造规划（Dice / D&D 风格骰点）

### 1. 总体目标

- **统一 RNG 表达方式**：将当前“`Random.Int(a, b)` / NormalIntRange 等 a-b 区间随机”的写法，统一抽象为 D&D 风格的骰点表达（如 `2d6+3`、`1d20+5` 等），并通过 `com.zootdungeon.utils.Dice` 作为唯一入口。
- **可读 / 可调**：伤害、命中、防御、暴击、掉落几率等核心公式用骰表达，方便策划/跑团玩家阅读和调数。
- **保持/可控的平衡**：在不破坏现有数值手感的前提下，尽量用等价或相近的骰表达替换旧随机逻辑（例如 NormalIntRange 用若干 dX 的和来近似）。
- **可选日志与 Debug**：在需要时输出骰表达与每次掷骰结果（`Dice.RollResult.describe()`），方便排查数值问题。

---

### 2. 现状梳理

- **Dice 实现现状（`com.zootdungeon.utils.Dice`）**
  - 支持由多个组件构成的骰表达：
    - `Die(amount, sides)`：代表 `amount d sides`，并支持：
      - `withMaxOf(k)`：只保留最大 k 个骰（`k` / keep highest）。
      - `withMinOf(k)`：只保留最小 k 个骰（`l` / keep lowest）。
      - `withExpandMorethan(threshold)`：大于等于某值时追加骰（类似 exploding dice）。
    - `Integer`：常数加减（支持负数）。
  - 掷骰接口：
    - `Dice.of(...)` 组合表达式；
    - `roll(Random)` 返回 `RollResult`，可 `getTotal()` 与 `describe()`。
  - 目前使用 `java.util.Random`，与游戏全局的 `com.watabou.utils.Random` / `Dungeon.seed` 分离（后续需统一 RNG 源）。

- **关键随机点（需要迁移到骰系统）**
  - **伤害系统**：
    - `Damage.physical(...)` 调用 `attacker.damageRoll()`，最终走到：
      - `KindOfWeapon.damageRoll(Char owner)`：Hero 用 `Hero.heroDamageIntRange(min, max)`，非 Hero 用 `Random.NormalIntRange(min, max)`。
      - 各 `Char` 子类（mob）覆写 `damageRoll()` / `drRoll()`。
  - **命中 / 闪避**：
    - `Char.hit(...)` 中通过 `Random.Float(acuStat)` / `Random.Float(defStat)` 做类对抗。
  - **防御 / DR 波动**：
    - `Char.drRoll()`、`Armor`/glyphs、Barkskin 等使用 `Random.NormalIntRange(...)`。
  - **掉落与随机事件**：
    - `Generator`、各类 `proc`（附魔/天赋）、陷阱/房间生成、食物效果（`FrozenCarpaccio` / `MysteryMeat`）等大量使用 `Random.Int(...)` / `Random.Float()`。
  - **音效 pitch / 视觉粒子抖动**：
    - 这些是“纯表现随机”，不需要用 D&D 式骰表达（继续保留简单 Random 即可，但也可以通过 Dice 做包装）。

---

### 3. 目标架构（骰点 API 设计）

- **Dice 作为“表达层 + 执行层”**
  - 表达层：
    - 提供从字符串解析的入口（便于在配置/调试中写 `"2d6+3k1e6-1"`）：
      - `static Dice parse(String expr)`，支持：
        - 基本：`NdM`、`+/-K`；
        - 修饰：`kX`（keep highest）、`lX`、`eX`（explode on >=X）。
  - 执行层：
    - 保留已有 `roll(Random)` 返回 `RollResult` 的接口；
    - 增加对游戏 RNG 的适配：
      - `int rollTotal()`：内部使用 `com.watabou.utils.Random`；
      - `RollResult rollWithDetails()`：同上，但保留明细。

- **常用辅助函数（便于在业务代码里直接用）**
  - 静态工具：
    - `int Dice.roll(String expr)` → `Dice.parse(expr).rollTotal()`；
    - `int Dice.uniform(int min, int max)` → 用等价骰表达实现，如 `1d(max-min+1) + (min-1)`；
    - `int Dice.normalLike(int min, int max)` → 用 `NdM` 的和近似 NormalIntRange（例如 `3dX` 或 `2dX+K`）。
  - 针对本项目语义的快捷封装（可选）：
    - `int DamageDice.weaponDamage(KindOfWeapon wep, Char owner)`；
    - `int DamageDice.drRoll(Char ch)`；  
    - `boolean DiceRolls.checkChance(float p)` → 映射到 `1d100` 或 `1d20` 之类的表达。

- **随机源统一**
  - 在 Dice 内部不再直接 new `java.util.Random`，而是：
    - 默认使用 `com.watabou.utils.Random`（其本身已与存档/种子系统集成）；
    - 仅在单元测试/示例中允许外部传入 `java.util.Random`。

---

### 4. 阶段实施计划

#### Phase 1：核心数值（伤害 / DR / 命中）

- [ ] 扩展 `Dice`：
  - [ ] 添加 `parse(String)`、`rollTotal()`、`rollWithDetails()`。
  - [ ] 添加 `uniform(int min, int max)` / `normalLike(int min, int max)` 等静态工具，并统一通过 `com.watabou.utils.Random`。
- [ ] 改造伤害主通路：
  - [ ] `KindOfWeapon.damageRoll(Char owner)`：
    - Hero：保留原 `Hero.heroDamageIntRange(min,max)` 语义，但改成通过 Dice 表达（例如 `Dice.normalLike(min, max)` / 显式 `NdM`）。
    - 非 Hero：`Random.NormalIntRange(min,max)` → `Dice.normalLike(min, max)`。
  - [ ] 各 `Char` 子类中自定义 `damageRoll()` / `drRoll()` 的地方，统一改成 Dice 写法：
    - 例如：`return Random.NormalIntRange(1, 8);` → `return Dice.normalLike(1, 8);` 或 `Dice.roll("2d4");`。
- [ ] 命中/闪避：
  - [ ] 在 `Char.hit(...)` 中，引入“命中骰 vs 闪避骰”的概念（可选）：
    - 方案 A：继续使用浮点比较，但内部用 Dice 生成 `acuRoll`、`defRoll`；
    - 方案 B（更 D&D）：`1d20 + attackBonus >= 1d20 + defenseBonus`，逐步迁移并微调数值。
  - [ ] 保持 PVP/PVE 命中率尽量接近当前实现。

#### Phase 2：掉落 / 触发几率 / 特效

- [ ] 在 `Generator` 中，将关键掉落逻辑改为骰表达：
  - 例如：`Random.Float() < p` → 基于 `1d100` 的比较（`Dice.roll("1d100") <= p*100`）。
- [ ] Enchant / 天赋 / Buff 触发几率统一用骰封装的工具方法：
  - 如 `Random.Int(20) < 1 + hero.pointsInTalent(...)` → `Dice.roll("1d20") <= 1 + ...`。
- [ ] 房间特性、陷阱生成等关卡逻辑中的分支概率统一改造为清晰的骰表达（便于今后挂在配置上）。

#### Phase 3：日志 / Debug 与可视化

- [ ] 为 `Damage` / `Char.hit` 等关键流程增加可选的 debug 开关：
  - 当开启（例如 dev console 或某个配置）时，输出：
    - 使用的骰表达（如 `"2d6+3"`）；
    - 本次掷骰结果 `RollResult.describe()`。
- [ ] 在 UI 或日志中为调试版增加“上一次伤害骰点详情”查看入口（可选）。

#### Phase 4：清理与文档

- [ ] 全局扫描 `Random.Int` / `Random.NormalIntRange` / `Random.Float`：
  - 对数值相关调用点（影响战斗/掉落的）逐个审查并改为 Dice；
  - 对纯视觉/音效随机保留现状，但可统一封装到 `VisualRandom` / `AudioRandom`（可选）。
- [ ] 增补文档：
  - `docs/dice-guide.md`：说明游戏内各主要数值的骰表达，方便后续调参和 Mod。

---

### 5. 预期示例（Target 使用方式示例）

这里先用伪代码说明我们打算让项目里“怎么看到骰点”的形式，等你确认后再按此风格落地实现。

#### 5.1 武器伤害（KindOfWeapon）

```java
// 例：短剑基础伤害 1d6+STR 修正
public int damageRoll(Char owner) {
    int baseMin = min(buffedLvl());
    int baseMax = max(buffedLvl());

    // 使用骰表达近似原来的范围（例如：1dN + K）
    return Dice.normalLike(baseMin, baseMax);
    // 或者显式表达（示意）：
    // return Dice.roll("1d6+2");
}
```

#### 5.2 角色 DR 掷骰（Char.drRoll）

```java
@Override
public int drRoll() {
    int bark = Barkskin.currentLevel(this);
    // 原本：Random.NormalIntRange(0, bark)
    return Dice.normalLike(0, bark);
    // 或者如果觉得更跑团味：return Dice.roll("1d" + bark);
}
```

#### 5.3 命中判定（Char.hit 的 D&D 化示意）

```java
public static boolean hit(Char attacker, Char defender, float accMulti, boolean magic) {
    int attackBonus = Math.round(attacker.attackSkill(defender) * accMulti);
    int defenseBonus = defender.defenseSkill(attacker);

    int attackRoll = Dice.roll("1d20+" + attackBonus);
    int defenseRoll = Dice.roll("1d20+" + defenseBonus);

    return attackRoll >= defenseRoll;
}
```

> 实际实现时会小心调整，使得在典型数值下命中率与现在接近，避免整体难度大幅跳变。

#### 5.4 掉落概率（Generator 中的概率事件）

```java
// 原：if (Random.Float() < ExoticCrystals.consumableExoticChance()) { ... }
float p = ExoticCrystals.consumableExoticChance(); // 例如 0.15
if (Dice.roll("1d100") <= Math.round(p * 100)) {
    // 触发掉落
}
```

#### 5.5 调试日志中的骰点说明

```java
Dice dice = Dice.parse("2d6+3");
Dice.RollResult res = dice.rollWithDetails();
System.out.println("Damage roll: " + dice.describe() + " -> " + res.describe());
// 示例输出：Damage roll: 2d6+3 -> [4, 2] + 3 = 9
```

---

### 6. 接下来要做的事（骰点相关）

1. 在你确认上面“骰点写法风格”（尤其是命中/伤害用 1d20 还是继续用 NormalIntRange 的近似骰）之后，完善 `Dice` 工具 API（`parse`/`normalLike` 等），并在一个小范围（例如某几把武器 + 少量怪物）试点替换。  
2. 检查测试/实战手感，对比旧版数据，微调骰表达后，再批量迁移关键数值点。  
3. 增加简单的 debug 输出/开关，方便你调试和验证跑团式骰点的行为。  

