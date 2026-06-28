package com.zootdungeon.items.cheat;

import java.util.ArrayList;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.sprites.TextureRegistry;
import com.zootdungeon.windows.WndBag;
import com.zootdungeon.windows.WndItemEditor;

public class ItemEditor extends Item {
    
    {
        image = TextureRegistry.once("item_editor_icon", "cola/item_editor_icon.png", 0, 0, 32, 32);
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
                    return Messages.get(ItemEditor.class, "prompt");
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
            return Messages.get(this, "ac_edit");
        }
        return super.actionName(action, hero);
    }

    @Override
    public String name() {
        return Messages.get(this, "name");
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc");
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

