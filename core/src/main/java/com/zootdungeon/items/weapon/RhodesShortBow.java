package com.zootdungeon.items.weapon;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.items.weapon.missiles.MissileWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class RhodesShortBow extends Weapon {

	public static final String AC_SHOOT  = "SHOOT";
	public static final String AC_RECALL = "RECALL";

	private static final int MAX_ARROWS = 6;
	private int arrows = MAX_ARROWS;

	{
		image = ItemSpriteSheet.SPIRIT_BOW;

		defaultAction = AC_SHOOT;
		usesTargeting = true;

		unique = true;
		bones = false;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_SHOOT);
		if (hero.buff(RhodesArrowStuck.class) != null) {
			actions.add(AC_RECALL);
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_SHOOT)) {
			if (arrows <= 0 && hero.buff(RhodesArrowStuck.class) == null) {
				GLog.w(Messages.get(this, "no_arrows"));
				return;
			}
			curUser = hero;
			curItem = this;
			GameScene.selectCell(shooter);
		} else if (action.equals(AC_RECALL)) {
			recallArrows(hero);
		}
	}

	private void recallArrows(Hero hero) {
		RhodesArrowStuck stuck = hero.buff(RhodesArrowStuck.class);
		if (stuck != null) {
			int recovered = stuck.count();
			arrows = Math.min(MAX_ARROWS, arrows + recovered);
			stuck.detach();
			GLog.p(Messages.get(this, "recall", recovered));
		}
	}

	@Override
	public int min(int lvl) {
		return 3;
	}

	@Override
	public int max(int lvl) {
		return 8;
	}

	@Override
	public int STRReq(int lvl) {
		return STRReq(2, lvl);
	}

	@Override
	public String info() {
		String info = super.info();
		info += "\n\n" + Messages.get(this, "stats", min(), max(), STRReq());
		info += "\n\n" + Messages.get(this, "arrows", arrows, MAX_ARROWS);
		return info;
	}

	@Override
	public String status() {
		return arrows + "/" + MAX_ARROWS;
	}

	private static final String ARROWS = "arrows";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(ARROWS, arrows);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		arrows = bundle.getInt(ARROWS);
	}

	public RhodesArrow knockArrow() {
		return new RhodesArrow();
	}

	public class RhodesArrow extends MissileWeapon {

		{
			image = ItemSpriteSheet.SPIRIT_ARROW;
			hitSound = Assets.Sounds.HIT_ARROW;
			tier = 1;
		}

		@Override
		public int min(int lvl) {
			return 1 + lvl;
		}

		@Override
		public int max(int lvl) {
			return 3 + 2 * lvl;
		}

		@Override
		public int STRReq(int lvl) {
			return RhodesShortBow.this.STRReq();
		}

		@Override
		public boolean hasEnchant(Class<? extends Enchantment> type, Char owner) {
			return RhodesShortBow.this.hasEnchant(type, owner);
		}

		@Override
		public int proc(Char attacker, Char defender, int damage) {
			return RhodesShortBow.this.proc(attacker, defender, damage);
		}

		@Override
		protected void onThrow(int cell) {
			Char enemy = Actor.findChar(cell);
			if (enemy == null || enemy == curUser) {
				parent = null;
				CellEmitter.get(cell).burst(Speck.factory(Speck.WOOL), 3);
				return;
			}
			if (!curUser.shoot(enemy, this)) {
				CellEmitter.get(cell).burst(Speck.factory(Speck.WOOL), 3);
				return;
			}
			// Arrow sticks into enemy
			RhodesArrowStuck stuck = Buff.affect(curUser, RhodesArrowStuck.class);
			stuck.incStuck();
		}

		@Override
		public void throwSound() {
			Sample.INSTANCE.play(Assets.Sounds.ATK_SPIRITBOW, 1, Random.Float(0.87f, 1.15f));
		}

		@Override
		public void cast(final Hero user, final int dst) {
			if (arrows <= 0) {
				GLog.w(Messages.get(RhodesShortBow.class, "no_arrows"));
				return;
			}
			arrows--;
			updateQuickslot();
			super.cast(user, dst);
		}
	}

	private CellSelector.Listener shooter = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer target) {
			if (target != null) {
				knockArrow().cast(curUser, target);
			}
		}

		@Override
		public String prompt() {
			return Messages.get(RhodesShortBow.class, "prompt");
		}
	};

	/**
	 * Tracks arrows stuck in enemies. Each stack = one arrow that can be recalled.
	 */
	public static class RhodesArrowStuck extends FlavourBuff {

		{
			type = buffType.NEGATIVE;
			revivePersists = true;
		}

		private int stuckCount = 0;

		public void incStuck() {
			stuckCount++;
			BuffIndicator.refreshHero();
		}

		public int count() {
			return stuckCount;
		}

		@Override
		public int icon() {
			return BuffIndicator.NONE;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.6f, 0.4f, 0.2f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(stuckCount);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", stuckCount);
		}

		private static final String STUCK_COUNT = "stuck_count";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(STUCK_COUNT, stuckCount);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			stuckCount = bundle.getInt(STUCK_COUNT);
		}
	}
}
