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

package com.zootdungeon.items.potions.elixirs;

import com.zootdungeon.actors.buffs.ArcaneArmor;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.potions.exotic.PotionOfEarthenArmor;
import com.zootdungeon.items.quest.GooBlob;
import com.zootdungeon.journal.Catalog;
import com.zootdungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;

public class ElixirOfArcaneArmor extends Elixir {
	
	{
		image = ItemSpriteSheet.ELIXIR_ARCANE;
	}
	
	@Override
	public void apply(Hero hero) {
		Buff.affect(hero, ArcaneArmor.class).set(5 + hero.lvl/2, 80);
	}
	
	public static class Recipe extends com.zootdungeon.items.Recipe.SimpleRecipe {
		
		{
			inputs =  new Class[]{PotionOfEarthenArmor.class, GooBlob.class};
			inQuantity = new int[]{1, 1};
			
			cost = 8;
			
			output = ElixirOfArcaneArmor.class;
			outQuantity = 1;
		}

		@Override
		public Item brew(ArrayList<Item> ingredients) {
			Catalog.countUse(GooBlob.class);
			return super.brew(ingredients);
		}
	}
}
