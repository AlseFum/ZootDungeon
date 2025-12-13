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

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.abilities.Ratmogrify;
import com.zootdungeon.items.potions.PotionOfHealing;
import com.zootdungeon.items.potions.PotionOfStrength;
import com.zootdungeon.items.scrolls.ScrollOfUpgrade;
import com.zootdungeon.items.LootRegistry;
import com.zootdungeon.sprites.RatSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class Rat extends Mob {

	static {
		// 额外的 Rat 掉落表：小概率掉三种基础资源之一
		LootRegistry.LootTable table = new LootRegistry.LootTable()
				// 力量药水
				.add(LootRegistry.forItemClass(1, PotionOfStrength.class))
				// 升级卷轴
				.add(LootRegistry.forItemClass(1, ScrollOfUpgrade.class))
				// 治疗药水
				.add(LootRegistry.forItemClass(2, PotionOfHealing.class));
		LootRegistry.register("mob:rat:basic_loot", table);
	}

	{
		spriteClass = RatSprite.class;
		
		HP = HT = 8;
		defenseSkill = 2;

		maxLvl = 5;

		// // 使用 LootRegistry 的 Rat 掉落表，小概率掉基础资源
		// lootTableId = "mob:rat:basic_loot";
		lootChance = 1f;
	}

	@Override
	protected boolean act() {
		if (Dungeon.level.heroFOV[pos] && Dungeon.hero.armorAbility instanceof Ratmogrify){
			alignment = Alignment.ALLY;
			if (state == SLEEPING) state = WANDERING;
		}
		return super.act();
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 1, 4 );
	}
	
	@Override
	public int attackSkill( Char target ) {
		return 8;
	}
	
	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 1);
	}

	private static final String RAT_ALLY = "rat_ally";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		if (alignment == Alignment.ALLY) bundle.put(RAT_ALLY, true);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(RAT_ALLY)) alignment = Alignment.ALLY;
	}
}
