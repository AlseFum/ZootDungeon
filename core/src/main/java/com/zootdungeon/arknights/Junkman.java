package com.zootdungeon.arknights;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.items.material.Gold;
import com.zootdungeon.sprites.MobSprite;
import com.zootdungeon.sprites.SpriteRegistry;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Random;

public class Junkman extends Mob {

	static {
		// SpriteRegistry no longer manages mob textures.
		SpriteRegistry.texture("mod:junkman", "cola/junkman.png");
	}

	{
		spriteClass = JunkmanSprite.class;

		HP = HT = 12;
		defenseSkill = 4;

		EXP = 2;
		maxLvl = 8;

		loot = Gold.class;
		lootChance = 0.5f;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 1, 6 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 10;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 2);
	}

	public static class JunkmanSprite extends MobSprite {

		public JunkmanSprite() {
			super();
			scale.set(0.8f);

			// Same frame indices as GnollSprite; die uses index 10. If cola/junkman.png is missing or
			// the sheet has too few cells, TextureFilm.get(n) is null and play() NPEs — fall back to gnoll sheet.
			TextureFilm frames = textureWithFallback("mod:junkman", Assets.Sprites.GNOLL, 38, 34);
			if (frames.get(10) == null) {
				frames = textureWithFallback(null, Assets.Sprites.GNOLL, 12, 15);
			}

			idle = new Animation( 2, true );
			idle.frames( frames, 5);

			run = new Animation( 12, true );
			run.frames( frames, 1,2,3);

			attack = new Animation( 12, false );
			attack.frames( frames, 7,8,9);

			die = new Animation( 12, false );
			die.frames( frames, 5,6);

			play( idle );
		}
	}
}
