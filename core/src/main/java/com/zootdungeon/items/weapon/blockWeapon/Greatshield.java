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

package com.zootdungeon.items.weapon.blockWeapon;

import com.zootdungeon.sprites.ItemSpriteSheet;

public class Greatshield extends BlockWeapon {

	{
		image = ItemSpriteSheet.GREATSHIELD;

		tier = 5;
        drBase=6;
        drPerLevel=2;
	}
	@Override
	public String upgradeAbilityStat(int level) {
		return Integer.toString(3 + level);
	}
}
