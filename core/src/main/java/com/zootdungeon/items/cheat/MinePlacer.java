package com.zootdungeon.items.cheat;

import java.util.ArrayList;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.levels.entities.CellEntity;
import com.zootdungeon.levels.entities.mines.ContactMine;
import com.zootdungeon.levels.entities.mines.CrossMine;
import com.zootdungeon.levels.entities.mines.Mine;
import com.zootdungeon.levels.entities.mines.ProximityMine;
import com.zootdungeon.levels.entities.mines.RemoteMine;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.utils.GLog;

/**
 * 调试工具：批量放置 / 远程引爆四类 {@link Mine}。
 * <p>
 * 提供以下动作（在包括 DebugSupply 的右键菜单中展开）：
 * <ul>
 *     <li>{@link #AC_PLACE_CONTACT}：放一枚 {@link ContactMine}
 *         （敌人踩上即爆 + 随机方向推开）。</li>
 *     <li>{@link #AC_PLACE_PROXIMITY}：放一枚 {@link ProximityMine}
 *         （敌人进入 3×3 即爆，施加麻痹 + 致盲）。</li>
 *     <li>{@link #AC_PLACE_CROSS}：放一枚 {@link CrossMine}
 *         （敌人踩上即爆，四向 2 格伤害，连锁引爆其它地雷）。</li>
 *     <li>{@link #AC_PLACE_REMOTE}：放一枚 {@link RemoteMine}
 *         （敌人踩上无反应，需要走 {@link #AC_DETONATE_REMOTE} 主动触发）。</li>
 *     <li>{@link #AC_DETONATE_REMOTE}：点某一格远程引爆该格上的
 *         {@link RemoteMine}，3×3 伤害随目标数量放大。</li>
 * </ul>
 */
public class MinePlacer extends Item {

    public static final String AC_PLACE_CONTACT = "PLACE_CONTACT";
    public static final String AC_PLACE_PROXIMITY = "PLACE_PROXIMITY";
    public static final String AC_PLACE_CROSS = "PLACE_CROSS";
    public static final String AC_PLACE_REMOTE = "PLACE_REMOTE";
    public static final String AC_DETONATE_REMOTE = "DETONATE_REMOTE";

    {
        image = SpriteRegistry.byLabel("debug_bag");
        stackable = false;
        unique = true;
        defaultAction = AC_PLACE_CONTACT;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_PLACE_CONTACT);
        actions.add(AC_PLACE_PROXIMITY);
        actions.add(AC_PLACE_CROSS);
        actions.add(AC_PLACE_REMOTE);
        actions.add(AC_DETONATE_REMOTE);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (action.equals(AC_PLACE_CONTACT)) {
            GameScene.selectCell(placeContact);
        } else if (action.equals(AC_PLACE_PROXIMITY)) {
            GameScene.selectCell(placeProximity);
        } else if (action.equals(AC_PLACE_CROSS)) {
            GameScene.selectCell(placeCross);
        } else if (action.equals(AC_PLACE_REMOTE)) {
            GameScene.selectCell(placeRemote);
        } else if (action.equals(AC_DETONATE_REMOTE)) {
            GameScene.selectCell(detonateRemote);
        }
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_PLACE_CONTACT)) {
            return Messages.get(this, "ac_place_contact");
        }
        if (action.equals(AC_PLACE_PROXIMITY)) {
            return Messages.get(this, "ac_place_proximity");
        }
        if (action.equals(AC_PLACE_CROSS)) {
            return Messages.get(this, "ac_place_cross");
        }
        if (action.equals(AC_PLACE_REMOTE)) {
            return Messages.get(this, "ac_place_remote");
        }
        if (action.equals(AC_DETONATE_REMOTE)) {
            return Messages.get(this, "ac_detonate_remote");
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

    // ==== CellSelector.Listeners ====

    private final CellSelector.Listener placeContact = placeListener(
            ContactMine::new, "prompt_place_contact");
    private final CellSelector.Listener placeProximity = placeListener(
            ProximityMine::new, "prompt_place_proximity");
    private final CellSelector.Listener placeCross = placeListener(
            CrossMine::new, "prompt_place_cross");
    private final CellSelector.Listener placeRemote = placeListener(
            RemoteMine::new, "prompt_place_remote");

    private CellSelector.Listener placeListener(final java.util.function.Supplier<? extends Mine> factory,
                                                final String promptKey) {
        return new CellSelector.Listener() {
            @Override
            public void onSelect(Integer cell) {
                if (cell == null || cell < 0 || Dungeon.level == null) {
                    return;
                }
                if (!validCell(cell)) {
                    GLog.w(Messages.get(MinePlacer.class, "invalid_cell"));
                    return;
                }
                Mine mine = factory.get();
                Dungeon.level.addCellEntity(mine, cell);
                GLog.p(Messages.get(MinePlacer.class, "placed", mine.name(), cell));
            }

            @Override
            public String prompt() {
                return Messages.get(MinePlacer.class, promptKey);
            }
        };
    }

    private final CellSelector.Listener detonateRemote = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            if (cell == null || cell < 0 || Dungeon.level == null) {
                return;
            }
            CellEntity entity = Dungeon.level.cellEntityAt(cell);
            if (!(entity instanceof RemoteMine)) {
                GLog.w(Messages.get(MinePlacer.class, "no_remote_mine"));
                return;
            }
            RemoteMine mine = (RemoteMine) entity;
            String name = mine.name();
            mine.detonate();
            GLog.i(Messages.get(MinePlacer.class, "detonated", name, cell));
        }

        @Override
        public String prompt() {
            return Messages.get(MinePlacer.class, "prompt_detonate_remote");
        }
    };

    private static boolean validCell(int cell) {
        if (Dungeon.level == null) return false;
        if (cell < 0 || cell >= Dungeon.level.length()) return false;
        return Dungeon.level.insideMap(cell);
    }
}
