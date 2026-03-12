package com.zootdungeon.levels.themes.device;

import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.mobs.Device;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.levels.painters.Painter;
import com.zootdungeon.levels.painters.RegularPainter;
import com.zootdungeon.levels.rooms.Room;
import com.zootdungeon.levels.themes.sewer.SewerLevel;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class DeviceSewerLevel extends SewerLevel {
{

}
@Override
	public String tilesTex() {
		// 通过 SpriteRegistry 查询当前关卡的 tilemapKey，对应材质包可覆盖；
		// 若未注册则回退到原版洞穴贴图。
		return com.zootdungeon.sprites.SpriteRegistry.tilemapTilesTextureOr(
				com.zootdungeon.Assets.Environment.TILES_DEV,
				tilemapKey
		);
	}

    private static final float THEME_TERRAIN_DENSITY = 0.5f;
    private static final float DEVICE_SPAWN_CHANCE = 0.5f;
    private static final int[] THEME_TERRAINS = new int[]{
            Terrain.THEME_TILE_1,
            Terrain.THEME_TILE_2,
            // Terrain.THEME_TILE_3,
            // Terrain.THEME_TILE_4,
            // Terrain.THEME_TILE_5,
            // Terrain.THEME_TILE_6,
            // Terrain.THEME_TILE_7,
            // Terrain.THEME_TILE_8
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

            if (Random.Float() >= DEVICE_SPAWN_CHANCE) continue;
            if (Actor.findChar(i) != null) continue;
            Device d = new Device();
            d.pos = i;
            mobs.add(d);
            Actor.ensureActorAdded(d);
        }
    }
}
