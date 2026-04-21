package com.zootdungeon.items.cheat;

import java.util.ArrayList;

import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Heap;
import com.zootdungeon.items.Item;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.windows.WndBag;
import com.zootdungeon.Dungeon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.windows.WndOptions;
import com.zootdungeon.ui.InventoryPane;


public class ItemRemover extends Item {
    static {
        SpriteRegistry.texture("sheet.cola.trashbin", "cola/trashbin.png")
                .grid(16, 16)
                .label("trashbin");
    }

    {
        image = SpriteRegistry.byLabel("trashbin");
        stackable = false;
        unique = true;
        defaultAction=AC_REMOVE;
    }

    public static final String AC_REMOVE = "REMOVE";
    public static final String AC_CLEAR = "CLEAR";
    public static final String AC_KILL = "KILL";

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_REMOVE);
        actions.add(AC_CLEAR);
        actions.add(AC_KILL);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (action.equals(AC_REMOVE)) {
            GameScene.selectItem(new WndBag.ItemSelector() {
                @Override
                public String textPrompt() {
                    return Messages.get(ItemRemover.class, "prompt_remove");
                }

                @Override
                public boolean itemSelectable(Item item) {
                    return true;
                }

                @Override
                public void onSelect(Item item) {
                    if (item == null) {
                        return;
                    }
                    if (item.quantity() > 5) {
                        int removeFifth = item.quantity() - (int) (item.quantity() * 4 / 5);
                        GameScene.show(WndOptions.make()
                        .title(Messages.get(ItemRemover.class, "title_qty"))
                        .message(Messages.get(ItemRemover.class, "msg_qty"))
                        .option(Messages.get(ItemRemover.class, "option_one"), (Object o) -> {
                            item.quantity(item.quantity() - 1);
                        })
                        .option(Messages.get(ItemRemover.class, "option_fifth", removeFifth), (Object o) -> {
                            item.quantity((int) (item.quantity() * 4 / 5));
                        })
                        .option(Messages.get(ItemRemover.class, "option_all"), (Object o) -> {
                            item.quantity(1);
                            item.detach(hero.belongings.backpack);
                            InventoryPane.refresh();
                        })
                        .build());
                    } else {
                        item.detach(hero.belongings.backpack);
                        InventoryPane.refresh();
                    }

                    // GameScene.selectItem(this);
                }
            });
        } else if (action.equals(AC_CLEAR)) {
            GameScene.selectCell(Messages.get(ItemRemover.class, "prompt_clear_cell"), (Integer pos) -> {
                if (pos == -1) {
                    return;
                }

                Heap heap = Dungeon.level.heaps.get(pos);
                if (heap != null) {
                    heap.destroy();
                }
            });
        } else if (action.equals(AC_KILL)) {
            GameScene.selectCell(Messages.get(ItemRemover.class, "prompt_kill"), (Integer pos) -> {
                if (pos == -1) {
                    return;
                }
                Char ch = Actor.findChar(pos);
                if (ch != null && ch.isAlive()) {
                    ch.die(this);
                } else {
                    GLog.w(Messages.get(ItemRemover.class, "no_unit"));
                }
            });
        }
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_REMOVE)) {
            return Messages.get(this, "ac_remove");
        }
        if (action.equals(AC_CLEAR)) {
            return Messages.get(this, "ac_clear");
        }
        if (action.equals(AC_KILL)) {
            return Messages.get(this, "ac_kill");
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
}
