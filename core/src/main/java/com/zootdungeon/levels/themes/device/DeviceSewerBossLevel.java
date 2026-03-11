package com.zootdungeon.levels.themes.device;

import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.mobs.Device;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.levels.themes.sewer.SewerBossLevel;
import com.watabou.utils.Random;

public class DeviceSewerBossLevel extends SewerBossLevel {

    private static final float TERRAIN_A_DENSITY = 0.015f;
    private static final float TERRAIN_B_DENSITY = 0.015f;
    private static final int TERRAIN_A = Terrain.EMPTY_DECO;
    private static final int TERRAIN_B = Terrain.CUSTOM_DECO_EMPTY;

    @Override
    protected boolean build() {
        if (!super.build()) return false;
        populateDeviceTerrainsAndUnits();
        return true;
    }

    private void populateDeviceTerrainsAndUnits() {
        for (int i = 0; i < length; i++) {
            if (i == entrance() || i == exit()) continue;
            if (map[i] != Terrain.EMPTY) continue;

            float roll = Random.Float();
            if (roll < TERRAIN_A_DENSITY) {
                map[i] = TERRAIN_A;
            } else if (roll < TERRAIN_A_DENSITY + TERRAIN_B_DENSITY) {
                map[i] = TERRAIN_B;
            } else {
                continue;
            }

            if (Actor.findChar(i) != null) continue;
            Device d = new Device();
            d.pos = i;
            mobs.add(d);
            Actor.ensureActorAdded(d);
        }
    }
}
