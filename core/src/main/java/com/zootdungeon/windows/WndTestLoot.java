package com.zootdungeon.windows;

import com.zootdungeon.Dungeon;
import com.zootdungeon.items.EquipableItem;
import com.zootdungeon.items.Generator;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.LootRegistry;
import com.zootdungeon.items.LootRegistry.LootArgs;
import com.zootdungeon.items.LootRegistry.LootHistory;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.ui.*;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.ui.Component;

import java.util.*;

/**
 * Debug 工具：测试 Loot 系统的抽取效果。
 * 支持：
 * - 按 Category 抽取
 * - 按 LootTable 抽取
 * - 设置 luck/quality 参数
 * - 批量抽取并统计
 * - 查看 Deck 状态
 */
public class WndTestLoot extends Window {

    private static final int SCREEN_EDGE_MARGIN = 24;
    private static final int GAP = 2;

    private static final int DEFAULT_WIDTH = 180;
    private static final int MIN_WIDTH = 140;
    private static final int MAX_WIDTH = 240;

    private final ScrollPane scrollPane;
    private final Component content;
    private final LootHistory history;

    // 当前配置
    private String selectedTable = "";
    private int rolls = 1;
    private int luck = 0;
    private int qualityBonus = 0;

    // 统计
    private final Map<String, Integer> stats = new LinkedHashMap<>();
    private int totalRolls = 0;

    public WndTestLoot() {
        super();

        history = new LootHistory();

        content = new Component();
        scrollPane = new ScrollPane(content);
        add(scrollPane);

        // 计算窗口尺寸（考虑屏幕尺寸和 chrome 边距）
        int screenW = (int) PixelScene.uiCamera.width;
        int screenH = (int) PixelScene.uiCamera.height;

        // 窗口宽度：根据屏幕宽度自适应，窄屏时用满宽度
        int maxByScreen = screenW - 2 * SCREEN_EDGE_MARGIN - chrome.marginHor();
        int windowW = DEFAULT_WIDTH;
        if (screenW < 300) {
            // 窄屏：使用可用最大宽度
            windowW = Math.max(MIN_WIDTH, maxByScreen);
        } else {
            // 正常屏：限制最大宽度
            windowW = Math.min(MAX_WIDTH, maxByScreen);
        }
        windowW = Math.max(MIN_WIDTH, windowW);

        // 构建内容（使用计算后的窗口宽度）
        float pos = buildUI(windowW);
        content.setSize(windowW, pos);

        // 窗口高度：根据内容高度和屏幕高度计算
        int contentH = (int) pos;
        int maxHByScreen = screenH - 2 * SCREEN_EDGE_MARGIN - chrome.marginVer();
        int windowH = contentH + chrome.marginTop() + chrome.marginBottom();
        windowH = Math.min(windowH, maxHByScreen);

        // 设置尺寸并定位
        resize(windowW, windowH);
        scrollPane.setRect(chrome.marginLeft(), chrome.marginTop(),
                width - chrome.marginHor(), height - chrome.marginVer());

        // 确保窗口在屏幕范围内
        boundOffsetWithMargin(SCREEN_EDGE_MARGIN);
    }

    private float buildUI(int windowW) {
        float pos = GAP;

        // 标题
        RenderedTextBlock title = PixelScene.renderTextBlock(Messages.titleCase("Debug Supply"), 9);
        title.hardlight(TITLE_COLOR);
        title.setPos(GAP, pos);
        title.maxWidth(windowW - GAP * 2);
        content.add(title);
        pos = title.bottom() + GAP * 2;

        // ---- 1. 按 Category 抽取 ----
        pos = addSectionHeader("Category 抽取 (Generator)", pos, windowW);
        pos = addCategoryButtons(pos, windowW);

        // ---- 2. 按 LootTable 抽取 ----
        pos = addSectionHeader("LootTable 抽取", pos, windowW);
        pos = addTableSelector(pos, windowW);

        // ---- 3. 参数设置 ----
        pos = addSectionHeader("参数设置", pos, windowW);
        pos = addParameterControls(pos, windowW);

        // ---- 4. 批量抽取 ----
        pos = addSectionHeader("批量抽取", pos, windowW);
        pos = addBatchControls(pos, windowW);

        // ---- 5. 统计结果 ----
        pos = addSectionHeader("统计结果", pos, windowW);
        pos = addStatsDisplay(pos, windowW);

        // ---- 6. 操作按钮 ----
        pos = addActionButtons(pos, windowW);

        return pos;
    }

    private float addSectionHeader(String text, float pos, int windowW) {
        ColorBlock divider = new ColorBlock(windowW - GAP * 2, 1, 0xFF444444);
        divider.x = GAP;
        divider.y = pos;
        content.add(divider);

        RenderedTextBlock header = PixelScene.renderTextBlock(text, 9);
        header.hardlight(0xFFFF44);
        header.setPos(GAP, pos + GAP);
        content.add(header);

        return header.bottom() + GAP * 2;
    }

    private float addCategoryButtons(float pos, int windowW) {
        // 使用 Generator.Category
        Generator.Category[] categories = {
            Generator.Category.POTION,
            Generator.Category.SCROLL,
            Generator.Category.SEED,
            Generator.Category.FOOD,
            Generator.Category.WEAPON,
            Generator.Category.ARMOR,
            Generator.Category.WAND,
            Generator.Category.RING,
            Generator.Category.ARTIFACT,
            Generator.Category.TRINKET,
            Generator.Category.GOLD
        };

        float x = GAP;
        float y = pos;
        float rowHeight = 18;
        float btnWidth = (windowW - GAP * 4) / 3;

        for (Generator.Category cat : categories) {
            String name = cat.name();
            RedButton btn = new RedButton(name, 8) {
                @Override
                protected void onClick() {
                    rollFromCategory(cat);
                }
            };
            btn.setRect(x, y, btnWidth, rowHeight);
            content.add(btn);

            x += btnWidth + GAP;
            if (x + btnWidth > windowW - GAP) {
                x = GAP;
                y += rowHeight + GAP;
            }
        }

        return y + rowHeight + GAP;
    }

    private float addTableSelector(float pos, int windowW) {
        // 获取所有已注册的 LootTable
        List<String> tableIds = new ArrayList<>(LootRegistry.TABLES.keySet());
        if (tableIds.isEmpty()) {
            RenderedTextBlock noTables = PixelScene.renderTextBlock("(无已注册的 LootTable)", 8);
            noTables.setPos(GAP, pos);
            noTables.hardlight(0x888888);
            content.add(noTables);
            return noTables.bottom() + GAP;
        }

        // 显示前10个表
        int shown = Math.min(tableIds.size(), 10);
        for (int i = 0; i < shown; i++) {
            String tableId = tableIds.get(i);
            RedButton btn = new RedButton(tableId.length() > 20 ? tableId.substring(0, 17) + "..." : tableId, 8) {
                @Override
                protected void onClick() {
                    selectedTable = tableId;
                    GLog.i("已选择: " + tableId);
                }
            };
            btn.setRect(GAP, pos, windowW - GAP * 2, 16);
            content.add(btn);
            pos += 18;
        }

        if (tableIds.size() > 10) {
            RenderedTextBlock more = PixelScene.renderTextBlock("... 还有 " + (tableIds.size() - 10) + " 个表", 8);
            more.setPos(GAP, pos);
            more.hardlight(0x888888);
            content.add(more);
            pos += 16;
        }

        return pos + GAP;
    }

    private float addParameterControls(float pos, int windowW) {
        int minusX = windowW - 80;
        int valueX = windowW - 55;

        // Luck
        RenderedTextBlock luckLabel = PixelScene.renderTextBlock("Luck:", 8);
        luckLabel.setPos(GAP, pos);
        content.add(luckLabel);

        RedButton luckMinus = new RedButton("-", 10) {
            @Override
            protected void onClick() {
                luck = Math.max(0, luck - 1);
            }
        };
        luckMinus.setRect(minusX, pos, 20, 16);
        content.add(luckMinus);

        RenderedTextBlock luckValue = PixelScene.renderTextBlock(String.valueOf(luck), 8);
        luckValue.setPos(valueX, pos);
        content.add(luckValue);

        RedButton luckPlus = new RedButton("+", 10) {
            @Override
            protected void onClick() {
                luck++;
            }
        };
        luckPlus.setRect(windowW - 35, pos, 20, 16);
        content.add(luckPlus);

        pos += 18;

        // Quality Bonus
        RenderedTextBlock qualLabel = PixelScene.renderTextBlock("Quality:", 8);
        qualLabel.setPos(GAP, pos);
        content.add(qualLabel);

        RedButton qualMinus = new RedButton("-", 10) {
            @Override
            protected void onClick() {
                qualityBonus = Math.max(0, qualityBonus - 1);
            }
        };
        qualMinus.setRect(minusX, pos, 20, 16);
        content.add(qualMinus);

        RenderedTextBlock qualValue = PixelScene.renderTextBlock(String.valueOf(qualityBonus), 8);
        qualValue.setPos(valueX, pos);
        content.add(qualValue);

        RedButton qualPlus = new RedButton("+", 10) {
            @Override
            protected void onClick() {
                qualityBonus++;
            }
        };
        qualPlus.setRect(windowW - 35, pos, 20, 16);
        content.add(qualPlus);

        pos += 18;

        // Roll Count
        RenderedTextBlock rollLabel = PixelScene.renderTextBlock("Rolls:", 8);
        rollLabel.setPos(GAP, pos);
        content.add(rollLabel);

        RedButton rollMinus = new RedButton("-", 10) {
            @Override
            protected void onClick() {
                rolls = Math.max(1, rolls - 1);
            }
        };
        rollMinus.setRect(minusX, pos, 20, 16);
        content.add(rollMinus);

        RenderedTextBlock rollValue = PixelScene.renderTextBlock(String.valueOf(rolls), 8);
        rollValue.setPos(valueX, pos);
        content.add(rollValue);

        RedButton rollPlus = new RedButton("+", 10) {
            @Override
            protected void onClick() {
                rolls++;
            }
        };
        rollPlus.setRect(windowW - 35, pos, 20, 16);
        content.add(rollPlus);

        pos += 18;

        return pos + GAP;
    }

    private float addBatchControls(float pos, int windowW) {
        int halfW = (windowW - GAP * 3) / 2;

        // 单次抽取按钮
        RedButton rollOnce = new RedButton("抽取 1 次", 8) {
            @Override
            protected void onClick() {
                performRoll(1);
            }
        };
        rollOnce.setRect(GAP, pos, halfW, 18);
        content.add(rollOnce);

        RedButton rollTen = new RedButton("抽取 10 次", 8) {
            @Override
            protected void onClick() {
                performRoll(10);
            }
        };
        rollTen.setRect(GAP * 2 + halfW, pos, halfW, 18);
        content.add(rollTen);

        pos += 20;

        RedButton rollHundred = new RedButton("抽取 100 次", 8) {
            @Override
            protected void onClick() {
                performRoll(100);
            }
        };
        rollHundred.setRect(GAP, pos, halfW, 18);
        content.add(rollHundred);

        RedButton rollThousand = new RedButton("抽取 1000 次", 8) {
            @Override
            protected void onClick() {
                performRoll(1000);
            }
        };
        rollThousand.setRect(GAP * 2 + halfW, pos, halfW, 18);
        content.add(rollThousand);

        pos += 20;

        return pos + GAP;
    }

    private float addStatsDisplay(float pos, int windowW) {
        if (stats.isEmpty()) {
            RenderedTextBlock noStats = PixelScene.renderTextBlock("(无统计数据)", 8);
            noStats.setPos(GAP, pos);
            noStats.hardlight(0x888888);
            content.add(noStats);
            return noStats.bottom() + GAP;
        }

        RenderedTextBlock totalLabel = PixelScene.renderTextBlock("总计: " + totalRolls + " 次抽取", 8);
        totalLabel.setPos(GAP, pos);
        totalLabel.hardlight(0xFFFFFF);
        content.add(totalLabel);
        pos += 14;

        // 按出现次数排序显示
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(stats.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int shown = 0;
        for (Map.Entry<String, Integer> e : sorted) {
            if (shown >= 15) break;
            String name = e.getKey();
            int count = e.getValue();
            float pct = totalRolls > 0 ? (count * 100f / totalRolls) : 0;

            RenderedTextBlock entry = PixelScene.renderTextBlock(
                String.format("%s: %d (%.1f%%)", name, count, pct), 8);
            entry.setPos(GAP, pos);
            content.add(entry);
            pos += 12;
            shown++;
        }

        if (sorted.size() > 15) {
            RenderedTextBlock more = PixelScene.renderTextBlock("... 还有 " + (sorted.size() - 15) + " 种物品", 8);
            more.setPos(GAP, pos);
            more.hardlight(0x888888);
            content.add(more);
            pos += 12;
        }

        return pos + GAP;
    }

    private float addActionButtons(float pos, int windowW) {
        int halfW = (windowW - GAP * 3) / 2;

        RedButton clearStats = new RedButton("清除统计", 8) {
            @Override
            protected void onClick() {
                stats.clear();
                totalRolls = 0;
            }
        };
        clearStats.setRect(GAP, pos, halfW, 18);
        content.add(clearStats);

        RedButton resetAll = new RedButton("重置全部", 8) {
            @Override
            protected void onClick() {
                stats.clear();
                totalRolls = 0;
                history.reset();
            }
        };
        resetAll.setRect(GAP * 2 + halfW, pos, halfW, 18);
        content.add(resetAll);

        return pos + 20 + GAP;
    }

    private void rollFromCategory(Generator.Category category) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < rolls; i++) {
            Item item = Generator.randomUsingDefaults(category);
            if (item != null) {
                items.add(item);
            }
        }

        if (items.isEmpty()) {
            GLog.w("Category 抽取失败: " + category.name());
            return;
        }

        displayItems(items, "Category: " + category.name());
        updateStats(items);
    }

    private void performRoll(int count) {
        if (selectedTable.isEmpty()) {
            GLog.w("请先选择一个 LootTable");
            return;
        }

        LootArgs args = LootArgs.create()
            .depth(Dungeon.depth)
            .luck(luck)
            .qualityBonus(qualityBonus)
            .history(history);

        List<Item> allItems = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            List<Item> items = LootRegistry.roll(selectedTable, args);
            allItems.addAll(items);
        }

        if (allItems.isEmpty()) {
            GLog.w("LootTable 抽取为空: " + selectedTable);
            return;
        }

        displayItems(allItems, "Table: " + selectedTable + " x" + count);
        updateStats(allItems);
    }

    private void updateStats(List<Item> items) {
        totalRolls += items.size();
        for (Item item : items) {
            String name = item.name();
            stats.merge(name, 1, Integer::sum);
        }
    }

    private void displayItems(List<Item> items, String source) {
        StringBuilder sb = new StringBuilder();
        sb.append(source).append(" (").append(items.size()).append(" items):\n");
        for (Item item : items) {
            sb.append("  - ").append(item.name());
            if (item instanceof EquipableItem) {
                sb.append(" [Lv").append(((EquipableItem) item).level()).append("]");
            }
            if (item.cursed) sb.append(" [诅咒]");
            sb.append("\n");
        }
        GLog.i(sb.toString());

        // 显示结果窗口
        GameScene.show(new WndOptions(
            Icons.get(Icons.INFO),
            source,
            formatItemList(items),
            Messages.get(Window.class, "continue")
        ));
    }

    private String formatItemList(List<Item> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(items.size(), 20); i++) {
            Item item = items.get(i);
            sb.append("- ").append(item.name());
            if (item instanceof EquipableItem) {
                sb.append(" [Lv").append(((EquipableItem) item).level()).append("]");
            }
            if (item.cursed) sb.append(" [CURSED]");
            sb.append("\n");
        }
        if (items.size() > 20) {
            sb.append("... and ").append(items.size() - 20).append(" more");
        }
        return sb.toString();
    }

    // ---- 静态便捷方法 ----

    public static void quickRoll(Generator.Category category, int count) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Item item = Generator.randomUsingDefaults(category);
            if (item != null) {
                items.add(item);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(category.name()).append(" x").append(count).append(":\n");
        for (Item item : items) {
            sb.append("- ").append(item.name()).append("\n");
        }
        GLog.i(sb.toString());
    }

    public static void quickRollTable(String tableId, int count) {
        LootHistory history = new LootHistory();
        LootArgs args = LootArgs.create()
            .depth(Dungeon.depth)
            .luck(0)
            .history(history);

        List<Item> allItems = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            allItems.addAll(LootRegistry.roll(tableId, args));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(tableId).append(" x").append(count).append(":\n");
        Map<String, Integer> counts = new HashMap<>();
        for (Item item : allItems) {
            counts.merge(item.name(), 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            sb.append("- ").append(e.getKey()).append(" x").append(e.getValue()).append("\n");
        }
        GLog.i(sb.toString());
    }
}
