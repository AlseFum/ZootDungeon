package com.zootdungeon.items.cheat;

import java.util.ArrayList;
import java.util.Set;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.utils.Select;

public class WaterPlacer extends Item {
    
    {
        image = ItemSpriteSheet.POTION_AZURE;
        stackable = false;
        unique = true;
        defaultAction = AC_PLACE;
    }
    
    public static final String AC_PLACE = "PLACE";
    
    public Select.PlaceBuilder selector;
    
    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_PLACE);
        return actions;
    }
    
    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (action.equals(AC_PLACE)) {
            if (selector == null) {
                GameScene.selectCell("选择位置", (Integer pos) -> {
                    if (pos == -1) {
                        return;
                    }
                    if (pos >= 0 && pos < Dungeon.level.length()) {
                        int x = pos % Dungeon.level.width();
                        int y = pos / Dungeon.level.width();
                        // -1,0,+1 for both x and y around center, i.e. rect of 3*3
                        Set<Integer> places = Select.place().rect(x - 1, y - 1, 3, 3).query();
                        int count = 0;
                        for (int place : places) {
                            if (place >= 0 && place < Dungeon.level.length()) {
                                Dungeon.level.map[place] = Terrain.WATER;
                                this.updateTerrainFlags(place);
                                GameScene.updateMap(place);
                                count++;
                            }
                        }
                        GLog.i("在 " + count + " 个位置添加了水");
                    }
                });
            } else {
                Set<Integer> places = selector.query();
                int count = 0;
                for (int place : places) {
                    if (place >= 0 && place < Dungeon.level.length()) {
                        Dungeon.level.map[place] = Terrain.WATER;
                        this.updateTerrainFlags(place);
                        GameScene.updateMap(place);
                        count++;
                    }
                }
                GLog.i("在 " + count + " 个位置添加了水");
            }
        }
    }
    
    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_PLACE)) {
            return "放置水";
        }
        return super.actionName(action, hero);
    }
    
    @Override
    public String name() {
        return "水放置器";
    }
    
    @Override
    public String desc() {
        return "使用 PlaceSelector 在选中位置添加水。";
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
    
    private void updateTerrainFlags(int pos) {
        if (pos < 0 || pos >= Dungeon.level.length()) {
            return;
        }
        int terrain = Dungeon.level.map[pos];
        int flags = Terrain.flags[terrain];
        
        Dungeon.level.passable[pos] = (flags & Terrain.PASSABLE) != 0;
        Dungeon.level.solid[pos] = (flags & Terrain.SOLID) != 0;
        Dungeon.level.losBlocking[pos] = (flags & Terrain.LOS_BLOCKING) != 0;
        Dungeon.level.flamable[pos] = (flags & Terrain.FLAMABLE) != 0;
        Dungeon.level.secret[pos] = (flags & Terrain.SECRET) != 0;
        Dungeon.level.avoid[pos] = (flags & Terrain.AVOID) != 0;
        Dungeon.level.water[pos] = (flags & Terrain.LIQUID) != 0;
        Dungeon.level.pit[pos] = (flags & Terrain.PIT) != 0;
    }
}
