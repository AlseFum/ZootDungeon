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

package com.zootdungeon.items.weapon;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class BerserkWeapon extends MeleeWeapon {

	private float speedPenalty = 0.5f;  // attack speed multiplier (<1 = slower)
	private float dmgMultiplier = 1.5f; // damage multiplier vs normal weapon

	private static final String SPEED_PENALTY = "speedPenalty";
	private static final String DMG_MULTIPLIER = "dmgMultiplier";

	{
		image = ItemSpriteSheet.GREATSWORD;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 0.8f;
		tier = 1;
	}

	@Override
	public int min(int lvl) {
		return Math.round((tier + lvl) * dmgMultiplier);
	}

	@Override
	public int max(int lvl) {
		return Math.round((5 * (tier + 5) + lvl * (tier + 2)) * dmgMultiplier);
	}

	@Override
	public int defenseFactor(Char owner) {
		return 0; // defense is zeroed by BerserkBuff in Char.damage()
	}

	@Override
	public boolean doEquip(Hero hero) {
		if (super.doEquip(hero)) {
			Buff.affect(hero, BerserkBuff.class).set(speedPenalty);
			return true;
		}
		return false;
	}

	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		Buff buff = hero.buff(BerserkBuff.class);
		if (buff != null) buff.detach();
		return super.doUnequip(hero, collect, single);
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc",
				Math.round(dmgMultiplier * 100),
				Math.round((1f - speedPenalty) * 100));
	}

	public BerserkWeapon randomize() {
		tier = Random.IntRange(3, 5);
		level(Random.IntRange(0, 3));
		dmgMultiplier = Random.Float(2.5f, 4.5f);
		speedPenalty = Random.Float(0.3f, 0.5f);
		return this;
	}

	@Override
	public Item random() {
		return randomize();
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(SPEED_PENALTY, speedPenalty);
		bundle.put(DMG_MULTIPLIER, dmgMultiplier);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(SPEED_PENALTY)) speedPenalty = bundle.getFloat(SPEED_PENALTY);
		if (bundle.contains(DMG_MULTIPLIER)) dmgMultiplier = bundle.getFloat(DMG_MULTIPLIER);
	}

	public static class BerserkBuff extends FlavourBuff {

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		private float factor = 0.4f;

		void set(float factor) {
			this.factor = factor;
		}

		public float speedFactor() {
			return factor;
		}

		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (factor - 0.3f) / 0.2f);
		}

		@Override
		public String toString() {
			return Messages.get(this, "name");
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", Math.round((1f - factor) * 100));
		}

		private static final String FACTOR = "factor";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(FACTOR, factor);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			if (bundle.contains(FACTOR)) factor = bundle.getFloat(FACTOR);
		}
	}
}
