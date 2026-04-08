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
                    return "Select an item to remove(click elsewhere to cancel)";
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
                        GameScene.show(WndOptions.make()
                        .title("删除物品")
                        .message("要删除多少？")
                        .option("1份",(Object o)->{
                            item.quantity(item.quantity() - 1);
                        })
                        .option("5分之一("+(item.quantity() - (int)(item.quantity() * 4 / 5))+")",(Object o)->{
                            item.quantity((int)(item.quantity() * 4 / 5));
                        })
                        .option("全部",(Object o)->{
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
            GameScene.selectCell("选择清除的格子", (Integer pos) -> {
                if (pos == -1) {
                    return;
                }

                Heap heap = Dungeon.level.heaps.get(pos);
                if (heap != null) {
                    heap.destroy();
                }
            });
        } else if (action.equals(AC_KILL)) {
            GameScene.selectCell("选择要清除的单位", (Integer pos) -> {
                if (pos == -1) {
                    return;
                }
                Char ch = Actor.findChar(pos);
                if (ch != null && ch.isAlive()) {
                    ch.die(this);
                } else {
                    GLog.w("该位置没有可清除的单位。");
                }
            });
        }
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_REMOVE)) {
            return "删除物品";
        }
        if (action.equals(AC_CLEAR)) {
            return "清除格子";
        }
        if (action.equals(AC_KILL)) {
            return "清除单位";
        }
        return super.actionName(action, hero);
    }

    @Override
    public String name() {
        return "Item Remover";
    }

    @Override
    public String desc() {
        return "调试工具：删除背包物品、清除地格掉落物，也可直接清除指定格子的单位。";
    }
}
