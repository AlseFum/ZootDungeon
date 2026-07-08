# 预备职业系统文档

## 概述

所有预备职业（RESERVED_X）共享以下特点：
- 无 T3 职业天赋（T3 由子类提供）
- 无护甲技能（T4 天赋不可用）
- T1/T2 天赋从对应原版职业复制，但可独立调整
- TalentsPane 在子类选择后解锁 T3 面板

---

## 1. RESERVED_GUARD（预备近卫）

**基类：** WARRIOR  
**特色物品：** GuardModal（守护模组，类似 BrokenSeal 但可投掷）  
**T1 天赋（4个）：** 丰盛餐食 / 老兵直觉 / 挑衅愤怒 / 不动如山  
**T2 天赋（5个）：** 铁胃 / 液体意志 / 符文转移 / 致命势头 / 临时投掷物  
**护盾公式：** `armTier + armLvl + IRON_WILL点数`

---

### 1.1 OP_SHARP（锋羽 → 改为 Sharp）

**核心机制：羽决（Feather Duel）**  
投掷 GuardModal 发起单挑，标记持续 8 回合（基础，无需天赋）。单挑期间：`FeatherDuelGuard` buff 提供减伤，`FeatherDuelMark` 在敌人身上倒计时。

**T3 天赋（5个）：**

| 天赋 | 图标ID | 效果 |
|------|--------|------|
| FEATHER_FURY（羽怒） | 227 | Lv1: 非目标减伤30% + 对目标增伤20%；Lv2: 50% + 25%；Lv3: 80% + 50%。减伤在 `FeatherDuelGuard.damageMultiplier()`，增伤在 `Damage.computeBaseDamage()` |
| DUEL_MOMENTUM（单挑衅势） | 228 | 击杀目标后 3/6/9 回合内：无视单挑CD + Guard持久化 + 速度+10/20/40%。速度在 `Char.speed()` 中通过 `DuelMomentumBuff.speedMultiplier()` 实现 |
| SWEEPING_STRIKES（横扫） | 229 | 20/40/60% 概率同时攻击3×3范围另一敌人，造成等量伤害。在 `Hero.attackProc()` 中处理 |
| BOUNTY_DUEL（悬赏单挑） | 230 | Lv1: 花费20金发起，击杀得40金+额外掉落(20金)；Lv2: 40/80/40；Lv3: 40/120/60。金币不足时GuardModal正常掉落 |
| STRENGTH_ENHANCE（力量增强） | 236 | STR +1/+2/+3，减少武器力量需求。在 `Hero.STR()` 中实现 |

**已移除的天赋：** FEATHER_DUEL（225，单挑基础机制不再需要天赋）、DUEL_MASTERY（226，固定加伤合并入 FEATHER_FURY）

---

### 1.2 ACE

**核心机制：**  
- **连击系统（AceCombo）：** 攻击+受击（未被GuardModal吸收）都积累连击数
- **GuardModal吸收（AceAbsorptionCounter）：** 护甲附着时吸收最多5次弱攻击（伤害<50%护甲DRMax），在 `Hero.defenseProc()` 中处理，先于护盾
- **消耗连击释放战技：** 5个战技（见下表）

**武器：** 初始获得 WornShortsword（通用）

**T3 天赋（5个）：**

| 天赋 | 图标ID | 效果 |
|------|--------|------|
| STRENGTH_ENHANCE（力量增强） | 231 | STR +1/+2/+3 |
| AOE_ATTACK（范围攻击） | 232 | 20/40/60% 溅射3×3 |
| ARMOR_ACCUMULATION（护甲积累） | 233 | 击杀或连击N次获得盾。Lv1: N=10, 5%HT；Lv2: N=7, 8%HT；Lv3: N=5, 12%HT |
| COMBO_EXTENSION（连击延长） | 234 | 连击持续时间 7/10/15 回合（默认5回合） |
| VERSATILE_COMBO（全能连击） | 235 | **2点天赋**。Lv1: 远程/投掷/法杖维持连击；Lv2: 也增加连击数 |

**ACE 战技（AceCombo，连击消耗2/4/6/8/10）：**

| 战技 | 消耗 | 效果 |
|------|------|------|
| SHIELD_BASH（盾击） | 2 | 伤害=当前护盾值，击退1格 |
| COUNTER_STANCE（反击架势） | 4 | 1回合内下次受击完全格挡+反击，`CounterTracker` + `CounterRiposteTracker` |
| GUARDIAN_SLASH（守护斩） | 6 | 25%×连击数额外伤害，AOE半数伤害给相邻敌人，消耗连击 |
| FORTIFY（强化） | 8 | 刷新护盾至最大值，3回合内护盾恢复+2 |
| ACE_STRIKE（ACE终结技） | 10 | 30%×连击数额外伤害，击杀重置吸收计数，消耗连击 |

---

### 1.3 BLAZE

**核心机制：**  
- **炽热buff（BlazeHeatBuff）：** 堆叠型buff（CounterBuff）。攻击+1层，击杀+3层，给GuardModal施可燃物+N层。每层加速回血5% + 饥饿3%
- **可燃物系统：** Predicate<Item> 判断有效燃料（火焰花种子、液火药剂等），消耗获得炽热层数
- **锯武器（Saw）：** 子类选择时自动替换 WornShortsword

**T3 天赋（5个，T5 留空）：**

| 天赋 | 图标ID | 效果 |
|------|--------|------|
| STRENGTH_ENHANCE（力量增强） | 237 | STR +1/+2/+3 |
| AOE_ATTACK（范围攻击） | 238 | 20/40/60% 溅射3×3 |
| SAW_MASTERY（锯刃精通） | 239 | Lv1: 锯33%额外攻击+附着燃烧；Lv2: 67%；Lv3: 100%+投掷GuardModal爆炸（炽热/燃烧状态下） |
| INFERNAL_ENDURANCE（炼狱耐久） | 240 | Lv1: 燃烧时30%火减伤+炽热加速回血/饥饿；Lv2: 50%+自动进食(不消耗回合)；Lv3: 75%+免死(HP归零消耗炽热/燃烧) |

**炽热修正：** `Regeneration.java` 中 `delay /= blazeHeat.regenMultiplier()`；`Hunger.java` 中 `hungerDelay /= blazeHeat.hungerMultiplier()`

---

## 2. RESERVED_SNIPER（预备狙击）— ❌ 全部待重做

**设计决策：** 整个预备狙击体系需要重新设计，所有已有实现均视为废弃（包括代码和文档中的描述），等待新方案。

**基类：** HUNTRESS  
**T1 天赋（4个）：** 自然恩赐 / 生存直觉 / 追击打击 / 自然援助 — ❌ 待重做  
**T2 天赋（5个）：** 活力餐食 / 液体自然 / 回春步伐 / 敏锐感知 / 耐久弹药 — ❌ 待重做

### 子职业

| 子职业 | 状态 |
|--------|------|
| STORMEYE | ❌ 待重做 |
| ROSMONTIS | ✅ **已实现** |
| OUTCAST | ❌ 待重做 |

### 代码状态

| 文件 | 状态 |
|------|------|
| `RosmontisThrow.java` | ✅ **新增** — 统一投掷逻辑 |
| `RhodesStandardBow.java` | ❌ 罗德短弓机制待重新设计 |
| `OutcastHighNoon.java` | ❌ 午时已到待重做 |
| Hero.java 中 ROSMONTIS 的 attackProc 逻辑 | ✅ 投掷强化 + 穿透投掷 |
| Hero.java 中 STORMEYE/OUTCAST 的 attackProc 逻辑 | ❌ 待随各自重做清理 |
| MeleeWeapon.java 的 AC_THROW | ✅ 已走 RosmontisThrow |
| Wand.java 的 AC_THROW | ✅ 已走 RosmontisThrow |
| ScoutLootBuff.java | ❌ 未使用（SCOUT 已暂停） |

---

### ROSMONTIS（已实现）

**定位：** 投掷武器特化，一切能扔的都扔出去。

#### 新增文件

- `RosmontisThrow.java` — 统一处理所有投掷行为，`actors.hero` 包

#### T3 天赋（5 个，2 公共 + 3 专属）

| # | 天赋 | 效果 |
|---|------|------|
| 1 | RECON_SHOT（公共） | 远程命中展开视野 |
| 2 | ROOTING_SHOT（公共） | 远程命中束缚周围 |
| 3 | **PIERCING_THROW** | 投掷武器 20/40/60% 穿透目标攻击后方敌人 |
| 4 | **ENHANCED_THROWN** | 投掷结算时等级视为 `实际等级 + 天赋点数`，影响 MissileWeapon 和 RosmontisThrow 投掷伤害 |
| 5 | **THROW_MELEE** | 可投掷近战武器和法杖，造成 50/100/150% 伤害，走 weapon.proc() |

#### 核心机制

**入口：** `MeleeWeapon.actions()` 和 `Wand.execute()` 在 ROSMONTIS 且天赋5 解锁时拦截 AC_THROW。

**执行（统一走 `RosmontisThrow.throwItem()`）：**

```
RosmontisThrow.throwItem(hero, item, cell)
    ├── Weapon / MeleeWeapon / MagesStaff
    │     damage = damageRoll(boostedLvl) × throwMult[天赋5点数]
    │     damage = item.proc(hero, enemy, damage)  ← 走武器自己的proc链，触发附魔/特殊效果
    │     enemy.damage(damage, hero)
    │
    └── Wand（背包内的法杖，不含 MagesStaff）
          TODO: 效果待定
```

其中 `boostedLvl = item.level() + pointsInTalent(ENHANCED_THROWN)`。

**Hero.attackProc 中 ENHANCED_THROWN 对 MissileWeapon 加成：** `damage += tier × talentPts`

**注意：** 投掷行为不是装备解除，只是远程攻击一次，武器/法杖仍保留在手上。

#### 已知问题

- **武器缓存问题：** 投掷武器后内部有状态缓存，再次投掷会失败。原因是 `RosmontisThrow.throwWeapon()` 临时修改 `weapon.level()` 又恢复，可能与 belongings 系统的引用缓存冲突。需要改为不修改武器等级、而是直接在伤害计算中注入等级加成的方式。
- **与弓无联动：** ROSMONTIS 的投掷特化和基类 HUNTRESS 的弓（SpiritBow / RhodesStandardBow）没有配合。虽然是纯投掷路线不需要弓，但作为 RESERVED_SNIPER 的子类，缺少与远程武器体系的交互仍是一个设计缺口，需要思考怎么补。

---

## 3. RESERVED_CASTER（预备术士）

**⚠️ 注意：** 术士子类对 Wand 的使用存在瞄准目标类型不统一的问题——`AC_ZAP`和`AC_DISCHARGE`走 `zapper`（Ballistica 投射物目标，cell/mob 皆可），`AC_RUNE`走自定义 CellSelector（选地板格子），`AC_DECLARE`则直接弹出窗口选类型。这个需要在后续重新思考统一。

**基类：** MAGE  
**特色物品：** MagesStaff（法师法杖）  
**T1 天赋（4个）：** 赋能餐食 / 学者直觉 / 残留魔法 / 备用护盾  
**T2 天赋（5个）：** 充能餐食 / 铭刻之力 / 法杖保存 / 奥术视野 / 护盾电池

**公共天赋（PITH/LOGOS/MANTRA 共享，2个）：**

| 天赋 | 图标ID | 效果 |
|------|--------|------|
| SCROLL_RECHARGE（卷轴充能） | 252 | 读卷轴时背包法杖恢复0.5/1/3充能。在 `Scroll.java:doRead()` 中实现 |
| LAST_CHARGE_BOOST（终充增强） | 253 | Lv1: 使用最后充能时法杖等级+1；Lv2: 读卷轴也+1；Lv3: 最后充能时给予5%HT护盾。在 `Wand.java:wandUsed()` 和 `buffedLvl()` 中实现 |

---

### 3.1 PITH

**核心机制：法杖记忆（PithWandMemory）**  
- 记忆上次使用的法杖类型
- 可修改下次法杖效果（二维表，TODO）
- 第二次使用后清除记忆
- 支持三层连锁（天赋4）

**T3 天赋（5个）：**

| 天赋 | 图标ID | 效果 |
|------|--------|------|
| SCROLL_RECHARGE | 222 | （公共） |
| LAST_CHARGE_BOOST | 223 | （公共） |
| WAND_CHAIN（法杖连锁） | 224 | 连续使用法杖30%/50%/70%给前一根充能。在 `Wand.java:wandUsed()` 中实现 |
| CHAIN_STRONGER（连锁强化） | 225 | Lv1: 联动法术更强；Lv2: 三次联动（记忆第1、2根修改第3根）；Lv3: 第3根成为下一循环第1根 |
| FULL_DISCHARGE（完全释放） | 226 | 一次性消耗法杖所有充能增强效果。在 `Wand.java` 中新增 AC_DISCHARGE，`dischargeBoost` 字段 |

---

### 3.2 LOGOS

**核心机制：**  
- **符文绘制（LogosRuneBuff）：** 消耗法杖充能在地板画符文。3种符文：FIRE（伤害+燃烧）、FROST（伤害+减速）、WARD（护盾）。天赋5使符文更强（+50%倍率/点）。`LogosRuneBuff.drawRune()`
- **空白卷轴（LogosBlankScroll）：** 使用卷轴后概率生成（天赋3）。可灌注法杖充能（最多3），作为一次性法杖使用。在 `Scroll.java` 中生成

**T3 天赋（5个）：**

| 天赋 | 图标ID | 效果 |
|------|--------|------|
| SCROLL_RECHARGE | 252 | （公共） |
| LAST_CHARGE_BOOST | 253 | （公共） |
| BLANK_SCROLL（空白卷轴） | 227 | 使用卷轴后40%/60%/90%生成空白卷轴 |
| WAND_SENTINEL（法杖哨卫） | 228 | TODO：法杖化为哨卫跟随。需行为表 |
| RUNE_POWER（符文强化） | 229 | 符文效果更强（+50%倍率/点） |

---

### 3.3 MANTRA

**核心机制：宣告（MantraDeclarationBuff）**  
- 消耗2法杖充能进行"宣告"（DECLARE 行动在法杖上）
- 选择触发类型：ATTACK（敌攻击）/ MOVE（敌移动）/ CAST（敌施法）
- 宣告持续20回合，视野内敌人做对应行为时触发法杖效果
- 触发挂钩：`Mob.onAttackComplete()` 中检查 ATTACK 触发

**T3 天赋（5个）：**

| 天赋 | 图标ID | 效果 |
|------|--------|------|
| SCROLL_RECHARGE | 252 | （公共） |
| LAST_CHARGE_BOOST | 253 | （公共） |
| SPLASH_TRIGGER（溅射触发） | 228 | 30%/50%/70% 触发效果溅射到相邻敌人 |
| ALLY_TRIGGER（友方触发） | 229 | TODO：友方行动也触发。需行为表 |
| ETERNAL_BUFF（永恒buff） | 230 | 20%/35%/60% 触发时延长buff 5回合（可永久维持） |

---

## 4. RESERVED_SPECIALIST（预备特种）

**基类：** ROGUE  
**特色物品：** CloakOfShadows（潜行斗篷，可未装备使用）  
**T1 天赋（4个）：** 藏匿口粮 / 盗贼直觉 / 偷袭 / 暗影防护  
**T2 天赋（5个）：** 神秘餐食 / 铭刻潜行 / 广域搜索 / 无声步伐 / 盗贼先见

**公共天赋（MISERY/SCOUT/RADIAN 共享，1个）：**

| 天赋 | 图标ID | 效果 |
|------|--------|------|
| CLOAK_UNEQUIPPED（斗篷精通） | 231 | 斗篷未装备时可使用，充能消耗+100%/70%/40%。在 `CloakOfShadows.actions()` 中实现 |

---

### 4.1 MISERY

**核心机制：阴影系统（MiseryShadowBlob）** ✅ 已实现
- 选择 MISERY 子类时自动激活（`TengusMask.choose()` → `GameScene.add(Blob.seed(...))`）
- 暗色阴影格，每次进入新楼层时一次性生成覆盖全图（`Dungeon.switchLevel()` → `MiseryShadowBlob.generateForLevel()`）
- 处于阴影中时敌人更不易发现MISERY（潜行倍率0.5，`Mob.surprisedBy()` 中强制触发突袭）
- 可消耗当前HP百分比传送至阴影格（基础每格3%HP）
- 传送后5回合：攻击若是暗杀+100%伤害，否则+50%（ShadowStrikeBuff，在 `Damage.computeBaseDamage()` 中计算）
- 致命伤时自动传送至最近阴影（LAST_SHADOW 天赋，长冷却，在 `Hero.defenseProc()` 中拦截）
- 跨楼层持久：`switchLevel()` 在每次楼层切换时自动生成（无需额外 buff 或每回合检查）

**T3 天赋（5个）：**

| 天赋 | 图标ID | 效果 | 状态 |
|------|--------|------|------|
| CLOAK_UNEQUIPPED | 231 | （公共） | ✅ |
| SHADOW_TELEPORT（暗影传送） | 232 | 传送HP消耗降低（-15/30/45%），暗杀倍率提升，Lv3传送后隐形3回合 | ✅ |
| CRIPPLE_BLOB（瘴气） | 233 | 投掷武器创建阴影瘴气AOE，范围内敌人减速减闪避（CrippleDebuff）。可免费传送至瘴气中并触发暗杀。点数越高：范围越大、冷却越短 | ✅ |
| SOUL_REAP（灵魂收割） | 234 | 暗杀击杀增加掉落（+15/30/45%，`Mob.lootChance()` 中乘以 `MiseryShadowBlob.soulReapDropChance()`）。Lv2+暗杀时吸血（15/25%，`Hero.attackProc()` 中处理） | ✅ |
| LAST_SHADOW（最终阴影） | 235 | 极长冷却（250/200/150回合），受致命伤自动传至最近阴影，保留至少1HP（`Hero.defenseProc()` 中拦截）。使用 `Talent.LastShadowCooldown` 追踪冷却 | ✅ |

**实现细节：**

| 文件 | 改动 |
|------|------|
| `MiseryShadowBlob.java` | **actors/blobs/ 包**；基于 tile 的 Blob 实现，`cur[]` 存储阴影强度；`findNearestShadow()`；`stealthMultiplier()`；`soulReapDropChance()` 静态方法 |
| `Dungeon.java` (`switchLevel()`) | 楼层切换时检查 `hero.subClass == MISERY` → `MiseryShadowBlob.generateForLevel()` |
| `ShadowStrikeBuff.java` | **新建文件**，从 MiseryShadowBlob 中提取为 public 类 |
| `CrippleDebuff.java` | `speedFactor()` 减速33%；`evasionMultiplier()` 闪避-30% |
| `Talent.java` | 新增 `LastShadowCooldown` 内部类追踪冷却 |
| `TengusMask.java` | 选择 MISERY 时 `GameScene.add(Blob.seed(...))` 初始化阴影 blob |
| `Hero.java` | `damageRoll()`: ShadowStrikeBuff 伤害加成；`attackProc()`: CRIPPLE_BLOB AOE + SOUL_REAP 吸血；`defenseProc()`: LAST_SHADOW 致命伤拦截 |
| `Mob.java` | `surprisedBy()`: 阴影格强制突袭；`defenseSkill()`: CrippleDebuff 闪避削减；`lootChance()`: SOUL_REAP 掉落加成 |
| `Char.java` | `spend()`: CrippleDebuff 减速 |
| `actors.properties` / `actors_zh.properties` | 新增 MiseryShadowBlob、ShadowStrikeBuff、CrippleDebuff 本地化文本 |

**ShadowStrikeBuff 伤害计算：**
- 突袭时（`surprisedBy`）: `dmg * 2.0`（+100%）
- 非突袭时: `dmg * 1.5`（+50%）
- 位于 `Hero.damageRoll()` 中 AttackUpBuff 之后，先于保底0检查

---

### 4.2 SCOUT — ❌ 暂停实现

**原因：** 核心天赋 GOLD_SYNTH 依赖炼金合成系统，但炼金缶可能改为其他形式，待合成系统定型后再做。

**核心机制（存档）：**  
- 发现更多仅SCOUT可见的房间（之前楼层未生成的会补充生成）
- 开箱子时概率额外掉落
- 更多炼金合成配方（TODO表）
- 可消耗金币在合成中替代材料（TODO表）

**T3 天赋（5个）：**

| 天赋 | 图标ID | 预期效果 | 状态 |
|------|--------|---------|------|
| CLOAK_UNEQUIPPED | 231 | （公共） | ✅ 已实现 |
| GOLD_SYNTH（金币合成） | 236 | 消耗金币替代炼金材料。需材料-金币对照表 | ❌ 暂停，等合成系统定型 |
| BETTER_LOOT（更佳掉落） | 237 | 掉率+30/60/90%。ScoutLootBuff + Mob.lootChance() | ⚠️ 代码已写但随 SCOUT 整体暂停 |
| MERCHANT_REFRESH（商人刷新） | 238 | Lv1: 可请求1次刷新/更多商品；Lv3: 2次刷新 | ❌ 暂停 |
| SUMMON_MERCHANT（召唤商人） | 239 | 消耗40%/30%/20%金钱召唤旅行商人提供临时商品 | ❌ 暂停 |

---

### 4.3 RADIAN — ❌ 暂停实现

**原因：** 召唤/部署物体系与地牢探索的孤军深入、步步为营的核心逻辑冲突，且多线程实现复杂。暂时搁置，等更成熟的方案或以后换个方向再做。

**T3 天赋（存档）：**

| 天赋 | 预期效果 |
|------|---------|
| CLOAK_UNEQUIPPED | （公共） |
| SUMMON_CONTROL | 增加召唤物控制模式 |
| CONTROL_RANGE | 增加召唤物控制范围 |
| REPAIR_COST | 降低维修成本 |
| TELEPORT_TO_SUMMON | 传送到召唤物位置 |

---

## 5. RESERVED_OP（预备干员）

**基类：** 独立  
**特色物品：** RhodesIslandTerminal（罗德岛终端，COST系统）  
**T1 天赋（3个）：** 评估鉴定 / 野战口粮 / 指挥护盾  
**T2 天赋（4个）：** 快速进食 / 炼金补贴 / COST激增 / COST精通  
**无子类，无T3天赋**

**机制：**
- 终端COST系统：通过吃食物（野战口粮）、使用物品等积累COST
- 炼金补贴：缺少的炼金能量由终端COST补足
- COST天花板：COST精通增加最大COST

---

## 关键文件索引

| 文件 | 说明 |
|------|------|
| `HeroClassSheet.java` | 所有预备职业注册、子类分配、初始物品、T1/T2天赋列表 |
| `Talent.java` | 天赋常量定义（图标ID 222-275新天赋）、initSubclassTalents、T3排除逻辑 |
| `TalentsPane.java` | T3天赋面板显示控制（预备职业解除2层限制） |
| `Hero.java` | STR()天赋加成、attackProc()子类特效、defenseProc()子类防御 |
| `GuardModal.java` | FeatherDuel(Sharp)、AceAbsorptionCounter(ACE)、BLAZE爆炸、DuelMomentumBuff |
| `Wand.java` | PITH AC_DISCHARGE/DECLARE、终充增强、法杖连锁、MANTRA宣告入口 |
| `Scroll.java` | 卷轴充能(CASTER)、空白卷轴生成(LOGOS) |
| `CloakOfShadows.java` | SPECIALIST斗篷未装备使用 |
| `Mob.java` | SCOUT掉率、MANTRA宣告触发、MISERY阴影潜行+瘴气闪避削减+灵魂收割掉落 |
| `RingOfWealth.java` | 掉率计算（SCOUT加成在Mob.lootChance而非此处） |
| `Hero.java` | OP_SHARP/ACE/BLAZE~~/SNIPER/STORMEYE/ROSMONTIS/OUTCAST~~/PITH/MANTRA/LOGOS 效果；MISERY 暗影一击伤害+瘴气AOE+灵魂收割吸血+最终阴影致命伤拦截 — ~~狙击系 attackProc 逻辑待清理~~ |
| `TengusMask.java` | MISERY 子类选择时激活 MiseryShadowBlob（tile blob）|
| `Talent.java` | LastShadowCooldown 内部类（MISERY 最终阴影冷却追踪） |
| `Char.java` | CrippleDebuff 减速时间缩放 |

## 新建文件索引

| 文件 | 子类 |
|------|------|
| `AceCombo.java` | ACE 连击系统 |
| `WndAceCombo.java` | ACE 战技选择窗口 |
| `BlazeHeatBuff.java` | BLAZE 炽热buff |
| `Saw.java` | BLAZE 锯武器 |
| `RhodesStandardBow.java` | ~~预备狙击 罗德短弓（待重做）~~ |
| `OutcastHighNoon.java` | ~~OUTCAST 午时已到（待重做）~~ |
| `PithWandMemory.java` | PITH 法杖记忆 |
| `LogosBlankScroll.java` | LOGOS 空白卷轴 |
| `LogosRuneBuff.java` | LOGOS 符文系统 |
| `MantraDeclarationBuff.java` | MANTRA 宣告系统 |
| `MiseryShadowBlob.java` | MISERY 阴影系统（actors/blobs/，tile-based Blob） |
| `Dungeon.java` (`switchLevel()`) | 楼层切换时检查 `hero.subClass == MISERY` → `MiseryShadowBlob.generateForLevel()` |
| `TengusMask.java` | 选择 MISERY 时 `GameScene.add(Blob.seed(...))` 初始化阴影 blob |
| `ShadowStrikeBuff.java` | MISERY 暗影一击buff（传送后5回合伤害加成） |
| `CrippleDebuff.java` | MISERY 瘴气debuff |
| `ScoutLootBuff.java` | SCOUT 更佳掉落 |

## TalentsPane 修改记录

预备职业的 T3 面板限制已按顺序解除：
1. RESERVED_GUARD → 允许 T3（有 OP_SHARP/ACE/BLAZE 子类）
2. RESERVED_SNIPER → 允许 T3（有 STORMEYE/ROSMONTIS/OUTCAST 子类）
3. RESERVED_CASTER → 允许 T3（有 PITH/LOGOS/MANTRA 子类）
4. RESERVED_SPECIALIST → 允许 T3（有 MISERY/SCOUT/RADIAN 子类）
5. ReservedOp → 保持 2 层限制（无子类）

## 天赋图标ID总览

所有新天赋图标ID已压缩至 255 以内（纹理上限256）：

| 范围 | 分配 |
|------|------|
| 222-226 | PITH (3) + LOGOS (3) 共享 + OP_SHARP removed 复用 |
| 227-230 | OP_SHARP (4) + MANTRA (3) 共享 |
| 231-235 | ACE (5) + SPECIALIST common (1) + MISERY (4) 共享 |
| 236 | OP_SHARP_STRENGTH_ENHANCE |
| 237-240 | BLAZE (4) + SCOUT (4) 共享 |
| 241-243 | SNIPER common (2) + STORMEYE (3) + RADIAN (4) 共享 |
| 244-251 | STORMEYE/ROSMONTIS/OUTCAST |
| 252-255 | CASTER common (2) + legacy |
