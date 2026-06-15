package com.zootdungeon.windows;

import com.zootdungeon.items.Item;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.sprites.ItemSprite;
import com.zootdungeon.ui.Window;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.PointerArea;

import java.util.List;
import java.util.function.Consumer;

/**
 * 物品选择窗口 - 一行横排 ItemSprite，点击选择
 */
public class WndItemSelect extends Window {

    private static final int WIDTH_MIN = 120;
    private static final int WIDTH_MAX = 180;
    private static final int MARGIN = 2;

    private final Consumer<Item> onSelect;
    private final List<Item> items;

    public WndItemSelect(String title, List<Item> items, Consumer<Item> onSelect) {
        super();

        this.items = items;
        this.onSelect = onSelect;

        int width = PixelScene.landscape() ? WIDTH_MAX : WIDTH_MIN;
        float pos = MARGIN;

        // 标题
        if (title != null) {
            com.zootdungeon.ui.RenderedTextBlock tfTitle = PixelScene.renderTextBlock(title, 9);
            tfTitle.hardlight(TITLE_COLOR);
            tfTitle.setPos(MARGIN, pos);
            tfTitle.maxWidth(width - MARGIN * 2);
            add(tfTitle);
            pos = tfTitle.bottom() + MARGIN * 2;
        }

        // 一行横排所有 ItemSprite
        float spriteSize = ItemSprite.SIZE;
        float totalWidth = items.size() * spriteSize + (items.size() - 1) * MARGIN;
        float startX = MARGIN + (width - MARGIN * 2 - totalWidth) / 2;

        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            final int index = i;

            ItemSprite sprite = new ItemSprite(item.image(), null);
            sprite.x = startX + i * (spriteSize + MARGIN);
            sprite.y = pos;
            add(sprite);

            PointerArea clickArea = new PointerArea(sprite) {
                @Override
                protected void onClick(PointerEvent event) {
                    hide();
                    if (onSelect != null) {
                        onSelect.accept(items.get(index));
                    }
                }
            };
            add(clickArea);
        }

        pos += spriteSize + MARGIN;

        resize(width, (int) pos);
    }
}
