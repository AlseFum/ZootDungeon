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

package com.zootdungeon.items;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.effects.FloatingText;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

public class EnergyCrystal extends Item.MassiveResource {

	{
		image = ItemSpriteSheet.ENERGY;
	}

	// 无参构造函数使用基类的默认构造函数，quantity = 1
	// 如果有参构造函数存在，Java 不会自动生成无参构造函数，所以需要显式定义

	public EnergyCrystal( int value ) {
		super(value);
	}

	@Override
	protected boolean onPickUp(Hero hero, int pos) {
		Dungeon.energy += quantity;
		//TODO track energy collected maybe? We do already track recipes crafted though..

		hero.sprite.showStatusWithIcon( 0x44CCFF, Integer.toString(quantity), FloatingText.ENERGY );

		return true;
	}

}
