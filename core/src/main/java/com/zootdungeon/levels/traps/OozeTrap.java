package com.zootdungeon.levels.traps;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Ooze;
import com.zootdungeon.effects.Splash;
// import com.zootdungeon.utils.EventBus; // EventBus removed - TODO: restore when needed
import com.watabou.utils.PathFinder;

public class OozeTrap extends Trap {

	{
		color = GREEN;
		shape = DOTS;
	}

	@Override
	public void activate() {
		for( int i : PathFinder.NEIGHBOURS9) {
			if (!Dungeon.level.solid[pos + i]) {
				Splash.at( pos + i, 0x000000, 5);
				
				// Handle characters
				Char ch = Actor.findChar( pos + i );
				if (ch != null && !ch.flying){
					Buff.affect(ch, Ooze.class).set( Ooze.DURATION );
				}
				// EventBus removed
			}
		}
	}
}
