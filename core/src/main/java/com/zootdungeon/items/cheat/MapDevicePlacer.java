package com.zootdungeon.items.cheat;

import java.util.ArrayList;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.mapDevice.BipolarDevice;
import com.zootdungeon.actors.mapDevice.CryoFieldDevice;
import com.zootdungeon.actors.mapDevice.DamageAuraDevice;
import com.zootdungeon.actors.mapDevice.DebugDummyMapDevice;
import com.zootdungeon.actors.mapDevice.MapDevice;
import com.zootdungeon.actors.mapDevice.WindTunnelDevice;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.TextureRegistry;
import com.zootdungeon.utils.GLog;

/**
 * 调试工具：在地图上放置各类 {@link MapDevice}。
 *
 * <p>支持以下装置类型：
 * <ul>
 *   <li>{@link DebugDummyMapDevice} — 测试占位</li>
 *   <li>{@link CryoFieldDevice} — 寒冷光环</li>
 *   <li>{@link DamageAuraDevice} — 伤害光环</li>
 *   <li>{@link BipolarDevice} — 双极切换</li>
 *   <li>{@link WindTunnelDevice} — 定向吹风（可设置方向）</li>
 * </ul>
 *
 * 通过 {@link GameScene#add(Char)} 挂 sprite（{@code scene.mobs} 容器）。
 */
public class MapDevicePlacer extends Item {

    public static final String AC_PLACE        = "PLACE";
    public static final String AC_PLACE_COLD   = "PLACE_COLD";
    public static final String AC_PLACE_DAMAGE = "PLACE_DAMAGE";
    public static final String AC_PLACE_MODE   = "PLACE_MODE";
    public static final String AC_PLACE_PUSH   = "PLACE_PUSH";
    public static final String AC_SET_DIR      = "SET_DIR";
    public static final String AC_REMOVE       = "REMOVE";

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
        actions.add(AC_PLACE_COLD);
        actions.add(AC_PLACE_DAMAGE);
        actions.add(AC_PLACE_MODE);
        actions.add(AC_PLACE_PUSH);
        actions.add(AC_SET_DIR);
        actions.add(AC_REMOVE);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        switch (action) {
            case AC_PLACE:
                GameScene.selectCell(placeDummy);
                break;
            case AC_PLACE_COLD:
                GameScene.selectCell(placeCold);
                break;
            case AC_PLACE_DAMAGE:
                GameScene.selectCell(placeDamage);
                break;
            case AC_PLACE_MODE:
                GameScene.selectCell(placeMode);
                break;
            case AC_PLACE_PUSH:
                GameScene.selectCell(placePush);
                break;
            case AC_SET_DIR:
                GameScene.selectCell(setDirection);
                break;
            case AC_REMOVE:
                GameScene.selectCell(removeTarget);
                break;
        }
    }

    @Override
    public String actionName(String action, Hero hero) {
        return Messages.get(this, "ac_" + action.toLowerCase());
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

    // ==================== 放置辅助 ====================

    /** 通用放置回调，将 device 放在 cell 上 */
    private void placeDevice(MapDevice device, int cell) {
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

        device.pos = cell;
        GameScene.add(device);
        GLog.p(Messages.get(MapDevicePlacer.class, "placed", device.name(), cell));
    }

    // ==================== 各装置放置 ====================

    private final CellSelector.Listener placeDummy = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            if (cell == null) return;
            placeDevice(new DebugDummyMapDevice(), cell);
        }

        @Override
        public String prompt() {
            return Messages.get(MapDevicePlacer.class, "prompt_place");
        }
    };

    private final CellSelector.Listener placeCold = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            if (cell == null) return;
            placeDevice(new CryoFieldDevice(), cell);
        }

        @Override
        public String prompt() {
            return Messages.get(MapDevicePlacer.class, "prompt_place");
        }
    };

    private final CellSelector.Listener placeDamage = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            if (cell == null) return;
            placeDevice(new DamageAuraDevice(), cell);
        }

        @Override
        public String prompt() {
            return Messages.get(MapDevicePlacer.class, "prompt_place");
        }
    };

    private final CellSelector.Listener placeMode = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            if (cell == null) return;
            placeDevice(new BipolarDevice(), cell);
        }

        @Override
        public String prompt() {
            return Messages.get(MapDevicePlacer.class, "prompt_place");
        }
    };

    private final CellSelector.Listener placePush = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            if (cell == null) return;
            placeDevice(new WindTunnelDevice(), cell);
        }

        @Override
        public String prompt() {
            return Messages.get(MapDevicePlacer.class, "prompt_place");
        }
    };

    // ==================== 方向切换 ====================

    private final CellSelector.Listener setDirection = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            if (cell == null || cell < 0 || Dungeon.level == null) return;
            Char ch = Actor.findChar(cell);
            if (!(ch instanceof WindTunnelDevice)) {
                GLog.w(Messages.get(MapDevicePlacer.class, "no_wind_tunnel"));
                return;
            }
            WindTunnelDevice fan = (WindTunnelDevice) ch;
            int oldDir = fan.getDirection();
            fan.cycleDirection();
            int newDir = fan.getDirection();
            GLog.i(Messages.get(MapDevicePlacer.class, "dir_changed",
                    cell, WindTunnelDevice.DIR_NAMES[oldDir], WindTunnelDevice.DIR_NAMES[newDir]));
        }

        @Override
        public String prompt() {
            return Messages.get(MapDevicePlacer.class, "prompt_set_dir");
        }
    };

    // ==================== 移除 ====================

    private final CellSelector.Listener removeTarget = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            if (cell == null || cell < 0 || Dungeon.level == null) return;
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

    // ==================== 通用 ====================

    private static boolean validCell(int cell) {
        if (Dungeon.level == null) return false;
        if (cell < 0 || cell >= Dungeon.level.length()) return false;
        return Dungeon.level.insideMap(cell);
    }
}
