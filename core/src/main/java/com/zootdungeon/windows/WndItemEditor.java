package com.zootdungeon.windows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import com.zootdungeon.items.Item;
import com.zootdungeon.items.armor.Armor;
import com.zootdungeon.items.rings.Ring;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.items.weapon.Weapon;
import com.zootdungeon.items.weapon.ambushWeapon.AmbushWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.sprites.ItemSprite;
import com.zootdungeon.ui.CheckBox;
import com.zootdungeon.ui.RedButton;
import com.zootdungeon.ui.RenderedTextBlock;
import com.zootdungeon.ui.ScrollPane;
import com.zootdungeon.ui.Window;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;

/**
 * 物品编辑器窗口。
 */
public class WndItemEditor extends Window {

    private static final int MIN_WIDTH = 140;
    private static final int MAX_WIDTH = 240;
    private static final int GAP = 2;
    private static final int SECTION_COLOR = 0xFFFF44;

    private final Item item;
    private final Map<String, Object> configValues = new LinkedHashMap<>();
    private final int windowW;
    private final ScrollPane scrollPane;
    private final Component content;

    public WndItemEditor(Item item) {
        super();
        this.item = item;

        // 1. 获取屏幕尺寸
        int screenW = (int) PixelScene.uiCamera.width;
        int screenH = (int) PixelScene.uiCamera.height;

        // 2. 根据屏幕尺寸计算窗口宽度（屏幕宽度减去边距，两边各留 10 像素）
        int maxWidth = Math.min(MAX_WIDTH, screenW-20);
        windowW = Math.max(MIN_WIDTH, maxWidth);

        // 3. 创建 content 和 scrollPane
        content = new Component();
        scrollPane = new InteractiveScrollPane(content);
        add(scrollPane);

        // 4. 构建内容
        float pos = buildContent(windowW);

        // 5. 设置 content 尺寸
        content.setSize(windowW, pos);

        // 6. 计算窗口高度（减去 chrome 边距和上下边距各 10 像素）
        int maxHeight = screenH - chrome.marginVer() - 20;
        int windowH = (int) Math.min(pos + chrome.marginTop() + chrome.marginBottom(), maxHeight);

        // 7. resize + 定位
        resize(windowW, windowH);
        scrollPane.setRect(chrome.marginLeft(), chrome.marginTop(),
                width - chrome.marginHor(), height - chrome.marginVer());
    }

    private float buildContent(int w) {
        float y = 0;

        // Title at top of content
        IconTitle title = new IconTitle(new ItemSprite(item), Messages.titleCase(item.name()));
        title.setRect(0, 0, w, 0);
        title.setPos(0, 0);
        content.add(title);

        y = title.bottom() + GAP;

        y = addSectionHeader("基本信息", y);
        y = addInfoRow("类型", item.getClass().getSimpleName(), y);
        y = addInfoRow("已鉴定", item.isIdentified() ? "是" : "否", y);

        y = addSectionHeader("通用属性", y + GAP);
        y = addIntField("等级", "level", item.level(), y);
        y = addIntField("数量", "quantity", item.quantity(), y);
        y = addBooleanField("诅咒", "cursed", item.cursed, y);

        if (item instanceof Weapon) {
            Weapon weapon = (Weapon) item;
            y = addSectionHeader("武器属性", y + GAP);
            y = addIntFieldSafe("Tier", "tier", y);
            y = addBooleanField("附魔硬化", "enchantHardened", weapon.enchantHardened, y);
            y = addBooleanField("诅咒灌注", "curseInfusionBonus", weapon.curseInfusionBonus, y);
            y = addBooleanField("精通药剂", "masteryPotionBonus", weapon.masteryPotionBonus, y);
        } else if (item instanceof Armor) {
            y = addSectionHeader("护甲属性", y + GAP);
            y = addBooleanFieldSafe("附魔硬化", "enchantHardened", y);
            y = addBooleanFieldSafe("诅咒灌注", "curseInfusionBonus", y);
            y = addBooleanFieldSafe("精通药剂", "masteryPotionBonus", y);
        }

        if (item instanceof AmbushWeapon) {
            AmbushWeapon aw = (AmbushWeapon) item;
            y = addFloatField("突袭倍率", "ambushRate", aw.ambushRate, y);
        }

        if (item instanceof Wand) {
            Wand wand = (Wand) item;
            y = addSectionHeader("法杖属性", y + GAP);
            y = addIntField("充能", "curCharges", wand.curCharges, y);
        }

        if (item instanceof Ring) {
            y = addSectionHeader("戒指属性", y + GAP);
            y = addIntFieldSafe("等级加成", "levelBonus", y);
        }

        // Load config values
        try {
            Method getConfig = item.getClass().getMethod("getConfig");
            Object config = getConfig.invoke(item);
            if (config instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> configMap = (Map<String, Object>) config;
                configValues.clear();
                configValues.putAll(configMap);
            }
        } catch (Exception ignored) {
        }

        if (!configValues.isEmpty()) {
            y = addSectionHeader("配置项", y + GAP);
            for (Map.Entry<String, Object> entry : configValues.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Integer) {
                    y = addConfigIntField(key, (Integer) value, y);
                } else if (value instanceof Boolean) {
                    y = addConfigBoolField(key, (Boolean) value, y);
                } else if (value instanceof Float) {
                    y = addConfigFloatField(key, (Float) value, y);
                } else if (value instanceof String) {
                    y = addConfigStringField(key, (String) value, y);
                }
            }
        }

        return y;
    }

    private float addSectionHeader(String text, float y) {
        RenderedTextBlock block = PixelScene.renderTextBlock(text, 7);
        block.hardlight(SECTION_COLOR);
        block.setPos(0, y);
        content.add(block);
        return block.bottom() + GAP;
    }

    private float addInfoRow(String label, String value, float y) {
        RenderedTextBlock block = PixelScene.renderTextBlock(label + ":  " + value, 6);
        block.maxWidth(windowW-40);
        block.setPos(0, y);
        content.add(block);
        return block.bottom() + GAP;
    }

    @FunctionalInterface
    private interface EditAction {
        void edit(RedButton button);
    }

    @FunctionalInterface
    private interface BoolAction {
        void apply(boolean value);
    }

    private float addRow(String label, String value, float y, EditAction edit) {
        float labelW = (windowW-40) * 0.45f;
        float btnW = windowW -40- labelW - GAP;

        RenderedTextBlock lb = PixelScene.renderTextBlock(label, 6);
        lb.maxWidth((int) labelW);
        content.add(lb);

        final RedButton[] holder = new RedButton[1];
        RedButton btn = new RedButton(value, 6) {
            @Override
            protected void onClick() {
                edit.edit(holder[0]);
            }
        };
        holder[0] = btn;
        btn.setRect(labelW + GAP, y, btnW, 16);
        content.add(btn);

        float lbY = y + (16 - lb.height()) / 2f;
        lb.setPos(0, lbY);

        return btn.bottom() + GAP;
    }

    private float addCheckBoxRow(String label, boolean initial, float y, BoolAction onChange) {
        CheckBox cb = new CheckBox(label) {
            @Override
            protected void onClick() {
                super.onClick();
                onChange.apply(checked());
            }
        };
        cb.checked(initial);
        cb.setRect(0, y, windowW-40, 16);
        content.add(cb);
        return cb.bottom() + GAP;
    }

    private float addIntField(String label, String fieldName, int currentValue, float y) {
        Field field = findField(item.getClass(), fieldName);
        if (field == null) return y;
        return addRow(label, String.valueOf(currentValue), y,
                btn -> editIntField(field, btn, label));
    }

    private float addIntFieldSafe(String label, String fieldName, float y) {
        Field field = findField(item.getClass(), fieldName);
        if (field == null) return y;
        try {
            field.setAccessible(true);
            Object cur = field.get(item);
            return addRow(label, String.valueOf(cur), y,
                    btn -> editIntField(field, btn, label));
        } catch (Exception e) {
            return y;
        }
    }

    private float addFloatField(String label, String fieldName, float currentValue, float y) {
        Field field = findField(item.getClass(), fieldName);
        if (field == null) return y;
        return addRow(label, String.valueOf(currentValue), y,
                btn -> editFloatField(field, btn, label));
    }

    private float addBooleanField(String label, String fieldName, boolean currentValue, float y) {
        Field field = findField(item.getClass(), fieldName);
        if (field == null) return y;
        return addCheckBoxRow(label, currentValue, y, val -> {
            try {
                field.setAccessible(true);
                field.set(item, val);
                GLog.p("已将 " + label + " 设置为 " + (val ? "是" : "否"));
            } catch (Exception e) {
                GLog.w("设置失败: " + e.getMessage());
            }
        });
    }

    private float addBooleanFieldSafe(String label, String fieldName, float y) {
        Field field = findField(item.getClass(), fieldName);
        if (field == null) return y;
        try {
            field.setAccessible(true);
            Object cur = field.get(item);
            boolean on = cur instanceof Boolean ? (Boolean) cur : false;
            return addCheckBoxRow(label, on, y, val -> {
                try {
                    field.setAccessible(true);
                    field.set(item, val);
                    GLog.p("已将 " + label + " 设置为 " + (val ? "是" : "否"));
                } catch (Exception e) {
                    GLog.w("设置失败: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            return y;
        }
    }

    private float addConfigIntField(String key, int currentValue, float y) {
        return addRow(key, String.valueOf(currentValue), y,
                btn -> editConfigInt(key, btn));
    }

    private float addConfigBoolField(String key, boolean currentValue, float y) {
        return addCheckBoxRow(key, currentValue, y,
                val -> setConfig(key, val, "已将 " + key + " 设置为 " + (val ? "是" : "否")));
    }

    private float addConfigFloatField(String key, float currentValue, float y) {
        return addRow(key, String.valueOf(currentValue), y,
                btn -> editConfigFloat(key, btn));
    }

    private float addConfigStringField(String key, String currentValue, float y) {
        return addRow(key, currentValue, y,
                btn -> editConfigString(key, btn));
    }

    private void editIntField(Field field, RedButton btn, String label) {
        Object cur;
        try {
            field.setAccessible(true);
            cur = field.get(item);
        } catch (Exception e) {
            cur = 0;
        }
        GameScene.show(new WndTextInput("编辑 " + label, "当前值: " + cur, String.valueOf(cur),
                10, false, "确定", "取消") {
            @Override
            public void onSelect(boolean positive, String text) {
                if (!positive) return;
                try {
                    int v = Integer.parseInt(text);
                    field.setAccessible(true);
                    field.set(item, v);
                    btn.text(String.valueOf(v));
                    GLog.p("已将 " + label + " 设置为 " + v);
                } catch (Exception e) {
                    GLog.w("设置失败: " + e.getMessage());
                }
            }
        });
    }

    private void editFloatField(Field field, RedButton btn, String label) {
        Object cur;
        try {
            field.setAccessible(true);
            cur = field.get(item);
        } catch (Exception e) {
            cur = 0f;
        }
        GameScene.show(new WndTextInput("编辑 " + label, "当前值: " + cur, String.valueOf(cur),
                20, false, "确定", "取消") {
            @Override
            public void onSelect(boolean positive, String text) {
                if (!positive) return;
                try {
                    float v = Float.parseFloat(text);
                    field.setAccessible(true);
                    field.set(item, v);
                    btn.text(String.valueOf(v));
                    GLog.p("已将 " + label + " 设置为 " + v);
                } catch (Exception e) {
                    GLog.w("设置失败: " + e.getMessage());
                }
            }
        });
    }

    private void editConfigInt(String key, RedButton btn) {
        Object cur = configValues.get(key);
        GameScene.show(new WndTextInput("编辑 " + key, "当前值: " + cur, String.valueOf(cur),
                10, false, "确定", "取消") {
            @Override
            public void onSelect(boolean positive, String text) {
                if (!positive) return;
                try {
                    int v = Integer.parseInt(text);
                    setConfig(key, v, "已将 " + key + " 设置为 " + v);
                    btn.text(String.valueOf(v));
                } catch (Exception e) {
                    GLog.w("设置失败: " + e.getMessage());
                }
            }
        });
    }

    private void editConfigFloat(String key, RedButton btn) {
        Object cur = configValues.get(key);
        GameScene.show(new WndTextInput("编辑 " + key, "当前值: " + cur, String.valueOf(cur),
                20, false, "确定", "取消") {
            @Override
            public void onSelect(boolean positive, String text) {
                if (!positive) return;
                try {
                    float v = Float.parseFloat(text);
                    setConfig(key, v, "已将 " + key + " 设置为 " + v);
                    btn.text(String.valueOf(v));
                } catch (Exception e) {
                    GLog.w("设置失败: " + e.getMessage());
                }
            }
        });
    }

    private void editConfigString(String key, RedButton btn) {
        Object cur = configValues.get(key);
        GameScene.show(new WndTextInput("编辑 " + key, "当前值: " + cur, String.valueOf(cur),
                50, false, "确定", "取消") {
            @Override
            public void onSelect(boolean positive, String text) {
                if (!positive) return;
                setConfig(key, text, "已将 " + key + " 设置为 " + text);
                btn.text(text);
            }
        });
    }

    private void setConfig(String key, Object value, String successMsg) {
        try {
            Method setConfig = item.getClass().getMethod("setConfig", String.class, Object.class);
            setConfig.invoke(item, key, value);
            configValues.put(key, value);
            GLog.p(successMsg);
        } catch (NoSuchMethodException e) {
            GLog.w("该物品不支持 setConfig 方法");
        } catch (Exception e) {
            GLog.w("设置失败: " + e.getMessage());
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    @Override
    public void offset(int xOffset, int yOffset) {
        super.offset(xOffset, yOffset);
        if (scrollPane != null) {
            scrollPane.setPos(scrollPane.left(), scrollPane.top());
        }
    }

    private static class InteractiveScrollPane extends ScrollPane {
        InteractiveScrollPane(Component content) {
            super(content);
            if (controller != null) {
                controller.blockLevel = PointerArea.NEVER_BLOCK;
            }
        }
    }
}
