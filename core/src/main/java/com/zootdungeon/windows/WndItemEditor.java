package com.zootdungeon.windows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
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
import com.watabou.noosa.Camera;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;

/**
 * 物品编辑器窗口。
 *
 * 显示优化要点（兼顾安卓竖屏的小屏幕）：
 * <ul>
 *   <li>窗口宽度按屏幕动态裁切，最大不超过 {@link #PREFERRED_W}，
 *       且至少留 {@code edge} 像素安全边距，避免在窄屏被裁掉。</li>
 *   <li>内容超过最大高度时自动启用 {@link ScrollPane} 滚动，
 *       标题保持固定可见，不随内容滚走。</li>
 *   <li>布尔字段使用 {@link CheckBox}，单击直接切换，
 *       不再弹窗或重建整个窗口；触摸目标更大、反馈更明确。</li>
 *   <li>数值/字符串字段使用「左侧标签 + 右侧按钮」两栏布局，
 *       让所有字段在一行内展示，避免长 label 把按钮挤下一行。</li>
 *   <li>编辑成功后只刷新对应按钮文本，不再重新打开窗口
 *       （之前每次编辑都会丢失滚动位置，对手机用户尤为不便）。</li>
 * </ul>
 */
public class WndItemEditor extends Window {

    /** 期望宽度上限；屏幕过窄时按屏幕宽度收缩。 */
    private static final int PREFERRED_W = 160;
    /** 内容高度上限；超过则启用滚动。 */
    private static final int MAX_CONTENT_HEIGHT = 180;

    private static final int BTN_HEIGHT = 16;
    private static final int ROW_HEIGHT = 16;
    private static final int GAP = 2;
    private static final int MARGIN = 2;

    /** 分节标题颜色（亮黄色，便于在低分辨率下区分组）。 */
    private static final int SECTION_COLOR = 0xFFFF44;

    private final Item item;
    private final Map<String, Object> configValues = new HashMap<>();
    private int layoutW;

    public WndItemEditor(Item item) {
        super();

        this.item = item;
        layoutW = computeLayoutWidth();
        int maxWindowH = computeMaxWindowHeight();

        IconTitle title = new IconTitle(new ItemSprite(item), Messages.titleCase(item.name()));
        title.setRect(0, 0, layoutW, 0);
        add(title);
        float topY = title.bottom() + GAP;

        try {
            Method getConfig = item.getClass().getMethod("getConfig");
            Object config = getConfig.invoke(item);
            if (config instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> configMap = (Map<String, Object>) config;
                configValues.putAll(configMap);
            }
        } catch (Exception ignored) {
            // 不支持 getConfig 即跳过配置项展示。
        }

        Component body = buildBody();

        int availH = maxWindowH - (int) topY - MARGIN;
        availH = Math.max(60, availH);
        int bodyH = (int) body.height();
        boolean needScroll = bodyH > availH || bodyH > MAX_CONTENT_HEIGHT;

        if (needScroll) {
            int scrollH = Math.min(bodyH, Math.min(MAX_CONTENT_HEIGHT, availH));
            ScrollPane scroll = new InteractiveScrollPane(body);
            add(scroll);
            scroll.setRect(0, topY, layoutW, scrollH);
            resize(layoutW, (int) (topY + scrollH + MARGIN));
        } else {
            add(body);
            body.setPos(0, topY);
            resize(layoutW, (int) (topY + bodyH + MARGIN));
        }

        boundOffsetWithMargin(3);
    }

    private static int computeLayoutWidth() {
        Camera cam = PixelScene.uiCamera.visible ? PixelScene.uiCamera : Camera.main;
        int screenW = (int) cam.width;
        int edge = 16;
        return Math.max(120, Math.min(PREFERRED_W, screenW - edge));
    }

    private static int computeMaxWindowHeight() {
        Camera cam = PixelScene.uiCamera.visible ? PixelScene.uiCamera : Camera.main;
        return Math.max(80, (int) cam.height - 24);
    }

    private Component buildBody() {
        Component body = new Component();
        float y = 0;

        y = addSectionHeader(body, "基本信息", y);
        y = addInfoRow(body, "类型", item.getClass().getSimpleName(), y);
        y = addInfoRow(body, "已鉴定", item.isIdentified() ? "是" : "否", y);

        y = addSectionHeader(body, "通用属性", y + GAP);
        y = addIntField(body, "等级", "level", item.level(), y);
        y = addIntField(body, "数量", "quantity", item.quantity(), y);
        y = addBooleanField(body, "诅咒", "cursed", item.cursed, y);

        if (item instanceof Weapon) {
            Weapon weapon = (Weapon) item;
            y = addSectionHeader(body, "武器属性", y + GAP);
            y = addIntFieldSafe(body, "Tier", "tier", y);
            y = addBooleanField(body, "附魔硬化", "enchantHardened", weapon.enchantHardened, y);
            y = addBooleanField(body, "诅咒灌注加成", "curseInfusionBonus", weapon.curseInfusionBonus, y);
            y = addBooleanField(body, "精通药剂加成", "masteryPotionBonus", weapon.masteryPotionBonus, y);
        } else if (item instanceof Armor) {
            y = addSectionHeader(body, "护甲属性", y + GAP);
            y = addBooleanFieldSafe(body, "附魔硬化", "enchantHardened", y);
            y = addBooleanFieldSafe(body, "诅咒灌注加成", "curseInfusionBonus", y);
            y = addBooleanFieldSafe(body, "精通药剂加成", "masteryPotionBonus", y);
        }

        if (item instanceof AmbushWeapon) {
            AmbushWeapon aw = (AmbushWeapon) item;
            y = addFloatField(body, "突袭倍率加成", "ambushRate", aw.ambushRate, y);
        }

        if (item instanceof Wand) {
            Wand wand = (Wand) item;
            y = addSectionHeader(body, "法杖属性", y + GAP);
            y = addIntField(body, "充能", "curCharges", wand.curCharges, y);
        }

        if (item instanceof Ring) {
            y = addSectionHeader(body, "戒指属性", y + GAP);
            y = addIntFieldSafe(body, "等级加成", "levelBonus", y);
        }

        if (!configValues.isEmpty()) {
            y = addSectionHeader(body, "配置项", y + GAP);
            for (Map.Entry<String, Object> entry : configValues.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Integer) {
                    y = addConfigIntField(body, key, (Integer) value, y);
                } else if (value instanceof Boolean) {
                    y = addConfigBoolField(body, key, (Boolean) value, y);
                } else if (value instanceof Float) {
                    y = addConfigFloatField(body, key, (Float) value, y);
                } else if (value instanceof String) {
                    y = addConfigStringField(body, key, (String) value, y);
                }
            }
        }

        body.setSize(layoutW, y);
        return body;
    }

    // ---- 行布局辅助 ----

    private float addSectionHeader(Component body, String text, float y) {
        RenderedTextBlock block = PixelScene.renderTextBlock(text, 7);
        block.hardlight(SECTION_COLOR);
        block.setPos(MARGIN, y);
        body.add(block);
        return block.bottom() + GAP;
    }

    private float addInfoRow(Component body, String label, String value, float y) {
        RenderedTextBlock block = PixelScene.renderTextBlock(label + ":  " + value, 6);
        block.maxWidth(layoutW - MARGIN * 2);
        block.setPos(MARGIN, y);
        body.add(block);
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

    /**
     * 标签 + 编辑按钮的两栏行：左侧约 45% 显示标签，右侧约 55% 显示按钮。
     * 这样长 label 不会把按钮挤掉，按钮也保有足够触摸面积。
     */
    private float addEditButtonRow(Component body, String label, String value, float y, EditAction edit) {
        int colW = layoutW - MARGIN * 2;
        int labelW = (int) Math.floor(colW * 0.45f);
        int btnW = colW - labelW - GAP;

        RenderedTextBlock lb = PixelScene.renderTextBlock(label, 6);
        lb.maxWidth(labelW);
        body.add(lb);

        final RedButton[] holder = new RedButton[1];
        RedButton btn = new RedButton(value, 6) {
            @Override
            protected void onClick() {
                edit.edit(holder[0]);
            }
        };
        holder[0] = btn;
        btn.setRect(MARGIN + labelW + GAP, y, btnW, BTN_HEIGHT);
        body.add(btn);

        // 标签相对按钮垂直居中
        float lbY = y + (BTN_HEIGHT - lb.height()) / 2f;
        lb.setPos(MARGIN, lbY);

        return btn.bottom() + GAP;
    }

    private float addCheckBoxRow(Component body, String label, boolean initial, float y, BoolAction onChange) {
        int colW = layoutW - MARGIN * 2;
        CheckBox cb = new CheckBox(label) {
            @Override
            protected void onClick() {
                super.onClick();
                onChange.apply(checked());
            }
        };
        cb.checked(initial);
        cb.setRect(MARGIN, y, colW, ROW_HEIGHT);
        body.add(cb);
        return cb.bottom() + GAP;
    }

    // ---- 字段适配 ----

    private float addIntField(Component body, String label, String fieldName, int currentValue, float y) {
        Field field = findField(item.getClass(), fieldName);
        if (field == null) {
            return y;
        }
        return addEditButtonRow(body, label, String.valueOf(currentValue), y,
                btn -> editIntField(field, btn, label));
    }

    private float addIntFieldSafe(Component body, String label, String fieldName, float y) {
        Field field = findField(item.getClass(), fieldName);
        if (field == null) {
            return y;
        }
        try {
            field.setAccessible(true);
            Object cur = field.get(item);
            return addEditButtonRow(body, label, String.valueOf(cur), y,
                    btn -> editIntField(field, btn, label));
        } catch (Exception e) {
            return y;
        }
    }

    private float addFloatField(Component body, String label, String fieldName, float currentValue, float y) {
        Field field = findField(item.getClass(), fieldName);
        if (field == null) {
            return y;
        }
        return addEditButtonRow(body, label, String.valueOf(currentValue), y,
                btn -> editFloatField(field, btn, label));
    }

    private float addBooleanField(Component body, String label, String fieldName, boolean currentValue, float y) {
        Field field = findField(item.getClass(), fieldName);
        if (field == null) {
            return y;
        }
        return addCheckBoxRow(body, label, currentValue, y, val -> {
            try {
                field.setAccessible(true);
                field.set(item, val);
                GLog.p("已将 " + label + " 设置为 " + (val ? "是" : "否"));
            } catch (Exception e) {
                GLog.w("设置失败: " + e.getMessage());
            }
        });
    }

    private float addBooleanFieldSafe(Component body, String label, String fieldName, float y) {
        Field field = findField(item.getClass(), fieldName);
        if (field == null) {
            return y;
        }
        try {
            field.setAccessible(true);
            Object cur = field.get(item);
            boolean on = cur instanceof Boolean ? (Boolean) cur : false;
            return addCheckBoxRow(body, label, on, y, val -> {
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

    private float addConfigIntField(Component body, String key, int currentValue, float y) {
        return addEditButtonRow(body, key, String.valueOf(currentValue), y,
                btn -> editConfigInt(key, btn));
    }

    private float addConfigBoolField(Component body, String key, boolean currentValue, float y) {
        return addCheckBoxRow(body, key, currentValue, y,
                val -> setConfig(key, val, "已将 " + key + " 设置为 " + (val ? "是" : "否")));
    }

    private float addConfigFloatField(Component body, String key, float currentValue, float y) {
        return addEditButtonRow(body, key, String.valueOf(currentValue), y,
                btn -> editConfigFloat(key, btn));
    }

    private float addConfigStringField(Component body, String key, String currentValue, float y) {
        return addEditButtonRow(body, key, currentValue, y,
                btn -> editConfigString(key, btn));
    }

    // ---- 输入对话 ----

    private void editIntField(Field field, RedButton btn, String label) {
        Object cur;
        try {
            field.setAccessible(true);
            cur = field.get(item);
        } catch (Exception e) {
            cur = 0;
        }
        final Object curRef = cur;
        GameScene.show(new WndTextInput("编辑 " + label, "当前值: " + curRef, String.valueOf(curRef),
                10, false, "确定", "取消") {
            @Override
            public void onSelect(boolean positive, String text) {
                if (!positive) {
                    return;
                }
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
        final Object curRef = cur;
        GameScene.show(new WndTextInput("编辑 " + label, "当前值: " + curRef, String.valueOf(curRef),
                20, false, "确定", "取消") {
            @Override
            public void onSelect(boolean positive, String text) {
                if (!positive) {
                    return;
                }
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
                if (!positive) {
                    return;
                }
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
                if (!positive) {
                    return;
                }
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
                if (!positive) {
                    return;
                }
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

    /** 滚动条默认会吞掉指针事件，下方的按钮/CheckBox 因此点不到；让事件继续向下传递。 */
    private static class InteractiveScrollPane extends ScrollPane {
        InteractiveScrollPane(Component content) {
            super(content);
            if (controller != null) {
                controller.blockLevel = PointerArea.NEVER_BLOCK;
            }
        }
    }
}
