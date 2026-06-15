/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
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

package com.zootdungeon.items.weapon.ambushWeapon;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;

public class Dirk extends AmbushWeapon {

	{
		image = ItemSpriteSheet.DIRK;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 1f;

		tier = 2;
        ambushRate=0.67f;
	}

}
