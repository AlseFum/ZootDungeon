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

package com.zootdungeon.arknights.misc;

import com.zootdungeon.Assets;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.sprites.ItemSpriteSheet;

/** 罗德岛制式战锤：比 {@link RhodesStandardSword} 高一档 tier，偏慢、打击音效。 */
public class RhodesStandardHammer extends MeleeWeapon {

	{
		image = ItemSpriteSheet.WAR_HAMMER;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1.0f;
		DLY = 1.25f;

		tier = 1;
	}

	@Override
	public int min(int lvl) {
		return tier + lvl;
	}

	@Override
	public int max(int lvl) {
		return 5 * (tier + 1) + lvl * (tier + 1);
	}
}
