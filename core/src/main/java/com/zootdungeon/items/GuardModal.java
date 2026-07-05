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
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.BlazeHeatBuff;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.HeroSubClass;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.sprites.TextureRegistry;
import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

/**
 * GuardModal is to RESERVED_GUARD what BrokenSeal is to WARRIOR.
 *
 * It can be affixed to armor to provide a regenerating shield.
 * For OP_SHARP subclass, it can also be THROWN at enemies to
 * initiate the Feather Duel mechanic.
 */
public class GuardModal extends BrokenSeal {

	{
		image = TextureRegistry.once("guard_modal","cola/guard_modal.png",0,0,32,32);
		cursedKnown = levelKnown = true;
		unique = true;
		bones = false;

		defaultAction = AC_THROW;
	}

	/**
	 * Uses RESERVED_GUARD's IRON_WILL talent instead of Warrior's for shield calculation.
	 */
	@Override
	public int maxShield(int armTier, int armLvl) {
		return armTier + armLvl + Dungeon.hero.pointsInTalent(Talent.RESERVED_GUARD_IRON_WILL);
	}

	/**
	 * OP_SHARP throws GuardModal to initiate a Feather Duel.
	 * Called when the thrown GuardModal reaches its target.
	 */
	@Override
	protected void onThrow(int cell) {
		Char enemy = Actor.findChar(cell);

		if (curUser instanceof Hero
				&& ((Hero) curUser).subClass == HeroSubClass.OP_SHARP
				&& (curUser.buff(FeatherDuelCooldown.class) == null
					|| curUser.buff(DuelMomentumBuff.class) != null)) {
			if (enemy != null && enemy.alignment != curUser.alignment) {
				// Bounty Duel: gold cost to initiate duel
				Hero hero = (Hero) curUser;
				boolean canDuel = true;
				if (hero.hasTalent(Talent.OP_SHARP_BOUNTY_DUEL)) {
					int pts = hero.pointsInTalent(Talent.OP_SHARP_BOUNTY_DUEL);
					int cost = pts >= 2 ? 40 : 20; // 20/40/40
					if (Dungeon.gold < cost) {
						GLog.w(Messages.get(GuardModal.class, "bounty_not_enough_gold"));
						canDuel = false;
					} else {
						Dungeon.gold -= cost;
					}
				}
				if (canDuel) {
					Sample.INSTANCE.play(Assets.Sounds.DESCEND);
					float markDuration = 8f; // Base duel duration for OP_SHARP subclass
					FeatherDuelMark mark = Buff.affect(enemy, FeatherDuelMark.class, markDuration);
					mark.stuckItem = this;
					FeatherDuelGuard guard = Buff.affect(curUser, FeatherDuelGuard.class);
					guard.duelTarget = enemy;
						Buff.affect(curUser, FeatherDuelCooldown.class, 40f);
						if (hero.hasTalent(Talent.OP_SHARP_BOUNTY_DUEL)) {
							BountyDuelLootBuff lootBuff = Buff.affect(enemy, BountyDuelLootBuff.class);
							lootBuff.pts = hero.pointsInTalent(Talent.OP_SHARP_BOUNTY_DUEL);
						}
					// Item is now stored in the mark buff; don't drop it on the ground
					return;
				}
			}
		}

		// BLAZE: Saw Mastery Lv3 explosion throw
		if (curUser instanceof Hero
				&& ((Hero) curUser).subClass == HeroSubClass.BLAZE
				&& ((Hero) curUser).pointsInTalent(Talent.BLAZE_SAW_MASTERY) >= 3
				&& (curUser.buff(BlazeHeatBuff.class) != null
					|| curUser.buff(com.zootdungeon.actors.buffs.Burning.class) != null)) {
			if (enemy != null && enemy.alignment != curUser.alignment) {
				Hero hero = (Hero) curUser;
				BlazeHeatBuff heat = hero.buff(BlazeHeatBuff.class);
				int heatStacks = heat != null ? (int) heat.count() : 0;
				int dmg = 5 + heatStacks * 2 + Dungeon.scalingDepth();
				com.zootdungeon.items.bombs.Bomb.ConjuredBomb bomb =
					new com.zootdungeon.items.bombs.Bomb.ConjuredBomb();
				bomb.explode(cell);
				if (enemy.isAlive()) {
					enemy.damage(dmg, hero);
					com.zootdungeon.actors.buffs.Buff.affect(enemy,
						com.zootdungeon.actors.buffs.Burning.class).reignite(enemy, 6f);
				}
				com.zootdungeon.actors.buffs.Buff.affect(hero,
					FeatherDuelCooldown.class, 40f);
				Sample.INSTANCE.play(Assets.Sounds.BLAST);
				if (!this.collect(hero.belongings.backpack)) {
					Dungeon.level.drop(this, cell).sprite.drop();
				}
				return;
			}
		}

		// For non-OP_SHARP/non-BLAZE or miss: drop on the ground as normal
		super.onThrow(cell);
	}

	// region Buffs

	public static class FeatherDuelCooldown extends FlavourBuff {
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.5f, 0.5f, 0.15f); }
		public float iconFadePercent() { return Math.max(0, visualcooldown() / 40); }
	}

	public static class FeatherDuelMark extends FlavourBuff {
		{
			type = buffType.NEGATIVE;
			announced = true;
		}
		public Item stuckItem = null;

		public int icon() { return BuffIndicator.FEATHER_DUEL_MARK; }
		public void tintIcon(Image icon) { icon.hardlight(0.8f, 0.6f, 0.1f); }
		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
		@Override
		public void detach() {
			super.detach();
			// Drop the stuck item (e.g. GuardModal) when mark expires
			if (stuckItem != null && target != null) {
				Dungeon.level.drop(stuckItem, target.pos).sprite.drop();
			}
			// Remove duel guard from the hero when mark expires
			if (Dungeon.hero != null && Dungeon.hero.buff(FeatherDuelGuard.class) != null) {
				FeatherDuelGuard guard = Dungeon.hero.buff(FeatherDuelGuard.class);
				boolean hasMomentum = Dungeon.hero.buff(DuelMomentumBuff.class) != null;
				// Duel Momentum: if target was killed and hero has talent, keep guard alive
				if (!target.isAlive() && Dungeon.hero.hasTalent(Talent.OP_SHARP_DUEL_MOMENTUM)) {
					int pts = Dungeon.hero.pointsInTalent(Talent.OP_SHARP_DUEL_MOMENTUM);
					float duration = 3f * pts; // 3/6/9 turns
					Buff.affect(Dungeon.hero, DuelMomentumBuff.class, duration);
						guard.duelTarget = null;
				} else if (hasMomentum) {
					// During momentum, guard persists even if mark expires naturally
					guard.duelTarget = null;
				} else {
					guard.detach();
				}
			}
		}


		private static final String STUCK_ITEM = "stuck_item";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(STUCK_ITEM, stuckItem);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			stuckItem = (Item) bundle.get(STUCK_ITEM);
		}
	}

	public static class FeatherDuelGuard extends Buff {
		{
			type = buffType.POSITIVE;
			announced = true;
		}
		public Char duelTarget = null;
		@Override
		public int icon() { return BuffIndicator.FEATHER_DUEL_GUARD; }
		@Override
		public void tintIcon(Image icon) { icon.hardlight(0.4f, 0.6f, 1f); }
		@Override
		public String iconTextDisplay() {
			if (duelTarget != null && duelTarget.isAlive()) {
				return Integer.toString((int) Dungeon.level.distance(target.pos, duelTarget.pos));
			}
			return "";
		}
		@Override
		public String desc() {
			return Messages.get(this, "desc");
		}
		public float damageMultiplier(Char attacker) {
			if (duelTarget != null && duelTarget.isAlive() && attacker != duelTarget) {
				int pts = Dungeon.hero.pointsInTalent(Talent.OP_SHARP_FEATHER_FURY);
				// 30%/50%/80% damage reduction at 1/2/3 talent points
				if (pts >= 3) return 0.2f;
				else if (pts == 2) return 0.5f;
				else if (pts == 1) return 0.7f;
				else return 1.0f;
			}
			return 1.0f;
		}
		private static final String DUEL_TARGET = "duel_target";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(DUEL_TARGET, duelTarget != null ? duelTarget.id() : -1);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			int id = bundle.getInt(DUEL_TARGET);
			if (id != -1) {
				duelTarget = (Char) Actor.findById(id);
			}
		}
	}

	/**
	 * Duel Momentum: after killing a duel target, the hero can ignore
	 * Feather Duel cooldown, the duel guard persists, and action speed is boosted.
	 * Duration: 3/6/9 turns based on OP_SHARP_DUEL_MOMENTUM talent points.
	 */
	public static class DuelMomentumBuff extends FlavourBuff {
		{
			type = buffType.POSITIVE;
			announced = true;
		}

		@Override
		public int icon() { return BuffIndicator.FEATHER_DUEL_GUARD; }
		@Override
		public void tintIcon(Image icon) { icon.hardlight(0.9f, 0.4f, 0.1f); }

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}

		/**
		 * Speed multiplier based on talent points: 1.10x/1.20x/1.40x at 1/2/3 points.
		 */
		public float speedMultiplier() {
			if (Dungeon.hero != null && Dungeon.hero.hasTalent(Talent.OP_SHARP_DUEL_MOMENTUM)) {
				int pts = Dungeon.hero.pointsInTalent(Talent.OP_SHARP_DUEL_MOMENTUM);
				float[] boost = {1.0f, 1.10f, 1.20f, 1.40f};
				return boost[pts];
			}
			return 1.0f;
		}

		@Override
		public void detach() {
			super.detach();
			// When momentum expires and no active mark, detach the guard
			if (Dungeon.hero != null) {
				FeatherDuelGuard guard = Dungeon.hero.buff(FeatherDuelGuard.class);
				if (guard != null && guard.duelTarget == null) {
					guard.detach();
				}
			}
		}
	}

		/** Bounty Duel: marks an enemy for extra loot when killed during the duel. */
		public static class BountyDuelLootBuff extends FlavourBuff {
			{
				type = buffType.NEUTRAL;
			}
			public int pts;
			@Override public int icon() { return BuffIndicator.NONE; }
		}

		// endregion
}
