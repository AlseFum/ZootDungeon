package com.zootdungeon.items.cheat;

import java.util.ArrayList;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.DebugDummyMapDevice;
import com.zootdungeon.actors.MapDevice;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.TextureRegistry;
import com.zootdungeon.utils.GLog;

/**
 * 调试工具：在指定格子放置一个 {@link MapDevice}。
 *
 * 当前默认放置 {@link DebugDummyMapDevice}（占位实现，验证阻挡寻路 +
 * {@link MapDevice#receiveDamage(Object)} 链）。
 *
 * 默认动作 {@link #AC_PLACE}：选一格后生成 device。
 * 提供 {@link #AC_REMOVE} 用于移除自己放下的 device，避免测试残留。
 *
 * 通过 {@link GameScene#add(Char)} 挂 sprite（{@code scene.mobs} 容器）。
 */
public class MapDevicePlacer extends Item {

    public static final String AC_PLACE = "PLACE";
    public static final String AC_REMOVE = "REMOVE";

    {
        image = TextureRegistry.idByLabel("debug_bag");
        stackable = false;
        unique = true;
        defaultAction = AC_PLACE;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_PLACE);
        actions.add(AC_REMOVE);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (action.equals(AC_PLACE)) {
            GameScene.selectCell(placeTarget);
        } else if (action.equals(AC_REMOVE)) {
            GameScene.selectCell(removeTarget);
        }
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_PLACE)) {
            return Messages.get(this, "ac_place");
        }
        if (action.equals(AC_REMOVE)) {
            return Messages.get(this, "ac_remove");
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

    private final CellSelector.Listener placeTarget = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            if (cell == null || cell < 0 || Dungeon.level == null) {
                return;
            }
            if (!validCell(cell)) {
                GLog.w(Messages.get(MapDevicePlacer.class, "invalid_cell"));
                return;
            }
            if (Actor.findChar(cell) != null) {
                GLog.w(Messages.get(MapDevicePlacer.class, "cell_occupied"));
                return;
            }
            if (!Dungeon.level.passable[cell]) {
                GLog.w(Messages.get(MapDevicePlacer.class, "cell_not_passable"));
                return;
            }

            DebugDummyMapDevice device = new DebugDummyMapDevice();
            device.pos = cell;
            GameScene.add(device);
            GLog.p(Messages.get(MapDevicePlacer.class, "placed", device.name(), cell));
        }

        @Override
        public String prompt() {
            return Messages.get(MapDevicePlacer.class, "prompt_place");
        }
    };

    private final CellSelector.Listener removeTarget = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            if (cell == null || cell < 0 || Dungeon.level == null) {
                return;
            }
            Char ch = Actor.findChar(cell);
            if (!(ch instanceof MapDevice)) {
                GLog.w(Messages.get(MapDevicePlacer.class, "no_map_device"));
                return;
            }
            String name = ch.name();
            ch.die(null);
            GLog.i(Messages.get(MapDevicePlacer.class, "removed", name, cell));
        }

        @Override
        public String prompt() {
            return Messages.get(MapDevicePlacer.class, "prompt_remove");
        }
    };

    private static boolean validCell(int cell) {
        if (Dungeon.level == null) return false;
        if (cell < 0 || cell >= Dungeon.level.length()) return false;
        return Dungeon.level.insideMap(cell);
    }
}
