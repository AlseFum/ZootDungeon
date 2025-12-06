package com.zootdungeon.items;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Generator.Category;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.utils.GLog;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Debug-only item which attempts to grant the hero one copy of every
 * item that can be produced by the {@link Generator} system.
 *
 * This is intended for testing and sandboxing; it should not appear
 * in regular gameplay.
 */
public class AllItemsBox extends Item {

    public static final String AC_GENERATE_ALL = "GENERATE_ALL";

    {
        image = ItemSpriteSheet.BACKPACK;
        stackable = false;
        defaultAction = AC_GENERATE_ALL;
        unique = true;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_GENERATE_ALL);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);

        if (AC_GENERATE_ALL.equals(action)) {
            giveAllItems(hero);
        }
    }

    private void giveAllItems(Hero hero) {
        Set<Class<? extends Item>> unique = new HashSet<>();
        int count = 0;

        for (Category cat : Category.values()) {
            if (cat.classes == null) continue;
            for (Class<?> raw : cat.classes) {
                if (raw == null) continue;
                if (!Item.class.isAssignableFrom(raw)) continue;

                @SuppressWarnings("unchecked")
                Class<? extends Item> itemCls = (Class<? extends Item>) raw;

                if (!unique.add(itemCls)) continue;

                Item item;
                try {
                    item = Reflection.newInstance(itemCls);
                } catch (Throwable t) {
                    continue;
                }

                if (item == null) continue;

                item.identify();
                // Try to put into backpack, otherwise drop at hero position
                if (!item.collect(hero.belongings.backpack)) {
                    Dungeon.level.drop(item, hero.pos).sprite.drop();
                }
                count++;
            }
        }

        GLog.p("尝试生成所有可获得的物品，共 " + count + " 种。");
    }

    @Override
    public String name() {
        return "全物品宝盒";
    }

    @Override
    public String desc() {
        return "一个调试用宝盒，打开后会尝试给予你通过 Generator 系统可获得的所有物品各一份。";
    }

    @Override
    public boolean isIdentified() {
        return true;
    }
}


