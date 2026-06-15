package com.zootdungeon.levels;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.levels.features.LevelTransition;

public class DebugLevel extends Level {

    private static final int W = 7;
    private static final int H = 7;

    {
        color1 = 0x534f3e;
        color2 = 0xb9d661;
    }

    @Override
    public String tilesTex() {
        return Assets.Environment.TILES_CAVES;
    }

    @Override
    public String waterTex() {
        return Assets.Environment.WATER_HALLS;
    }

    @Override
    protected boolean build() {
        setSize(W, H);
        for (int i = 0; i < length(); i++) {
            map[i] = Terrain.EMPTY;
        }
        int entrance = 0;
        int exit = length() - 1;
        transitions.add(new LevelTransition(this, entrance, LevelTransition.Type.REGULAR_ENTRANCE));
        map[entrance] = Terrain.ENTRANCE;
        transitions.add(new LevelTransition(this, exit, LevelTransition.Type.REGULAR_EXIT));
        map[exit] = Terrain.EXIT;
        return true;
    }

    @Override
    public Mob createMob() {
        return null;
    }

    @Override
    protected void createMobs() {
    }

    @Override
    public Actor addRespawner() {
        return null;
    }

    @Override
    protected void createItems() {
    }

    @Override
    public int randomRespawnCell(Char ch) {
        return entrance();
    }
}
