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

package com.zootdungeon.actors.buffs;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.items.BrokenSeal;
import com.zootdungeon.items.GuardModal;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.ui.ActionIndicator;
import com.zootdungeon.ui.AttackIndicator;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.ui.HeroIcon;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.windows.WndAceCombo;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

public class AceCombo extends Buff implements ActionIndicator.Action {

	{
		type = buffType.POSITIVE;
	}

	private int count = 0;
	private float comboTime = 0f;
	private float initialComboTime = 0f;

	private boolean shieldBashUsed = false;
	private boolean counterStanceUsed = false;
	private boolean fortifyUsed = false;

	@Override
	public int icon() { return BuffIndicator.ACE_COMBO; }

	@Override
	public void tintIcon(com.watabou.noosa.Image icon) {
		AceComboMove move = getHighestMove();
		if (move != null) {
			icon.hardlight(move.tintColor & 0x00FF00,
					(move.tintColor & 0x0000FF) >> 8,
					move.tintColor & 0xFF0000);
		}
	}

	@Override
	public float iconFadePercent() {
		if (initialComboTime > 0) {
			return Math.max(0, (initialComboTime - comboTime) / initialComboTime);
		}
		return 1f;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString((int) comboTime);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", count, dispTurns(comboTime));
	}

	public void hit(Char enemy) {
		count++;
		comboTime = getComboTime();
		initialComboTime = comboTime;

		if (!enemy.isAlive()) {
			grantArmorOnKill();
		}

		checkAndShowIndicator();
		BuffIndicator.refreshHero();
	}

	public void struck(Char attacker, int damage) {
		if (damage <= 0) return;
		count++;
		comboTime = getComboTime();
		initialComboTime = comboTime;

		checkAndShowIndicator();
		BuffIndicator.refreshHero();
	}

	private float getComboTime() {
		Hero hero = (Hero) target;
		if (hero.hasTalent(Talent.ACE_COMBO_EXTENSION)) {
			int pts = hero.pointsInTalent(Talent.ACE_COMBO_EXTENSION);
			int[] timers = {5, 7, 10, 15};
			return timers[pts];
		}
		return 5f;
	}

	private void checkAndShowIndicator() {
		if (getHighestMove() != null) {
			ActionIndicator.setAction(this);
			GLog.p(Messages.get(this, "combo", count));
		}
		Hero hero = (Hero) target;
		if (hero.hasTalent(Talent.ACE_ARMOR_ACCUMULATION)) {
			int[] thresholds = {10, 7, 5};
			int threshold = thresholds[hero.pointsInTalent(Talent.ACE_ARMOR_ACCUMULATION) - 1];
			if (threshold > 0 && count % threshold == 0) {
				grantShield(hero);
			}
		}
	}

	private void grantArmorOnKill() {
		Hero hero = (Hero) target;
		if (hero.hasTalent(Talent.ACE_ARMOR_ACCUMULATION)) {
			grantShield(hero);
		}
	}

	private void grantShield(Hero hero) {
		if (hero.buff(BrokenSeal.WarriorShield.class) == null) return;
		int pts = hero.pointsInTalent(Talent.ACE_ARMOR_ACCUMULATION);
		float[] ratios = {0f, 0.05f, 0.08f, 0.12f};
		int shieldAmt = Math.round(hero.HT * ratios[pts]);
		BrokenSeal.WarriorShield shield = hero.buff(BrokenSeal.WarriorShield.class);
		shield.supercharge(Math.min(shield.maxShield(), shield.shielding() + shieldAmt));
	}

	@Override
	public boolean act() {
		comboTime -= TICK;
		spend(TICK);
		if (comboTime <= 0) {
			detach();
		}
		return true;
	}

	@Override
	public void detach() {
		super.detach();
		ActionIndicator.clearAction(this);
	}

	public int getComboCount() {
		return count;
	}

	public AceComboMove getHighestMove() {
		AceComboMove best = null;
		for (AceComboMove move : AceComboMove.values()) {
			if (count >= move.comboReq) {
				best = move;
			}
		}
		return best;
	}

	public boolean canUseMove(AceComboMove move) {
		if (move == AceComboMove.SHIELD_BASH && shieldBashUsed) return false;
		if (move == AceComboMove.COUNTER_STANCE && counterStanceUsed) return false;
		if (move == AceComboMove.FORTIFY && fortifyUsed) return false;
		return move.comboReq <= count;
	}

	public void useMove(AceComboMove move) {
		if (move == AceComboMove.COUNTER_STANCE) {
			counterStanceUsed = true;
			comboTime = getComboTime();
			initialComboTime = comboTime;
			Buff.affect(target, CounterTracker.class, 1f);
			((Hero)target).spendAndNext(((Hero)target).attackDelay());
			if (getHighestMove() == null) ActionIndicator.clearAction(this);
			return;
		}
		if (move == AceComboMove.FORTIFY) {
			fortifyUsed = true;
			comboTime = getComboTime();
			initialComboTime = comboTime;
			doFortify();
			((Hero)target).spendAndNext(((Hero)target).attackDelay());
			if (getHighestMove() == null) ActionIndicator.clearAction(this);
			return;
		}

		moveBeingUsed = move;
		GameScene.selectCell(listener);
	}

	private AceComboMove moveBeingUsed;

	private CellSelector.Listener listener = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			final Char enemy = Actor.findChar(cell);
			if (enemy == null || enemy == target || !Dungeon.level.heroFOV[cell]
					|| target.isCharmedBy(enemy)) {
				GLog.w(Messages.get(AceCombo.class, "bad_target"));
				return;
			}
			target.sprite.attack(enemy.pos, () -> doAttack(enemy, moveBeingUsed));
		}

		@Override
		public String prompt() {
			return Messages.get(AceCombo.class, "prompt");
		}
	};

	private void doFortify() {
		Hero hero = (Hero) target;
		BrokenSeal.WarriorShield shield = hero.buff(BrokenSeal.WarriorShield.class);
		if (shield != null) {
			shield.supercharge(shield.maxShield());
		}
		FortifyBuff fb = Buff.affect(hero, FortifyBuff.class, 3f);
		fb.regenBoost = 2;
	}

	void doAttack(Char enemy, AceComboMove move) {
		Hero hero = (Hero) target;
		float dmgMulti = 1f;
		int dmgBonus = 0;

		switch (move) {
			case SHIELD_BASH:
				dmgMulti = 1f;
				BrokenSeal.WarriorShield shield = hero.buff(BrokenSeal.WarriorShield.class);
				if (shield != null) {
					dmgBonus = shield.shielding();
				}
				break;
			case GUARDIAN_SLASH:
				dmgMulti = 1f;
				dmgBonus = Math.round(count * 0.25f);
				break;
			case ACE_STRIKE:
				dmgMulti = 1f;
				dmgBonus = Math.round(count * 0.30f);
				break;
			default:
				break;
		}

		AttackIndicator.target(enemy);
		boolean hit = hero.attack(enemy, dmgMulti, dmgBonus, 1f);

		if (hit) {
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);

			switch (move) {
				case SHIELD_BASH:
					if (enemy.isAlive()) {
						final int[] knockPos = {hero.pos};
						for (int n : PathFinder.NEIGHBOURS8) {
							int cell = enemy.pos + n;
							if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null
									&& Dungeon.level.trueDistance(hero.pos, cell) > Dungeon.level.trueDistance(hero.pos, enemy.pos)) {
								knockPos[0] = cell;
								break;
							}
						}
						if (knockPos[0] != hero.pos) {
							Actor.addDelayed(new Actor() {
								{ actPriority = VFX_PRIO; }
								@Override
								protected boolean act() {
									if (enemy.isAlive()) {
										enemy.pos = knockPos[0];
										enemy.sprite.move(hero.pos, knockPos[0]);
									}
									Actor.remove(this);
									return true;
								}
							}, 0.1f);
						}
					}
					shieldBashUsed = true;
					break;
				case GUARDIAN_SLASH:
				{
					int aoeDmg = (dmgBonus + (hit ? 1 : 0)) / 2;
					for (Char ch : Actor.chars()) {
						if (ch != enemy && ch.alignment == Char.Alignment.ENEMY
								&& ch.isAlive() && Dungeon.level.adjacent(enemy.pos, ch.pos)) {
							ch.damage(aoeDmg, hero);
						}
					}
					detach();
					ActionIndicator.clearAction(this);
					break;
				}
				case ACE_STRIKE:
					if (!enemy.isAlive()) {
						GuardModal.AceAbsorptionCounter abs = hero.buff(GuardModal.AceAbsorptionCounter.class);
						if (abs != null) abs.absorbedHits = 0;
					}
					detach();
					ActionIndicator.clearAction(this);
					break;
				default:
					break;
			}

			if (move != AceComboMove.GUARDIAN_SLASH && move != AceComboMove.ACE_STRIKE) {
				if (getHighestMove() == null) ActionIndicator.clearAction(this);
			}

		} else {
			if (move == AceComboMove.GUARDIAN_SLASH || move == AceComboMove.ACE_STRIKE) {
				detach();
				ActionIndicator.clearAction(this);
			}
		}

		if (!enemy.isAlive()) {
			hero.next();
		} else {
			hero.spendAndNext(hero.attackDelay());
		}
	}

	// region ActionIndicator

	@Override
	public String actionName() {
		return Messages.get(this, "action_name");
	}

	@Override
	public int actionIcon() {
		return HeroIcon.ACE_COMBO;
	}

	@Override
	public Visual secondaryVisual() {
		BitmapText txt = new BitmapText(Integer.toString(count), PixelScene.pixelFont);
		txt.hardlight(CharSprite.POSITIVE);
		txt.measure();
		return txt;
	}

	@Override
	public int indicatorColor() {
		AceComboMove move = getHighestMove();
		if (move != null) {
			int color = move.tintColor;
			color -= 0x222222;
			return color;
		}
		return 0x444444;
	}

	@Override
	public void doAction() {
		GameScene.show(new WndAceCombo(this));
	}

	// endregion

	// region ComboMove enum

	public enum AceComboMove {
		SHIELD_BASH   (2,  0x00AAFF),
		COUNTER_STANCE(4,  0x44CCFF),
		GUARDIAN_SLASH(6,  0xFFCC00),
		FORTIFY       (8,  0x44FF44),
		ACE_STRIKE    (10, 0xFF4444);

		public int comboReq;
		public int tintColor;

		AceComboMove(int comboReq, int tintColor) {
			this.comboReq = comboReq;
			this.tintColor = tintColor;
		}

		public String moveName() {
			return Messages.get(AceCombo.class, name() + ".name");
		}

		public String moveDesc() {
			return Messages.get(AceCombo.class, name() + ".desc");
		}
	}

	// endregion

	// region Inner buffs

	public static class CounterTracker extends FlavourBuff {
		{
			type = buffType.POSITIVE;
			actPriority = HERO_PRIO + 1;
		}

		@Override
		public boolean act() {
			detach();
			return true;
		}
	}

	public static class CounterRiposteTracker extends Buff {
		{
			actPriority = VFX_PRIO;
		}
		public Char enemy;

		@Override
		public boolean act() {
			if (enemy != null && enemy.isAlive() && ((Hero)target).canAttack(enemy) && !target.isCharmedBy(enemy)) {
				AceCombo combo = target.buff(AceCombo.class);
				if (combo != null) {
					combo.doAttack(enemy, AceComboMove.COUNTER_STANCE);
				}
			}
			Actor.remove(this);
			return true;
		}

		private static final String ENEMY = "enemy";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(ENEMY, enemy != null ? enemy.id() : -1);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			int id = bundle.getInt(ENEMY);
			if (id != -1) enemy = (Char) Actor.findById(id);
		}
	}

	public static class FortifyBuff extends FlavourBuff {
		{
			type = buffType.POSITIVE;
		}
		public int regenBoost = 2;

		@Override
		public int icon() { return BuffIndicator.ARMOR; }
		@Override
		public void tintIcon(com.watabou.noosa.Image icon) {
			icon.hardlight(0.3f, 1f, 0.3f);
		}

		private static final String REGEN_BOOST = "regen_boost";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(REGEN_BOOST, regenBoost);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			regenBoost = bundle.getInt(REGEN_BOOST);
		}
	}

	// endregion

	// region Serialization

	private static final String COUNT            = "count";
	private static final String TIME             = "comboTime";
	private static final String INITIAL_TIME     = "initialComboTime";
	private static final String SHIELD_BASH_USED = "shieldBashUsed";
	private static final String COUNTER_USED     = "counterStanceUsed";
	private static final String FORTIFY_USED     = "fortifyUsed";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(COUNT, count);
		bundle.put(TIME, comboTime);
		bundle.put(INITIAL_TIME, initialComboTime);
		bundle.put(SHIELD_BASH_USED, shieldBashUsed);
		bundle.put(COUNTER_USED, counterStanceUsed);
		bundle.put(FORTIFY_USED, fortifyUsed);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		count = bundle.getInt(COUNT);
		comboTime = bundle.getFloat(TIME);
		initialComboTime = bundle.getFloat(INITIAL_TIME);
		shieldBashUsed = bundle.getBoolean(SHIELD_BASH_USED);
		counterStanceUsed = bundle.getBoolean(COUNTER_USED);
		fortifyUsed = bundle.getBoolean(FORTIFY_USED);

		if (getHighestMove() != null) {
			ActionIndicator.setAction(this);
		}
	}

	// endregion
}
