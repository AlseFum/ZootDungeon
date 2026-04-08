package com.zootdungeon.items.cheat;

import java.util.ArrayList;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.windows.WndBag;
import com.zootdungeon.windows.WndItemEditor;

public class ItemEditor extends Item {
    static {
        SpriteRegistry.texture("sheet.cola.handle", "cola/handle.png")
                .grid(16, 16)
                .label("handle_holder");
    }
    
    {
        image = SpriteRegistry.byLabel("handle_holder");
        icon = ItemSpriteSheet.Icons.SYMBOL_DEBUG;
        stackable = false;
        unique = true;
        defaultAction = AC_EDIT;
    }

    public static final String AC_EDIT = "EDIT";

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_EDIT);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (action.equals(AC_EDIT)) {
            GameScene.selectItem(new WndBag.ItemSelector() {
                @Override
                public String textPrompt() {
                    return "选择要编辑的物品";
                }

                @Override
                public boolean itemSelectable(Item item) {
                    return item != null;
                }

                @Override
                public void onSelect(Item item) {
                    if (item == null) {
                        return;
                    }
                    GameScene.show(new WndItemEditor(item));
                }
            });
        }
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_EDIT)) {
            return "编辑物品";
        }
        return super.actionName(action, hero);
    }

    @Override
    public String name() {
        return "物品编辑器";
    }

    @Override
    public String desc() {
        return "这个工具可以让你编辑物品的属性，包括等级、数量、诅咒状态等。";
    }

    @Override
    public boolean isUpgradable() {
        return false;
    }

    @Override
    public boolean isIdentified() {
        return true;
    }

    @Override
    public int value() {
        return 0;
    }
}

