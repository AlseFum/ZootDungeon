/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.zootdungeon.actors.mobs;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.items.food.MysteryMeat;
import com.zootdungeon.sprites.MobSprite;
import com.zootdungeon.sprites.SpriteRegistry;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Random;

public class Hound extends Mob {

	static {
		SpriteRegistry.registerMob("mod:hound",
				new SpriteRegistry.MobDef("cola/hound.png", 32, 32));
	}

	{
		spriteClass = HoundSprite.class;

		HP = HT = 15;
		defenseSkill = 5;
		baseSpeed = 2f;

		EXP = 4;
		maxLvl = 9;

		loot = MysteryMeat.class;
		lootChance = 0.167f;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 1, 7 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 12;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 4);
	}

	public static class HoundSprite extends MobSprite {

		public HoundSprite() {
			super();

			TextureFilm frames = textureWithFallback("mod:hound", Assets.Sprites.CRAB, 32, 32);

			idle = new Animation( 5, true );
			idle.frames( frames, 0 );

			run = new Animation( 15, true );
			run.frames( frames, 2,3, 4, 5, 6 );

			attack = new Animation( 12, false );
			attack.frames( frames, 7, 8, 9 );

			die = new Animation( 12, false );
			die.frames( frames, 10, 11, 12, 13 );

			play( idle );
		}

		@Override
		public int blood() {
			return 0xFFFFEA80;
		}
	}
}
