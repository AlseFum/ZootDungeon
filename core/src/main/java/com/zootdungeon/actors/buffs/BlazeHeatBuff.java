package com.zootdungeon.actors.buffs;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * BLAZE subclass Heat buff. Stacks speed up natural regen and hunger, but both
 * consume heat. Heat also passively cools down over time.
 */
public class BlazeHeatBuff extends CounterBuff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	private int turnCounter;

	@Override
	public int icon() { return BuffIndicator.FIRE; }

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(1f, 0.4f, 0f);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString((int) count());
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", count());
	}

	/**
	 * Regen speed multiplier. Consumes 1 heat if stacks available.
	 */
	public float consumeRegenBoost() {
		if (count() <= 0) return 1f;
		countDown(1);
		return 1f + Math.min(count() + 1, 10) * 0.05f;
	}

	/**
	 * Hunger speed multiplier. Consumes 1 heat if stacks available.
	 */
	public float consumeHungerBoost() {
		if (count() <= 0) return 1f;
		countDown(1);
		return 1f + Math.min(count() + 1, 10) * 0.03f;
	}

	@Override
	public boolean act() {
		spend(TICK);
		turnCounter++;
		// passive cooldown: lose 1 heat every 3 turns
		if (turnCounter >= 3 && count() > 0) {
			turnCounter = 0;
			countDown(1);
		}
		if (count() <= 0) {
			detach();
			return true;
		}
		return true;
	}

	/**
	 * Infernal Endurance: while Burning, reduce damage and convert absorbed damage into Heat.
	 * Called from {@link com.zootdungeon.actors.hero.Hero#defenseProc}.
	 * @return reduced damage
	 */
	public static int infernalEndurance(Hero hero, int damage) {
		if (damage <= 0 || hero.buff(Burning.class) == null) return damage;
		if (!hero.hasTalent(Talent.BLAZE_INFERNAL_ENDURANCE)) return damage;
		BlazeHeatBuff heat = hero.buff(BlazeHeatBuff.class);
		int stacks = heat != null ? (int) heat.count() : 0;
		if (stacks <= 0) return damage;

		int pts = hero.pointsInTalent(Talent.BLAZE_INFERNAL_ENDURANCE);
		// per-stack resist increases with talent: 3% / 7.5% / 15%, capped at 75%
		float[] perStack = {0f, 0.03f, 0.075f, 0.15f};
		float resistRate = Math.min(0.75f, stacks * perStack[pts]);

		int absorbed = Math.round(damage * resistRate);
		damage -= absorbed;

		if (absorbed > 0) {
			heat.countUp(Math.max(1, Math.round(absorbed * resistRate)));
		}
		return damage;
	}

	private static final String TURN_COUNTER = "turnCounter";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(TURN_COUNTER, turnCounter);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		turnCounter = bundle.getInt(TURN_COUNTER);
	}
}
