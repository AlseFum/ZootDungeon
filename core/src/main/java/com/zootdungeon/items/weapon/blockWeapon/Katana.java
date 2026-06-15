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

import com.zootdungeon.Assets;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.sprites.ItemSpriteSheet;

public class Katana extends BlockWeapon {

	{
		image = ItemSpriteSheet.KATANA;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1.1f;

		tier = 4;
	}

	public int DRMax() {
		return 3;
	}
	
	public int DRMax(int lvl) {
		return 3; // 武士刀的防御值是固定的
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		//+(8+2*lvl) damage, roughly +67% damage
		int dmgBoost = augment.damageFactor(8 + Math.round(2f*buffedLvl()));
		BlockWeapon.lungeAbility(hero, target, 1, dmgBoost, this);
	}


	public String upgradeAbilityStat(int level){
		int dmgBoost = 8 + Math.round(2f*level);
		return augment.damageFactor(min(level)+dmgBoost) + "-" + augment.damageFactor(max(level)+dmgBoost);
	}

}
