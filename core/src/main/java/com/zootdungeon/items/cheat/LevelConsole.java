package com.zootdungeon.items.cheat;

import java.io.IOException;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.GamesInProgress;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.items.Item;
import com.zootdungeon.levels.Level;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.levels.features.LevelTransition;
import com.zootdungeon.levels.themes.Theme;
import com.zootdungeon.scenes.InterlevelScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.windows.WndGeneral;
import com.watabou.noosa.Game;
//Not finished
public class LevelConsole extends Item {

    // static {
    //     SpriteRegistry.texture("sheet.cola.debug_bag", "cola/debug_bag.png")
    //             .setXY("debug_bag", 0, 0, 32, 32);
    // }

    {
        image = SpriteRegistry.byLabel("debug_bag");
        stackable = false;
        unique = true;
        defaultAction = AC_OPEN;
    }

    public static final String AC_OPEN = "OPEN";

    // Live state for the custom-generation tab
    private int inputDepth = 1;
    private int inputBranch = 0;

    @Override
    public java.util.ArrayList<String> actions(Hero hero) {
        java.util.ArrayList<String> actions = super.actions(hero);
        actions.add(AC_OPEN);
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (AC_OPEN.equals(action)) {
            return "关卡控制台";
        }
        return super.actionName(action, hero);
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (AC_OPEN.equals(action)) {
            openConsole();
        }
    }

    private void openConsole() {
        //This is cheat tool, so we don't need to check

        inputDepth = Dungeon.depth;
        inputBranch = Dungeon.branch;

        WndGeneral.make()
            .title("关卡控制台")
            .tab("快速旅行", pane -> {
                pane.line("当前楼层: depth=" + Dungeon.depth + ", branch=" + Dungeon.branch);
                pane.line("");
                pane.hrow(r -> {
                    r.button("↑1", () -> quickTravel(1));
                    r.button("↑5", () -> quickTravel(5));
                });
                pane.hrow(r -> {
                    r.button("↓1", () -> quickTravel(-1));
                    r.button("↓5", () -> quickTravel(-5));
                });
                pane.line("");
                pane.line("提示: 只在已生成的关卡间跳转");
            })
            .tab("已生成关卡", pane -> {
                int size = Dungeon.generatedLevels.size();
                if (size == 0) {
                    pane.line("尚无已生成的关卡，先去其他关卡转转。");
                } else {
                    pane.line("点击跳转到已生成的关卡：");
                    for (int i = 0; i < size; i++) {
                        int code = Dungeon.generatedLevels.get(i);
                        int d = code % 1000;
                        int b = code / 1000;
                        pane.option("depth=" + d + ", branch=" + b,
                            () -> teleportTo(d, b, null));
                    }
                }
            })
            .tab("自定义生成", pane -> {
                pane.line("填写 depth / branch，选择关卡类型后点击\"前往\"：");

                pane.inputRow("depth:"+String.valueOf(inputDepth), String.valueOf(inputDepth), 5, text -> {
                    try { inputDepth = Integer.parseInt(text.trim()); } catch (NumberFormatException ignored) {}
                });
                pane.inputRow("branch:"+String.valueOf(inputBranch), String.valueOf(inputBranch), 5, text -> {
                    try { inputBranch = Integer.parseInt(text.trim()); } catch (NumberFormatException ignored) {}
                });

                pane.line("");
                pane.hrow(r -> {
                    r.button("Debug", () -> teleportTo(inputDepth, inputBranch, DebugLevel.class));
                    r.button("DeadEnd", () -> teleportTo(inputDepth, inputBranch, DeadEndLevel.class));
                    r.button("Last", () -> teleportTo(inputDepth, inputBranch, LastLevel.class));
                    r.button("Sandbox", () -> teleportTo(inputDepth, inputBranch, SandboxLevel.class));
                });
            })
            .show();
    }

    private void teleportTo(int depth, int branch, Class<? extends Level> levelClass) {
        System.out.println("teleportTo: depth=" + depth + ", branch=" + branch);

        // For branch != 0, pre-generate and save the level
        // (InterlevelScene doesn't handle non-zero branch level generation)
        if (branch != 0) {
            if (!Dungeon.levelHasBeenGenerated(depth, branch)) {
                Level newLevel;
                if (levelClass != null) {
                    try {
                        newLevel = levelClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        newLevel = Theme.createLevel(depth, branch);
                    }
                } else {
                    newLevel = Theme.createLevel(depth, branch);
                }

                int code = depth + 1000 * branch;
                if (!Dungeon.generatedLevels.contains(code)) {
                    Dungeon.generatedLevels.add(code);
                }
                newLevel.create();

                // Save the new level to slot before RETURN mode loads it
                try {
                    Dungeon.depth = depth;
                    Dungeon.branch = branch;
                    Dungeon.level = newLevel;
                    Dungeon.saveLevel(GamesInProgress.curSlot);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Level.beforeTransition();
        InterlevelScene.mode = InterlevelScene.Mode.RETURN;
        InterlevelScene.returnDepth = depth;
        InterlevelScene.returnBranch = branch;
        InterlevelScene.returnPos = -1;
        Game.switchScene(InterlevelScene.class);
    }

    /** Quick travel up/down by delta floors, staying in same branch */
    private void quickTravel(int delta) {
        int targetDepth = Dungeon.depth + delta;
        if (targetDepth < 1) {
            targetDepth = 1;
        }
        if (targetDepth > 40) {
            targetDepth = 40;
        }
        if (targetDepth != Dungeon.depth) {
            teleportTo(targetDepth, Dungeon.branch, null);
        }
    }

    @Override
    public int value() { return 0; }

    @Override
    public String name() { return "关卡控制台"; }

    @Override
    public String desc() {
        return "调试工具：查看已生成关卡，或自定义 depth/branch/类型后直接前往。";
    }

    @Override
    public boolean isIdentified() { return true; }

    @Override
    public boolean isUpgradable() { return false; }

    // -------------------------------------------------------------------------
    // Inner level classes
    // -------------------------------------------------------------------------

    /** 15x15 empty level for sandbox / debug use. */
    public static class SandboxLevel extends Level {

        private static final int WIDTH = 15;
        private static final int HEIGHT = 15;

        public SandboxLevel() {
            transitions = new java.util.ArrayList<>();
        }

        @Override public String tilesTex() { return Assets.Environment.TILES_CAVES; }
        @Override public String waterTex() { return Assets.Environment.WATER_CAVES; }

        @Override
        protected boolean build() {
            setSize(WIDTH, HEIGHT);
            for (int i = 0; i < length(); i++) map[i] = Terrain.EMPTY;

            int entrance = width() + 1;
            int exit = width() * (height() - 2) + width() - 2;

            map[entrance] = Terrain.ENTRANCE;
            transitions.add(new LevelTransition(this, entrance,
                    LevelTransition.Type.REGULAR_ENTRANCE, 1, 0, LevelTransition.Type.REGULAR_EXIT));

            map[exit] = Terrain.EXIT;
            transitions.add(new LevelTransition(this, exit,
                    LevelTransition.Type.REGULAR_EXIT, 1, 0, LevelTransition.Type.REGULAR_ENTRANCE));

            return true;
        }

        @Override public Mob createMob() { return null; }
        @Override protected void createMobs() {}
        @Override public Actor addRespawner() { return null; }
        @Override protected void createItems() {}
        @Override public int randomRespawnCell(Char ch) { return entrance(); }
    }

    /** Level used for depths -5 to 0. */
    public static class DebugLevel extends Level {

        public DebugLevel() {
            transitions = new java.util.ArrayList<>();
        }

        @Override public String tilesTex() { return Assets.Environment.TILES_CAVES; }
        @Override public String waterTex() { return Assets.Environment.WATER_CAVES; }

        @Override
        protected boolean build() {
            setSize(30, 30);
            for (int i = 0; i < length(); i++) map[i] = Terrain.WALL;

            int entrance = width() + 1;
            map[entrance] = Terrain.ENTRANCE;
            transitions.add(new LevelTransition(this, entrance,
                    LevelTransition.Type.REGULAR_ENTRANCE, 1, 0, LevelTransition.Type.REGULAR_EXIT));

            int exit = width() * (height() - 2) + width() - 2;
            map[exit] = Terrain.EXIT;
            transitions.add(new LevelTransition(this, exit,
                    LevelTransition.Type.REGULAR_EXIT, 1, 0, LevelTransition.Type.REGULAR_ENTRANCE));

            // clear a small area
            for (int y = 2; y < height() - 2; y++) {
                for (int x = 2; x < width() - 2; x++) {
                    map[y * width() + x] = Terrain.EMPTY;
                }
            }
            return true;
        }

        @Override public Mob createMob() { return null; }
        @Override protected void createMobs() {}
        @Override public Actor addRespawner() { return null; }
        @Override protected void createItems() {}
        @Override public int randomRespawnCell(Char ch) { return entrance(); }
    }

    /** Level used for depths < -5. */
    public static class DeadEndLevel extends Level {

        public DeadEndLevel() {
            transitions = new java.util.ArrayList<>();
        }

        @Override public String tilesTex() { return Assets.Environment.TILES_CAVES; }
        @Override public String waterTex() { return Assets.Environment.WATER_CAVES; }

        @Override
        protected boolean build() {
            setSize(15, 15);
            for (int i = 0; i < length(); i++) map[i] = Terrain.EMPTY;

            int entrance = width() + 1;
            map[entrance] = Terrain.ENTRANCE;
            transitions.add(new LevelTransition(this, entrance,
                    LevelTransition.Type.REGULAR_ENTRANCE, 1, 0, LevelTransition.Type.REGULAR_EXIT));

            int exit = width() * (height() - 2) + width() - 2;
            map[exit] = Terrain.EXIT;
            transitions.add(new LevelTransition(this, exit,
                    LevelTransition.Type.REGULAR_EXIT, 1, 0, LevelTransition.Type.REGULAR_ENTRANCE));

            return true;
        }

        @Override public Mob createMob() { return null; }
        @Override protected void createMobs() {}
        @Override public Actor addRespawner() { return null; }
        @Override protected void createItems() {}
        @Override public int randomRespawnCell(Char ch) { return entrance(); }
    }

    /** Level used for depths >= 26. */
    public static class LastLevel extends Level {

        public LastLevel() {
            transitions = new java.util.ArrayList<>();
        }

        @Override public String tilesTex() { return Assets.Environment.TILES_CAVES; }
        @Override public String waterTex() { return Assets.Environment.WATER_CAVES; }

        @Override
        protected boolean build() {
            setSize(50, 50);
            for (int i = 0; i < length(); i++) map[i] = Terrain.WALL;

            int entrance = width() + 1;
            map[entrance] = Terrain.ENTRANCE;
            transitions.add(new LevelTransition(this, entrance,
                    LevelTransition.Type.REGULAR_ENTRANCE, 1, 0, LevelTransition.Type.REGULAR_EXIT));

            int exit = width() * (height() - 2) + width() - 2;
            map[exit] = Terrain.EXIT;
            transitions.add(new LevelTransition(this, exit,
                    LevelTransition.Type.REGULAR_EXIT, 1, 0, LevelTransition.Type.REGULAR_ENTRANCE));

            // clear a larger area
            for (int y = 3; y < height() - 3; y++) {
                for (int x = 3; x < width() - 3; x++) {
                    map[y * width() + x] = Terrain.EMPTY;
                }
            }
            return true;
        }

        @Override public Mob createMob() { return null; }
        @Override protected void createMobs() {}
        @Override public Actor addRespawner() { return null; }
        @Override protected void createItems() {}
        @Override public int randomRespawnCell(Char ch) { return entrance(); }
    }
}
