package com.zootdungeon.items.cheat;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Invisibility;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.scrolls.ScrollOfTeleportation;
import com.zootdungeon.items.stones.Runestone;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.InterlevelScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.windows.WndOptions;
import com.zootdungeon.levels.LevelGraph;
import com.zootdungeon.levels.LevelGraph.LevelNode;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;

import java.util.List;

/**
 * Debug/helper runestone which lets the player pick from any
 * already-generated floor and travel directly to that level's
 * regular entrance.
 *
 * Uses the new LevelGraph metadata instead of assuming a simple
 * depth+1 / depth-1 linear structure.
 */
public class StoneOfLevelSelect extends Runestone {

    {
        image = ItemSpriteSheet.STONE_CLAIRVOYANCE;
        defaultAction = AC_APPLY;
    }

    public static final String AC_APPLY = "APPLY";

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);

        if (AC_APPLY.equals(action)) {
            openSelector(hero);
        }
    }

    @Override
    protected void activate(int cell) {
        // When thrown/used as a runestone, just open the selector for the hero.
        // We ignore the target cell here because this stone always teleports
        // the hero to another floor, not to a specific cell on the current map.
        if (Dungeon.hero != null) {
            openSelector(Dungeon.hero);
        }
    }

    private void openSelector(Hero hero) {
        if (!Dungeon.interfloorTeleportAllowed()) {
            GLog.w(Messages.get(ScrollOfTeleportation.class, "no_tele"));
            return;
        }

        List<LevelNode> nodes = LevelGraph.generatedNodes();

        // +1 entry for "create new special level"
        String[] options = new String[nodes.size() + 1];
        for (int i = 0; i < nodes.size(); i++) {
            LevelNode n = nodes.get(i);
            String label = n.id;
            // include depth/branch info to help debugging
            label += " (depth " + n.depth + ", branch " + n.branch + ")";
            options[i] = label;
        }
        options[nodes.size()] = "新建随机特殊楼层";

        GameScene.show(new WndOptions(
                "楼层选择",
                "选择一个已生成的楼层，前往它的入口。",
                options
        ) {
            @Override
            protected void onSelect(int index) {
                if (index < 0) return;

                // Last option: create a brand new special level which
                // can only be reached via this stone.
                if (index == nodes.size()) {
                    LevelNode special = LevelGraph.createSpecialNode(
                            Dungeon.currentLevelId,
                            Dungeon.depth,
                            Dungeon.branch
                    );
                    teleportTo(hero, special);
                    return;
                }

                if (index >= nodes.size()) return;
                LevelNode target = nodes.get(index);
                teleportTo(hero, target);
            }
        });
    }

    private void teleportTo(Hero hero, LevelNode node) {
        if (node == null) return;

        Buff.affect(hero, Invisibility.class, 2f);
        InterlevelScene.mode = InterlevelScene.Mode.RETURN;
        InterlevelScene.returnDepth = node.depth;
        InterlevelScene.returnBranch = node.branch;
        InterlevelScene.returnPos = -1; // entrance

        Sample.INSTANCE.play(Assets.Sounds.TELEPORT);

        // consume the stone
        detach(hero.belongings.backpack);

        Game.switchScene(InterlevelScene.class);
    }

    @Override
    public String name() {
        return "楼层选择魔石";
    }

    @Override
    public String desc() {
        return "允许你在已生成的楼层之间自由旅行，直接前往目标楼层的入口。\n"
                + "这是一个调试/管理用道具，正常游戏过程中不应获得。";
    }

    @Override
    public boolean isIdentified() {
        return true;
    }
}


