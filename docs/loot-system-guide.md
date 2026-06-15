## 掉落系统指南（LootRegistry + 兼容旧逻辑）

本文件说明当前版本的掉落系统实现方式，以及如何在**编译期**与**运行时**扩展新的掉落表。

- 原有体系：
  - 怪物上的 `loot` / `lootChance` 字段；
  - `Generator` 中的分类表（`Generator.Category`）；
  - 各种场景/房间中直接调用 `Generator.random(...)` 和 `Dungeon.level.drop(...)`。
- 新增体系：
  - `LootRegistry`：可注册的 loot 表管理器；
  - `Mob.lootTableId` + 兼容逻辑；
  - 示例：给普通老鼠增加一个“药水/卷轴”掉落表。

---

### 1. 原有掉落逻辑回顾

#### 1.1 `Generator`：全局物品生成器

`com.zootdungeon.items.Generator` 定义了大量物品类别：

```java
public enum Category {
    WEAPON  (2, 2, MeleeWeapon.class, null),
    ARMOR   (2, 1, Armor.class, null),
    MISSILE (1, 2, MissileWeapon.class, null),
    WAND    (1, 1, Wand.class, null),
    RING    (1, 0, Ring.class, null),
    ARTIFACT(0, 1, Artifact.class, null),
    FOOD    (0, 0, Food.class, null),
    POTION  (8, 8, Potion.class, null),
    // ...
}
```

- 每个 `Category` 维护 `classes[]` 与 `probs[]`，有较复杂的“牌堆式”概率控制（`defaultProbs`、`dropped`、`seed` 等）。
- 调用方式：
  - `Generator.random(Category)` / `randomUsingDefaults(Category)` → 按权重从该类别中抽一个实现类，并创建对应 `Item`；
  - `Generator.random(Class<? extends Item>)` → 在某个类族中随机一个实现类。

许多关卡脚本、道具效果用法形如：

```java
Item it = Generator.randomUsingDefaults(Generator.Category.POTION);
Dungeon.level.drop(it, cell).sprite.drop();
```

#### 1.2 `Mob`：怪物的基本掉落

在 `com.zootdungeon.actors.mobs.Mob` 中：

- 字段：

```java
protected Object loot = null;     // Category / Class / Item
protected float lootChance = 0;   // 基础掉落几率
```

- 每个 `Mob` 子类可在初始化块中设定：

```java
{
    loot = Generator.Category.WEAPON;
    lootChance = 0.2f;
}
```

或：

```java
{
    loot = PotionOfHealing.class;
    lootChance = 0.5f;
}
```

- 掉落流程：
  - `die()` 中调用 `rollToDropLoot()`；
  - `lootChance()` 把基础概率乘上各种修正：
    - 财富戒指 `RingOfWealth`；
    - 天赋 `Bounty Hunter` + `Preparation`；
    - `ShardOfOblivion` 等；
  - 若 `Random.Float() < lootChance()` 则调用 `createLoot()` → 生成 `Item`，再用 `Dungeon.level.drop(...)` 扔到地上。

`createLoot()` 的原始实现：

```java
@SuppressWarnings("unchecked")
public Item createLoot() {
    Item item;
    if (loot instanceof Generator.Category) {
        item = Generator.randomUsingDefaults((Generator.Category) loot);
    } else if (loot instanceof Class<?>) {

        if (ExoticPotion.regToExo.containsKey(loot)){
            if (Random.Float() < ExoticCrystals.consumableExoticChance()){
                return Generator.random(ExoticPotion.regToExo.get(loot));
            }
        } else if (ExoticScroll.regToExo.containsKey(loot)){
            if (Random.Float() < ExoticCrystals.consumableExoticChance()){
                return Generator.random(ExoticScroll.regToExo.get(loot));
            }
        }

        item = Generator.random((Class<? extends Item>) loot);

    } else {
        item = (Item) loot;
    }
    return item;
}
```

---

### 2. 新增：`LootRegistry` 与 lootTableId

#### 2.1 `LootRegistry` 概览

新文件：`com.zootdungeon.items.LootRegistry`。

职责：

- 按字符串 ID 管理若干 `LootTable`；
- 提供简易的 `LootContext`（目前主要为 `Mob` 击杀场景）；
- 提供默认的简单 `LootEntry` 实现。

核心 API：

```java
public final class LootRegistry {
    private static final Map<String, LootTable> TABLES = new HashMap<>();

    public static void register(String id, LootTable table);
    public static LootTable get(String id);

    public static List<Item> roll(String id, LootContext ctx);
    public static Item rollOne(String id, LootContext ctx);
}
```

#### 2.2 `LootContext`：掉落上下文

当前实现只包含最基础的字段：

```java
public static final class LootContext {
    public enum Source {
        MOB_KILL,
        ROOM_REWARD,
        CHEST_OPEN,
        OTHER
    }

    public final Mob mob;
    public final Char killer;
    public final int depth;
    public final int pos;
    public final Source source;

    public static LootContext forMobKill(Mob mob, Char killer, int pos) {
        int depth = Dungeon.depth;
        return new LootContext(mob, killer, depth, pos, Source.MOB_KILL);
    }
}
```

> 未来可以扩展更多字段（关卡标签、房间类型、Modifier 等）以支持更复杂规则。

#### 2.3 `LootTable` 与 `LootEntry`

```java
public interface LootEntry {
    boolean matches(LootContext ctx);
    int weight();
    List<Item> generate(LootContext ctx);
}

public static class LootTable {
    private final List<LootEntry> entries = new ArrayList<>();
    private int rolls = 1;

    public LootTable add(LootEntry e);
    public LootTable rolls(int rolls);
    public List<Item> roll(LootContext ctx);
}
```

- `rolls`：该表在一次 roll 中抽取几次 entry（每次独立按权重选一个 entry 并生成掉落）。
- 内部按 `weight()` 和 `matches(ctx)` 做一次加权随机选择。

内置简易 entry：

```java
public static LootEntry forItemSupplier(int weight, Supplier<Item> supplier);
public static LootEntry forItemClass(int weight, Class<? extends Item> cls);
```

用法示例：

```java
LootTable table = new LootTable()
    .add(LootRegistry.forItemClass(1, PotionOfHealing.class))
    .add(LootRegistry.forItemClass(2, Gold.class))
    .rolls(1);

LootRegistry.register("mob:example", table);
```

#### 2.4 `Mob` 中的兼容改造

在 `Mob` 中加入：

```java
protected Object loot = null;
protected float lootChance = 0;

// 可选：使用 LootRegistry 的表
protected String lootTableId = null;
```

并修改 `createLoot()`：

```java
@SuppressWarnings("unchecked")
public Item createLoot() {

    // 新系统：优先使用 LootRegistry 表
    if (lootTableId != null){
        LootRegistry.LootContext ctx =
            LootRegistry.LootContext.forMobKill(this, Dungeon.hero, pos);
        Item fromTable = LootRegistry.rollOne(lootTableId, ctx);
        if (fromTable != null){
            return fromTable;
        }
    }

    // 兼容旧逻辑：沿用 loot 字段与 Generator 的行为
    Item item;
    if (loot instanceof Generator.Category) {
        item = Generator.randomUsingDefaults((Generator.Category) loot);
    } else if (loot instanceof Class<?>) {
        // ExoticPotion / ExoticScroll 升级判定保持不变
        item = Generator.random((Class<? extends Item>) loot);
    } else {
        item = (Item) loot;
    }
    return item;
}
```

> 若 `lootTableId` 为空或对应表未注册/返回 null，则自动回退到原有 loot 字段逻辑。

---

### 3. 示例：给老鼠（Rat）加一套 LootRegistry 掉落表

`com.zootdungeon.actors.mobs.Rat` 现在增加了一个静态表注册与绑定：

```java
public class Rat extends Mob {

    static {
        // 额外的 Rat 掉落表：力量药水 / 升级卷轴 / 治疗药水
        LootRegistry.LootTable table = new LootRegistry.LootTable()
                // 力量药水
                .add(LootRegistry.forItemClass(1, PotionOfStrength.class))
                // 升级卷轴
                .add(LootRegistry.forItemClass(1, ScrollOfUpgrade.class))
                // 治疗药水（权重 2）
                .add(LootRegistry.forItemClass(2, PotionOfHealing.class));
        LootRegistry.register("mob:rat:basic_loot", table);
    }

    {
        spriteClass = RatSprite.class;

        HP = HT = 8;
        defenseSkill = 2;
        maxLvl = 5;

        // 使用 LootRegistry 的 Rat 掉落表
        lootTableId = "mob:rat:basic_loot";
        lootChance = 1f;   // 你当前项目中将其调整为 1f（100% 掉落）
    }
}
```

解释：

- Rat 使用 `lootTableId = "mob:rat:basic_loot"`，所以 `createLoot()` 会先尝试从该表生成掉落；
- 表中三条 entry 的权重分别为 1 / 1 / 2 → 在一次 roll 中：
  - 力量药水：25%；
  - 升级卷轴：25%；
  - 治疗药水：50%；
- `lootChance` 当前项目中设为 `1f`（100%），因此每只老鼠必定掉一个上述三者之一（仍然会再叠加财富戒指/幸运附魔等后续逻辑）。

如果你希望恢复到小概率掉落，只需在 `Rat` 初始化块中调整 `lootChance` 即可。

---

### 4. 编译期扩展：给其它怪物或房间挂表

#### 4.1 为新怪物挂表

假设你有一个 `Goblin` 怪物，希望它有如下掉落：

- 60% 概率：金币；
- 30% 概率：随机食物（Generator.Category.FOOD）；
- 10% 概率：某个自定义道具。

可以这样写：

```java
public class Goblin extends Mob {

    static {
        LootRegistry.LootTable table = new LootRegistry.LootTable()
                .add(LootRegistry.forItemSupplier(6, () -> new Gold(Random.Int(5, 11))))
                .add(LootRegistry.forItemSupplier(3, () ->
                        Generator.randomUsingDefaults(Generator.Category.FOOD)))
                .add(LootRegistry.forItemClass(1, MyCustomItem.class));

        LootRegistry.register("mob:goblin", table);
    }

    {
        lootTableId = "mob:goblin";
        lootChance = 0.4f; // 40% 触发这套 lootTable
    }
}
```

#### 4.2 房间/宝箱奖励

目前 `LootRegistry` 的 `LootContext.Source` 已包含 `ROOM_REWARD` / `CHEST_OPEN` 等枚举，可在自定义场景中手动构造：

```java
LootRegistry.LootContext ctx =
        new LootRegistry.LootContext(null, Dungeon.hero, Dungeon.depth, dropPos,
                                     LootRegistry.LootContext.Source.ROOM_REWARD);

for (Item it : LootRegistry.roll("room:secret_reward", ctx)) {
    level.drop(it, dropPos).sprite.drop();
}
```

> 当前工程还未批量把房间/宝箱接入 `LootRegistry`，但你可以在新场景中直接使用上述模式。

---

### 5. 运行时扩展（预留）

虽然当前版本只在 Java 中使用 `LootRegistry.register(...)` 注册 loot 表，但代码结构已经适合作为 JSON/配置加载器的后端：

- 可以在启动时枚举 `assets/cola/loot/*.json`；
- 对每个 JSON 构造 `LootTable` + `LootEntry`，调用 `LootRegistry.register(id, table)`；
- 在 Mob/房间中只需要设置好 `lootTableId` 字符串即可。

该 JSON 加载逻辑尚未写入本仓库，但未来可以参考 `TexturePackManager` 的做法来实现。

---

### 6. 向后兼容与迁移建议

- **向后兼容**：不设置 `lootTableId` 的怪物仍完全走旧逻辑（`loot` 字段 + `Generator` + 各种加成）。  
- **推荐迁移顺序**：
  1. 为代表性怪物（例如 Rat / Gnoll / Boss）写并挂接 `LootRegistry` 表，观察数值效果；
  2. 再逐步为特殊房间与剧情奖励挂表；
  3. 如有需要，再将部分 `Generator` 调用迁移为通过 `LootRegistry` 管理（例如为整个 `Category` 定义子表）。


