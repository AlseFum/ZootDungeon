package com.zootdungeon.levels.themes.device;

import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.mobs.Device;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.levels.painters.Painter;
import com.zootdungeon.levels.painters.RegularPainter;
import com.zootdungeon.levels.rooms.Room;
import com.zootdungeon.levels.themes.sewer.SewerBossLevel;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class DeviceSewerBossLevel extends SewerBossLevel {

    private static final float THEME_TERRAIN_DENSITY = 0.03f;
    private static final int[] THEME_TERRAINS = new int[]{
            Terrain.THEME_TILE_1,
            Terrain.THEME_TILE_2,
            Terrain.THEME_TILE_3,
            Terrain.THEME_TILE_4,
            Terrain.THEME_TILE_5,
            Terrain.THEME_TILE_6,
            Terrain.THEME_TILE_7,
            Terrain.THEME_TILE_8
    };

    @Override
    protected boolean build() {
        if (!super.build()) return false;
        populateDeviceTerrainsAndUnits();
        return true;
    }

    @Override
    protected Painter painter() {
        return new RegularPainter() {
            @Override
            protected void decorate(com.zootdungeon.levels.Level level, ArrayList<Room> rooms) {
                // Device theme temporarily disables sewer painter decoration.
            }
        };
    }

    private void populateDeviceTerrainsAndUnits() {
        for (int i = 0; i < length; i++) {
            if (i == entrance() || i == exit()) continue;
            if (map[i] != Terrain.EMPTY) continue;

            if (Random.Float() >= THEME_TERRAIN_DENSITY) {
                continue;
            }
            map[i] = THEME_TERRAINS[Random.Int(THEME_TERRAINS.length)];

            if (Actor.findChar(i) != null) continue;
            Device d = new Device();
            d.pos = i;
            mobs.add(d);
            Actor.ensureActorAdded(d);
        }
    }
}
