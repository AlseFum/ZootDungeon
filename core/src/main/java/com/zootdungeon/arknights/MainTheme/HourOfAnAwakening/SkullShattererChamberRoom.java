package com.zootdungeon.arknights.MainTheme.HourOfAnAwakening;

import com.zootdungeon.actors.Actor;
import com.zootdungeon.arknights.MainTheme.SkullShatterer;
import com.zootdungeon.levels.Level;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.levels.painters.Painter;
import com.zootdungeon.levels.rooms.Room;
import com.zootdungeon.levels.rooms.special.SpecialRoom;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Boss 特殊房：中央碎骨，周围较弱小怪。不与入口/出口直连，避免跳过流程。
 */
public class SkullShattererChamberRoom extends SpecialRoom {

	@Override
	public int minWidth() {
		return Math.max(super.minWidth(), 11);
	}

	@Override
	public int minHeight() {
		return Math.max(super.minHeight(), 11);
	}

	@Override
	public int maxWidth() {
		return Math.max(super.maxWidth(), 13);
	}

	@Override
	public int maxHeight() {
		return Math.max(super.maxHeight(), 13);
	}

	@Override
	public void paint( Level level ) {
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY );

		Door door = entrance();
		if ( door != null ) {
			door.set( Door.Type.UNLOCKED );
		}

		Point c = center();
		int bossPos = c.x + c.y * level.width();

		SkullShatterer boss = new SkullShatterer();
		boss.pos = bossPos;
		level.mobs.add( boss );
		Actor.ensureActorAdded( boss );

		ArrayList<Integer> spawnCells = new ArrayList<>();
		for ( int x = left + 1; x < right; x++ ) {
			for ( int y = top + 1; y < bottom; y++ ) {
				int cell = x + y * level.width();
				if ( cell != bossPos ) {
					spawnCells.add( cell );
				}
			}
		}
		Random.shuffle( spawnCells );

		// Reduce squad spawn density: roll each slot independently.
		int maxSlots = Math.min( 3, spawnCells.size() );
		int spawned = 0;
		for ( int i = 0; i < maxSlots; i++ ) {
			if ( Random.Float() > 0.35f ) continue;
			ShattererSquadMob m = new ShattererSquadMob();
			m.pos = spawnCells.get( i );
			level.mobs.add( m );
			Actor.ensureActorAdded( m );
			spawned++;
		}
		// Keep at least one support mob in the chamber.
		if ( spawned == 0 && maxSlots > 0 ) {
			ShattererSquadMob m = new ShattererSquadMob();
			m.pos = spawnCells.get( 0 );
			level.mobs.add( m );
			Actor.ensureActorAdded( m );
		}
	}

	@Override
	public boolean connect( Room room ) {
		if ( room.isExit() || room.isEntrance() ) {
			return false;
		}
		return super.connect( room );
	}
}
