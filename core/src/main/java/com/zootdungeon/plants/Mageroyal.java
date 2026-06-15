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

package com.zootdungeon.plants;

import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.BlobImmunity;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.HeroSubClass;
import com.zootdungeon.items.potions.PotionOfHealing;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.utils.GLog;

public class Mageroyal extends Plant {

	{
		image = 7;
		seedClass = Seed.class;
	}

	@Override
	public void activate( Char ch ) {

		if (ch != null) {
			PotionOfHealing.cure(ch);

			if (ch instanceof Hero) {
				GLog.i( Messages.get(this, "refreshed") );

				if (((Hero) ch).subClass == HeroSubClass.WARDEN){
					Buff.affect(ch, BlobImmunity.class, BlobImmunity.DURATION/2f);
				}
			}
		}
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_MAGEROYAL;

			plantClass = Mageroyal.class;
		}
	}
}