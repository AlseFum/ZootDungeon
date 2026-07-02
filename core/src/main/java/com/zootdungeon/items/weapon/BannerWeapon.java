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
import com.zootdungeon.items.weapon.base.MeleeWeapon;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Barrier;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.base.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class BannerWeapon extends MeleeWeapon {

	public static final String AC_COMMAND = "COMMAND";

	private int reach        = 2;
	private int barrierAmt   = 10;
	private float dmgBonus   = 0.3f;

	private static final String REACH       = "reach";
	private static final String BARRIER_AMT = "barrierAmt";
	private static final String DMG_BONUS   = "dmgBonus";

	{
		image = ItemSpriteSheet.RUNIC_BLADE;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1.0f;
		tier = 1;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_COMMAND);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_COMMAND)) {
			curUser = hero;
			curItem = this;
			GameScene.selectCell(commander);
		}
	}

	@Override
	public int min(int lvl) {
		return tier + lvl;
	}

	@Override
	public int max(int lvl) {
		return 5 * (tier + 2) + lvl * (tier + 1);
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		return super.proc(attacker, defender, damage);
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", reach, barrierAmt, Math.round(dmgBonus * 100));
	}

	public BannerWeapon randomize() {
		tier = Random.IntRange(1, 3);
		level(Random.IntRange(0, 3));
		reach = Random.IntRange(1, 3);
		barrierAmt = Random.IntRange(6, 18);
		dmgBonus = Random.Float(0.15f, 0.45f);
		return this;
	}

	@Override
	public Item random() {
		return randomize();
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(REACH, reach);
		bundle.put(BARRIER_AMT, barrierAmt);
		bundle.put(DMG_BONUS, dmgBonus);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(REACH))      reach      = bundle.getInt(REACH);
		if (bundle.contains(BARRIER_AMT))barrierAmt  = bundle.getInt(BARRIER_AMT);
		if (bundle.contains(DMG_BONUS))  dmgBonus   = bundle.getFloat(DMG_BONUS);
	}

	private CellSelector.Listener commander = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer target) {
			if (target == null) return;

			int dist = Dungeon.level.distance(curUser.pos, target);
			if (dist < 1 || dist > reach || !Dungeon.level.heroFOV[target]) {
				curUser.spendAndNext(curUser.attackDelay());
				return;
			}

			curUser.sprite.zap(target);
			curUser.busy();

			Char ch = Actor.findChar(target);

			if (ch != null && ch.alignment == Char.Alignment.ALLY && ch != curUser) {
				// ally → grant barrier
				Buff.affect(ch, Barrier.class).setShield(barrierAmt);
				ch.sprite.showStatusWithIcon(com.zootdungeon.sprites.CharSprite.POSITIVE,
						Integer.toString(barrierAmt),
						com.zootdungeon.effects.FloatingText.SHIELDING);
				CellEmitter.center(target).burst(Speck.factory(Speck.UP), 8);
				Sample.INSTANCE.play(Assets.Sounds.EVOKE);

			} else if (ch != null && ch.alignment == Char.Alignment.ENEMY) {
				// enemy → ranged attack with bonus
				int dmg = damageRoll(curUser);
				dmg = Math.round(dmg * (1f + dmgBonus));
				dmg = curUser.attackProc(ch, dmg);
				ch.damage(dmg, BannerWeapon.this);
				Sample.INSTANCE.play(hitSound, 1, hitSoundPitch);

			} else {
				// empty tile → visual only
				CellEmitter.center(target).burst(Speck.factory(Speck.UP), 4);
				Sample.INSTANCE.play(Assets.Sounds.EVOKE);
			}

			curUser.spendAndNext(curUser.attackDelay());
		}

		@Override
		public String prompt() {
			return Messages.get(BannerWeapon.class, "prompt", reach);
		}
	};
}
