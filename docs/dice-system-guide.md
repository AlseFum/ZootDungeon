## 骰点系统改造总览（Dice / D&D 风格）

本文件说明当前版本中与战斗数值相关的“掷骰系统”实现方式，包括：

- 基础类 `com.zootdungeon.utils.Dice`
- 武器基础伤害与 exSTR 等增益如何组装成一个骰组
- 不同武器类型（近战、远程、埋伏武器等）在伤害上的特殊处理

本指南面向**想要调整数值、加新武器、或在运行时动态生成伤害公式**的开发者/Mod 作者。

---

### 1. 核心：`Dice` 类

`Dice` 代表一个“骰组表达式”，由若干 `Die` 与整型常数组成：

- `Dice.Die(amount, sides)`：表示 \\( amount \\times d\\,sides \\) 的骰子，例如：
  - `new Die(2, 6)` 表示 `2d6`
  - 其上支持修饰：
    - `withMaxOf(k)`：只保留最大 k 个（keep highest）
    - `withMinOf(k)`：只保留最小 k 个（keep lowest）
    - `withExpandMorethan(t)`：每次掷出的点数 ≥t 时追加一次同样的骰（exploding 风格）
- `Integer`：表示一个常数偏移（可以为负数）。

构建方式：

```java
// 快捷静态方法
Dice dice = Dice.of(
    new Dice.Die(2, 6), // 2d6
    3                    // +3
);

// 动态追加组件
dice.addDie(1, 4)        // +1d4
    .addConstant(-1);    // -1
```

执行掷骰：

```java
// 使用指定 Random（通常仅用于测试）
Dice.RollResult res = dice.roll(new java.util.Random());
int total = res.getTotal();

// 在游戏中，推荐使用全局 RNG 封装：
int total = dice.rollTotalGame();
```

`rollTotalGame()` 内部通过适配器调用 `com.watabou.utils.Random.Int(bound)`，与现有随机体系兼容。

其他实用接口：

- `describe()`：返回表达式字符串（例如 `"2d6+3k1e6-1"`），用于 UI。
- `RollResult.describe()`：返回一次掷骰的详细过程，如 `"[4, 2] + 3 = 9"`。
- `addAll(Dice other)`：将另一个骰组的所有组件并入当前骰组（常用于把基础伤害骰与 exSTR 骰组合）。

> 当前版本尚未实现从字符串 `parse("2d6+3")` 的解析，但后续可以在此基础上扩展。

---

### 2. 武器基础伤害与 exSTR：`KindOfWeapon`

所有武器类型都继承自 `KindOfWeapon`，其核心接口如下：

```java
abstract public class KindOfWeapon extends EquipableItem {

    public int min() { return min(buffedLvl()); }
    public int max() { return max(buffedLvl()); }

    abstract public int min(int lvl);
    abstract public int max(int lvl);

    // 构建基础伤害骰（不含 exSTR）
    public Dice baseDamageDice(int lvl) { ... }

    // exSTR 数值加成（保持原 clover 机制）
    protected int rollExStrBonus(Hero hero) { ... }

    // exSTR 对骰组的贡献：返回一个“可被加和”的 Dice
    public Dice exSTRDice(Hero hero) { ... }

    // 完整伤害骰组 = 基础骰 + exSTR骰
    public Dice damageDice(Char owner) { ... }

    // 实际伤害 = 完整伤害骰组 roll 一次
    public int damageRoll(Char owner) { ... }
}
```

#### 2.1 基础伤害骰 `baseDamageDice(int lvl)`

为了兼容原有的 `min(lvl)~max(lvl)` 区间，这里使用一个简单的等价骰表达：

```java
int mn = min(lvl);
int mx = max(lvl);
if (mn >= mx) {
    return Dice.of(mn);
}
int span = mx - mn;
// 覆盖 [mn, mx] 区间
return Dice.of(new Dice.Die(1, span + 1), mn - 1);
```

即 `1d(span+1) + (mn-1)`。

#### 2.2 exSTR 数值加成与骰组加成

- **数值加成**（保持原 ThirteenLeafClover 等机制）：

```java
protected int rollExStrBonus(Hero hero){
    int req = (this instanceof Weapon) ? ((Weapon) this).STRReq() : 0;
    int exStr = hero.STR() - req;
    if (exStr > 0){
        return Hero.heroDamageIntRange(0, exStr);
    }
    return 0;
}
```

- **exSTR 骰组**（用于描述/扩展）：

```java
public Dice exSTRDice(Hero hero){
    int req = (this instanceof Weapon) ? ((Weapon) this).STRReq() : 0;
    int exStr = hero.STR() - req;
    if (exStr <= 0){
        return new Dice(); // 空骰组
    }
    // 对应 0..exStr 的波动：1d(exStr+1) - 1
    return Dice.of(new Dice.Die(1, exStr + 1), -1);
}
```

#### 2.3 完整伤害骰组与实际伤害

```java
public Dice damageDice(Char owner){
    Dice dice = baseDamageDice(buffedLvl());
    if (owner instanceof Hero){
        dice.addAll(exSTRDice((Hero) owner));
    }
    return dice;
}

public int damageRoll(Char owner) {
    Dice dice = damageDice(owner);
    return dice.rollTotalGame();
}
```

> 注意：`MeleeWeapon` / `MissileWeapon` / `SpiritBow` 等子类在 `damageRoll` 上仍可能叠加其它倍率（如动量、sniper 特效等），这些乘法/加法是基于 Dice roll 出的基础伤害上再处理的。

---

### 3. 特殊武器：埋伏武器 & 投掷武器的骰点实现

#### 3.1 埋伏武器 `AmbushWeapon`

`AmbushWeapon.damageRoll(Char owner)` 对“偷袭”做了特殊高端偏移，并用 Dice 重写：

```java
@Override
public int damageRoll(Char owner) {
    if (owner instanceof Hero) {
        Hero hero = (Hero)owner;
        Char enemy = hero.enemy();
        if (enemy instanceof Mob && ((Mob) enemy).surprisedBy(hero)) {
            int lvl = buffedLvl();
            int mn = min(lvl);
            int mx = max(lvl);
            int diff = mx - mn;

            // 基础部分：将 [mn, mx] 区间压缩到 [mn+ambushRate*diff, mx]
            int biasedMin = mn + Math.round(diff * ambushRate); // ambushRate 默认 0.5f/0.75f 等
            int biasedMax = mx + Math.round(diff * ambushRate);
            if (biasedMin > biasedMax) biasedMin = biasedMax;

            int span = biasedMax - biasedMin;
            Dice base = span > 0
                    ? Dice.of(new Dice.Die(1, span + 1), biasedMin - 1)
                    : Dice.of(biasedMin);

            int damage = augment.damageFactor(base.rollTotalGame());

            // 追加 exSTR 骰
            damage += exSTRDice(hero).rollTotalGame();
            return damage;
        }
    }
    return super.damageRoll(owner);
}
```

#### 3.2 投掷小刀 `ThrowingKnife`

同样对“惊袭”做 `75% toward max ~ max` 的偏移，并用 Dice 重构：

```java
@Override
public int damageRoll(Char owner) {
    if (owner instanceof Hero) {
        Hero hero = (Hero)owner;
        Char enemy = hero.enemy();
        if (enemy instanceof Mob && ((Mob) enemy).surprisedBy(hero)) {
            int lvl = buffedLvl();
            int mn = min(lvl);
            int mx = max(lvl);
            int diff = mx - mn;

            // 75% toward max to max
            int biasedMin = mn + Math.round(diff * 0.75f);
            if (biasedMin > mx) biasedMin = mx;

            int span = mx - biasedMin;
            Dice base = span > 0
                    ? Dice.of(new Dice.Die(1, span + 1), biasedMin - 1)
                    : Dice.of(biasedMin);

            int damage = augment.damageFactor(base.rollTotalGame());
            damage += exSTRDice(hero).rollTotalGame();
            return damage;
        }
    }
    return super.damageRoll(owner);
}
```

---

### 4. UI 中的伤害骰描述

在近战/远程武器信息中，追加了一行“伤害骰”描述，用于清楚展示当前武器的骰组：

- `MeleeWeapon.info()`：

```java
String statsInfo = statsInfo();
if (!statsInfo.equals("")) info += "\n\n" + statsInfo;

// 追加伤害骰描述，展示基础伤害与 exSTR 等加成在骰组中的表现
if (Dungeon.hero != null){
    info += "\n" + "伤害骰: " + damageDice(Dungeon.hero).describe();
}
```

- `MissileWeapon.info()`：

```java
if (durabilityPerUse() > 0){
    info += " " + Messages.get(this, "uses_left",
            (int)Math.ceil(durability/durabilityPerUse()),
            (int)Math.ceil(MAX_DURABILITY/durabilityPerUse()));
} else {
    info += " " + Messages.get(this, "unlimited_uses");
}

// 追加伤害骰描述
if (Dungeon.hero != null){
    info += "\n" + "伤害骰: " + damageDice(Dungeon.hero).describe();
}
```

---

### 5. 开发建议

- 新增武器时：
  - 只需正确实现 `min(int lvl)` / `max(int lvl)`，骰组会自动构建；
  - 如需特别的伤害浮动（例如“高端偏移”、“多段伤害”），可以在子类中重写 `damageRoll`，使用 `Dice` 来表达，而不是直接调用 `Random`。
- 想要在技能或特效中增加“额外骰子伤害”：
  - 推荐模式：
    ```java
    Dice extra = Dice.of(new Dice.Die(1, 6)); // +1d6
    int dmg = baseDamage + extra.rollTotalGame();
    ```
  - 或者，为其设计一个单独的 `Dice` 字符串（未来可通过配置/脚本驱动）。


