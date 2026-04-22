package com.zootdungeon.items.cheat;

import java.util.ArrayList;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.levels.entities.CellEntity;
import com.zootdungeon.levels.entities.DebugCellMarker;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.utils.GLog;

/**
 * 调试工具：在地图上放置 / 查询 / 移除 {@link CellEntity}。
 * <p>
 * 默认动作 {@link #AC_PLACE}：点击一个格子后在上面放一个
 * {@link DebugCellMarker}，用于可视化地面实体的生命周期。
 */
public class CellEntityPlacer extends Item {

    public static final String AC_PLACE = "PLACE";
    public static final String AC_REMOVE = "REMOVE";
    public static final String AC_INSPECT = "INSPECT";

    {
        image = SpriteRegistry.byLabel("debug_bag");
        stackable = false;
        unique = true;
        defaultAction = AC_PLACE;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_PLACE);
        actions.add(AC_REMOVE);
        actions.add(AC_INSPECT);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (action.equals(AC_PLACE)) {
            GameScene.selectCell(placeTarget);
        } else if (action.equals(AC_REMOVE)) {
            GameScene.selectCell(removeTarget);
        } else if (action.equals(AC_INSPECT)) {
            GameScene.selectCell(inspectTarget);
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
        if (action.equals(AC_INSPECT)) {
            return Messages.get(this, "ac_inspect");
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
                GLog.w(Messages.get(CellEntityPlacer.class, "invalid_cell"));
                return;
            }
            DebugCellMarker marker = new DebugCellMarker();
            Dungeon.level.addCellEntity(marker, cell);
            GLog.p(Messages.get(CellEntityPlacer.class, "placed", marker.name(), cell));
        }

        @Override
        public String prompt() {
            return Messages.get(CellEntityPlacer.class, "prompt_place");
        }
    };

    private final CellSelector.Listener removeTarget = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            if (cell == null || cell < 0 || Dungeon.level == null) {
                return;
            }
            CellEntity entity = Dungeon.level.cellEntityAt(cell);
            if (entity == null) {
                GLog.w(Messages.get(CellEntityPlacer.class, "no_entity"));
                return;
            }
            String name = entity.name();
            Dungeon.level.removeCellEntity(entity);
            GLog.i(Messages.get(CellEntityPlacer.class, "removed", name, cell));
        }

        @Override
        public String prompt() {
            return Messages.get(CellEntityPlacer.class, "prompt_remove");
        }
    };

    private final CellSelector.Listener inspectTarget = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            if (cell == null || cell < 0 || Dungeon.level == null) {
                return;
            }
            CellEntity entity = Dungeon.level.cellEntityAt(cell);
            if (entity == null) {
                GLog.i(Messages.get(CellEntityPlacer.class, "inspect_empty", cell));
                return;
            }
            GLog.i(Messages.get(CellEntityPlacer.class, "inspect_hit",
                    entity.name(), cell, entity.getClass().getSimpleName()));
            GLog.i(entity.desc());
        }

        @Override
        public String prompt() {
            return Messages.get(CellEntityPlacer.class, "prompt_inspect");
        }
    };

    private static boolean validCell(int cell) {
        if (Dungeon.level == null) return false;
        if (cell < 0 || cell >= Dungeon.level.length()) return false;
        return Dungeon.level.insideMap(cell);
    }
}
