# UI 组件索引

按**作用**分组，便于查找。物理上仍是 `com.zootdungeon.ui` 单层包，没有
子包拆分（避免外部 import 改动）。本文件是"心理索引"。

## 基础控件（widget）

构建型、可被复用的 UI 原子。

| 文件 | 说明 |
| --- | --- |
| `Button` | 按钮基类（所有按钮的父类） |
| `RedButton` | 红色高亮按钮（外部 import 41 处） |
| `StyledButton` | 主题样式按钮（外部 16 处） |
| `IconButton` | 图标按钮（外部 15 处） |
| `CheckBox` | 复选框（外部 6 处） |

## 槽位（slot）

背包 / 快捷栏 / 天赋等"带数据绑定"的格子。

| 文件 | 说明 |
| --- | --- |
| `ItemSlot` | 物品槽基类（外部 4 处） |
| `ItemButton` | 物品按钮 |
| `InventorySlot` | 库存槽 |
| `QuickSlotButton` | 快捷栏槽（外部 22 处） |
| `TalentButton` | 天赋按钮（外部 6 处） |
| `CustomNoteButton` | 自定义备注按钮 |

## 状态指示器（indicator）

HUD 上的"实时状态"显示，不带交互或仅只读。

| 文件 | 说明 |
| --- | --- |
| `BuffIndicator` | Buff 图标容器（外部 151 处，最常用） |
| `BuffIcon` | 单个 buff 图标 |
| `HealthBar` | 血条基类 |
| `BossHealthBar` | Boss 血条（外部 12 处） |
| `CharHealthIndicator` | 角色血条 |
| `TargetHealthIndicator` | 目标血条（外部 7 处） |
| `ActionIndicator` | 行动指示（外部 11 处） |
| `AttackIndicator` | 攻击指示（外部 20 处） |
| `DangerIndicator` | 危险指示 |
| `ResumeIndicator` | 恢复指示 |
| `LootIndicator` | 战利品指示 |
| `CurrencyIndicator` | 金币指示 |

## 面板（pane）

屏幕上的主要面板，整体布局单元。

| 文件 | 说明 |
| --- | --- |
| `StatusPane` | 状态面板（外部 5 处） |
| `Toolbar` | 工具栏（外部 5 处，最大单文件 29 KB） |
| `InventoryPane` | 库存面板 |
| `MenuPane` | 菜单面板 |
| `TalentsPane` | 天赋面板（外部 7 处） |
| `QuickRecipe` | 快速合成 |

## 滚动与列表（list / scroll）

| 文件 | 说明 |
| --- | --- |
| `ScrollPane` | 滚动面板基类（外部 10 处） |
| `ScrollingGridPane` | 网格滚动 |
| `ScrollingListPane` | 列表滚动 |
| `BadgesGrid` | 徽章网格（外部 2 处） |
| `BadgesList` | 徽章列表（外部 2 处） |

## 弹窗（window）

| 文件 | 说明 |
| --- | --- |
| `Window` | 窗口基类（外部 66 处） |
| `changelist/WndChanges` | 变更窗口 |
| `changelist/WndChangesTabbed` | 变更窗口 Tab 版 |

## 文本与渲染（render）

| 文件 | 说明 |
| --- | --- |
| `RenderedTextBlock` | 富文本块（外部 67 处） |
| `GameLog` | 游戏日志（外部 4 处） |
| `Toast` | Toast 提示 |
| `Tooltip` | 悬浮提示 |
| `Tag` | 标签 |
| `OptionSlider` | 选项滑条 |

## 图标资源（icon）

| 文件 | 说明 |
| --- | --- |
| `Icons` | 图标资源表（外部 52 处） |
| `HeroIcon` | 英雄图标（外部 62 处） |
| `TalentIcon` | 天赋图标 |
| `Archs` | 背景拱（外部 10 处） |
| `Chrome` | 边框 / 边角（外部 23 处） |

## 菜单（menu）

| 文件 | 说明 |
| --- | --- |
| `RightClickMenu` | 右键菜单（外部 3 处） |
| `RadialMenu` | 径向菜单 |

## 杂项（misc）

| 文件 | 说明 |
| --- | --- |
| `BusyIndicator` | 忙碌指示 |
| `Compass` | 罗盘 |
| `Banner` | 横幅 |
| `KeyDisplay` | 按键显示（外部 5 处） |
| `ExitButton` | 退出按钮（外部 10 处） |

## changelist/（已独立子包）

`com.zootdungeon.ui.changelist` — 变更日志窗口的内容与组件。

| 文件 | 说明 |
| --- | --- |
| `ChangeInfo` | 单条变更的描述数据 |
| `ChangeButton` | 变更按钮 |
| `WndChanges` | 经典变更窗口 |
| `WndChangesTabbed` | Tab 版变更窗口 |
| `v0_1_X_Changes` … `v3_X_Changes` | 各版本变更内容数据 |
