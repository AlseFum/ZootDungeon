# Weapon 目录结构

> 包路径: `com.zootdungeon.items.weapon`

## base/ — 武器基类 (18)

所有武器的抽象基类和未实现的基类原型。这些类在游戏正常流程中**不掉落**。

| 文件 | 说明 |
|------|------|
| [Weapon.java](base/Weapon.java) | **根基类** — 所有武器的抽象父类 (extends KindOfWeapon)，含 Enchantment 内部类 |
| [MeleeWeapon.java](base/MeleeWeapon.java) | **近战基类** — 所有近战武器的父类 (extends Weapon) |
| [MissileWeapon.java](base/MissileWeapon.java) | **投掷基类** — 所有投掷武器的抽象父类 (extends Weapon) |
| [FirearmWeapon.java](base/FirearmWeapon.java) | **枪械基类** — 所有枪械的抽象父类 (extends Weapon) |
| [AccurateWeapon.java](base/AccurateWeapon.java) | 精准系 — 高命中近战 (extends MeleeWeapon) |
| [AmbushWeapon.java](base/AmbushWeapon.java) | 伏击系 — 潜行/偷袭近战 (extends MeleeWeapon) |
| [BlockWeapon.java](base/BlockWeapon.java) | 格挡系 — 防御近战 (extends MeleeWeapon) |
| [CleaveWeapon.java](base/CleaveWeapon.java) | 劈砍系 — 横扫近战 (extends MeleeWeapon) |
| [CrowdWeapon.java](base/CrowdWeapon.java) | 范围系 — 群体近战 (extends MeleeWeapon) |
| [FastWeapon.java](base/FastWeapon.java) | 快速系 — 高攻速近战 (extends MeleeWeapon) |
| [LongRangeWeapon.java](base/LongRangeWeapon.java) | 长柄系 — 远距离近战 (extends MeleeWeapon) |
| [InstantMechWeapon.java](base/InstantMechWeapon.java) | (未实现基类) 驭械武器原型 |
| [MomentumWeapon.java](base/MomentumWeapon.java) | (未实现基类) 冲量武器原型 |
| [PropertyHuntingWeapon.java](base/PropertyHuntingWeapon.java) | (未实现基类) 特性狩猎武器原型 |
| [RangeReducedWeapon.java](base/RangeReducedWeapon.java) | (未实现基类) 距离衰减武器原型 |
| [StateSwitchWeapon.java](base/StateSwitchWeapon.java) | (未实现基类) 状态切换武器原型 |
| [TransferMechWeapon.java](base/TransferMechWeapon.java) | (未实现基类) 转移机甲武器原型 |
| [TwinBlade.java](base/TwinBlade.java) | (未实现基类) 双刀武器原型 |

## 具体武器 (40) — package: `com.zootdungeon.items.weapon`

平铺在根目录，按功能系列排列：

| 文件 | 系列 | 说明 |
|------|------|------|
| [BannerWeapon.java](BannerWeapon.java) | — | 旗帜 |
| [BerserkWeapon.java](BerserkWeapon.java) | — | 狂战士 |
| [BlastWeapon.java](BlastWeapon.java) | — | 爆破 |
| [BloodWeapon.java](BloodWeapon.java) | — | 血怒 |
| [Chakram.java](Chakram.java) | — | 轮刃 (extends Weapon) |
| [RhodesStandardBow.java](RhodesStandardBow.java) | — | 罗德制式弓 (extends Weapon) |
| [SpiritBow.java](SpiritBow.java) | — | 灵弓 (extends Weapon) |
| [MagesStaff.java](MagesStaff.java) | — | 法师法杖 |
| [Saw.java](Saw.java) | — | 电锯 |
| [RunicBlade.java](RunicBlade.java) | — | 符文刃 |
| [Crossbow.java](Crossbow.java) | — | 弩 |
| [Flail.java](Flail.java) | — | 链枷 |
| [Greataxe.java](Greataxe.java) | — | 巨斧 |
| [Scimitar.java](Scimitar.java) | — | 弯刀 |
| [WornShortsword.java](WornShortsword.java) | — | 破旧短剑 (初始武器) |
| [BattleAxe.java](BattleAxe.java) | AccurateWeapon | 战斧 |
| [Cudgel.java](Cudgel.java) | AccurateWeapon | 棍棒 |
| [HandAxe.java](HandAxe.java) | AccurateWeapon | 手斧 |
| [Mace.java](Mace.java) | AccurateWeapon | 钉头锤 |
| [WarHammer.java](WarHammer.java) | AccurateWeapon | 战锤 |
| [AssassinsBlade.java](AssassinsBlade.java) | AmbushWeapon | 暗杀刃 |
| [Dagger.java](Dagger.java) | AmbushWeapon | 匕首 |
| [Dirk.java](Dirk.java) | AmbushWeapon | 短剑 |
| [Greatshield.java](Greatshield.java) | BlockWeapon | 大盾 |
| [Katana.java](Katana.java) | BlockWeapon | 武士刀 |
| [Quarterstaff.java](Quarterstaff.java) | BlockWeapon | 长棍 |
| [Rapier.java](Rapier.java) | BlockWeapon | 刺剑 |
| [RoundShield.java](RoundShield.java) | BlockWeapon | 圆盾 |
| [Greatsword.java](Greatsword.java) | CleaveWeapon | 巨剑 |
| [Longsword.java](Longsword.java) | CleaveWeapon | 长剑 |
| [Shortsword.java](Shortsword.java) | CleaveWeapon | 短剑 |
| [Sword.java](Sword.java) | CleaveWeapon | 剑 |
| [Sickle.java](Sickle.java) | CrowdWeapon | 镰刀 |
| [WarScythe.java](WarScythe.java) | CrowdWeapon | 战镰 |
| [Gauntlet.java](Gauntlet.java) | FastWeapon | 石拳套 |
| [Gloves.java](Gloves.java) | FastWeapon | 钉刺手套 |
| [Sai.java](Sai.java) | FastWeapon | 十手 |
| [Glaive.java](Glaive.java) | LongRangeWeapon | 长刃戟 |
| [Spear.java](Spear.java) | LongRangeWeapon | 矛 |
| [Whip.java](Whip.java) | LongRangeWeapon | 鞭 |

## missiles/ — 投掷武器 (15)

Package: `com.zootdungeon.items.weapon.missiles`，基类 MissileWeapon 在 `base/`。

| 文件 | 说明 |
|------|------|
| [Bolas.java](missiles/Bolas.java) | 流星索 |
| [FishingSpear.java](missiles/FishingSpear.java) | 渔叉 |
| [ForceCube.java](missiles/ForceCube.java) | 力场方块 |
| [HeavyBoomerang.java](missiles/HeavyBoomerang.java) | 重型回旋镖 |
| [Javelin.java](missiles/Javelin.java) | 标枪 |
| [Kunai.java](missiles/Kunai.java) | 苦无 |
| [Shuriken.java](missiles/Shuriken.java) | 手里剑 |
| [ThrowingClub.java](missiles/ThrowingClub.java) | 投掷棍 |
| [ThrowingHammer.java](missiles/ThrowingHammer.java) | 投掷锤 |
| [ThrowingKnife.java](missiles/ThrowingKnife.java) | 飞刀 |
| [ThrowingSpear.java](missiles/ThrowingSpear.java) | 投矛 |
| [ThrowingSpike.java](missiles/ThrowingSpike.java) | 飞刺 |
| [ThrowingStone.java](missiles/ThrowingStone.java) | 投石 |
| [Tomahawk.java](missiles/Tomahawk.java) | 手斧 |
| [Trident.java](missiles/Trident.java) | 三叉戟 |

## darts/ — 飞镖 (14)

Package: `com.zootdungeon.items.weapon.darts`，基类 MissileWeapon 在 `base/`。

| 文件 | 说明 |
|------|------|
| [Dart.java](darts/Dart.java) | 基础飞镖 |
| [TippedDart.java](darts/TippedDart.java) | 淬毒飞镖基类 |
| [AdrenalineDart.java](darts/AdrenalineDart.java) | 肾上腺素镖 |
| [BlindingDart.java](darts/BlindingDart.java) | 致盲镖 |
| [ChillingDart.java](darts/ChillingDart.java) | 冰霜镖 |
| [CleansingDart.java](darts/CleansingDart.java) | 净化镖 |
| [DisplacingDart.java](darts/DisplacingDart.java) | 位移镖 |
| [HealingDart.java](darts/HealingDart.java) | 治疗镖 |
| [HolyDart.java](darts/HolyDart.java) | 圣光镖 |
| [IncendiaryDart.java](darts/IncendiaryDart.java) | 燃烧镖 |
| [ParalyticDart.java](darts/ParalyticDart.java) | 麻痹镖 |
| [PoisonDart.java](darts/PoisonDart.java) | 毒镖 |
| [RotDart.java](darts/RotDart.java) | 腐化镖 |
| [ShockingDart.java](darts/ShockingDart.java) | 电击镖 |

## curses/ — 诅咒附魔 (8)

Package: `com.zootdungeon.items.weapon.curses`，全部 extends `Weapon.Enchantment`。

| 文件 | 说明 |
|------|------|
| [Annoying.java](curses/Annoying.java) | 恼人 |
| [Dazzling.java](curses/Dazzling.java) | 目眩 |
| [Displacing.java](curses/Displacing.java) | 移形 |
| [Explosive.java](curses/Explosive.java) | 爆炸 |
| [Friendly.java](curses/Friendly.java) | 友善 |
| [Polarized.java](curses/Polarized.java) | 极化 |
| [Sacrificial.java](curses/Sacrificial.java) | 献祭 |
| [Wayward.java](curses/Wayward.java) | 任性 |

## enchantments/ — 正向附魔 (13)

Package: `com.zootdungeon.items.weapon.enchantments`，全部 extends `Weapon.Enchantment`。

| 文件 | 说明 |
|------|------|
| [Blazing.java](enchantments/Blazing.java) | 烈焰 |
| [Blocking.java](enchantments/Blocking.java) | 格挡 |
| [Blooming.java](enchantments/Blooming.java) | 绽放 |
| [Chilling.java](enchantments/Chilling.java) | 冰霜 |
| [Corrupting.java](enchantments/Corrupting.java) | 腐化 |
| [Elastic.java](enchantments/Elastic.java) | 弹性 |
| [Grim.java](enchantments/Grim.java) | 严峻 |
| [Kinetic.java](enchantments/Kinetic.java) | 动能 |
| [Lucky.java](enchantments/Lucky.java) | 幸运 |
| [Projecting.java](enchantments/Projecting.java) | 投射 |
| [Shocking.java](enchantments/Shocking.java) | 电击 |
| [Unstable.java](enchantments/Unstable.java) | 不稳定 |
| [Vampiric.java](enchantments/Vampiric.java) | 吸血 |

## firearms/ — 枪械弹药 (2)

Package: `com.zootdungeon.items.weapon.firearms`，这些不是武器，是 Item 的子类。基类 FirearmWeapon 在 `base/`。

| 文件 | 说明 |
|------|------|
| [FirearmBullet.java](firearms/FirearmBullet.java) | 弹药 |
| [FirearmMagazine.java](firearms/FirearmMagazine.java) | 弹匣 |

---

**总文件数: 110**
