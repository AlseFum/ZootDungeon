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
import com.zootdungeon.Badges;
import com.zootdungeon.Dungeon;
import com.zootdungeon.Statistics;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.effects.FloatingText;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

public class Gold extends Item.MassiveResource {

	{
		image = ItemSpriteSheet.GOLD;
	}

	public Gold() {
		super(1);
	}

	public Gold( int value ) {
		super(value);
	}
	
	@Override
	protected boolean onPickUp(Hero hero, int pos) {
		Dungeon.gold += quantity;
		Statistics.goldCollected += quantity;
		Badges.validateGoldCollected();

		hero.sprite.showStatusWithIcon( CharSprite.NEUTRAL, Integer.toString(quantity), FloatingText.GOLD );
		
		return true;
	}

	@Override
	protected void playPickUpSound() {
		Sample.INSTANCE.play( Assets.Sounds.GOLD, 1, 1, Random.Float( 0.9f, 1.1f ) );
	}
	
	@Override
	public Item random() {
		quantity = Random.IntRange( 30 + Dungeon.depth * 10, 60 + Dungeon.depth * 20 );
		return this;
	}

}
