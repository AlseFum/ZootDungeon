package com.zootdungeon.windows;

import java.util.Map;

import com.zootdungeon.items.Item;
import com.zootdungeon.items.armor.Armor;
import com.zootdungeon.items.weapon.Weapon;
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
import com.watabou.utils.Reflection;

public class WndItemEditor extends Window {

    private static final int MIN_WIDTH = 140;
    private static final int MAX_WIDTH = 240;
    private static final int GAP = 2;
    private static final int BTN_HEIGHT = 18;

    private final Item item;
    private final int windowW;
    private final ScrollPane scrollPane;
    private final Component content;

    public WndItemEditor(Item item) {
        super();
        this.item = item;

        int screenW = (int) PixelScene.uiCamera.width;
        int screenH = (int) PixelScene.uiCamera.height;

        int maxWidth = Math.min(MAX_WIDTH, screenW - 20);
        windowW = Math.max(MIN_WIDTH, maxWidth);

        content = new Component();
        scrollPane = new InteractiveScrollPane(content);
        add(scrollPane);

        float pos = buildContent(windowW);

        content.setSize(windowW, pos);

        int maxHeight = screenH - chrome.marginVer() - 20;
        int windowH = (int) Math.min(pos + chrome.marginTop() + chrome.marginBottom(), maxHeight);

        resize(windowW, windowH);
        scrollPane.setRect(chrome.marginLeft(), chrome.marginTop(),
                width - chrome.marginHor(), height - chrome.marginVer());
    }

    private float buildContent(int w) {
        float y = 0;

        // Title
        IconTitle title = new IconTitle(new ItemSprite(item), Messages.titleCase(item.name()));
        title.setRect(0, 0, w, 0);
        title.setPos(0, 0);
        content.add(title);
        y = title.bottom() + GAP;

        // Info
        y = addInfoRow("已鉴定", item.isIdentified() ? "是" : "否", y);

        // --- Config entries ---
        Map<String, Object> config = item.getConfig();
        if (!config.isEmpty()) {
            y = addSectionHeader("属性", y + GAP);
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Runnable) {
                    y = addButtonRow(key, y, windowW - 40, (Runnable) value);
                } else if (value instanceof Boolean) {
                    y = addBoolField(key, (Boolean) value, y);
                } else if (value instanceof Integer) {
                    y = addIntField(key, (Integer) value, y);
                } else if (value instanceof Float) {
                    y = addFloatField(key, (Float) value, y);
                } else if (value instanceof String) {
                    y = addStringField(key, (String) value, y);
                } else {
                    y = addInfoRow(key, String.valueOf(value), y);
                }
            }
        }

        return y;
    }

    // ---- UI helpers ----

    private float addSectionHeader(String text, float y) {
        RenderedTextBlock block = PixelScene.renderTextBlock(text, 7);
        block.hardlight(0xFFFF44);
        block.setPos(0, y);
        content.add(block);
        return block.bottom() + GAP;
    }

    private float addInfoRow(String label, String value, float y) {
        RenderedTextBlock block = PixelScene.renderTextBlock(label + ":  " + value, 6);
        block.maxWidth(windowW - 40);
        block.setPos(0, y);
        content.add(block);
        return block.bottom() + GAP;
    }

    private float addButtonRow(String label, float y, float btnW, Runnable onClick) {
        RedButton btn = new RedButton(label, 6) {
            @Override
            protected void onClick() {
                onClick.run();
            }
        };
        btn.setRect(0, y, btnW, BTN_HEIGHT);
        content.add(btn);
        return btn.bottom() + GAP;
    }

    // ---- Config field editors ----

    private float addIntField(String key, int value, float y) {
        return addConfigRow(key, String.valueOf(value), y,
                () -> editInt(key, value));
    }

    private float addFloatField(String key, float value, float y) {
        return addConfigRow(key, String.valueOf(value), y,
                () -> editFloat(key, value));
    }

    private float addStringField(String key, String value, float y) {
        return addConfigRow(key, value, y,
                () -> editString(key, value));
    }

    private float addBoolField(String key, boolean value, float y) {
        CheckBox cb = new CheckBox(key) {
            @Override
            protected void onClick() {
                super.onClick();
                item.setConfig(key, checked());
                GLog.p(key + " = " + checked());
            }
        };
        cb.checked(value);
        cb.setRect(0, y, windowW - 40, BTN_HEIGHT);
        content.add(cb);
        return cb.bottom() + GAP;
    }

    private float addConfigRow(String label, String displayValue, float y, Runnable onTap) {
        float labelW = (windowW - 40) * 0.45f;
        float btnW = windowW - 40 - labelW - GAP;

        RenderedTextBlock lb = PixelScene.renderTextBlock(label, 6);
        lb.maxWidth((int) labelW);
        content.add(lb);

        RedButton btn = new RedButton(displayValue, 6) {
            @Override
            protected void onClick() {
                onTap.run();
            }
        };
        btn.setRect(labelW + GAP, y, btnW, BTN_HEIGHT);
        content.add(btn);

        float lbY = y + (BTN_HEIGHT - lb.height()) / 2f;
        lb.setPos(0, lbY);

        return btn.bottom() + GAP;
    }

    private void editInt(String key, int cur) {
        GameScene.show(new WndTextInput("编辑 " + key, "当前值: " + cur, String.valueOf(cur),
                10, false, "确定", "取消") {
            @Override
            public void onSelect(boolean positive, String text) {
                if (!positive) return;
                try {
                    int v = Integer.parseInt(text);
                    item.setConfig(key, v);
                    GLog.p(key + " = " + v);
                    // rebuild
                    refresh();
                } catch (Exception e) {
                    GLog.w("无效数字: " + text);
                }
            }
        });
    }

    private void editFloat(String key, float cur) {
        GameScene.show(new WndTextInput("编辑 " + key, "当前值: " + cur, String.valueOf(cur),
                20, false, "确定", "取消") {
            @Override
            public void onSelect(boolean positive, String text) {
                if (!positive) return;
                try {
                    float v = Float.parseFloat(text);
                    item.setConfig(key, v);
                    GLog.p(key + " = " + v);
                    refresh();
                } catch (Exception e) {
                    GLog.w("无效数字: " + text);
                }
            }
        });
    }

    private void editString(String key, String cur) {
        GameScene.show(new WndTextInput("编辑 " + key, "当前值: " + cur, String.valueOf(cur),
                50, false, "确定", "取消") {
            @Override
            public void onSelect(boolean positive, String text) {
                if (!positive) return;
                item.setConfig(key, text);
                GLog.p(key + " = " + text);
                refresh();
            }
        });
    }

    private void refresh() {
        // Close and reopen to reflect changes
        // int w = (int) width;
        // int h = (int) height;
        hide();
        GameScene.show(new WndItemEditor(item));
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

    /** Compact grid picker for enchantments / glyphs / curses. */
    public static class WndListSelect extends Window {

        private static final int COLS = 3;
        private static final int CELL_W = 60;
        private static final int GAP = 3;
        private static final int BTN_H = 22;
        private static final int FONT = 7;

        public WndListSelect(String title, String[] labels, java.util.List<Runnable> actions) {
            super();

            int screenH = (int) PixelScene.uiCamera.height;
            int screenW = (int) PixelScene.uiCamera.width;

            int cellW = CELL_W;
            int gw = COLS * (cellW + GAP) - GAP;
            int w = Math.min(gw, screenW - 20);

            Component content = new Component();
            float y = 0;

            // Title
            RenderedTextBlock titleBlock = PixelScene.renderTextBlock(title, 6);
            titleBlock.hardlight(0xFFFF44);
            titleBlock.setPos(0, y);
            content.add(titleBlock);
            y = titleBlock.bottom() + GAP;

            // Grid buttons
            for (int i = 0; i < labels.length; i++) {
                int col = i % COLS;
                int row = i / COLS;
                float bx = col * (cellW + GAP);
                float by = y + row * (BTN_H + GAP);
                final int idx = i;
                RedButton btn = new RedButton(labels[i], FONT) {
                    @Override
                    protected void onClick() {
                        hide();
                        actions.get(idx).run();
                    }
                };
                btn.setRect(bx, by, cellW, BTN_H);
                content.add(btn);
            }

            int rows = (labels.length + COLS - 1) / COLS;
            float contentH = y + rows * (BTN_H + GAP) - GAP;
            content.setSize(w, Math.max(contentH, 1));

            int maxH = screenH - chrome.marginVer() - 20;
            int h = (int) Math.min(contentH + chrome.marginTop() + chrome.marginBottom(), maxH);

            resize(w, h);
            if (contentH + chrome.marginTop() + chrome.marginBottom() > maxH) {
                ScrollPane sp = new ScrollPane(content);
                sp.setRect(chrome.marginLeft(), chrome.marginTop(),
                        w - chrome.marginHor(), h - chrome.marginVer());
                add(sp);
            } else {
                content.setPos(chrome.marginLeft(), chrome.marginTop());
                add(content);
            }
        }

        public static WndListSelect forWeaponEnchant(Weapon weapon) {
            Class<?>[] all = merge(
                    Weapon.Enchantment.common,
                    Weapon.Enchantment.uncommon,
                    Weapon.Enchantment.rare);
            String[] labels = new String[all.length];
            for (int i = 0; i < all.length; i++)
                labels[i] = Messages.get((Class<?>) all[i], "name", "");
            java.util.List<Runnable> actions = new java.util.ArrayList<>();
            for (int i = 0; i < all.length; i++) {
                final int idx = i;
                actions.add(() -> {
                    Weapon.Enchantment e = (Weapon.Enchantment) Reflection.newInstance(all[idx]);
                    weapon.enchant(e);
                    Item.updateQuickslot();
                });
            }
            return new WndListSelect("选择附魔", labels, actions);
        }

        public static WndListSelect forWeaponCurse(Weapon weapon) {
            Class<?>[] all = Weapon.Enchantment.curses;
            String[] labels = new String[all.length];
            for (int i = 0; i < all.length; i++)
                labels[i] = Messages.get((Class<?>) all[i], "name", "");
            java.util.List<Runnable> actions = new java.util.ArrayList<>();
            for (int i = 0; i < all.length; i++) {
                final int idx = i;
                actions.add(() -> {
                    Weapon.Enchantment e = (Weapon.Enchantment) Reflection.newInstance(all[idx]);
                    weapon.enchant(e);
                    weapon.cursed = true;
                    weapon.cursedKnown = true;
                    Item.updateQuickslot();
                });
            }
            return new WndListSelect("选择诅咒", labels, actions);
        }

        public static WndListSelect forArmorGlyph(Armor armor) {
            Class<?>[] all = merge(
                    Armor.Glyph.common,
                    Armor.Glyph.uncommon,
                    Armor.Glyph.rare);
            String[] labels = new String[all.length];
            for (int i = 0; i < all.length; i++)
                labels[i] = Messages.get((Class<?>) all[i], "name", "");
            java.util.List<Runnable> actions = new java.util.ArrayList<>();
            for (int i = 0; i < all.length; i++) {
                final int idx = i;
                actions.add(() -> {
                    Armor.Glyph g = (Armor.Glyph) Reflection.newInstance(all[idx]);
                    armor.inscribe(g);
                    Item.updateQuickslot();
                });
            }
            return new WndListSelect("选择铭刻", labels, actions);
        }

        private static Class<?>[] merge(Class<?>[]... arrays) {
            int total = 0;
            for (Class<?>[] a : arrays) total += a.length;
            Class<?>[] result = new Class<?>[total];
            int pos = 0;
            for (Class<?>[] a : arrays) {
                System.arraycopy(a, 0, result, pos, a.length);
                pos += a.length;
            }
            return result;
        }
    }
}
