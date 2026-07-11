package com.zootdungeon.items.stones;

import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.ItemEffects;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.sprites.ItemSpriteSheet;

public class StoneOfBlink extends Runestone {
	
	{
		image = ItemSpriteSheet.STONE_BLINK;
	}
	
	private static Ballistica throwPath;
	
	@Override
	public int throwPos(Hero user, int dst) {
		throwPath = new Ballistica( user.pos, dst, Ballistica.PROJECTILE );
		return throwPath.collisionPos;
	}
	
	@Override
	public void onThrow(int cell) {
		if (Actor.findChar(cell) != null && throwPath.dist >= 1){
			cell = throwPath.path.get(throwPath.dist-1);
		}
		throwPath = null;
		super.onThrow(cell);
	}
	
	@Override
	protected void activate(int cell) {
		ItemEffects.teleportToLocation(curUser, cell);
	}
}
